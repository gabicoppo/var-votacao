package br.unicamp.var.web.organizador;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.unicamp.var.apuracao.ContagemVotos;
import br.unicamp.var.apuracao2turno.ResultadoApuracao;
import br.unicamp.var.auditoria.TipoEvento;
import br.unicamp.var.eleitorado.Eleitor;
import br.unicamp.var.processo.PeriodoVotacao;
import br.unicamp.var.processo.Votacao;
import br.unicamp.var.urna.Sigilo;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Testes do {@link ConversorFormularioVotacao}: validação por campo, com classes de equivalência
 * e valores-limite, e conversão do formulário válido numa {@link Votacao}.
 */
class ConversorFormularioVotacaoTest {

    private static final Clock RELOGIO = Clock.fixed(Instant.parse("2027-02-20T12:00:00Z"), ZoneOffset.UTC);
    private static final double TOLERANCIA = 1e-12;

    /** Formulário válido; cada teste troca só o campo que testa. */
    private static FormularioVotacao valido() {
        FormularioVotacao f = new FormularioVotacao();
        f.setPergunta("Quem deve ser o presidente?");
        f.setOpcoes("Chapa A\nChapa B");
        f.setEleitores("e01;Ana\ne02;Bruno\ne03;Carla");
        return f;
    }

    private static List<String> camposComErro(FormularioVotacao f) {
        return ConversorFormularioVotacao.validar(f).stream().map(ErroCampo::campo).toList();
    }

    private static String mensagemDe(FormularioVotacao f, String campo) {
        return ConversorFormularioVotacao.validar(f).stream()
                .filter(erro -> erro.campo().equals(campo))
                .map(ErroCampo::mensagem)
                .findFirst()
                .orElseThrow();
    }

    @Nested
    @DisplayName("Formulário")
    class Formulario {

        @Test
        @DisplayName("começa com maioria absoluta, 2º turno, voto secreto, quórum 50% e fração 2/3")
        void valoresPadrao() {
            FormularioVotacao f = new FormularioVotacao();

            assertEquals(TipoRegra.ABSOLUTA, f.getTipoRegra());
            assertTrue(f.isSegundoTurno());
            assertEquals(Sigilo.SECRETO, f.getSigilo());
            assertEquals("50%", f.getQuorum());
            assertEquals("2/3", f.getFracaoExigida());
            assertEquals("", f.getDescricao());
        }

        @Test
        @DisplayName("texto nulo vira texto vazio")
        void nuloViraVazio() {
            FormularioVotacao f = new FormularioVotacao();
            f.setPergunta(null);
            f.setDescricao(null);
            f.setOpcoes(null);
            f.setQuorum(null);
            f.setFracaoExigida(null);
            f.setEleitores(null);

            assertEquals(List.of("", "", "", "", "", ""), List.of(f.getPergunta(), f.getDescricao(),
                    f.getOpcoes(), f.getQuorum(), f.getFracaoExigida(), f.getEleitores()));
        }

        @Test
        @DisplayName("o formulário válido não tem erros")
        void validoSemErros() {
            assertEquals(List.of(), ConversorFormularioVotacao.validar(valido()));
        }

        @Test
        @DisplayName("formulário vazio aponta pergunta, opções e eleitores")
        void vazioApontaObrigatorios() {
            assertEquals(List.of("pergunta", "opcoes", "eleitores"), camposComErro(new FormularioVotacao()));
        }
    }

    @Nested
    @DisplayName("Pergunta")
    class Pergunta {

        @ParameterizedTest
        @ValueSource(strings = {"", "   "})
        @DisplayName("é obrigatória")
        void obrigatoria(String pergunta) {
            FormularioVotacao f = valido();
            f.setPergunta(pergunta);

            assertEquals("Informe a pergunta da votação.", mensagemDe(f, "pergunta"));
        }
    }

    @Nested
    @DisplayName("Opções")
    class Opcoes {

        @Test
        @DisplayName("uma opção só é pouco")
        void umaOpcao() {
            FormularioVotacao f = valido();
            f.setOpcoes("Chapa A");

            assertEquals("Informe pelo menos duas opções, uma por linha.", mensagemDe(f, "opcoes"));
        }

        @Test
        @DisplayName("duas opções é o mínimo aceito")
        void duasOpcoes() {
            FormularioVotacao f = valido();
            f.setOpcoes("Sim\nNão");

            assertFalse(camposComErro(f).contains("opcoes"));
        }

        @Test
        @DisplayName("linhas em branco e espaços nas pontas são ignorados")
        void ignoraLinhasEmBranco() {
            FormularioVotacao f = valido();
            f.setOpcoes("\n  Chapa A  \n\n   \r\nChapa B\n");

            assertFalse(camposComErro(f).contains("opcoes"));
            assertEquals(List.of("Chapa A", "Chapa B"),
                    ConversorFormularioVotacao.paraVotacao(f, RELOGIO, "organizador").opcoes());
        }

        @Test
        @DisplayName("uma opção e uma linha em branco contam como uma opção só")
        void umaOpcaoComLinhaEmBranco() {
            FormularioVotacao f = valido();
            f.setOpcoes("Chapa A\n   ");

            assertTrue(camposComErro(f).contains("opcoes"));
        }

        @Test
        @DisplayName("rejeita opção repetida, mesmo com espaços diferentes")
        void repetida() {
            FormularioVotacao f = valido();
            f.setOpcoes("Chapa A\nChapa B\n  Chapa A ");

            assertEquals("A opção \"Chapa A\" aparece mais de uma vez.", mensagemDe(f, "opcoes"));
        }
    }

    @Nested
    @DisplayName("Regra, quórum e fração")
    class Regra {

        private FormularioVotacao qualificada(String quorum, String fracao) {
            FormularioVotacao f = valido();
            f.setTipoRegra(TipoRegra.QUALIFICADA);
            f.setQuorum(quorum);
            f.setFracaoExigida(fracao);
            return f;
        }

        @Test
        @DisplayName("regra é obrigatória")
        void regraObrigatoria() {
            FormularioVotacao f = valido();
            f.setTipoRegra(null);

            assertEquals(List.of("tipoRegra"), camposComErro(f));
        }

        @Test
        @DisplayName("quórum e fração inválidos não importam fora da maioria qualificada")
        void ignoraQuorumForaDaQualificada() {
            FormularioVotacao f = valido();
            f.setTipoRegra(TipoRegra.SIMPLES);
            f.setQuorum("abc");
            f.setFracaoExigida("");

            assertEquals(List.of(), camposComErro(f));
        }

        @ParameterizedTest(name = "\"{0}\" é aceito")
        @ValueSource(strings = {"0,1", "100", "3/3", "1/3", "50%", "66,7", "66.7 %", " 2 / 3 "})
        @DisplayName("valores dentro de (0, 100%] são aceitos")
        void aceitaDentroDoIntervalo(String valor) {
            assertEquals(List.of(), camposComErro(qualificada(valor, valor)));
        }

        @ParameterizedTest(name = "\"{0}\" está fora do intervalo")
        @ValueSource(strings = {"0", "0/3", "100,1", "4/3", "0%"})
        @DisplayName("valores fora de (0, 100%] são rejeitados")
        void rejeitaForaDoIntervalo(String valor) {
            FormularioVotacao f = qualificada(valor, valor);

            assertEquals(List.of("quorum", "fracaoExigida"), camposComErro(f));
            assertEquals("Informe um valor maior que 0% e de no máximo 100%.", mensagemDe(f, "quorum"));
        }

        @ParameterizedTest(name = "\"{0}\" tem formato inválido")
        @ValueSource(strings = {"abc", "2/0", "-50", "1/2/3", "50%%", "2/", "/3"})
        @DisplayName("formatos inválidos são rejeitados")
        void rejeitaFormatoInvalido(String valor) {
            FormularioVotacao f = qualificada(valor, "2/3");

            assertEquals("Use uma fração (2/3) ou um percentual (50%).", mensagemDe(f, "quorum"));
        }

        @Test
        @DisplayName("na maioria qualificada, quórum e fração são obrigatórios")
        void obrigatoriosNaQualificada() {
            FormularioVotacao f = qualificada(" ", "");

            assertEquals("Informe o quórum mínimo.", mensagemDe(f, "quorum"));
            assertEquals("Informe a fração exigida.", mensagemDe(f, "fracaoExigida"));
        }
    }

    @Nested
    @DisplayName("lerFracao")
    class LerFracao {

        @Test
        @DisplayName("2/3 vira exatamente 2.0 / 3")
        void doisTercosExato() {
            assertEquals(2.0 / 3, ConversorFormularioVotacao.lerFracao("2/3"));
        }

        @ParameterizedTest(name = "\"{0}\" vira {1}")
        @CsvSource(delimiter = '|', value = {
            "3/5|0.6", "0/3|0", "4/3|1.3333333333333333", "50|0.5", "50%|0.5", "66,7|0.667",
            "0,1|0.001", "100|1", "100,1|1.001", "  25 %  |0.25", "7.5|0.075"})
        void converte(String texto, double esperado) {
            assertEquals(esperado, ConversorFormularioVotacao.lerFracao(texto), TOLERANCIA);
        }

        @ParameterizedTest(name = "\"{0}\" é rejeitado")
        @ValueSource(strings = {"abc", "", "2/0", "-1", "1,", ",5", "1/-2", "50 % %"})
        void rejeita(String texto) {
            assertThrows(IllegalArgumentException.class, () -> ConversorFormularioVotacao.lerFracao(texto));
        }

        @Test
        @DisplayName("rejeita texto nulo")
        void rejeitaNulo() {
            assertThrows(IllegalArgumentException.class, () -> ConversorFormularioVotacao.lerFracao(null));
        }
    }

    @Nested
    @DisplayName("Sigilo")
    class SigiloDoFormulario {

        @Test
        @DisplayName("sigilo é obrigatório")
        void obrigatorio() {
            FormularioVotacao f = valido();
            f.setSigilo(null);

            assertEquals(List.of("sigilo"), camposComErro(f));
        }
    }

    @Nested
    @DisplayName("Período")
    class Periodo {

        private final LocalDateTime inicio = LocalDateTime.of(2027, 3, 1, 8, 0);

        @Test
        @DisplayName("sem início e sem fim é válido")
        void semPeriodo() {
            assertFalse(camposComErro(valido()).contains("inicio"));
        }

        @Test
        @DisplayName("só o fim aponta erro no início")
        void soFim() {
            FormularioVotacao f = valido();
            f.setFim(inicio);

            assertEquals(List.of("inicio"), camposComErro(f));
        }

        @Test
        @DisplayName("só o início aponta erro no fim")
        void soInicio() {
            FormularioVotacao f = valido();
            f.setInicio(inicio);

            assertEquals(List.of("fim"), camposComErro(f));
        }

        @Test
        @DisplayName("fim igual ao início é rejeitado")
        void fimIgualAoInicio() {
            FormularioVotacao f = valido();
            f.setInicio(inicio);
            f.setFim(inicio);

            assertEquals("O fim do período deve ser depois do início.", mensagemDe(f, "fim"));
        }

        @Test
        @DisplayName("fim um minuto depois do início é aceito")
        void fimUmMinutoDepois() {
            FormularioVotacao f = valido();
            f.setInicio(inicio);
            f.setFim(inicio.plusMinutes(1));

            assertEquals(List.of(), camposComErro(f));
        }
    }

    @Nested
    @DisplayName("Eleitores")
    class Eleitores {

        @ParameterizedTest
        @ValueSource(strings = {"", "  \n  "})
        @DisplayName("ao menos um eleitor é obrigatório")
        void obrigatorio(String texto) {
            FormularioVotacao f = valido();
            f.setEleitores(texto);

            assertEquals("Informe pelo menos um eleitor, no formato id;nome.", mensagemDe(f, "eleitores"));
        }

        @Test
        @DisplayName("um eleitor só é aceito")
        void umEleitor() {
            FormularioVotacao f = valido();
            f.setEleitores("e01;Ana");

            assertEquals(List.of(), camposComErro(f));
        }

        @ParameterizedTest(name = "{0}")
        @CsvSource(delimiter = '|', value = {
            "e01;Ana\\nsem separador|Linha 2: use o formato id;nome, com um só ponto e vírgula.",
            "e01;Ana\\ne02;Bia;extra|Linha 2: use o formato id;nome, com um só ponto e vírgula.",
            "e01;Ana\\n\\n ;Bruno|Linha 3: falta o id do eleitor.",
            "e01; |Linha 1: falta o nome do eleitor.",
            "e01;Ana\\ne02;Bia\\ne01;Outra|Linha 3: o id \"e01\" já apareceu antes."})
        @DisplayName("linha inválida aponta o número da linha em português")
        void linhaInvalida(String texto, String esperado) {
            FormularioVotacao f = valido();
            f.setEleitores(texto.replace("\\n", "\n"));

            assertEquals(esperado, mensagemDe(f, "eleitores"));
        }

        @Test
        @DisplayName("mensagem desconhecida do domínio vira um aviso genérico")
        void mensagemDesconhecida() {
            assertEquals("Linha 4: linha inválida.", ConversorFormularioVotacao.traduzirErroEleitores("linha 4: ???"));
            assertEquals("Lista de eleitores inválida. Use uma linha por eleitor, no formato id;nome.",
                    ConversorFormularioVotacao.traduzirErroEleitores("deve haver ao menos um eleitor"));
            assertEquals("Lista de eleitores inválida. Use uma linha por eleitor, no formato id;nome.",
                    ConversorFormularioVotacao.traduzirErroEleitores(null));
        }
    }

    @Nested
    @DisplayName("Conversão em votação")
    class Conversao {

        @Test
        @DisplayName("monta a votação em rascunho com tudo o que foi digitado")
        void montaVotacao() {
            FormularioVotacao f = valido();
            f.setDescricao("  Mandato 2027–2029 ");
            f.setSigilo(Sigilo.IDENTIFICADO);

            Votacao votacao = ConversorFormularioVotacao.paraVotacao(f, RELOGIO, "organizador");

            assertEquals("rascunho", votacao.status());
            assertEquals("Quem deve ser o presidente?", votacao.pergunta());
            assertEquals("Mandato 2027–2029", votacao.descricao());
            assertEquals(List.of("Chapa A", "Chapa B"), votacao.opcoes());
            assertEquals(Sigilo.IDENTIFICADO, votacao.sigilo());
            assertEquals(List.of(new Eleitor("e01", "Ana"), new Eleitor("e02", "Bruno"), new Eleitor("e03", "Carla")),
                    votacao.eleitorado().listar());
            assertEquals(Optional.empty(), votacao.periodo());
            assertEquals(RELOGIO.instant(), votacao.criadaEm());
            assertEquals("organizador", votacao.auditoria().get(0).autor());
            assertEquals(TipoEvento.CRIADA, votacao.auditoria().get(0).tipo());
        }

        @Test
        @DisplayName("converte o período do fuso de São Paulo para Instant")
        void converteOPeriodo() {
            FormularioVotacao f = valido();
            f.setInicio(LocalDateTime.of(2027, 3, 1, 8, 0));
            f.setFim(LocalDateTime.of(2027, 3, 1, 18, 30));

            Votacao votacao = ConversorFormularioVotacao.paraVotacao(f, RELOGIO, "organizador");

            assertEquals(Optional.of(new PeriodoVotacao(Instant.parse("2027-03-01T11:00:00Z"),
                    Instant.parse("2027-03-01T21:30:00Z"))), votacao.periodo());
        }

        @Test
        @DisplayName("maioria simples")
        void maioriaSimples() {
            FormularioVotacao f = valido();
            f.setTipoRegra(TipoRegra.SIMPLES);

            assertEquals("Maioria simples: vence a opção mais votada; empate fica sem vencedor.",
                    ConversorFormularioVotacao.paraVotacao(f, RELOGIO, "organizador").regra().descricao());
        }

        @Test
        @DisplayName("maioria absoluta com e sem 2º turno")
        void maioriaAbsoluta() {
            FormularioVotacao f = valido();
            assertEquals("Maioria absoluta: mais da metade dos votos válidos, com 2º turno entre os dois mais votados.",
                    ConversorFormularioVotacao.descricaoRegra(f));

            f.setSegundoTurno(false);
            assertEquals("Maioria absoluta: mais da metade dos votos válidos, sem 2º turno.",
                    ConversorFormularioVotacao.descricaoRegra(f));
        }

        @Test
        @DisplayName("maioria qualificada de 2/3 aprova exatamente 2 de 3 votos (limite da A5)")
        void maioriaQualificadaNoLimite() {
            FormularioVotacao f = valido();
            f.setTipoRegra(TipoRegra.QUALIFICADA);
            f.setQuorum("50%");
            f.setFracaoExigida("2/3");

            Votacao votacao = ConversorFormularioVotacao.paraVotacao(f, RELOGIO, "organizador");

            assertEquals("Maioria qualificada: quórum de 50% dos aptos e pelo menos 66,7% dos votos válidos.",
                    votacao.regra().descricao());
            assertEquals(new ResultadoApuracao.Eleito("Sim"),
                    votacao.regra().apurar(new ContagemVotos(Map.of("Sim", 2, "Não", 1), 3)));
        }

        @Test
        @DisplayName("pré-visualização da qualificada com valores inválidos mostra um aviso")
        void preVisualizacaoInvalida() {
            FormularioVotacao f = valido();
            f.setTipoRegra(TipoRegra.QUALIFICADA);
            f.setQuorum("abc");

            assertEquals("Maioria qualificada: informe quórum e fração válidos para ver a regra.",
                    ConversorFormularioVotacao.descricaoRegra(f));
        }

        @Test
        @DisplayName("sem regra escolhida, a pré-visualização usa a maioria absoluta")
        void preVisualizacaoSemRegra() {
            FormularioVotacao f = valido();
            f.setTipoRegra(null);

            assertTrue(ConversorFormularioVotacao.descricaoRegra(f).startsWith("Maioria absoluta"));
        }
    }
}

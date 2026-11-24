package br.unicamp.var.web.organizador;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.unicamp.var.apuracao.MaioriaAbsoluta;
import br.unicamp.var.apuracao.MaioriaQualificada;
import br.unicamp.var.apuracao.MaioriaSimples;
import br.unicamp.var.apuracao2turno.ResultadoApuracao;
import br.unicamp.var.auditoria.EventoAuditoria;
import br.unicamp.var.auditoria.TipoEvento;
import br.unicamp.var.processo.Votacao;
import br.unicamp.var.urna.Sigilo;
import br.unicamp.var.web.comum.ContasUsuariosEmMemoria;
import br.unicamp.var.web.comum.RepositorioPremiacoesEmMemoria;
import br.unicamp.var.web.comum.RepositorioVotacoesEmMemoria;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Funcionalidades da Fase 3 no serviço, sem Spring: presença ao vivo, alterar regra no rascunho
 * (RNF-07), duplicar votação, convite e a configuração da regra.
 */
class PolimentoGestaoTest {

    private static final Clock RELOGIO = Clock.fixed(Instant.parse("2027-02-20T12:00:00Z"), ZoneOffset.UTC);
    private static final String ORGANIZADOR = "organizador";

    private RepositorioVotacoesEmMemoria votacoes;
    private GestaoVotacoes gestao;

    @BeforeEach
    void preparar() {
        votacoes = new RepositorioVotacoesEmMemoria();
        gestao = new GestaoVotacoes(votacoes, new RepositorioPremiacoesEmMemoria(RELOGIO),
                new ContasUsuariosEmMemoria(new BCryptPasswordEncoder(4), false), RELOGIO);
    }

    private static FormularioVotacao formulario() {
        FormularioVotacao f = new FormularioVotacao();
        f.setPergunta("Aprovar a reforma?");
        f.setDescricao("Assembleia geral");
        f.setOpcoes("Sim\nNão");
        f.setEleitores("e1;Ana\ne2;Bia\ne3;Caio");
        return f;
    }

    private String criar(FormularioVotacao f) {
        return gestao.criar(f, ORGANIZADOR).id();
    }

    private Votacao votacao(String id) {
        return votacoes.buscar(id).orElseThrow();
    }

    private static FormularioVotacao qualificada(String quorum, String fracao) {
        FormularioVotacao f = new FormularioVotacao();
        f.setTipoRegra(TipoRegra.QUALIFICADA);
        f.setQuorum(quorum);
        f.setFracaoExigida(fracao);
        return f;
    }

    @Nested
    @DisplayName("Presença ao vivo")
    class Presenca {

        @Test
        @DisplayName("devolve só os ids de quem votou, na ordem da lista de aptos")
        void soIds() {
            String id = criar(formulario());
            gestao.abrir(id, ORGANIZADOR);
            votacao(id).registrarVoto("e3", "Sim");
            votacao(id).registrarVoto("e1", "Não");

            assertEquals(List.of("e1", "e3"), gestao.quemVotou(id));
        }

        @Test
        @DisplayName("votação inexistente lança VotacaoNaoEncontradaException")
        void inexistente() {
            assertThrows(VotacaoNaoEncontradaException.class, () -> gestao.quemVotou("x"));
        }
    }

    @Nested
    @DisplayName("Alterar regra no rascunho (#21, RNF-07)")
    class AlterarRegra {

        @Test
        @DisplayName("troca a regra, registra na auditoria e o formulário passa a mostrar a nova")
        void trocaERegistra() {
            String id = criar(formulario());

            assertEquals(List.of(), gestao.alterarRegra(id, qualificada("50%", "2/3"), "ana.org"));

            Votacao votacao = votacao(id);
            assertEquals("Maioria qualificada: quórum de 50% dos aptos e pelo menos 66,7% dos votos válidos.",
                    votacao.regra().descricao());
            EventoAuditoria ultimo = votacao.auditoria().get(votacao.auditoria().size() - 1);
            assertEquals(TipoEvento.REGRA_ALTERADA, ultimo.tipo());
            assertEquals("ana.org", ultimo.autor());
            FormularioVotacao atual = gestao.formularioRegra(id);
            assertEquals(TipoRegra.QUALIFICADA, atual.getTipoRegra());
            assertEquals("2/3", atual.getFracaoExigida());
        }

        @Test
        @DisplayName("campos inválidos devolvem erros e não mudam nada")
        void camposInvalidos() {
            String id = criar(formulario());
            int eventos = votacao(id).auditoria().size();

            List<ErroCampo> erros = gestao.alterarRegra(id, qualificada("abc", "0"), ORGANIZADOR);

            assertEquals(List.of("quorum", "fracaoExigida"), erros.stream().map(ErroCampo::campo).toList());
            assertTrue(votacao(id).regra().descricao().startsWith("Maioria absoluta"));
            assertEquals(eventos, votacao(id).auditoria().size());
        }

        @Test
        @DisplayName("com a votação aberta, recusa com mensagem amigável")
        void abertaRecusa() {
            String id = criar(formulario());
            gestao.abrir(id, ORGANIZADOR);
            FormularioVotacao simples = new FormularioVotacao();
            simples.setTipoRegra(TipoRegra.SIMPLES);

            OperacaoRecusadaException erro = assertThrows(OperacaoRecusadaException.class,
                    () -> gestao.alterarRegra(id, simples, ORGANIZADOR));

            assertEquals("Esta ação não é permitida com a votação aberta.", erro.getMessage());
        }
    }

    @Nested
    @DisplayName("Duplicar votação")
    class Duplicar {

        @Test
        @DisplayName("copia pergunta com (cópia), descrição, opções, regra, sigilo e eleitores, sem período")
        void copiaSemPeriodo() {
            FormularioVotacao original = formulario();
            original.setTipoRegra(TipoRegra.ABSOLUTA);
            original.setSegundoTurno(false);
            original.setSigilo(Sigilo.IDENTIFICADO);
            original.setInicio(LocalDateTime.of(2027, 3, 1, 8, 0));
            original.setFim(LocalDateTime.of(2027, 3, 1, 18, 0));
            String id = criar(original);

            FormularioVotacao copia = gestao.duplicar(id);

            assertEquals("Aprovar a reforma? (cópia)", copia.getPergunta());
            assertEquals("Assembleia geral", copia.getDescricao());
            assertEquals("Sim\nNão", copia.getOpcoes());
            assertEquals(TipoRegra.ABSOLUTA, copia.getTipoRegra());
            assertFalse(copia.isSegundoTurno());
            assertEquals(Sigilo.IDENTIFICADO, copia.getSigilo());
            assertEquals("e1;Ana\ne2;Bia\ne3;Caio", copia.getEleitores());
            assertNull(copia.getInicio());
            assertNull(copia.getFim());
            assertEquals(List.of(), ConversorFormularioVotacao.validar(copia));
        }

        @Test
        @DisplayName("a cópia usa a regra alterada no rascunho, como foi digitada")
        void usaRegraAlterada() {
            String id = criar(formulario());
            gestao.alterarRegra(id, qualificada("1/3", "2/3"), ORGANIZADOR);

            assertEquals("1/3", gestao.duplicar(id).getQuorum());
        }
    }

    @Test
    @DisplayName("votação criada fora do formulário (como o 2º turno) é duplicada com a regra deduzida da descrição")
    void duplicaSegundoTurno() {
        String id = criar(formulario());
        Votacao avulsa = Votacao.nova().pergunta("Avulsa").opcoes(List.of("A", "B"))
                .regra(new MaioriaQualificada(0.5, 2.0 / 3)).eleitorado(votacao(id).eleitorado()).build();
        votacoes.salvar(avulsa);

        FormularioVotacao copia = gestao.duplicar(avulsa.id());

        assertEquals(List.of(TipoRegra.QUALIFICADA, "50%", "2/3"),
                List.of(copia.getTipoRegra(), copia.getQuorum(), copia.getFracaoExigida()));
    }

    @Nested
    @DisplayName("Configuração da regra deduzida da descrição")
    class Deduzir {

        @Test
        @DisplayName("maioria simples")
        void simples() {
            assertEquals(TipoRegra.SIMPLES, ConfiguracaoRegra.deduzir(new MaioriaSimples()).tipo());
        }

        @Test
        @DisplayName("maioria absoluta com e sem 2º turno")
        void absoluta() {
            assertTrue(ConfiguracaoRegra.deduzir(new MaioriaAbsoluta(true)).segundoTurno());
            assertFalse(ConfiguracaoRegra.deduzir(new MaioriaAbsoluta(false)).segundoTurno());
            assertEquals(TipoRegra.ABSOLUTA, ConfiguracaoRegra.deduzir(new MaioriaAbsoluta(false)).tipo());
        }

        @Test
        @DisplayName("maioria qualificada: 66,7% volta a ser 2/3 e 33,3% volta a ser 1/3; o resto fica em %")
        void qualificada() {
            ConfiguracaoRegra doisTercos = ConfiguracaoRegra.deduzir(new MaioriaQualificada(1.0 / 3, 2.0 / 3));
            ConfiguracaoRegra percentuais = ConfiguracaoRegra.deduzir(new MaioriaQualificada(0.5, 0.605));

            assertEquals(List.of(TipoRegra.QUALIFICADA, "1/3", "2/3"),
                    List.of(doisTercos.tipo(), doisTercos.quorum(), doisTercos.fracaoExigida()));
            assertEquals(List.of("50%", "60,5%"), List.of(percentuais.quorum(), percentuais.fracaoExigida()));
        }

        @Test
        @DisplayName("regra personalizada vira maioria simples")
        void personalizada() {
            assertEquals(TipoRegra.SIMPLES,
                    ConfiguracaoRegra.deduzir(contagem -> new ResultadoApuracao.SemVencedor()).tipo());
        }
    }

    @Nested
    @DisplayName("Convite")
    class Convite {

        @Test
        @DisplayName("com período, traz o prazo no horário de Brasília")
        void comPrazo() {
            ConviteVotacao convite = ConviteVotacao.de("Eleição 2027", Instant.parse("2027-03-01T21:30:00Z"),
                    "http://localhost:8080/eleitor/votacoes/abc");

            assertEquals("http://localhost:8080/eleitor/votacoes/abc", convite.link());
            assertEquals("🗳️ *Eleição 2027*\nVote até 01/03/2027 às 18:30 (horário de Brasília).\n"
                    + "Cédula: http://localhost:8080/eleitor/votacoes/abc\n"
                    + "Entre com seu login e código de acesso.", convite.texto());
        }

        @Test
        @DisplayName("sem período, pede para votar enquanto estiver aberta")
        void semPrazo() {
            assertTrue(ConviteVotacao.de("Enquete", null, "http://x/eleitor/votacoes/1").texto()
                    .contains("Vote enquanto a votação estiver aberta."));
        }
    }
}

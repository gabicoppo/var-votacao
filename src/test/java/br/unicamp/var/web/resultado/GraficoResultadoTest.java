package br.unicamp.var.web.resultado;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import br.unicamp.var.apuracao.ContagemVotos;
import br.unicamp.var.apuracao.MaioriaAbsoluta;
import br.unicamp.var.apuracao.MaioriaQualificada;
import br.unicamp.var.apuracao.MaioriaSimples;
import br.unicamp.var.apuracao.RegraApuracao;
import br.unicamp.var.apuracao2turno.ResultadoApuracao;
import br.unicamp.var.resultado.ResultadoDetalhado;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Teste de {@link GraficoResultado} por classes de equivalência e valores limite (história #29).
 *
 * <pre>
 * | ID   | Entrada                | Classe                                                        |
 * |------|------------------------|---------------------------------------------------------------|
 * | CE1  | apuracao               | Eleito → faixa verde, barra vencedora                          |
 * | CE2  | apuracao               | SegundoTurno → faixa amarela, duas finalistas                  |
 * | CE3  | apuracao               | SemVencedor por empate no 1º lugar (2 ou mais opções)          |
 * | CE4  | apuracao               | SemVencedor por fração exigida não alcançada                   |
 * | CE5  | apuracao               | SemVencedor sem nenhum voto válido                             |
 * | CE6  | apuracao               | QuorumNaoAtingido → faixa vermelha                             |
 * | CE7  | regra                  | informada (explicação termina com a regra) ou nula (sem regra) |
 * | CE8  | detalhado ou apuracao  | nulo → IllegalArgumentException                                |
 * | VL1  | percentual / largura   | 0 votos → 0% e largura 0; todos os votos → 100%                |
 * | VL2  | votos                  | 1 voto (singular) e 2 votos (plural)                           |
 * </pre>
 */
class GraficoResultadoTest {

    private static final RegraApuracao QUALIFICADA = new MaioriaQualificada(0.5, 2.0 / 3);
    private static final String DESCRICAO_QUALIFICADA =
            " Maioria qualificada: quórum de 50% dos aptos e pelo menos 66,7% dos votos válidos.";

    private static Map<String, Integer> votos(Object... pares) {
        Map<String, Integer> mapa = new LinkedHashMap<>();
        for (int i = 0; i < pares.length; i += 2) {
            mapa.put((String) pares[i], (Integer) pares[i + 1]);
        }
        return mapa;
    }

    private static GraficoResultado apurar(RegraApuracao regra, int aptos, Object... pares) {
        ContagemVotos contagem = new ContagemVotos(votos(pares), aptos);
        ResultadoApuracao apuracao = regra.apurar(contagem);
        return GraficoResultado.de(ResultadoDetalhado.de(contagem, apuracao), apuracao, regra);
    }

    private static List<Destaque> destaques(GraficoResultado grafico) {
        return grafico.barras().stream().map(BarraResultado::destaque).toList();
    }

    private static List<Integer> colocacoes(GraficoResultado grafico) {
        return grafico.barras().stream().map(BarraResultado::colocacao).toList();
    }

    @Nested
    @DisplayName("Situação e faixa")
    class Situacao {

        @Test
        @DisplayName("CE1: eleito destaca a vencedora e explica com os votos e a regra")
        void eleito() {
            GraficoResultado grafico = apurar(QUALIFICADA, 9, "Aprovar", 7, "Rejeitar", 2);

            assertEquals(TipoSituacao.ELEITO, grafico.tipoSituacao());
            assertEquals("situacao-eleito", grafico.tipoSituacao().classeCss());
            assertEquals("Aprovar venceu", grafico.titulo());
            assertEquals("Aprovar recebeu 7 de 9 votos válidos (77,8%)." + DESCRICAO_QUALIFICADA,
                    grafico.explicacao());
            assertEquals(List.of(
                    new BarraResultado("Aprovar", 7, "77,8%", 100, Destaque.VENCEDOR, 1),
                    new BarraResultado("Rejeitar", 2, "22,2%", 200.0 / 7, Destaque.NENHUM, 2)), grafico.barras());
            assertEquals(9, grafico.votantes());
            assertEquals(9, grafico.aptos());
            assertEquals("100%", grafico.participacaoTexto());
        }

        @Test
        @DisplayName("CE2: 2º turno destaca as duas finalistas e cita os percentuais delas")
        void segundoTurno() {
            GraficoResultado grafico = apurar(new MaioriaAbsoluta(true), 12, "A", 4, "B", 3, "C", 2);

            assertEquals(TipoSituacao.SEGUNDO_TURNO, grafico.tipoSituacao());
            assertEquals("situacao-segundo-turno", grafico.tipoSituacao().classeCss());
            assertEquals("2º turno entre A e B", grafico.titulo());
            assertEquals("Nenhuma opção passou da metade dos votos válidos. A (44,4%) e B (33,3%) foram as mais "
                    + "votadas e disputam o 2º turno.", grafico.explicacao());
            assertEquals(List.of(Destaque.FINALISTA, Destaque.FINALISTA, Destaque.NENHUM), destaques(grafico));
            assertEquals("75%", grafico.participacaoTexto());
        }

        @Test
        @DisplayName("CE3: empate de duas opções no 1º lugar divide a colocação")
        void empateDeDuas() {
            GraficoResultado grafico = apurar(new MaioriaSimples(), 10, "A", 3, "B", 3, "C", 1);

            assertEquals(TipoSituacao.SEM_VENCEDOR, grafico.tipoSituacao());
            assertEquals("Sem vencedor", grafico.titulo());
            assertEquals("A e B empataram em primeiro lugar, com 3 votos cada. Maioria simples: vence a opção mais "
                    + "votada; empate fica sem vencedor.", grafico.explicacao());
            assertEquals(List.of(1, 1, 3), colocacoes(grafico));
            assertEquals(List.of(Destaque.NENHUM, Destaque.NENHUM, Destaque.NENHUM), destaques(grafico));
        }

        @Test
        @DisplayName("CE3: empate de três opções lista todas")
        void empateDeTres() {
            GraficoResultado grafico = apurar(new MaioriaSimples(), 6, "A", 1, "B", 1, "C", 1);

            assertEquals("A, B e C empataram em primeiro lugar, com 1 voto cada. Maioria simples: vence a opção "
                    + "mais votada; empate fica sem vencedor.", grafico.explicacao());
            assertEquals(List.of(1, 1, 1), colocacoes(grafico));
        }

        @Test
        @DisplayName("CE4: a mais votada sem a fração exigida fica sem vencedor")
        void fracaoNaoAlcancada() {
            GraficoResultado grafico = apurar(QUALIFICADA, 9, "Aprovar", 5, "Rejeitar", 4);

            assertEquals(TipoSituacao.SEM_VENCEDOR, grafico.tipoSituacao());
            assertEquals("Aprovar foi a mais votada, com 55,6% dos votos válidos, mas não alcançou o mínimo exigido."
                    + DESCRICAO_QUALIFICADA, grafico.explicacao());
        }

        @Test
        @DisplayName("CE5 e VL1: sem votos válidos, barras com 0% e largura 0")
        void semVotos() {
            GraficoResultado grafico = apurar(new MaioriaSimples(), 5, "Sim", 0, "Não", 0);

            assertEquals(TipoSituacao.SEM_VENCEDOR, grafico.tipoSituacao());
            assertEquals("Nenhum voto válido foi registrado. Maioria simples: vence a opção mais votada; empate "
                    + "fica sem vencedor.", grafico.explicacao());
            assertEquals(List.of(
                    new BarraResultado("Sim", 0, "0%", 0, Destaque.NENHUM, 1),
                    new BarraResultado("Não", 0, "0%", 0, Destaque.NENHUM, 1)), grafico.barras());
            assertEquals("0%", grafico.participacaoTexto());
        }

        @Test
        @DisplayName("CE6: quórum não atingido mostra votantes, aptos e participação")
        void quorumNaoAtingido() {
            GraficoResultado grafico = apurar(QUALIFICADA, 10, "Aprovar", 2, "Rejeitar", 1);

            assertEquals(TipoSituacao.QUORUM_NAO_ATINGIDO, grafico.tipoSituacao());
            assertEquals("situacao-quorum-nao-atingido", grafico.tipoSituacao().classeCss());
            assertEquals("Quórum não atingido", grafico.titulo());
            assertEquals("Votaram 3 de 10 aptos (30%), abaixo do quórum mínimo." + DESCRICAO_QUALIFICADA,
                    grafico.explicacao());
            assertEquals("situacao-sem-vencedor", TipoSituacao.SEM_VENCEDOR.classeCss());
        }
    }

    @Nested
    @DisplayName("Regra e entradas")
    class Entradas {

        @Test
        @DisplayName("CE7: sem a regra, a explicação fica só com os fatos")
        void semRegra() {
            ContagemVotos contagem = new ContagemVotos(votos("A", 2, "B", 1), 3);
            ResultadoApuracao apuracao = new MaioriaSimples().apurar(contagem);

            GraficoResultado grafico = GraficoResultado.de(ResultadoDetalhado.de(contagem, apuracao), apuracao);

            assertEquals("A recebeu 2 de 3 votos válidos (66,7%).", grafico.explicacao());
        }

        @Test
        @DisplayName("VL2: um único voto válido fica no singular")
        void umVoto() {
            GraficoResultado grafico = apurar(new MaioriaSimples(), 4, "A", 1, "B", 0);

            assertEquals("A recebeu 1 de 1 voto válido (100%). Maioria simples: vence a opção mais votada; empate "
                    + "fica sem vencedor.", grafico.explicacao());
            assertEquals("1 voto", grafico.barras().get(0).votosTexto());
            assertEquals("0 votos", grafico.barras().get(1).votosTexto());
            assertEquals("25%", grafico.participacaoTexto());
        }

        @Test
        @DisplayName("Opção vencedora ou finalista fora da contagem não quebra a explicação")
        void opcaoForaDaContagem() {
            ContagemVotos contagem = new ContagemVotos(votos("A", 2, "B", 1), 3);
            ResultadoApuracao eleito = new ResultadoApuracao.Eleito("Z");
            ResultadoApuracao turno = new ResultadoApuracao.SegundoTurno("Z", "A");

            assertEquals("Z venceu pela regra de apuração.",
                    GraficoResultado.de(ResultadoDetalhado.de(contagem, eleito), eleito).explicacao());
            assertEquals("Nenhuma opção passou da metade dos votos válidos. Z e A (66,7%) foram as mais votadas e "
                    + "disputam o 2º turno.", GraficoResultado.de(ResultadoDetalhado.de(contagem, turno), turno)
                    .explicacao());
        }

        @Test
        @DisplayName("CE8: detalhado ou apuração nulos são rejeitados")
        void rejeitaNulos() {
            ContagemVotos contagem = new ContagemVotos(votos("A", 1), 1);
            ResultadoApuracao apuracao = new MaioriaSimples().apurar(contagem);
            ResultadoDetalhado detalhado = ResultadoDetalhado.de(contagem, apuracao);

            assertThrows(IllegalArgumentException.class, () -> GraficoResultado.de(null, apuracao));
            assertThrows(IllegalArgumentException.class, () -> GraficoResultado.de(detalhado, null));
        }

        @Test
        @DisplayName("As barras são imutáveis")
        void barrasImutaveis() {
            GraficoResultado grafico = apurar(new MaioriaSimples(), 3, "A", 2, "B", 1);

            assertThrows(UnsupportedOperationException.class, () -> grafico.barras().clear());
        }

        @ParameterizedTest
        @CsvSource({"0, 0%", "50, 50%", "66.6666, '66,7%'", "100, 100%", "12.25, '12,3%'"})
        @DisplayName("VL1: percentual com uma casa decimal, vírgula e sem ',0'")
        void formataPercentual(double valor, String esperado) {
            assertEquals(esperado, GraficoResultado.percentual(valor));
        }
    }
}

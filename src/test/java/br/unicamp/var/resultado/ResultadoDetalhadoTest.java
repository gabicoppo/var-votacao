package br.unicamp.var.resultado;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import br.unicamp.var.apuracao.ContagemVotos;
import br.unicamp.var.apuracao.MaioriaAbsoluta;
import br.unicamp.var.apuracao.MaioriaQualificada;
import br.unicamp.var.apuracao.MaioriaSimples;
import br.unicamp.var.apuracao2turno.ResultadoApuracao;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Testes de {@link ResultadoDetalhado}.
 *
 * <p>O teste do cenário principal veio com a funcionalidade (#49). Os grupos
 * {@code @Nested} foram criados depois, por teste estrutural (Atividade 5, C3,
 * issue #51): cada um cobre lacunas que o relatório do JaCoCo apontou no commit
 * {@code 7421728}, quando o pacote tinha 27 de 42 linhas e 4 de 12 branches
 * cobertos.</p>
 *
 * <pre>
 * | ID | Método     | Linhas       | Lacuna (o que nunca era executado)          | Grupo       |
 * |----|------------|--------------|---------------------------------------------|-------------|
 * | L1 | de         | 52-53        | contagem == null: ramo verdadeiro e throw   | Validacao   |
 * | L2 | de         | 55-56        | apuracao == null: ramo verdadeiro e throw   | Validacao   |
 * | L3 | formatar   | 122-130      | método inteiro, com os 2 ramos do for       | Formatacao  |
 * | L4 | percentual | 134-135      | total == 0: ramo do percentual 0            | Percentuais |
 * | L5 | descrever  | 141, 143-144 | caso SegundoTurno do switch                 | Situacao    |
 * | L6 | descrever  | 141, 145     | caso SemVencedor do switch                  | Situacao    |
 * | L7 | descrever  | 141, 146     | caso QuorumNaoAtingido do switch            | Situacao    |
 * </pre>
 */
class ResultadoDetalhadoTest {

    private static final double TOLERANCIA = 0.001;

    @Test
    void mostraVotosPercentuaisTotaisESituacaoDeUmaVotacaoComEleito() {
        Map<String, Integer> votos = new LinkedHashMap<>();
        votos.put("Juliana", 20);
        votos.put("Gabriela", 50);
        votos.put("Fabricio", 30);
        ContagemVotos contagem = new ContagemVotos(votos, 200);
        ResultadoApuracao apuracao = new MaioriaSimples().apurar(contagem);

        ResultadoDetalhado resultado = ResultadoDetalhado.de(contagem, apuracao);

        List<LinhaResultado> linhas = resultado.linhas();
        assertEquals(List.of("Gabriela", "Fabricio", "Juliana"), linhas.stream().map(LinhaResultado::opcao).toList());
        assertEquals(List.of(50, 30, 20), linhas.stream().map(LinhaResultado::votos).toList());
        assertEquals(50.0, linhas.get(0).percentual(), TOLERANCIA);
        assertEquals(30.0, linhas.get(1).percentual(), TOLERANCIA);
        assertEquals(20.0, linhas.get(2).percentual(), TOLERANCIA);
        assertEquals(100, resultado.totalVotantes());
        assertEquals(200, resultado.totalAptos());
        assertEquals(50.0, resultado.participacao(), TOLERANCIA);
        assertEquals("Eleito: Gabriela", resultado.situacao());
    }

    private static ContagemVotos contagem(int totalAptos, Object... opcoesEVotos) {
        Map<String, Integer> votos = new LinkedHashMap<>();
        for (int i = 0; i < opcoesEVotos.length; i += 2) {
            votos.put((String) opcoesEVotos[i], (Integer) opcoesEVotos[i + 1]);
        }
        return new ContagemVotos(votos, totalAptos);
    }

    @Nested
    @DisplayName("Validação dos argumentos de de()")
    class Validacao {

        @Test
        @DisplayName("L1: contagem nula é rejeitada")
        void l1RejeitaContagemNula() {
            IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                    () -> ResultadoDetalhado.de(null, new ResultadoApuracao.Eleito("Gabriela")));

            assertEquals("contagem nao pode ser nula", erro.getMessage());
        }

        @Test
        @DisplayName("L2: apuração nula é rejeitada")
        void l2RejeitaApuracaoNula() {
            ContagemVotos contagem = contagem(100, "Gabriela", 40, "Fabricio", 30);

            IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                    () -> ResultadoDetalhado.de(contagem, null));

            assertEquals("apuracao nao pode ser nula", erro.getMessage());
        }
    }

    @Nested
    @DisplayName("Percentuais sem votos válidos")
    class Percentuais {

        @Test
        @DisplayName("L4: com zero votos válidos, os percentuais são 0 e não há divisão por zero")
        void l4SemVotosValidosTodosOsPercentuaisSaoZero() {
            ContagemVotos contagem = contagem(100, "Gabriela", 0, "Fabricio", 0);

            ResultadoDetalhado resultado = ResultadoDetalhado.de(contagem, new MaioriaSimples().apurar(contagem));

            assertEquals(0, resultado.totalVotantes());
            assertEquals(0.0, resultado.linhas().get(0).percentual(), TOLERANCIA);
            assertEquals(0.0, resultado.linhas().get(1).percentual(), TOLERANCIA);
            assertEquals(0.0, resultado.participacao(), TOLERANCIA);
        }
    }

    @Nested
    @DisplayName("Situação para cada caso de ResultadoApuracao")
    class Situacao {

        @Test
        @DisplayName("L5: segundo turno mostra os dois mais votados")
        void l5SegundoTurnoMostraOsDoisColocados() {
            ContagemVotos contagem = contagem(100, "Juliana", 25, "Gabriela", 40, "Fabricio", 35);

            ResultadoDetalhado resultado =
                    ResultadoDetalhado.de(contagem, new MaioriaAbsoluta(true).apurar(contagem));

            assertEquals("2º turno entre Gabriela e Fabricio", resultado.situacao());
        }

        @Test
        @DisplayName("L6: empate no primeiro lugar aparece como sem vencedor")
        void l6EmpateNoPrimeiroLugarFicaSemVencedor() {
            ContagemVotos contagem = contagem(100, "Gabriela", 40, "Fabricio", 40);

            ResultadoDetalhado resultado = ResultadoDetalhado.de(contagem, new MaioriaSimples().apurar(contagem));

            assertEquals("Sem vencedor", resultado.situacao());
        }

        @Test
        @DisplayName("L7: votantes abaixo do quórum aparecem como quórum não atingido")
        void l7VotantesAbaixoDoQuorumFicamSemQuorum() {
            ContagemVotos contagem = contagem(100, "Sim", 25, "Não", 5);

            ResultadoDetalhado resultado =
                    ResultadoDetalhado.de(contagem, new MaioriaQualificada(0.5, 2.0 / 3).apurar(contagem));

            assertEquals("Quórum não atingido", resultado.situacao());
            assertEquals(30.0, resultado.participacao(), TOLERANCIA);
        }
    }

    @Nested
    @DisplayName("Quadro em texto de formatar()")
    class Formatacao {

        @Test
        @DisplayName("L3: quadro traz linhas, totais e situação, com uma casa decimal e vírgula")
        void l3QuadroTrazLinhasTotaisESituacao() {
            ContagemVotos contagem = contagem(18, "Fabricio", 2, "Gabriela", 4);

            ResultadoDetalhado resultado = ResultadoDetalhado.de(contagem, new MaioriaSimples().apurar(contagem));

            String esperado = String.join("\n",
                    "Resultado da votação",
                    "Gabriela: 4 votos (66,7%)",
                    "Fabricio: 2 votos (33,3%)",
                    "Votantes: 6 de 18 aptos (participação de 33,3%)",
                    "Situação: Eleito: Gabriela");
            assertEquals(esperado, resultado.formatar());
        }
    }
}

package br.unicamp.var.web.premiacao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.unicamp.var.top3.CategoriaEleitor;
import br.unicamp.var.top3.CedulaTop3;
import br.unicamp.var.top3.PremiacaoTop3;
import br.unicamp.var.top3.RankingTop3;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Teste de {@link Podio} por classes de equivalência e valores limite (história #30).
 *
 * <pre>
 * | ID   | Entrada            | Classe                                                      |
 * |------|--------------------|-------------------------------------------------------------|
 * | CE1  | colocações         | 1, 2, 3 distintas → três degraus com um nome cada            |
 * | CE2  | colocações         | empate no 1º (1, 1, 3) → ouro com dois nomes, sem prata      |
 * | CE3  | colocações         | empate no 2º (1, 2, 2, 4) → ouro e prata com dois nomes      |
 * | CE4  | colocações         | todos empatados → um só degrau (menos de 3 colocações)       |
 * | CE5  | cédulas            | nenhuma → pódio vazio                                        |
 * | CE6  | ranking            | nulo → IllegalArgumentException                              |
 * | VL1  | pontos             | 1 → "1 pt"; fracionário → uma casa; inteiro → sem ",0"       |
 * </pre>
 */
class PodioTest {

    private static final String GRUPO = "Torcedores";

    private static RankingTop3 apurar(double peso, List<String> candidatos, List<CedulaTop3> cedulas) {
        Map<String, String> eleitores = new LinkedHashMap<>();
        for (int i = 0; i < cedulas.size(); i++) {
            eleitores.put("e" + i, GRUPO);
        }
        PremiacaoTop3 premiacao = new PremiacaoTop3("Craque", "", candidatos,
                List.of(new CategoriaEleitor(GRUPO, peso)), eleitores);
        premiacao.abrir();
        for (int i = 0; i < cedulas.size(); i++) {
            premiacao.registrarCedula("e" + i, cedulas.get(i));
        }
        premiacao.encerrar();
        return premiacao.apurar();
    }

    private static List<List<String>> nomesPorDegrau(Podio podio) {
        return podio.lugares().stream()
                .map(l -> l.candidatos().stream().map(CandidatoPodio::nome).toList())
                .toList();
    }

    private static List<Integer> colocacoes(Podio podio) {
        return podio.lugares().stream().map(LugarPodio::colocacao).toList();
    }

    @Nested
    @DisplayName("Montar o pódio")
    class Montagem {

        @Test
        @DisplayName("CE1: três colocações distintas, com ouro, prata e bronze")
        void tresColocacoes() {
            Podio podio = Podio.de(apurar(1, List.of("A", "B", "C", "D"),
                    List.of(new CedulaTop3("A", "B", "C"), new CedulaTop3("A", "B", "D"))));

            assertEquals(List.of(
                    new LugarPodio(1, List.of(new CandidatoPodio("A", 10, "10 pts"))),
                    new LugarPodio(2, List.of(new CandidatoPodio("B", 6, "6 pts"))),
                    new LugarPodio(3, List.of(new CandidatoPodio("C", 1, "1 pt"), new CandidatoPodio("D", 1, "1 pt")))),
                    podio.lugares());
            assertEquals(List.of("ouro", "prata", "bronze"),
                    podio.lugares().stream().map(LugarPodio::medalha).toList());
            assertFalse(podio.vazio());
        }

        @Test
        @DisplayName("CE2: empate no 1º lugar mostra dois nomes no ouro e pula a prata")
        void empateNoPrimeiro() {
            Podio podio = Podio.de(apurar(1, List.of("A", "B", "C", "D"),
                    List.of(new CedulaTop3("A", "B", "C"), new CedulaTop3("B", "A", "D"))));

            assertEquals(List.of(1, 3), colocacoes(podio));
            assertEquals(List.of(List.of("A", "B"), List.of("C", "D")), nomesPorDegrau(podio));
            assertEquals(List.of("ouro", "bronze"), podio.lugares().stream().map(LugarPodio::medalha).toList());
        }

        @Test
        @DisplayName("CE3: empate no 2º lugar deixa a 4ª colocação fora do pódio")
        void empateNoSegundo() {
            Podio podio = Podio.de(apurar(1, List.of("A", "B", "C", "D"),
                    List.of(new CedulaTop3("A", "B", "C"), new CedulaTop3("A", "C", "B"))));

            assertEquals(List.of(1, 2), colocacoes(podio));
            assertEquals(List.of(List.of("A"), List.of("B", "C")), nomesPorDegrau(podio));
        }

        @Test
        @DisplayName("CE4: todos empatados formam um único degrau")
        void todosEmpatados() {
            Podio podio = Podio.de(apurar(1, List.of("A", "B", "C"), List.of(new CedulaTop3("A", "B", "C"),
                    new CedulaTop3("B", "C", "A"), new CedulaTop3("C", "A", "B"))));

            assertEquals(List.of(1), colocacoes(podio));
            assertEquals(List.of(List.of("A", "B", "C")), nomesPorDegrau(podio));
        }

        @Test
        @DisplayName("CE5: sem cédulas, o pódio fica vazio")
        void semCedulas() {
            Podio podio = Podio.de(apurar(1, List.of("A", "B", "C"), List.of()));

            assertTrue(podio.vazio());
            assertEquals(List.of(), podio.lugares());
        }

        @Test
        @DisplayName("CE6: ranking nulo é rejeitado")
        void rankingNulo() {
            assertThrows(IllegalArgumentException.class, () -> Podio.de(null));
        }

        @Test
        @DisplayName("Degraus e candidatos são imutáveis")
        void imutavel() {
            Podio podio = Podio.de(apurar(1, List.of("A", "B", "C"), List.of(new CedulaTop3("A", "B", "C"))));

            assertThrows(UnsupportedOperationException.class, () -> podio.lugares().clear());
            assertThrows(UnsupportedOperationException.class, () -> podio.lugares().getFirst().candidatos().clear());
        }
    }

    @Nested
    @DisplayName("Formatar os pontos")
    class Pontos {

        @Test
        @DisplayName("VL1: peso fracionário gera pontos com uma casa decimal")
        void pesoFracionario() {
            Podio podio = Podio.de(apurar(1.5, List.of("A", "B", "C"), List.of(new CedulaTop3("A", "B", "C"))));

            assertEquals(List.of("7,5 pts", "4,5 pts", "1,5 pts"), podio.lugares().stream()
                    .map(l -> l.candidatos().getFirst().pontosTexto()).toList());
        }

        @ParameterizedTest
        @CsvSource({"0, 0 pts", "1, 1 pt", "1.04, 1 pt", "2, 2 pts", "23.5, '23,5 pts'", "36.25, '36,3 pts'"})
        @DisplayName("VL1: singular só para 1 ponto; sem ',0' nos inteiros")
        void formata(double pontos, String esperado) {
            assertEquals(esperado, Podio.pontos(pontos));
        }
    }
}

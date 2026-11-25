package br.unicamp.var.top3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class RankingTop3Test {

    private static final String JORNALISTAS = "Jornalistas";
    private static final String TORCEDORES = "Torcedores";
    private static final List<String> CANDIDATOS = List.of("Elisa", "Davi", "Caio", "Bia", "Ana");

    private PremiacaoTop3 abrir(List<String> candidatos, List<CategoriaEleitor> grupos, Map<String, String> eleitores) {
        PremiacaoTop3 premiacao = new PremiacaoTop3("Craque", "", candidatos, grupos, eleitores);
        premiacao.abrir();
        return premiacao;
    }

    private PremiacaoTop3 abrirComGrupoUnico() {
        return abrir(CANDIDATOS, List.of(new CategoriaEleitor(TORCEDORES, 1)),
                Map.of("e1", TORCEDORES, "e2", TORCEDORES, "e3", TORCEDORES));
    }

    private void votar(PremiacaoTop3 premiacao, String id, String primeiro, String segundo, String terceiro) {
        premiacao.registrarCedula(id, new CedulaTop3(primeiro, segundo, terceiro));
    }

    private RankingTop3 encerrarEApurar(PremiacaoTop3 premiacao) {
        premiacao.encerrar();
        return premiacao.apurar();
    }

    private RankingTop3 craqueDoCampeonato() {
        PremiacaoTop3 premiacao = new PremiacaoTop3("Craque do Campeonato", "Contribuição para o time", CANDIDATOS,
                List.of(new CategoriaEleitor(JORNALISTAS, 2), new CategoriaEleitor(TORCEDORES, 1)),
                Map.of("j1", JORNALISTAS, "j2", JORNALISTAS, "j3", JORNALISTAS, "t1", TORCEDORES,
                        "t2", TORCEDORES, "t3", TORCEDORES, "t4", TORCEDORES, "t5", TORCEDORES));
        premiacao.abrir();
        votar(premiacao, "j1", "Ana", "Bia", "Caio");
        votar(premiacao, "j2", "Bia", "Ana", "Davi");
        votar(premiacao, "j3", "Ana", "Caio", "Bia");
        votar(premiacao, "t1", "Bia", "Caio", "Ana");
        votar(premiacao, "t2", "Caio", "Bia", "Davi");
        votar(premiacao, "t3", "Bia", "Ana", "Caio");
        votar(premiacao, "t4", "Davi", "Bia", "Ana");
        votar(premiacao, "t5", "Ana", "Davi", "Bia");
        return encerrarEApurar(premiacao);
    }

    @Nested
    @DisplayName("Conferir as linhas do ranking")
    class Linhas {

        @Test
        @DisplayName("Craque do Campeonato: posições, pontos dos dois grupos e candidato sem votos")
        void confereApuracaoManual() {
            RankingTop3 ranking = craqueDoCampeonato();

            assertEquals(List.of(
                    new LinhaRanking(1, "Ana", 36, 3, 2, 2, Map.of(JORNALISTAS, 26.0, TORCEDORES, 10.0)),
                    new LinhaRanking(2, "Bia", 35, 3, 3, 2, Map.of(JORNALISTAS, 18.0, TORCEDORES, 17.0)),
                    new LinhaRanking(3, "Caio", 17, 1, 2, 2, Map.of(JORNALISTAS, 8.0, TORCEDORES, 9.0)),
                    new LinhaRanking(4, "Davi", 11, 1, 1, 2, Map.of(JORNALISTAS, 2.0, TORCEDORES, 9.0)),
                    new LinhaRanking(5, "Elisa", 0, 0, 0, 0, Map.of(JORNALISTAS, 0.0, TORCEDORES, 0.0))),
                    ranking.linhas());
            assertEquals(8, ranking.totalCedulas());
            assertEquals((5 + 3 + 1) * (3 * 2 + 5), ranking.linhas().stream().mapToDouble(LinhaRanking::pontos).sum());
        }

        @Test
        @DisplayName("Premiação sem cédulas conserva todos os candidatos e grupos com zero")
        void apuraSemVotos() {
            PremiacaoTop3 premiacao = abrir(CANDIDATOS,
                    List.of(new CategoriaEleitor(JORNALISTAS, 2), new CategoriaEleitor(TORCEDORES, 1)), Map.of());
            RankingTop3 ranking = encerrarEApurar(premiacao);

            assertEquals(0, ranking.totalCedulas());
            assertEquals(List.of("Ana", "Bia", "Caio", "Davi", "Elisa"),
                    ranking.linhas().stream().map(LinhaRanking::candidato).toList());
            for (LinhaRanking linha : ranking.linhas()) {
                assertEquals(new LinhaRanking(1, linha.candidato(), 0, 0, 0, 0,
                        Map.of(JORNALISTAS, 0.0, TORCEDORES, 0.0)), linha);
            }
        }

        @Test
        @DisplayName("Listas e mapas do resultado são imutáveis")
        void protegeResultado() {
            RankingTop3 ranking = craqueDoCampeonato();
            Map<String, Double> pontos = new HashMap<>(Map.of(TORCEDORES, 3.0));
            LinhaRanking linha = new LinhaRanking(1, "Ana", 3, 0, 1, 0, pontos);
            pontos.clear();
            assertEquals(Map.of(TORCEDORES, 3.0), linha.pontosPorCategoria());
            assertThrows(UnsupportedOperationException.class, () -> ranking.linhas().clear());
            assertThrows(UnsupportedOperationException.class, () -> ranking.criterios().clear());
            assertThrows(UnsupportedOperationException.class, () -> ranking.linhas().getFirst().pontosPorCategoria()
                    .clear());
        }

        @Test
        @DisplayName("Recusa publicar uma soma que exceda o limite finito de double")
        void rejeitaEstouroDePontuacao() {
            PremiacaoTop3 premiacao = abrir(CANDIDATOS, List.of(new CategoriaEleitor(TORCEDORES, Double.MAX_VALUE)),
                    Map.of("e1", TORCEDORES));
            votar(premiacao, "e1", "Ana", "Bia", "Caio");
            premiacao.encerrar();

            assertThrows(IllegalStateException.class, premiacao::apurar);
            assertEquals(StatusPremiacao.ENCERRADA, premiacao.status());
        }
    }

    @Nested
    @DisplayName("Desempatar candidatos")
    class Desempate {

        @Test
        @DisplayName("Em seis pontos, um primeiro lugar supera dois segundos")
        void preferePrimeirosLugares() {
            PremiacaoTop3 premiacao = abrirComGrupoUnico();
            votar(premiacao, "e1", "Bia", "Ana", "Caio");
            votar(premiacao, "e2", "Davi", "Ana", "Bia");
            RankingTop3 ranking = encerrarEApurar(premiacao);

            assertEquals(new LinhaRanking(1, "Bia", 6, 1, 0, 1, Map.of(TORCEDORES, 6.0)), ranking.linhas().get(0));
            assertEquals(new LinhaRanking(2, "Ana", 6, 0, 2, 0, Map.of(TORCEDORES, 6.0)), ranking.linhas().get(1));
            assertTrue(ranking.criterios().contains("Desempate aplicado no grupo de candidatos: Bia, Ana."));
        }

        @Test
        @DisplayName("Em três pontos e sem primeiros lugares, um segundo supera três terceiros")
        void prefereSegundosLugares() {
            PremiacaoTop3 premiacao = abrirComGrupoUnico();
            votar(premiacao, "e1", "Caio", "Bia", "Ana");
            votar(premiacao, "e2", "Caio", "Davi", "Ana");
            votar(premiacao, "e3", "Davi", "Caio", "Ana");
            List<LinhaRanking> linhas = encerrarEApurar(premiacao).linhas();

            assertEquals(new LinhaRanking(3, "Bia", 3, 0, 1, 0, Map.of(TORCEDORES, 3.0)), linhas.get(2));
            assertEquals(new LinhaRanking(4, "Ana", 3, 0, 0, 3, Map.of(TORCEDORES, 3.0)), linhas.get(3));
        }

        @Test
        @DisplayName("Empate esportivo divide o segundo lugar e a colocação seguinte é quarta")
        void compartilhaColocacao() {
            PremiacaoTop3 premiacao = abrirComGrupoUnico();
            votar(premiacao, "e1", "Ana", "Caio", "Bia");
            votar(premiacao, "e2", "Ana", "Bia", "Caio");
            List<LinhaRanking> linhas = encerrarEApurar(premiacao).linhas();

            assertEquals(List.of(1, 2, 2, 4, 4), linhas.stream().map(LinhaRanking::colocacao).toList());
            assertEquals(List.of("Ana", "Bia", "Caio", "Davi", "Elisa"),
                    linhas.stream().map(LinhaRanking::candidato).toList());
        }

        @Test
        @DisplayName("0,1 + 0,1 + 0,1 empata com 0,3 independentemente dos terceiros lugares")
        void toleraArredondamento() {
            PremiacaoTop3 premiacao = abrir(List.of("Zulu", "Álvaro", "Bruno", "Caio"),
                    List.of(new CategoriaEleitor("G", 0.1), new CategoriaEleitor("H", 0.3)),
                    Map.of("e1", "G", "e2", "G", "e3", "G", "e4", "H"));
            for (String id : List.of("e1", "e2", "e3")) {
                votar(premiacao, id, "Caio", "Bruno", "Zulu");
            }
            votar(premiacao, "e4", "Caio", "Bruno", "Álvaro");
            List<LinhaRanking> linhas = encerrarEApurar(premiacao).linhas();

            assertEquals(List.of("Caio", "Bruno", "Álvaro", "Zulu"), linhas.stream().map(LinhaRanking::candidato)
                    .toList());
            assertNotEquals(linhas.get(2).pontos(), linhas.get(3).pontos());
            assertEquals(3, linhas.get(2).colocacao());
            assertEquals(3, linhas.get(3).colocacao());
        }

        @Test
        @DisplayName("Grafias equivalentes em português mantêm uma ordem estável no empate")
        void ordenaGrafiasEquivalentes() {
            String composto = "Éder";
            String decomposto = "E\u0301der";
            PremiacaoTop3 premiacao = abrir(List.of(composto, "Bia", decomposto),
                    List.of(new CategoriaEleitor(TORCEDORES, 1)), Map.of());

            List<LinhaRanking> linhas = encerrarEApurar(premiacao).linhas();
            assertEquals(List.of("Bia", decomposto, composto), linhas.stream().map(LinhaRanking::candidato).toList());
            assertEquals(List.of(1, 1, 1), linhas.stream().map(LinhaRanking::colocacao).toList());
        }

        @Test
        @DisplayName("A tolerância parte do maior total do bloco e não encadeia empates")
        void separaPontuacoesForaDaTolerancia() {
            List<CategoriaEleitor> grupos = List.of(new CategoriaEleitor("G", 0.4),
                    new CategoriaEleitor("H", 0.4 + 0.75e-9), new CategoriaEleitor("I", 0.4 + 1.5e-9));
            PremiacaoTop3 premiacao = abrir(CANDIDATOS, grupos, Map.of("e1", "G", "e2", "H", "e3", "I"));
            votar(premiacao, "e1", "Davi", "Elisa", "Ana");
            votar(premiacao, "e2", "Davi", "Elisa", "Bia");
            votar(premiacao, "e3", "Davi", "Elisa", "Caio");
            List<LinhaRanking> linhas = encerrarEApurar(premiacao).linhas();

            assertEquals(List.of("Davi", "Elisa", "Bia", "Caio", "Ana"),
                    linhas.stream().map(LinhaRanking::candidato).toList());
            assertEquals(List.of(1, 2, 3, 3, 5), linhas.stream().map(LinhaRanking::colocacao).toList());
        }
    }

    @Nested
    @DisplayName("Exibir os critérios")
    class Criterios {

        @Test
        @DisplayName("Descreve pesos fracionários com a precisão configurada e aceita descrição vazia")
        void preservaPesoFracionario() {
            PremiacaoTop3 premiacao = abrir(CANDIDATOS, List.of(new CategoriaEleitor(TORCEDORES, 0.125)), Map.of());
            List<String> criterios = encerrarEApurar(premiacao).criterios();

            assertEquals(List.of("1º lugar = 5 pontos", "2º lugar = 3 pontos", "3º lugar = 1 ponto",
                    "Torcedores: peso 0,125",
                    "Empate em pontos: diferença de até 0,000000001 para a maior pontuação do bloco.",
                    "Desempate: mais primeiros lugares, depois mais segundos lugares. "
                    + "Persistindo o empate, a colocação é compartilhada e os nomes aparecem em ordem alfabética."),
                    criterios);
        }
    }

    @Nested
    @DisplayName("Formatar o quadro")
    class Formatacao {

        @Test
        @DisplayName("Exibe a tabela completa, os subtotais por grupo e os critérios em pt-BR")
        void formataCraqueDoCampeonato() {
            String esperado = """
                    Craque do Campeonato
                    Colocação | Candidato | Pontos | 1º | 2º | 3º | Jornalistas | Torcedores
                    1º | Ana | 36,0 | 3 | 2 | 2 | 26,0 | 10,0
                    2º | Bia | 35,0 | 3 | 3 | 2 | 18,0 | 17,0
                    3º | Caio | 17,0 | 1 | 2 | 2 | 8,0 | 9,0
                    4º | Davi | 11,0 | 1 | 1 | 2 | 2,0 | 9,0
                    5º | Elisa | 0,0 | 0 | 0 | 0 | 0,0 | 0,0
                    Cédulas: 8
                    Critérios:
                    Critério de avaliação: Contribuição para o time
                    1º lugar = 5 pontos
                    2º lugar = 3 pontos
                    3º lugar = 1 ponto
                    Jornalistas: peso 2,0
                    Torcedores: peso 1,0
                    Empate em pontos: diferença de até 0,000000001 para a maior pontuação do bloco.
                    Desempate: mais primeiros lugares, depois mais segundos lugares. \
                    Persistindo o empate, a colocação é compartilhada e os nomes aparecem em ordem alfabética.""";

            assertEquals(esperado, craqueDoCampeonato().formatar());
        }
    }
}

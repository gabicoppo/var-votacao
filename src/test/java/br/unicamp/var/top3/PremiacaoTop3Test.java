package br.unicamp.var.top3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class PremiacaoTop3Test {

    private static final List<String> CANDIDATOS = List.of("A", "B", "C");
    private static final CategoriaEleitor PUBLICO = new CategoriaEleitor("Público", 1);
    private static final List<CategoriaEleitor> GRUPOS = List.of(PUBLICO);
    private static final Map<String, String> ELEITORES = Map.of("e1", "Público", "e2", "Público");
    private static final CedulaTop3 CEDULA = new CedulaTop3("B", "A", "C");

    private PremiacaoTop3 criar(List<String> candidatos, List<CategoriaEleitor> grupos, Map<String, String> eleitores) {
        return new PremiacaoTop3("Craque", "", candidatos, grupos, eleitores);
    }

    private PremiacaoTop3 noEstado(StatusPremiacao estado) {
        PremiacaoTop3 premiacao = criar(CANDIDATOS, GRUPOS, ELEITORES);
        if (estado != StatusPremiacao.RASCUNHO) {
            premiacao.abrir();
        }
        if (estado == StatusPremiacao.ENCERRADA || estado == StatusPremiacao.APURADA) {
            premiacao.encerrar();
        }
        if (estado == StatusPremiacao.APURADA) {
            premiacao.apurar();
        }
        return premiacao;
    }

    @Nested
    @DisplayName("Configurar a premiação")
    class Construcao {

        @Test
        @DisplayName("Começa em rascunho e preserva a configuração recebida")
        void copiaConfiguracao() {
            List<String> candidatos = new ArrayList<>(CANDIDATOS);
            List<CategoriaEleitor> grupos = new ArrayList<>(GRUPOS);
            Map<String, String> eleitores = new HashMap<>(ELEITORES);
            PremiacaoTop3 premiacao = criar(candidatos, grupos, eleitores);
            candidatos.clear();
            grupos.clear();
            eleitores.clear();

            assertEquals(StatusPremiacao.RASCUNHO, premiacao.status());
            assertEquals("Craque", premiacao.titulo());
            assertEquals("", premiacao.descricao());
            assertEquals(CANDIDATOS, premiacao.candidatos());
            assertEquals(GRUPOS, premiacao.categorias());
            assertThrows(UnsupportedOperationException.class, () -> premiacao.candidatos().clear());
            assertThrows(UnsupportedOperationException.class, () -> premiacao.categorias().clear());
            premiacao.abrir();
            premiacao.registrarCedula("e1", CEDULA);
            assertTrue(premiacao.jaVotou("e1"));
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" "})
        @DisplayName("Exige título preenchido")
        void rejeitaTitulo(String titulo) {
            assertThrows(IllegalArgumentException.class, () -> new PremiacaoTop3(titulo, "", CANDIDATOS,
                    GRUPOS, ELEITORES));
        }

        @Test
        @DisplayName("Rejeita descrição, listas ou cadastro de eleitores nulos")
        void rejeitaCamposNulos() {
            assertThrows(IllegalArgumentException.class, () -> new PremiacaoTop3("Craque", null, CANDIDATOS,
                    GRUPOS, ELEITORES));
            assertThrows(IllegalArgumentException.class, () -> criar(null, GRUPOS, ELEITORES));
            assertThrows(IllegalArgumentException.class, () -> criar(CANDIDATOS, null, ELEITORES));
            assertThrows(IllegalArgumentException.class, () -> criar(CANDIDATOS, GRUPOS, null));
        }

        @Test
        @DisplayName("Exige pelo menos três candidatos válidos e distintos")
        void rejeitaCandidatosInvalidos() {
            assertThrows(IllegalArgumentException.class, () -> criar(List.of("A", "B"), GRUPOS, ELEITORES));
            assertThrows(IllegalArgumentException.class, () -> criar(List.of("A", "B", "A"), GRUPOS, ELEITORES));
            assertThrows(IllegalArgumentException.class, () -> criar(List.of("A", "B", " "), GRUPOS, ELEITORES));
            assertThrows(IllegalArgumentException.class, () -> criar(Arrays.asList("A", null, "B"), GRUPOS, ELEITORES));
        }

        @Test
        @DisplayName("Exige grupos com nomes únicos")
        void rejeitaGruposInvalidos() {
            assertThrows(IllegalArgumentException.class, () -> criar(CANDIDATOS, List.of(), ELEITORES));
            assertThrows(IllegalArgumentException.class, () -> criar(CANDIDATOS,
                    Arrays.asList(PUBLICO, null), ELEITORES));
            assertThrows(IllegalArgumentException.class, () -> criar(CANDIDATOS,
                    List.of(PUBLICO, new CategoriaEleitor("Público", 2)), ELEITORES));
        }

        @ParameterizedTest
        @CsvSource({"e1,Imprensa", "e1,", ",Público", "' ',Público"})
        @DisplayName("Exige identificador preenchido e grupo existente para cada eleitor")
        void rejeitaEleitorInvalido(String id, String nomeGrupo) {
            Map<String, String> eleitores = new HashMap<>();
            eleitores.put(id, nomeGrupo);
            assertThrows(IllegalArgumentException.class, () -> criar(CANDIDATOS, GRUPOS, eleitores));
        }
    }

    @Nested
    @DisplayName("Abrir a votação")
    class Abertura {

        @Test
        @DisplayName("Passa de rascunho para aberta")
        void abreRascunho() {
            PremiacaoTop3 premiacao = noEstado(StatusPremiacao.RASCUNHO);
            premiacao.abrir();
            assertEquals(StatusPremiacao.ABERTA, premiacao.status());
        }

        @ParameterizedTest
        @EnumSource(value = StatusPremiacao.class, names = {"ABERTA", "ENCERRADA", "APURADA"})
        @DisplayName("Só permite abrir em rascunho")
        void recusaReabertura(StatusPremiacao estado) {
            PremiacaoTop3 premiacao = noEstado(estado);
            assertThrows(IllegalStateException.class, premiacao::abrir);
            assertEquals(estado, premiacao.status());
        }
    }

    @Nested
    @DisplayName("Registrar a cédula")
    class Registro {

        @ParameterizedTest
        @EnumSource(value = StatusPremiacao.class, names = {"RASCUNHO", "ENCERRADA", "APURADA"})
        @DisplayName("Só recebe cédula enquanto aberta")
        void exigeVotacaoAberta(StatusPremiacao estado) {
            PremiacaoTop3 premiacao = noEstado(estado);
            assertThrows(IllegalStateException.class, () -> premiacao.registrarCedula("e1", CEDULA));
            assertEquals(0, premiacao.totalCedulas());
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" ", "fora"})
        @DisplayName("Recusa eleitor sem categoria")
        void rejeitaEleitorNaoApto(String id) {
            PremiacaoTop3 premiacao = noEstado(StatusPremiacao.ABERTA);
            assertThrows(IllegalArgumentException.class, () -> premiacao.registrarCedula(id, CEDULA));
            assertFalse(premiacao.jaVotou(id));
            assertEquals(0, premiacao.totalCedulas());
        }

        @ParameterizedTest
        @CsvSource({"Z,B,C", "A,Z,C", "A,B,Z"})
        @DisplayName("Cédula inválida não consome o voto nem atribui pontos parciais")
        void permiteCorrigirCedula(String primeiro, String segundo, String terceiro) {
            PremiacaoTop3 premiacao = noEstado(StatusPremiacao.ABERTA);
            CedulaTop3 invalida = new CedulaTop3(primeiro, segundo, terceiro);
            assertThrows(IllegalArgumentException.class, () -> premiacao.registrarCedula("e1", null));
            assertThrows(IllegalArgumentException.class, () -> premiacao.registrarCedula("e1", invalida));
            assertFalse(premiacao.jaVotou("e1"));
            premiacao.registrarCedula("e1", CEDULA);
            premiacao.encerrar();
            assertEquals(List.of(5.0, 3.0, 1.0), premiacao.apurar().linhas().stream()
                    .map(LinhaRanking::pontos).toList());
        }

        @Test
        @DisplayName("Cada eleitor entrega uma única cédula")
        void rejeitaVotoDuplicado() {
            PremiacaoTop3 premiacao = noEstado(StatusPremiacao.ABERTA);
            premiacao.registrarCedula("e1", CEDULA);
            assertThrows(IllegalStateException.class, () -> premiacao.registrarCedula("e1", CEDULA));
            assertEquals(1, premiacao.totalCedulas());
            assertTrue(premiacao.jaVotou("e1"));
            assertFalse(premiacao.jaVotou("e2"));
            premiacao.registrarCedula("e2", CEDULA);
            assertEquals(2, premiacao.totalCedulas());
        }
    }

    @Nested
    @DisplayName("Encerrar a votação")
    class Encerramento {

        @Test
        @DisplayName("Passa de aberta para encerrada mesmo sem votos")
        void encerraAberta() {
            PremiacaoTop3 premiacao = noEstado(StatusPremiacao.ABERTA);
            premiacao.encerrar();
            assertEquals(StatusPremiacao.ENCERRADA, premiacao.status());
        }

        @ParameterizedTest
        @EnumSource(value = StatusPremiacao.class, names = {"RASCUNHO", "ENCERRADA", "APURADA"})
        @DisplayName("Só permite encerrar a votação aberta")
        void recusaEncerramento(StatusPremiacao estado) {
            PremiacaoTop3 premiacao = noEstado(estado);
            assertThrows(IllegalStateException.class, premiacao::encerrar);
            assertEquals(estado, premiacao.status());
        }
    }

    @Nested
    @DisplayName("Apurar os votos")
    class Apuracao {

        @ParameterizedTest
        @EnumSource(value = StatusPremiacao.class, names = {"RASCUNHO", "ABERTA"})
        @DisplayName("Exige encerramento antes de divulgar resultados")
        void exigeEncerramento(StatusPremiacao estado) {
            PremiacaoTop3 premiacao = noEstado(estado);
            assertThrows(IllegalStateException.class, premiacao::apurar);
            assertEquals(estado, premiacao.status());
        }

        @Test
        @DisplayName("Apura uma vez e devolve o mesmo ranking nas consultas seguintes")
        void apuracaoIdempotente() {
            PremiacaoTop3 premiacao = noEstado(StatusPremiacao.ENCERRADA);
            RankingTop3 ranking = premiacao.apurar();
            assertEquals(StatusPremiacao.APURADA, premiacao.status());
            assertSame(ranking, premiacao.apurar());
        }
    }
}

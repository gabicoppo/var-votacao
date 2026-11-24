package br.unicamp.var.top3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Teste funcional de {@link PremiacaoTop3#categoriaDe}, {@link PremiacaoTop3#ehApto} e
 * {@link PremiacaoTop3#totalEleitores} por particionamento em classes de equivalência (#23).
 * Entradas: {@code eleitorId} das consultas e {@code categoriaPorEleitor} do construtor.
 *
 * <pre>
 * | ID   | Entrada             | Classe de equivalência                                    | Válida?  |
 * |------|---------------------|-----------------------------------------------------------|----------|
 * | CE1  | eleitorId           | id cadastrado, escrito como na configuração               | válida   |
 * | CE2  | eleitorId           | id cadastrado com espaços nas pontas                      | válida   |
 * | CE3  | eleitorId           | null → vazio / false                                      | inválida |
 * | CE4  | eleitorId           | vazio ou só espaços → vazio / false                       | inválida |
 * | CE5  | eleitorId           | id não cadastrado, inclusive com outra caixa → vazio      | inválida |
 * | CE6  | categoriaPorEleitor | mapa vazio → 0 aptos (limite inferior)                    | válida   |
 * | CE7  | categoriaPorEleitor | n ids distintos, com ou sem espaços → n aptos             | válida   |
 * | CE8  | categoriaPorEleitor | ids iguais depois do strip → IllegalArgumentException     | inválida |
 * </pre>
 */
class PremiacaoTop3AptosClassesEquivalenciaTest {

    private static final CategoriaEleitor JORNALISTAS = new CategoriaEleitor("Jornalistas", 2);
    private static final CategoriaEleitor TORCEDORES = new CategoriaEleitor("Torcedores", 1);
    private static final List<String> CANDIDATOS = List.of("Ana", "Bia", "Caio");
    private static final CedulaTop3 CEDULA = new CedulaTop3("Ana", "Bia", "Caio");

    private static PremiacaoTop3 criar(Map<String, String> eleitores) {
        return new PremiacaoTop3("Craque", "", CANDIDATOS, List.of(JORNALISTAS, TORCEDORES), eleitores);
    }

    private static PremiacaoTop3 padrao() {
        return criar(Map.of("e1", "Jornalistas", "e2", "Torcedores", "e3", "Torcedores"));
    }

    @Nested
    @DisplayName("Entrada: eleitorId")
    class EntradaEleitorId {

        @ParameterizedTest
        @CsvSource({"e1, Jornalistas", "e2, Torcedores", "e3, Torcedores"})
        @DisplayName("CE1 (válida): id cadastrado devolve o grupo dele e é apto")
        void ce1DevolveGrupoDoEleitor(String id, String grupo) {
            PremiacaoTop3 premiacao = padrao();

            assertEquals(grupo, premiacao.categoriaDe(id).orElseThrow().nome());
            assertTrue(premiacao.ehApto(id));
        }

        @ParameterizedTest
        @ValueSource(strings = {" e1", "e1 ", "  e1  ", "\te1\n"})
        @DisplayName("CE2 (válida): espaços nas pontas são ignorados")
        void ce2IgnoraEspacosNasPontas(String id) {
            PremiacaoTop3 premiacao = padrao();

            assertEquals(Optional.of(JORNALISTAS), premiacao.categoriaDe(id));
            assertTrue(premiacao.ehApto(id));
        }

        @Test
        @DisplayName("CE3 (inválida): id nulo não tem grupo e não é apto")
        void ce3IdNulo() {
            PremiacaoTop3 premiacao = padrao();

            assertEquals(Optional.empty(), premiacao.categoriaDe(null));
            assertFalse(premiacao.ehApto(null));
            assertFalse(premiacao.jaVotou(null));
        }

        @ParameterizedTest
        @ValueSource(strings = {"", " ", "\t"})
        @DisplayName("CE4 (inválida): id vazio ou só com espaços não é apto")
        void ce4IdSemTexto(String id) {
            PremiacaoTop3 premiacao = padrao();

            assertEquals(Optional.empty(), premiacao.categoriaDe(id));
            assertFalse(premiacao.ehApto(id));
        }

        @ParameterizedTest
        @ValueSource(strings = {"e4", "E1", "e 1", "Jornalistas"})
        @DisplayName("CE5 (inválida): id não cadastrado não é apto; a caixa conta")
        void ce5IdDesconhecido(String id) {
            PremiacaoTop3 premiacao = padrao();

            assertEquals(Optional.empty(), premiacao.categoriaDe(id));
            assertFalse(premiacao.ehApto(id));
        }

        @ParameterizedTest
        @EnumSource(StatusPremiacao.class)
        @DisplayName("CE1: a consulta funciona em qualquer estado e não registra voto")
        void consultaNaoMudaEstado(StatusPremiacao estado) {
            PremiacaoTop3 premiacao = padrao();
            levarAte(premiacao, estado);

            assertTrue(premiacao.ehApto("e2"));
            assertEquals(Optional.of(TORCEDORES), premiacao.categoriaDe("e2"));
            assertEquals(estado, premiacao.status());
            assertEquals(0, premiacao.totalCedulas());
            assertFalse(premiacao.jaVotou("e2"));
        }

        @Test
        @DisplayName("CE2: quem é apto com espaços vota uma vez só, com ou sem espaços")
        void ce2VotoUsaMesmoIdNormalizado() {
            PremiacaoTop3 premiacao = padrao();
            premiacao.abrir();

            premiacao.registrarCedula(" e1 ", CEDULA);

            assertTrue(premiacao.jaVotou("e1"));
            assertTrue(premiacao.jaVotou("\te1"));
            assertThrows(IllegalStateException.class, () -> premiacao.registrarCedula("e1", CEDULA));
            assertEquals(1, premiacao.totalCedulas());
        }
    }

    @Nested
    @DisplayName("Entrada: categoriaPorEleitor")
    class EntradaCategoriaPorEleitor {

        @Test
        @DisplayName("CE6 (válida, limite): sem eleitores, ninguém é apto e o total é 0")
        void ce6SemEleitores() {
            PremiacaoTop3 premiacao = criar(Map.of());

            assertEquals(0, premiacao.totalEleitores());
            assertFalse(premiacao.ehApto("e1"));
        }

        @Test
        @DisplayName("CE7 (válida): o total conta os eleitores cadastrados e não muda com os votos")
        void ce7ContaEleitores() {
            PremiacaoTop3 premiacao = padrao();
            assertEquals(3, premiacao.totalEleitores());

            premiacao.abrir();
            premiacao.registrarCedula("e1", CEDULA);
            premiacao.encerrar();

            assertEquals(3, premiacao.totalEleitores());
            assertEquals(1, premiacao.totalCedulas());
        }

        @Test
        @DisplayName("CE7 (válida): id cadastrado com espaços é guardado sem eles")
        void ce7NormalizaIdDaConfiguracao() {
            PremiacaoTop3 premiacao = criar(Map.of(" e1 ", "Jornalistas", "e2", "Torcedores"));

            assertEquals(2, premiacao.totalEleitores());
            assertEquals(Optional.of(JORNALISTAS), premiacao.categoriaDe("e1"));
            assertTrue(premiacao.ehApto(" e1 "));
        }

        @ParameterizedTest
        @ValueSource(strings = {" e1", "e1 ", "\te1"})
        @DisplayName("CE8 (inválida): dois ids iguais depois do strip são rejeitados")
        void ce8RejeitaIdRepetidoAposStrip(String outro) {
            Map<String, String> eleitores = new HashMap<>();
            eleitores.put("e1", "Jornalistas");
            eleitores.put(outro, "Torcedores");

            IllegalArgumentException erro = assertThrows(IllegalArgumentException.class, () -> criar(eleitores));
            assertEquals("id de eleitor repetido", erro.getMessage());
        }
    }

    private static void levarAte(PremiacaoTop3 premiacao, StatusPremiacao estado) {
        if (estado != StatusPremiacao.RASCUNHO) {
            premiacao.abrir();
        }
        if (estado == StatusPremiacao.ENCERRADA || estado == StatusPremiacao.APURADA) {
            premiacao.encerrar();
        }
        if (estado == StatusPremiacao.APURADA) {
            premiacao.apurar();
        }
    }
}

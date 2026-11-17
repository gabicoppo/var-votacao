package br.unicamp.var.cadastro;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Teste funcional de {@link Candidato} por particionamento em classes de
 * equivalência (Atividade 5, C1). Entradas: {@code nome} e {@code codigo}
 * do construtor.
 *
 * <pre>
 * | ID   | Entrada | Classe de equivalência                          | Válida?  |
 * |------|---------|-------------------------------------------------|----------|
 * | CE1  | nome    | texto comum                                     | válida   |
 * | CE2  | nome    | texto com espaços nas bordas (normalizado)      | válida   |
 * | CE3  | nome    | null                                            | inválida |
 * | CE4  | nome    | string vazia ""                                 | inválida |
 * | CE5  | nome    | só espaços em branco (espaço, tab, quebra)      | inválida |
 * | CE6  | codigo  | texto comum (ex.: "10", "P-01")                 | válida   |
 * | CE7  | codigo  | texto com espaços nas bordas (normalizado)      | válida   |
 * | CE8  | codigo  | null                                            | inválida |
 * | CE9  | codigo  | string vazia ""                                 | inválida |
 * | CE10 | codigo  | só espaços em branco (espaço, tab, quebra)      | inválida |
 * </pre>
 */
class CandidatoClassesEquivalenciaTest {

    @Nested
    @DisplayName("Entrada: nome")
    class EntradaNome {

        @Test
        @DisplayName("CE1 (válida): nome com texto comum")
        void ce1CriaCandidatoComNomeComum() {
            Candidato candidato = new Candidato("Ana Souza", "10");

            assertEquals("Ana Souza", candidato.nome());
        }

        @Test
        @DisplayName("CE2 (válida): espaços nas bordas do nome são removidos")
        void ce2RemoveEspacosDasBordasDoNome() {
            Candidato candidato = new Candidato("  Ana Souza ", "10");

            assertEquals("Ana Souza", candidato.nome());
        }

        @Test
        @DisplayName("CE3 (inválida): nome nulo")
        void ce3RejeitaNomeNulo() {
            assertThrows(IllegalArgumentException.class, () -> new Candidato(null, "10"));
        }

        @Test
        @DisplayName("CE4 (inválida): nome vazio")
        void ce4RejeitaNomeVazio() {
            assertThrows(IllegalArgumentException.class, () -> new Candidato("", "10"));
        }

        @ParameterizedTest
        @ValueSource(strings = {" ", "   ", "\t", "\n"})
        @DisplayName("CE5 (inválida): nome só com espaços em branco")
        void ce5RejeitaNomeSoComEspacosEmBranco(String nomeInvalido) {
            assertThrows(IllegalArgumentException.class, () -> new Candidato(nomeInvalido, "10"));
        }
    }

    @Nested
    @DisplayName("Entrada: codigo")
    class EntradaCodigo {

        @Test
        @DisplayName("CE6 (válida): código com texto comum")
        void ce6CriaCandidatoComCodigoComum() {
            Candidato candidato = new Candidato("Ana Souza", "P-01");

            assertEquals("P-01", candidato.codigo());
        }

        @Test
        @DisplayName("CE7 (válida): espaços nas bordas do código são removidos")
        void ce7RemoveEspacosDasBordasDoCodigo() {
            Candidato candidato = new Candidato("Ana Souza", " P-01  ");

            assertEquals("P-01", candidato.codigo());
        }

        @Test
        @DisplayName("CE8 (inválida): código nulo")
        void ce8RejeitaCodigoNulo() {
            assertThrows(IllegalArgumentException.class, () -> new Candidato("Ana Souza", null));
        }

        @Test
        @DisplayName("CE9 (inválida): código vazio")
        void ce9RejeitaCodigoVazio() {
            assertThrows(IllegalArgumentException.class, () -> new Candidato("Ana Souza", ""));
        }

        @ParameterizedTest
        @ValueSource(strings = {" ", "   ", "\t", "\n"})
        @DisplayName("CE10 (inválida): código só com espaços em branco")
        void ce10RejeitaCodigoSoComEspacosEmBranco(String codigoInvalido) {
            assertThrows(IllegalArgumentException.class, () -> new Candidato("Ana Souza", codigoInvalido));
        }
    }
}

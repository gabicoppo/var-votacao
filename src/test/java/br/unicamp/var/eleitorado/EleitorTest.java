package br.unicamp.var.eleitorado;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/** Testes de {@link Eleitor} (história #24). */
class EleitorTest {

    @Nested
    @DisplayName("Criação")
    class Criacao {

        @Test
        @DisplayName("eleitor com id e nome válidos é criado")
        void criaEleitorValido() {
            Eleitor eleitor = new Eleitor("123", "Ana Souza");

            assertEquals("123", eleitor.id());
            assertEquals("Ana Souza", eleitor.nome());
        }

        @Test
        @DisplayName("id e nome perdem os espaços nas pontas")
        void normalizaIdENome() {
            Eleitor eleitor = new Eleitor("  123\t", " Ana Souza  ");

            assertEquals(new Eleitor("123", "Ana Souza"), eleitor);
        }

        @Test
        @DisplayName("espaços internos do nome são mantidos")
        void mantemEspacosInternos() {
            assertEquals("Ana  Souza", new Eleitor("1", "Ana  Souza").nome());
        }
    }

    @Nested
    @DisplayName("Validação")
    class Validacao {

        @ParameterizedTest(name = "id inválido: \"{0}\"")
        @NullAndEmptySource
        @ValueSource(strings = {" ", "   ", "\t\n"})
        @DisplayName("id nulo, vazio ou só com espaços é rejeitado")
        void rejeitaIdInvalido(String id) {
            IllegalArgumentException erro =
                    assertThrows(IllegalArgumentException.class, () -> new Eleitor(id, "Ana"));
            assertEquals("id do eleitor nao pode ser vazio", erro.getMessage());
        }

        @ParameterizedTest(name = "nome inválido: \"{0}\"")
        @NullAndEmptySource
        @ValueSource(strings = {" ", "   ", "\t\n"})
        @DisplayName("nome nulo, vazio ou só com espaços é rejeitado")
        void rejeitaNomeInvalido(String nome) {
            IllegalArgumentException erro =
                    assertThrows(IllegalArgumentException.class, () -> new Eleitor("123", nome));
            assertEquals("nome do eleitor nao pode ser vazio", erro.getMessage());
        }
    }
}

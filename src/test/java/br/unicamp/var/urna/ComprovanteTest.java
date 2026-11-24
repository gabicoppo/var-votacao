package br.unicamp.var.urna;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.RecordComponent;
import java.time.Instant;
import java.util.Arrays;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/** Testes de {@link Comprovante} (história #28, RNF-01). */
class ComprovanteTest {

    private static final Instant HORARIO = Instant.parse("2026-11-24T12:00:00Z");

    @Nested
    @DisplayName("Criação")
    class Criacao {

        @Test
        @DisplayName("comprovante válido guarda código e horário")
        void criaComprovanteValido() {
            Comprovante comprovante = new Comprovante("ABCD234567", HORARIO);

            assertEquals("ABCD234567", comprovante.codigo());
            assertEquals(HORARIO, comprovante.registradoEm());
        }

        @Test
        @DisplayName("código perde os espaços nas pontas")
        void normalizaCodigo() {
            assertEquals("ABCD234567", new Comprovante("  ABCD234567 ", HORARIO).codigo());
        }

        @ParameterizedTest(name = "código inválido: \"{0}\"")
        @NullAndEmptySource
        @ValueSource(strings = {" ", "\t"})
        @DisplayName("código nulo, vazio ou em branco é rejeitado")
        void rejeitaCodigoInvalido(String codigo) {
            assertThrows(IllegalArgumentException.class, () -> new Comprovante(codigo, HORARIO));
        }

        @Test
        @DisplayName("horário nulo é rejeitado")
        void rejeitaHorarioNulo() {
            assertThrows(IllegalArgumentException.class, () -> new Comprovante("ABCD234567", null));
        }
    }

    @Nested
    @DisplayName("Sigilo (RNF-01)")
    class SigiloDoComprovante {

        @Test
        @DisplayName("o comprovante só tem código e horário: nem eleitor nem opção")
        void naoTemEleitorNemOpcao() {
            String[] componentes = Arrays.stream(Comprovante.class.getRecordComponents())
                    .map(RecordComponent::getName)
                    .toArray(String[]::new);

            assertArrayEquals(new String[] {"codigo", "registradoEm"}, componentes);
        }
    }
}

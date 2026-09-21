package br.unicamp.var.cadastro;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class CandidatoTest {

    @Test
    void criaCandidatoComNomeECodigo() {
        Candidato candidato = new Candidato("Ana Souza", "10");

        assertEquals("Ana Souza", candidato.nome());
        assertEquals("10", candidato.codigo());
    }

    @Test
    void removeEspacosDasPontasDeNomeECodigo() {
        Candidato candidato = new Candidato("  Ana Souza ", " P-01  ");

        assertEquals("Ana Souza", candidato.nome());
        assertEquals("P-01", candidato.codigo());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   ", "\t"})
    void rejeitaNomeNuloVazioOuEmBranco(String nomeInvalido) {
        assertThrows(IllegalArgumentException.class, () -> new Candidato(nomeInvalido, "10"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   ", "\t"})
    void rejeitaCodigoNuloVazioOuEmBranco(String codigoInvalido) {
        assertThrows(IllegalArgumentException.class, () -> new Candidato("Ana Souza", codigoInvalido));
    }
}
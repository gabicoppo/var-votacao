package br.unicamp.var.web.resultado;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Código de verificação do relatório (história #31): estável, sensível a mudanças e no formato da tela. */
class CodigoVerificacaoTest {

    private static final String TEXTO = "Relatório de apuração\nVotação: Assembleia\nSituação: Eleito: Aprovar";

    @Test
    @DisplayName("Mesmo texto, mesmo código")
    void estavel() {
        assertEquals(CodigoVerificacao.de(TEXTO), CodigoVerificacao.de(new String(TEXTO)));
    }

    @Test
    @DisplayName("Mudar um caractere muda o código")
    void mudaComOTexto() {
        assertNotEquals(CodigoVerificacao.de(TEXTO), CodigoVerificacao.de(TEXTO.replace("Aprovar", "Aprovar.")));
        assertNotEquals(CodigoVerificacao.de(TEXTO), CodigoVerificacao.de(TEXTO + "\n"));
    }

    @Test
    @DisplayName("12 caracteres hexadecimais maiúsculos do SHA-256, em três grupos de quatro")
    void formato() {
        // SHA-256("") = e3b0c442 98fc1c14 ...
        assertEquals("E3B0-C442-98FC", CodigoVerificacao.de(""));
        assertTrue(CodigoVerificacao.de(TEXTO).matches("[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{4}"));
    }

    @Test
    @DisplayName("Texto nulo é rejeitado")
    void nulo() {
        assertThrows(IllegalArgumentException.class, () -> CodigoVerificacao.de(null));
    }
}

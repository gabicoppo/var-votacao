package br.unicamp.var.web.comum;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Alertas flash, tipos e papéis.
 */
class AlertaTest {

    @Test
    @DisplayName("fábricas criam cada tipo, com a classe CSS correspondente")
    void fabricas() {
        assertEquals(new Alerta(TipoAlerta.SUCESSO, "ok"), Alerta.sucesso("ok"));
        assertEquals(new Alerta(TipoAlerta.ERRO, "falhou"), Alerta.erro("falhou"));
        assertEquals(new Alerta(TipoAlerta.INFO, "aviso"), Alerta.info("aviso"));
        assertEquals("alerta-sucesso", TipoAlerta.SUCESSO.classeCss());
        assertEquals("alerta-erro", TipoAlerta.ERRO.classeCss());
        assertEquals("alerta-info", TipoAlerta.INFO.classeCss());
    }

    @Test
    @DisplayName("tipo nulo ou texto vazio são recusados")
    void validacoes() {
        assertThrows(IllegalArgumentException.class, () -> new Alerta(null, "texto"));
        assertThrows(IllegalArgumentException.class, () -> Alerta.info(null));
        assertThrows(IllegalArgumentException.class, () -> Alerta.erro("  "));
    }

    @Test
    @DisplayName("papéis viram authorities ROLE_*")
    void papeis() {
        assertEquals("ROLE_ORGANIZADOR", Papel.ORGANIZADOR.authority());
        assertEquals("ROLE_ELEITOR", Papel.ELEITOR.authority());
    }
}

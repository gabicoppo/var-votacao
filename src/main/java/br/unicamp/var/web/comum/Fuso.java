package br.unicamp.var.web.comum;

import java.time.ZoneId;

/**
 * Fuso horário de exibição da aplicação web. O domínio trabalha com {@link java.time.Instant}; a
 * web mostra as datas no horário de Brasília.
 */
public final class Fuso {

    /** Fuso usado em todas as telas e no relógio da aplicação. */
    public static final ZoneId ZONA = ZoneId.of("America/Sao_Paulo");

    private Fuso() {
        // só constantes
    }
}

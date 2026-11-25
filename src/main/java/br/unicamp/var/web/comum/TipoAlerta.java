package br.unicamp.var.web.comum;

import java.util.Locale;

/**
 * Tipo de um {@link Alerta}; define a cor no layout ({@code .alerta-sucesso}, ...).
 */
public enum TipoAlerta {
    /** Operação concluída. */
    SUCESSO,
    /** Operação recusada ou com falha. */
    ERRO,
    /** Aviso neutro. */
    INFO;

    /**
     * Classe CSS do alerta.
     *
     * @return {@code "alerta-sucesso"}, {@code "alerta-erro"} ou {@code "alerta-info"}
     */
    public String classeCss() {
        return "alerta-" + name().toLowerCase(Locale.ROOT);
    }
}

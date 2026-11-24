package br.unicamp.var.web.comum;

/**
 * Papel de quem usa a aplicação. No Spring Security vira a authority {@code ROLE_<nome>}.
 */
public enum Papel {
    /** Configura, abre, encerra e apura votações e premiações. */
    ORGANIZADOR,
    /** Vota nas votações em que está apto e confere o comprovante. */
    ELEITOR;

    /**
     * Nome da authority no Spring Security.
     *
     * @return {@code "ROLE_ORGANIZADOR"} ou {@code "ROLE_ELEITOR"}
     */
    public String authority() {
        return "ROLE_" + name();
    }
}

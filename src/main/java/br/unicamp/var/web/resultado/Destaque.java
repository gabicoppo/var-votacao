package br.unicamp.var.web.resultado;

/** Destaque visual de uma barra do resultado (história #29). */
public enum Destaque {
    /** Opção vencedora. */
    VENCEDOR("barra-vencedor"),
    /** Uma das duas opções que vão ao 2º turno. */
    FINALISTA("barra-finalista"),
    /** Sem destaque. */
    NENHUM("");

    private final String classeCss;

    Destaque(String classeCss) {
        this.classeCss = classeCss;
    }

    /**
     * Classe CSS da barra.
     *
     * @return {@code "barra-vencedor"}, {@code "barra-finalista"} ou vazio
     */
    public String classeCss() {
        return classeCss;
    }
}

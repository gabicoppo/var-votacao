package br.unicamp.var.web.resultado;

/**
 * Votação ou premiação inexistente; os controllers respondem 404 com uma página amigável.
 */
public class RecursoNaoEncontradoException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String recurso;

    /**
     * Cria a exceção.
     *
     * @param recurso o que não foi encontrado, para a tela (ex.: {@code "Votação"})
     */
    public RecursoNaoEncontradoException(String recurso) {
        super(recurso + " nao encontrada");
        this.recurso = recurso;
    }

    /**
     * O que não foi encontrado.
     *
     * @return ex.: {@code "Votação"} ou {@code "Premiação"}
     */
    public String recurso() {
        return recurso;
    }
}

package br.unicamp.var.web.resultado;

/**
 * Eleitor que não está na lista de aptos tentando ver um resultado ou ranking (RNF-05); os
 * controllers respondem 403 com a página de acesso negado.
 */
public class AcessoNegadoException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** Cria a exceção. */
    public AcessoNegadoException() {
        super("eleitor nao apto");
    }
}

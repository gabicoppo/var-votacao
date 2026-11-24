package br.unicamp.var.web.organizador;

/**
 * Não há votação com o id pedido; o controller responde 404.
 */
public class VotacaoNaoEncontradaException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * @param id id procurado
     */
    public VotacaoNaoEncontradaException(String id) {
        super("votacao nao encontrada: " + id);
    }
}

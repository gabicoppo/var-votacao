package br.unicamp.var.web.premiacao;

/**
 * Ação do ciclo da premiação recusada pelo domínio (ex.: encerrar em rascunho). A mensagem já vem
 * em português, pronta para o alerta.
 */
public class OperacaoRecusadaException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Cria a exceção.
     *
     * @param mensagem frase para o organizador
     */
    public OperacaoRecusadaException(String mensagem) {
        super(mensagem);
    }
}

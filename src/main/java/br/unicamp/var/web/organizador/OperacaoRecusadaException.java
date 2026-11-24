package br.unicamp.var.web.organizador;

/**
 * O domínio recusou uma operação do organizador. A mensagem já vem pronta para a tela,
 * traduzida por {@link br.unicamp.var.web.comum.MensagensErro#amigavel}.
 */
public class OperacaoRecusadaException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * @param mensagemAmigavel frase em português para o organizador
     * @param causa            exceção original do domínio
     */
    public OperacaoRecusadaException(String mensagemAmigavel, RuntimeException causa) {
        super(mensagemAmigavel, causa);
    }
}

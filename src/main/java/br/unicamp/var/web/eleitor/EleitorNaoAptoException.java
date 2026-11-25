package br.unicamp.var.web.eleitor;

/**
 * O eleitor logado não está na lista de aptos da votação ou da premiação (RNF-05). Vira a página
 * {@code eleitor/nao-apto} com HTTP 403.
 */
public class EleitorNaoAptoException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Cria a exceção.
     *
     * @param titulo pergunta da votação ou título da premiação, para a página de aviso
     */
    public EleitorNaoAptoException(String titulo) {
        super(titulo);
    }
}

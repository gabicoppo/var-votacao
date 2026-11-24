package br.unicamp.var.web.eleitor;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Votação ou premiação inexistente: a resposta é 404 e o Spring mostra {@code error/404.html}.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class NaoEncontradoException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Cria a exceção.
     *
     * @param mensagem o que não foi encontrado
     */
    public NaoEncontradoException(String mensagem) {
        super(mensagem);
    }
}

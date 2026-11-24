package br.unicamp.var.web.organizador;

/**
 * Erro de validação de um campo do formulário, com mensagem pronta para a tela.
 *
 * @param campo    nome da propriedade do {@link FormularioVotacao} (por exemplo, {@code "opcoes"})
 * @param mensagem frase em português para o organizador
 */
public record ErroCampo(String campo, String mensagem) {
}

package br.unicamp.var.web.premiacao;

/**
 * Erro de validação de um campo do formulário da premiação, com a frase pronta para a tela.
 *
 * @param campo    propriedade do {@link FormularioPremiacao} (ex.: {@code "categorias"})
 * @param mensagem frase em português para o organizador, com o número da linha quando houver
 */
public record ErroCampo(String campo, String mensagem) {
}

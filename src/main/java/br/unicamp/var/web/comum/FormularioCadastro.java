package br.unicamp.var.web.comum;

/**
 * Dados digitados no cadastro público ({@code /cadastro}).
 *
 * @param nome      nome de exibição
 * @param usuario   login escolhido
 * @param senha     senha
 * @param confirmar senha repetida
 * @param papel     {@code "ORGANIZADOR"} ou {@code "ELEITOR"}
 */
public record FormularioCadastro(String nome, String usuario, String senha, String confirmar, String papel) {

    /**
     * Formulário vazio, com o papel de eleitor marcado.
     *
     * @return o formulário inicial
     */
    public static FormularioCadastro vazio() {
        return new FormularioCadastro("", "", "", "", Papel.ELEITOR.name());
    }

    /**
     * O mesmo formulário sem as senhas, para mostrar de novo depois de um erro.
     *
     * @return cópia sem senha e confirmação
     */
    public FormularioCadastro semSenhas() {
        return new FormularioCadastro(nome, usuario, "", "", papel);
    }
}

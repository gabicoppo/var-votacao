package br.unicamp.var.web.comum;

import java.util.Optional;

/**
 * Contas de acesso à aplicação (RNF-05: só eleitores autenticados votam).
 */
public interface ContasUsuarios {

    /**
     * Cria a conta de eleitor (login = {@code eleitorId}) se ela ainda não existir.
     *
     * @param eleitorId identificador do eleitor na lista de aptos; vira o login
     * @param nome      nome exibido no cabeçalho
     * @return o código de acesso gerado, só quando a conta é criada; vazio se já existia
     * @throws IllegalArgumentException se o id ou o nome forem vazios
     */
    Optional<String> garantirContaEleitor(String eleitorId, String nome);

    /**
     * Nome de exibição de uma conta.
     *
     * @param login login da conta
     * @return o nome, ou vazio se a conta não existir
     */
    Optional<String> nomeDe(String login);
}

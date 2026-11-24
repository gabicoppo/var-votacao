package br.unicamp.var.web.organizador;

import java.util.List;

/**
 * Resultado de {@link GestaoVotacoes#criar}: a votação criada ou os erros do formulário.
 *
 * @param id      id da votação criada; {@code null} se houve erros
 * @param erros   erros por campo; vazia se a votação foi criada
 * @param acessos códigos de acesso das contas de eleitor criadas agora
 */
public record ResultadoCriacao(String id, List<ErroCampo> erros, List<AcessoEleitor> acessos) {

    /** Cópias defensivas das listas. */
    public ResultadoCriacao {
        erros = List.copyOf(erros);
        acessos = List.copyOf(acessos);
    }

    /**
     * Diz se a votação foi criada.
     *
     * @return {@code true} se não houve erros
     */
    public boolean criada() {
        return erros.isEmpty();
    }
}

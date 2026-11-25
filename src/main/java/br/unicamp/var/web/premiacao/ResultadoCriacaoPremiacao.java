package br.unicamp.var.web.premiacao;

import java.util.List;

/**
 * Resultado de criar uma premiação pelo formulário (história #23).
 *
 * @param id      id da premiação criada, ou {@code null} se o formulário tinha erros
 * @param erros   erros por campo; vazia se criou
 * @param acessos códigos das contas de eleitor criadas agora
 */
public record ResultadoCriacaoPremiacao(String id, List<ErroCampo> erros, List<AcessoPremiacao> acessos) {

    /** Guarda cópias imutáveis das listas. */
    public ResultadoCriacaoPremiacao {
        erros = List.copyOf(erros);
        acessos = List.copyOf(acessos);
    }

    /**
     * Diz se a premiação foi criada.
     *
     * @return {@code true} se não houve erro
     */
    public boolean criada() {
        return erros.isEmpty();
    }
}

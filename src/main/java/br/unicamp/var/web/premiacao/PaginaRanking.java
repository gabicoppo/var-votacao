package br.unicamp.var.web.premiacao;

import br.unicamp.var.top3.StatusPremiacao;
import br.unicamp.var.web.resultado.Andamento;
import java.util.List;

/**
 * Tudo o que a tela {@code /premiacoes/{id}/ranking} mostra (história #30). Antes da apuração só há
 * o andamento; depois, pódio, tabela completa e critérios.
 *
 * @param id           id da premiação
 * @param titulo       título
 * @param descricao    critério de avaliação, ou vazio
 * @param status       estado do ciclo
 * @param andamento    cédulas sobre eleitores aptos
 * @param podio        pódio (vazio antes da apuração ou sem cédulas)
 * @param grupos       nomes dos grupos, uma coluna da tabela para cada
 * @param linhas       todas as linhas do ranking (vazia antes da apuração)
 * @param criterios    como os pontos foram calculados (vazia antes da apuração)
 */
public record PaginaRanking(String id, String titulo, String descricao, StatusPremiacao status, Andamento andamento,
                            Podio podio, List<String> grupos, List<LinhaTabelaRanking> linhas,
                            List<String> criterios) {

    /** Guarda cópias imutáveis das listas. */
    public PaginaRanking {
        grupos = List.copyOf(grupos);
        linhas = List.copyOf(linhas);
        criterios = List.copyOf(criterios);
    }

    /**
     * Diz se o ranking já foi publicado.
     *
     * @return {@code true} com a premiação apurada
     */
    public boolean apurada() {
        return status == StatusPremiacao.APURADA;
    }
}

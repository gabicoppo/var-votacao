package br.unicamp.var.web.organizador;

import br.unicamp.var.top3.StatusPremiacao;
import java.util.List;

/**
 * Tudo o que o painel do organizador mostra, já calculado.
 *
 * @param abertas       votações abertas
 * @param rascunhos     votações em rascunho
 * @param apuradas      votações apuradas
 * @param totalVotos    votos registrados em todas as votações
 * @param abas          abas de filtro por status
 * @param filtro        status filtrado, ou "todas"
 * @param votacoes      cards das votações do filtro: abertas, encerradas, rascunhos e apuradas
 * @param totalVotacoes total de votações, sem filtro
 * @param premiacoes    premiações Top 3 cadastradas
 */
public record PainelOrganizador(int abertas, int rascunhos, int apuradas, int totalVotos, List<Aba> abas,
                                String filtro, List<CartaoVotacao> votacoes, int totalVotacoes,
                                List<CartaoPremiacao> premiacoes) {

    /** Cópias defensivas das listas. */
    public PainelOrganizador {
        abas = List.copyOf(abas);
        votacoes = List.copyOf(votacoes);
        premiacoes = List.copyOf(premiacoes);
    }

    /**
     * Aba de filtro por status.
     *
     * @param status     valor do parâmetro {@code ?status=}
     * @param rotulo     texto da aba
     * @param quantidade votações nesse status
     * @param ativa      se é o filtro atual
     */
    public record Aba(String status, String rotulo, int quantidade, boolean ativa) {
    }

    /**
     * Card de uma premiação Top 3 (a tela de gestão é de outra fatia).
     *
     * @param id      id da premiação no repositório
     * @param titulo  título
     * @param status  etapa da premiação
     * @param cedulas cédulas recebidas
     */
    public record CartaoPremiacao(String id, String titulo, StatusPremiacao status, int cedulas) {
    }
}

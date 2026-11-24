package br.unicamp.var.web.premiacao;

import br.unicamp.var.top3.LinhaRanking;
import br.unicamp.var.top3.RankingTop3;
import br.unicamp.var.web.comum.Formatador;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Pódio da premiação (história #30): as três primeiras <b>colocações</b> do ranking, não os três
 * primeiros nomes. Com empate pode haver dois "1º", e cada degrau mostra todos os candidatos daquela
 * colocação (ranking 1, 1, 3 → ouro com dois nomes e bronze; 1, 2, 2 → ouro e prata com dois nomes).
 * Ranking sem cédulas dá pódio vazio.
 *
 * @param lugares degraus em ordem de colocação
 */
public record Podio(List<LugarPodio> lugares) {

    private static final int ULTIMA_COLOCACAO = 3;
    private static final Formatador FMT = new Formatador();

    /** Guarda uma cópia imutável dos degraus. */
    public Podio {
        lugares = List.copyOf(lugares);
    }

    /**
     * Monta o pódio a partir do ranking apurado.
     *
     * @param ranking ranking da premiação
     * @return o pódio; vazio se nenhuma cédula foi registrada
     * @throws IllegalArgumentException se o ranking for nulo
     */
    public static Podio de(RankingTop3 ranking) {
        if (ranking == null) {
            throw new IllegalArgumentException("ranking nao pode ser nulo");
        }
        if (ranking.totalCedulas() == 0) {
            return new Podio(List.of());
        }
        Map<Integer, List<CandidatoPodio>> porColocacao = new TreeMap<>();
        for (LinhaRanking linha : ranking.linhas()) {
            if (linha.colocacao() <= ULTIMA_COLOCACAO) {
                porColocacao.computeIfAbsent(linha.colocacao(), c -> new ArrayList<>())
                        .add(new CandidatoPodio(linha.candidato(), linha.pontos(), pontos(linha.pontos())));
            }
        }
        List<LugarPodio> lugares = new ArrayList<>();
        porColocacao.forEach((colocacao, candidatos) -> lugares.add(new LugarPodio(colocacao, candidatos)));
        return new Podio(lugares);
    }

    /**
     * Diz se o pódio está vazio (premiação sem cédulas).
     *
     * @return {@code true} se não há degraus
     */
    public boolean vazio() {
        return lugares.isEmpty();
    }

    /**
     * Pontuação para a tela, com no máximo uma casa decimal.
     *
     * @param pontos pontuação ponderada
     * @return ex.: {@code "23,5 pts"}, {@code "1 pt"}
     */
    static String pontos(double pontos) {
        String numero = FMT.numero(pontos);
        return numero + ("1".equals(numero) ? " pt" : " pts");
    }
}

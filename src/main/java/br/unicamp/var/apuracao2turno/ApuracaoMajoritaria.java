package br.unicamp.var.apuracao2turno;

import br.unicamp.var.comum.Proporcao;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Apura o resultado de uma votação majoritária, decidindo entre eleição direta
 * por maioria absoluta e a necessidade de segundo turno (história #21).
 *
 * <p>Escopo desta classe: apenas a decisão de quem venceu no primeiro turno ou
 * quais dois candidatos avançam para o segundo turno. A execução do segundo
 * turno em si, a mudança de estado do processo decisório e o tratamento de votos
 * nulos/brancos ficam fora deste escopo.</p>
 *
 * <p><b>Empates.</b> Candidatos com o mesmo número de votos ficam na ordem de
 * cadastro, isto é, na ordem de iteração do mapa de entrada (use um
 * {@link java.util.LinkedHashMap}). Assim, com A=40, B=30 e C=30 cadastrados
 * nessa ordem, o 2º turno é entre A e B; num empate no 1º lugar, o primeiro
 * cadastrado aparece como {@code primeiroColocado}. O resultado é sempre
 * determinístico para a mesma ordem de cadastro.</p>
 */
public final class ApuracaoMajoritaria {

    private static final double LIMIAR_MAIORIA_ABSOLUTA = 0.5;

    private ApuracaoMajoritaria() {
        // Classe utilitária, não instanciada.
    }

    /**
     * Apura os votos recebidos por cada candidato e determina se algum candidato
     * obteve maioria absoluta (mais de 50% dos votos válidos) ou se o processo
     * deve seguir para segundo turno entre os dois candidatos mais votados.
     *
     * @param votosPorCandidato mapa com o nome de cada candidato e o número de
     *                          votos recebidos por ele, na ordem de cadastro; a
     *                          soma dos valores é considerada o total de votos
     *                          válidos
     * @return {@link ResultadoApuracao.SemVencedor} se não houver votos válidos;
     *         {@link ResultadoApuracao.Eleito} quando um candidato obtém maioria
     *         absoluta (um candidato único com votos sempre obtém); senão
     *         {@link ResultadoApuracao.SegundoTurno} com os dois mais votados,
     *         desempatados pela ordem de cadastro
     * @throws IllegalArgumentException se o mapa for nulo
     */
    public static ResultadoApuracao apurar(Map<String, Integer> votosPorCandidato) {
        if (votosPorCandidato == null) {
            throw new IllegalArgumentException("votos por candidato nao pode ser nulo");
        }
        int totalDeVotosValidos = votosPorCandidato.values().stream()
                .mapToInt(Integer::intValue)
                .sum();
        if (totalDeVotosValidos == 0) {
            return new ResultadoApuracao.SemVencedor();
        }

        // sorted() é estável em stream ordenado: empates mantêm a ordem do mapa de entrada.
        List<Map.Entry<String, Integer>> candidatosPorVotosDecrescente = votosPorCandidato.entrySet().stream()
                .sorted(Comparator.<Map.Entry<String, Integer>>comparingInt(Map.Entry::getValue).reversed())
                .toList();

        Map.Entry<String, Integer> maisVotado = candidatosPorVotosDecrescente.get(0);
        if (temMaioriaAbsoluta(maisVotado.getValue(), totalDeVotosValidos)) {
            return new ResultadoApuracao.Eleito(maisVotado.getKey());
        }

        Map.Entry<String, Integer> segundoMaisVotado = candidatosPorVotosDecrescente.get(1);
        return new ResultadoApuracao.SegundoTurno(maisVotado.getKey(), segundoMaisVotado.getKey());
    }

    private static boolean temMaioriaAbsoluta(int votosDoCandidato, int totalDeVotosValidos) {
        return new Proporcao(votosDoCandidato, totalDeVotosValidos).supera(LIMIAR_MAIORIA_ABSOLUTA);
    }
}

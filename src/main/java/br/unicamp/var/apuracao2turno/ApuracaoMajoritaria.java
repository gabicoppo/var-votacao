package br.unicamp.var.apuracao2turno;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Apura o resultado de uma votação majoritária, decidindo entre eleição direta 
 * por maioria absoluta e a necessidade de segundo turno.
 *
 * <p>Escopo desta classe: apenas a decisão de quem venceu no primeiro turno ou 
 * quais dois candidatos avançam para o segundo turno. A execução do segundo 
 * turno em si, a mudança de estado do processo decisório e o tratamento de votos 
 * nulos/brancos ficam fora deste escopo. Empates (tanto no primeiro quanto no 
 * segundo lugar) também não são tratados nem cobertos pelos testes desta 
 * feature; o comportamento nesses casos não é garantido.</p>
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
     *                          votos recebidos por ele; a soma dos valores é 
     *                          considerada o total de votos válidos
     * @return {@link ResultadoApuracao.Eleito} quando um candidato obtém maioria 
     *         absoluta, ou {@link ResultadoApuracao.SegundoTurno} com os dois 
     *         candidatos mais votados, caso contrário
     */
    public static ResultadoApuracao apurar(Map<String, Integer> votosPorCandidato) {
        int totalDeVotosValidos = votosPorCandidato.values().stream()
                .mapToInt(Integer::intValue)
                .sum();

        List<Map.Entry<String, Integer>> candidatosPorVotosDecrescente = votosPorCandidato.entrySet().stream()
                .sorted(Comparator.<Map.Entry<String, Integer>>comparingInt(Map.Entry::getValue).reversed())
                .collect(Collectors.toList());

        Map.Entry<String, Integer> maisVotado = candidatosPorVotosDecrescente.get(0);

        if (temMaioriaAbsoluta(maisVotado.getValue(), totalDeVotosValidos)) {
            return new ResultadoApuracao.Eleito(maisVotado.getKey());
        }

        Map.Entry<String, Integer> segundoMaisVotado = candidatosPorVotosDecrescente.get(1);
        return new ResultadoApuracao.SegundoTurno(maisVotado.getKey(), segundoMaisVotado.getKey());
    }

    private static boolean temMaioriaAbsoluta(int votosDoCandidato, int totalDeVotosValidos) {
        return (double) votosDoCandidato / totalDeVotosValidos > LIMIAR_MAIORIA_ABSOLUTA;
    }
}
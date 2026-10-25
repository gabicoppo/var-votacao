package br.unicamp.var.apuracao;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Contagem de votos de uma votação encerrada, entrada comum de toda
 * {@link RegraApuracao}.
 *
 * <p>O mapa é copiado e fica imutável, preservando a ordem de inserção das
 * opções. Por enquanto o modelo não distingue votos brancos, nulos nem
 * abstenções: todos os votos do mapa são considerados válidos.</p>
 *
 * @param votosPorOpcao nome de cada opção e o número de votos recebidos por ela
 * @param totalAptos    número de pessoas aptas a votar
 */
public record ContagemVotos(Map<String, Integer> votosPorOpcao, int totalAptos) {

    /**
     * Valida e copia a contagem.
     *
     * @throws IllegalArgumentException se o mapa for nulo ou vazio, tiver opção
     *                                  nula, votos nulos ou negativos, se
     *                                  {@code totalAptos} não for positivo ou se
     *                                  a soma dos votos passar de {@code totalAptos}
     */
    public ContagemVotos {
        if (votosPorOpcao == null || votosPorOpcao.isEmpty()) {
            throw new IllegalArgumentException("deve haver ao menos uma opcao");
        }
        if (totalAptos <= 0) {
            throw new IllegalArgumentException("totalAptos deve ser maior que zero");
        }
        long soma = 0;
        for (Map.Entry<String, Integer> entrada : votosPorOpcao.entrySet()) {
            if (entrada.getKey() == null) {
                throw new IllegalArgumentException("opcao nao pode ser nula");
            }
            if (entrada.getValue() == null || entrada.getValue() < 0) {
                throw new IllegalArgumentException("votos de " + entrada.getKey() + " devem ser >= 0");
            }
            soma += entrada.getValue();
        }
        if (soma > totalAptos) {
            throw new IllegalArgumentException("soma dos votos nao pode passar do total de aptos");
        }
        votosPorOpcao = Collections.unmodifiableMap(new LinkedHashMap<>(votosPorOpcao));
    }

    /**
     * Soma dos votos de todas as opções.
     *
     * @return total de votos válidos
     */
    public int totalVotosValidos() {
        return votosPorOpcao.values().stream()
                .mapToInt(Integer::intValue)
                .sum();
    }

    /**
     * Opção mais votada, se ela for única.
     *
     * @return a opção mais votada, ou vazio se houver empate no primeiro lugar
     */
    Optional<String> maisVotadaSemEmpate() {
        String maisVotada = null;
        int maiorVotacao = -1;
        boolean empate = false;
        for (Map.Entry<String, Integer> entrada : votosPorOpcao.entrySet()) {
            if (entrada.getValue() > maiorVotacao) {
                maisVotada = entrada.getKey();
                maiorVotacao = entrada.getValue();
                empate = false;
            } else if (entrada.getValue() == maiorVotacao) {
                empate = true;
            }
        }
        return empate ? Optional.empty() : Optional.of(maisVotada);
    }
}

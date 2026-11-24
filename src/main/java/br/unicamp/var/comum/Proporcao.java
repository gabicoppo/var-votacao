package br.unicamp.var.comum;

/**
 * Proporção {@code parte / total} comparada a um limiar, usada pelas regras de
 * quórum, de aprovação e de maioria absoluta (histórias #21 e #29, RNF-02).
 *
 * <p>Extraída de {@code RegrasVotacao} e {@code ApuracaoMajoritaria} (Extract
 * Class), que repetiam o cálculo {@code (double) parte / total} com diferenças
 * implícitas: {@code >=} em uns, {@code >} em outro, e só um deles protegia o
 * total zero. Aqui as duas comparações têm nomes próprios: {@link #atinge}
 * ({@code >=}) e {@link #supera} ({@code >}).</p>
 *
 * <p>Total zero é aceito: o valor é 0 e nenhum limiar é atingido nem superado
 * (nunca dá {@code NaN}). Fica num pacote próprio para que pacotes de baixo
 * nível, como {@code apuracao2turno} e {@code votacao}, usem a mesma regra sem
 * depender um do outro.</p>
 *
 * @param parte quantidade que se quer comparar, entre 0 e {@code total}
 * @param total quantidade de referência, maior ou igual a zero
 */
public record Proporcao(int parte, int total) {

    /**
     * Valida a proporção.
     *
     * @throws IllegalArgumentException se {@code parte} ou {@code total} for
     *                                  negativo, ou se {@code parte > total}
     */
    public Proporcao {
        if (total < 0) {
            throw new IllegalArgumentException("total deve ser >= 0");
        }
        if (parte < 0) {
            throw new IllegalArgumentException("parte deve ser >= 0");
        }
        if (parte > total) {
            throw new IllegalArgumentException("parte nao pode passar do total");
        }
    }

    /**
     * Valor da proporção, sem truncar (ex.: 1/3 não vira 0).
     *
     * @return {@code parte / total}, entre 0 e 1; 0 se o total for zero
     */
    public double valor() {
        if (total == 0) {
            return 0.0;
        }
        return (double) parte / total;
    }

    /**
     * Verifica se a proporção atinge o limiar (maior ou igual).
     *
     * @param limiar fração mínima exigida
     * @return {@code true} se {@code parte / total >= limiar}; {@code false} se
     *         o total for zero
     */
    public boolean atinge(double limiar) {
        return total > 0 && valor() >= limiar;
    }

    /**
     * Verifica se a proporção supera o limiar (estritamente maior).
     *
     * @param limiar fração que precisa ser ultrapassada (ex.: 0.5 para maioria
     *               absoluta)
     * @return {@code true} se {@code parte / total > limiar}; {@code false} se
     *         o total for zero
     */
    public boolean supera(double limiar) {
        return total > 0 && valor() > limiar;
    }
}

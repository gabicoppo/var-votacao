package br.unicamp.var.votacao;

/**
 * Regras de quorum e maioria qualificada para votacoes do VAR.
 *
 * <p>Abstencao nao conta nos votos validos: o calculo de aprovacao considera
 * apenas os votos a favor e contra. Classe sem estado, so com metodos estaticos.
 */
public final class RegrasVotacao {

    private RegrasVotacao() {
        // Classe utilitaria, nao deve ser instanciada.
    }

    /**
     * Verifica se o quorum minimo foi atingido.
     *
     * @param presentes numero de presentes
     * @param total     numero total de membros
     * @param minimo    fracao minima exigida (entre 0 e 1)
     * @return true se a proporcao de presentes for maior ou igual ao minimo
     * @throws IllegalArgumentException se total for menor ou igual a zero
     */
    public static boolean quorumAtingido(int presentes, int total, double minimo) {
        if (total <= 0) {
            throw new IllegalArgumentException("total deve ser maior que zero");
        }
        // Usa divisao com double pra nao truncar a proporcao (ex: 1/3 nao pode virar 0).
        double proporcao = (double) presentes / total;
        return proporcao >= minimo;
    }

    /**
     * Verifica se a votacao foi aprovada, dada a fracao minima exigida.
     *
     * <p>Abstencao nao entra na conta: a fracao e calculada so sobre os votos
     * validos (a favor + contra).
     *
     * @param aFavor votos a favor
     * @param contra votos contra
     * @param fracao fracao minima exigida de aprovacao (entre 0 e 1)
     * @return true se a fracao de votos a favor for maior ou igual a exigida
     * @throws IllegalArgumentException se nao houver votos validos (aFavor + contra == 0)
     */
    public static boolean aprovada(int aFavor, int contra, double fracao) {
        int validos = aFavor + contra;
        if (validos <= 0) {
            throw new IllegalArgumentException("nao ha votos validos para calcular a aprovacao");
        }
        double proporcao = (double) aFavor / validos;
        return proporcao >= fracao;
    }
}
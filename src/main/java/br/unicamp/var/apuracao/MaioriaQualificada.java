package br.unicamp.var.apuracao;

import br.unicamp.var.apuracao2turno.ResultadoApuracao;
import br.unicamp.var.votacao.RegrasVotacao;

/**
 * Maioria qualificada com quórum: exige que uma fração mínima dos aptos vote e
 * que a opção mais votada alcance a fração exigida dos votos válidos.
 *
 * <p>Usa {@link RegrasVotacao#quorumAtingido} e {@link RegrasVotacao#aprovada}.
 * Como o modelo ainda não tem votos brancos nem abstenções, por enquanto os
 * votantes considerados no quórum são a soma dos votos de todas as opções.
 * A mais votada conta como "a favor" e as demais opções, somadas, como
 * "contra".</p>
 */
public final class MaioriaQualificada implements RegraApuracao {

    private final double quorumMinimo;
    private final double fracaoExigida;

    /**
     * Cria a regra.
     *
     * @param quorumMinimo  fração mínima dos aptos que precisam votar, em (0, 1]
     * @param fracaoExigida fração mínima dos votos válidos que a mais votada
     *                      precisa ter, em (0, 1] (ex.: 2/3)
     * @throws IllegalArgumentException se alguma fração estiver fora de (0, 1]
     */
    public MaioriaQualificada(double quorumMinimo, double fracaoExigida) {
        validarFracao(quorumMinimo, "quorumMinimo");
        validarFracao(fracaoExigida, "fracaoExigida");
        this.quorumMinimo = quorumMinimo;
        this.fracaoExigida = fracaoExigida;
    }

    /**
     * Apura a contagem por maioria qualificada.
     *
     * @param contagem votos por opção e total de aptos
     * @return {@link ResultadoApuracao.QuorumNaoAtingido} se o quórum não for
     *         atingido; {@link ResultadoApuracao.Eleito} se a mais votada, sem
     *         empate, alcançar a fração exigida; senão
     *         {@link ResultadoApuracao.SemVencedor}
     */
    @Override
    public ResultadoApuracao apurar(ContagemVotos contagem) {
        int votantes = contagem.totalVotosValidos();
        if (!RegrasVotacao.quorumAtingido(votantes, contagem.totalAptos(), quorumMinimo)) {
            return new ResultadoApuracao.QuorumNaoAtingido();
        }
        return contagem.maisVotadaSemEmpate()
                .filter(opcao -> aprovada(contagem, opcao, votantes))
                .<ResultadoApuracao>map(ResultadoApuracao.Eleito::new)
                .orElseGet(ResultadoApuracao.SemVencedor::new);
    }

    private boolean aprovada(ContagemVotos contagem, String opcao, int votantes) {
        int votosDaOpcao = contagem.votosPorOpcao().get(opcao);
        return RegrasVotacao.aprovada(votosDaOpcao, votantes - votosDaOpcao, fracaoExigida);
    }

    private static void validarFracao(double fracao, String nome) {
        if (!(fracao > 0 && fracao <= 1)) {
            throw new IllegalArgumentException(nome + " deve estar em (0, 1]");
        }
    }
}

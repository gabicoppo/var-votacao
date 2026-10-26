package br.unicamp.var.apuracao;

import br.unicamp.var.apuracao2turno.ApuracaoMajoritaria;
import br.unicamp.var.apuracao2turno.ResultadoApuracao;

/**
 * Maioria absoluta: vence a opção com mais de 50% dos votos válidos, com
 * segundo turno opcional entre as duas mais votadas.
 *
 * <p>A decisão é delegada a {@link ApuracaoMajoritaria}. Se o segundo turno
 * estiver desabilitado, a votação sem maioria absoluta fica sem vencedor.
 * Empates continuam fora do escopo de {@link ApuracaoMajoritaria}.</p>
 */
public final class MaioriaAbsoluta implements RegraApuracao {

    private final boolean segundoTurnoHabilitado;

    /**
     * Cria a regra.
     *
     * @param segundoTurnoHabilitado se a votação sem maioria absoluta segue para
     *                               segundo turno entre as duas mais votadas
     */
    public MaioriaAbsoluta(boolean segundoTurnoHabilitado) {
        this.segundoTurnoHabilitado = segundoTurnoHabilitado;
    }

    /**
     * Apura a contagem por maioria absoluta.
     *
     * @param contagem votos por opção
     * @return {@link ResultadoApuracao.Eleito} com mais de 50% dos votos válidos;
     *         senão {@link ResultadoApuracao.SegundoTurno} se habilitado, ou
     *         {@link ResultadoApuracao.SemVencedor}; também
     *         {@link ResultadoApuracao.SemVencedor} se não houver votos
     */
    @Override
    public ResultadoApuracao apurar(ContagemVotos contagem) {
        if (contagem.totalVotosValidos() == 0) {
            return new ResultadoApuracao.SemVencedor();
        }
        ResultadoApuracao resultado = ApuracaoMajoritaria.apurar(contagem.votosPorOpcao());
        if (resultado instanceof ResultadoApuracao.SegundoTurno && !segundoTurnoHabilitado) {
            return new ResultadoApuracao.SemVencedor();
        }
        return resultado;
    }
}

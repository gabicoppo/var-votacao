package br.unicamp.var.apuracao;

import br.unicamp.var.apuracao2turno.ResultadoApuracao;

/**
 * Maioria simples: vence a opção mais votada, qualquer que seja o percentual.
 *
 * <p>Sem votos válidos ou com empate no primeiro lugar, não há vencedor.</p>
 */
public final class MaioriaSimples implements RegraApuracao {

    /**
     * Apura a contagem por maioria simples.
     *
     * @param contagem votos por opção
     * @return {@link ResultadoApuracao.Eleito} com a mais votada, ou
     *         {@link ResultadoApuracao.SemVencedor} se não houver votos ou
     *         houver empate no primeiro lugar
     */
    @Override
    public ResultadoApuracao apurar(ContagemVotos contagem) {
        if (contagem.totalVotosValidos() == 0) {
            return new ResultadoApuracao.SemVencedor();
        }
        return contagem.maisVotadaSemEmpate()
                .<ResultadoApuracao>map(ResultadoApuracao.Eleito::new)
                .orElseGet(ResultadoApuracao.SemVencedor::new);
    }
}

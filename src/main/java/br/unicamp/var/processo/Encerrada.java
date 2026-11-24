package br.unicamp.var.processo;

import br.unicamp.var.apuracao.ContagemVotos;
import br.unicamp.var.apuracao2turno.ResultadoApuracao;

/**
 * Votação que não aceita mais votos e aguarda apuração.
 */
final class Encerrada implements EstadoVotacao {

    @Override
    public String nome() {
        return "encerrada";
    }

    /**
     * Apura a contagem com a regra configurada na votação (ponto de integração
     * entre State e Strategy) e passa a votação para apurada, que guarda a
     * contagem usada para montar o resultado detalhado (história #29).
     */
    @Override
    public ResultadoApuracao apurar(Votacao votacao) {
        ContagemVotos contagem = votacao.contagem();
        ResultadoApuracao resultado = votacao.regra().apurar(contagem);
        votacao.mudarPara(new Apurada(contagem, resultado));
        return resultado;
    }
}

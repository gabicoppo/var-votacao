package br.unicamp.var.processo;

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
     * entre State e Strategy) e passa a votação para apurada.
     */
    @Override
    public ResultadoApuracao apurar(Votacao votacao) {
        ResultadoApuracao resultado = votacao.regra().apurar(votacao.contagem());
        votacao.mudarPara(new Apurada(resultado));
        return resultado;
    }
}

package br.unicamp.var.processo;

import br.unicamp.var.apuracao.RegraApuracao;

/**
 * Votação em configuração: a regra de apuração ainda pode ser alterada e a votação pode ser aberta.
 */
final class Rascunho implements EstadoVotacao {

    @Override
    public String nome() {
        return "rascunho";
    }

    @Override
    public void alterarRegra(Votacao votacao, RegraApuracao novaRegra) {
        votacao.substituirRegra(novaRegra);
    }

    @Override
    public void abrir(Votacao votacao) {
        votacao.mudarPara(new Aberta());
    }
}

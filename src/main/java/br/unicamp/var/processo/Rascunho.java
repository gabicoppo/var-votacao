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

    /**
     * Abre a votação se o início do período já chegou; o novo estado aplica o relógio
     * em seguida, então uma votação cujo período inteiro já passou vai direto a encerrada. A
     * abertura fica na auditoria com o autor {@value Votacao#AUTOR_SISTEMA} (RNF-07).
     */
    @Override
    public void atualizarPeloRelogio(Votacao votacao) {
        if (votacao.periodoComecou()) {
            votacao.abrirPeloRelogio();
            votacao.atualizarPeloRelogio();
        }
    }
}

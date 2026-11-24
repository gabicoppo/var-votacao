package br.unicamp.var.processo;

import br.unicamp.var.urna.Comprovante;

/**
 * Votação recebendo votos: a configuração está congelada e a votação pode ser encerrada.
 */
final class Aberta implements EstadoVotacao {

    @Override
    public String nome() {
        return "aberta";
    }

    /**
     * Deposita o voto na urna, que confere se o eleitor é apto e se ainda não votou.
     */
    @Override
    public Comprovante registrarVoto(Votacao votacao, String eleitorId, String opcao) {
        return votacao.depositarNaUrna(eleitorId, opcao);
    }

    @Override
    public void encerrar(Votacao votacao) {
        votacao.mudarPara(new Encerrada());
    }

    /**
     * Encerra a votação se o fim do período já chegou, registrando o encerramento na auditoria
     * com o autor {@value Votacao#AUTOR_SISTEMA} (RNF-07).
     */
    @Override
    public void atualizarPeloRelogio(Votacao votacao) {
        if (votacao.periodoTerminou()) {
            votacao.encerrarPeloRelogio();
        }
    }
}

package br.unicamp.var.processo;

/**
 * Votação recebendo votos: a configuração está congelada e a votação pode ser encerrada.
 */
final class Aberta implements EstadoVotacao {

    @Override
    public String nome() {
        return "aberta";
    }

    @Override
    public void registrarVoto(Votacao votacao, String opcao) {
        votacao.contabilizarVoto(opcao);
    }

    @Override
    public void encerrar(Votacao votacao) {
        votacao.mudarPara(new Encerrada());
    }
}

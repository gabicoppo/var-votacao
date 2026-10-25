package br.unicamp.var.processo;

import br.unicamp.var.apuracao2turno.ResultadoApuracao;

/**
 * Votação já apurada: guarda o resultado, que continua disponível para consulta.
 */
final class Apurada implements EstadoVotacao {

    private final ResultadoApuracao resultado;

    Apurada(ResultadoApuracao resultado) {
        this.resultado = resultado;
    }

    @Override
    public String nome() {
        return "apurada";
    }

    @Override
    public ResultadoApuracao apurar(Votacao votacao) {
        return resultado;
    }
}

package br.unicamp.var.processo;

import br.unicamp.var.apuracao.ContagemVotos;
import br.unicamp.var.apuracao2turno.ResultadoApuracao;
import br.unicamp.var.resultado.ResultadoDetalhado;
import java.util.Optional;

/**
 * Votação já apurada: guarda o resultado e a contagem usada na apuração, que
 * continuam disponíveis para consulta (história #29).
 */
final class Apurada implements EstadoVotacao {

    private final ResultadoApuracao resultado;
    private final ResultadoDetalhado detalhado;

    Apurada(ContagemVotos contagem, ResultadoApuracao resultado) {
        this.resultado = resultado;
        this.detalhado = ResultadoDetalhado.de(contagem, resultado);
    }

    @Override
    public String nome() {
        return "apurada";
    }

    @Override
    public ResultadoApuracao apurar(Votacao votacao) {
        return resultado;
    }

    @Override
    public ResultadoDetalhado resultadoDetalhado(Votacao votacao) {
        return detalhado;
    }

    @Override
    public Optional<ResultadoApuracao> resultado() {
        return Optional.of(resultado);
    }
}

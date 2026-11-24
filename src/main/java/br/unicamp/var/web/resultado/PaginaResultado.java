package br.unicamp.var.web.resultado;

/**
 * Tudo o que as telas {@code /votacoes/{id}/resultado} e {@code /votacoes/{id}/telao} mostram
 * (histórias #29 e #21). Antes da apuração só há o {@link Andamento}; depois, só o
 * {@link GraficoResultado}.
 *
 * @param votacao        dados da votação
 * @param andamento      participação antes da apuração, ou {@code null} se apurada
 * @param grafico        resultado depois da apuração, ou {@code null} antes dela
 * @param segundoTurnoId id do 2º turno criado a partir desta votação, ou {@code null}
 * @param primeiroTurno  resumo do 1º turno, quando esta votação é um 2º turno; senão {@code null}
 * @param votoDoEleitor  selo do eleitor logado sobre o próprio voto
 */
public record PaginaResultado(CabecalhoVotacao votacao, Andamento andamento, GraficoResultado grafico,
                              String segundoTurnoId, PrimeiroTurno primeiroTurno, VotoDoEleitor votoDoEleitor) {

    /**
     * Diz se a votação está apurada.
     *
     * @return {@code true} se há gráfico de resultado
     */
    public boolean apurada() {
        return grafico != null;
    }
}

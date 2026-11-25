package br.unicamp.var.web.resultado;

import br.unicamp.var.web.comum.Formatador;

/**
 * Participação antes da apuração (histórias #29 e #30): só votantes e aptos, nunca votos por opção
 * (RNF-01).
 *
 * @param status            status atual, em minúsculas
 * @param votantes          quantos já votaram
 * @param aptos             quantos podem votar
 * @param largura           largura da barra de participação, de 0 a 100
 * @param participacaoTexto participação formatada, ex.: {@code "58,3%"}
 * @param tempoTexto        prazo, ex.: {@code "Encerra em 2 dias e 5 h"}; vazio se não se aplica
 */
public record Andamento(String status, int votantes, int aptos, double largura, String participacaoTexto,
                        String tempoTexto) {

    private static final Formatador FMT = new Formatador();
    private static final double CEM = 100.0;

    /**
     * Calcula a participação.
     *
     * @param status     status atual, em minúsculas
     * @param votantes   quantos já votaram
     * @param aptos      quantos podem votar
     * @param tempoTexto texto do prazo, ou vazio
     * @return o andamento; com 0 aptos, a participação é 0%
     */
    public static Andamento de(String status, int votantes, int aptos, String tempoTexto) {
        double largura = aptos == 0 ? 0 : Math.min(CEM, votantes * CEM / aptos);
        return new Andamento(status, votantes, aptos, largura, FMT.numero(largura) + "%", tempoTexto);
    }

    /**
     * Versão JSON, para a atualização ao vivo.
     *
     * @return status, votantes e aptos
     */
    public AndamentoJson json() {
        return new AndamentoJson(status, votantes, aptos);
    }
}

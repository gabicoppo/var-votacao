package br.unicamp.var.web.resultado;

import br.unicamp.var.processo.PeriodoVotacao;
import java.time.Duration;
import java.time.Instant;

/**
 * Texto do prazo de uma votação ainda não apurada (história #20), para a tela de andamento.
 */
public final class TempoRestante {

    private static final long HORAS_POR_DIA = 24;
    private static final long MINUTOS_POR_HORA = 60;

    private TempoRestante() {
    }

    /**
     * Descreve o prazo conforme o status.
     *
     * @param status  status da votação ({@code "rascunho"}, {@code "aberta"}, {@code "encerrada"}...)
     * @param periodo período de votação, ou {@code null} se ela abre e encerra manualmente
     * @param agora   instante atual
     * @return ex.: {@code "Encerra em 2 dias e 5 h"}; vazio para votação apurada
     */
    public static String texto(String status, PeriodoVotacao periodo, Instant agora) {
        return switch (status) {
            case "rascunho" -> periodo == null ? "Ainda não foi aberta pelo organizador."
                    : periodo.naoComecou(agora) ? "Abre em " + duracao(Duration.between(agora, periodo.inicio()))
                    : "Abre em instantes.";
            case "aberta" -> periodo == null ? "Sem prazo definido: o organizador encerra a votação."
                    : periodo.terminou(agora) ? "Encerra em instantes."
                    : "Encerra em " + duracao(Duration.between(agora, periodo.fim()));
            case "encerrada" -> "Votação encerrada. Aguardando a apuração.";
            default -> "";
        };
    }

    /**
     * Duração legível, com no máximo duas unidades.
     *
     * @param duracao duração positiva
     * @return ex.: {@code "3 dias e 2 h"}, {@code "1 h e 5 min"}, {@code "menos de 1 min"}
     */
    static String duracao(Duration duracao) {
        long dias = duracao.toDays();
        long horas = duracao.toHours() % HORAS_POR_DIA;
        long minutos = duracao.toMinutes() % MINUTOS_POR_HORA;
        if (dias > 0) {
            return dias + (dias == 1 ? " dia" : " dias") + (horas > 0 ? " e " + horas + " h" : "");
        }
        if (horas > 0) {
            return horas + " h" + (minutos > 0 ? " e " + minutos + " min" : "");
        }
        return minutos > 0 ? minutos + " min" : "menos de 1 min";
    }
}

package br.unicamp.var.web.eleitor;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * Prazos em texto pt-BR para os cards do eleitor (história #26, período da história #20).
 *
 * <p>Regras, do mais próximo ao mais distante (o dia é o do calendário no fuso informado):</p>
 * <ul>
 *   <li>já passou: "encerrada" ou "já começou";</li>
 *   <li>menos de 1 hora: "encerra em 25 min" (minutos arredondados para cima);</li>
 *   <li>mesmo dia: "encerra hoje às 18:00";</li>
 *   <li>dia seguinte: "encerra amanhã às 18:00";</li>
 *   <li>depois: "encerra em 3 dias" (diferença de dias no calendário).</li>
 * </ul>
 */
public final class TempoRestante {

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");
    private static final Duration UMA_HORA = Duration.ofHours(1);
    private static final long SEGUNDOS_POR_MINUTO = 60;

    private TempoRestante() {
        // só métodos estáticos
    }

    /**
     * Quanto falta para a votação encerrar.
     *
     * @param agora instante atual
     * @param fim   fim do período
     * @param zona  fuso usado para "hoje" e "amanhã"
     * @return ex.: "encerra hoje às 18:00"; "encerrada" se {@code agora >= fim}
     */
    public static String descrever(Instant agora, Instant fim, ZoneId zona) {
        if (!agora.isBefore(fim)) {
            return "encerrada";
        }
        return "encerra " + falta(agora, fim, zona);
    }

    /**
     * Quanto falta para a votação começar.
     *
     * @param agora  instante atual
     * @param inicio início do período
     * @param zona   fuso usado para "hoje" e "amanhã"
     * @return ex.: "começa em 2 dias"; "já começou" se {@code agora >= inicio}
     */
    public static String descreverInicio(Instant agora, Instant inicio, ZoneId zona) {
        if (!agora.isBefore(inicio)) {
            return "já começou";
        }
        return "começa " + falta(agora, inicio, zona);
    }

    private static String falta(Instant agora, Instant alvo, ZoneId zona) {
        Duration duracao = Duration.between(agora, alvo);
        if (duracao.compareTo(UMA_HORA) < 0) {
            long minutos = (duracao.getSeconds() + SEGUNDOS_POR_MINUTO - 1) / SEGUNDOS_POR_MINUTO;
            return "em " + Math.max(1, minutos) + " min";
        }
        ZonedDateTime quando = alvo.atZone(zona);
        long dias = ChronoUnit.DAYS.between(LocalDate.ofInstant(agora, zona), quando.toLocalDate());
        if (dias == 0) {
            return "hoje às " + HORA.format(quando);
        }
        if (dias == 1) {
            return "amanhã às " + HORA.format(quando);
        }
        return "em " + dias + " dias";
    }
}

package br.unicamp.var.web.comum;

import br.unicamp.var.top3.StatusPremiacao;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.springframework.stereotype.Component;

/**
 * Formatação pt-BR para os templates, disponível como {@code ${@fmt.metodo(...)}}. Independe do
 * locale e do fuso da máquina: datas sempre em {@link Fuso#ZONA}, números com vírgula decimal.
 */
@Component("fmt")
public class Formatador {

    /** Texto exibido no lugar de um valor ausente. */
    public static final String AUSENTE = "—";

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");
    private static final DateTimeFormatter DATA_HORA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(Fuso.ZONA);
    private static final double CEM = 100.0;

    /**
     * Data e hora no fuso de Brasília.
     *
     * @param instante instante a exibir
     * @return ex.: {@code "24/11/2026 18:30"}; {@value #AUSENTE} se for nulo
     */
    public String dataHora(Instant instante) {
        return instante == null ? AUSENTE : DATA_HORA.format(instante);
    }

    /**
     * Fração como percentual, com no máximo uma casa decimal.
     *
     * @param fracao valor entre 0 e 1 (ex.: 0.6667)
     * @return ex.: {@code "66,7%"}, {@code "50%"}
     */
    public String percentual(double fracao) {
        return numero(fracao * CEM) + "%";
    }

    /**
     * Número com no máximo uma casa decimal e vírgula.
     *
     * @param valor número a exibir
     * @return ex.: {@code "12,5"}, {@code "12"}
     */
    public String numero(double valor) {
        // DecimalFormat não é thread-safe: um por chamada.
        DecimalFormat formato = new DecimalFormat("0.#", DecimalFormatSymbols.getInstance(PT_BR));
        formato.setRoundingMode(RoundingMode.HALF_UP);
        return formato.format(valor);
    }

    /**
     * Status da votação para exibição.
     *
     * @param status status do domínio ({@code "rascunho"}, {@code "aberta"}, ...)
     * @return o status com inicial maiúscula (ex.: {@code "Aberta"}); {@value #AUSENTE} se vazio
     */
    public String statusVotacao(String status) {
        if (status == null || status.isBlank()) {
            return AUSENTE;
        }
        return status.substring(0, 1).toUpperCase(PT_BR) + status.substring(1).toLowerCase(PT_BR);
    }

    /**
     * Status da premiação para exibição.
     *
     * @param status status do domínio
     * @return ex.: {@code ABERTA} → {@code "Aberta"}; {@value #AUSENTE} se nulo
     */
    public String statusPremiacao(StatusPremiacao status) {
        return status == null ? AUSENTE : statusVotacao(status.name());
    }
}

package br.unicamp.var.web.organizador;

import br.unicamp.var.web.comum.Fuso;
import java.time.Instant;
import java.time.format.DateTimeFormatter;

/**
 * Convite para os eleitores: o link absoluto da cédula e um texto pronto para colar no WhatsApp
 * (história #26, do lado do organizador).
 *
 * @param link  endereço absoluto da cédula, {@code .../eleitor/votacoes/{id}}
 * @param texto convite com título, prazo, link e instrução de acesso
 */
public record ConviteVotacao(String link, String texto) {

    private static final DateTimeFormatter PRAZO =
            DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm").withZone(Fuso.ZONA);

    /**
     * Monta o convite.
     *
     * @param pergunta pergunta da votação, usada como título
     * @param fim      fim do período, ou {@code null} se a votação não tiver período
     * @param link     endereço absoluto da cédula
     * @return o convite
     */
    public static ConviteVotacao de(String pergunta, Instant fim, String link) {
        String prazo = fim == null
                ? "Vote enquanto a votação estiver aberta."
                : "Vote até " + PRAZO.format(fim) + " (horário de Brasília).";
        String texto = "🗳️ *" + pergunta + "*\n"
                + prazo + "\n"
                + "Cédula: " + link + "\n"
                + "Entre com seu login e código de acesso.";
        return new ConviteVotacao(link, texto);
    }
}

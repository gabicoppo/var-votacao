package br.unicamp.var.auditoria;

import java.time.Instant;

/**
 * Um evento da trilha de auditoria de uma votação: quando aconteceu, quem fez,
 * o que foi feito e um detalhe opcional (RNF-07).
 *
 * <p>O detalhe descreve a operação (ex.: "Eleito: Chapa A" na apuração) e nunca
 * deve conter o conteúdo de votos (RNF-01).</p>
 *
 * @param quando  instante do evento, obtido do {@link java.time.Clock} do registro
 * @param autor   quem executou a operação (ex.: "sistema" ou o organizador); não vazio
 * @param tipo    tipo do evento
 * @param detalhe informação complementar; pode ser vazio, mas não nulo
 */
public record EventoAuditoria(Instant quando, String autor, TipoEvento tipo, String detalhe) {

    /**
     * Valida o evento.
     *
     * @throws IllegalArgumentException se algum campo for nulo ou se o autor
     *                                  for vazio
     */
    public EventoAuditoria {
        if (quando == null) {
            throw new IllegalArgumentException("quando nao pode ser nulo");
        }
        if (autor == null || autor.isBlank()) {
            throw new IllegalArgumentException("autor nao pode ser vazio");
        }
        if (tipo == null) {
            throw new IllegalArgumentException("tipo nao pode ser nulo");
        }
        if (detalhe == null) {
            throw new IllegalArgumentException("detalhe nao pode ser nulo");
        }
    }
}

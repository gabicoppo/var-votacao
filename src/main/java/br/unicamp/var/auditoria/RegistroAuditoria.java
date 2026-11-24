package br.unicamp.var.auditoria;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;

/**
 * Trilha de auditoria de uma votação, só de acréscimo (append-only): criação,
 * alteração de regra, abertura, encerramento, apuração e criação de 2º turno
 * ficam registrados com data/hora e autor (RNF-07).
 *
 * <p>Não há como remover nem alterar eventos, e a lista devolvida por
 * {@link #eventos()} é uma cópia imutável. O horário vem sempre do
 * {@link Clock} recebido no construtor, o que permite testes com relógio fixo.</p>
 *
 * <p>Votos <b>não</b> são registrados aqui: não existe {@link TipoEvento} para
 * voto, porque isso quebraria o sigilo (RNF-01).</p>
 */
public final class RegistroAuditoria {

    private final Clock relogio;
    private final List<EventoAuditoria> eventos = new ArrayList<>();

    /**
     * Cria um registro vazio.
     *
     * @param relogio relógio usado para datar os eventos
     * @throws IllegalArgumentException se o relógio for nulo
     */
    public RegistroAuditoria(Clock relogio) {
        if (relogio == null) {
            throw new IllegalArgumentException("relogio nao pode ser nulo");
        }
        this.relogio = relogio;
    }

    /**
     * Acrescenta um evento no fim da trilha, com o instante atual do relógio.
     *
     * @param autor   quem executou a operação; não vazio
     * @param tipo    tipo do evento
     * @param detalhe informação complementar, sem conteúdo de votos; pode ser
     *                vazio, mas não nulo
     * @return o evento registrado
     * @throws IllegalArgumentException se algum parâmetro for inválido (ver
     *                                  {@link EventoAuditoria})
     */
    public EventoAuditoria registrar(String autor, TipoEvento tipo, String detalhe) {
        EventoAuditoria evento = new EventoAuditoria(relogio.instant(), autor, tipo, detalhe);
        eventos.add(evento);
        return evento;
    }

    /**
     * Eventos registrados, na ordem em que aconteceram.
     *
     * @return cópia imutável da trilha, em ordem cronológica
     */
    public List<EventoAuditoria> eventos() {
        return List.copyOf(eventos);
    }
}

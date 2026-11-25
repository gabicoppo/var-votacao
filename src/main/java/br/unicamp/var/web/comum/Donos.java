package br.unicamp.var.web.comum;

import br.unicamp.var.auditoria.EventoAuditoria;
import br.unicamp.var.processo.Votacao;
import java.util.List;
import java.util.Optional;

/**
 * Quem pode ver e gerenciar cada votação e premiação: só o organizador que a criou.
 *
 * <p>O dono de uma votação é o autor do evento de criação na auditoria (RNF-07), registrado por
 * {@code Votacao.Builder.criadaPor}; o domínio não precisou mudar. O dono de uma premiação fica no
 * {@link RepositorioPremiacoes}. Votações criadas pelo sistema (autor {@value Votacao#AUTOR_SISTEMA})
 * e premiações sem dono valem para todos os organizadores. Por isso o cadastro não aceita o login
 * {@value Votacao#AUTOR_SISTEMA}.</p>
 */
public final class Donos {

    private Donos() {
        // só métodos estáticos
    }

    /**
     * Dono da votação. Lê a auditoria: chame dentro de {@code synchronized (votacao)}.
     *
     * @param votacao votação
     * @return o login de quem a criou, ou vazio se foi criada pelo sistema
     */
    public static Optional<String> dono(Votacao votacao) {
        List<EventoAuditoria> eventos = votacao.auditoria();
        if (eventos.isEmpty() || Votacao.AUTOR_SISTEMA.equals(eventos.get(0).autor())) {
            return Optional.empty();
        }
        return Optional.of(eventos.get(0).autor());
    }

    /**
     * Diz se o organizador pode ver e gerenciar a votação. Chame dentro de
     * {@code synchronized (votacao)}.
     *
     * @param votacao     votação
     * @param organizador login do organizador
     * @return {@code true} se ele é o dono ou se a votação não tem dono
     */
    public static boolean podeGerenciar(Votacao votacao, String organizador) {
        return dono(votacao).map(d -> d.equals(organizador)).orElse(true);
    }

    /**
     * Diz se o organizador pode ver e gerenciar a premiação.
     *
     * @param premiacoes  repositório de premiações
     * @param id          id do cadastro
     * @param organizador login do organizador
     * @return {@code true} se ele é o dono ou se a premiação não tem dono
     */
    public static boolean podeGerenciar(RepositorioPremiacoes premiacoes, String id, String organizador) {
        return premiacoes.donoDe(id).map(d -> d.equals(organizador)).orElse(true);
    }
}

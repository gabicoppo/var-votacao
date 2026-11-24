package br.unicamp.var.relatorio;

import br.unicamp.var.auditoria.EventoAuditoria;
import br.unicamp.var.auditoria.TipoEvento;
import br.unicamp.var.processo.PeriodoVotacao;
import br.unicamp.var.processo.Votacao;
import br.unicamp.var.urna.Sigilo;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Relatório de apuração de uma votação apurada (história #31), em texto pt-BR: pergunta,
 * descrição, turno, regra, sigilo, período, o quadro do resultado detalhado (história #29) e a
 * trilha de auditoria com data/hora e autor (RNF-07).
 *
 * <p>O relatório <b>nunca</b> inclui o voto de ninguém, nem em votação identificada: a
 * consulta do voto nominal fica para a tela (Fase 2), e o relatório pode circular fora da
 * comissão (RNF-01). Datas saem no formato {@code dd/MM/yyyy HH:mm}, no fuso
 * {@code America/Sao_Paulo}, qualquer que seja o fuso da máquina.</p>
 *
 * <p>Fica num pacote próprio, acima de {@code processo}, para não criar ciclo entre
 * {@code processo} e {@code resultado}. É um valor imutável: o texto é montado em
 * {@link #de(Votacao)} e não muda se a votação mudar depois (ex.: ao criar o 2º turno).</p>
 */
public final class RelatorioApuracao {

    private static final DateTimeFormatter DATA_HORA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.of("America/Sao_Paulo"));
    private static final String STATUS_APURADA = "apurada";

    private final String texto;

    private RelatorioApuracao(String texto) {
        this.texto = texto;
    }

    /**
     * Monta o relatório de uma votação apurada.
     *
     * @param votacao votação já apurada
     * @return o relatório
     * @throws IllegalArgumentException se a votação for nula
     * @throws IllegalStateException    se a votação ainda não foi apurada
     */
    public static RelatorioApuracao de(Votacao votacao) {
        if (votacao == null) {
            throw new IllegalArgumentException("votacao nao pode ser nula");
        }
        if (!STATUS_APURADA.equals(votacao.status())) {
            throw new IllegalStateException("relatorio so existe para votacao apurada");
        }
        StringBuilder relatorio = new StringBuilder("Relatório de apuração\n");
        relatorio.append("Votação: ").append(votacao.pergunta()).append('\n');
        if (!votacao.descricao().isEmpty()) {
            relatorio.append("Descrição: ").append(votacao.descricao()).append('\n');
        }
        relatorio.append("Turno: ").append(votacao.turno()).append('º');
        votacao.turnoAnteriorId().ifPresent(anterior -> relatorio.append(" (1º turno: ").append(anterior).append(')'));
        relatorio.append('\n');
        relatorio.append("Regra: ").append(votacao.regra().descricao()).append('\n');
        relatorio.append("Sigilo: ").append(sigilo(votacao.sigilo())).append('\n');
        votacao.periodo().ifPresent(periodo -> relatorio.append("Período: ").append(periodo(periodo)).append('\n'));
        relatorio.append('\n').append(votacao.resultadoDetalhado().formatar()).append("\n\n");
        relatorio.append("Auditoria");
        for (EventoAuditoria evento : votacao.auditoria()) {
            relatorio.append('\n').append(evento(evento));
        }
        return new RelatorioApuracao(relatorio.toString());
    }

    /**
     * Texto do relatório, com as linhas separadas por {@code \n}.
     *
     * @return o relatório formatado
     */
    public String formatar() {
        return texto;
    }

    private static String sigilo(Sigilo sigilo) {
        return switch (sigilo) {
            case SECRETO -> "secreto";
            case IDENTIFICADO -> "identificado (voto nominal)";
        };
    }

    private static String periodo(PeriodoVotacao periodo) {
        return data(periodo.inicio()) + " a " + data(periodo.fim());
    }

    private static String evento(EventoAuditoria evento) {
        String linha = data(evento.quando()) + " - " + evento.autor() + " - " + nome(evento.tipo());
        return evento.detalhe().isEmpty() ? linha : linha + " (" + evento.detalhe() + ")";
    }

    private static String data(Instant instante) {
        return DATA_HORA.format(instante);
    }

    private static String nome(TipoEvento tipo) {
        return switch (tipo) {
            case CRIADA -> "votação criada";
            case REGRA_ALTERADA -> "regra alterada";
            case ABERTA -> "votação aberta";
            case ENCERRADA -> "votação encerrada";
            case APURADA -> "votação apurada";
            case SEGUNDO_TURNO_CRIADO -> "2º turno criado";
        };
    }
}

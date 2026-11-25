package br.unicamp.var.web.resultado;

import br.unicamp.var.apuracao2turno.ResultadoApuracao;
import br.unicamp.var.processo.PeriodoVotacao;
import br.unicamp.var.processo.Votacao;
import br.unicamp.var.relatorio.RelatorioApuracao;
import br.unicamp.var.urna.Sigilo;
import br.unicamp.var.web.comum.RepositorioVotacoes;
import java.time.Clock;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Consultas das telas de resultado, telão, andamento e relatório de uma votação (histórias #29, #21 e #31).
 *
 * <p>Regras de acesso: o organizador vê sempre; o eleitor só se estiver na lista de aptos da votação
 * ({@link AcessoNegadoException}); id inexistente dá {@link RecursoNaoEncontradoException}.</p>
 *
 * <p>Toda leitura acontece dentro de {@code synchronized (votacao)}, porque {@link Votacao} não é
 * thread-safe. {@link Votacao#apurar()} só é chamado com a votação já apurada: nesse estado ele
 * devolve o resultado guardado, sem efeito colateral. Em qualquer outro estado ele mudaria a votação,
 * então as consultas nunca o chamam. Antes da apuração só sai a participação (RNF-01).</p>
 */
@Service
public class ConsultaResultados {

    private static final String APURADA = "apurada";
    private static final String VOTACAO = "Votação";

    private final RepositorioVotacoes votacoes;
    private final Clock relogio;

    /**
     * Cria o serviço.
     *
     * @param votacoes repositório de votações
     * @param relogio  relógio da aplicação, para o prazo e a data de emissão
     */
    public ConsultaResultados(RepositorioVotacoes votacoes, Clock relogio) {
        this.votacoes = votacoes;
        this.relogio = relogio;
    }

    /**
     * Página de resultado: andamento antes da apuração, gráfico depois.
     *
     * @param id          id da votação
     * @param usuario     login de quem consulta
     * @param organizador se quem consulta é organizador
     * @return a página
     * @throws RecursoNaoEncontradoException se a votação não existir
     * @throws AcessoNegadoException         se o eleitor não for apto
     */
    public PaginaResultado pagina(String id, String usuario, boolean organizador) {
        Votacao votacao = buscar(id, usuario, organizador);
        CabecalhoVotacao cabecalho;
        Andamento andamento = null;
        GraficoResultado grafico = null;
        VotoDoEleitor voto = VotoDoEleitor.NAO_SE_APLICA;
        synchronized (votacao) {
            cabecalho = cabecalho(votacao);
            if (APURADA.equals(votacao.status())) {
                grafico = grafico(votacao);
            } else {
                andamento = andamento(votacao);
            }
            if (!organizador) {
                voto = votacao.jaVotou(usuario) ? VotoDoEleitor.CONTADO : VotoDoEleitor.NAO_VOTOU;
            }
        }
        PrimeiroTurno primeiroTurno = cabecalho.turnoAnteriorId() == null ? null
                : primeiroTurno(cabecalho.turnoAnteriorId());
        return new PaginaResultado(cabecalho, andamento, grafico, segundoTurnoDe(id).orElse(null), primeiroTurno,
                voto);
    }

    /**
     * Participação atual, para a atualização ao vivo.
     *
     * @param id          id da votação
     * @param usuario     login de quem consulta
     * @param organizador se quem consulta é organizador
     * @return status, votantes e aptos
     * @throws RecursoNaoEncontradoException se a votação não existir
     * @throws AcessoNegadoException         se o eleitor não for apto
     */
    public AndamentoJson andamento(String id, String usuario, boolean organizador) {
        Votacao votacao = buscar(id, usuario, organizador);
        synchronized (votacao) {
            return new AndamentoJson(votacao.status(), votacao.totalVotantes(), votacao.eleitorado().total());
        }
    }

    /**
     * Ata de apuração, só para votação apurada.
     *
     * @param id          id da votação
     * @param usuario     login de quem consulta
     * @param organizador se quem consulta é organizador
     * @return a ata, ou vazio se a votação ainda não foi apurada
     * @throws RecursoNaoEncontradoException se a votação não existir
     * @throws AcessoNegadoException         se o eleitor não for apto
     */
    public Optional<PaginaRelatorio> relatorio(String id, String usuario, boolean organizador) {
        Votacao votacao = buscar(id, usuario, organizador);
        synchronized (votacao) {
            if (!APURADA.equals(votacao.status())) {
                return Optional.empty();
            }
            String texto = RelatorioApuracao.de(votacao).formatar();
            return Optional.of(new PaginaRelatorio(votacao.id(), votacao.pergunta(), texto,
                    CodigoVerificacao.de(texto), relogio.instant()));
        }
    }

    private Votacao buscar(String id, String usuario, boolean organizador) {
        Votacao votacao = votacoes.buscar(id).orElseThrow(() -> new RecursoNaoEncontradoException(VOTACAO));
        if (!organizador) {
            synchronized (votacao) {
                if (!votacao.eleitorado().ehApto(usuario)) {
                    throw new AcessoNegadoException();
                }
            }
        }
        return votacao;
    }

    private CabecalhoVotacao cabecalho(Votacao votacao) {
        Optional<PeriodoVotacao> periodo = votacao.periodo();
        return new CabecalhoVotacao(votacao.id(), votacao.pergunta(), votacao.descricao(), votacao.status(),
                votacao.regra().descricao(), sigilo(votacao.sigilo()), votacao.turno(),
                votacao.turnoAnteriorId().orElse(null), periodo.map(PeriodoVotacao::inicio).orElse(null),
                periodo.map(PeriodoVotacao::fim).orElse(null));
    }

    private Andamento andamento(Votacao votacao) {
        String tempo = TempoRestante.texto(votacao.status(), votacao.periodo().orElse(null), relogio.instant());
        return Andamento.de(votacao.status(), votacao.totalVotantes(), votacao.eleitorado().total(), tempo);
    }

    /** Só é chamado com a votação apurada: aí {@code apurar()} devolve o resultado guardado. */
    private static GraficoResultado grafico(Votacao votacao) {
        ResultadoApuracao apuracao = votacao.apurar();
        return GraficoResultado.de(votacao.resultadoDetalhado(), apuracao, votacao.regra());
    }

    /** Resumo do 1º turno, se ele existir e estiver apurado; trava só a votação anterior. */
    private PrimeiroTurno primeiroTurno(String anteriorId) {
        Optional<Votacao> anterior = votacoes.buscar(anteriorId);
        if (anterior.isEmpty()) {
            return null;
        }
        Votacao votacao = anterior.get();
        synchronized (votacao) {
            if (!APURADA.equals(votacao.status())) {
                return null;
            }
            return new PrimeiroTurno(votacao.id(), votacao.pergunta(), grafico(votacao));
        }
    }

    /** Procura o 2º turno criado a partir desta votação; trava uma votação por vez. */
    private Optional<String> segundoTurnoDe(String id) {
        for (Votacao outra : votacoes.listar()) {
            synchronized (outra) {
                if (outra.turnoAnteriorId().filter(id::equals).isPresent()) {
                    return Optional.of(outra.id());
                }
            }
        }
        return Optional.empty();
    }

    private static String sigilo(Sigilo sigilo) {
        return switch (sigilo) {
            case SECRETO -> "Secreto";
            case IDENTIFICADO -> "Identificado (voto nominal)";
        };
    }
}

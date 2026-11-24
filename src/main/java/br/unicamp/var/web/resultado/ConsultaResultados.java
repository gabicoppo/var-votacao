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
 * Consultas das telas de resultado, andamento e relatório de uma votação (histórias #29 e #31).
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
        PaginaResultado pagina;
        synchronized (votacao) {
            CabecalhoVotacao cabecalho = cabecalho(votacao);
            if (APURADA.equals(votacao.status())) {
                ResultadoApuracao apuracao = votacao.apurar();
                pagina = new PaginaResultado(cabecalho, null,
                        GraficoResultado.de(votacao.resultadoDetalhado(), apuracao, votacao.regra()), null);
            } else {
                pagina = new PaginaResultado(cabecalho, andamento(votacao), null, null);
            }
        }
        return segundoTurnoDe(id)
                .map(segundo -> new PaginaResultado(pagina.votacao(), pagina.andamento(), pagina.grafico(), segundo))
                .orElse(pagina);
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
            return Optional.of(new PaginaRelatorio(votacao.id(), votacao.pergunta(),
                    RelatorioApuracao.de(votacao).formatar(), relogio.instant()));
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

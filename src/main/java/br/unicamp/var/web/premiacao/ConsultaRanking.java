package br.unicamp.var.web.premiacao;

import br.unicamp.var.top3.LinhaRanking;
import br.unicamp.var.top3.PremiacaoTop3;
import br.unicamp.var.top3.RankingTop3;
import br.unicamp.var.top3.StatusPremiacao;
import br.unicamp.var.web.comum.Formatador;
import br.unicamp.var.web.comum.PremiacaoCadastrada;
import br.unicamp.var.web.comum.RepositorioPremiacoes;
import br.unicamp.var.web.resultado.AcessoNegadoException;
import br.unicamp.var.web.resultado.Andamento;
import br.unicamp.var.web.resultado.AndamentoJson;
import br.unicamp.var.web.resultado.RecursoNaoEncontradoException;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

/**
 * Consulta do ranking de uma premiação (história #30): andamento antes da apuração; pódio, tabela
 * completa com os pontos de cada grupo (D15, D16) e critérios depois.
 *
 * <p>O organizador vê sempre; o eleitor só se for apto na premiação ({@link AcessoNegadoException}).
 * {@link PremiacaoTop3#apurar()} só é chamado com a premiação já apurada, quando ele devolve o mesmo
 * ranking sem mudar nada.</p>
 */
@Service
public class ConsultaRanking {

    private static final Formatador FMT = new Formatador();

    private final RepositorioPremiacoes premiacoes;

    /**
     * Cria o serviço.
     *
     * @param premiacoes repositório de premiações
     */
    public ConsultaRanking(RepositorioPremiacoes premiacoes) {
        this.premiacoes = premiacoes;
    }

    /**
     * Página do ranking.
     *
     * @param id          id da premiação
     * @param usuario     login de quem consulta
     * @param organizador se quem consulta é organizador
     * @return a página
     * @throws RecursoNaoEncontradoException se a premiação não existir
     * @throws AcessoNegadoException         se o eleitor não for apto
     */
    public PaginaRanking pagina(String id, String usuario, boolean organizador) {
        PremiacaoTop3 p = buscar(id, usuario, organizador);
        synchronized (p) {
            Andamento andamento = andamento(p);
            if (p.status() != StatusPremiacao.APURADA) {
                return new PaginaRanking(id, p.titulo(), p.descricao(), p.status(), andamento, new Podio(List.of()),
                        List.of(), List.of(), List.of());
            }
            RankingTop3 ranking = p.apurar();
            List<String> grupos = p.categorias().stream().map(c -> c.nome()).toList();
            List<LinhaTabelaRanking> linhas = ranking.linhas().stream().map(l -> linha(l, grupos)).toList();
            return new PaginaRanking(id, p.titulo(), p.descricao(), p.status(), andamento, Podio.de(ranking), grupos,
                    linhas, ranking.criterios());
        }
    }

    /**
     * Participação atual, para a atualização ao vivo.
     *
     * @param id          id da premiação
     * @param usuario     login de quem consulta
     * @param organizador se quem consulta é organizador
     * @return status, cédulas e eleitores aptos
     * @throws RecursoNaoEncontradoException se a premiação não existir
     * @throws AcessoNegadoException         se o eleitor não for apto
     */
    public AndamentoJson andamento(String id, String usuario, boolean organizador) {
        PremiacaoTop3 p = buscar(id, usuario, organizador);
        synchronized (p) {
            return andamento(p).json();
        }
    }

    private PremiacaoTop3 buscar(String id, String usuario, boolean organizador) {
        PremiacaoTop3 premiacao = premiacoes.buscar(id).map(PremiacaoCadastrada::premiacao)
                .orElseThrow(() -> new RecursoNaoEncontradoException(GestaoPremiacoes.PREMIACAO));
        if (!organizador && !premiacao.ehApto(usuario)) {
            throw new AcessoNegadoException();
        }
        return premiacao;
    }

    private static Andamento andamento(PremiacaoTop3 p) {
        return Andamento.de(p.status().name().toLowerCase(Locale.ROOT), p.totalCedulas(), p.totalEleitores(), "");
    }

    private static LinhaTabelaRanking linha(LinhaRanking l, List<String> grupos) {
        List<String> porGrupo = grupos.stream()
                .map(g -> FMT.numero(l.pontosPorCategoria().getOrDefault(g, 0.0))).toList();
        return new LinhaTabelaRanking(l.colocacao(), l.candidato(), FMT.numero(l.pontos()), l.primeirosLugares(),
                l.segundosLugares(), l.terceirosLugares(), porGrupo);
    }
}

package br.unicamp.var.web.premiacao;

import br.unicamp.var.eleitorado.Eleitor;
import br.unicamp.var.top3.PremiacaoTop3;
import br.unicamp.var.web.comum.ContasUsuarios;
import br.unicamp.var.web.comum.MensagensErro;
import br.unicamp.var.web.comum.PremiacaoCadastrada;
import br.unicamp.var.web.comum.RepositorioPremiacoes;
import br.unicamp.var.web.resultado.Andamento;
import br.unicamp.var.web.resultado.RecursoNaoEncontradoException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.springframework.stereotype.Service;

/**
 * Casos de uso do organizador para premiações Top 3 (história #23): criar pelo formulário (com as
 * contas de eleitor), ver a gestão e conduzir o ciclo rascunho → aberta → encerrada → apurada.
 */
@Service
public class GestaoPremiacoes {

    /** Recurso usado nas páginas de não encontrado. */
    static final String PREMIACAO = "Premiação";

    private static final int JORNALISTAS_EXEMPLO = 3;
    private static final int ELEITORES_EXEMPLO = 12;
    private static final List<String> JOGADORES_EXEMPLO =
            List.of("Beto Farias", "Gui Moreira", "Jota Nunes", "Kadu Ribeiro", "Tiago Paz");

    private final RepositorioPremiacoes premiacoes;
    private final ContasUsuarios contas;

    /**
     * Cria o serviço.
     *
     * @param premiacoes repositório de premiações
     * @param contas     contas, para criar o acesso dos eleitores
     */
    public GestaoPremiacoes(RepositorioPremiacoes premiacoes, ContasUsuarios contas) {
        this.premiacoes = premiacoes;
        this.contas = contas;
    }

    /**
     * Formulário com um cenário fictício: Jornalistas (peso 2) para e01–e03, Torcedores (peso 1) para
     * e04–e12 e cinco jogadores inventados.
     *
     * @return o formulário preenchido
     */
    public FormularioPremiacao exemplo() {
        FormularioPremiacao f = new FormularioPremiacao();
        f.setTitulo("Craque da Final 2026");
        f.setDescricao("Escolha, em ordem, os três jogadores que mais decidiram a final.");
        f.setCandidatos(String.join("\n", JOGADORES_EXEMPLO));
        f.setCategorias("Jornalistas;2\nTorcedores;1");
        f.setEleitores(IntStream.rangeClosed(1, ELEITORES_EXEMPLO)
                .mapToObj(i -> {
                    String id = String.format("e%02d", i);
                    String grupo = i <= JORNALISTAS_EXEMPLO ? "Jornalistas" : "Torcedores";
                    return id + ";" + contas.nomeDe(id).orElse("Eleitor " + id) + ";" + grupo;
                })
                .collect(Collectors.joining("\n")));
        return f;
    }

    /**
     * Cria a premiação em rascunho e garante a conta de cada eleitor.
     *
     * @param f formulário digitado
     * @return o id e os códigos das contas novas, ou os erros por campo
     */
    public ResultadoCriacaoPremiacao criar(FormularioPremiacao f) {
        List<ErroCampo> erros = ConversorFormularioPremiacao.validar(f);
        if (!erros.isEmpty()) {
            return new ResultadoCriacaoPremiacao(null, erros, List.of());
        }
        PremiacaoCadastrada cadastrada = premiacoes.salvar(ConversorFormularioPremiacao.paraPremiacao(f));
        List<AcessoPremiacao> acessos = new ArrayList<>();
        for (Eleitor eleitor : ConversorFormularioPremiacao.eleitores(f)) {
            contas.garantirContaEleitor(eleitor.id(), eleitor.nome())
                    .ifPresent(codigo -> acessos.add(new AcessoPremiacao(eleitor.id(), eleitor.nome(), codigo)));
        }
        return new ResultadoCriacaoPremiacao(cadastrada.id(), List.of(), acessos);
    }

    /**
     * Dados da tela de gestão.
     *
     * @param id id da premiação
     * @return a gestão
     * @throws RecursoNaoEncontradoException se a premiação não existir
     */
    public DetalhePremiacao detalhe(String id) {
        PremiacaoCadastrada cadastrada = buscar(id);
        PremiacaoTop3 p = cadastrada.premiacao();
        synchronized (p) {
            List<GrupoPremiacao> grupos = p.categorias().stream()
                    .map(c -> new GrupoPremiacao(c.nome(), peso(c.peso()))).toList();
            Andamento andamento = Andamento.de(p.status().name().toLowerCase(Locale.ROOT), p.totalCedulas(),
                    p.totalEleitores(), "");
            return new DetalhePremiacao(cadastrada.id(), p.titulo(), p.descricao(), p.status(),
                    cadastrada.criadaEm(), p.candidatos(), grupos, andamento);
        }
    }

    /**
     * Título da premiação, para a página de códigos de acesso.
     *
     * @param id id da premiação
     * @return o título
     * @throws RecursoNaoEncontradoException se a premiação não existir
     */
    public String titulo(String id) {
        return buscar(id).premiacao().titulo();
    }

    /**
     * Abre a premiação.
     *
     * @param id id da premiação
     * @throws OperacaoRecusadaException se não estiver em rascunho
     */
    public void abrir(String id) {
        executar(id, PremiacaoTop3::abrir);
    }

    /**
     * Encerra a premiação.
     *
     * @param id id da premiação
     * @throws OperacaoRecusadaException se não estiver aberta
     */
    public void encerrar(String id) {
        executar(id, PremiacaoTop3::encerrar);
    }

    /**
     * Apura a premiação e publica o ranking.
     *
     * @param id id da premiação
     * @throws OperacaoRecusadaException se não estiver encerrada
     */
    public void apurar(String id) {
        executar(id, PremiacaoTop3::apurar);
    }

    private void executar(String id, Consumer<PremiacaoTop3> acao) {
        PremiacaoTop3 premiacao = buscar(id).premiacao();
        try {
            acao.accept(premiacao);
        } catch (IllegalStateException | IllegalArgumentException e) {
            throw new OperacaoRecusadaException(MensagensErro.amigavel(e));
        }
    }

    private PremiacaoCadastrada buscar(String id) {
        return premiacoes.buscar(id).orElseThrow(() -> new RecursoNaoEncontradoException(PREMIACAO));
    }

    /** Peso com até três casas e vírgula, sem zeros à direita: 2 → "2", 1.5 → "1,5". */
    static String peso(double peso) {
        return new DecimalFormat("0.###", DecimalFormatSymbols.getInstance(Locale.forLanguageTag("pt-BR")))
                .format(peso);
    }
}

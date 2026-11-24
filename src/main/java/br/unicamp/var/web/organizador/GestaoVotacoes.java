package br.unicamp.var.web.organizador;

import br.unicamp.var.apuracao2turno.ResultadoApuracao;
import br.unicamp.var.eleitorado.Eleitor;
import br.unicamp.var.processo.PeriodoVotacao;
import br.unicamp.var.processo.Votacao;
import br.unicamp.var.urna.Sigilo;
import br.unicamp.var.web.comum.ContasUsuarios;
import br.unicamp.var.web.comum.MensagensErro;
import br.unicamp.var.web.comum.PremiacaoCadastrada;
import br.unicamp.var.web.comum.RepositorioPremiacoes;
import br.unicamp.var.web.comum.RepositorioVotacoes;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * Casos de uso da área do organizador: criar votação, conduzir o ciclo (abrir, encerrar, apurar,
 * 2º turno) e montar os <em>view models</em> das telas (histórias #19 a #24, RNF-07).
 *
 * <p>{@link Votacao} não é thread-safe: toda leitura e escrita acontece dentro de
 * {@code synchronized (votacao)}, e as telas recebem só records já calculados. Erros do domínio
 * viram {@link OperacaoRecusadaException} com a frase de {@link MensagensErro#amigavel}.</p>
 */
@Service
public class GestaoVotacoes {

    /** Status aceitos no filtro do painel, na ordem das abas. */
    static final List<String> STATUS = List.of("rascunho", "aberta", "encerrada", "apurada");
    /** Ordem dos cards no painel: primeiro o que pede ação do organizador. */
    private static final List<String> PRIORIDADE = List.of("aberta", "encerrada", "rascunho", "apurada");
    /** Filtro que mostra todas as votações. */
    static final String TODAS = "todas";

    private static final Map<String, String> ROTULOS_ABAS = Map.of(
            TODAS, "Todas", "rascunho", "Rascunho", "aberta", "Abertas", "encerrada", "Encerradas",
            "apurada", "Apuradas");

    private final RepositorioVotacoes votacoes;
    private final RepositorioPremiacoes premiacoes;
    private final ContasUsuarios contas;
    private final Clock relogio;

    /**
     * @param votacoes   repositório de votações
     * @param premiacoes repositório de premiações (só leitura, para o painel)
     * @param contas     contas de usuários, para criar as contas dos eleitores
     * @param relogio    relógio da aplicação
     */
    public GestaoVotacoes(RepositorioVotacoes votacoes, RepositorioPremiacoes premiacoes, ContasUsuarios contas,
                          Clock relogio) {
        this.votacoes = votacoes;
        this.premiacoes = premiacoes;
        this.contas = contas;
        this.relogio = relogio;
    }

    /**
     * Valida o formulário, cria a votação em rascunho, salva e garante uma conta para cada eleitor
     * (histórias #19 e #24).
     *
     * @param formulario dados digitados
     * @param autor      organizador logado
     * @return o id e os códigos de acesso das contas novas, ou os erros por campo
     */
    public ResultadoCriacao criar(FormularioVotacao formulario, String autor) {
        List<ErroCampo> erros = ConversorFormularioVotacao.validar(formulario);
        if (!erros.isEmpty()) {
            return new ResultadoCriacao(null, erros, List.of());
        }
        Votacao votacao = ConversorFormularioVotacao.paraVotacao(formulario, relogio, autor);
        votacoes.salvar(votacao);
        List<AcessoEleitor> acessos = new ArrayList<>();
        for (Eleitor eleitor : votacao.eleitorado().listar()) {
            contas.garantirContaEleitor(eleitor.id(), eleitor.nome())
                    .ifPresent(codigo -> acessos.add(new AcessoEleitor(eleitor.id(), eleitor.nome(), codigo)));
        }
        return new ResultadoCriacao(votacao.id(), List.of(), acessos);
    }

    /**
     * Abre a votação (rascunho → aberta).
     *
     * @param id    id da votação
     * @param autor organizador logado, registrado na auditoria
     * @throws VotacaoNaoEncontradaException se a votação não existir
     * @throws OperacaoRecusadaException     se o domínio recusar
     */
    public void abrir(String id, String autor) {
        executar(id, votacao -> votacao.abrir(autor));
    }

    /**
     * Encerra a votação (aberta → encerrada).
     *
     * @param id    id da votação
     * @param autor organizador logado, registrado na auditoria
     * @throws VotacaoNaoEncontradaException se a votação não existir
     * @throws OperacaoRecusadaException     se o domínio recusar
     */
    public void encerrar(String id, String autor) {
        executar(id, votacao -> votacao.encerrar(autor));
    }

    /**
     * Apura a votação (encerrada → apurada).
     *
     * @param id    id da votação
     * @param autor organizador logado, registrado na auditoria
     * @return a situação apurada, como "Eleito: Chapa A"
     * @throws VotacaoNaoEncontradaException se a votação não existir
     * @throws OperacaoRecusadaException     se o domínio recusar
     */
    public String apurar(String id, String autor) {
        Votacao votacao = buscar(id);
        synchronized (votacao) {
            try {
                votacao.apurar(autor);
                return votacao.resultadoDetalhado().situacao();
            } catch (IllegalArgumentException | IllegalStateException e) {
                throw recusada(e);
            }
        }
    }

    /**
     * Cria e salva o 2º turno da votação, ligado a ela (história #21).
     *
     * @param id    id da votação apurada com resultado de 2º turno
     * @param autor organizador logado, registrado na auditoria das duas votações
     * @return a nova votação, em rascunho
     * @throws VotacaoNaoEncontradaException se a votação não existir
     * @throws OperacaoRecusadaException     se não houver 2º turno a criar ou se ele já existir
     */
    public Votacao criarSegundoTurno(String id, String autor) {
        Votacao votacao = buscar(id);
        Votacao segundoTurno;
        synchronized (votacao) {
            try {
                segundoTurno = votacao.criarSegundoTurno(autor);
            } catch (IllegalArgumentException | IllegalStateException e) {
                throw recusada(e);
            }
        }
        return votacoes.salvar(segundoTurno);
    }

    /**
     * Pergunta da votação, para títulos e mensagens.
     *
     * @param id id da votação
     * @return a pergunta
     * @throws VotacaoNaoEncontradaException se a votação não existir
     */
    public String pergunta(String id) {
        Votacao votacao = buscar(id);
        synchronized (votacao) {
            return votacao.pergunta();
        }
    }

    /**
     * Monta o painel do organizador.
     *
     * @param filtro status das votações a listar ("rascunho", "aberta"...); outro valor lista todas
     * @return o painel
     */
    public PainelOrganizador painel(String filtro) {
        String ativo = filtro != null && STATUS.contains(filtro) ? filtro : TODAS;
        List<CartaoVotacao> todas = votacoes.listar().stream()
                .map(GestaoVotacoes::cartao)
                .sorted(Comparator.comparingInt(cartao -> PRIORIDADE.indexOf(cartao.status())))
                .toList();
        Map<String, Long> porStatus = todas.stream()
                .collect(Collectors.groupingBy(CartaoVotacao::status, Collectors.counting()));
        List<PainelOrganizador.Aba> abas = new ArrayList<>();
        abas.add(new PainelOrganizador.Aba(TODAS, ROTULOS_ABAS.get(TODAS), todas.size(), TODAS.equals(ativo)));
        for (String status : STATUS) {
            abas.add(new PainelOrganizador.Aba(status, ROTULOS_ABAS.get(status),
                    porStatus.getOrDefault(status, 0L).intValue(), status.equals(ativo)));
        }
        List<CartaoVotacao> filtradas = todas.stream()
                .filter(cartao -> TODAS.equals(ativo) || cartao.status().equals(ativo))
                .toList();
        int totalVotos = todas.stream().mapToInt(CartaoVotacao::votantes).sum();
        List<PainelOrganizador.CartaoPremiacao> cartoesPremiacao = premiacoes.listar().stream()
                .map(GestaoVotacoes::cartao)
                .toList();
        return new PainelOrganizador(quantidade(porStatus, "aberta"), quantidade(porStatus, "rascunho"),
                quantidade(porStatus, "apurada"), totalVotos, abas, ativo, filtradas, todas.size(),
                cartoesPremiacao);
    }

    /**
     * Monta a tela de gestão de uma votação.
     *
     * @param id id da votação
     * @return o detalhe da votação
     * @throws VotacaoNaoEncontradaException se a votação não existir
     */
    public DetalheVotacao detalhe(String id) {
        Votacao votacao = buscar(id);
        String segundoTurnoId = segundoTurnoDe(id);
        synchronized (votacao) {
            String status = votacao.status();
            boolean apurada = "apurada".equals(status);
            // Em votação apurada, apurar() só devolve o resultado guardado, sem efeito colateral.
            boolean pedeSegundoTurno = apurada && votacao.apurar() instanceof ResultadoApuracao.SegundoTurno;
            boolean mostraEscolhas = votacao.sigilo() == Sigilo.IDENTIFICADO
                    && ("encerrada".equals(status) || apurada);
            PeriodoVotacao periodo = votacao.periodo().orElse(null);
            DetalheVotacao.Configuracao configuracao = new DetalheVotacao.Configuracao(
                    votacao.regra().descricao(), RotulosVotacao.sigilo(votacao.sigilo()),
                    periodo == null ? null : periodo.inicio(), periodo == null ? null : periodo.fim());
            return new DetalheVotacao(votacao.id(), votacao.pergunta(), votacao.descricao(), status,
                    votacao.turno(), votacao.turnoAnteriorId().orElse(null), configuracao,
                    votacao.totalVotantes(), votacao.eleitorado().total(),
                    apurada ? votacao.resultadoDetalhado().situacao() : null,
                    pedeSegundoTurno && segundoTurnoId == null, segundoTurnoId, mostraEscolhas,
                    presenca(votacao, mostraEscolhas), auditoria(votacao));
        }
    }

    /**
     * Lista de aptos e1..e12 com os nomes das contas de demonstração, para o botão
     * "Usar eleitores de demonstração".
     *
     * @return texto no formato {@code id;nome}, uma linha por eleitor
     */
    public String eleitoresDemonstracao() {
        StringBuilder texto = new StringBuilder();
        for (int i = 1; i <= 12; i++) {
            String login = String.format("e%02d", i);
            texto.append(login).append(';').append(contas.nomeDe(login).orElse("Eleitor " + i)).append('\n');
        }
        return texto.toString().strip();
    }

    private static List<DetalheVotacao.Presenca> presenca(Votacao votacao, boolean mostraEscolhas) {
        return votacao.eleitorado().listar().stream()
                .map(eleitor -> new DetalheVotacao.Presenca(eleitor.id(), eleitor.nome(),
                        votacao.jaVotou(eleitor.id()),
                        mostraEscolhas ? votacao.votoDe(eleitor.id()).orElse(null) : null))
                .toList();
    }

    private static List<DetalheVotacao.LinhaAuditoria> auditoria(Votacao votacao) {
        return votacao.auditoria().stream()
                .map(evento -> new DetalheVotacao.LinhaAuditoria(evento.quando(), evento.autor(),
                        RotulosVotacao.evento(evento.tipo()), RotulosVotacao.detalhe(evento.tipo(), evento.detalhe())))
                .toList();
    }

    private String segundoTurnoDe(String id) {
        for (Votacao outra : votacoes.listar()) {
            synchronized (outra) {
                if (outra.turnoAnteriorId().filter(id::equals).isPresent()) {
                    return outra.id();
                }
            }
        }
        return null;
    }

    private void executar(String id, Consumer<Votacao> operacao) {
        Votacao votacao = buscar(id);
        synchronized (votacao) {
            try {
                operacao.accept(votacao);
            } catch (IllegalArgumentException | IllegalStateException e) {
                throw recusada(e);
            }
        }
    }

    private Votacao buscar(String id) {
        return votacoes.buscar(id).orElseThrow(() -> new VotacaoNaoEncontradaException(id));
    }

    private static OperacaoRecusadaException recusada(RuntimeException e) {
        return new OperacaoRecusadaException(MensagensErro.amigavel(e), e);
    }

    private static CartaoVotacao cartao(Votacao votacao) {
        synchronized (votacao) {
            return CartaoVotacao.de(votacao);
        }
    }

    private static PainelOrganizador.CartaoPremiacao cartao(PremiacaoCadastrada cadastrada) {
        return new PainelOrganizador.CartaoPremiacao(cadastrada.id(), cadastrada.premiacao().titulo(),
                cadastrada.premiacao().status(), cadastrada.premiacao().totalCedulas());
    }

    private static int quantidade(Map<String, Long> porStatus, String status) {
        return porStatus.getOrDefault(status, 0L).intValue();
    }
}

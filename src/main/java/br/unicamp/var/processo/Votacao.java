package br.unicamp.var.processo;

import br.unicamp.var.apuracao.ContagemVotos;
import br.unicamp.var.apuracao.RegraApuracao;
import br.unicamp.var.apuracao2turno.ResultadoApuracao;
import br.unicamp.var.cadastro.CadastroCandidatos;
import br.unicamp.var.cadastro.Candidato;
import br.unicamp.var.eleitorado.Eleitorado;
import br.unicamp.var.resultado.ResultadoDetalhado;
import br.unicamp.var.urna.Comprovante;
import br.unicamp.var.urna.Sigilo;
import br.unicamp.var.urna.Urna;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Votação com ciclo de vida controlado por estados (padrão State):
 * rascunho → aberta → encerrada → apurada.
 *
 * <p>O que cada operação pode fazer depende do estado atual (RNF-07): a regra
 * só muda em rascunho, o voto só é aceito com a votação aberta e a apuração só
 * acontece após o encerramento. Os métodos públicos apenas delegam ao
 * {@link EstadoVotacao} atual, que decide se a operação é permitida e para qual
 * estado a votação passa. A apuração em si é delegada à {@link RegraApuracao}
 * configurada (padrão Strategy).</p>
 *
 * <p>É criada pelo {@link Builder}, obtido em {@link #nova()} (história #19: pergunta,
 * opções ou candidatos do cadastro e descrição).</p>
 *
 * <p>Com um {@link PeriodoVotacao} configurado (história #20), a votação abre no início e
 * encerra no fim do período: o horário é lido do {@link Clock} da votação em
 * {@link #atualizarPeloRelogio()}, chamado também antes de cada voto. Abrir e encerrar
 * manualmente continuam permitidos (o organizador pode encerrar antes do fim).</p>
 *
 * <p>Os votos ficam numa {@link Urna} criada com o {@link Eleitorado} e o {@link Sigilo} da
 * votação (histórias #22, #24, #26 e #28): só eleitores aptos votam (RNF-05), cada um uma única
 * vez (RNF-02), e cada voto gera um {@link Comprovante}. A contagem por opção só sai da votação
 * na apuração, porque resultado parcial influencia o voto. Pergunta, opções, eleitorado, sigilo
 * e período são fixados na criação, e a regra só muda em rascunho (RNF-07).</p>
 *
 * <p>Fora do escopo: persistência e interface.</p>
 */
public final class Votacao {

    private static final int MINIMO_OPCOES = 2;

    private final String id;
    private final String pergunta;
    private final String descricao;
    private final List<String> opcoes;
    private final Eleitorado eleitorado;
    private final Urna urna;
    private final PeriodoVotacao periodo;
    private final Clock relogio;
    private final Instant criadaEm;
    private RegraApuracao regra;
    private EstadoVotacao estado = new Rascunho();

    private Votacao(Builder builder) {
        if (builder.pergunta == null || builder.pergunta.isBlank()) {
            throw new IllegalArgumentException("pergunta nao pode ser vazia");
        }
        if (builder.descricao == null) {
            throw new IllegalArgumentException("descricao nao pode ser nula");
        }
        if (builder.regra == null) {
            throw new IllegalArgumentException("regra de apuracao nao pode ser nula");
        }
        if (builder.eleitorado == null) {
            throw new IllegalArgumentException("eleitorado nao pode ser nulo");
        }
        if (builder.sigilo == null) {
            throw new IllegalArgumentException("sigilo nao pode ser nulo");
        }
        if (builder.relogio == null) {
            throw new IllegalArgumentException("relogio nao pode ser nulo");
        }
        this.id = UUID.randomUUID().toString();
        this.pergunta = builder.pergunta.strip();
        this.descricao = builder.descricao.strip();
        this.opcoes = validarOpcoes(builder.opcoes);
        this.regra = builder.regra;
        this.eleitorado = builder.eleitorado;
        this.periodo = builder.periodo;
        this.relogio = builder.relogio;
        this.criadaEm = relogio.instant();
        this.urna = new Urna(eleitorado, opcoes, builder.sigilo, relogio);
    }

    /**
     * Começa a configuração de uma nova votação.
     *
     * @return builder vazio; pergunta, opções, regra e eleitorado são obrigatórios
     */
    public static Builder nova() {
        return new Builder();
    }

    private static List<String> validarOpcoes(List<String> opcoes) {
        if (opcoes == null || opcoes.size() < MINIMO_OPCOES) {
            throw new IllegalArgumentException("deve haver ao menos " + MINIMO_OPCOES + " opcoes");
        }
        Set<String> vistas = new HashSet<>();
        for (String opcao : opcoes) {
            if (opcao == null || opcao.isBlank()) {
                throw new IllegalArgumentException("opcao nao pode ser vazia");
            }
            if (!vistas.add(opcao.strip())) {
                throw new IllegalArgumentException("opcao repetida: " + opcao.strip());
            }
        }
        return opcoes.stream().map(String::strip).toList();
    }

    /**
     * Troca a regra de apuração. Só é permitido em rascunho.
     *
     * @param novaRegra nova regra de apuração
     * @throws IllegalArgumentException se a regra for nula
     * @throws IllegalStateException    se a votação não estiver em rascunho
     */
    public void alterarRegra(RegraApuracao novaRegra) {
        estado.alterarRegra(this, novaRegra);
    }

    /**
     * Abre a votação para receber votos (rascunho → aberta).
     *
     * @throws IllegalStateException se a votação não estiver em rascunho
     */
    public void abrir() {
        estado.abrir(this);
    }

    /**
     * Aplica o período ao ciclo (história #20): em rascunho, abre se o início já chegou; aberta,
     * encerra se o fim já chegou. Sem período configurado, não faz nada.
     */
    public void atualizarPeloRelogio() {
        estado.atualizarPeloRelogio(this);
    }

    /**
     * Registra o voto de um eleitor na urna (história #26). Só é permitido com a votação aberta
     * e, se houver período, dentro dele (história #20). Antes de registrar, aplica o relógio
     * ({@link #atualizarPeloRelogio()}).
     *
     * @param eleitorId identificador do eleitor na lista de aptos
     * @param opcao     opção escolhida
     * @return o comprovante do voto (história #28)
     * @throws IllegalArgumentException se o eleitor não for apto (RNF-05) ou a opção não existir
     * @throws IllegalStateException    se o horário estiver fora do período, se a votação não
     *                                  estiver aberta ou se o eleitor já votou (RNF-02)
     */
    public Comprovante registrarVoto(String eleitorId, String opcao) {
        atualizarPeloRelogio();
        if (periodo != null && !periodo.contem(relogio.instant())) {
            throw new IllegalStateException("voto fora do periodo de votacao");
        }
        return estado.registrarVoto(this, eleitorId, opcao);
    }

    /**
     * Encerra a votação (aberta → encerrada).
     *
     * @throws IllegalStateException se a votação não estiver aberta
     */
    public void encerrar() {
        estado.encerrar(this);
    }

    /**
     * Apura a votação com a regra configurada (encerrada → apurada). Chamado de
     * novo após a apuração, devolve o mesmo resultado.
     *
     * @return o resultado da apuração
     * @throws IllegalStateException se a votação ainda não foi encerrada
     */
    public ResultadoApuracao apurar() {
        return estado.apurar(this);
    }

    /**
     * Resultado com votos absolutos, percentuais, participação e situação (história #29).
     * Só existe depois da apuração.
     *
     * @return o resultado detalhado da apuração
     * @throws IllegalStateException se a votação ainda não foi apurada
     */
    public ResultadoDetalhado resultadoDetalhado() {
        return estado.resultadoDetalhado(this);
    }

    /**
     * Status atual da votação.
     *
     * @return "rascunho", "aberta", "encerrada" ou "apurada"
     */
    public String status() {
        return estado.nome();
    }

    /**
     * Pergunta da votação.
     *
     * @return a pergunta, sem espaços nas pontas
     */
    public String pergunta() {
        return pergunta;
    }

    /**
     * Opções da votação, na ordem de cadastro.
     *
     * @return lista imutável com as opções
     */
    public List<String> opcoes() {
        return List.copyOf(opcoes);
    }

    /**
     * Identificador único da votação, gerado na criação.
     *
     * @return UUID em texto
     */
    public String id() {
        return id;
    }

    /**
     * Descrição da votação (história #19).
     *
     * @return a descrição sem espaços nas pontas; vazia se não foi informada
     */
    public String descricao() {
        return descricao;
    }

    /**
     * Período de votação (história #20).
     *
     * @return o período, ou vazio se a votação só abre e encerra manualmente
     */
    public Optional<PeriodoVotacao> periodo() {
        return Optional.ofNullable(periodo);
    }

    /**
     * Instante de criação da votação, lido do relógio configurado.
     *
     * @return o instante de criação
     */
    public Instant criadaEm() {
        return criadaEm;
    }

    /**
     * Regra de apuração configurada.
     *
     * @return a regra atual
     */
    public RegraApuracao regra() {
        return regra;
    }

    /**
     * Lista de eleitores aptos (história #24).
     *
     * @return o eleitorado, que é imutável
     */
    public Eleitorado eleitorado() {
        return Eleitorado.de(eleitorado.listar());
    }

    /**
     * Sigilo da votação (história #22).
     *
     * @return {@link Sigilo#SECRETO}, a menos que outro tenha sido configurado
     */
    public Sigilo sigilo() {
        return urna.sigilo();
    }

    /**
     * Quantos eleitores já votaram. Não revela em quem.
     *
     * @return o número de votos registrados
     */
    public int totalVotantes() {
        return urna.totalVotos();
    }

    /**
     * Diz se o eleitor já votou (RNF-02).
     *
     * @param eleitorId identificador do eleitor
     * @return {@code true} se há voto registrado desse eleitor
     */
    public boolean jaVotou(String eleitorId) {
        return urna.jaVotou(eleitorId);
    }

    /**
     * Confere um comprovante emitido por esta votação (história #28).
     *
     * @param codigo código impresso no comprovante
     * @return {@code true} se o código foi emitido por esta votação
     */
    public boolean comprovanteValido(String codigo) {
        return urna.comprovanteValido(codigo);
    }

    /**
     * Voto de um eleitor, só em votação identificada (voto nominal, história #22).
     *
     * @param eleitorId identificador do eleitor
     * @return a opção votada, ou vazio se o eleitor não votou
     * @throws IllegalStateException se a votação for secreta
     */
    public Optional<String> votoDe(String eleitorId) {
        return urna.votoDe(eleitorId);
    }

    boolean periodoComecou() {
        return periodo != null && !periodo.naoComecou(relogio.instant());
    }

    boolean periodoTerminou() {
        return periodo != null && periodo.terminou(relogio.instant());
    }

    void mudarPara(EstadoVotacao novoEstado) {
        this.estado = novoEstado;
    }

    void substituirRegra(RegraApuracao novaRegra) {
        if (novaRegra == null) {
            throw new IllegalArgumentException("regra de apuracao nao pode ser nula");
        }
        this.regra = novaRegra;
    }

    Comprovante depositarNaUrna(String eleitorId, String opcao) {
        return urna.depositar(eleitorId, opcao);
    }

    ContagemVotos contagem() {
        return new ContagemVotos(urna.votosPorOpcao(), eleitorado.total());
    }

    /**
     * Monta uma {@link Votacao} em rascunho (história #19). Os campos são validados só em {@link #build()}.
     */
    public static final class Builder {

        private String pergunta;
        private String descricao = "";
        private List<String> opcoes;
        private RegraApuracao regra;
        private Eleitorado eleitorado;
        private Sigilo sigilo = Sigilo.SECRETO;
        private PeriodoVotacao periodo;
        private Clock relogio = Clock.systemUTC();

        private Builder() {
        }

        /**
         * Define a pergunta (obrigatória).
         *
         * @param pergunta pergunta da votação; espaços nas pontas são removidos
         * @return este builder
         */
        public Builder pergunta(String pergunta) {
            this.pergunta = pergunta;
            return this;
        }

        /**
         * Define a descrição (opcional; o padrão é vazia).
         *
         * @param descricao descrição da votação; espaços nas pontas são removidos
         * @return este builder
         */
        public Builder descricao(String descricao) {
            this.descricao = descricao;
            return this;
        }

        /**
         * Define as opções (obrigatórias).
         *
         * @param opcoes ao menos duas opções não vazias e sem repetição
         * @return este builder
         */
        public Builder opcoes(List<String> opcoes) {
            this.opcoes = opcoes == null ? null : new ArrayList<>(opcoes);
            return this;
        }

        /**
         * Usa como opções os nomes dos candidatos do cadastro, na ordem de cadastro (história #19).
         *
         * @param cadastro cadastro de candidatos
         * @return este builder
         * @throws IllegalArgumentException se o cadastro for nulo
         */
        public Builder opcoesDoCadastro(CadastroCandidatos cadastro) {
            if (cadastro == null) {
                throw new IllegalArgumentException("cadastro de candidatos nao pode ser nulo");
            }
            this.opcoes = cadastro.listar().stream().map(Candidato::nome).toList();
            return this;
        }

        /**
         * Define a regra de apuração (obrigatória).
         *
         * @param regra regra usada na apuração
         * @return este builder
         */
        public Builder regra(RegraApuracao regra) {
            this.regra = regra;
            return this;
        }

        /**
         * Define a lista de eleitores aptos (obrigatória; história #24, RNF-05).
         *
         * @param eleitorado eleitores que podem votar
         * @return este builder
         */
        public Builder eleitorado(Eleitorado eleitorado) {
            this.eleitorado = eleitorado;
            return this;
        }

        /**
         * Define o sigilo (opcional; o padrão é {@link Sigilo#SECRETO}, história #22).
         *
         * @param sigilo secreto ou identificado (voto nominal)
         * @return este builder
         */
        public Builder sigilo(Sigilo sigilo) {
            this.sigilo = sigilo;
            return this;
        }

        /**
         * Define o período de votação (opcional; sem período, a votação só abre e encerra manualmente).
         *
         * @param periodo período em que a votação recebe votos
         * @return este builder
         * @throws IllegalArgumentException se o período for nulo
         */
        public Builder periodo(PeriodoVotacao periodo) {
            if (periodo == null) {
                throw new IllegalArgumentException("periodo nao pode ser nulo");
            }
            this.periodo = periodo;
            return this;
        }

        /**
         * Define o relógio usado para datar a votação e aplicar o período
         * (opcional; o padrão é {@link Clock#systemUTC()}).
         *
         * @param relogio relógio da votação
         * @return este builder
         */
        public Builder relogio(Clock relogio) {
            this.relogio = relogio;
            return this;
        }

        /**
         * Cria a votação em rascunho, com zero votos para cada opção.
         *
         * @return a nova votação
         * @throws IllegalArgumentException se algum campo obrigatório faltar ou for inválido
         */
        public Votacao build() {
            return new Votacao(this);
        }
    }
}

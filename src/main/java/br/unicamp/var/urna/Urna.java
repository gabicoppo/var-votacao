package br.unicamp.var.urna;

import br.unicamp.var.eleitorado.Eleitorado;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.random.RandomGenerator;

/**
 * Recebe e guarda os votos de uma votação, garantindo o sigilo (RNF-01) e a
 * integridade (RNF-02) do voto. Atende às histórias #22 (voto secreto ou
 * identificado) e #28 (comprovante de voto).
 *
 * <p><b>Sigilo (RNF-01).</b> A urna guarda três estruturas independentes, sem
 * nenhuma ligação entre elas: a contagem por opção, o conjunto de quem votou e o
 * conjunto de códigos de comprovante emitidos. Não há horário por voto, e
 * {@link #quemVotou()} sai em ordem alfabética, para que a ordem não revele a
 * sequência dos votos. Só em {@link Sigilo#IDENTIFICADO} a urna guarda também
 * qual opção cada eleitor escolheu.</p>
 *
 * <p><b>Integridade (RNF-02).</b> Só eleitores aptos do {@link Eleitorado}
 * votam, cada um no máximo uma vez, e a soma da contagem é sempre igual a
 * {@link #totalVotos()} e ao número de comprovantes emitidos. Um voto recusado
 * não altera nada.</p>
 *
 * <p>Ids de eleitor e opções são comparados depois de {@link String#strip()}.
 * A urna não controla abertura e encerramento (isso fica com a votação) e não é
 * thread-safe.</p>
 */
public final class Urna {

    private static final int MINIMO_OPCOES = 2;
    /** Letras maiúsculas e dígitos, sem os ambíguos 0/O e 1/I. */
    private static final String ALFABETO_CODIGO = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int TAMANHO_CODIGO = 10;

    private final Eleitorado eleitorado;
    private final Sigilo sigilo;
    private final Clock relogio;
    private final RandomGenerator aleatorio;

    private final Map<String, Integer> contagem = new LinkedHashMap<>();
    private final Set<String> presenca = new TreeSet<>();
    private final Set<String> codigosEmitidos = new HashSet<>();
    /** Só é preenchido em votação {@link Sigilo#IDENTIFICADO}. */
    private final Map<String, String> votoPorEleitor = new HashMap<>();

    /**
     * Cria a urna vazia, com zero votos para cada opção.
     *
     * @param eleitorado eleitores aptos a votar nesta urna (#24)
     * @param opcoes     ao menos duas opções não vazias e sem repetição (após strip)
     * @param sigilo     se o voto é secreto ou identificado (#22)
     * @param relogio    fonte do horário gravado nos comprovantes (#28)
     * @throws IllegalArgumentException se algum argumento for nulo ou as opções forem inválidas
     */
    public Urna(Eleitorado eleitorado, List<String> opcoes, Sigilo sigilo, Clock relogio) {
        this(eleitorado, opcoes, sigilo, relogio, new SecureRandom());
    }

    /**
     * Construtor para testes, que permite controlar a geração dos códigos.
     */
    Urna(Eleitorado eleitorado, List<String> opcoes, Sigilo sigilo, Clock relogio, RandomGenerator aleatorio) {
        this.eleitorado = exigir(eleitorado, "eleitorado nao pode ser nulo");
        this.sigilo = exigir(sigilo, "sigilo nao pode ser nulo");
        this.relogio = exigir(relogio, "relogio nao pode ser nulo");
        this.aleatorio = exigir(aleatorio, "gerador aleatorio nao pode ser nulo");
        for (String opcao : validarOpcoes(opcoes)) {
            contagem.put(opcao, 0);
        }
    }

    private static <T> T exigir(T valor, String mensagem) {
        if (valor == null) {
            throw new IllegalArgumentException(mensagem);
        }
        return valor;
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
     * Registra o voto do eleitor e devolve o comprovante (#22, #28).
     *
     * <p>Todas as verificações são feitas antes de qualquer alteração: se o voto
     * for recusado, a urna fica como estava (RNF-02).</p>
     *
     * @param eleitorId id de um eleitor apto; espaços nas pontas são ignorados
     * @param opcao     uma das opções da urna; espaços nas pontas são ignorados
     * @return comprovante com código aleatório e o horário do {@link Clock}
     * @throws IllegalArgumentException se algum argumento for nulo, o eleitor não
     *                                  for apto ou a opção não existir
     * @throws IllegalStateException    se o eleitor já tiver votado
     */
    public Comprovante depositar(String eleitorId, String opcao) {
        exigir(eleitorId, "eleitor nao pode ser nulo");
        exigir(opcao, "opcao nao pode ser nula");
        String eleitor = eleitorId.strip();
        String escolha = opcao.strip();
        if (!eleitorado.ehApto(eleitor)) {
            throw new IllegalArgumentException("eleitor nao apto: " + eleitor);
        }
        if (!contagem.containsKey(escolha)) {
            throw new IllegalArgumentException("opcao inexistente: " + escolha);
        }
        if (presenca.contains(eleitor)) {
            throw new IllegalStateException("eleitor ja votou: " + eleitor);
        }
        String codigo = novoCodigo();
        contagem.merge(escolha, 1, Integer::sum);
        presenca.add(eleitor);
        codigosEmitidos.add(codigo);
        if (sigilo == Sigilo.IDENTIFICADO) {
            votoPorEleitor.put(eleitor, escolha);
        }
        return new Comprovante(codigo, relogio.instant());
    }

    /** Gera um código aleatório ainda não emitido por esta urna. */
    private String novoCodigo() {
        String codigo;
        do {
            StringBuilder sb = new StringBuilder(TAMANHO_CODIGO);
            for (int i = 0; i < TAMANHO_CODIGO; i++) {
                sb.append(ALFABETO_CODIGO.charAt(aleatorio.nextInt(ALFABETO_CODIGO.length())));
            }
            codigo = sb.toString();
        } while (codigosEmitidos.contains(codigo));
        return codigo;
    }

    /**
     * Diz se o eleitor já votou nesta urna (lista de presença, RNF-02).
     *
     * @param eleitorId id do eleitor; espaços nas pontas são ignorados
     * @return {@code true} se já votou; {@code false} se não votou ou se o id for nulo
     */
    public boolean jaVotou(String eleitorId) {
        return eleitorId != null && presenca.contains(eleitorId.strip());
    }

    /**
     * Número de votos registrados (RNF-02: igual à soma de {@link #votosPorOpcao()}).
     *
     * @return total de votos
     */
    public int totalVotos() {
        return presenca.size();
    }

    /**
     * Contagem de votos por opção, pronta para virar uma
     * {@code br.unicamp.var.apuracao.ContagemVotos}.
     *
     * @return mapa imutável com todas as opções (inclusive as com 0 votos), na
     *         ordem recebida no construtor
     */
    public Map<String, Integer> votosPorOpcao() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(contagem));
    }

    /**
     * Lista de presença: ids de quem já votou (RNF-01, RNF-02).
     *
     * @return conjunto imutável em ordem alfabética, nunca na ordem dos votos
     */
    public Set<String> quemVotou() {
        return Collections.unmodifiableSortedSet(new TreeSet<>(presenca));
    }

    /**
     * Sigilo configurado para esta urna (#22).
     *
     * @return {@link Sigilo#SECRETO} ou {@link Sigilo#IDENTIFICADO}
     */
    public Sigilo sigilo() {
        return sigilo;
    }

    /**
     * Opção escolhida por um eleitor, só em votação identificada (#22).
     *
     * @param eleitorId id do eleitor; espaços nas pontas são ignorados
     * @return a opção escolhida, ou vazio se o eleitor não votou ou o id for nulo
     * @throws IllegalStateException se a votação for {@link Sigilo#SECRETO} (RNF-01)
     */
    public Optional<String> votoDe(String eleitorId) {
        if (sigilo == Sigilo.SECRETO) {
            throw new IllegalStateException("voto secreto: nao e possivel consultar o voto de um eleitor");
        }
        if (eleitorId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(votoPorEleitor.get(eleitorId.strip()));
    }

    /**
     * Diz se o código foi emitido por esta urna, para o eleitor conferir que o
     * voto foi computado (#28).
     *
     * <p>Ignora espaços nas pontas e diferença entre maiúsculas e minúsculas,
     * porque o código é digitado pelo eleitor.</p>
     *
     * @param codigo código do comprovante
     * @return {@code true} se o código foi emitido aqui; {@code false} se for
     *         desconhecido ou nulo
     */
    public boolean comprovanteValido(String codigo) {
        return codigo != null && codigosEmitidos.contains(codigo.strip().toUpperCase(Locale.ROOT));
    }
}

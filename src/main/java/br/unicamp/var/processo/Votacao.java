package br.unicamp.var.processo;

import br.unicamp.var.apuracao.ContagemVotos;
import br.unicamp.var.apuracao.RegraApuracao;
import br.unicamp.var.apuracao2turno.ResultadoApuracao;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
 * <p>Fora do escopo: abertura/encerramento automático por horário, controle de
 * eleitor único, sigilo, persistência e interface.</p>
 */
public final class Votacao {

    private static final int MINIMO_OPCOES = 2;

    private final String pergunta;
    private final List<String> opcoes;
    private final int totalAptos;
    private final Map<String, Integer> votos = new LinkedHashMap<>();
    private RegraApuracao regra;
    private EstadoVotacao estado = new Rascunho();

    /**
     * Cria a votação em rascunho, com zero votos para cada opção.
     *
     * @param pergunta   pergunta da votação; espaços nas pontas são removidos
     * @param opcoes     ao menos duas opções não vazias e sem repetição
     * @param regra      regra usada na apuração
     * @param totalAptos número de pessoas aptas a votar
     * @throws IllegalArgumentException se algum parâmetro for inválido
     */
    public Votacao(String pergunta, List<String> opcoes, RegraApuracao regra, int totalAptos) {
        if (pergunta == null || pergunta.isBlank()) {
            throw new IllegalArgumentException("pergunta nao pode ser vazia");
        }
        if (regra == null) {
            throw new IllegalArgumentException("regra de apuracao nao pode ser nula");
        }
        if (totalAptos <= 0) {
            throw new IllegalArgumentException("totalAptos deve ser maior que zero");
        }
        this.pergunta = pergunta.strip();
        this.opcoes = validarOpcoes(opcoes);
        this.regra = regra;
        this.totalAptos = totalAptos;
        for (String opcao : this.opcoes) {
            votos.put(opcao, 0);
        }
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
     * Registra um voto. Só é permitido com a votação aberta.
     *
     * @param opcao opção escolhida
     * @throws IllegalArgumentException se a opção não existir
     * @throws IllegalStateException    se a votação não estiver aberta ou se o
     *                                  número de votos já for igual ao de aptos
     */
    public void registrarVoto(String opcao) {
        estado.registrarVoto(this, opcao);
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

    void mudarPara(EstadoVotacao novoEstado) {
        this.estado = novoEstado;
    }

    void substituirRegra(RegraApuracao novaRegra) {
        if (novaRegra == null) {
            throw new IllegalArgumentException("regra de apuracao nao pode ser nula");
        }
        this.regra = novaRegra;
    }

    void contabilizarVoto(String opcao) {
        if (!votos.containsKey(opcao)) {
            throw new IllegalArgumentException("opcao inexistente: " + opcao);
        }
        int totalVotos = votos.values().stream().mapToInt(Integer::intValue).sum();
        if (totalVotos >= totalAptos) {
            throw new IllegalStateException("numero de votos ja atingiu o total de aptos");
        }
        votos.merge(opcao, 1, Integer::sum);
    }

    RegraApuracao regra() {
        return regra;
    }

    ContagemVotos contagem() {
        return new ContagemVotos(votos, totalAptos);
    }
}

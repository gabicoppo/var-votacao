package br.unicamp.var.top3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Votação de uma categoria de premiação, como Craque do Campeonato (#23).
 * Título, critério de avaliação, candidatos e grupos ficam fixos desde a criação.
 * Cada eleitor apto pode entregar uma única cédula. O ranking só é publicado após o encerramento (#30).
 */
public final class PremiacaoTop3 {

    private final String titulo;
    private final String descricao;
    private final List<String> candidatos;
    private final Set<String> nomesCandidatos;
    private final List<CategoriaEleitor> categorias;
    private final Map<String, CategoriaEleitor> grupoPorEleitor;
    private final Set<String> votantes = new HashSet<>();
    private final Map<String, List<VotoCategoria>> votosPorCategoria = new HashMap<>();
    private StatusPremiacao status = StatusPremiacao.RASCUNHO;
    private RankingTop3 ranking;

    /**
     * Cria a premiação em rascunho (#23).
     *
     * @param titulo nome da categoria de premiação
     * @param descricao critério apresentado ao eleitor, ou texto vazio
     * @param candidatos ao menos três nomes distintos
     * @param categorias grupos de votantes, com nomes únicos e pesos positivos
     * @param categoriaPorEleitor identificador de cada eleitor apto e nome de seu grupo
     * @throws IllegalArgumentException se faltar um campo ou a configuração for inválida
     */
    public PremiacaoTop3(String titulo, String descricao, List<String> candidatos,
                         List<CategoriaEleitor> categorias, Map<String, String> categoriaPorEleitor) {
        this.titulo = ValidacaoTop3.nome(titulo, "titulo");
        this.descricao = ValidacaoTop3.presente(descricao, "descricao");
        this.nomesCandidatos = validarCandidatos(candidatos);
        this.candidatos = List.copyOf(candidatos);
        Map<String, CategoriaEleitor> porNome = indexarCategorias(categorias);
        this.categorias = List.copyOf(categorias);
        Map<String, CategoriaEleitor> grupos = new HashMap<>();
        ValidacaoTop3.presente(categoriaPorEleitor, "mapa de eleitores").forEach((id, nome) -> {
            ValidacaoTop3.nome(id, "id do eleitor");
            CategoriaEleitor grupo = porNome.get(nome);
            if (grupo == null) {
                throw new IllegalArgumentException("categoria do eleitor inexistente");
            }
            grupos.put(id, grupo);
        });
        this.grupoPorEleitor = Map.copyOf(grupos);
    }

    private static Set<String> validarCandidatos(List<String> candidatos) {
        if (ValidacaoTop3.presente(candidatos, "lista de candidatos").size() < 3) {
            throw new IllegalArgumentException("deve haver ao menos tres candidatos");
        }
        Set<String> nomes = new HashSet<>();
        for (String candidato : candidatos) {
            if (!nomes.add(ValidacaoTop3.nome(candidato, "candidato"))) {
                throw new IllegalArgumentException("candidato repetido");
            }
        }
        return Set.copyOf(nomes);
    }

    private static Map<String, CategoriaEleitor> indexarCategorias(List<CategoriaEleitor> categorias) {
        if (ValidacaoTop3.presente(categorias, "lista de categorias").isEmpty()) {
            throw new IllegalArgumentException("deve haver ao menos uma categoria");
        }
        Map<String, CategoriaEleitor> porNome = new HashMap<>();
        for (CategoriaEleitor grupo : categorias) {
            ValidacaoTop3.presente(grupo, "grupo");
            if (porNome.put(grupo.nome(), grupo) != null) {
                throw new IllegalArgumentException("nome de categoria repetido");
            }
        }
        return porNome;
    }

    /** Abre a votação em rascunho (#23). */
    public synchronized void abrir() {
        exigirEstado(StatusPremiacao.RASCUNHO);
        status = StatusPremiacao.ABERTA;
    }

    /**
     * Registra a cédula com o peso do grupo do eleitor (#23).
     *
     * @param eleitorId identificador cadastrado na configuração
     * @param cedula três candidatos inscritos na premiação
     * @throws IllegalStateException se a votação estiver fechada ou o eleitor já tiver votado
     * @throws IllegalArgumentException se o eleitor, a cédula ou um candidato for inválido
     */
    public synchronized void registrarCedula(String eleitorId, CedulaTop3 cedula) {
        exigirEstado(StatusPremiacao.ABERTA);
        CategoriaEleitor grupo = grupoPorEleitor.get(ValidacaoTop3.nome(eleitorId, "id do eleitor"));
        if (grupo == null) {
            throw new IllegalArgumentException("eleitor nao apto");
        }
        if (votantes.contains(eleitorId)) {
            throw new IllegalStateException("eleitor ja votou");
        }
        List<VotoCategoria> votos = ValidacaoTop3.presente(cedula, "cedula").comPeso(grupo.peso());
        for (VotoCategoria voto : votos) {
            if (!nomesCandidatos.contains(voto.candidato())) {
                throw new IllegalArgumentException("candidato inexistente");
            }
        }
        votosPorCategoria.computeIfAbsent(grupo.nome(), nome -> new ArrayList<>()).addAll(votos);
        votantes.add(eleitorId);
    }

    /** Encerra o recebimento de cédulas da votação aberta (#23). */
    public synchronized void encerrar() {
        exigirEstado(StatusPremiacao.ABERTA);
        status = StatusPremiacao.ENCERRADA;
    }

    /**
     * Publica o resultado da votação encerrada (#30).
     *
     * @return ranking imutável, o mesmo nas consultas seguintes
     * @throws IllegalStateException se a votação ainda não foi encerrada ou se o total ponderado
     *                               exceder o maior valor finito representável por um double
     */
    public synchronized RankingTop3 apurar() {
        if (status != StatusPremiacao.APURADA) {
            exigirEstado(StatusPremiacao.ENCERRADA);
            ranking = new RankingTop3(titulo, descricao, candidatos, categorias, votosPorCategoria, votantes.size());
            status = StatusPremiacao.APURADA;
        }
        return ranking;
    }

    private void exigirEstado(StatusPremiacao esperado) {
        if (status != esperado) {
            throw new IllegalStateException("operacao invalida no estado atual da premiacao");
        }
    }

    /** Retorna o estado atual da premiação (#23). */
    public synchronized StatusPremiacao status() {
        return status;
    }

    /** Retorna o nome da categoria de premiação (#23). */
    public String titulo() {
        return titulo;
    }

    /** Retorna o critério de avaliação a apresentar na cédula e no ranking (#23, #30). */
    public String descricao() {
        return descricao;
    }

    /** Lista os candidatos na ordem de inscrição (#23). */
    public List<String> candidatos() {
        return candidatos;
    }

    /** Lista os grupos de votantes na ordem configurada (#23). */
    public List<CategoriaEleitor> categorias() {
        return categorias;
    }

    /** Conta as cédulas aceitas (#23, #30). */
    public synchronized int totalCedulas() {
        return votantes.size();
    }

    /** Indica se há cédula aceita para o identificador informado (#23). */
    public synchronized boolean jaVotou(String eleitorId) {
        return votantes.contains(eleitorId);
    }
}

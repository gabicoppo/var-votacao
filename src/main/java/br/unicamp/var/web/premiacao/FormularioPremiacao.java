package br.unicamp.var.web.premiacao;

/**
 * Dados digitados no formulário de nova premiação Top 3 (história #23). Os campos de lista são
 * textos com uma entrada por linha; quem interpreta é o {@link ConversorFormularioPremiacao}.
 * Os getters nunca devolvem {@code null}.
 */
public class FormularioPremiacao {

    private String titulo = "";
    private String descricao = "";
    private String candidatos = "";
    private String categorias = "";
    private String eleitores = "";

    /**
     * Título da premiação.
     *
     * @return ex.: {@code "Craque do Campeonato"}
     */
    public String getTitulo() {
        return titulo;
    }

    /**
     * Define o título.
     *
     * @param titulo título digitado; {@code null} vira vazio
     */
    public void setTitulo(String titulo) {
        this.titulo = texto(titulo);
    }

    /**
     * Critério de avaliação mostrado ao eleitor (opcional).
     *
     * @return o critério, ou vazio
     */
    public String getDescricao() {
        return descricao;
    }

    /**
     * Define o critério de avaliação.
     *
     * @param descricao texto digitado; {@code null} vira vazio
     */
    public void setDescricao(String descricao) {
        this.descricao = texto(descricao);
    }

    /**
     * Candidatos, um por linha.
     *
     * @return o texto digitado
     */
    public String getCandidatos() {
        return candidatos;
    }

    /**
     * Define os candidatos.
     *
     * @param candidatos um por linha; {@code null} vira vazio
     */
    public void setCandidatos(String candidatos) {
        this.candidatos = texto(candidatos);
    }

    /**
     * Grupos de votantes e pesos, uma linha {@code Nome;peso} por grupo.
     *
     * @return o texto digitado, ex.: {@code "Jornalistas;2\nTorcedores;1,0"}
     */
    public String getCategorias() {
        return categorias;
    }

    /**
     * Define os grupos e pesos.
     *
     * @param categorias linhas {@code Nome;peso}; {@code null} vira vazio
     */
    public void setCategorias(String categorias) {
        this.categorias = texto(categorias);
    }

    /**
     * Eleitores aptos, uma linha {@code id;nome;grupo} por eleitor.
     *
     * @return o texto digitado, ex.: {@code "e01;Ana Ribeiro;Jornalistas"}
     */
    public String getEleitores() {
        return eleitores;
    }

    /**
     * Define os eleitores.
     *
     * @param eleitores linhas {@code id;nome;grupo}; {@code null} vira vazio
     */
    public void setEleitores(String eleitores) {
        this.eleitores = texto(eleitores);
    }

    private static String texto(String valor) {
        return valor == null ? "" : valor;
    }
}

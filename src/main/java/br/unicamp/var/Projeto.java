package br.unicamp.var;

/**
 * Informacoes basicas do projeto VAR.
 * Classe de partida: pode ser removida quando as primeiras features chegarem.
 */
public final class Projeto {

    /** Nome oficial do produto. */
    public static final String NOME = "VAR - Votacao, Apuracao e Resultados";

    private Projeto() {
        // classe utilitaria: nao instanciar
    }

    /**
     * Retorna o nome do projeto.
     *
     * @return nome do projeto
     */
    public static String nome() {
        return NOME;
    }
}

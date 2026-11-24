package br.unicamp.var.top3;

/**
 * Grupo de votantes da premiação (#23), com o peso que aparece no ranking (#30).
 *
 * @param nome nome do grupo, por exemplo, Jornalistas ou Torcedores
 * @param peso multiplicador finito e positivo dos pontos de cada posição
 */
public record CategoriaEleitor(String nome, double peso) {

    /** Valida o nome e o peso do grupo (#23). */
    public CategoriaEleitor {
        ValidacaoTop3.nome(nome, "nome da categoria");
        ValidacaoTop3.validarPeso(peso);
    }
}

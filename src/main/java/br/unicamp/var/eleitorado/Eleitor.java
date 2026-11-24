package br.unicamp.var.eleitorado;

/**
 * Eleitor apto a votar numa votação (história #24).
 *
 * <p>A validação fica no construtor: não existe {@code Eleitor} com id ou nome
 * nulo, vazio ou só com espaços. Os dois campos são guardados sem espaços nas
 * pontas ({@link String#strip()}), para que " 123 " e "123" sejam o mesmo id.</p>
 *
 * @param id   identificador único do eleitor na votação (ex.: matrícula ou número de sócio)
 * @param nome nome do eleitor
 */
public record Eleitor(String id, String nome) {

    /**
     * Valida e normaliza os campos.
     *
     * @throws IllegalArgumentException se id ou nome forem nulos, vazios ou em branco
     */
    public Eleitor {
        id = normalizar(id, "id");
        nome = normalizar(nome, "nome");
    }

    private static String normalizar(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(campo + " do eleitor nao pode ser vazio");
        }
        return valor.strip();
    }
}

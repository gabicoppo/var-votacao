package br.unicamp.var.cadastro;

/**
 * Candidato de um processo de votacao.
 *
 * <p>A validacao fica no construtor: e impossivel existir um {@code Candidato}
 * com nome ou codigo vazio. Os dois campos sao guardados sem espacos nas pontas.
 *
 * @param nome   nome do candidato (nao pode ser nulo, vazio ou em branco)
 * @param codigo codigo do candidato, como "10" ou "P-01" (idem)
 */
public record Candidato(String nome, String codigo) {

    /**
     * Valida e normaliza os campos.
     *
     * @throws IllegalArgumentException se nome ou codigo forem nulos, vazios ou em branco
     */
    public Candidato {
        nome = normalizar(nome, "nome");
        codigo = normalizar(codigo, "codigo");
    }

    private static String normalizar(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("O " + campo + " nao pode ser nulo, vazio ou em branco");
        }
        return valor.strip();
    }
}
package br.unicamp.var.top3;

/** Valida os campos comuns da configuração e da cédula. */
final class ValidacaoTop3 {

    private ValidacaoTop3() {
    }

    static <T> T presente(T valor, String campo) {
        if (valor == null) {
            throw new IllegalArgumentException(campo + " nao pode ser nulo");
        }
        return valor;
    }

    static String nome(String valor, String campo) {
        if (presente(valor, campo).isBlank()) {
            throw new IllegalArgumentException(campo + " nao pode estar em branco");
        }
        return valor;
    }

    static void validarPeso(double peso) {
        if (!Double.isFinite(peso) || peso <= 0) {
            throw new IllegalArgumentException("peso deve ser finito e maior que zero");
        }
    }
}

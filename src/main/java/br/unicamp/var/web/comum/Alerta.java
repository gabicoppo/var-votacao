package br.unicamp.var.web.comum;

/**
 * Mensagem de retorno exibida no topo da página. Uso:
 * {@code redirectAttributes.addFlashAttribute("alerta", Alerta.sucesso("Votação aberta."))}; o
 * layout mostra o atributo {@code alerta}, se existir.
 *
 * @param tipo  tipo do alerta
 * @param texto texto para o usuário, em pt-BR
 */
public record Alerta(TipoAlerta tipo, String texto) {

    /**
     * Valida o alerta.
     *
     * @throws IllegalArgumentException se o tipo for nulo ou o texto vazio
     */
    public Alerta {
        if (tipo == null) {
            throw new IllegalArgumentException("tipo do alerta nao pode ser nulo");
        }
        if (texto == null || texto.isBlank()) {
            throw new IllegalArgumentException("texto do alerta nao pode ser vazio");
        }
    }

    /**
     * Alerta de sucesso.
     *
     * @param texto texto para o usuário
     * @return o alerta
     */
    public static Alerta sucesso(String texto) {
        return new Alerta(TipoAlerta.SUCESSO, texto);
    }

    /**
     * Alerta de erro.
     *
     * @param texto texto para o usuário
     * @return o alerta
     */
    public static Alerta erro(String texto) {
        return new Alerta(TipoAlerta.ERRO, texto);
    }

    /**
     * Alerta informativo.
     *
     * @param texto texto para o usuário
     * @return o alerta
     */
    public static Alerta info(String texto) {
        return new Alerta(TipoAlerta.INFO, texto);
    }
}

package br.unicamp.var.web.organizador;

import java.util.List;

/**
 * Presença de uma votação para a gestão ao vivo: só quem já votou, nunca a escolha (RNF-01).
 *
 * @param votaram ids dos eleitores que já votaram, na ordem da lista de aptos
 */
public record PresencaJson(List<String> votaram) {

    /** Cópia defensiva da lista. */
    public PresencaJson {
        votaram = List.copyOf(votaram);
    }
}

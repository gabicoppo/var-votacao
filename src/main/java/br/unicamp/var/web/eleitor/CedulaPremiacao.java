package br.unicamp.var.web.eleitor;

import java.util.List;

/**
 * Cédula Top 3 de uma premiação para o eleitor apto (história #23).
 *
 * @param id         id do cadastro da premiação
 * @param titulo     título (ex.: "Craque do Campeonato 2026")
 * @param descricao  critério de avaliação (pode ser vazio)
 * @param candidatos candidatos, na ordem de inscrição
 * @param categoria  grupo do eleitor (ex.: "Jornalistas")
 * @param peso       peso do grupo já formatado (ex.: "2")
 * @param situacao   o que mostrar
 */
public record CedulaPremiacao(String id, String titulo, String descricao, List<String> candidatos, String categoria,
                              String peso, SituacaoCedula situacao) {

    /** Copia os candidatos. */
    public CedulaPremiacao {
        candidatos = List.copyOf(candidatos);
    }
}

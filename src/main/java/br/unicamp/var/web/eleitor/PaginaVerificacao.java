package br.unicamp.var.web.eleitor;

import java.util.List;

/**
 * Página pública de verificação de comprovante (história #28). Nunca revela o voto.
 *
 * @param votacoes  votações que já receberam ou recebem votos, para o select
 * @param votacaoId votação escolhida (pode ser vazia)
 * @param codigo    código digitado (pode ser vazio)
 * @param resultado resultado da conferência, ou {@code null} se ainda não houve consulta
 * @param pergunta  pergunta da votação conferida, ou {@code null}
 */
public record PaginaVerificacao(List<OpcaoVotacao> votacoes, String votacaoId, String codigo, Resultado resultado,
                                String pergunta) {

    /** Copia a lista. */
    public PaginaVerificacao {
        votacoes = List.copyOf(votacoes);
    }

    /**
     * Votação no select.
     *
     * @param id       id da votação
     * @param pergunta pergunta exibida
     */
    public record OpcaoVotacao(String id, String pergunta) {
    }

    /** Resultado da conferência. */
    public enum Resultado {
        /** O código foi emitido por esta votação. */
        VALIDO,
        /** O código não foi emitido por esta votação. */
        INVALIDO,
        /** A votação informada não existe. */
        VOTACAO_INEXISTENTE
    }
}

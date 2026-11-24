package br.unicamp.var.top3;

/** Etapas da votação e da divulgação do ranking (#23, #30). */
public enum StatusPremiacao {
    /** Configuração criada, antes da abertura (#23). */
    RASCUNHO,
    /** Recebe cédulas de eleitores aptos (#23). */
    ABERTA,
    /** Votação finalizada e pronta para apurar (#23). */
    ENCERRADA,
    /** Ranking disponível para consulta (#30). */
    APURADA
}

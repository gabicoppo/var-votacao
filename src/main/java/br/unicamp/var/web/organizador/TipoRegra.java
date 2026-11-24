package br.unicamp.var.web.organizador;

/**
 * Regra de apuração escolhida no formulário de nova votação (história #21).
 */
public enum TipoRegra {

    /** Vence a opção mais votada ({@link br.unicamp.var.apuracao.MaioriaSimples}). */
    SIMPLES,

    /** Mais da metade dos votos válidos, com 2º turno opcional ({@link br.unicamp.var.apuracao.MaioriaAbsoluta}). */
    ABSOLUTA,

    /** Quórum e fração mínima dos votos válidos ({@link br.unicamp.var.apuracao.MaioriaQualificada}). */
    QUALIFICADA
}

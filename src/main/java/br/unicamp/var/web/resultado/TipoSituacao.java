package br.unicamp.var.web.resultado;

import java.util.Locale;

/**
 * Situação final de uma votação apurada, para colorir a faixa do resultado (história #29).
 * Corresponde, um para um, às variantes de {@code ResultadoApuracao}.
 */
public enum TipoSituacao {
    /** Uma opção venceu (faixa verde). */
    ELEITO,
    /** Ninguém teve maioria absoluta e as duas mais votadas vão ao 2º turno (faixa amarelo-cartão). */
    SEGUNDO_TURNO,
    /** Empate, nenhum voto válido ou fração exigida não alcançada (faixa cinza). */
    SEM_VENCEDOR,
    /** Votaram menos aptos que o quórum mínimo (faixa vermelho-cartão). */
    QUORUM_NAO_ATINGIDO;

    /**
     * Classe CSS da faixa de situação.
     *
     * @return ex.: {@code "situacao-segundo-turno"}
     */
    public String classeCss() {
        return "situacao-" + name().toLowerCase(Locale.ROOT).replace('_', '-');
    }
}

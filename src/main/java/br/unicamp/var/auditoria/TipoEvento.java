package br.unicamp.var.auditoria;

/**
 * Tipos de evento registrados na trilha de auditoria de uma votação (RNF-07).
 *
 * <p>Intencionalmente <b>não existe</b> tipo de evento para voto: registrar cada
 * voto no log, com horário e autor, permitiria ligar o eleitor à sua escolha e
 * quebraria o sigilo do voto (RNF-01). A auditoria cobre só o ciclo de vida da
 * votação.</p>
 */
public enum TipoEvento {
    /** Votação criada (em rascunho). */
    CRIADA,
    /** Regra de apuração trocada enquanto a votação estava em rascunho. */
    REGRA_ALTERADA,
    /** Votação aberta para receber votos. */
    ABERTA,
    /** Votação encerrada; não recebe mais votos. */
    ENCERRADA,
    /** Votação apurada pela regra configurada. */
    APURADA,
    /** Segundo turno criado a partir de uma apuração que o exigiu. */
    SEGUNDO_TURNO_CRIADO
}

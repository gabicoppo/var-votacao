package br.unicamp.var.web.eleitor;

import java.time.Instant;

/**
 * Dados do comprovante mostrados uma única vez depois do voto (história #28). De propósito, não
 * guarda a opção escolhida (RNF-01).
 *
 * @param votacaoId    id da votação
 * @param pergunta     pergunta da votação
 * @param codigo       código do comprovante
 * @param registradoEm quando o voto foi registrado
 */
public record ComprovanteVoto(String votacaoId, String pergunta, String codigo, Instant registradoEm) {

    /**
     * Link para conferir o comprovante depois.
     *
     * @return {@code /verificar?votacao=...&codigo=...}
     */
    public String linkVerificar() {
        return "/verificar?votacao=" + votacaoId + "&codigo=" + codigo;
    }
}

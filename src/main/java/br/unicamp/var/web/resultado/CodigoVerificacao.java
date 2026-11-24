package br.unicamp.var.web.resultado;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Código de verificação do relatório de apuração (história #31, D4): os 12 primeiros caracteres
 * hexadecimais (48 bits) do SHA-256 do texto de {@code RelatorioApuracao.formatar()}, em UTF-8,
 * exibidos em três grupos de quatro (ex.: {@code 3F9A-1C2B-7D8E}).
 *
 * <p><b>O que o código prova:</b> que o texto de uma cópia impressa é o mesmo que o sistema gera
 * agora para aquela votação. Quem tem o papel abre o relatório no sistema e compara os dois códigos;
 * se um número, nome ou evento da auditoria tiver sido alterado na cópia, o código calculado sobre o
 * texto dela não bate. Serve para detectar edição acidental ou ingênua de uma ata que circulou.</p>
 *
 * <p><b>O que o código não prova:</b></p>
 * <ul>
 *   <li>autoria ou autenticidade: não há chave secreta, então qualquer pessoa pode calcular o código de
 *       um texto forjado; ele não é assinatura digital;</li>
 *   <li>resistência a falsificação deliberada: com 48 bits, achar outro texto com o mesmo código é caro,
 *       mas possível para quem se dedicar;</li>
 *   <li>que a apuração esteja correta: ele só amarra o texto, não refaz a contagem;</li>
 *   <li>a data de emissão nem o layout, que ficam fora do texto. O código muda se o relatório mudar
 *       depois, por exemplo quando o 2º turno é criado e entra um evento novo na auditoria.</li>
 * </ul>
 */
public final class CodigoVerificacao {

    private static final int CARACTERES = 12;
    private static final int GRUPO = 4;

    private CodigoVerificacao() {
    }

    /**
     * Calcula o código de um texto.
     *
     * @param texto texto do relatório
     * @return ex.: {@code "3F9A-1C2B-7D8E"}
     * @throws IllegalArgumentException se o texto for nulo
     */
    public static String de(String texto) {
        if (texto == null) {
            throw new IllegalArgumentException("texto do relatorio nao pode ser nulo");
        }
        String hex = HexFormat.of().withUpperCase().formatHex(sha256(texto.getBytes(StandardCharsets.UTF_8)));
        StringBuilder codigo = new StringBuilder();
        for (int i = 0; i < CARACTERES; i += GRUPO) {
            if (i > 0) {
                codigo.append('-');
            }
            codigo.append(hex, i, i + GRUPO);
        }
        return codigo.toString();
    }

    private static byte[] sha256(byte[] dados) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(dados);
        } catch (NoSuchAlgorithmException e) {
            // Toda JVM é obrigada a ter SHA-256 (MessageDigest, Java SE).
            throw new IllegalStateException("sha-256 indisponivel", e);
        }
    }
}

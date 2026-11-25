package br.unicamp.var.urna;

import java.time.Instant;

/**
 * Comprovante entregue ao eleitor ao depositar o voto (história #28).
 *
 * <p>De propósito, não contém o eleitor nem a opção escolhida (RNF-01): o código
 * é aleatório e serve só para o eleitor conferir, com
 * {@link Urna#comprovanteValido(String)}, que o voto foi computado. A urna não
 * guarda o horário de cada voto; ele existe apenas no comprovante, que fica com o
 * eleitor.</p>
 *
 * @param codigo       código único do comprovante, sem espaços nas pontas
 * @param registradoEm momento em que o voto foi registrado
 */
public record Comprovante(String codigo, Instant registradoEm) {

    /**
     * Valida e normaliza os campos.
     *
     * @throws IllegalArgumentException se o código for nulo ou em branco, ou se
     *                                  {@code registradoEm} for nulo
     */
    public Comprovante {
        if (codigo == null || codigo.isBlank()) {
            throw new IllegalArgumentException("codigo do comprovante nao pode ser vazio");
        }
        if (registradoEm == null) {
            throw new IllegalArgumentException("horario do comprovante nao pode ser nulo");
        }
        codigo = codigo.strip();
    }
}

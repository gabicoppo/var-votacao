package br.unicamp.var.resultado;

/**
 * Linha do resultado detalhado: uma opção, seus votos absolutos e o percentual
 * que eles representam sobre os votos válidos.
 *
 * <p>As linhas são criadas por {@link ResultadoDetalhado}, que garante a
 * coerência entre votos e percentual.</p>
 *
 * @param opcao      nome da opção
 * @param votos      número de votos recebidos pela opção
 * @param percentual percentual sobre os votos válidos, de 0 a 100
 */
public record LinhaResultado(String opcao, int votos, double percentual) {
}

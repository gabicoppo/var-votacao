package br.unicamp.var.apuracao;

import br.unicamp.var.apuracao2turno.ResultadoApuracao;

/**
 * Regra de apuração de uma votação (padrão Strategy).
 *
 * <p>Cada implementação encapsula um algoritmo de apuração (maioria simples,
 * maioria absoluta, maioria qualificada, ...). Quem apura a votação guarda a
 * regra escolhida na configuração e só chama {@link #apurar(ContagemVotos)},
 * sem precisar saber qual regra está sendo usada. Uma regra nova é só uma
 * classe nova que implementa esta interface.</p>
 *
 * <p>A interface tem um único método e pode ser implementada por lambda.</p>
 */
@FunctionalInterface
public interface RegraApuracao {

    /**
     * Apura a contagem de votos segundo esta regra.
     *
     * @param contagem votos recebidos por cada opção e total de aptos a votar
     * @return o resultado da apuração
     */
    ResultadoApuracao apurar(ContagemVotos contagem);
}

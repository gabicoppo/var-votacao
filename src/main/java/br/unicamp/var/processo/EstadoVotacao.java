package br.unicamp.var.processo;

import br.unicamp.var.apuracao.RegraApuracao;
import br.unicamp.var.apuracao2turno.ResultadoApuracao;

/**
 * Estado do ciclo de vida de uma {@link Votacao} (padrão State).
 *
 * <p>Há uma operação para cada método público da {@link Votacao}. Por padrão,
 * toda operação é proibida e lança {@link IllegalStateException}; cada estado
 * sobrescreve só as operações que permite e decide para qual estado a votação
 * passa.</p>
 */
interface EstadoVotacao {

    /**
     * Nome do estado, exibido como status da votação.
     *
     * @return "rascunho", "aberta", "encerrada" ou "apurada"
     */
    String nome();

    default void alterarRegra(Votacao votacao, RegraApuracao novaRegra) {
        throw operacaoProibida("alterar a regra de apuração");
    }

    default void abrir(Votacao votacao) {
        throw operacaoProibida("abrir");
    }

    default void registrarVoto(Votacao votacao, String opcao) {
        throw operacaoProibida("registrar voto");
    }

    default void encerrar(Votacao votacao) {
        throw operacaoProibida("encerrar");
    }

    default ResultadoApuracao apurar(Votacao votacao) {
        throw operacaoProibida("apurar");
    }

    private IllegalStateException operacaoProibida(String operacao) {
        return new IllegalStateException("não é possível " + operacao + " em votação " + nome());
    }
}

package br.unicamp.var.processo;

import br.unicamp.var.apuracao.RegraApuracao;
import br.unicamp.var.apuracao2turno.ResultadoApuracao;
import br.unicamp.var.resultado.ResultadoDetalhado;
import br.unicamp.var.urna.Comprovante;
import java.util.Optional;

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

    /**
     * Aplica o período de votação ao estado atual (história #20). Por padrão não faz nada:
     * só rascunho (que abre no início) e aberta (que encerra no fim) reagem ao relógio.
     *
     * @param votacao votação cujo relógio e período são consultados
     */
    default void atualizarPeloRelogio(Votacao votacao) {
        // nenhum efeito nos estados que não dependem do horário
    }

    default void alterarRegra(Votacao votacao, RegraApuracao novaRegra) {
        throw operacaoProibida("alterar a regra de apuração");
    }

    default void abrir(Votacao votacao) {
        throw operacaoProibida("abrir");
    }

    default Comprovante registrarVoto(Votacao votacao, String eleitorId, String opcao) {
        throw operacaoProibida("registrar voto");
    }

    default void encerrar(Votacao votacao) {
        throw operacaoProibida("encerrar");
    }

    default ResultadoApuracao apurar(Votacao votacao) {
        throw operacaoProibida("apurar");
    }

    default ResultadoDetalhado resultadoDetalhado(Votacao votacao) {
        throw operacaoProibida("ver o resultado detalhado");
    }

    /**
     * Resultado da apuração, se já houver (usado para decidir se existe 2º turno).
     *
     * @return o resultado, ou vazio antes da apuração
     */
    default Optional<ResultadoApuracao> resultado() {
        return Optional.empty();
    }

    private IllegalStateException operacaoProibida(String operacao) {
        return new IllegalStateException("não é possível " + operacao + " em votação " + nome());
    }
}

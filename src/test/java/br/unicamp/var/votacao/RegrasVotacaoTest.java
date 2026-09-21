package br.unicamp.var.votacao;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RegrasVotacaoTest {

    @Test
    void quorumAtingidoExatamenteNoLimite() {
        // 5 de 10 presentes, minimo de 50%: bate exatamente no limite
        assertTrue(RegrasVotacao.quorumAtingido(5, 10, 0.5));
    }

    @Test
    void quorumNaoAtingidoAbaixoDoMinimo() {
        assertFalse(RegrasVotacao.quorumAtingido(4, 10, 0.5));
    }

    @Test
    void quorumAtingidoComFolga() {
        assertTrue(RegrasVotacao.quorumAtingido(8, 10, 0.5));
    }

    @Test
    void quorumComTotalZeroLancaExcecao() {
        assertThrows(IllegalArgumentException.class, () -> RegrasVotacao.quorumAtingido(0, 0, 0.5));
    }

    @Test
    void aprovadaComFracaoExata() {
        // 6 a favor e 4 contra: 60% dos validos, exatamente a fracao exigida
        assertTrue(RegrasVotacao.aprovada(6, 4, 0.6));
    }

    @Test
    void reprovadaComFracaoAbaixoDoExigido() {
        assertFalse(RegrasVotacao.aprovada(5, 5, 0.6));
    }

    @Test
    void abstencaoContaParaQuorumMasNaoParaAprovacao() {
        // 10 presentes de 10 membros (quorum cheio), mas 4 deles se abstiveram
        // Abstencao conta como presenca, mas nao entra no calculo de aprovacao
        assertTrue(RegrasVotacao.quorumAtingido(10, 10, 1.0));

        // Dos 10 presentes, so 6 votaram (4 a favor, 2 contra): as 4 abstencoes nao sao passadas para aprovada.
        assertTrue(RegrasVotacao.aprovada(4, 2, 0.6));
    }

    @Test
    void aprovadaSemVotosValidosLancaExcecao() {
        assertThrows(IllegalArgumentException.class, () -> RegrasVotacao.aprovada(0, 0, 0.5));
    }
}
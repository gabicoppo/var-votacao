package br.unicamp.var.apuracao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import br.unicamp.var.apuracao2turno.ResultadoApuracao;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MaioriaQualificadaTest {

    private static final double DOIS_TERCOS = 2.0 / 3;

    private final RegraApuracao regra = new MaioriaQualificada(0.5, DOIS_TERCOS);

    @Test
    void maisVotadaComDoisTercosEQuorumAtingidoVence() {
        ContagemVotos contagem = new ContagemVotos(Map.of("Aprovar", 70, "Rejeitar", 20), 100);

        ResultadoApuracao resultado = regra.apurar(contagem);

        ResultadoApuracao.Eleito eleito = assertInstanceOf(ResultadoApuracao.Eleito.class, resultado);
        assertEquals("Aprovar", eleito.candidato());
    }

    @Test
    void fracaoExataPassa() {
        // 6 de 10 votos validos: exatamente os 60% exigidos
        RegraApuracao sessentaPorCento = new MaioriaQualificada(0.5, 0.6);
        ContagemVotos contagem = new ContagemVotos(Map.of("Aprovar", 6, "Rejeitar", 4), 10);

        ResultadoApuracao resultado = sessentaPorCento.apurar(contagem);

        ResultadoApuracao.Eleito eleito = assertInstanceOf(ResultadoApuracao.Eleito.class, resultado);
        assertEquals("Aprovar", eleito.candidato());
    }

    @Test
    void fracaoNaoAtingidaFicaSemVencedor() {
        ContagemVotos contagem = new ContagemVotos(Map.of("Aprovar", 60, "Rejeitar", 40), 100);

        ResultadoApuracao resultado = regra.apurar(contagem);

        assertInstanceOf(ResultadoApuracao.SemVencedor.class, resultado);
    }

    @Test
    void demaisOpcoesSomadasContamComoContra() {
        // Aprovar tem 60 de 90 validos (2/3 exatos); as outras duas somam 30
        Map<String, Integer> votos = new LinkedHashMap<>();
        votos.put("Aprovar", 60);
        votos.put("Rejeitar", 20);
        votos.put("Adiar", 10);

        ResultadoApuracao resultado = regra.apurar(new ContagemVotos(votos, 100));

        ResultadoApuracao.Eleito eleito = assertInstanceOf(ResultadoApuracao.Eleito.class, resultado);
        assertEquals("Aprovar", eleito.candidato());
    }

    @Test
    void quorumNaoAtingido() {
        ContagemVotos contagem = new ContagemVotos(Map.of("Aprovar", 49), 100);

        ResultadoApuracao resultado = regra.apurar(contagem);

        assertInstanceOf(ResultadoApuracao.QuorumNaoAtingido.class, resultado);
    }

    @Test
    void quorumExatoEAtingido() {
        ContagemVotos contagem = new ContagemVotos(Map.of("Aprovar", 50), 100);

        ResultadoApuracao resultado = regra.apurar(contagem);

        ResultadoApuracao.Eleito eleito = assertInstanceOf(ResultadoApuracao.Eleito.class, resultado);
        assertEquals("Aprovar", eleito.candidato());
    }

    @Test
    void empateNoPrimeiroLugarFicaSemVencedor() {
        RegraApuracao metade = new MaioriaQualificada(0.5, 0.5);
        ContagemVotos contagem = new ContagemVotos(Map.of("Aprovar", 40, "Rejeitar", 40), 100);

        ResultadoApuracao resultado = metade.apurar(contagem);

        assertInstanceOf(ResultadoApuracao.SemVencedor.class, resultado);
    }

    @Test
    void semVotosQuorumNaoAtingido() {
        ContagemVotos contagem = new ContagemVotos(Map.of("Aprovar", 0, "Rejeitar", 0), 100);

        ResultadoApuracao resultado = regra.apurar(contagem);

        assertInstanceOf(ResultadoApuracao.QuorumNaoAtingido.class, resultado);
    }

    @Test
    void fracoesIguaisAUmSaoAceitas() {
        RegraApuracao unanimidade = new MaioriaQualificada(1.0, 1.0);
        ContagemVotos contagem = new ContagemVotos(Map.of("Aprovar", 10, "Rejeitar", 0), 10);

        ResultadoApuracao resultado = unanimidade.apurar(contagem);

        assertInstanceOf(ResultadoApuracao.Eleito.class, resultado);
    }

    @Test
    void quorumMinimoZeroLancaExcecao() {
        assertThrows(IllegalArgumentException.class, () -> new MaioriaQualificada(0.0, DOIS_TERCOS));
    }

    @Test
    void quorumMinimoAcimaDeUmLancaExcecao() {
        assertThrows(IllegalArgumentException.class, () -> new MaioriaQualificada(1.01, DOIS_TERCOS));
    }

    @Test
    void fracaoExigidaZeroLancaExcecao() {
        assertThrows(IllegalArgumentException.class, () -> new MaioriaQualificada(0.5, 0.0));
    }

    @Test
    void fracaoExigidaAcimaDeUmLancaExcecao() {
        assertThrows(IllegalArgumentException.class, () -> new MaioriaQualificada(0.5, 1.5));
    }

    @Test
    void fracaoNaoNumericaLancaExcecao() {
        assertThrows(IllegalArgumentException.class, () -> new MaioriaQualificada(Double.NaN, DOIS_TERCOS));
    }
}

package br.unicamp.var.resultado;

import static org.junit.jupiter.api.Assertions.assertEquals;

import br.unicamp.var.apuracao.ContagemVotos;
import br.unicamp.var.apuracao.MaioriaSimples;
import br.unicamp.var.apuracao2turno.ResultadoApuracao;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ResultadoDetalhadoTest {

    private static final double TOLERANCIA = 0.001;

    @Test
    void mostraVotosPercentuaisTotaisESituacaoDeUmaVotacaoComEleito() {
        Map<String, Integer> votos = new LinkedHashMap<>();
        votos.put("Juliana", 20);
        votos.put("Gabriela", 50);
        votos.put("Fabricio", 30);
        ContagemVotos contagem = new ContagemVotos(votos, 200);
        ResultadoApuracao apuracao = new MaioriaSimples().apurar(contagem);

        ResultadoDetalhado resultado = ResultadoDetalhado.de(contagem, apuracao);

        List<LinhaResultado> linhas = resultado.linhas();
        assertEquals(List.of("Gabriela", "Fabricio", "Juliana"), linhas.stream().map(LinhaResultado::opcao).toList());
        assertEquals(List.of(50, 30, 20), linhas.stream().map(LinhaResultado::votos).toList());
        assertEquals(50.0, linhas.get(0).percentual(), TOLERANCIA);
        assertEquals(30.0, linhas.get(1).percentual(), TOLERANCIA);
        assertEquals(20.0, linhas.get(2).percentual(), TOLERANCIA);
        assertEquals(100, resultado.totalVotantes());
        assertEquals(200, resultado.totalAptos());
        assertEquals(50.0, resultado.participacao(), TOLERANCIA);
        assertEquals("Eleito: Gabriela", resultado.situacao());
    }
}

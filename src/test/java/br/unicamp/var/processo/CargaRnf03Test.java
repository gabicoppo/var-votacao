package br.unicamp.var.processo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.unicamp.var.apuracao.MaioriaAbsoluta;
import br.unicamp.var.apuracao2turno.ResultadoApuracao;
import br.unicamp.var.eleitorado.Eleitor;
import br.unicamp.var.eleitorado.Eleitorado;
import br.unicamp.var.relatorio.RelatorioApuracao;
import br.unicamp.var.resultado.LinhaResultado;
import br.unicamp.var.resultado.ResultadoDetalhado;
import br.unicamp.var.urna.Sigilo;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Teste de carga do RNF-03: "resultado disponível em até 5 min após o encerramento, para até
 * 20.000 votos".
 *
 * <p>Monta uma eleição de clube com 20.000 eleitores aptos, 4 chapas, maioria absoluta com 2º
 * turno e voto secreto; registra os 20.000 votos com uma distribuição fixa ({@code new
 * Random(42)}); encerra, apura e gera o {@link ResultadoDetalhado} e o
 * {@link RelatorioApuracao}. Mede com {@link System#nanoTime()} o tempo dos votos e o tempo do
 * encerramento até o relatório pronto, e imprime os dois. No mesmo volume, verifica o RNF-02:
 * a soma dos votos do resultado é 20.000, igual a {@code totalVotantes()}, e um voto a mais de
 * quem já votou é recusado.</p>
 *
 * <p>Tem a tag {@code carga}, mas roda no {@code ./mvnw verify} normal porque leva menos de um
 * segundo. Para rodar só ele: {@code ./mvnw test -Dgroups=carga}.</p>
 */
@Tag("carga")
class CargaRnf03Test {

    private static final int TOTAL_ELEITORES = 20_000;
    private static final List<String> CHAPAS =
            List.of("Chapa Tradição", "Chapa Renovação", "Chapa Base Forte", "Chapa Arquibancada");
    /** Peso de cada chapa no sorteio (soma 100): ninguém passa de 50%, então há 2º turno. */
    private static final int[] PESOS = {38, 32, 20, 10};
    private static final Duration LIMITE_RNF03 = Duration.ofMinutes(5);
    private static final Clock RELOGIO = Clock.fixed(Instant.parse("2026-11-24T12:00:00Z"), ZoneOffset.UTC);

    @Test
    @DisplayName("RNF-03: 20.000 votos apurados com relatório em menos de 5 min, sem perder votos (RNF-02)")
    void apuraVinteMilVotosDentroDoLimite() {
        Eleitorado eleitorado = Eleitorado.de(IntStream.rangeClosed(1, TOTAL_ELEITORES)
                .mapToObj(i -> new Eleitor(String.format("s%05d", i), "Sócio " + i))
                .toList());
        Votacao votacao = Votacao.nova()
                .pergunta("Quem deve presidir o clube no triênio 2027-2029?")
                .descricao("Eleição presidencial: maioria absoluta dos votos válidos, com 2º turno.")
                .opcoes(CHAPAS)
                .regra(new MaioriaAbsoluta(true))
                .eleitorado(eleitorado)
                .sigilo(Sigilo.SECRETO)
                .relogio(RELOGIO)
                .build();
        votacao.abrir();

        Map<String, Integer> esperado = new LinkedHashMap<>();
        CHAPAS.forEach(chapa -> esperado.put(chapa, 0));
        Random sorteio = new Random(42);
        long inicioVotos = System.nanoTime();
        for (Eleitor eleitor : eleitorado.listar()) {
            String chapa = sortear(sorteio);
            votacao.registrarVoto(eleitor.id(), chapa);
            esperado.merge(chapa, 1, Integer::sum);
        }
        long nanosVotos = System.nanoTime() - inicioVotos;

        assertEquals(TOTAL_ELEITORES, votacao.totalVotantes());
        assertThrows(IllegalStateException.class, () -> votacao.registrarVoto("s00001", CHAPAS.get(1)),
                "o 20.001º voto, de quem já votou, deve ser recusado (RNF-02)");
        assertEquals(TOTAL_ELEITORES, votacao.totalVotantes());

        Apuracao apuracao = assertTimeout(LIMITE_RNF03, () -> encerrarEApurar(votacao),
                "RNF-03: o resultado deve sair em até 5 min após o encerramento");

        System.out.printf("RNF-03 | %d votos registrados em %d ms (%.1f µs por voto)%n",
                TOTAL_ELEITORES, nanosVotos / 1_000_000, nanosVotos / 1_000.0 / TOTAL_ELEITORES);
        System.out.printf("RNF-03 | encerramento -> relatório pronto em %.1f ms (limite: %d min)%n",
                apuracao.nanos() / 1_000_000.0, LIMITE_RNF03.toMinutes());

        ResultadoDetalhado resultado = apuracao.resultado();
        int somaVotos = resultado.linhas().stream().mapToInt(LinhaResultado::votos).sum();
        assertEquals(TOTAL_ELEITORES, somaVotos);
        assertEquals(TOTAL_ELEITORES, resultado.totalVotantes());
        assertEquals(TOTAL_ELEITORES, resultado.totalAptos());
        for (LinhaResultado linha : resultado.linhas()) {
            assertEquals(esperado.get(linha.opcao()), linha.votos(), "votos de " + linha.opcao());
        }
        assertInstanceOf(ResultadoApuracao.SegundoTurno.class, apuracao.apuracao());
        assertEquals("apurada", votacao.status());
        assertFalse(apuracao.relatorio().isBlank());
        assertTrue(apuracao.relatorio().contains(CHAPAS.get(0)));
    }

    private static String sortear(Random sorteio) {
        int valor = sorteio.nextInt(100);
        int acumulado = 0;
        for (int i = 0; i < PESOS.length; i++) {
            acumulado += PESOS[i];
            if (valor < acumulado) {
                return CHAPAS.get(i);
            }
        }
        throw new IllegalStateException("pesos devem somar 100");
    }

    private static Apuracao encerrarEApurar(Votacao votacao) {
        long inicio = System.nanoTime();
        votacao.encerrar();
        ResultadoApuracao apuracao = votacao.apurar();
        ResultadoDetalhado resultado = votacao.resultadoDetalhado();
        String relatorio = RelatorioApuracao.de(votacao).formatar();
        return new Apuracao(apuracao, resultado, relatorio, System.nanoTime() - inicio);
    }

    /** O que sai da apuração medida: resultado, quadro detalhado, relatório e o tempo gasto. */
    private record Apuracao(ResultadoApuracao apuracao, ResultadoDetalhado resultado, String relatorio, long nanos) {
    }
}

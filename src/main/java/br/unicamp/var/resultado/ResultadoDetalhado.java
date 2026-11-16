package br.unicamp.var.resultado;

import br.unicamp.var.apuracao.ContagemVotos;
import br.unicamp.var.apuracao2turno.ResultadoApuracao;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Resultado detalhado de uma votação apurada: votos absolutos e percentuais de
 * cada opção, totais de votantes e de aptos, participação e a situação final
 * (história #29).
 *
 * <p>É um valor imutável, montado a partir da {@link ContagemVotos} usada na
 * apuração e do {@link ResultadoApuracao} que a regra escolhida produziu. Esta
 * classe não decide quem venceu: ela só apresenta o que a regra já decidiu.</p>
 *
 * <p>Cada opção da contagem vira exatamente uma linha, com os mesmos votos.
 * Por isso a soma dos votos das linhas é sempre igual ao total de votos
 * registrados na contagem (RNF-02); a igualdade vale por construção.</p>
 *
 * <p>Fora do escopo: horários de encerramento e de publicação e a escolha de
 * cada eleitor em votação identificada.</p>
 */
public final class ResultadoDetalhado {

    private static final Locale LOCALE_TEXTO = Locale.forLanguageTag("pt-BR");
    private static final double CEM_POR_CENTO = 100.0;

    private final List<LinhaResultado> linhas;
    private final int totalVotantes;
    private final int totalAptos;
    private final String situacao;

    private ResultadoDetalhado(List<LinhaResultado> linhas, int totalVotantes, int totalAptos, String situacao) {
        this.linhas = List.copyOf(linhas);
        this.totalVotantes = totalVotantes;
        this.totalAptos = totalAptos;
        this.situacao = situacao;
    }

    /**
     * Monta o resultado detalhado de uma votação apurada.
     *
     * @param contagem votos por opção e total de aptos usados na apuração
     * @param apuracao resultado que a regra de apuração produziu para a contagem
     * @return o resultado detalhado, com as linhas ordenadas por votos
     * @throws IllegalArgumentException se {@code contagem} ou {@code apuracao}
     *                                  for nulo
     */
    public static ResultadoDetalhado de(ContagemVotos contagem, ResultadoApuracao apuracao) {
        if (contagem == null) {
            throw new IllegalArgumentException("contagem nao pode ser nula");
        }
        if (apuracao == null) {
            throw new IllegalArgumentException("apuracao nao pode ser nula");
        }
        int totalValidos = contagem.totalVotosValidos();
        List<LinhaResultado> linhas = contagem.votosPorOpcao().entrySet().stream()
                .map(entrada -> new LinhaResultado(
                        entrada.getKey(), entrada.getValue(), percentual(entrada.getValue(), totalValidos)))
                .sorted(Comparator.comparingInt(LinhaResultado::votos).reversed())
                .toList();
        return new ResultadoDetalhado(linhas, totalValidos, contagem.totalAptos(), descrever(apuracao));
    }

    /**
     * Linhas do resultado, uma por opção.
     *
     * @return lista imutável, ordenada por votos do maior para o menor; opções
     *         empatadas mantêm a ordem em que aparecem na contagem
     */
    public List<LinhaResultado> linhas() {
        return linhas;
    }

    /**
     * Total de votantes, igual à soma dos votos de todas as linhas.
     *
     * @return número de votos registrados
     */
    public int totalVotantes() {
        return totalVotantes;
    }

    /**
     * Total de pessoas aptas a votar.
     *
     * @return número de aptos
     */
    public int totalAptos() {
        return totalAptos;
    }

    /**
     * Participação: quanto do total de aptos efetivamente votou.
     *
     * @return percentual de votantes sobre os aptos, de 0 a 100
     */
    public double participacao() {
        return percentual(totalVotantes, totalAptos);
    }

    /**
     * Situação final da votação, em texto.
     *
     * @return "Eleito: nome", "2º turno entre A e B", "Sem vencedor" ou
     *         "Quórum não atingido"
     */
    public String situacao() {
        return situacao;
    }

    /**
     * Gera o quadro do resultado em texto, com uma linha por opção, os totais e
     * a situação. Os percentuais saem com uma casa decimal e vírgula, sempre no
     * formato pt-BR, qualquer que seja o locale da máquina.
     *
     * @return o quadro do resultado, com as linhas separadas por {@code \n}
     */
    public String formatar() {
        StringBuilder quadro = new StringBuilder("Resultado da votação\n");
        for (LinhaResultado linha : linhas) {
            quadro.append(String.format(LOCALE_TEXTO, "%s: %d votos (%.1f%%)",
                    linha.opcao(), linha.votos(), linha.percentual())).append('\n');
        }
        quadro.append(String.format(LOCALE_TEXTO, "Votantes: %d de %d aptos (participação de %.1f%%)",
                totalVotantes, totalAptos, participacao()));
        quadro.append("\nSituação: ").append(situacao);
        return quadro.toString();
    }

    private static double percentual(int parte, int total) {
        if (total == 0) {
            return 0.0;
        }
        return parte * CEM_POR_CENTO / total;
    }

    private static String descrever(ResultadoApuracao apuracao) {
        return switch (apuracao) {
            case ResultadoApuracao.Eleito eleito -> "Eleito: " + eleito.candidato();
            case ResultadoApuracao.SegundoTurno turno ->
                    "2º turno entre " + turno.primeiroColocado() + " e " + turno.segundoColocado();
            case ResultadoApuracao.SemVencedor semVencedor -> "Sem vencedor";
            case ResultadoApuracao.QuorumNaoAtingido semQuorum -> "Quórum não atingido";
        };
    }
}

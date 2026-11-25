package br.unicamp.var.web.resultado;

import br.unicamp.var.apuracao.RegraApuracao;
import br.unicamp.var.apuracao2turno.ResultadoApuracao;
import br.unicamp.var.resultado.LinhaResultado;
import br.unicamp.var.resultado.ResultadoDetalhado;
import br.unicamp.var.web.comum.Formatador;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Tudo o que a tela de resultado de uma votação apurada mostra (história #29), já calculado:
 * barras, faixa de situação com título e explicação, e totais. Não depende do Spring.
 *
 * <p>As barras seguem a ordem do {@link ResultadoDetalhado} (mais votada primeiro). A largura é
 * normalizada pela opção mais votada, que ocupa 100%, para as barras ficarem legíveis mesmo com
 * percentuais pequenos; zero votos dá largura 0. Percentuais com uma casa decimal e vírgula, sem
 * ",0" ({@link Formatador}, pt-BR fixo).</p>
 *
 * @param barras            uma barra por opção, na ordem do resultado detalhado
 * @param tipoSituacao      tipo da situação final, para a cor da faixa
 * @param titulo            título da faixa, ex.: {@code "2º turno entre Chapa A e Chapa B"}
 * @param explicacao        frase que explica o porquê da situação
 * @param votantes          votos válidos registrados
 * @param aptos             eleitores aptos
 * @param participacaoTexto participação dos aptos, ex.: {@code "75%"}
 */
public record GraficoResultado(List<BarraResultado> barras, TipoSituacao tipoSituacao, String titulo,
                               String explicacao, int votantes, int aptos, String participacaoTexto) {

    private static final Formatador FMT = new Formatador();
    private static final double CEM = 100.0;

    /** Guarda uma cópia imutável das barras. */
    public GraficoResultado {
        barras = List.copyOf(barras);
    }

    /**
     * Monta o gráfico sem citar a regra na explicação.
     *
     * @param detalhado resultado detalhado da votação apurada
     * @param apuracao  resultado que a regra produziu
     * @return o gráfico pronto para a tela
     * @throws IllegalArgumentException se algum argumento for nulo
     */
    public static GraficoResultado de(ResultadoDetalhado detalhado, ResultadoApuracao apuracao) {
        return de(detalhado, apuracao, null);
    }

    /**
     * Monta o gráfico; quando a regra é informada, a explicação termina com a descrição dela.
     *
     * @param detalhado resultado detalhado da votação apurada
     * @param apuracao  resultado que a regra produziu
     * @param regra     regra de apuração usada, ou {@code null}
     * @return o gráfico pronto para a tela
     * @throws IllegalArgumentException se {@code detalhado} ou {@code apuracao} for nulo
     */
    public static GraficoResultado de(ResultadoDetalhado detalhado, ResultadoApuracao apuracao,
                                      RegraApuracao regra) {
        if (detalhado == null || apuracao == null) {
            throw new IllegalArgumentException("resultado detalhado e apuracao nao podem ser nulos");
        }
        List<LinhaResultado> linhas = detalhado.linhas();
        String regraTexto = regra == null ? "" : " " + regra.descricao();
        Situacao situacao = switch (apuracao) {
            case ResultadoApuracao.Eleito eleito -> new Situacao(TipoSituacao.ELEITO,
                    eleito.candidato() + " venceu", explicarEleito(eleito.candidato(), detalhado) + regraTexto);
            case ResultadoApuracao.SegundoTurno turno -> new Situacao(TipoSituacao.SEGUNDO_TURNO,
                    "2º turno entre " + turno.primeiroColocado() + " e " + turno.segundoColocado(),
                    explicarSegundoTurno(turno, linhas));
            case ResultadoApuracao.SemVencedor semVencedor -> new Situacao(TipoSituacao.SEM_VENCEDOR,
                    "Sem vencedor", explicarSemVencedor(linhas) + regraTexto);
            case ResultadoApuracao.QuorumNaoAtingido semQuorum -> new Situacao(TipoSituacao.QUORUM_NAO_ATINGIDO,
                    "Quórum não atingido", "Votaram " + detalhado.totalVotantes() + " de " + detalhado.totalAptos()
                    + " aptos (" + participacao(detalhado) + "), abaixo do quórum mínimo." + regraTexto);
        };
        return new GraficoResultado(barras(linhas, apuracao), situacao.tipo(), situacao.titulo(),
                situacao.explicacao(), detalhado.totalVotantes(), detalhado.totalAptos(), participacao(detalhado));
    }

    private record Situacao(TipoSituacao tipo, String titulo, String explicacao) {
    }

    private static List<BarraResultado> barras(List<LinhaResultado> linhas, ResultadoApuracao apuracao) {
        int maior = linhas.stream().mapToInt(LinhaResultado::votos).max().orElse(0);
        Set<String> vencedores = apuracao instanceof ResultadoApuracao.Eleito eleito
                ? Set.of(eleito.candidato()) : Set.of();
        Set<String> finalistas = apuracao instanceof ResultadoApuracao.SegundoTurno turno
                ? Set.of(turno.primeiroColocado(), turno.segundoColocado()) : Set.of();
        List<BarraResultado> barras = new ArrayList<>();
        int colocacao = 0;
        for (int i = 0; i < linhas.size(); i++) {
            LinhaResultado linha = linhas.get(i);
            if (i == 0 || linha.votos() != linhas.get(i - 1).votos()) {
                colocacao = i + 1;
            }
            Destaque destaque = vencedores.contains(linha.opcao()) ? Destaque.VENCEDOR
                    : finalistas.contains(linha.opcao()) ? Destaque.FINALISTA : Destaque.NENHUM;
            double largura = maior == 0 ? 0 : linha.votos() * CEM / maior;
            barras.add(new BarraResultado(linha.opcao(), linha.votos(), percentual(linha.percentual()), largura,
                    destaque, colocacao));
        }
        return barras;
    }

    private static String explicarEleito(String vencedor, ResultadoDetalhado detalhado) {
        return linha(detalhado.linhas(), vencedor)
                .map(linha -> vencedor + " recebeu " + linha.votos() + " de " + detalhado.totalVotantes()
                        + (detalhado.totalVotantes() == 1 ? " voto válido (" : " votos válidos (")
                        + percentual(linha.percentual()) + ").")
                .orElse(vencedor + " venceu pela regra de apuração.");
    }

    private static String explicarSegundoTurno(ResultadoApuracao.SegundoTurno turno, List<LinhaResultado> linhas) {
        return "Nenhuma opção passou da metade dos votos válidos. " + comPercentual(turno.primeiroColocado(), linhas)
                + " e " + comPercentual(turno.segundoColocado(), linhas)
                + " foram as mais votadas e disputam o 2º turno.";
    }

    private static String explicarSemVencedor(List<LinhaResultado> linhas) {
        int maior = linhas.stream().mapToInt(LinhaResultado::votos).max().orElse(0);
        if (maior == 0) {
            return "Nenhum voto válido foi registrado.";
        }
        List<String> empatados = linhas.stream().filter(l -> l.votos() == maior).map(LinhaResultado::opcao).toList();
        if (empatados.size() > 1) {
            return juntar(empatados) + " empataram em primeiro lugar, com " + votos(maior) + " cada.";
        }
        LinhaResultado primeira = linhas.getFirst();
        return primeira.opcao() + " foi a mais votada, com " + percentual(primeira.percentual())
                + " dos votos válidos, mas não alcançou o mínimo exigido.";
    }

    private static String comPercentual(String opcao, List<LinhaResultado> linhas) {
        return linha(linhas, opcao).map(l -> opcao + " (" + percentual(l.percentual()) + ")").orElse(opcao);
    }

    private static Optional<LinhaResultado> linha(List<LinhaResultado> linhas, String opcao) {
        return linhas.stream().filter(l -> l.opcao().equals(opcao)).findFirst();
    }

    private static String juntar(List<String> nomes) {
        int ultimo = nomes.size() - 1;
        return String.join(", ", nomes.subList(0, ultimo)) + " e " + nomes.get(ultimo);
    }

    private static String participacao(ResultadoDetalhado detalhado) {
        return percentual(detalhado.participacao());
    }

    /** Percentual de 0 a 100 no formato da tela, ex.: {@code 66.666} → {@code "66,7%"}. */
    static String percentual(double deZeroACem) {
        return FMT.numero(deZeroACem) + "%";
    }

    /** Quantidade de votos no singular ou no plural, ex.: {@code "1 voto"}, {@code "3 votos"}. */
    static String votos(int quantidade) {
        return quantidade + (quantidade == 1 ? " voto" : " votos");
    }
}

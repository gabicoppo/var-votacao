package br.unicamp.var.top3;

import java.text.Collator;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Ranking completo e imutável da premiação (#30). A pontuação segue a regra vigente de
 * {@link Posicao} e {@link PontuacaoTop3}: 5/3/1 pontos, multiplicados pelo peso do grupo (#23).
 *
 * <p>Após ordenar por pontos, cada bloco de empate admite totais a até 1e-9 do maior
 * total do bloco. Usar uma referência fixa evita que uma sequência de diferenças pequenas
 * torne a ordenação intransitiva. No bloco contam os primeiros e depois os segundos lugares,
 * sem ponderação. Quem empata nos dois divide a colocação; o nome só define a ordem de exibição.</p>
 */
public final class RankingTop3 {

    private static final double TOLERANCIA = 1e-9;
    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");
    private static final Comparator<Parcial> DESEMPATE = Comparator
            .comparingInt((Parcial p) -> p.quantidade(Posicao.PRIMEIRO))
            .thenComparingInt(p -> p.quantidade(Posicao.SEGUNDO)).reversed();

    private final String titulo;
    private final List<String> grupos;
    private final int totalCedulas;
    private final List<LinhaRanking> linhas;
    private final List<String> criterios;

    RankingTop3(String titulo, String descricao, List<String> candidatos, List<CategoriaEleitor> categorias,
                Map<String, List<VotoCategoria>> votosPorCategoria, int totalCedulas) {
        this.titulo = titulo;
        this.totalCedulas = totalCedulas;
        this.grupos = categorias.stream().map(CategoriaEleitor::nome).toList();
        List<String> regras = descreverCriterios(descricao, categorias);
        this.linhas = List.copyOf(classificar(pontuar(candidatos, categorias, votosPorCategoria), regras));
        this.criterios = List.copyOf(regras);
    }

    private static List<Parcial> pontuar(List<String> candidatos, List<CategoriaEleitor> categorias,
                                         Map<String, List<VotoCategoria>> votosPorCategoria) {
        Map<String, Parcial> porCandidato = new HashMap<>();
        for (String candidato : candidatos) {
            porCandidato.put(candidato, new Parcial(candidato));
        }
        for (CategoriaEleitor categoria : categorias) {
            List<VotoCategoria> votos = votosPorCategoria.getOrDefault(categoria.nome(), List.of());
            Map<String, Double> pontos = PontuacaoTop3.calcularPontos(votos);
            porCandidato.forEach((nome, parcial) -> parcial.somar(categoria.nome(), pontos.getOrDefault(nome, 0.0)));
            for (VotoCategoria voto : votos) {
                porCandidato.get(voto.candidato()).lugares.merge(voto.posicao(), 1, Integer::sum);
            }
        }
        return new ArrayList<>(porCandidato.values());
    }

    private static List<LinhaRanking> classificar(List<Parcial> parciais, List<String> criterios) {
        parciais.sort(Comparator.comparingDouble((Parcial p) -> p.pontos).reversed());
        Comparator<Parcial> ordem = DESEMPATE.thenComparing(p -> p.nome, Collator.getInstance(PT_BR))
                .thenComparing(p -> p.nome);
        List<LinhaRanking> linhas = new ArrayList<>();
        int inicio = 0;
        while (inicio < parciais.size()) {
            double referencia = parciais.get(inicio).pontos;
            int fim = inicio + 1;
            while (fim < parciais.size() && referencia - parciais.get(fim).pontos <= TOLERANCIA) {
                fim++;
            }
            List<Parcial> bloco = parciais.subList(inicio, fim);
            bloco.sort(ordem);
            int colocacao = inicio + 1;
            for (int i = 0; i < bloco.size(); i++) {
                if (i > 0 && DESEMPATE.compare(bloco.get(i - 1), bloco.get(i)) != 0) {
                    colocacao = inicio + i + 1;
                }
                linhas.add(bloco.get(i).linha(colocacao));
            }
            if (colocacao > inicio + 1) {
                String nomes = bloco.stream().map(p -> p.nome).collect(Collectors.joining(", "));
                criterios.add("Desempate aplicado no grupo de candidatos: " + nomes + ".");
            }
            inicio = fim;
        }
        return linhas;
    }

    private static List<String> descreverCriterios(String descricao, List<CategoriaEleitor> categorias) {
        List<String> criterios = new ArrayList<>();
        if (!descricao.isBlank()) {
            criterios.add("Critério de avaliação: " + descricao);
        }
        for (Posicao posicao : Posicao.values()) {
            String unidade = posicao.pontos() == 1 ? " ponto" : " pontos";
            criterios.add(posicao.colocacao() + "º lugar = " + posicao.pontos() + unidade);
        }
        NumberFormat formatoPeso = NumberFormat.getNumberInstance(PT_BR);
        formatoPeso.setGroupingUsed(false);
        formatoPeso.setMinimumFractionDigits(1);
        formatoPeso.setMaximumFractionDigits(340);
        for (CategoriaEleitor grupo : categorias) {
            criterios.add(grupo.nome() + ": peso " + formatoPeso.format(grupo.peso()));
        }
        criterios.add("Empate em pontos: diferença de até " + formatoPeso.format(TOLERANCIA)
                + " para a maior pontuação do bloco.");
        criterios.add("Desempate: mais primeiros lugares, depois mais segundos lugares. "
                + "Persistindo o empate, a colocação é compartilhada e os nomes aparecem em ordem alfabética.");
        return criterios;
    }

    /** Retorna uma linha por candidato, inclusive aqueles sem votos (#30). */
    public List<LinhaRanking> linhas() {
        return linhas;
    }

    /** Explica o critério de avaliação, os pontos, os pesos e o desempate (#23, #30). */
    public List<String> criterios() {
        return criterios;
    }

    /** Retorna o número de cédulas usadas na apuração (#30). */
    public int totalCedulas() {
        return totalCedulas;
    }

    /** Gera o quadro completo em pt-BR, com uma casa decimal nas pontuações (#30). */
    public String formatar() {
        StringBuilder quadro = new StringBuilder(titulo).append("\nColocação | Candidato | Pontos | 1º | 2º | 3º | ")
                .append(String.join(" | ", grupos));
        for (LinhaRanking linha : linhas) {
            quadro.append('\n').append(String.format(PT_BR, "%dº | %s | %.1f | %d | %d | %d", linha.colocacao(),
                    linha.candidato(), linha.pontos(), linha.primeirosLugares(),
                    linha.segundosLugares(), linha.terceirosLugares()));
            for (String grupo : grupos) {
                quadro.append(String.format(PT_BR, " | %.1f", linha.pontosPorCategoria().get(grupo)));
            }
        }
        return quadro.append("\nCédulas: ").append(totalCedulas)
                .append("\nCritérios:\n").append(String.join("\n", criterios)).toString();
    }

    private static final class Parcial {
        private final String nome;
        private final Map<String, Double> porCategoria = new HashMap<>();
        private final Map<Posicao, Integer> lugares = new EnumMap<>(Posicao.class);
        private double pontos;

        private Parcial(String nome) {
            this.nome = nome;
        }

        private void somar(String categoria, double subtotal) {
            porCategoria.put(categoria, subtotal);
            pontos += subtotal;
            if (!Double.isFinite(pontos)) {
                throw new IllegalStateException("pontuacao excede o limite numerico");
            }
        }

        private int quantidade(Posicao posicao) {
            return lugares.getOrDefault(posicao, 0);
        }

        private LinhaRanking linha(int colocacao) {
            return new LinhaRanking(colocacao, nome, pontos, quantidade(Posicao.PRIMEIRO),
                    quantidade(Posicao.SEGUNDO), quantidade(Posicao.TERCEIRO), porCategoria);
        }
    }
}

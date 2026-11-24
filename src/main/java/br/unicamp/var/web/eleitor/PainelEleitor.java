package br.unicamp.var.web.eleitor;

import br.unicamp.var.processo.PeriodoVotacao;
import br.unicamp.var.processo.Votacao;
import br.unicamp.var.top3.PremiacaoTop3;
import br.unicamp.var.top3.StatusPremiacao;
import br.unicamp.var.urna.Sigilo;
import br.unicamp.var.web.comum.Fuso;
import br.unicamp.var.web.comum.PremiacaoCadastrada;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Painel "Minhas votações" do eleitor (história #26), separado em seções. Só entram votações e
 * premiações em que o eleitor é apto (RNF-05).
 *
 * <ul>
 *   <li><b>Para votar agora:</b> aberta, ainda sem voto e dentro do período (ou sem período), mais as
 *       premiações abertas ainda sem cédula; ordenadas pelo fim mais próximo (sem prazo por último);</li>
 *   <li><b>Já votei:</b> aberta e com voto do eleitor;</li>
 *   <li><b>Em breve:</b> rascunho, ou aberta com o período ainda não iniciado;</li>
 *   <li><b>Aguardando apuração:</b> encerrada (ou aberta com o período já terminado, antes de o
 *       agendador encerrá-la);</li>
 *   <li><b>Resultados:</b> apurada.</li>
 * </ul>
 *
 * <p>Cada votação é lida dentro de {@code synchronized (votacao)}: {@link Votacao} não é thread-safe.
 * Nenhum card traz resultado parcial nem o voto do eleitor (RNF-01).</p>
 *
 * @param votarAgora         seção "Para votar agora"
 * @param jaVotei            seção "Já votei"
 * @param emBreve            seção "Em breve"
 * @param aguardandoApuracao seção "Aguardando apuração"
 * @param resultados         seção "Resultados"
 */
public record PainelEleitor(List<CartaoVotacao> votarAgora, List<CartaoVotacao> jaVotei, List<CartaoVotacao> emBreve,
                            List<CartaoVotacao> aguardandoApuracao, List<CartaoVotacao> resultados) {

    /** Tamanho máximo da descrição nos cards. */
    public static final int TAMANHO_DESCRICAO = 140;

    private static final String SECRETO = "Voto secreto";
    private static final String IDENTIFICADO = "Voto identificado";
    private static final String SEM_PRAZO = "sem prazo definido";

    /** Copia as listas, que ficam imutáveis. */
    public PainelEleitor {
        votarAgora = List.copyOf(votarAgora);
        jaVotei = List.copyOf(jaVotei);
        emBreve = List.copyOf(emBreve);
        aguardandoApuracao = List.copyOf(aguardandoApuracao);
        resultados = List.copyOf(resultados);
    }

    /**
     * Monta o painel só com votações.
     *
     * @param eleitorId id do eleitor logado (o username)
     * @param votacoes  todas as votações
     * @param relogio   relógio da aplicação
     * @return o painel
     */
    public static PainelEleitor montar(String eleitorId, Collection<Votacao> votacoes, Clock relogio) {
        return montar(eleitorId, votacoes, List.of(), relogio);
    }

    /**
     * Monta o painel com votações e premiações Top 3 (história #23).
     *
     * @param eleitorId  id do eleitor logado (o username)
     * @param votacoes   todas as votações
     * @param premiacoes todas as premiações
     * @param relogio    relógio da aplicação
     * @return o painel
     */
    public static PainelEleitor montar(String eleitorId, Collection<Votacao> votacoes,
                                       Collection<PremiacaoCadastrada> premiacoes, Clock relogio) {
        Instant agora = relogio.instant();
        Secoes secoes = new Secoes();
        for (Votacao votacao : votacoes) {
            synchronized (votacao) {
                classificar(eleitorId, votacao, agora, secoes);
            }
        }
        for (PremiacaoCadastrada cadastro : premiacoes) {
            classificar(eleitorId, cadastro, secoes);
        }
        secoes.votarAgora.sort(Comparator.comparing(Item::fim, Comparator.nullsLast(Comparator.naturalOrder())));
        return new PainelEleitor(cartoes(secoes.votarAgora), secoes.jaVotei, secoes.emBreve,
                secoes.aguardando, secoes.resultados);
    }

    /**
     * Quantas votações e premiações esperam o voto do eleitor.
     *
     * @return tamanho da seção "Para votar agora"
     */
    public int totalParaVotar() {
        return votarAgora.size();
    }

    /**
     * Diz se o painel não tem nada para o eleitor.
     *
     * @return {@code true} se todas as seções estiverem vazias
     */
    public boolean vazio() {
        return votarAgora.isEmpty() && jaVotei.isEmpty() && emBreve.isEmpty() && aguardandoApuracao.isEmpty()
                && resultados.isEmpty();
    }

    private static void classificar(String eleitorId, Votacao votacao, Instant agora, Secoes secoes) {
        if (eleitorId == null || !votacao.eleitorado().ehApto(eleitorId)) {
            return;
        }
        Optional<PeriodoVotacao> periodo = votacao.periodo();
        String link = "/eleitor/votacoes/" + votacao.id();
        switch (votacao.status()) {
            case "rascunho" -> secoes.emBreve.add(cartao(votacao, prazoInicio(periodo, agora), false, false, link));
            case "aberta" -> classificarAberta(eleitorId, votacao, agora, secoes, link);
            case "encerrada" -> secoes.aguardando.add(cartao(votacao, "encerrada", false, false, null));
            case "apurada" -> secoes.resultados.add(cartao(votacao, "resultado publicado", false,
                    votacao.jaVotou(eleitorId), "/votacoes/" + votacao.id() + "/resultado"));
            default -> {
                // status desconhecido: não aparece no painel
            }
        }
    }

    private static void classificarAberta(String eleitorId, Votacao votacao, Instant agora, Secoes secoes,
                                          String link) {
        Optional<PeriodoVotacao> periodo = votacao.periodo();
        if (votacao.jaVotou(eleitorId)) {
            secoes.jaVotei.add(cartao(votacao, prazo(periodo, agora), false, true,
                    "/verificar?votacao=" + votacao.id()));
        } else if (periodo.isPresent() && periodo.get().naoComecou(agora)) {
            secoes.emBreve.add(cartao(votacao, prazoInicio(periodo, agora), false, false, link));
        } else if (periodo.isPresent() && periodo.get().terminou(agora)) {
            secoes.aguardando.add(cartao(votacao, "encerrada", false, false, null));
        } else {
            Instant fim = periodo.map(PeriodoVotacao::fim).orElse(null);
            secoes.votarAgora.add(new Item(cartao(votacao, prazo(periodo, agora), true, false, link), fim));
        }
    }

    private static void classificar(String eleitorId, PremiacaoCadastrada cadastro, Secoes secoes) {
        PremiacaoTop3 premiacao = cadastro.premiacao();
        if (premiacao.categoriaDe(eleitorId).isEmpty()) {
            return;
        }
        StatusPremiacao status = premiacao.status();
        boolean votou = premiacao.jaVotou(eleitorId);
        String link = "/eleitor/premiacoes/" + cadastro.id();
        if (status == StatusPremiacao.ABERTA && !votou) {
            secoes.votarAgora.add(new Item(cartao(cadastro, "aberta para votação", true, false, link), null));
        } else if (status == StatusPremiacao.ABERTA) {
            secoes.jaVotei.add(cartao(cadastro, "cédula registrada", false, true, null));
        } else if (status == StatusPremiacao.RASCUNHO) {
            secoes.emBreve.add(cartao(cadastro, "abre em breve", false, false, null));
        } else if (status == StatusPremiacao.ENCERRADA) {
            secoes.aguardando.add(cartao(cadastro, "encerrada", false, votou, null));
        } else {
            secoes.resultados.add(cartao(cadastro, "ranking publicado", false, votou,
                    "/premiacoes/" + cadastro.id() + "/ranking"));
        }
    }

    private static CartaoVotacao cartao(Votacao votacao, String prazo, boolean podeVotar, boolean jaVotou,
                                        String link) {
        String status = votacao.status();
        return new CartaoVotacao(votacao.id(), votacao.pergunta(), resumir(votacao.descricao()), status,
                capitalizar(status), prazo, votacao.sigilo() == Sigilo.SECRETO ? SECRETO : IDENTIFICADO,
                podeVotar, jaVotou, link, false);
    }

    private static CartaoVotacao cartao(PremiacaoCadastrada cadastro, String prazo, boolean podeVotar,
                                        boolean jaVotou, String link) {
        PremiacaoTop3 premiacao = cadastro.premiacao();
        String status = premiacao.status().name().toLowerCase(Locale.ROOT);
        return new CartaoVotacao(cadastro.id(), premiacao.titulo(), resumir(premiacao.descricao()), status,
                capitalizar(status), prazo, SECRETO, podeVotar, jaVotou, link, true);
    }

    private static String prazo(Optional<PeriodoVotacao> periodo, Instant agora) {
        return periodo.map(p -> TempoRestante.descrever(agora, p.fim(), Fuso.ZONA)).orElse(SEM_PRAZO);
    }

    private static String prazoInicio(Optional<PeriodoVotacao> periodo, Instant agora) {
        return periodo.map(p -> TempoRestante.descreverInicio(agora, p.inicio(), Fuso.ZONA)).orElse("abre em breve");
    }

    static String resumir(String texto) {
        if (texto == null) {
            return "";
        }
        String limpo = texto.strip();
        if (limpo.length() <= TAMANHO_DESCRICAO) {
            return limpo;
        }
        return limpo.substring(0, TAMANHO_DESCRICAO - 1).stripTrailing() + "…";
    }

    private static String capitalizar(String status) {
        return status.substring(0, 1).toUpperCase(Locale.ROOT) + status.substring(1);
    }

    private static List<CartaoVotacao> cartoes(List<Item> itens) {
        return itens.stream().map(Item::cartao).toList();
    }

    /** Card de "Para votar agora" com o fim do período, usado só para ordenar. */
    private record Item(CartaoVotacao cartao, Instant fim) {
    }

    /** Listas mutáveis usadas durante a montagem. */
    private static final class Secoes {
        private final List<Item> votarAgora = new ArrayList<>();
        private final List<CartaoVotacao> jaVotei = new ArrayList<>();
        private final List<CartaoVotacao> emBreve = new ArrayList<>();
        private final List<CartaoVotacao> aguardando = new ArrayList<>();
        private final List<CartaoVotacao> resultados = new ArrayList<>();
    }
}

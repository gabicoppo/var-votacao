package br.unicamp.var.web.eleitor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.unicamp.var.apuracao.MaioriaSimples;
import br.unicamp.var.eleitorado.Eleitor;
import br.unicamp.var.eleitorado.Eleitorado;
import br.unicamp.var.processo.PeriodoVotacao;
import br.unicamp.var.processo.Votacao;
import br.unicamp.var.top3.CategoriaEleitor;
import br.unicamp.var.top3.CedulaTop3;
import br.unicamp.var.top3.PremiacaoTop3;
import br.unicamp.var.urna.Sigilo;
import br.unicamp.var.web.comum.PremiacaoCadastrada;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Seções do painel do eleitor, eleitor não apto, ordenação e períodos.
 */
class PainelEleitorTest {

    private static final Instant AGORA = Instant.parse("2026-11-24T13:00:00Z");
    private static final Clock RELOGIO = Clock.fixed(AGORA, ZoneOffset.UTC);
    private static final String ELEITOR = "e01";

    private static Votacao.Builder votacao(String pergunta) {
        return Votacao.nova()
                .pergunta(pergunta)
                .descricao("Descrição de " + pergunta)
                .opcoes(List.of("Sim", "Não"))
                .regra(new MaioriaSimples())
                .eleitorado(Eleitorado.de(List.of(new Eleitor("e01", "Ana Ribeiro"), new Eleitor("e02", "Bruno"))))
                .relogio(RELOGIO);
    }

    private static PeriodoVotacao periodo(Duration doInicio, Duration doFim) {
        return new PeriodoVotacao(AGORA.plus(doInicio), AGORA.plus(doFim));
    }

    private static Votacao aberta(String pergunta) {
        Votacao votacao = votacao(pergunta).build();
        votacao.abrir();
        return votacao;
    }

    private static List<String> perguntas(List<CartaoVotacao> cartoes) {
        return cartoes.stream().map(CartaoVotacao::pergunta).toList();
    }

    @Nested
    @DisplayName("Seções")
    class Secoes {

        @Test
        @DisplayName("aberta, apto e sem voto vai para \"Para votar agora\", com link para a cédula")
        void votarAgora() {
            Votacao votacao = aberta("Aberta");

            PainelEleitor painel = PainelEleitor.montar(ELEITOR, List.of(votacao), RELOGIO);

            CartaoVotacao cartao = painel.votarAgora().get(0);
            assertEquals("/eleitor/votacoes/" + votacao.id(), cartao.link());
            assertTrue(cartao.podeVotar());
            assertEquals("sem prazo definido", cartao.prazo());
            assertEquals("Aberta", cartao.textoStatus());
            assertEquals("Voto secreto", cartao.modalidade());
            assertEquals(1, painel.totalParaVotar());
            assertFalse(painel.vazio());
        }

        @Test
        @DisplayName("aberta e já votada vai para \"Já votei\", com link para verificar")
        void jaVotei() {
            Votacao votacao = aberta("Votada");
            votacao.registrarVoto(ELEITOR, "Sim");

            PainelEleitor painel = PainelEleitor.montar(ELEITOR, List.of(votacao), RELOGIO);

            assertEquals(List.of("Votada"), perguntas(painel.jaVotei()));
            assertTrue(painel.jaVotei().get(0).jaVotou());
            assertEquals("/verificar?votacao=" + votacao.id(), painel.jaVotei().get(0).link());
            assertTrue(painel.votarAgora().isEmpty());
        }

        @Test
        @DisplayName("rascunho e aberta com período não iniciado vão para \"Em breve\"")
        void emBreve() {
            Votacao rascunho = votacao("Rascunho").build();
            Votacao rascunhoComPeriodo = votacao("Rascunho com período")
                    .periodo(periodo(Duration.ofDays(2), Duration.ofDays(3))).build();
            Votacao futura = votacao("Futura").periodo(periodo(Duration.ofHours(2), Duration.ofDays(1))).build();
            futura.abrir();

            PainelEleitor painel =
                    PainelEleitor.montar(ELEITOR, List.of(rascunho, rascunhoComPeriodo, futura), RELOGIO);

            assertEquals(List.of("Rascunho", "Rascunho com período", "Futura"), perguntas(painel.emBreve()));
            assertEquals("abre em breve", painel.emBreve().get(0).prazo());
            assertEquals("começa em 2 dias", painel.emBreve().get(1).prazo());
            assertEquals("começa hoje às 12:00", painel.emBreve().get(2).prazo());
        }

        @Test
        @DisplayName("encerrada (ou aberta com o período já terminado) vai para \"Aguardando apuração\"")
        void aguardandoApuracao() {
            Votacao encerrada = aberta("Encerrada");
            encerrada.encerrar();
            Votacao vencida = votacao("Vencida").periodo(periodo(Duration.ofDays(-2), Duration.ofDays(-1))).build();
            vencida.abrir();

            PainelEleitor painel = PainelEleitor.montar(ELEITOR, List.of(encerrada, vencida), RELOGIO);

            assertEquals(List.of("Encerrada", "Vencida"), perguntas(painel.aguardandoApuracao()));
            assertNull(painel.aguardandoApuracao().get(0).link());
        }

        @Test
        @DisplayName("apurada vai para \"Resultados\", com link para a tela de resultado")
        void resultados() {
            Votacao apurada = aberta("Apurada");
            apurada.registrarVoto(ELEITOR, "Sim");
            apurada.encerrar();
            apurada.apurar();

            PainelEleitor painel = PainelEleitor.montar(ELEITOR, List.of(apurada), RELOGIO);

            assertEquals("/votacoes/" + apurada.id() + "/resultado", painel.resultados().get(0).link());
            assertTrue(painel.resultados().get(0).jaVotou());
        }
    }

    @Nested
    @DisplayName("Aptidão, ordenação e textos")
    class Regras {

        @Test
        @DisplayName("votação em que o eleitor não é apto não aparece; eleitor nulo vê painel vazio")
        void naoApto() {
            Votacao votacao = aberta("Só para outros");

            assertTrue(PainelEleitor.montar("e99", List.of(votacao), RELOGIO).vazio());
            assertTrue(PainelEleitor.montar(null, List.of(votacao), RELOGIO).vazio());
        }

        @Test
        @DisplayName("\"Para votar agora\" ordena pelo fim mais próximo, sem prazo por último")
        void ordenacao() {
            Votacao semPrazo = aberta("Sem prazo");
            Votacao longe = votacao("Longe").periodo(periodo(Duration.ofDays(-1), Duration.ofDays(5))).build();
            longe.abrir();
            Votacao perto = votacao("Perto").periodo(periodo(Duration.ofDays(-1), Duration.ofMinutes(25))).build();
            perto.abrir();

            PainelEleitor painel = PainelEleitor.montar(ELEITOR, List.of(semPrazo, longe, perto), RELOGIO);

            assertEquals(List.of("Perto", "Longe", "Sem prazo"), perguntas(painel.votarAgora()));
            assertEquals("encerra em 25 min", painel.votarAgora().get(0).prazo());
            assertEquals("encerra em 5 dias", painel.votarAgora().get(1).prazo());
        }

        @Test
        @DisplayName("sigilo identificado e descrição longa resumida com reticências")
        void textos() {
            Votacao votacao = votacao("Nominal").sigilo(Sigilo.IDENTIFICADO).descricao("x".repeat(300)).build();
            votacao.abrir();

            CartaoVotacao cartao = PainelEleitor.montar(ELEITOR, List.of(votacao), RELOGIO).votarAgora().get(0);

            assertEquals("Voto identificado", cartao.modalidade());
            assertEquals(PainelEleitor.TAMANHO_DESCRICAO, cartao.descricaoCurta().length());
            assertTrue(cartao.descricaoCurta().endsWith("…"));
            assertEquals("", PainelEleitor.resumir(null));
            assertEquals("curta", PainelEleitor.resumir("  curta "));
        }

        @Test
        @DisplayName("painel sem nada é vazio e imutável")
        void vazio() {
            PainelEleitor painel = PainelEleitor.montar(ELEITOR, List.of(), RELOGIO);

            assertTrue(painel.vazio());
            assertEquals(0, painel.totalParaVotar());
        }
    }

    @Nested
    @DisplayName("Premiações Top 3")
    class Premiacoes {

        private PremiacaoCadastrada premiacao(String id) {
            PremiacaoTop3 premiacao = new PremiacaoTop3("Craque " + id, "Melhor do campeonato",
                    List.of("Atleta A", "Atleta B", "Atleta C"), List.of(new CategoriaEleitor("Torcedores", 1.0)),
                    Map.of("e01", "Torcedores", "e02", "Torcedores"));
            return new PremiacaoCadastrada(id, premiacao, AGORA);
        }

        @Test
        @DisplayName("aberta sem cédula vai para \"Para votar agora\" com a etiqueta Top 3")
        void abertaParaVotar() {
            PremiacaoCadastrada cadastro = premiacao("p1");
            cadastro.premiacao().abrir();

            PainelEleitor painel = PainelEleitor.montar(ELEITOR, List.of(), List.of(cadastro), RELOGIO);

            CartaoVotacao cartao = painel.votarAgora().get(0);
            assertTrue(cartao.top3());
            assertEquals("/eleitor/premiacoes/p1", cartao.link());
        }

        @Test
        @DisplayName("cada estado da premiação vai para a seção certa; apurada leva ao ranking")
        void estados() {
            PremiacaoCadastrada rascunho = premiacao("rascunho");
            PremiacaoCadastrada votada = premiacao("votada");
            votada.premiacao().abrir();
            votada.premiacao().registrarCedula(ELEITOR, new CedulaTop3("Atleta A", "Atleta B", "Atleta C"));
            PremiacaoCadastrada encerrada = premiacao("encerrada");
            encerrada.premiacao().abrir();
            encerrada.premiacao().encerrar();
            PremiacaoCadastrada apurada = premiacao("apurada");
            apurada.premiacao().abrir();
            apurada.premiacao().encerrar();
            apurada.premiacao().apurar();

            PainelEleitor painel = PainelEleitor.montar(ELEITOR, List.of(),
                    List.of(rascunho, votada, encerrada, apurada), RELOGIO);

            assertEquals(List.of("Craque rascunho"), perguntas(painel.emBreve()));
            assertEquals(List.of("Craque votada"), perguntas(painel.jaVotei()));
            assertEquals(List.of("Craque encerrada"), perguntas(painel.aguardandoApuracao()));
            assertEquals("/premiacoes/apurada/ranking", painel.resultados().get(0).link());
            assertTrue(painel.votarAgora().isEmpty());
        }

        @Test
        @DisplayName("premiação em que o eleitor não tem categoria não aparece")
        void naoApto() {
            PremiacaoCadastrada cadastro = premiacao("p1");
            cadastro.premiacao().abrir();

            assertTrue(PainelEleitor.montar("e99", List.of(), List.of(cadastro), RELOGIO).vazio());
        }
    }

    @Nested
    @DisplayName("Polimento da Fase 3: peso, selo \"Encerra hoje\" e resumo")
    class Polimento {

        private PremiacaoCadastrada premiacaoComPesos() {
            PremiacaoTop3 premiacao = new PremiacaoTop3("Craque", "Melhor do campeonato",
                    List.of("Atleta A", "Atleta B", "Atleta C"),
                    List.of(new CategoriaEleitor("Jornalistas", 2.0), new CategoriaEleitor("Torcedores", 1.5)),
                    Map.of("e01", "Jornalistas", "e02", "Torcedores"));
            premiacao.abrir();
            return new PremiacaoCadastrada("p1", premiacao, AGORA);
        }

        @Test
        @DisplayName("card de premiação mostra \"Top 3 · peso N\" do grupo do eleitor, e não o sigilo")
        void premiacaoMostraPeso() {
            PremiacaoCadastrada cadastro = premiacaoComPesos();

            CartaoVotacao jornalista = PainelEleitor.montar("e01", List.of(), List.of(cadastro), RELOGIO)
                    .votarAgora().get(0);
            CartaoVotacao torcedor = PainelEleitor.montar("e02", List.of(), List.of(cadastro), RELOGIO)
                    .votarAgora().get(0);

            assertEquals("Top 3 · peso 2", jornalista.modalidade());
            assertEquals("Top 3 · peso 1,5", torcedor.modalidade());
        }

        @Test
        @DisplayName("votação continua mostrando \"Voto secreto\" ou \"Voto identificado\"")
        void votacaoMostraSigilo() {
            Votacao secreta = aberta("Secreta");
            Votacao nominal = votacao("Nominal").sigilo(Sigilo.IDENTIFICADO).build();
            nominal.abrir();

            List<CartaoVotacao> cartoes = PainelEleitor.montar(ELEITOR, List.of(secreta, nominal), RELOGIO)
                    .votarAgora();

            assertEquals(List.of("Voto secreto", "Voto identificado"),
                    cartoes.stream().map(CartaoVotacao::modalidade).toList());
        }

        @Test
        @DisplayName("com mais de uma para votar, a de prazo mais curto que encerra hoje ganha o selo")
        void seloEncerraHoje() {
            Votacao hoje = votacao("Hoje").periodo(periodo(Duration.ofDays(-1), Duration.ofHours(5))).build();
            hoje.abrir();
            Votacao depois = votacao("Depois").periodo(periodo(Duration.ofDays(-1), Duration.ofDays(3))).build();
            depois.abrir();

            List<CartaoVotacao> cartoes = PainelEleitor.montar(ELEITOR, List.of(depois, hoje), RELOGIO).votarAgora();

            assertEquals(List.of("Hoje", "Depois"), perguntas(cartoes));
            assertTrue(cartoes.get(0).encerraHoje());
            assertFalse(cartoes.get(1).encerraHoje());
        }

        @Test
        @DisplayName("sem selo se só há uma para votar, se a mais próxima encerra amanhã ou se não há prazo")
        void semSelo() {
            Votacao hoje = votacao("Hoje").periodo(periodo(Duration.ofDays(-1), Duration.ofHours(5))).build();
            hoje.abrir();
            Votacao amanha = votacao("Amanhã").periodo(periodo(Duration.ofDays(-1), Duration.ofDays(1))).build();
            amanha.abrir();

            assertFalse(PainelEleitor.montar(ELEITOR, List.of(hoje), RELOGIO).votarAgora().get(0).encerraHoje());
            assertFalse(PainelEleitor.montar(ELEITOR, List.of(amanha, aberta("Sem prazo")), RELOGIO)
                    .votarAgora().get(0).encerraHoje());
            assertTrue(PainelEleitor.montar(ELEITOR, List.of(aberta("A"), aberta("B")), RELOGIO).votarAgora()
                    .stream().noneMatch(CartaoVotacao::encerraHoje));
        }

        @Test
        @DisplayName("descrição longa é cortada na última palavra inteira, sem pontuação solta antes do \"…\"")
        void resumoPorPalavra() {
            String texto = "Escolha da diretoria para o triênio 2027-2029. Voto secreto; se nenhuma chapa tiver "
                    + "mais da metade dos votos válidos, as duas mais votadas vão para o segundo turno, uma semana "
                    + "depois.";

            String resumo = PainelEleitor.resumir(texto);
            String semReticencias = resumo.substring(0, resumo.length() - 1);

            assertTrue(resumo.endsWith("…"));
            assertTrue(resumo.length() <= PainelEleitor.TAMANHO_DESCRICAO);
            assertTrue(texto.startsWith(semReticencias + " ") || texto.startsWith(semReticencias + ","), resumo);
            assertFalse(semReticencias.endsWith(","));
            assertEquals("a".repeat(100) + " fim…", PainelEleitor.resumir("a".repeat(100) + " fim, " + "b".repeat(60)));
        }
    }
}

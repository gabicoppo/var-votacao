package br.unicamp.var.processo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import br.unicamp.var.apuracao.ContagemVotos;
import br.unicamp.var.apuracao.MaioriaAbsoluta;
import br.unicamp.var.apuracao.MaioriaSimples;
import br.unicamp.var.apuracao.RegraApuracao;
import br.unicamp.var.apuracao2turno.ResultadoApuracao;
import br.unicamp.var.cadastro.CadastroCandidatos;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Testes de {@link Votacao}: ciclo de vida pelo padrão State (RNF-07), configuração pelo builder
 * (história #19) e apuração pela regra configurada (padrão Strategy).
 */
class VotacaoTest {

    private static final List<String> OPCOES = List.of("Gabriela", "Fabricio", "Juliana");
    private static final RegraApuracao SEM_VENCEDOR = contagem -> new ResultadoApuracao.SemVencedor();
    private static final Instant CRIACAO = Instant.parse("2027-02-20T12:00:00Z");
    private static final Clock RELOGIO_FIXO = Clock.fixed(CRIACAO, ZoneOffset.UTC);

    /** Builder com todos os campos obrigatórios válidos; cada teste troca o que precisar. */
    private static Votacao.Builder configuracaoValida() {
        return Votacao.nova()
                .pergunta("Quem deve ser o presidente?")
                .opcoes(OPCOES)
                .regra(SEM_VENCEDOR)
                .totalAptos(10)
                .relogio(RELOGIO_FIXO);
    }

    private static Votacao novaVotacao(RegraApuracao regra, int totalAptos) {
        return configuracaoValida().regra(regra).totalAptos(totalAptos).build();
    }

    private static Votacao rascunho() {
        return novaVotacao(SEM_VENCEDOR, 10);
    }

    private static Votacao aberta() {
        Votacao votacao = rascunho();
        votacao.abrir();
        return votacao;
    }

    private static Votacao encerrada() {
        Votacao votacao = aberta();
        votacao.encerrar();
        return votacao;
    }

    private static Votacao apurada() {
        Votacao votacao = encerrada();
        votacao.apurar();
        return votacao;
    }

    @Nested
    @DisplayName("Transições válidas")
    class TransicoesValidas {

        @Test
        @DisplayName("votação criada fica em rascunho")
        void votacaoCriadaFicaEmRascunho() {
            assertEquals("rascunho", rascunho().status());
        }

        @Test
        @DisplayName("abrir leva rascunho para aberta")
        void abrirLevaRascunhoParaAberta() {
            assertEquals("aberta", aberta().status());
        }

        @Test
        @DisplayName("encerrar leva aberta para encerrada")
        void encerrarLevaAbertaParaEncerrada() {
            assertEquals("encerrada", encerrada().status());
        }

        @Test
        @DisplayName("apurar leva encerrada para apurada")
        void apurarLevaEncerradaParaApurada() {
            assertEquals("apurada", apurada().status());
        }

        @Test
        @DisplayName("alterar a regra em rascunho mantém rascunho")
        void alterarRegraEmRascunhoMantemRascunho() {
            Votacao votacao = rascunho();

            votacao.alterarRegra(new MaioriaAbsoluta(false));

            assertEquals("rascunho", votacao.status());
        }

        @Test
        @DisplayName("registrar voto com a votação aberta mantém aberta")
        void registrarVotoEmVotacaoAbertaMantemAberta() {
            Votacao votacao = aberta();

            votacao.registrarVoto("Gabriela");

            assertEquals("aberta", votacao.status());
        }
    }

    @Nested
    @DisplayName("Operações proibidas em rascunho")
    class ProibidasEmRascunho {

        @Test
        @DisplayName("não registra voto")
        void naoRegistraVotoEmVotacaoRascunho() {
            IllegalStateException erro = assertThrows(IllegalStateException.class,
                    () -> rascunho().registrarVoto("Gabriela"));

            assertEquals("não é possível registrar voto em votação rascunho", erro.getMessage());
        }

        @Test
        @DisplayName("não encerra")
        void naoEncerraVotacaoRascunho() {
            assertThrows(IllegalStateException.class, () -> rascunho().encerrar());
        }

        @Test
        @DisplayName("não apura")
        void naoApuraVotacaoRascunho() {
            assertThrows(IllegalStateException.class, () -> rascunho().apurar());
        }
    }

    @Nested
    @DisplayName("Operações proibidas com a votação aberta")
    class ProibidasComAberta {

        @Test
        @DisplayName("não altera a regra")
        void naoAlteraRegraComVotacaoAberta() {
            assertThrows(IllegalStateException.class, () -> aberta().alterarRegra(new MaioriaAbsoluta(true)));
        }

        @Test
        @DisplayName("não abre de novo")
        void naoAbreVotacaoJaAberta() {
            assertThrows(IllegalStateException.class, () -> aberta().abrir());
        }

        @Test
        @DisplayName("não apura")
        void naoApuraVotacaoAberta() {
            assertThrows(IllegalStateException.class, () -> aberta().apurar());
        }
    }

    @Nested
    @DisplayName("Operações proibidas com a votação encerrada")
    class ProibidasComEncerrada {

        @Test
        @DisplayName("não altera a regra")
        void naoAlteraRegraComVotacaoEncerrada() {
            assertThrows(IllegalStateException.class, () -> encerrada().alterarRegra(new MaioriaAbsoluta(true)));
        }

        @Test
        @DisplayName("não abre")
        void naoAbreVotacaoEncerrada() {
            assertThrows(IllegalStateException.class, () -> encerrada().abrir());
        }

        @Test
        @DisplayName("não registra voto")
        void naoRegistraVotoDepoisDeEncerrada() {
            assertThrows(IllegalStateException.class, () -> encerrada().registrarVoto("Gabriela"));
        }

        @Test
        @DisplayName("não encerra de novo")
        void naoEncerraVotacaoJaEncerrada() {
            assertThrows(IllegalStateException.class, () -> encerrada().encerrar());
        }
    }

    @Nested
    @DisplayName("Operações proibidas com a votação apurada")
    class ProibidasComApurada {

        @Test
        @DisplayName("não altera a regra")
        void naoAlteraRegraComVotacaoApurada() {
            assertThrows(IllegalStateException.class, () -> apurada().alterarRegra(new MaioriaAbsoluta(true)));
        }

        @Test
        @DisplayName("não abre")
        void naoAbreVotacaoApurada() {
            assertThrows(IllegalStateException.class, () -> apurada().abrir());
        }

        @Test
        @DisplayName("não registra voto")
        void naoRegistraVotoDepoisDeApurada() {
            assertThrows(IllegalStateException.class, () -> apurada().registrarVoto("Gabriela"));
        }

        @Test
        @DisplayName("não encerra")
        void naoEncerraVotacaoApurada() {
            assertThrows(IllegalStateException.class, () -> apurada().encerrar());
        }
    }

    @Nested
    @DisplayName("Validações do builder")
    class ValidacoesDoBuilder {

        @Test
        @DisplayName("guarda a pergunta sem espaços nas pontas")
        void guardaPerguntaSemEspacosNasPontas() {
            Votacao votacao = configuracaoValida().pergunta("  Quem deve ser o presidente?  ").build();

            assertEquals("Quem deve ser o presidente?", votacao.pergunta());
        }

        @Test
        @DisplayName("guarda as opções na ordem de cadastro")
        void guardaOpcoesNaOrdemDeCadastro() {
            assertEquals(OPCOES, rascunho().opcoes());
        }

        @Test
        @DisplayName("alterar a lista original não afeta as opções")
        void alteracaoNaListaOriginalNaoAfetaOpcoes() {
            List<String> opcoes = new ArrayList<>(OPCOES);
            Votacao.Builder builder = configuracaoValida().opcoes(opcoes);
            Votacao votacao = builder.build();

            opcoes.add("Joao");

            assertEquals(OPCOES, votacao.opcoes());
            assertEquals(OPCOES, builder.build().opcoes());
        }

        @Test
        @DisplayName("as opções devolvidas são imutáveis")
        void opcoesDevolvidasSaoImutaveis() {
            assertThrows(UnsupportedOperationException.class, () -> rascunho().opcoes().add("Joao"));
        }

        @Test
        @DisplayName("rejeita pergunta nula")
        void rejeitaPerguntaNula() {
            assertThrows(IllegalArgumentException.class, () -> configuracaoValida().pergunta(null).build());
        }

        @Test
        @DisplayName("rejeita pergunta vazia")
        void rejeitaPerguntaVazia() {
            assertThrows(IllegalArgumentException.class, () -> configuracaoValida().pergunta("   ").build());
        }

        @Test
        @DisplayName("rejeita opções nulas")
        void rejeitaOpcoesNulas() {
            assertThrows(IllegalArgumentException.class, () -> configuracaoValida().opcoes(null).build());
        }

        @Test
        @DisplayName("rejeita menos de duas opções")
        void rejeitaMenosDeDuasOpcoes() {
            assertThrows(IllegalArgumentException.class,
                    () -> configuracaoValida().opcoes(List.of("Gabriela")).build());
        }

        @Test
        @DisplayName("rejeita opção vazia")
        void rejeitaOpcaoVazia() {
            assertThrows(IllegalArgumentException.class,
                    () -> configuracaoValida().opcoes(List.of("Gabriela", " ")).build());
        }

        @Test
        @DisplayName("rejeita opção nula")
        void rejeitaOpcaoNula() {
            assertThrows(IllegalArgumentException.class,
                    () -> configuracaoValida().opcoes(Arrays.asList("Gabriela", null)).build());
        }

        @Test
        @DisplayName("rejeita opção repetida")
        void rejeitaOpcaoRepetida() {
            assertThrows(IllegalArgumentException.class,
                    () -> configuracaoValida().opcoes(List.of("Gabriela", "Fabricio", "Gabriela")).build());
        }

        @Test
        @DisplayName("rejeita regra nula")
        void rejeitaRegraNula() {
            assertThrows(IllegalArgumentException.class, () -> configuracaoValida().regra(null).build());
        }

        @Test
        @DisplayName("rejeita total de aptos zero")
        void rejeitaTotalAptosZero() {
            assertThrows(IllegalArgumentException.class, () -> configuracaoValida().totalAptos(0).build());
        }

        @Test
        @DisplayName("rejeita total de aptos negativo")
        void rejeitaTotalAptosNegativo() {
            assertThrows(IllegalArgumentException.class, () -> configuracaoValida().totalAptos(-1).build());
        }
    }

    @Nested
    @DisplayName("Configuração pelo builder (#19)")
    class ConfiguracaoPeloBuilder {

        @Test
        @DisplayName("rejeita votação sem pergunta informada")
        void rejeitaSemPergunta() {
            Votacao.Builder builder = Votacao.nova().opcoes(OPCOES).regra(SEM_VENCEDOR).totalAptos(10);

            IllegalArgumentException erro = assertThrows(IllegalArgumentException.class, builder::build);

            assertEquals("pergunta nao pode ser vazia", erro.getMessage());
        }

        @Test
        @DisplayName("rejeita votação sem opções informadas")
        void rejeitaSemOpcoes() {
            Votacao.Builder builder = Votacao.nova().pergunta("Pergunta").regra(SEM_VENCEDOR).totalAptos(10);

            assertThrows(IllegalArgumentException.class, builder::build);
        }

        @Test
        @DisplayName("rejeita votação sem regra informada")
        void rejeitaSemRegra() {
            Votacao.Builder builder = Votacao.nova().pergunta("Pergunta").opcoes(OPCOES).totalAptos(10);

            IllegalArgumentException erro = assertThrows(IllegalArgumentException.class, builder::build);

            assertEquals("regra de apuracao nao pode ser nula", erro.getMessage());
        }

        @Test
        @DisplayName("rejeita votação sem total de aptos informado")
        void rejeitaSemTotalAptos() {
            Votacao.Builder builder = Votacao.nova().pergunta("Pergunta").opcoes(OPCOES).regra(SEM_VENCEDOR);

            IllegalArgumentException erro = assertThrows(IllegalArgumentException.class, builder::build);

            assertEquals("totalAptos deve ser maior que zero", erro.getMessage());
        }

        @Test
        @DisplayName("sem descrição informada, a descrição é vazia")
        void descricaoPadraoEhVazia() {
            assertEquals("", rascunho().descricao());
        }

        @Test
        @DisplayName("guarda a descrição sem espaços nas pontas")
        void guardaDescricaoSemEspacosNasPontas() {
            Votacao votacao = configuracaoValida().descricao("  Eleição 2027–2029  ").build();

            assertEquals("Eleição 2027–2029", votacao.descricao());
        }

        @Test
        @DisplayName("rejeita descrição nula")
        void rejeitaDescricaoNula() {
            assertThrows(IllegalArgumentException.class, () -> configuracaoValida().descricao(null).build());
        }

        @Test
        @DisplayName("usa os nomes dos candidatos do cadastro como opções, na ordem de cadastro")
        void usaCandidatosDoCadastroComoOpcoes() {
            CadastroCandidatos cadastro = new CadastroCandidatos();
            cadastro.cadastrar("Chapa A", "10");
            cadastro.cadastrar("Chapa B", "20");

            Votacao votacao = configuracaoValida().opcoesDoCadastro(cadastro).build();

            assertEquals(List.of("Chapa A", "Chapa B"), votacao.opcoes());
        }

        @Test
        @DisplayName("cadastro com um só candidato não forma uma votação válida")
        void rejeitaCadastroComUmCandidato() {
            CadastroCandidatos cadastro = new CadastroCandidatos();
            cadastro.cadastrar("Chapa A", "10");

            assertThrows(IllegalArgumentException.class,
                    () -> configuracaoValida().opcoesDoCadastro(cadastro).build());
        }

        @Test
        @DisplayName("rejeita cadastro nulo")
        void rejeitaCadastroNulo() {
            assertThrows(IllegalArgumentException.class, () -> Votacao.nova().opcoesDoCadastro(null));
        }

        @Test
        @DisplayName("gera um id UUID diferente para cada votação")
        void geraIdUnico() {
            Votacao primeira = rascunho();
            Votacao segunda = rascunho();

            assertEquals(primeira.id(), UUID.fromString(primeira.id()).toString());
            assertNotEquals(primeira.id(), segunda.id());
        }

        @Test
        @DisplayName("data a criação pelo relógio configurado")
        void dataCriacaoPeloRelogio() {
            assertEquals(CRIACAO, rascunho().criadaEm());
        }

        @Test
        @DisplayName("rejeita relógio nulo")
        void rejeitaRelogioNulo() {
            assertThrows(IllegalArgumentException.class, () -> configuracaoValida().relogio(null).build());
        }

        @Test
        @DisplayName("expõe a regra configurada")
        void expoeRegraConfigurada() {
            RegraApuracao regra = new MaioriaSimples();

            assertSame(regra, novaVotacao(regra, 10).regra());
        }
    }

    /** Relógio de teste que só anda quando o teste manda. */
    private static final class RelogioAjustavel extends Clock {

        private Instant agora;

        RelogioAjustavel(Instant agora) {
            this.agora = agora;
        }

        void ajustarPara(Instant novoAgora) {
            this.agora = novoAgora;
        }

        @Override
        public Instant instant() {
            return agora;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }
    }

    @Nested
    @DisplayName("Período no ciclo (#20)")
    class PeriodoNoCiclo {

        private final Instant inicio = CRIACAO.plus(Duration.ofDays(1));
        private final Instant fim = inicio.plus(Duration.ofHours(10));
        private final PeriodoVotacao periodo = new PeriodoVotacao(inicio, fim);

        private Votacao comPeriodo(Clock relogio) {
            return configuracaoValida().periodo(periodo).relogio(relogio).build();
        }

        private Clock fixoEm(Instant instante) {
            return Clock.fixed(instante, ZoneOffset.UTC);
        }

        @Test
        @DisplayName("sem período informado, o período é vazio")
        void semPeriodoOPeriodoEhVazio() {
            assertEquals(Optional.empty(), rascunho().periodo());
        }

        @Test
        @DisplayName("guarda o período informado")
        void guardaPeriodo() {
            assertEquals(Optional.of(periodo), comPeriodo(RELOGIO_FIXO).periodo());
        }

        @Test
        @DisplayName("rejeita período nulo no builder")
        void rejeitaPeriodoNulo() {
            assertThrows(IllegalArgumentException.class, () -> Votacao.nova().periodo(null));
        }

        @Test
        @DisplayName("sem período, atualizar pelo relógio não muda nenhum estado")
        void semPeriodoAtualizarNaoFazNada() {
            Votacao votacao = rascunho();
            votacao.atualizarPeloRelogio();
            assertEquals("rascunho", votacao.status());

            votacao.abrir();
            votacao.atualizarPeloRelogio();
            assertEquals("aberta", votacao.status());
        }

        @Test
        @DisplayName("antes do início, continua em rascunho")
        void antesDoInicioContinuaEmRascunho() {
            Votacao votacao = comPeriodo(fixoEm(inicio.minusNanos(1)));

            votacao.atualizarPeloRelogio();

            assertEquals("rascunho", votacao.status());
        }

        @Test
        @DisplayName("no início exato, abre sozinha")
        void noInicioAbreSozinha() {
            Votacao votacao = comPeriodo(fixoEm(inicio));

            votacao.atualizarPeloRelogio();

            assertEquals("aberta", votacao.status());
        }

        @Test
        @DisplayName("um instante antes do fim, continua aberta")
        void logoAntesDoFimContinuaAberta() {
            Votacao votacao = comPeriodo(Clock.offset(fixoEm(fim), Duration.ofNanos(-1)));

            votacao.atualizarPeloRelogio();

            assertEquals("aberta", votacao.status());
        }

        @Test
        @DisplayName("no fim exato, encerra sozinha")
        void noFimEncerraSozinha() {
            RelogioAjustavel relogio = new RelogioAjustavel(inicio);
            Votacao votacao = comPeriodo(relogio);
            votacao.atualizarPeloRelogio();

            relogio.ajustarPara(fim);
            votacao.atualizarPeloRelogio();

            assertEquals("encerrada", votacao.status());
        }

        @Test
        @DisplayName("em rascunho com o período inteiro já passado, vai direto a encerrada")
        void periodoPassadoVaiDiretoAEncerrada() {
            Votacao votacao = comPeriodo(Clock.offset(fixoEm(fim), Duration.ofHours(1)));

            votacao.atualizarPeloRelogio();

            assertEquals("encerrada", votacao.status());
        }

        @Test
        @DisplayName("encerrada e apurada não mudam com o relógio")
        void encerradaEApuradaNaoMudamComORelogio() {
            RelogioAjustavel relogio = new RelogioAjustavel(inicio);
            Votacao votacao = comPeriodo(relogio);
            votacao.abrir();
            votacao.encerrar();

            votacao.atualizarPeloRelogio();
            assertEquals("encerrada", votacao.status());

            votacao.apurar();
            relogio.ajustarPara(fim);
            votacao.atualizarPeloRelogio();
            assertEquals("apurada", votacao.status());
        }

        @Test
        @DisplayName("o organizador pode abrir manualmente antes do início")
        void abreManualmenteAntesDoInicio() {
            Votacao votacao = comPeriodo(fixoEm(inicio.minusSeconds(60)));

            votacao.abrir();

            assertEquals("aberta", votacao.status());
        }

        @Test
        @DisplayName("o organizador pode encerrar manualmente antes do fim")
        void encerraManualmenteAntesDoFim() {
            Votacao votacao = comPeriodo(fixoEm(inicio));
            votacao.atualizarPeloRelogio();

            votacao.encerrar();

            assertEquals("encerrada", votacao.status());
        }

        @Test
        @DisplayName("o voto dentro do período abre a votação e é aceito")
        void votoDentroDoPeriodoAbreEAceita() {
            Votacao votacao = comPeriodo(fixoEm(inicio));

            votacao.registrarVoto("Gabriela");

            assertEquals("aberta", votacao.status());
        }

        @Test
        @DisplayName("rejeita voto antes do início, mesmo com a votação aberta manualmente")
        void rejeitaVotoAntesDoInicio() {
            Votacao votacao = comPeriodo(fixoEm(inicio.minusNanos(1)));
            votacao.abrir();

            IllegalStateException erro = assertThrows(IllegalStateException.class,
                    () -> votacao.registrarVoto("Gabriela"));

            assertEquals("voto fora do periodo de votacao", erro.getMessage());
        }

        @Test
        @DisplayName("rejeita voto em rascunho antes do início")
        void rejeitaVotoEmRascunhoAntesDoInicio() {
            Votacao votacao = comPeriodo(fixoEm(inicio.minusNanos(1)));

            IllegalStateException erro = assertThrows(IllegalStateException.class,
                    () -> votacao.registrarVoto("Gabriela"));

            assertEquals("voto fora do periodo de votacao", erro.getMessage());
            assertEquals("rascunho", votacao.status());
        }

        @Test
        @DisplayName("rejeita voto no fim exato e encerra a votação")
        void rejeitaVotoNoFimEEncerra() {
            RelogioAjustavel relogio = new RelogioAjustavel(inicio);
            Votacao votacao = comPeriodo(relogio);
            votacao.registrarVoto("Gabriela");

            relogio.ajustarPara(fim);
            IllegalStateException erro = assertThrows(IllegalStateException.class,
                    () -> votacao.registrarVoto("Fabricio"));

            assertEquals("voto fora do periodo de votacao", erro.getMessage());
            assertEquals("encerrada", votacao.status());
        }
    }

    @Nested
    @DisplayName("Registro de votos")
    class RegistroDeVotos {

        @Test
        @DisplayName("não registra voto em opção inexistente")
        void naoRegistraVotoEmOpcaoInexistente() {
            assertThrows(IllegalArgumentException.class, () -> aberta().registrarVoto("Joao"));
        }

        @Test
        @DisplayName("não registra voto além do total de aptos")
        void naoRegistraVotoAlemDoTotalDeAptos() {
            Votacao votacao = novaVotacao(SEM_VENCEDOR, 2);
            votacao.abrir();
            votacao.registrarVoto("Gabriela");
            votacao.registrarVoto("Fabricio");

            assertThrows(IllegalStateException.class, () -> votacao.registrarVoto("Juliana"));
        }
    }

    @Nested
    @DisplayName("Apuração")
    class Apuracao {

        @Test
        @DisplayName("apurar delega à regra com a contagem dos votos")
        void apurarDelegaARegraComAContagemDosVotos() {
            List<ContagemVotos> recebidas = new ArrayList<>();
            ResultadoApuracao esperado = new ResultadoApuracao.Eleito("Gabriela");
            Votacao votacao = novaVotacao(contagem -> {
                recebidas.add(contagem);
                return esperado;
            }, 10);
            votacao.abrir();
            votacao.registrarVoto("Gabriela");
            votacao.registrarVoto("Gabriela");
            votacao.registrarVoto("Juliana");
            votacao.encerrar();

            ResultadoApuracao resultado = votacao.apurar();

            assertSame(esperado, resultado);
            assertEquals(List.of(new ContagemVotos(Map.of("Gabriela", 2, "Fabricio", 0, "Juliana", 1), 10)),
                    recebidas);
        }

        @Test
        @DisplayName("apurar de novo devolve o mesmo resultado sem reapurar")
        void apurarDeNovoDevolveOMesmoResultadoSemReapurar() {
            List<ContagemVotos> recebidas = new ArrayList<>();
            Votacao votacao = novaVotacao(contagem -> {
                recebidas.add(contagem);
                return new ResultadoApuracao.SemVencedor();
            }, 10);
            votacao.abrir();
            votacao.encerrar();

            ResultadoApuracao primeiro = votacao.apurar();
            ResultadoApuracao segundo = votacao.apurar();

            assertSame(primeiro, segundo);
            assertEquals(1, recebidas.size());
        }

        @Test
        @DisplayName("alterar a regra em rascunho troca a regra usada na apuração")
        void alterarRegraEmRascunhoTrocaARegraUsadaNaApuracao() {
            ResultadoApuracao esperado = new ResultadoApuracao.QuorumNaoAtingido();
            Votacao votacao = rascunho();
            votacao.alterarRegra(contagem -> esperado);
            votacao.abrir();
            votacao.encerrar();

            assertSame(esperado, votacao.apurar());
        }

        @Test
        @DisplayName("não altera a regra para nula")
        void naoAlteraRegraParaNula() {
            assertThrows(IllegalArgumentException.class, () -> rascunho().alterarRegra(null));
        }

        /**
         * Teste de mutação (Atividade 5, C4): mata o mutante do PIT que troca
         * {@code opcoes.size() < MINIMO_OPCOES} por {@code <=} em
         * {@code Votacao.validarOpcoes}. O limite de exatamente 2 opções é o caso
         * mais comum do domínio (aprovar/rejeitar, 2º turno) e não era exercitado
         * pela suíte, que só criava votações com 1 ou com 3 opções.
         */
        @Test
        @DisplayName("votação com o mínimo de duas opções é aceita e apurada de ponta a ponta")
        void votacaoComOMinimoDeDuasOpcoesEhAceitaEApuradaDePontaAPonta() {
            Votacao votacao = configuracaoValida()
                    .pergunta("Aprovar a prestação de contas?")
                    .opcoes(List.of("Aprovar", "Rejeitar"))
                    .regra(new MaioriaSimples())
                    .build();
            votacao.abrir();
            for (String voto : List.of("Aprovar", "Aprovar", "Rejeitar")) {
                votacao.registrarVoto(voto);
            }
            votacao.encerrar();

            assertEquals(List.of("Aprovar", "Rejeitar"), votacao.opcoes());
            assertEquals(new ResultadoApuracao.Eleito("Aprovar"), votacao.apurar());
            assertEquals("apurada", votacao.status());
        }

        @Test
        @DisplayName("votação de ponta a ponta com maioria absoluta")
        void votacaoDePontaAPontaComMaioriaAbsoluta() {
            Votacao votacao = novaVotacao(new MaioriaAbsoluta(true), 10);
            votacao.abrir();
            for (String voto : List.of("Gabriela", "Gabriela", "Gabriela", "Fabricio", "Fabricio", "Juliana")) {
                votacao.registrarVoto(voto);
            }
            votacao.encerrar();

            // Gabriela tem exatamente 50% dos votos válidos: não é maioria absoluta

            assertEquals(new ResultadoApuracao.SegundoTurno("Gabriela", "Fabricio"), votacao.apurar());
            assertEquals("apurada", votacao.status());
        }
    }
}

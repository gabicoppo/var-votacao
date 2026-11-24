package br.unicamp.var.web.organizador;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.unicamp.var.auditoria.EventoAuditoria;
import br.unicamp.var.auditoria.TipoEvento;
import br.unicamp.var.processo.Votacao;
import br.unicamp.var.top3.CategoriaEleitor;
import br.unicamp.var.top3.PremiacaoTop3;
import br.unicamp.var.top3.StatusPremiacao;
import br.unicamp.var.urna.Sigilo;
import br.unicamp.var.web.comum.ContasUsuariosEmMemoria;
import br.unicamp.var.web.comum.RepositorioPremiacoesEmMemoria;
import br.unicamp.var.web.comum.RepositorioVotacoesEmMemoria;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Testes do serviço {@link GestaoVotacoes}, sem Spring: criação com códigos de acesso, ciclo com
 * auditoria, 2º turno e os <em>view models</em> do painel e da gestão.
 */
class GestaoVotacoesTest {

    private static final Clock RELOGIO = Clock.fixed(Instant.parse("2027-02-20T12:00:00Z"), ZoneOffset.UTC);
    private static final String ORGANIZADOR = "organizador";

    private RepositorioVotacoesEmMemoria votacoes;
    private RepositorioPremiacoesEmMemoria premiacoes;
    private ContasUsuariosEmMemoria contas;
    private GestaoVotacoes gestao;

    @BeforeEach
    void preparar() {
        votacoes = new RepositorioVotacoesEmMemoria();
        premiacoes = new RepositorioPremiacoesEmMemoria(RELOGIO);
        contas = new ContasUsuariosEmMemoria(new BCryptPasswordEncoder(4), false);
        gestao = new GestaoVotacoes(votacoes, premiacoes, contas, RELOGIO);
    }

    private static FormularioVotacao formulario(String opcoes, String eleitores) {
        FormularioVotacao f = new FormularioVotacao();
        f.setPergunta("Quem deve ser o presidente?");
        f.setOpcoes(opcoes);
        f.setEleitores(eleitores);
        return f;
    }

    private String criar(FormularioVotacao f) {
        return gestao.criar(f, ORGANIZADOR).id();
    }

    private Votacao votacao(String id) {
        return votacoes.buscar(id).orElseThrow();
    }

    /** Cria, abre, registra os votos (e1, e2... na ordem), encerra e apura. */
    private String apurada(String opcoes, String... votos) {
        StringBuilder eleitores = new StringBuilder();
        for (int i = 1; i <= Math.max(votos.length, 1); i++) {
            eleitores.append('e').append(i).append(";Eleitor ").append(i).append('\n');
        }
        String id = criar(formulario(opcoes, eleitores.toString()));
        gestao.abrir(id, ORGANIZADOR);
        for (int i = 0; i < votos.length; i++) {
            votacao(id).registrarVoto("e" + (i + 1), votos[i]);
        }
        gestao.encerrar(id, ORGANIZADOR);
        gestao.apurar(id, ORGANIZADOR);
        return id;
    }

    @Nested
    @DisplayName("Criação (#19, #24)")
    class Criacao {

        @Test
        @DisplayName("salva a votação em rascunho e gera códigos só para as contas novas")
        void criaComCodigos() {
            contas.garantirContaEleitor("e01", "Ana");

            ResultadoCriacao resultado = gestao.criar(formulario("A\nB", "e01;Ana\ne02;Bruno\ne03;Carla"), ORGANIZADOR);

            assertTrue(resultado.criada());
            assertEquals("rascunho", votacao(resultado.id()).status());
            assertEquals(List.of("e02", "e03"), resultado.acessos().stream().map(AcessoEleitor::eleitorId).toList());
            assertEquals("Bruno", resultado.acessos().get(0).nome());
            assertEquals(ContasUsuariosEmMemoria.TAMANHO_CODIGO, resultado.acessos().get(0).codigo().length());
            assertEquals("Bruno", contas.nomeDe("e02").orElseThrow());
        }

        @Test
        @DisplayName("na segunda votação com os mesmos eleitores, não gera códigos")
        void semCodigosNaSegunda() {
            gestao.criar(formulario("A\nB", "e01;Ana"), ORGANIZADOR);

            assertEquals(List.of(), gestao.criar(formulario("C\nD", "e01;Ana"), ORGANIZADOR).acessos());
        }

        @Test
        @DisplayName("registra o organizador como autor da criação")
        void autorNaAuditoria() {
            String id = criar(formulario("A\nB", "e01;Ana"));

            EventoAuditoria criada = votacao(id).auditoria().get(0);
            assertEquals(TipoEvento.CRIADA, criada.tipo());
            assertEquals(ORGANIZADOR, criada.autor());
        }

        @Test
        @DisplayName("formulário inválido não salva nada e devolve os erros por campo")
        void formularioInvalido() {
            ResultadoCriacao resultado = gestao.criar(formulario("A", ""), ORGANIZADOR);

            assertFalse(resultado.criada());
            assertNull(resultado.id());
            assertEquals(List.of("opcoes", "eleitores"), resultado.erros().stream().map(ErroCampo::campo).toList());
            assertEquals(List.of(), votacoes.listar());
        }
    }

    @Nested
    @DisplayName("Ciclo e auditoria (RNF-07)")
    class Ciclo {

        @Test
        @DisplayName("abrir, encerrar e apurar registram o autor certo")
        void transicoesComAutor() {
            String id = criar(formulario("A\nB", "e1;Ana"));

            gestao.abrir(id, "ana.org");
            votacao(id).registrarVoto("e1", "A");
            gestao.encerrar(id, "bruno.org");
            String situacao = gestao.apurar(id, "carla.org");

            assertEquals("Eleito: A", situacao);
            List<EventoAuditoria> eventos = votacao(id).auditoria();
            assertEquals(List.of(TipoEvento.CRIADA, TipoEvento.ABERTA, TipoEvento.ENCERRADA, TipoEvento.APURADA),
                    eventos.stream().map(EventoAuditoria::tipo).toList());
            assertEquals(List.of(ORGANIZADOR, "ana.org", "bruno.org", "carla.org"),
                    eventos.stream().map(EventoAuditoria::autor).toList());
        }

        @Test
        @DisplayName("operação no estado errado vira erro amigável")
        void estadoErrado() {
            String id = criar(formulario("A\nB", "e1;Ana"));

            OperacaoRecusadaException encerrar = assertThrows(OperacaoRecusadaException.class,
                    () -> gestao.encerrar(id, ORGANIZADOR));
            OperacaoRecusadaException apurar = assertThrows(OperacaoRecusadaException.class,
                    () -> gestao.apurar(id, ORGANIZADOR));

            assertEquals("Esta ação não é permitida com a votação em rascunho.", encerrar.getMessage());
            assertEquals("Esta ação não é permitida com a votação em rascunho.", apurar.getMessage());
        }

        @Test
        @DisplayName("votação inexistente lança VotacaoNaoEncontradaException")
        void inexistente() {
            assertThrows(VotacaoNaoEncontradaException.class, () -> gestao.abrir("nao-existe", ORGANIZADOR));
            assertThrows(VotacaoNaoEncontradaException.class, () -> gestao.detalhe("nao-existe"));
            assertThrows(VotacaoNaoEncontradaException.class, () -> gestao.pergunta("nao-existe"));
        }
    }

    @Nested
    @DisplayName("2º turno (#21)")
    class SegundoTurno {

        @Test
        @DisplayName("é criado, salvo e ligado à votação anterior")
        void criaSalvaELiga() {
            String id = apurada("A\nB\nC", "A", "B", "C", "A", "B");

            Votacao segundo = gestao.criarSegundoTurno(id, ORGANIZADOR);

            assertEquals(segundo, votacao(segundo.id()));
            assertEquals(id, segundo.turnoAnteriorId().orElseThrow());
            assertEquals(List.of("A", "B"), segundo.opcoes());
            assertEquals(2, segundo.turno());
            DetalheVotacao primeiro = gestao.detalhe(id);
            assertEquals(segundo.id(), primeiro.segundoTurnoId());
            assertFalse(primeiro.podeCriarSegundoTurno());
            assertEquals(id, gestao.detalhe(segundo.id()).turnoAnteriorId());
        }

        @Test
        @DisplayName("antes de criar, a gestão oferece o 2º turno")
        void ofereceSegundoTurno() {
            DetalheVotacao detalhe = gestao.detalhe(apurada("A\nB\nC", "A", "B", "C", "A", "B"));

            assertTrue(detalhe.podeCriarSegundoTurno());
            assertNull(detalhe.segundoTurnoId());
            assertEquals("2º turno entre A e B", detalhe.situacao());
        }

        @Test
        @DisplayName("criar duas vezes dá erro amigável")
        void duasVezes() {
            String id = apurada("A\nB\nC", "A", "B", "C", "A", "B");
            gestao.criarSegundoTurno(id, ORGANIZADOR);

            OperacaoRecusadaException erro = assertThrows(OperacaoRecusadaException.class,
                    () -> gestao.criarSegundoTurno(id, ORGANIZADOR));

            assertEquals("O 2º turno desta votação já foi criado.", erro.getMessage());
        }

        @Test
        @DisplayName("sem resultado de 2º turno dá erro amigável")
        void semSegundoTurno() {
            String id = apurada("A\nB", "A", "A", "B");

            OperacaoRecusadaException erro = assertThrows(OperacaoRecusadaException.class,
                    () -> gestao.criarSegundoTurno(id, ORGANIZADOR));

            assertEquals("O 2º turno só pode ser criado quando a apuração indica 2º turno.", erro.getMessage());
            assertFalse(gestao.detalhe(id).podeCriarSegundoTurno());
        }
    }

    @Nested
    @DisplayName("Painel")
    class Painel {

        @Test
        @DisplayName("sem nada cadastrado, tudo zerado")
        void vazio() {
            PainelOrganizador painel = gestao.painel(null);

            assertEquals(0, painel.totalVotacoes());
            assertEquals(List.of(), painel.votacoes());
            assertEquals(List.of(), painel.premiacoes());
            assertEquals("todas", painel.filtro());
        }

        @Test
        @DisplayName("conta votações por status e os votos registrados, e filtra pela aba")
        void metricasEFiltro() {
            criar(formulario("A\nB", "e1;Ana"));
            String aberta = criar(formulario("A\nB", "e1;Ana\ne2;Bia"));
            gestao.abrir(aberta, ORGANIZADOR);
            votacao(aberta).registrarVoto("e1", "A");
            apurada("A\nB", "A", "B", "A");

            PainelOrganizador painel = gestao.painel("aberta");

            assertEquals(List.of(1, 1, 1, 4), List.of(painel.abertas(), painel.rascunhos(), painel.apuradas(),
                    painel.totalVotos()));
            assertEquals(3, painel.totalVotacoes());
            assertEquals("aberta", painel.filtro());
            CartaoVotacao cartao = painel.votacoes().get(0);
            assertEquals(List.of(aberta), painel.votacoes().stream().map(CartaoVotacao::id).toList());
            assertEquals(1, cartao.votantes());
            assertEquals(2, cartao.aptos());
            assertEquals(0.5, cartao.participacao());
            assertEquals("Maioria absoluta", cartao.regra());
            assertFalse(cartao.temPeriodo());
            assertEquals(List.of("todas:3", "rascunho:1", "aberta:1", "encerrada:0", "apurada:1"),
                    painel.abas().stream().map(aba -> aba.status() + ":" + aba.quantidade()).toList());
            assertTrue(painel.abas().get(2).ativa());
        }

        @Test
        @DisplayName("ordena os cards pelo que pede ação: abertas, encerradas, rascunhos e apuradas")
        void ordenaPorPrioridade() {
            apurada("A\nB", "A");
            criar(formulario("A\nB", "e1;Ana"));
            String aberta = criar(formulario("A\nB", "e1;Ana"));
            gestao.abrir(aberta, ORGANIZADOR);
            String encerrada = criar(formulario("A\nB", "e1;Ana"));
            gestao.abrir(encerrada, ORGANIZADOR);
            gestao.encerrar(encerrada, ORGANIZADOR);

            assertEquals(List.of("aberta", "encerrada", "rascunho", "apurada"),
                    gestao.painel(null).votacoes().stream().map(CartaoVotacao::status).toList());
        }

        @Test
        @DisplayName("filtro desconhecido mostra todas")
        void filtroDesconhecido() {
            criar(formulario("A\nB", "e1;Ana"));

            assertEquals(1, gestao.painel("xyz").votacoes().size());
            assertEquals("todas", gestao.painel("xyz").filtro());
        }

        @Test
        @DisplayName("lista as premiações com status e cédulas")
        void premiacoes() {
            premiacoes.salvar(new PremiacaoTop3("Craque do Campeonato", "", List.of("X", "Y", "Z"),
                    List.of(new CategoriaEleitor("Torcida", 1)), Map.of("e1", "Torcida")));

            PainelOrganizador.CartaoPremiacao cartao = gestao.painel(null).premiacoes().get(0);

            assertEquals("Craque do Campeonato", cartao.titulo());
            assertEquals(StatusPremiacao.RASCUNHO, cartao.status());
            assertEquals(0, cartao.cedulas());
        }

        @Test
        @DisplayName("cartão mostra o período quando há")
        void cartaoComPeriodo() {
            FormularioVotacao f = formulario("A\nB", "e1;Ana");
            f.setInicio(LocalDateTime.of(2027, 3, 1, 8, 0));
            f.setFim(LocalDateTime.of(2027, 3, 1, 18, 0));
            criar(f);

            CartaoVotacao cartao = gestao.painel(null).votacoes().get(0);

            assertTrue(cartao.temPeriodo());
            assertEquals(Instant.parse("2027-03-01T11:00:00Z"), cartao.inicio());
        }
    }

    @Nested
    @DisplayName("Gestão da votação")
    class Gestao {

        @Test
        @DisplayName("rascunho: só pode abrir; mostra configuração, presença e auditoria")
        void rascunho() {
            FormularioVotacao f = formulario("A\nB", "e1;Ana\ne2;Bia");
            f.setInicio(LocalDateTime.of(2027, 3, 1, 8, 0));
            f.setFim(LocalDateTime.of(2027, 3, 1, 18, 0));
            DetalheVotacao d = gestao.detalhe(criar(f));

            assertEquals(List.of(true, false, false, false),
                    List.of(d.podeAbrir(), d.podeEncerrar(), d.podeApurar(), d.apurada()));
            assertEquals("Secreto", d.configuracao().sigilo());
            assertTrue(d.configuracao().temPeriodo());
            assertTrue(d.configuracao().regra().startsWith("Maioria absoluta"));
            assertEquals(2, d.aptos());
            assertEquals(0.0, d.participacao());
            assertNull(d.situacao());
            assertEquals(List.of("e1", "e2"), d.presenca().stream().map(DetalheVotacao.Presenca::eleitorId).toList());
            assertEquals("Votação criada", d.auditoria().get(0).evento());
            assertEquals(ORGANIZADOR, d.auditoria().get(0).autor());
        }

        @Test
        @DisplayName("aberta: só pode encerrar e marca quem votou")
        void aberta() {
            String id = criar(formulario("A\nB", "e1;Ana\ne2;Bia"));
            gestao.abrir(id, ORGANIZADOR);
            votacao(id).registrarVoto("e1", "A");

            DetalheVotacao d = gestao.detalhe(id);

            assertEquals(List.of(false, true, false), List.of(d.podeAbrir(), d.podeEncerrar(), d.podeApurar()));
            assertEquals(List.of(true, false), d.presenca().stream().map(DetalheVotacao.Presenca::votou).toList());
            assertEquals(0.5, d.participacao());
        }

        @Test
        @DisplayName("votação secreta nunca mostra a escolha de ninguém, nem apurada")
        void secretaSemEscolhas() {
            DetalheVotacao d = gestao.detalhe(apurada("A\nB", "A", "B", "A"));

            assertFalse(d.mostraEscolhas());
            assertTrue(d.presenca().stream().allMatch(p -> p.escolha() == null));
        }

        @Test
        @DisplayName("votação identificada mostra as escolhas só depois de encerrada")
        void identificadaComEscolhas() {
            FormularioVotacao f = formulario("A\nB", "e1;Ana\ne2;Bia");
            f.setSigilo(Sigilo.IDENTIFICADO);
            String id = criar(f);
            gestao.abrir(id, ORGANIZADOR);
            votacao(id).registrarVoto("e1", "B");
            assertFalse(gestao.detalhe(id).mostraEscolhas());

            gestao.encerrar(id, ORGANIZADOR);
            DetalheVotacao d = gestao.detalhe(id);

            assertTrue(d.mostraEscolhas());
            assertTrue(d.podeApurar());
            assertEquals("Identificado (voto nominal)", d.configuracao().sigilo());
            assertEquals(java.util.Arrays.asList("B", null),
                    d.presenca().stream().map(DetalheVotacao.Presenca::escolha).toList());
        }

        @Test
        @DisplayName("eleitores de demonstração usam os nomes das contas, se existirem")
        void eleitoresDemonstracao() {
            contas.garantirContaEleitor("e01", "Ana Ribeiro");

            String texto = gestao.eleitoresDemonstracao();

            assertTrue(texto.startsWith("e01;Ana Ribeiro\ne02;Eleitor 2\n"));
            assertTrue(texto.endsWith("e12;Eleitor 12"));
        }
    }

    @Nested
    @DisplayName("Rótulos")
    class Rotulos {

        @Test
        @DisplayName("eventos e detalhes da auditoria em português")
        void eventos() {
            assertEquals("Regra alterada", RotulosVotacao.evento(TipoEvento.REGRA_ALTERADA));
            assertEquals("2º turno criado", RotulosVotacao.evento(TipoEvento.SEGUNDO_TURNO_CRIADO));
            assertEquals("Automático: início do período",
                    RotulosVotacao.detalhe(TipoEvento.ABERTA, "inicio do periodo"));
            assertEquals("Automático: fim do período", RotulosVotacao.detalhe(TipoEvento.ENCERRADA, "fim do periodo"));
            assertEquals("2º turno da votação 12345678",
                    RotulosVotacao.detalhe(TipoEvento.CRIADA, "2º turno da votacao 1234567890ab"));
            assertEquals("Nova votação abc", RotulosVotacao.detalhe(TipoEvento.SEGUNDO_TURNO_CRIADO, "abc"));
            assertEquals("Eleito: A", RotulosVotacao.detalhe(TipoEvento.APURADA, "Eleito: A"));
            assertEquals("Regra personalizada", RotulosVotacao.regraResumida("Regra personalizada"));
        }
    }
}

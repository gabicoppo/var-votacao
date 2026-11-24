package br.unicamp.var.web.comum;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.unicamp.var.apuracao.MaioriaAbsoluta;
import br.unicamp.var.apuracao.MaioriaQualificada;
import br.unicamp.var.apuracao.MaioriaSimples;
import br.unicamp.var.apuracao2turno.ResultadoApuracao;
import br.unicamp.var.auditoria.EventoAuditoria;
import br.unicamp.var.processo.Votacao;
import br.unicamp.var.top3.PremiacaoTop3;
import br.unicamp.var.top3.StatusPremiacao;
import br.unicamp.var.urna.Sigilo;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Dados de demonstração: cada votação e a premiação no estado e com as contagens descritas.
 */
class DadosDemonstracaoTest {

    private static final Instant AGORA = Instant.parse("2026-11-24T15:00:00Z");

    private final RepositorioVotacoes votacoes = new RepositorioVotacoesEmMemoria();
    private final Clock relogio = Clock.fixed(AGORA, ZoneOffset.UTC);
    private final RepositorioPremiacoes premiacoes = new RepositorioPremiacoesEmMemoria(relogio);
    private final ContasUsuarios contas = new ContasUsuariosEmMemoria(new BCryptPasswordEncoder(4), true);

    /** Carrega no construtor: as classes aninhadas leem as votações nos próprios campos. */
    DadosDemonstracaoTest() {
        new DadosDemonstracao(votacoes, premiacoes, contas, relogio).run(new DefaultApplicationArguments());
    }

    private Votacao votacao(String pergunta) {
        return votacoes.listar().stream().filter(v -> v.pergunta().equals(pergunta)).findFirst().orElseThrow();
    }

    @Test
    @DisplayName("cria 4 votações e 1 premiação, com os eleitores e01..e12 e os nomes das contas")
    void quantidades() {
        assertEquals(4, votacoes.listar().size());
        assertEquals(1, premiacoes.listar().size());
        Votacao enquete = votacao(DadosDemonstracao.ENQUETE);
        assertEquals(12, enquete.eleitorado().total());
        assertEquals("Ana Ribeiro", enquete.eleitorado().buscar("e01").orElseThrow().nome());
    }

    @Test
    @DisplayName("toda a auditoria tem o organizador como autor")
    void auditoriaComOrganizador() {
        for (Votacao votacao : votacoes.listar()) {
            assertTrue(votacao.auditoria().stream().map(EventoAuditoria::autor)
                    .allMatch(DadosDemonstracao.ORGANIZADOR::equals), votacao.pergunta());
        }
    }

    @Nested
    @DisplayName("Eleição presidencial")
    class Presidencial {

        private final Votacao votacao = votacao(DadosDemonstracao.PRESIDENCIAL);

        @Test
        @DisplayName("aberta, secreta, maioria absoluta com 2º turno e período de ontem a daqui a 3 dias")
        void configuracao() {
            assertEquals("aberta", votacao.status());
            assertEquals(Sigilo.SECRETO, votacao.sigilo());
            assertInstanceOf(MaioriaAbsoluta.class, votacao.regra());
            assertEquals(3, votacao.opcoes().size());
            assertEquals(AGORA.minus(Duration.ofDays(1)), votacao.periodo().orElseThrow().inicio());
            assertEquals(AGORA.plus(Duration.ofDays(3)), votacao.periodo().orElseThrow().fim());
        }

        @Test
        @DisplayName("e05..e11 votaram; e01..e04 e e12 ainda podem votar")
        void votantes() {
            assertEquals(7, votacao.totalVotantes());
            IntStream.rangeClosed(5, 11).forEach(i -> assertTrue(votacao.jaVotou(String.format("e%02d", i))));
            IntStream.rangeClosed(1, 4).forEach(i -> assertFalse(votacao.jaVotou("e0" + i)));
            assertFalse(votacao.jaVotou("e12"));
        }

        @Test
        @DisplayName("ninguém tem maioria absoluta: encerrada agora, iria a 2º turno")
        void semMaioriaAbsoluta() {
            votacao.encerrar();
            assertInstanceOf(ResultadoApuracao.SegundoTurno.class, votacao.apurar());
        }
    }

    @Test
    @DisplayName("Conselho Fiscal: apurado com 2º turno, pronto para o botão \"Criar 2º turno\"")
    void conselhoFiscal() {
        Votacao votacao = votacao(DadosDemonstracao.CONSELHO);

        assertEquals("apurada", votacao.status());
        ResultadoApuracao.SegundoTurno turno = assertInstanceOf(ResultadoApuracao.SegundoTurno.class, votacao.apurar());
        assertEquals("Chapa Transparência", turno.primeiroColocado());
        assertEquals("Chapa Contas Abertas", turno.segundoColocado());
        assertEquals(9, votacao.totalVotantes());
    }

    @Test
    @DisplayName("Assembleia: identificada, maioria qualificada, apurada com 7 de 9 em Aprovar")
    void assembleia() {
        Votacao votacao = votacao(DadosDemonstracao.ASSEMBLEIA);

        assertEquals("apurada", votacao.status());
        assertEquals(Sigilo.IDENTIFICADO, votacao.sigilo());
        assertInstanceOf(MaioriaQualificada.class, votacao.regra());
        assertEquals(new ResultadoApuracao.Eleito("Aprovar"), votacao.apurar());
        assertEquals(9, votacao.totalVotantes());
        assertEquals(7, votacao.resultadoDetalhado().linhas().get(0).votos());
    }

    @Test
    @DisplayName("Enquete: Sim/Não, maioria simples, em rascunho")
    void enquete() {
        Votacao votacao = votacao(DadosDemonstracao.ENQUETE);

        assertEquals("rascunho", votacao.status());
        assertEquals(List.of("Sim", "Não"), votacao.opcoes());
        assertInstanceOf(MaioriaSimples.class, votacao.regra());
    }

    @Test
    @DisplayName("Craque do Campeonato: aberta, 5 jogadores, 2 grupos e cédulas de e05..e09")
    void craque() {
        PremiacaoTop3 premiacao = premiacoes.listar().get(0).premiacao();

        assertEquals(DadosDemonstracao.CRAQUE, premiacao.titulo());
        assertEquals(StatusPremiacao.ABERTA, premiacao.status());
        assertEquals(5, premiacao.candidatos().size());
        assertEquals(2.0, premiacao.categorias().get(0).peso());
        assertEquals(1.0, premiacao.categorias().get(1).peso());
        assertEquals(5, premiacao.totalCedulas());
        assertTrue(premiacao.jaVotou("e05"));
        assertFalse(premiacao.jaVotou("e01"));
    }
}

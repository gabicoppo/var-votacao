package br.unicamp.var.processo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.unicamp.var.apuracao.MaioriaAbsoluta;
import br.unicamp.var.apuracao.MaioriaSimples;
import br.unicamp.var.apuracao2turno.ResultadoApuracao;
import br.unicamp.var.auditoria.EventoAuditoria;
import br.unicamp.var.auditoria.TipoEvento;
import br.unicamp.var.eleitorado.Eleitor;
import br.unicamp.var.eleitorado.Eleitorado;
import br.unicamp.var.urna.Sigilo;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * 2º turno da {@link Votacao} (história #21; transição "Aguardando 2º turno → novo turno" do
 * diagrama de estados da A1).
 */
final class VotacaoSegundoTurnoTest {

    private static final Instant CRIACAO = Instant.parse("2027-02-20T12:00:00Z");
    private static final Clock RELOGIO = Clock.fixed(CRIACAO, ZoneOffset.UTC);
    private static final String MENSAGEM = "segundo turno so existe quando a apuracao indica segundo turno";
    private static final List<String> CHAPAS = List.of("Chapa A", "Chapa B", "Chapa C");
    private static final int ELEITORES = 10;

    private static Eleitorado eleitorado() {
        return Eleitorado.de(IntStream.rangeClosed(1, ELEITORES)
                .mapToObj(i -> new Eleitor("e" + i, "Eleitor " + i))
                .toList());
    }

    private static Votacao.Builder eleicao() {
        return Votacao.nova()
                .pergunta("Eleição para presidente do clube")
                .descricao("Mandato 2027-2029")
                .opcoes(CHAPAS)
                .regra(new MaioriaAbsoluta(true))
                .eleitorado(eleitorado())
                .relogio(RELOGIO);
    }

    /** Vota um eleitor por opção, a partir de "e1". */
    private static void votar(Votacao votacao, String... opcoes) {
        for (int i = 0; i < opcoes.length; i++) {
            votacao.registrarVoto("e" + (i + 1), opcoes[i]);
        }
    }

    /** Chapa B 4, Chapa A 3, Chapa C 3: ninguém passa de 50% e o empate no 2º lugar vai para A. */
    private static Votacao apuradaComSegundoTurno(Votacao.Builder builder) {
        Votacao votacao = builder.build();
        votacao.abrir();
        votar(votacao, "Chapa B", "Chapa A", "Chapa C", "Chapa B", "Chapa A",
                "Chapa C", "Chapa B", "Chapa A", "Chapa C", "Chapa B");
        votacao.encerrar();
        votacao.apurar();
        return votacao;
    }

    @Nested
    @DisplayName("Quando o 2º turno pode ser criado")
    final class Condicoes {

        @Test
        @DisplayName("em rascunho, aberta ou encerrada é recusado")
        void antesDaApuracaoLancaExcecao() {
            Votacao votacao = eleicao().build();
            assertEquals(MENSAGEM, assertThrows(IllegalStateException.class, votacao::criarSegundoTurno).getMessage());

            votacao.abrir();
            assertThrows(IllegalStateException.class, votacao::criarSegundoTurno);

            votacao.encerrar();
            assertThrows(IllegalStateException.class, votacao::criarSegundoTurno);
        }

        @Test
        @DisplayName("apurada com Eleito é recusado")
        void apuradaComEleitoLancaExcecao() {
            Votacao votacao = eleicao().build();
            votacao.abrir();
            votar(votacao, "Chapa A", "Chapa A", "Chapa B");
            votacao.encerrar();
            assertInstanceOf(ResultadoApuracao.Eleito.class, votacao.apurar());

            assertEquals(MENSAGEM, assertThrows(IllegalStateException.class, votacao::criarSegundoTurno).getMessage());
        }

        @Test
        @DisplayName("apurada sem vencedor é recusado")
        void apuradaSemVencedorLancaExcecao() {
            Votacao votacao = eleicao().regra(new MaioriaAbsoluta(false)).build();
            votacao.abrir();
            votar(votacao, "Chapa A", "Chapa B", "Chapa C");
            votacao.encerrar();
            votacao.apurar();

            assertThrows(IllegalStateException.class, votacao::criarSegundoTurno);
        }

        @Test
        @DisplayName("operação recusada não gera evento na auditoria")
        void recusaNaoGeraEvento() {
            Votacao votacao = eleicao().build();

            assertThrows(IllegalStateException.class, () -> votacao.criarSegundoTurno("organizador"));

            assertEquals(1, votacao.auditoria().size());
        }

        @Test
        @DisplayName("só pode existir um 2º turno: a segunda chamada é recusada")
        void segundaChamadaLancaExcecao() {
            Votacao primeiroTurno = apuradaComSegundoTurno(eleicao());
            primeiroTurno.criarSegundoTurno();

            assertThrows(IllegalStateException.class, primeiroTurno::criarSegundoTurno);
            assertEquals(1, primeiroTurno.auditoria().stream()
                    .filter(evento -> evento.tipo() == TipoEvento.SEGUNDO_TURNO_CRIADO).count());
        }

        @Test
        @DisplayName("autor vazio é entrada inválida mesmo antes da apuração (IllegalArgumentException)")
        void autorVazioAntesDaApuracaoLancaIllegalArgument() {
            Votacao votacao = eleicao().build();

            assertThrows(IllegalArgumentException.class, () -> votacao.criarSegundoTurno(null));
        }

        @Test
        @DisplayName("autor vazio é recusado sem criar o 2º turno")
        void autorVazioLancaExcecao() {
            Votacao primeiroTurno = apuradaComSegundoTurno(eleicao());

            assertThrows(IllegalArgumentException.class, () -> primeiroTurno.criarSegundoTurno(" "));
            assertEquals("Chapa B", primeiroTurno.criarSegundoTurno().opcoes().get(0));
        }
    }

    @Nested
    @DisplayName("Configuração da nova votação")
    final class Configuracao {

        private final Votacao primeiroTurno = apuradaComSegundoTurno(eleicao().sigilo(Sigilo.IDENTIFICADO));
        private final Votacao segundoTurno = primeiroTurno.criarSegundoTurno("organizador");

        @Test
        @DisplayName("pergunta ganha o sufixo \" (2º turno)\" e a descrição se mantém")
        void perguntaEDescricao() {
            assertEquals("Eleição para presidente do clube (2º turno)", segundoTurno.pergunta());
            assertEquals("Mandato 2027-2029", segundoTurno.descricao());
        }

        @Test
        @DisplayName("opções são os dois colocados, na ordem do resultado (empate no 2º lugar: ordem de cadastro)")
        void opcoesSaoOsDoisColocados() {
            assertEquals(List.of("Chapa B", "Chapa A"), segundoTurno.opcoes());
        }

        @Test
        @DisplayName("mesmo eleitorado, mesmo sigilo e mesmo relógio")
        void mantemEleitoradoSigiloERelogio() {
            assertEquals(primeiroTurno.eleitorado().listar(), segundoTurno.eleitorado().listar());
            assertEquals(Sigilo.IDENTIFICADO, segundoTurno.sigilo());
            assertEquals(CRIACAO, segundoTurno.criadaEm());
        }

        @Test
        @DisplayName("regra é maioria simples, sem período, em rascunho e com urna vazia")
        void regraPeriodoEstadoEUrna() {
            assertInstanceOf(MaioriaSimples.class, segundoTurno.regra());
            assertEquals(Optional.empty(), segundoTurno.periodo());
            assertEquals("rascunho", segundoTurno.status());
            assertEquals(0, segundoTurno.totalVotantes());
            assertFalse(segundoTurno.jaVotou("e1"));
        }

        @Test
        @DisplayName("rastreabilidade: turno 1 e 2, e o 2º aponta para o id do 1º")
        void rastreabilidade() {
            assertEquals(1, primeiroTurno.turno());
            assertEquals(Optional.empty(), primeiroTurno.turnoAnteriorId());
            assertEquals(2, segundoTurno.turno());
            assertEquals(Optional.of(primeiroTurno.id()), segundoTurno.turnoAnteriorId());
            assertNotEquals(primeiroTurno.id(), segundoTurno.id());
        }

        @Test
        @DisplayName("auditoria: SEGUNDO_TURNO_CRIADO na original, com o id da nova; CRIADA na nova")
        void auditoriaDosDoisTurnos() {
            EventoAuditoria ultimoDaOriginal = primeiroTurno.auditoria().get(primeiroTurno.auditoria().size() - 1);
            assertEquals(new EventoAuditoria(CRIACAO, "organizador", TipoEvento.SEGUNDO_TURNO_CRIADO,
                    segundoTurno.id()), ultimoDaOriginal);

            assertEquals(List.of(new EventoAuditoria(CRIACAO, "organizador", TipoEvento.CRIADA,
                    "2º turno da votacao " + primeiroTurno.id())), segundoTurno.auditoria());
        }
    }

    @Test
    @DisplayName("ponta a ponta: 3 chapas, ninguém passa de 50%, 2º turno com os mesmos eleitores elege")
    void pontaAPonta() {
        Votacao primeiroTurno = apuradaComSegundoTurno(eleicao());
        ResultadoApuracao.SegundoTurno resultado =
                assertInstanceOf(ResultadoApuracao.SegundoTurno.class, primeiroTurno.apurar());
        assertEquals("Chapa B", resultado.primeiroColocado());
        assertEquals("Chapa A", resultado.segundoColocado());

        Votacao segundoTurno = primeiroTurno.criarSegundoTurno("organizador");
        segundoTurno.abrir("organizador");
        assertTrue(primeiroTurno.jaVotou("e1"));
        assertFalse(segundoTurno.jaVotou("e1"));
        votar(segundoTurno, "Chapa A", "Chapa A", "Chapa A", "Chapa B", "Chapa A",
                "Chapa A", "Chapa B", "Chapa A", "Chapa B", "Chapa B");
        assertThrows(IllegalArgumentException.class, () -> segundoTurno.registrarVoto("e99", "Chapa A"));
        segundoTurno.encerrar("organizador");

        ResultadoApuracao.Eleito eleito =
                assertInstanceOf(ResultadoApuracao.Eleito.class, segundoTurno.apurar("organizador"));
        assertEquals("Chapa A", eleito.candidato());
        assertEquals(List.of(TipoEvento.CRIADA, TipoEvento.ABERTA, TipoEvento.ENCERRADA, TipoEvento.APURADA),
                segundoTurno.auditoria().stream().map(EventoAuditoria::tipo).toList());
        assertEquals("Eleito: Chapa A", segundoTurno.auditoria().get(3).detalhe());
    }
}

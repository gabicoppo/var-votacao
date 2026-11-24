package br.unicamp.var.relatorio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import br.unicamp.var.apuracao.MaioriaAbsoluta;
import br.unicamp.var.apuracao.MaioriaSimples;
import br.unicamp.var.eleitorado.Eleitor;
import br.unicamp.var.eleitorado.Eleitorado;
import br.unicamp.var.processo.PeriodoVotacao;
import br.unicamp.var.processo.Votacao;
import br.unicamp.var.urna.Sigilo;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Relatório de apuração (história #31): texto completo com relógio fixo, sem o voto de ninguém
 * (RNF-01) e com a trilha de auditoria (RNF-07). Horários em UTC no teste; no relatório, em
 * America/Sao_Paulo (UTC-3).
 */
final class RelatorioApuracaoTest {

    private static final Instant CRIACAO = Instant.parse("2027-02-20T12:00:00Z");

    /** Relógio de teste que o teste ajusta. */
    private static final class RelogioAjustavel extends Clock {
        private Instant agora = CRIACAO;

        void ajustarPara(String instante) {
            agora = Instant.parse(instante);
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
        public Clock withZone(ZoneId zona) {
            return this;
        }
    }

    private final RelogioAjustavel relogio = new RelogioAjustavel();

    private static Eleitorado eleitorado() {
        return Eleitorado.de(IntStream.rangeClosed(1, 10)
                .mapToObj(i -> new Eleitor("e" + i, "Eleitor " + i))
                .toList());
    }

    private Votacao.Builder eleicao() {
        return Votacao.nova()
                .pergunta("Eleição para presidente do clube")
                .opcoes(List.of("Chapa A", "Chapa B", "Chapa C"))
                .regra(new MaioriaSimples())
                .eleitorado(eleitorado())
                .relogio(relogio);
    }

    private static void votar(Votacao votacao, String... opcoes) {
        for (int i = 0; i < opcoes.length; i++) {
            votacao.registrarVoto("e" + (i + 1), opcoes[i]);
        }
    }

    @Nested
    @DisplayName("Texto do relatório")
    final class Texto {

        @Test
        @DisplayName("votação completa: cabeçalho, período, quadro do resultado e auditoria em America/Sao_Paulo")
        void relatorioCompleto() {
            PeriodoVotacao periodo = new PeriodoVotacao(Instant.parse("2027-02-20T13:00:00Z"),
                    Instant.parse("2027-02-20T15:00:00Z"));
            Votacao votacao = eleicao().descricao("Mandato 2027-2029").sigilo(Sigilo.IDENTIFICADO)
                    .periodo(periodo).criadaPor("organizador").build();
            relogio.ajustarPara("2027-02-20T12:05:00Z");
            votacao.alterarRegra(new MaioriaAbsoluta(true), "organizador");
            relogio.ajustarPara("2027-02-20T13:00:00Z");
            votar(votacao, "Chapa A", "Chapa A", "Chapa A", "Chapa B", "Chapa C");
            relogio.ajustarPara("2027-02-20T14:30:00Z");
            votacao.encerrar("organizador");
            relogio.ajustarPara("2027-02-20T14:45:00Z");
            votacao.apurar("comissao eleitoral");

            String esperado = """
                    Relatório de apuração
                    Votação: Eleição para presidente do clube
                    Descrição: Mandato 2027-2029
                    Turno: 1º
                    Regra: Maioria absoluta: mais da metade dos votos válidos, \
                    com 2º turno entre os dois mais votados.
                    Sigilo: identificado (voto nominal)
                    Período: 20/02/2027 10:00 a 20/02/2027 12:00

                    Resultado da votação
                    Chapa A: 3 votos (60,0%)
                    Chapa B: 1 votos (20,0%)
                    Chapa C: 1 votos (20,0%)
                    Votantes: 5 de 10 aptos (participação de 50,0%)
                    Situação: Eleito: Chapa A

                    Auditoria
                    20/02/2027 09:00 - organizador - votação criada
                    20/02/2027 09:05 - organizador - regra alterada (Maioria absoluta: mais da metade dos votos \
                    válidos, com 2º turno entre os dois mais votados.)
                    20/02/2027 10:00 - sistema - votação aberta (inicio do periodo)
                    20/02/2027 11:30 - organizador - votação encerrada
                    20/02/2027 11:45 - comissao eleitoral - votação apurada (Eleito: Chapa A)""";
            assertEquals(esperado, RelatorioApuracao.de(votacao).formatar());
        }

        @Test
        @DisplayName("2º turno, sem descrição nem período: mostra o turno anterior e omite as linhas vazias")
        void relatorioDoSegundoTurno() {
            Votacao primeiroTurno = eleicao().regra(new MaioriaAbsoluta(true)).build();
            primeiroTurno.abrir();
            votar(primeiroTurno, "Chapa A", "Chapa B", "Chapa C", "Chapa A");
            primeiroTurno.encerrar();
            primeiroTurno.apurar();
            Votacao segundoTurno = primeiroTurno.criarSegundoTurno();
            segundoTurno.abrir();
            votar(segundoTurno, "Chapa B", "Chapa B", "Chapa A");
            segundoTurno.encerrar();
            segundoTurno.apurar();

            String esperado = """
                    Relatório de apuração
                    Votação: Eleição para presidente do clube (2º turno)
                    Turno: 2º (1º turno: %1$s)
                    Regra: Maioria simples: vence a opção mais votada; empate fica sem vencedor.
                    Sigilo: secreto

                    Resultado da votação
                    Chapa B: 2 votos (66,7%%)
                    Chapa A: 1 votos (33,3%%)
                    Votantes: 3 de 10 aptos (participação de 30,0%%)
                    Situação: Eleito: Chapa B

                    Auditoria
                    20/02/2027 09:00 - sistema - votação criada (2º turno da votacao %1$s)
                    20/02/2027 09:00 - sistema - votação aberta
                    20/02/2027 09:00 - sistema - votação encerrada
                    20/02/2027 09:00 - sistema - votação apurada (Eleito: Chapa B)""".formatted(primeiroTurno.id());
            assertEquals(esperado, RelatorioApuracao.de(segundoTurno).formatar());
        }

        @Test
        @DisplayName("é um retrato: criar o 2º turno depois não muda um relatório já montado")
        void relatorioEhImutavel() {
            Votacao votacao = eleicao().regra(new MaioriaAbsoluta(true)).build();
            votacao.abrir();
            votar(votacao, "Chapa A", "Chapa B", "Chapa C");
            votacao.encerrar();
            votacao.apurar();
            RelatorioApuracao relatorio = RelatorioApuracao.de(votacao);
            String antes = relatorio.formatar();

            votacao.criarSegundoTurno();

            assertEquals(antes, relatorio.formatar());
            assertFalse(antes.contains("2º turno criado"));
        }
    }

    @Nested
    @DisplayName("Sigilo (RNF-01)")
    final class SigiloDoVoto {

        @Test
        @DisplayName("nem em votação identificada o relatório mostra eleitores ou seus votos")
        void naoMostraVotoDeNinguem() {
            Votacao votacao = eleicao().sigilo(Sigilo.IDENTIFICADO).build();
            votacao.abrir();
            votar(votacao, "Chapa A", "Chapa B");
            votacao.encerrar();
            votacao.apurar();

            String texto = RelatorioApuracao.de(votacao).formatar();

            assertFalse(texto.contains("e1"));
            assertFalse(texto.contains("Eleitor 1"));
        }
    }

    @Nested
    @DisplayName("Validações")
    final class Validacoes {

        @Test
        @DisplayName("votação nula lança IllegalArgumentException")
        void votacaoNula() {
            assertThrows(IllegalArgumentException.class, () -> RelatorioApuracao.de(null));
        }

        @Test
        @DisplayName("votação não apurada lança IllegalStateException")
        void votacaoNaoApurada() {
            Votacao votacao = eleicao().build();
            assertThrows(IllegalStateException.class, () -> RelatorioApuracao.de(votacao));
            votacao.abrir();
            assertThrows(IllegalStateException.class, () -> RelatorioApuracao.de(votacao));
            votacao.encerrar();
            IllegalStateException erro = assertThrows(IllegalStateException.class, () -> RelatorioApuracao.de(votacao));
            assertEquals("relatorio so existe para votacao apurada", erro.getMessage());
        }
    }
}

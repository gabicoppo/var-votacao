package br.unicamp.var.web.comum;

import static org.junit.jupiter.api.Assertions.assertEquals;

import br.unicamp.var.apuracao.MaioriaSimples;
import br.unicamp.var.eleitorado.Eleitor;
import br.unicamp.var.eleitorado.Eleitorado;
import br.unicamp.var.processo.PeriodoVotacao;
import br.unicamp.var.processo.Votacao;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Atualizador agendado dos períodos (história #20).
 */
class AtualizadorPeriodosTest {

    private static final Instant AGORA = Instant.parse("2026-11-24T12:00:00Z");

    private static Votacao votacao(PeriodoVotacao periodo) {
        Votacao.Builder builder = Votacao.nova()
                .pergunta("Pergunta")
                .opcoes(List.of("Sim", "Não"))
                .regra(new MaioriaSimples())
                .eleitorado(Eleitorado.de(List.of(new Eleitor("e01", "Ana Ribeiro"))))
                .relogio(Clock.fixed(AGORA, ZoneOffset.UTC));
        return periodo == null ? builder.build() : builder.periodo(periodo).build();
    }

    @Test
    @DisplayName("abre as votações cujo período começou, encerra as que terminaram e ignora as sem período")
    void aplicaORelogioEmTodas() {
        RepositorioVotacoes repositorio = new RepositorioVotacoesEmMemoria();
        Votacao comecou = repositorio.salvar(
                votacao(new PeriodoVotacao(AGORA.minusSeconds(60), AGORA.plusSeconds(60))));
        Votacao terminou = repositorio.salvar(votacao(new PeriodoVotacao(AGORA.minusSeconds(120), AGORA)));
        Votacao futura = repositorio.salvar(
                votacao(new PeriodoVotacao(AGORA.plusSeconds(60), AGORA.plusSeconds(120))));
        Votacao semPeriodo = repositorio.salvar(votacao(null));

        new AtualizadorPeriodos(repositorio).atualizar();

        assertEquals("aberta", comecou.status());
        assertEquals("encerrada", terminou.status());
        assertEquals("rascunho", futura.status());
        assertEquals("rascunho", semPeriodo.status());
    }
}

package br.unicamp.var.e2e;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import br.unicamp.var.processo.Votacao;
import br.unicamp.var.web.comum.RepositorioVotacoes;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 2º turno (história #21): da apuração com {@code SegundoTurno} à gestão da nova votação.
 *
 * <p>A tela de gestão (área do organizador) ainda não lista as opções; por isso os finalistas são
 * conferidos no repositório, pela votação aberta na tela.</p>
 */
@DisplayName("2º turno")
class SegundoTurnoE2E extends E2EBase {

    @Autowired
    private RepositorioVotacoes votacoes;

    @Test
    @DisplayName("criar o 2º turno do Conselho Fiscal leva à gestão da nova votação com os dois finalistas")
    void criarSegundoTurno() {
        Page org = organizador();
        abrirCartao(org, "Eleição do Conselho Fiscal");
        capturar(org, "01-conselho-apurado");

        acaoConfirmada(org, "Criar 2º turno");

        assertThat(org.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setLevel(1)))
                .containsText("Eleição do Conselho Fiscal (2º turno)");
        assertThat(org.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Ver o 1º turno"))).isVisible();
        assertThat(org.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Abrir votação"))).isVisible();
        capturar(org, "02-gestao-segundo-turno");

        String id = org.url().replaceAll(".*/organizador/votacoes/([0-9a-f-]{36}).*", "$1");
        Votacao segundoTurno = votacoes.buscar(id).orElseThrow();
        synchronized (segundoTurno) {
            assertEquals(List.of("Chapa Transparência", "Chapa Contas Abertas"), segundoTurno.opcoes());
            assertEquals(2, segundoTurno.turno());
        }
    }
}

package br.unicamp.var.e2e;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Premiação Top 3 (histórias #23 e #30): cédula do eleitor, encerramento, apuração e ranking.
 */
@DisplayName("Premiação Craque do Campeonato")
class PremiacaoE2E extends E2EBase {

    private static final String CRAQUE = "Craque do Campeonato 2026";

    @Test
    @DisplayName("e01 (Jornalistas) vota no Top 3, o organizador encerra e apura, e o ranking mostra pódio e grupos")
    void votarEApurar() {
        Page eleitor = novaPagina(CELULAR);
        entrar(eleitor, "e01", SENHA_ELEITOR);
        abrirCartao(eleitor, CRAQUE);
        assertThat(eleitor.getByText("Você vota como Jornalistas")).isVisible();
        // Cédula por toque: os jogadores são tocados na ordem 1º, 2º e 3º (sem JS, são três selects).
        for (String jogador : new String[] {"Davi Prado", "Rafa Lima", "Nico Sales"}) {
            eleitor.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(jogador)).click();
        }
        capturar(eleitor, "01-cedula-top3");
        eleitor.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Revisar cédula")).click();
        assertThat(eleitor.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setName("Confira sua cédula")))
                .isVisible();
        eleitor.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Confirmar cédula")).click();
        assertThat(eleitor.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setName("Cédula registrada")))
                .isVisible();
        capturar(eleitor, "02-cedula-registrada");

        Page org = organizador();
        abrirCartao(org, CRAQUE);
        acaoConfirmada(org, "Encerrar premiação");
        acaoConfirmada(org, "Apurar e publicar ranking");
        if (!org.url().contains("/ranking")) {
            org.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Ver ranking")).first().click();
        }
        assertThat(org.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setName("Pódio"))).isVisible();
        assertThat(org.getByRole(AriaRole.COLUMNHEADER, new Page.GetByRoleOptions().setName("Jornalistas")))
                .isVisible();
        assertThat(org.getByRole(AriaRole.COLUMNHEADER, new Page.GetByRoleOptions().setName("Torcedores")))
                .isVisible();
        assertThat(org.getByRole(AriaRole.ROWHEADER, new Page.GetByRoleOptions().setName("Rafa Lima"))).isVisible();
        capturar(org, "03-ranking");
    }
}

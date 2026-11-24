package br.unicamp.var.e2e;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Diálogo de confirmação do {@code var.js} para {@code form[data-confirmar]}.
 */
@DisplayName("Diálogo de confirmação")
class DialogoConfirmacaoE2E extends E2EBase {

    @Test
    @DisplayName("abre com o foco em Cancelar, Esc cancela sem enviar e devolve o foco, confirmar envia")
    void abrirCancelarConfirmar() {
        Page pagina = novaPagina(CELULAR);
        pagina.navigate("/estilo#dialogo");
        Locator encerrar = pagina.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions()
                .setName("Encerrar votação").setExact(true));
        Locator dialogo = pagina.getByRole(AriaRole.DIALOG);

        encerrar.click();
        assertThat(dialogo).isVisible();
        assertThat(dialogo.getByRole(AriaRole.HEADING)).hasText("Encerrar votação");
        assertThat(dialogo.getByText("Ninguém mais poderá votar.")).isVisible();
        assertThat(dialogo.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Cancelar"))).isFocused();
        capturar(pagina, "01-dialogo-aberto");

        pagina.keyboard().press("Escape");
        assertThat(dialogo).isHidden();
        assertThat(encerrar).isFocused();
        assertTrue(pagina.url().endsWith("/estilo#dialogo"), "Esc não pode enviar o form: " + pagina.url());

        encerrar.click();
        dialogo.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Encerrar")).click();
        pagina.waitForURL("**/estilo?");
        assertThat(dialogo).isHidden();
    }

    @Test
    @DisplayName("na gestão, cancelar o diálogo não abre a votação")
    void cancelarNaGestao() {
        Page org = organizador();
        abrirCartao(org, "Enquete: o gol da final foi golaço?");
        org.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Abrir votação")).click();
        org.getByRole(AriaRole.DIALOG).getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Cancelar"))
                .click();
        assertThat(org.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Abrir votação"))).isVisible();
        assertThat(org.getByText("Rascunho").first()).isVisible();
    }
}

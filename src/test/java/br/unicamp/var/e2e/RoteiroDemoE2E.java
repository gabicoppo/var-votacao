package br.unicamp.var.e2e;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Roteiro da apresentação, de ponta a ponta: abrir, votar, verificar, encerrar, apurar, ver o
 * resultado e o relatório (histórias #19 a #31).
 */
@DisplayName("Roteiro da demo")
class RoteiroDemoE2E extends E2EBase {

    private static final String ENQUETE = "Enquete: o gol da final foi golaço?";

    @Test
    @DisplayName("organizador abre a enquete, e01 vota e verifica, organizador apura, resultado e relatório abrem")
    void roteiroCompleto() {
        Page org = organizador();
        abrirCartao(org, ENQUETE);
        capturar(org, "01-gestao-rascunho");
        acaoConfirmada(org, "Abrir votação");
        assertThat(org.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Encerrar votação")))
                .isVisible();

        Page eleitor = novaPagina(CELULAR);
        entrar(eleitor, "e01", SENHA_ELEITOR);
        capturar(eleitor, "02-painel-eleitor");
        abrirCartao(eleitor, ENQUETE);
        eleitor.getByLabel("Sim").check(new Locator.CheckOptions().setForce(true));
        capturar(eleitor, "03-cedula");
        eleitor.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Revisar voto")).click();
        assertThat(eleitor.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setName("Confira seu voto")))
                .isVisible();
        eleitor.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Confirmar voto")).click();
        assertThat(eleitor.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setName("Voto registrado")))
                .isVisible();
        assertThat(eleitor.getByText("Sim", new Page.GetByTextOptions().setExact(true))).hasCount(0);
        capturar(eleitor, "04-comprovante");

        eleitor.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Verificar depois")).click();
        assertThat(eleitor.getByText("Este comprovante foi emitido por esta votação")).isVisible();
        capturar(eleitor, "05-verificar");

        org.reload();
        acaoConfirmada(org, "Encerrar votação");
        acaoConfirmada(org, "Apurar");
        org.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Ver resultado")).click();
        pularMomentoVar(org);
        assertThat(org.getByRole(AriaRole.STATUS).filter(new Locator.FilterOptions().setHasText("Sim")))
                .isVisible();
        assertThat(org.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setName("Votos por opção")))
                .isVisible();
        assertThat(org.getByText("100%").first()).isVisible();
        capturar(org, "06-resultado");

        org.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Relatório")).click();
        assertThat(org.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setName("Relatório de apuração")))
                .isVisible();
        capturar(org, "07-relatorio");
    }
}

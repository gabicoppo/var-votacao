package br.unicamp.var.e2e;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.unicamp.var.web.comum.RepositorioPremiacoes;
import br.unicamp.var.web.comum.RepositorioVotacoes;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * RNF-04: uso no celular (360 × 740) com no máximo 3 telas do link da cédula à confirmação e sem
 * rolagem horizontal nas páginas principais.
 */
@DisplayName("RNF-04: usabilidade no celular")
class UsabilidadeCelularE2E extends E2EBase {

    private static final int MAXIMO_TELAS = 3;

    @Autowired
    private RepositorioVotacoes votacoes;

    @Autowired
    private RepositorioPremiacoes premiacoes;

    @Test
    @DisplayName("do link da cédula à confirmação do voto são no máximo 3 telas, todas sem rolagem horizontal")
    void tresTelasAteAConfirmacao() {
        Page pagina = novaPagina(CELULAR);
        entrar(pagina, "e02", SENHA_ELEITOR);
        AtomicInteger telas = new AtomicInteger();
        List<String> comRolagem = new ArrayList<>();
        pagina.onFrameNavigated(frame -> {
            if (frame == pagina.mainFrame()) {
                telas.incrementAndGet();
            }
        });

        abrirCartao(pagina, "Eleição presidencial");
        verificarRolagem(pagina, "cedula", comRolagem);
        pagina.getByLabel("Chapa Renovação").check(new Locator.CheckOptions().setForce(true));
        pagina.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Revisar voto")).click();
        pagina.waitForURL("**/revisar");
        verificarRolagem(pagina, "revisar", comRolagem);
        pagina.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Confirmar voto")).click();
        assertThat(pagina.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setName("Voto registrado")))
                .isVisible();
        verificarRolagem(pagina, "comprovante", comRolagem);
        capturar(pagina, "comprovante");

        assertTrue(telas.get() <= MAXIMO_TELAS, "telas até a confirmação: " + telas.get());
        assertTrue(comRolagem.isEmpty(), "com rolagem horizontal: " + comRolagem);
    }

    @Test
    @DisplayName("nenhuma página principal tem rolagem horizontal a 360 px")
    void semRolagemHorizontal() {
        Map<String, Page> paginas = new HashMap<>();
        List<String> comRolagem = new ArrayList<>();
        for (PaginasPrincipais.Visita visita : PaginasPrincipais.listar(votacoes, premiacoes)) {
            Page pagina = paginas.computeIfAbsent(visita.usuario(), this::paginaDe);
            pagina.navigate(visita.url());
            pularMomentoVar(pagina);
            if (temRolagemHorizontal(pagina)) {
                comRolagem.add(visita.nome());
            }
            pagina.screenshot(new Page.ScreenshotOptions().setFullPage(true)
                    .setPath(java.nio.file.Path.of("target", "e2e-screenshots", getClass().getSimpleName(),
                            "360-" + visita.nome() + ".png")));
        }
        assertTrue(comRolagem.isEmpty(), "páginas com rolagem horizontal: " + comRolagem);
    }

    private Page paginaDe(String usuario) {
        Page pagina = novaPagina(CELULAR);
        if (PaginasPrincipais.ORGANIZADOR.equals(usuario)) {
            entrar(pagina, usuario, "var2026");
        } else if (PaginasPrincipais.ELEITOR.equals(usuario)) {
            entrar(pagina, usuario, SENHA_ELEITOR);
        }
        return pagina;
    }

    private static void verificarRolagem(Page pagina, String nome, List<String> comRolagem) {
        assertFalse(pagina.url().contains("/login"), "sessão perdida em " + nome);
        if (temRolagemHorizontal(pagina)) {
            comRolagem.add(nome);
        }
    }
}

package br.unicamp.var.e2e;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.ReducedMotion;
import com.microsoft.playwright.options.ViewportSize;
import com.microsoft.playwright.options.WaitForSelectorState;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;

/**
 * Base dos cenários E2E: sobe a aplicação com os dados de demonstração numa porta aleatória e abre um
 * Chromium pelo Playwright. Cada classe de cenário ganha um contexto Spring novo
 * ({@link DirtiesContext}), para que um cenário não altere os dados de outro.
 *
 * <p>Os seletores são por papel e texto ({@code getByRole}, {@code getByLabel}, {@code getByText}):
 * os testes descrevem o que o usuário vê, não o CSS.</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "var.demo.ativo=true")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class E2EBase {

    /** Celular de referência do RNF-04. */
    protected static final int CELULAR = 360;
    /** Desktop de referência. */
    protected static final int DESKTOP = 1280;
    /** Senha das contas de eleitor da demonstração. */
    protected static final String SENHA_ELEITOR = "1234";

    private static final Path PASTA_CAPTURAS = Path.of("target", "e2e-screenshots");
    private static final Path CHROMIUM_PADRAO = Path.of("/opt/pw-browsers/chromium");
    private static final int ALTURA_CELULAR = 740;
    private static final int ALTURA_DESKTOP = 800;

    @LocalServerPort
    private int porta;

    private Playwright playwright;
    private Browser navegador;

    @BeforeAll
    void abrirNavegador() {
        Optional<Path> executavel = executavel();
        Playwright.CreateOptions opcoes = new Playwright.CreateOptions();
        if (executavel.isPresent()) {
            opcoes.setEnv(Map.of("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD", "1"));
        }
        playwright = Playwright.create(opcoes);
        BrowserType.LaunchOptions lancamento = new BrowserType.LaunchOptions().setArgs(List.of("--no-sandbox"));
        executavel.ifPresent(lancamento::setExecutablePath);
        navegador = playwright.chromium().launch(lancamento);
    }

    @AfterAll
    void fecharNavegador() {
        if (playwright != null) {
            playwright.close();
        }
    }

    /**
     * Executável do Chromium: propriedade {@code e2e.chromium}, variável {@code E2E_CHROMIUM},
     * {@code /opt/pw-browsers/chromium} ou vazio (instalação padrão do Playwright).
     *
     * @return o caminho do executável, se houver um configurado ou encontrado
     */
    static Optional<Path> executavel() {
        String configurado = System.getProperty("e2e.chromium", System.getenv("E2E_CHROMIUM"));
        if (configurado != null && !configurado.isBlank()) {
            return Optional.of(Path.of(configurado));
        }
        if (Files.isRegularFile(CHROMIUM_PADRAO) && Files.isExecutable(CHROMIUM_PADRAO)) {
            return Optional.of(CHROMIUM_PADRAO);
        }
        if (Files.isDirectory(CHROMIUM_PADRAO)) {
            try (Stream<Path> arquivos = Files.walk(CHROMIUM_PADRAO, 4)) {
                return arquivos.filter(p -> p.getFileName().toString().equals("chrome") && Files.isExecutable(p))
                        .findFirst();
            } catch (IOException erro) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    /**
     * Página nova num contexto próprio (cookies separados), com a largura pedida.
     *
     * @param largura largura da janela, em px
     * @return a página
     */
    protected Page novaPagina(int largura) {
        BrowserContext contexto = navegador.newContext(new Browser.NewContextOptions()
                .setBaseURL("http://localhost:" + porta)
                .setViewportSize(largura, largura == CELULAR ? ALTURA_CELULAR : ALTURA_DESKTOP)
                // sem transições: screenshots estáveis (o CSS respeita prefers-reduced-motion)
                .setReducedMotion(ReducedMotion.REDUCE));
        return contexto.newPage();
    }

    /**
     * Entra pela página de login, como um usuário faria.
     *
     * @param pagina  página do navegador
     * @param usuario login
     * @param senha   senha
     */
    protected void entrar(Page pagina, String usuario, String senha) {
        pagina.navigate("/login");
        pagina.getByLabel("Usuário").fill(usuario);
        pagina.getByLabel("Senha ou código de acesso").fill(senha);
        pagina.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Entrar")).click();
        pagina.waitForURL(url -> !url.contains("/login"));
    }

    /**
     * Entra como organizador da demonstração.
     *
     * @return a página, já no painel do organizador
     */
    protected Page organizador() {
        Page pagina = novaPagina(DESKTOP);
        entrar(pagina, "organizador", "var2026");
        return pagina;
    }

    /**
     * Clica no card (link) que contém o texto.
     *
     * @param pagina página
     * @param texto  texto do card (pergunta ou título)
     */
    protected static void abrirCartao(Page pagina, String texto) {
        pagina.getByRole(AriaRole.LINK).filter(new Locator.FilterOptions().setHasText(texto)).first().click();
        pagina.waitForLoadState();
    }

    /**
     * Clica num botão de ação que pede confirmação (form[data-confirmar]) e confirma no diálogo.
     *
     * @param pagina página
     * @param botao  nome do botão da ação
     */
    protected static void acaoConfirmada(Page pagina, String botao) {
        pagina.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(botao).setExact(true)).click();
        Locator dialogo = pagina.getByRole(AriaRole.DIALOG);
        // O rótulo do botão de confirmar muda por área (data-confirmar-botao); o outro é sempre "Cancelar".
        dialogo.getByRole(AriaRole.BUTTON).filter(new Locator.FilterOptions().setHasNotText("Cancelar")).click();
        pagina.waitForLoadState();
    }

    /**
     * Pula o "Momento VAR" (overlay animado da tela de resultado), se ele estiver na tela.
     *
     * @param pagina página
     */
    protected static void pularMomentoVar(Page pagina) {
        Locator overlay = pagina.getByText("Toque para pular");
        if (overlay.isVisible()) {
            pagina.keyboard().press("Escape");
            overlay.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.HIDDEN));
        }
    }

    /**
     * Salva o screenshot do passo a 360 px e a 1280 px em {@code target/e2e-screenshots/<Cenario>/}.
     *
     * @param pagina página
     * @param passo  nome do passo (vira o nome do arquivo)
     */
    protected void capturar(Page pagina, String passo) {
        ViewportSize original = pagina.viewportSize();
        Path pasta = PASTA_CAPTURAS.resolve(getClass().getSimpleName());
        for (int largura : new int[] {CELULAR, DESKTOP}) {
            pagina.setViewportSize(largura, largura == CELULAR ? ALTURA_CELULAR : ALTURA_DESKTOP);
            pagina.screenshot(new Page.ScreenshotOptions().setFullPage(true)
                    .setPath(pasta.resolve(passo + "-" + largura + ".png")));
        }
        pagina.setViewportSize(original.width, original.height);
    }

    /**
     * Diz se a página tem rolagem horizontal no documento (RNF-04).
     *
     * @param pagina página
     * @return {@code true} se {@code scrollWidth > innerWidth}
     */
    protected static boolean temRolagemHorizontal(Page pagina) {
        return (Boolean) pagina.evaluate("document.documentElement.scrollWidth > window.innerWidth");
    }
}

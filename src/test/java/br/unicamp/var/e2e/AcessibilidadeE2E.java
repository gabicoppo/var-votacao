package br.unicamp.var.e2e;

import static org.junit.jupiter.api.Assertions.assertTrue;

import br.unicamp.var.web.comum.RepositorioPremiacoes;
import br.unicamp.var.web.comum.RepositorioVotacoes;
import com.microsoft.playwright.Page;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Acessibilidade básica nas páginas principais: campos com rótulo, imagens com {@code alt} e um único
 * {@code h1}. Campos fora da árvore de acessibilidade ({@code aria-hidden="true"}, como o texto de apoio
 * do "Copiar convite") não precisam de rótulo.
 */
@DisplayName("Acessibilidade básica")
class AcessibilidadeE2E extends E2EBase {

    /** Problemas de acessibilidade encontrados na página, em texto. */
    private static final String VERIFICACAO = """
            () => {
              const problemas = [];
              const campos = 'input:not([type=hidden]), select, textarea';
              document.querySelectorAll(campos).forEach(c => {
                if (c.closest('[aria-hidden="true"]')) { return; }
                const rotulo = (c.labels && c.labels.length > 0) || c.getAttribute('aria-label')
                    || c.getAttribute('aria-labelledby');
                if (!rotulo) { problemas.push('campo sem rótulo: ' + (c.name || c.id || c.type)); }
              });
              document.querySelectorAll('img').forEach(i => {
                if (!i.hasAttribute('alt')) { problemas.push('imagem sem alt: ' + i.getAttribute('src')); }
              });
              const h1 = document.querySelectorAll('h1').length;
              if (h1 !== 1) { problemas.push(h1 + ' elementos h1'); }
              return problemas;
            }""";

    @Autowired
    private RepositorioVotacoes votacoes;

    @Autowired
    private RepositorioPremiacoes premiacoes;

    @Test
    @DisplayName("todo campo tem rótulo, toda imagem tem alt e cada página tem um único h1")
    void paginasPrincipais() {
        Map<String, Page> paginas = new HashMap<>();
        List<String> problemas = new ArrayList<>();
        for (PaginasPrincipais.Visita visita : PaginasPrincipais.listar(votacoes, premiacoes)) {
            Page pagina = paginas.computeIfAbsent(visita.usuario(), this::paginaDe);
            pagina.navigate(visita.url());
            for (Object problema : (List<?>) pagina.evaluate(VERIFICACAO)) {
                problemas.add(visita.nome() + ": " + problema);
            }
        }
        assertTrue(problemas.isEmpty(), "problemas de acessibilidade: " + problemas);
    }

    private Page paginaDe(String usuario) {
        Page pagina = novaPagina(DESKTOP);
        if (PaginasPrincipais.ORGANIZADOR.equals(usuario)) {
            entrar(pagina, usuario, "var2026");
        } else if (PaginasPrincipais.ELEITOR.equals(usuario)) {
            entrar(pagina, usuario, SENHA_ELEITOR);
        }
        return pagina;
    }
}

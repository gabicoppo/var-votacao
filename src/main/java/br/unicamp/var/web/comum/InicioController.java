package br.unicamp.var.web.comum;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Página inicial, login e guia de estilo.
 *
 * <p>{@code GET /} decide pelo papel: anônimo vê a página pública, organizador vai para
 * {@code /organizador} e eleitor para {@code /eleitor}. O login bem-sucedido também passa por aqui.</p>
 */
@Controller
public class InicioController {

    /**
     * Página inicial ou redirecionamento pelo papel.
     *
     * @param autenticacao usuário logado, ou {@code null} se anônimo
     * @return a view {@code inicio} ou o redirecionamento para o painel do papel
     */
    @GetMapping("/")
    public String inicio(Authentication autenticacao) {
        if (tem(autenticacao, Papel.ORGANIZADOR)) {
            return "redirect:/organizador";
        }
        if (tem(autenticacao, Papel.ELEITOR)) {
            return "redirect:/eleitor";
        }
        return "inicio";
    }

    /**
     * Página de login.
     *
     * @return a view {@code login}
     */
    @GetMapping("/login")
    public String login() {
        return "login";
    }

    /**
     * Guia de estilo vivo, com todos os componentes de {@code var.css}.
     *
     * @return a view {@code estilo}
     */
    @GetMapping("/estilo")
    public String estilo() {
        return "estilo";
    }

    private static boolean tem(Authentication autenticacao, Papel papel) {
        return autenticacao != null && autenticacao.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(papel.authority()::equals);
    }
}

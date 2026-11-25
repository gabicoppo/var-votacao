package br.unicamp.var.web.comum;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Página inicial, login e guia de estilo.
 *
 * <p>{@code GET /} decide pelo papel: anônimo vê a página pública, organizador vai para
 * {@code /organizador} e eleitor para {@code /eleitor}. O login bem-sucedido também passa por aqui.</p>
 */
@Controller
public class InicioController {

    private final boolean demoAtivo;
    private final String senhaOrganizadorVisivel;

    /**
     * Cria o controller.
     *
     * @param demoAtivo               valor de {@code var.demo.ativo}; se verdadeiro, o login mostra as
     *                                contas de demonstração
     * @param mostrarSenhaOrganizador valor de {@code var.demo.mostrar-senha-organizador}; falso no perfil
     *                                {@code prod}, em que a senha do organizador é secreta
     * @param senhaOrganizador        valor de {@code var.senha-organizador}, mostrado só se permitido
     */
    public InicioController(@Value("${var.demo.ativo:false}") boolean demoAtivo,
                            @Value("${var.demo.mostrar-senha-organizador:true}") boolean mostrarSenhaOrganizador,
                            @Value("${var.senha-organizador:" + ContasUsuariosEmMemoria.SENHA_ORGANIZADOR_PADRAO + "}")
                            String senhaOrganizador) {
        this.demoAtivo = demoAtivo;
        this.senhaOrganizadorVisivel = mostrarSenhaOrganizador && senhaOrganizador != null
                && !senhaOrganizador.isBlank() ? senhaOrganizador : null;
    }

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
     * Página de login; com a demonstração ativa, mostra as contas de demonstração.
     *
     * @param modelo modelo da página
     * @return a view {@code login}
     */
    @GetMapping("/login")
    public String login(Model modelo) {
        modelo.addAttribute("demoAtivo", demoAtivo);
        modelo.addAttribute("senhaOrganizador", senhaOrganizadorVisivel);
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

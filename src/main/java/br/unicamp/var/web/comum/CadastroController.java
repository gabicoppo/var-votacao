package br.unicamp.var.web.comum;

import java.security.Principal;
import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Cadastro público de contas ({@code /cadastro}): organizador ou eleitor. Depois de criar a conta,
 * a pessoa vai para o login com o usuário já preenchido.
 */
@Controller
public class CadastroController {

    private static final String VIEW = "cadastro";

    private final ServicoCadastro servico;

    /**
     * Cria o controller.
     *
     * @param servico serviço de cadastro
     */
    public CadastroController(ServicoCadastro servico) {
        this.servico = servico;
    }

    /**
     * Formulário de cadastro. Quem já está logado volta ao início.
     *
     * @param usuario usuário logado, ou {@code null}
     * @param modelo  modelo da página
     * @return a view {@code cadastro} ou o redirecionamento ao início
     */
    @GetMapping("/cadastro")
    public String formulario(Principal usuario, Model modelo) {
        if (usuario != null) {
            return "redirect:/";
        }
        modelo.addAttribute("formulario", FormularioCadastro.vazio());
        modelo.addAttribute("erros", Map.of());
        return VIEW;
    }

    /**
     * Cria a conta ou mostra o formulário de novo com os erros (sem as senhas).
     *
     * @param formulario dados digitados
     * @param modelo     modelo da página
     * @param redirecao  atributos flash
     * @return o redirecionamento ao login ou a view {@code cadastro} com erros
     */
    @PostMapping("/cadastro")
    public String cadastrar(@ModelAttribute FormularioCadastro formulario, Model modelo,
                            RedirectAttributes redirecao) {
        Map<String, String> erros = servico.cadastrar(formulario);
        if (!erros.isEmpty()) {
            modelo.addAttribute("formulario", formulario.semSenhas());
            modelo.addAttribute("erros", erros);
            modelo.addAttribute("alerta", Alerta.erro("Corrija os campos destacados para criar a conta."));
            return VIEW;
        }
        redirecao.addFlashAttribute("alerta", Alerta.sucesso("Conta criada. Entre com o seu usuário e a sua senha."));
        redirecao.addFlashAttribute("usuarioCriado", formulario.usuario().strip());
        return "redirect:/login";
    }
}

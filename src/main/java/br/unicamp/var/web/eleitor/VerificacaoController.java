package br.unicamp.var.web.eleitor;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Verificação pública de comprovante (história #28), sem login.
 */
@Controller
public class VerificacaoController {

    private final ServicoVerificacao servico;

    /**
     * Cria o controller.
     *
     * @param servico serviço de verificação
     */
    public VerificacaoController(ServicoVerificacao servico) {
        this.servico = servico;
    }

    /**
     * Formulário e, se vierem os parâmetros, o resultado da conferência.
     *
     * @param votacao id da votação (opcional)
     * @param codigo  código do comprovante (opcional)
     * @param modelo  modelo da página
     * @return a view {@code verificar}
     */
    @GetMapping("/verificar")
    public String verificar(@RequestParam(required = false) String votacao,
                            @RequestParam(required = false) String codigo, Model modelo) {
        modelo.addAttribute("pagina", servico.verificar(votacao, codigo));
        return "verificar";
    }
}

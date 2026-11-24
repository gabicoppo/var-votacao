package br.unicamp.var.web.eleitor;

import br.unicamp.var.web.comum.Alerta;
import br.unicamp.var.web.comum.MensagensErro;
import java.util.Optional;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Cédula Top 3 de uma premiação (história #23): escolhas → revisão → sucesso, como no voto.
 */
@Controller
@RequestMapping("/eleitor/premiacoes/{id}")
public class PremiacaoEleitorController {

    private static final String REGISTRADA = "cedulaRegistrada";

    private final ServicoPremiacaoEleitor servico;

    /**
     * Cria o controller.
     *
     * @param servico serviço da cédula Top 3
     */
    public PremiacaoEleitorController(ServicoPremiacaoEleitor servico) {
        this.servico = servico;
    }

    /**
     * Cédula Top 3 (ou o aviso de que não dá para votar agora).
     *
     * @param id      id da premiação
     * @param usuario eleitor logado
     * @param modelo  modelo da página
     * @return {@code eleitor/premiacao-cedula} ou {@code eleitor/premiacao-indisponivel}
     */
    @GetMapping
    public String cedula(@PathVariable String id, @AuthenticationPrincipal UserDetails usuario, Model modelo) {
        CedulaPremiacao cedula = servico.cedula(id, usuario.getUsername());
        modelo.addAttribute("cedula", cedula);
        return cedula.situacao() == SituacaoCedula.PODE_VOTAR
                ? "eleitor/premiacao-cedula" : "eleitor/premiacao-indisponivel";
    }

    /**
     * Revisão da cédula; com escolhas inválidas, volta à cédula com o erro.
     *
     * @param id       id da premiação
     * @param escolhas 1º, 2º e 3º lugares
     * @param usuario  eleitor logado
     * @param modelo   modelo da página
     * @return {@code eleitor/premiacao-revisar}, a cédula com erro ou o redirecionamento
     */
    @PostMapping("/revisar")
    public String revisar(@PathVariable String id, EscolhasTop3 escolhas, @AuthenticationPrincipal UserDetails usuario,
                          Model modelo) {
        CedulaPremiacao cedula = servico.cedula(id, usuario.getUsername());
        if (cedula.situacao() != SituacaoCedula.PODE_VOTAR) {
            return "redirect:/eleitor/premiacoes/" + id;
        }
        modelo.addAttribute("cedula", cedula);
        modelo.addAttribute("escolhas", escolhas);
        Optional<String> erro = servico.validar(cedula, escolhas.primeiro(), escolhas.segundo(), escolhas.terceiro());
        if (erro.isPresent()) {
            modelo.addAttribute("erro", erro.get());
            return "eleitor/premiacao-cedula";
        }
        return "eleitor/premiacao-revisar";
    }

    /**
     * Registra a cédula e redireciona à tela de sucesso; em caso de erro, volta à cédula com o alerta.
     *
     * @param id        id da premiação
     * @param escolhas  1º, 2º e 3º lugares
     * @param usuario   eleitor logado
     * @param redirecao atributos flash
     * @return o redirecionamento
     */
    @PostMapping("/voto")
    public String votar(@PathVariable String id, EscolhasTop3 escolhas, @AuthenticationPrincipal UserDetails usuario,
                        RedirectAttributes redirecao) {
        try {
            servico.registrar(id, usuario.getUsername(), escolhas.primeiro(), escolhas.segundo(), escolhas.terceiro());
            redirecao.addFlashAttribute(REGISTRADA, Boolean.TRUE);
            return "redirect:/eleitor/premiacoes/" + id + "/sucesso";
        } catch (IllegalArgumentException | IllegalStateException erro) {
            redirecao.addFlashAttribute("alerta", Alerta.erro(MensagensErro.amigavel(erro)));
            return "redirect:/eleitor/premiacoes/" + id;
        }
    }

    /**
     * Tela de sucesso, mostrada uma vez. Recarregar leva ao painel.
     *
     * @param id        id da premiação
     * @param usuario   eleitor logado
     * @param modelo    modelo da página
     * @param redirecao atributos flash
     * @return {@code eleitor/premiacao-sucesso} ou um redirecionamento
     */
    @GetMapping("/sucesso")
    public String sucesso(@PathVariable String id, @AuthenticationPrincipal UserDetails usuario, Model modelo,
                          RedirectAttributes redirecao) {
        if (modelo.containsAttribute(REGISTRADA)) {
            modelo.addAttribute("cedula", servico.cedula(id, usuario.getUsername()));
            return "eleitor/premiacao-sucesso";
        }
        if (servico.jaVotou(id, usuario.getUsername())) {
            redirecao.addFlashAttribute("alerta", Alerta.info("Sua cédula já foi registrada."));
            return "redirect:/eleitor";
        }
        return "redirect:/eleitor/premiacoes/" + id;
    }

    /**
     * Escolhas do formulário Top 3.
     *
     * @param primeiro candidato em 1º lugar
     * @param segundo  candidato em 2º lugar
     * @param terceiro candidato em 3º lugar
     */
    public record EscolhasTop3(String primeiro, String segundo, String terceiro) {
    }
}

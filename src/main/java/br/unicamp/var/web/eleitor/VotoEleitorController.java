package br.unicamp.var.web.eleitor;

import br.unicamp.var.web.comum.Alerta;
import br.unicamp.var.web.comum.MensagensErro;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * Voto numa votação em até 3 telas (RNF-04): cédula → revisão → comprovante (histórias #26 e #28).
 * O voto segue o padrão POST → redirect → GET, e o comprovante aparece uma única vez.
 */
@Controller
@RequestMapping("/eleitor/votacoes/{id}")
public class VotoEleitorController {

    private static final String COMPROVANTE = "comprovante";

    private final ServicoVotoEleitor servico;

    /**
     * Cria o controller.
     *
     * @param servico serviço de voto
     */
    public VotoEleitorController(ServicoVotoEleitor servico) {
        this.servico = servico;
    }

    /**
     * Tela 1: a cédula (ou o aviso de que não dá para votar agora).
     *
     * @param id      id da votação
     * @param opcao   opção já marcada, ao voltar da revisão (opcional)
     * @param usuario eleitor logado
     * @param modelo  modelo da página
     * @return {@code eleitor/cedula} ou {@code eleitor/indisponivel}
     */
    @GetMapping
    public String cedula(@PathVariable String id, @RequestParam(required = false) String opcao,
                         @AuthenticationPrincipal UserDetails usuario, Model modelo) {
        CedulaVotacao cedula = servico.cedula(id, usuario.getUsername());
        modelo.addAttribute("cedula", cedula);
        modelo.addAttribute("selecionada", opcao);
        return cedula.situacao() == SituacaoCedula.PODE_VOTAR ? "eleitor/cedula" : "eleitor/indisponivel";
    }

    /**
     * Tela 2: "Confira seu voto". Sem opção válida, volta à cédula com o erro.
     *
     * @param id      id da votação
     * @param opcao   opção marcada
     * @param usuario eleitor logado
     * @param modelo  modelo da página
     * @return {@code eleitor/revisar}, {@code eleitor/cedula} com erro ou o redirecionamento à cédula
     */
    @PostMapping("/revisar")
    public String revisar(@PathVariable String id, @RequestParam(required = false) String opcao,
                          @AuthenticationPrincipal UserDetails usuario, Model modelo) {
        CedulaVotacao cedula = servico.cedula(id, usuario.getUsername());
        if (cedula.situacao() != SituacaoCedula.PODE_VOTAR) {
            return "redirect:/eleitor/votacoes/" + id;
        }
        modelo.addAttribute("cedula", cedula);
        if (!cedula.temOpcao(opcao)) {
            modelo.addAttribute("erro", "Escolha uma opção para continuar.");
            return "eleitor/cedula";
        }
        modelo.addAttribute("opcao", opcao);
        return "eleitor/revisar";
    }

    /**
     * Registra o voto e redireciona ao comprovante; em caso de erro, volta à cédula com o alerta.
     *
     * @param id          id da votação
     * @param opcao       opção confirmada
     * @param usuario     eleitor logado
     * @param redirecao   atributos flash
     * @return o redirecionamento
     */
    @PostMapping("/voto")
    public String votar(@PathVariable String id, @RequestParam(required = false) String opcao,
                        @AuthenticationPrincipal UserDetails usuario, RedirectAttributes redirecao) {
        try {
            redirecao.addFlashAttribute(COMPROVANTE, servico.votar(id, usuario.getUsername(), opcao));
            return "redirect:/eleitor/votacoes/" + id + "/comprovante";
        } catch (IllegalArgumentException | IllegalStateException erro) {
            redirecao.addFlashAttribute("alerta", Alerta.erro(MensagensErro.amigavel(erro)));
            return "redirect:/eleitor/votacoes/" + id;
        }
    }

    /**
     * Tela 3: o comprovante, mostrado uma vez (chega por flash). Recarregar leva ao painel. Recebe também
     * {@code urlBase} (ex.: {@code https://var.exemplo}), para o cartão impresso trazer o link completo de
     * verificação.
     *
     * @param id        id da votação
     * @param usuario   eleitor logado
     * @param modelo    modelo da página (com o flash, se houver)
     * @param redirecao atributos flash
     * @return {@code eleitor/comprovante} ou um redirecionamento
     */
    @GetMapping("/comprovante")
    public String comprovante(@PathVariable String id, @AuthenticationPrincipal UserDetails usuario, Model modelo,
                              RedirectAttributes redirecao) {
        if (modelo.containsAttribute(COMPROVANTE)) {
            modelo.addAttribute("urlBase", ServletUriComponentsBuilder.fromCurrentContextPath().toUriString());
            return "eleitor/comprovante";
        }
        if (servico.jaVotou(id, usuario.getUsername())) {
            redirecao.addFlashAttribute("alerta", Alerta.info("Seu voto já foi registrado."));
            return "redirect:/eleitor";
        }
        return "redirect:/eleitor/votacoes/" + id;
    }
}

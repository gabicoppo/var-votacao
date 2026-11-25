package br.unicamp.var.web.premiacao;

import br.unicamp.var.web.comum.Alerta;
import br.unicamp.var.web.resultado.RecursoNaoEncontradoException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Telas do organizador para premiações Top 3 (história #23): nova premiação, códigos de acesso e
 * gestão do ciclo. Controller fino: a lógica fica em {@link GestaoPremiacoes}.
 */
@Controller
@RequestMapping("/organizador/premiacoes")
public class PremiacaoOrganizadorController {

    private static final String ALERTA = "alerta";
    private static final String FORMULARIO = "formulario";
    private static final String REDIRECT_GESTAO = "redirect:/organizador/premiacoes/";

    private final GestaoPremiacoes gestao;

    /**
     * Cria o controller.
     *
     * @param gestao casos de uso das premiações
     */
    public PremiacaoOrganizadorController(GestaoPremiacoes gestao) {
        this.gestao = gestao;
    }

    /**
     * Formulário de nova premiação, vazio.
     *
     * @param model modelo da view
     * @return a view do formulário
     */
    @GetMapping("/nova")
    public String nova(Model model) {
        return formulario(new FormularioPremiacao(), Map.of(), model);
    }

    /**
     * Preenche o formulário com o cenário de exemplo. Funciona sem JavaScript (botão de envio com
     * {@code formnovalidate}).
     *
     * @param model modelo da view
     * @return a view do formulário preenchido
     */
    @PostMapping(value = "/nova", params = "preencherExemplo")
    public String preencherExemplo(Model model) {
        model.addAttribute(ALERTA, Alerta.info("Exemplo preenchido: confira e clique em \"Criar premiação\"."));
        return formulario(gestao.exemplo(), Map.of(), model);
    }

    /**
     * Cria a premiação ou mostra o formulário de novo, com os erros por campo.
     *
     * @param formulario dados digitados
     * @param usuario    organizador logado, que vira o dono da premiação
     * @param model      modelo da view
     * @param redirect   atributos flash
     * @return redirecionamento para os códigos de acesso ou a gestão, ou o formulário com erros
     */
    @PostMapping("/nova")
    public String criar(@ModelAttribute(FORMULARIO) FormularioPremiacao formulario,
                        @AuthenticationPrincipal UserDetails usuario, Model model, RedirectAttributes redirect) {
        ResultadoCriacaoPremiacao resultado = gestao.criar(formulario, usuario.getUsername());
        if (!resultado.criada()) {
            Map<String, String> erros = new LinkedHashMap<>();
            resultado.erros().forEach(erro -> erros.putIfAbsent(erro.campo(), erro.mensagem()));
            model.addAttribute(ALERTA, Alerta.erro("Corrija os campos destacados para criar a premiação."));
            return formulario(formulario, erros, model);
        }
        redirect.addFlashAttribute(ALERTA, Alerta.sucesso("Premiação criada em rascunho."));
        if (resultado.acessos().isEmpty()) {
            return REDIRECT_GESTAO + resultado.id();
        }
        redirect.addFlashAttribute("acessos", resultado.acessos());
        return REDIRECT_GESTAO + resultado.id() + "/acessos";
    }

    /**
     * Códigos de acesso das contas novas, mostrados uma única vez (vêm em atributo flash).
     *
     * @param id    id da premiação
     * @param model modelo da view (com "acessos", se acabou de criar)
     * @return a view dos acessos
     */
    @GetMapping("/{id}/acessos")
    public String acessos(@PathVariable("id") String id, Model model) {
        model.addAttribute("premiacaoId", id);
        model.addAttribute("tituloPremiacao", gestao.titulo(id));
        if (!model.containsAttribute("acessos")) {
            model.addAttribute("acessos", List.<AcessoPremiacao>of());
        }
        return "premiacao/acessos";
    }

    /**
     * Gestão da premiação: configuração, participação e ações do ciclo.
     *
     * @param id    id da premiação
     * @param model modelo da view
     * @return a view de gestão
     */
    @GetMapping("/{id}")
    public String gestao(@PathVariable("id") String id, Model model) {
        model.addAttribute("premiacao", gestao.detalhe(id));
        return "premiacao/gestao";
    }

    /**
     * Abre a premiação.
     *
     * @param id       id da premiação
     * @param redirect atributos flash
     * @return redirecionamento para a gestão
     */
    @PostMapping("/{id}/abrir")
    public String abrir(@PathVariable("id") String id, RedirectAttributes redirect) {
        return acao(id, redirect, () -> {
            gestao.abrir(id);
            return "Premiação aberta. Os eleitores aptos já podem votar.";
        });
    }

    /**
     * Encerra a premiação.
     *
     * @param id       id da premiação
     * @param redirect atributos flash
     * @return redirecionamento para a gestão
     */
    @PostMapping("/{id}/encerrar")
    public String encerrar(@PathVariable("id") String id, RedirectAttributes redirect) {
        return acao(id, redirect, () -> {
            gestao.encerrar(id);
            return "Premiação encerrada. Agora é só apurar.";
        });
    }

    /**
     * Apura a premiação e publica o ranking.
     *
     * @param id       id da premiação
     * @param redirect atributos flash
     * @return redirecionamento para a gestão
     */
    @PostMapping("/{id}/apurar")
    public String apurar(@PathVariable("id") String id, RedirectAttributes redirect) {
        return acao(id, redirect, () -> {
            gestao.apurar(id);
            return "Premiação apurada. O ranking já está publicado.";
        });
    }

    /**
     * Premiação inexistente: página 404 amigável.
     *
     * @param erro  exceção com o que não foi encontrado
     * @param model modelo da view
     * @return a view de não encontrado
     */
    @ExceptionHandler(RecursoNaoEncontradoException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String naoEncontrada(RecursoNaoEncontradoException erro, Model model) {
        model.addAttribute("recurso", erro.recurso());
        return "resultado/nao-encontrado";
    }

    private String formulario(FormularioPremiacao formulario, Map<String, String> erros, Model model) {
        model.addAttribute(FORMULARIO, formulario);
        model.addAttribute("erros", erros);
        return "premiacao/nova";
    }

    private String acao(String id, RedirectAttributes redirect, AcaoComMensagem acao) {
        try {
            redirect.addFlashAttribute(ALERTA, Alerta.sucesso(acao.executar()));
        } catch (OperacaoRecusadaException e) {
            redirect.addFlashAttribute(ALERTA, Alerta.erro(e.getMessage()));
        }
        return REDIRECT_GESTAO + id;
    }

    /** Ação que devolve a mensagem de sucesso ou lança {@link OperacaoRecusadaException}. */
    @FunctionalInterface
    private interface AcaoComMensagem {
        String executar();
    }
}

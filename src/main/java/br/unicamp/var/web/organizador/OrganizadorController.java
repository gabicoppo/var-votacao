package br.unicamp.var.web.organizador;

import br.unicamp.var.processo.Votacao;
import br.unicamp.var.web.comum.Alerta;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * Telas do organizador: painel, nova votação, códigos de acesso e gestão da votação
 * (histórias #19 a #24, RNF-07). Controller fino: a lógica fica em {@link GestaoVotacoes}.
 */
@Controller
@RequestMapping("/organizador")
public class OrganizadorController {

    private static final String ALERTA = "alerta";
    private static final String FORMULARIO = "formulario";
    private static final String VIEW_NOVA = "organizador/nova";
    private static final String REDIRECT_GESTAO = "redirect:/organizador/votacoes/";

    private final GestaoVotacoes gestao;

    /**
     * @param gestao casos de uso do organizador
     */
    public OrganizadorController(GestaoVotacoes gestao) {
        this.gestao = gestao;
    }

    /**
     * Painel com métricas, votações filtradas por status e premiações.
     *
     * @param status  filtro opcional ({@code ?status=aberta})
     * @param usuario organizador logado: o painel mostra só o que ele criou
     * @param model   modelo da view
     * @return a view do painel
     */
    @GetMapping
    public String painel(@RequestParam(name = "status", required = false) String status,
                         @AuthenticationPrincipal UserDetails usuario, Model model) {
        model.addAttribute("painel", gestao.painel(status, usuario.getUsername()));
        return "organizador/painel";
    }

    /**
     * Formulário de nova votação, com os valores padrão.
     *
     * @param model modelo da view
     * @return a view do formulário
     */
    @GetMapping("/votacoes/nova")
    public String nova(Model model) {
        return formulario(new FormularioVotacao(), Map.of(), model);
    }

    /**
     * Preenche a lista de aptos com os eleitores de demonstração, mantendo o resto do formulário.
     * Funciona sem JavaScript (botão de envio com {@code formnovalidate}).
     *
     * @param formulario dados digitados até agora
     * @param binding    erros de conversão (ignorados aqui)
     * @param model      modelo da view
     * @return a view do formulário
     */
    @PostMapping(value = "/votacoes/nova", params = "usarDemonstracao")
    public String usarDemonstracao(@ModelAttribute(FORMULARIO) FormularioVotacao formulario, BindingResult binding,
                                   Model model) {
        formulario.setEleitores(gestao.eleitoresDemonstracao());
        model.addAttribute(ALERTA, Alerta.info("Lista de eleitores de demonstração (e01 a e12) preenchida."));
        return formulario(formulario, Map.of(), model);
    }

    /**
     * Cria a votação ou mostra o formulário de novo, com os erros por campo.
     *
     * @param formulario dados digitados
     * @param binding    erros de conversão, como data inválida
     * @param usuario    organizador logado
     * @param model      modelo da view
     * @param redirect   atributos flash
     * @return redirecionamento para os acessos ou a gestão, ou o formulário com erros
     */
    @PostMapping("/votacoes/nova")
    public String criar(@ModelAttribute(FORMULARIO) FormularioVotacao formulario, BindingResult binding,
                        @AuthenticationPrincipal UserDetails usuario, Model model, RedirectAttributes redirect) {
        Map<String, String> erros = new LinkedHashMap<>();
        binding.getFieldErrors().forEach(erro -> erros.putIfAbsent(erro.getField(), "Data e hora inválidas."));
        ResultadoCriacao resultado = erros.isEmpty() ? gestao.criar(formulario, usuario.getUsername()) : null;
        if (resultado == null || !resultado.criada()) {
            if (resultado != null) {
                resultado.erros().forEach(erro -> erros.putIfAbsent(erro.campo(), erro.mensagem()));
            }
            model.addAttribute(ALERTA, Alerta.erro("Corrija os campos destacados para criar a votação."));
            return formulario(formulario, erros, model);
        }
        redirect.addFlashAttribute(ALERTA, Alerta.sucesso("Votação criada em rascunho."));
        if (resultado.acessos().isEmpty()) {
            return REDIRECT_GESTAO + resultado.id();
        }
        redirect.addFlashAttribute("acessos", resultado.acessos());
        return REDIRECT_GESTAO + resultado.id() + "/acessos";
    }

    /**
     * Códigos de acesso das contas novas, mostrados uma única vez (vêm em atributo flash).
     *
     * @param id    id da votação
     * @param model modelo da view (com "acessos", se acabou de criar)
     * @return a view dos acessos
     */
    @GetMapping("/votacoes/{id}/acessos")
    public String acessos(@PathVariable("id") String id, Model model) {
        model.addAttribute("votacaoId", id);
        model.addAttribute("pergunta", gestao.pergunta(id));
        if (!model.containsAttribute("acessos")) {
            model.addAttribute("acessos", List.<AcessoEleitor>of());
        }
        return "organizador/acessos";
    }

    /**
     * Gestão de uma votação: configuração, andamento, ações, presença e auditoria.
     *
     * @param id    id da votação
     * @param model modelo da view
     * @return a view de gestão
     */
    @GetMapping("/votacoes/{id}")
    public String gestao(@PathVariable("id") String id, Model model) {
        return telaGestao(id, gestao.formularioRegra(id), Map.of(), model);
    }

    /**
     * Ids de quem já votou, para a lista de presença ao vivo. Nunca inclui a escolha (RNF-01).
     *
     * @param id id da votação
     * @return {@code {votaram: [...]}}
     */
    @GetMapping("/votacoes/{id}/presenca.json")
    @ResponseBody
    public PresencaJson presenca(@PathVariable("id") String id) {
        return new PresencaJson(gestao.quemVotou(id));
    }

    /**
     * Descrição da regra montada com os campos do formulário, para o resumo ao vivo.
     *
     * @param formulario campos da regra ({@code tipoRegra}, {@code segundoTurno}, {@code quorum}...)
     * @return {@code {descricao}}
     */
    @GetMapping("/votacoes/descricao-regra")
    @ResponseBody
    public DescricaoRegraJson descricaoRegra(@ModelAttribute FormularioVotacao formulario) {
        return new DescricaoRegraJson(ConversorFormularioVotacao.descricaoRegra(formulario));
    }

    /**
     * Troca a regra de uma votação em rascunho (RNF-07).
     *
     * @param id         id da votação
     * @param formulario campos da regra
     * @param usuario    organizador logado
     * @param model      modelo da view, se houver erro nos campos
     * @param redirect   atributos flash
     * @return redirecionamento para a gestão, ou a gestão com os erros da regra
     */
    @PostMapping("/votacoes/{id}/regra")
    public String alterarRegra(@PathVariable("id") String id, @ModelAttribute FormularioVotacao formulario,
                               @AuthenticationPrincipal UserDetails usuario, Model model,
                               RedirectAttributes redirect) {
        List<ErroCampo> erros;
        try {
            erros = gestao.alterarRegra(id, formulario, usuario.getUsername());
        } catch (OperacaoRecusadaException e) {
            redirect.addFlashAttribute(ALERTA, Alerta.erro(e.getMessage()));
            return REDIRECT_GESTAO + id;
        }
        if (!erros.isEmpty()) {
            Map<String, String> porCampo = new LinkedHashMap<>();
            erros.forEach(erro -> porCampo.putIfAbsent(erro.campo(), erro.mensagem()));
            model.addAttribute(ALERTA, Alerta.erro("Corrija a regra para salvar a alteração."));
            return telaGestao(id, formulario, porCampo, model);
        }
        redirect.addFlashAttribute(ALERTA, Alerta.sucesso("Regra alterada: "
                + ConversorFormularioVotacao.descricaoRegra(formulario)));
        return REDIRECT_GESTAO + id;
    }

    /**
     * Formulário de nova votação preenchido com a configuração desta, sem período.
     *
     * @param id    id da votação copiada
     * @param model modelo da view
     * @return a view do formulário
     */
    @GetMapping("/votacoes/{id}/duplicar")
    public String duplicar(@PathVariable("id") String id, Model model) {
        FormularioVotacao copia = gestao.duplicar(id);
        model.addAttribute(ALERTA, Alerta.info("Cópia pronta. Revise os dados, defina o período se quiser e crie."));
        return formulario(copia, Map.of(), model);
    }

    /**
     * Abre a votação.
     *
     * @param id       id da votação
     * @param usuario  organizador logado
     * @param redirect atributos flash
     * @return redirecionamento para a gestão
     */
    @PostMapping("/votacoes/{id}/abrir")
    public String abrir(@PathVariable("id") String id, @AuthenticationPrincipal UserDetails usuario,
                        RedirectAttributes redirect) {
        return acao(id, redirect, () -> {
            gestao.abrir(id, usuario.getUsername());
            return "Votação aberta. Os eleitores já podem votar.";
        });
    }

    /**
     * Encerra a votação.
     *
     * @param id       id da votação
     * @param usuario  organizador logado
     * @param redirect atributos flash
     * @return redirecionamento para a gestão
     */
    @PostMapping("/votacoes/{id}/encerrar")
    public String encerrar(@PathVariable("id") String id, @AuthenticationPrincipal UserDetails usuario,
                           RedirectAttributes redirect) {
        return acao(id, redirect, () -> {
            gestao.encerrar(id, usuario.getUsername());
            return "Votação encerrada. Agora é só apurar.";
        });
    }

    /**
     * Apura a votação.
     *
     * @param id       id da votação
     * @param usuario  organizador logado
     * @param redirect atributos flash
     * @return redirecionamento para a gestão
     */
    @PostMapping("/votacoes/{id}/apurar")
    public String apurar(@PathVariable("id") String id, @AuthenticationPrincipal UserDetails usuario,
                         RedirectAttributes redirect) {
        return acao(id, redirect, () -> "Votação apurada. " + gestao.apurar(id, usuario.getUsername()) + ".");
    }

    /**
     * Cria o 2º turno e leva à gestão dele.
     *
     * @param id       id da votação apurada
     * @param usuario  organizador logado
     * @param redirect atributos flash
     * @return redirecionamento para a gestão do 2º turno, ou desta votação se houver erro
     */
    @PostMapping("/votacoes/{id}/segundo-turno")
    public String segundoTurno(@PathVariable("id") String id, @AuthenticationPrincipal UserDetails usuario,
                               RedirectAttributes redirect) {
        try {
            Votacao segundoTurno = gestao.criarSegundoTurno(id, usuario.getUsername());
            redirect.addFlashAttribute(ALERTA,
                    Alerta.sucesso("2º turno criado em rascunho. Confira e abra a votação."));
            return REDIRECT_GESTAO + segundoTurno.id();
        } catch (OperacaoRecusadaException e) {
            redirect.addFlashAttribute(ALERTA, Alerta.erro(e.getMessage()));
            return REDIRECT_GESTAO + id;
        }
    }

    /**
     * Votação inexistente: página 404 amigável.
     *
     * @return a view de não encontrada
     */
    @ExceptionHandler(VotacaoNaoEncontradaException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String naoEncontrada() {
        return "organizador/nao-encontrada";
    }

    private String telaGestao(String id, FormularioVotacao formularioRegra, Map<String, String> errosRegra,
                              Model model) {
        DetalheVotacao detalhe = gestao.detalhe(id);
        model.addAttribute("votacao", detalhe);
        model.addAttribute("formularioRegra", formularioRegra);
        model.addAttribute("erros", errosRegra);
        model.addAttribute("descricaoRegra", ConversorFormularioVotacao.descricaoRegra(formularioRegra));
        String link = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/eleitor/votacoes/{id}").buildAndExpand(id).toUriString();
        model.addAttribute("convite", ConviteVotacao.de(detalhe.pergunta(), detalhe.configuracao().fim(), link));
        return "organizador/gestao";
    }

    private String formulario(FormularioVotacao formulario, Map<String, String> erros, Model model) {
        model.addAttribute(FORMULARIO, formulario);
        model.addAttribute("erros", erros);
        model.addAttribute("descricaoRegra", ConversorFormularioVotacao.descricaoRegra(formulario));
        return VIEW_NOVA;
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

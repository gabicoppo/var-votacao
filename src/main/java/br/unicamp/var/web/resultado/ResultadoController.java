package br.unicamp.var.web.resultado;

import br.unicamp.var.web.comum.Alerta;
import br.unicamp.var.web.comum.Papel;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Telas de resultado, andamento e relatório de uma votação (histórias #29 e #31). Controller fino:
 * a lógica fica em {@link ConsultaResultados}.
 */
@Controller
@RequestMapping("/votacoes/{id}")
public class ResultadoController {

    private final ConsultaResultados consulta;

    /**
     * Cria o controller.
     *
     * @param consulta consultas de resultado
     */
    public ResultadoController(ConsultaResultados consulta) {
        this.consulta = consulta;
    }

    /**
     * Resultado da votação: andamento ao vivo antes da apuração, faixa de situação e barras depois.
     *
     * @param id         id da votação
     * @param usuario    usuário logado
     * @param requisicao requisição, para saber o papel
     * @param model      modelo da view
     * @return a view do resultado
     */
    @GetMapping("/resultado")
    public String resultado(@PathVariable("id") String id, @AuthenticationPrincipal UserDetails usuario,
                            HttpServletRequest requisicao, Model model) {
        boolean organizador = organizador(requisicao);
        model.addAttribute("pagina", consulta.pagina(id, usuario.getUsername(), organizador));
        model.addAttribute("organizador", organizador);
        return "resultado/resultado";
    }

    /**
     * Participação atual em JSON, para a página de andamento se atualizar sozinha.
     *
     * @param id         id da votação
     * @param usuario    usuário logado
     * @param requisicao requisição, para saber o papel
     * @return {@code {status, votantes, aptos}}
     */
    @GetMapping("/andamento.json")
    @ResponseBody
    public AndamentoJson andamento(@PathVariable("id") String id, @AuthenticationPrincipal UserDetails usuario,
                                   HttpServletRequest requisicao) {
        return consulta.andamento(id, usuario.getUsername(), organizador(requisicao));
    }

    /**
     * Ata de apuração imprimível; antes da apuração, volta ao resultado com um aviso.
     *
     * @param id         id da votação
     * @param usuario    usuário logado
     * @param requisicao requisição, para saber o papel
     * @param model      modelo da view
     * @param redirect   atributos flash
     * @return a view do relatório ou o redirecionamento para o resultado
     */
    @GetMapping("/relatorio")
    public String relatorio(@PathVariable("id") String id, @AuthenticationPrincipal UserDetails usuario,
                            HttpServletRequest requisicao, Model model, RedirectAttributes redirect) {
        Optional<PaginaRelatorio> relatorio = consulta.relatorio(id, usuario.getUsername(), organizador(requisicao));
        if (relatorio.isEmpty()) {
            redirect.addFlashAttribute("alerta",
                    Alerta.info("O relatório de apuração fica disponível depois que a votação for apurada."));
            return "redirect:/votacoes/" + id + "/resultado";
        }
        model.addAttribute("relatorio", relatorio.get());
        return "resultado/relatorio";
    }

    /**
     * Votação inexistente: página 404 amigável.
     *
     * @param erro  exceção com o que não foi encontrado
     * @param model modelo da view
     * @return a view de não encontrado
     */
    @ExceptionHandler(RecursoNaoEncontradoException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String naoEncontrado(RecursoNaoEncontradoException erro, Model model) {
        model.addAttribute("recurso", erro.recurso());
        return "resultado/nao-encontrado";
    }

    /**
     * Eleitor fora da lista de aptos: página 403 amigável.
     *
     * @return a view de acesso negado
     */
    @ExceptionHandler(AcessoNegadoException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String acessoNegado() {
        return "error/403";
    }

    /**
     * Diz se quem fez a requisição é organizador.
     *
     * @param requisicao requisição atual
     * @return {@code true} se tem o papel {@link Papel#ORGANIZADOR}
     */
    static boolean organizador(HttpServletRequest requisicao) {
        return requisicao.isUserInRole(Papel.ORGANIZADOR.name());
    }
}

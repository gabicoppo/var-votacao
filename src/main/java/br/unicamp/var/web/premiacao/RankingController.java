package br.unicamp.var.web.premiacao;

import br.unicamp.var.web.comum.Papel;
import br.unicamp.var.web.resultado.AcessoNegadoException;
import br.unicamp.var.web.resultado.AndamentoJson;
import br.unicamp.var.web.resultado.RecursoNaoEncontradoException;
import jakarta.servlet.http.HttpServletRequest;
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

/**
 * Ranking público de uma premiação (história #30), para o organizador e os eleitores aptos.
 * Controller fino: a lógica fica em {@link ConsultaRanking}.
 */
@Controller
@RequestMapping("/premiacoes/{id}")
public class RankingController {

    private final ConsultaRanking consulta;

    /**
     * Cria o controller.
     *
     * @param consulta consulta do ranking
     */
    public RankingController(ConsultaRanking consulta) {
        this.consulta = consulta;
    }

    /**
     * Ranking com pódio, tabela e critérios; antes da apuração, o andamento.
     *
     * @param id         id da premiação
     * @param usuario    usuário logado
     * @param requisicao requisição, para saber o papel
     * @param model      modelo da view
     * @return a view do ranking
     */
    @GetMapping("/ranking")
    public String ranking(@PathVariable("id") String id, @AuthenticationPrincipal UserDetails usuario,
                          HttpServletRequest requisicao, Model model) {
        boolean organizador = organizador(requisicao);
        model.addAttribute("pagina", consulta.pagina(id, usuario.getUsername(), organizador));
        model.addAttribute("organizador", organizador);
        return "premiacao/ranking";
    }

    /**
     * Modo telão da premiação (só organizador): cédulas ao vivo enquanto aberta e, depois de apurada,
     * a revelação do pódio do 3º ao 1º lugar, no ritmo de quem apresenta.
     *
     * @param id         id da premiação
     * @param usuario    usuário logado
     * @param requisicao requisição, para saber o papel
     * @param model      modelo da view
     * @return a view do telão, que não usa o layout do site
     * @throws AcessoNegadoException se quem pede não for organizador
     */
    @GetMapping("/telao")
    public String telao(@PathVariable("id") String id, @AuthenticationPrincipal UserDetails usuario,
                        HttpServletRequest requisicao, Model model) {
        if (!organizador(requisicao)) {
            throw new AcessoNegadoException();
        }
        model.addAttribute("pagina", consulta.pagina(id, usuario.getUsername(), true));
        return "premiacao/telao";
    }

    /**
     * Participação atual em JSON, para a página de andamento se atualizar sozinha.
     *
     * @param id         id da premiação
     * @param usuario    usuário logado
     * @param requisicao requisição, para saber o papel
     * @return {@code {status, votantes, aptos}}, com cédulas no lugar de votantes
     */
    @GetMapping("/andamento.json")
    @ResponseBody
    public AndamentoJson andamento(@PathVariable("id") String id, @AuthenticationPrincipal UserDetails usuario,
                                   HttpServletRequest requisicao) {
        return consulta.andamento(id, usuario.getUsername(), organizador(requisicao));
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

    private static boolean organizador(HttpServletRequest requisicao) {
        return requisicao.isUserInRole(Papel.ORGANIZADOR.name());
    }
}

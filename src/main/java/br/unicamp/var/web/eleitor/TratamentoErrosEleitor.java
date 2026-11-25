package br.unicamp.var.web.eleitor;

import br.unicamp.var.web.comum.ContasUsuarios;
import jakarta.servlet.http.HttpServletRequest;
import java.security.Principal;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

/**
 * Eleitor fora da lista de aptos (RNF-05): página amigável com HTTP 403, só nos controllers do eleitor.
 *
 * <p>Os métodos {@code @ModelAttribute} não rodam para páginas de {@code @ExceptionHandler}, então
 * este advice preenche ele mesmo o que o layout usa ({@code nomeUsuario} e {@code caminhoAtual}). O
 * título da votação vai em {@code tituloVotacao}, porque {@code titulo} é o parâmetro do layout.</p>
 */
@ControllerAdvice(assignableTypes = {VotoEleitorController.class, PremiacaoEleitorController.class})
public class TratamentoErrosEleitor {

    private final ContasUsuarios contas;

    /**
     * Cria o advice.
     *
     * @param contas contas de usuário, para o nome no cabeçalho
     */
    public TratamentoErrosEleitor(ContasUsuarios contas) {
        this.contas = contas;
    }

    /**
     * Página "Você não está na lista de eleitores".
     *
     * @param erro       exceção com o título da votação ou premiação
     * @param usuario    eleitor logado
     * @param requisicao requisição atual
     * @return a view {@code eleitor/nao-apto} com status 403
     */
    @ExceptionHandler(EleitorNaoAptoException.class)
    public ModelAndView naoApto(EleitorNaoAptoException erro, Principal usuario, HttpServletRequest requisicao) {
        ModelAndView pagina = new ModelAndView("eleitor/nao-apto", HttpStatus.FORBIDDEN);
        pagina.addObject("tituloVotacao", erro.getMessage());
        pagina.addObject("nomeUsuario", contas.nomeDe(usuario.getName()).orElse(usuario.getName()));
        pagina.addObject("caminhoAtual", requisicao.getRequestURI().substring(requisicao.getContextPath().length()));
        return pagina;
    }
}

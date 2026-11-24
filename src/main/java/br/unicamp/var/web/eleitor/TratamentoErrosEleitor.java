package br.unicamp.var.web.eleitor;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

/**
 * Eleitor fora da lista de aptos (RNF-05): página amigável com HTTP 403, só nos controllers do eleitor.
 */
@ControllerAdvice(assignableTypes = {VotoEleitorController.class, PremiacaoEleitorController.class})
public class TratamentoErrosEleitor {

    /**
     * Página "Você não está na lista de eleitores".
     *
     * @param erro exceção com o título da votação ou premiação
     * @return a view {@code eleitor/nao-apto} com status 403
     */
    @ExceptionHandler(EleitorNaoAptoException.class)
    public ModelAndView naoApto(EleitorNaoAptoException erro) {
        ModelAndView pagina = new ModelAndView("eleitor/nao-apto", HttpStatus.FORBIDDEN);
        pagina.addObject("titulo", erro.getMessage());
        return pagina;
    }
}

package br.unicamp.var.web.comum;

import jakarta.servlet.http.HttpServletRequest;
import java.security.Principal;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Atributos que todas as páginas recebem, usados pelo layout.
 */
@ControllerAdvice
public class ModeloComum {

    private final ContasUsuarios contas;

    /**
     * Cria o advice.
     *
     * @param contas contas de usuário, para achar o nome de exibição
     */
    public ModeloComum(ContasUsuarios contas) {
        this.contas = contas;
    }

    /**
     * Nome de exibição do usuário logado, para o cabeçalho.
     *
     * @param usuario usuário logado, ou {@code null}
     * @return o nome cadastrado, o login se não houver nome, ou {@code null} se anônimo
     */
    @ModelAttribute("nomeUsuario")
    public String nomeUsuario(Principal usuario) {
        if (usuario == null) {
            return null;
        }
        return contas.nomeDe(usuario.getName()).orElse(usuario.getName());
    }

    /**
     * Caminho da página atual, sem o contexto da aplicação, para o layout marcar o item ativo da
     * navegação (o Thymeleaf 3.1 não expõe mais {@code #request} nos templates).
     *
     * @param requisicao requisição atual
     * @return ex.: {@code "/organizador/votacoes/nova"}
     */
    @ModelAttribute("caminhoAtual")
    public String caminhoAtual(HttpServletRequest requisicao) {
        return requisicao.getRequestURI().substring(requisicao.getContextPath().length());
    }
}

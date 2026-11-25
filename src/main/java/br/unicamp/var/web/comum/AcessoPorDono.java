package br.unicamp.var.web.comum;

import br.unicamp.var.processo.Votacao;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Isola os organizadores: um organizador só abre as votações e premiações que criou (gestão,
 * códigos de acesso, resultado, relatório, telão, ranking). Para as dos outros, a resposta é 404,
 * como se não existissem. Eleitores não passam por aqui: o acesso deles é pela lista de aptos.
 */
@Component
public class AcessoPorDono implements HandlerInterceptor {

    /** Rotas que esta regra cobre (registradas em {@link ConfiguracaoWeb}). */
    static final String[] ROTAS = {
        "/organizador/votacoes/**", "/organizador/premiacoes/**", "/votacoes/**", "/premiacoes/**",
    };

    private static final Pattern RECURSO = Pattern.compile("^/(?:organizador/)?(votacoes|premiacoes)/([^/]+)");
    private static final Set<String> SEM_ID = Set.of("nova", "descricao-regra");

    private final RepositorioVotacoes votacoes;
    private final RepositorioPremiacoes premiacoes;

    /**
     * Cria o interceptador.
     *
     * @param votacoes   repositório de votações
     * @param premiacoes repositório de premiações
     */
    public AcessoPorDono(RepositorioVotacoes votacoes, RepositorioPremiacoes premiacoes) {
        this.votacoes = votacoes;
        this.premiacoes = premiacoes;
    }

    @Override
    public boolean preHandle(HttpServletRequest requisicao, HttpServletResponse resposta, Object handler)
            throws IOException {
        if (requisicao.getUserPrincipal() == null || !requisicao.isUserInRole(Papel.ORGANIZADOR.name())) {
            return true;
        }
        String caminho = requisicao.getRequestURI().substring(requisicao.getContextPath().length());
        Matcher recurso = RECURSO.matcher(caminho);
        if (!recurso.find() || SEM_ID.contains(recurso.group(2))) {
            return true;
        }
        if (podeAbrir(recurso.group(1), recurso.group(2), requisicao.getUserPrincipal().getName())) {
            return true;
        }
        resposta.sendError(HttpServletResponse.SC_NOT_FOUND);
        return false;
    }

    private boolean podeAbrir(String tipo, String id, String organizador) {
        if ("premiacoes".equals(tipo)) {
            return Donos.podeGerenciar(premiacoes, id, organizador);
        }
        Optional<Votacao> votacao = votacoes.buscar(id);
        if (votacao.isEmpty()) {
            return true;
        }
        synchronized (votacao.get()) {
            return Donos.podeGerenciar(votacao.get(), organizador);
        }
    }
}

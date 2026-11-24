package br.unicamp.var.web.eleitor;

import br.unicamp.var.web.comum.ContasUsuarios;
import br.unicamp.var.web.comum.RepositorioPremiacoes;
import br.unicamp.var.web.comum.RepositorioVotacoes;
import java.time.Clock;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * "Minhas votações" (história #26): o painel do eleitor logado.
 */
@Controller
public class PainelEleitorController {

    private final RepositorioVotacoes votacoes;
    private final RepositorioPremiacoes premiacoes;
    private final ContasUsuarios contas;
    private final Clock relogio;

    /**
     * Cria o controller.
     *
     * @param votacoes   repositório de votações
     * @param premiacoes repositório de premiações
     * @param contas     contas, para o nome na saudação
     * @param relogio    relógio da aplicação
     */
    public PainelEleitorController(RepositorioVotacoes votacoes, RepositorioPremiacoes premiacoes,
                                   ContasUsuarios contas, Clock relogio) {
        this.votacoes = votacoes;
        this.premiacoes = premiacoes;
        this.contas = contas;
        this.relogio = relogio;
    }

    /**
     * Painel com as votações e premiações do eleitor, em seções.
     *
     * @param usuario eleitor logado (username = id na lista de aptos)
     * @param modelo  modelo da página
     * @return a view {@code eleitor/painel}
     */
    @GetMapping("/eleitor")
    public String painel(@AuthenticationPrincipal UserDetails usuario, Model modelo) {
        String eleitorId = usuario.getUsername();
        modelo.addAttribute("painel",
                PainelEleitor.montar(eleitorId, votacoes.listar(), premiacoes.listar(), relogio));
        modelo.addAttribute("primeiroNome", primeiroNome(contas.nomeDe(eleitorId).orElse(eleitorId)));
        return "eleitor/painel";
    }

    static String primeiroNome(String nome) {
        return nome.strip().split("\\s+")[0];
    }
}

package br.unicamp.var.e2e;

import br.unicamp.var.processo.Votacao;
import br.unicamp.var.web.comum.PremiacaoCadastrada;
import br.unicamp.var.web.comum.RepositorioPremiacoes;
import br.unicamp.var.web.comum.RepositorioVotacoes;
import java.util.ArrayList;
import java.util.List;

/**
 * Páginas principais da aplicação com os dados de demonstração, para os cenários que percorrem todas
 * (RNF-04 e acessibilidade).
 */
final class PaginasPrincipais {

    /** Usuário anônimo. */
    static final String ANONIMO = "anônimo";
    /** Organizador da demonstração. */
    static final String ORGANIZADOR = "organizador";
    /** Eleitor da demonstração. */
    static final String ELEITOR = "e01";

    /**
     * Página a visitar.
     *
     * @param usuario quem visita ({@link #ANONIMO}, {@link #ORGANIZADOR} ou {@link #ELEITOR})
     * @param nome    nome curto, para mensagens e screenshots
     * @param url     caminho
     */
    record Visita(String usuario, String nome, String url) {
    }

    private PaginasPrincipais() {
    }

    /**
     * Lista as páginas principais, com os ids reais das votações e da premiação da demonstração.
     *
     * @param votacoes   repositório de votações
     * @param premiacoes repositório de premiações
     * @return as visitas, agrupadas por usuário
     */
    static List<Visita> listar(RepositorioVotacoes votacoes, RepositorioPremiacoes premiacoes) {
        String presidencial = id(votacoes, "Eleição presidencial");
        String assembleia = id(votacoes, "Assembleia");
        String craque = premiacoes.listar().stream().map(PremiacaoCadastrada::id).findFirst().orElseThrow();
        List<Visita> visitas = new ArrayList<>(List.of(
                new Visita(ANONIMO, "inicio", "/"),
                new Visita(ANONIMO, "login", "/login"),
                new Visita(ANONIMO, "verificar", "/verificar"),
                new Visita(ANONIMO, "erro-404", "/pagina-que-nao-existe"),
                new Visita(ORGANIZADOR, "painel-organizador", "/organizador"),
                new Visita(ORGANIZADOR, "nova-votacao", "/organizador/votacoes/nova"),
                new Visita(ORGANIZADOR, "gestao", "/organizador/votacoes/" + presidencial),
                new Visita(ORGANIZADOR, "resultado", "/votacoes/" + assembleia + "/resultado"),
                new Visita(ORGANIZADOR, "relatorio", "/votacoes/" + assembleia + "/relatorio"),
                new Visita(ORGANIZADOR, "gestao-premiacao", "/organizador/premiacoes/" + craque),
                new Visita(ORGANIZADOR, "ranking", "/premiacoes/" + craque + "/ranking"),
                new Visita(ORGANIZADOR, "erro-403", "/eleitor"),
                new Visita(ELEITOR, "painel-eleitor", "/eleitor"),
                new Visita(ELEITOR, "cedula", "/eleitor/votacoes/" + presidencial),
                new Visita(ELEITOR, "cedula-top3", "/eleitor/premiacoes/" + craque)));
        return visitas;
    }

    private static String id(RepositorioVotacoes votacoes, String inicioPergunta) {
        return votacoes.listar().stream()
                .filter(v -> v.pergunta().startsWith(inicioPergunta))
                .map(Votacao::id)
                .findFirst()
                .orElseThrow();
    }
}

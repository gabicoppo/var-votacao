package br.unicamp.var.web.eleitor;

import java.time.Instant;
import java.util.List;

/**
 * Cédula de uma votação para o eleitor apto, com tudo calculado para a tela (história #26). Nunca
 * traz resultado parcial nem o voto de ninguém (RNF-01).
 *
 * @param id          id da votação
 * @param pergunta    pergunta
 * @param descricao   descrição (pode ser vazia)
 * @param opcoes      opções, na ordem de cadastro
 * @param secreta     se o voto é secreto
 * @param prazo       prazo em texto ("encerra hoje às 18:00", ...)
 * @param inicio      início do período, ou {@code null}
 * @param fim         fim do período, ou {@code null}
 * @param situacao    o que mostrar
 */
public record CedulaVotacao(String id, String pergunta, String descricao, List<String> opcoes, boolean secreta,
                            String prazo, Instant inicio, Instant fim, SituacaoCedula situacao) {

    /** Copia as opções. */
    public CedulaVotacao {
        opcoes = List.copyOf(opcoes);
    }

    /**
     * Aviso de sigilo exibido acima das opções.
     *
     * @return o texto do aviso, conforme o sigilo
     */
    public String avisoSigilo() {
        return secreta
                ? "Seu voto é secreto: ninguém, nem o organizador, verá sua escolha."
                : "Votação identificada: o organizador verá sua escolha.";
    }

    /**
     * Diz se a opção é uma das opções da cédula.
     *
     * @param opcao opção enviada pelo formulário
     * @return {@code true} se existir na cédula
     */
    public boolean temOpcao(String opcao) {
        return opcao != null && opcoes.contains(opcao);
    }
}

package br.unicamp.var.web.premiacao;

import br.unicamp.var.top3.StatusPremiacao;
import br.unicamp.var.web.resultado.Andamento;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

/**
 * Tela de gestão de uma premiação (história #23): configuração, participação e ações do ciclo.
 *
 * @param id         id da premiação
 * @param titulo     título
 * @param descricao  critério de avaliação, ou vazio
 * @param status     estado do ciclo
 * @param criadaEm   instante de cadastro
 * @param candidatos candidatos, na ordem de inscrição
 * @param grupos     grupos de votantes com os pesos
 * @param andamento  cédulas sobre eleitores aptos
 */
public record DetalhePremiacao(String id, String titulo, String descricao, StatusPremiacao status, Instant criadaEm,
                               List<String> candidatos, List<GrupoPremiacao> grupos, Andamento andamento) {

    /** Guarda cópias imutáveis das listas. */
    public DetalhePremiacao {
        candidatos = List.copyOf(candidatos);
        grupos = List.copyOf(grupos);
    }

    /**
     * Etapa em minúsculas, para a linha do tempo comum ({@code fragments/componentes :: linhaTempo}).
     *
     * @return ex.: {@code "aberta"}
     */
    public String etapa() {
        return status.name().toLowerCase(Locale.ROOT);
    }

    /**
     * Diz se dá para abrir.
     *
     * @return {@code true} em rascunho
     */
    public boolean podeAbrir() {
        return status == StatusPremiacao.RASCUNHO;
    }

    /**
     * Diz se dá para encerrar.
     *
     * @return {@code true} com a premiação aberta
     */
    public boolean podeEncerrar() {
        return status == StatusPremiacao.ABERTA;
    }

    /**
     * Diz se dá para apurar.
     *
     * @return {@code true} com a premiação encerrada
     */
    public boolean podeApurar() {
        return status == StatusPremiacao.ENCERRADA;
    }

    /**
     * Diz se o ranking já foi publicado.
     *
     * @return {@code true} com a premiação apurada
     */
    public boolean apurada() {
        return status == StatusPremiacao.APURADA;
    }
}

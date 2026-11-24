package br.unicamp.var.web.comum;

import br.unicamp.var.processo.Votacao;
import java.util.List;
import java.util.Optional;

/**
 * Porta de persistência das votações (portas e adaptadores). A implementação atual guarda em
 * memória; um banco de dados entra depois como outro adaptador, sem mudar quem usa esta interface.
 *
 * <p><b>Concorrência (regra para toda a camada web):</b> {@link Votacao} não é thread-safe. Toda
 * leitura e escrita de uma votação obtida daqui acontece dentro de
 * {@code synchronized (votacao) { ... }}, inclusive em templates de dados preparados pelo
 * controller (copie o que precisar para o modelo dentro do bloco). O repositório em si é
 * thread-safe.</p>
 */
public interface RepositorioVotacoes {

    /**
     * Guarda a votação, com chave {@code votacao.id()}; sobrescreve se já existir.
     *
     * @param votacao votação a guardar
     * @return a própria votação
     * @throws IllegalArgumentException se a votação for nula
     */
    Votacao salvar(Votacao votacao);

    /**
     * Busca uma votação pelo id.
     *
     * @param id identificador da votação
     * @return a votação, ou vazio se o id for nulo ou desconhecido
     */
    Optional<Votacao> buscar(String id);

    /**
     * Todas as votações, das mais recentes para as mais antigas ({@code criadaEm} decrescente).
     *
     * @return lista imutável
     */
    List<Votacao> listar();
}

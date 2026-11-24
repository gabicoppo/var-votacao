package br.unicamp.var.web.comum;

import br.unicamp.var.top3.PremiacaoTop3;
import java.util.List;
import java.util.Optional;

/**
 * Porta de persistência das premiações Top 3 (história #23). {@link PremiacaoTop3} já sincroniza
 * os próprios métodos, então a web não precisa de {@code synchronized} extra para ela.
 */
public interface RepositorioPremiacoes {

    /**
     * Cadastra a premiação, gerando id (UUID) e horário de cadastro (pelo {@link java.time.Clock}).
     *
     * @param premiacao premiação a guardar
     * @return o cadastro, com id e horário
     * @throws IllegalArgumentException se a premiação for nula
     */
    PremiacaoCadastrada salvar(PremiacaoTop3 premiacao);

    /**
     * Busca uma premiação pelo id.
     *
     * @param id identificador do cadastro
     * @return o cadastro, ou vazio se o id for nulo ou desconhecido
     */
    Optional<PremiacaoCadastrada> buscar(String id);

    /**
     * Todas as premiações, das mais recentes para as mais antigas.
     *
     * @return lista imutável
     */
    List<PremiacaoCadastrada> listar();
}

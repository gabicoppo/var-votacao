package br.unicamp.var.web.comum;

import br.unicamp.var.processo.Votacao;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

/**
 * {@link RepositorioVotacoes} em memória e thread-safe. Os dados somem quando a aplicação para.
 */
@Repository
public class RepositorioVotacoesEmMemoria implements RepositorioVotacoes {

    private final Map<String, Votacao> votacoes = new ConcurrentHashMap<>();

    @Override
    public Votacao salvar(Votacao votacao) {
        if (votacao == null) {
            throw new IllegalArgumentException("votacao nao pode ser nula");
        }
        votacoes.put(votacao.id(), votacao);
        return votacao;
    }

    @Override
    public Optional<Votacao> buscar(String id) {
        return id == null ? Optional.empty() : Optional.ofNullable(votacoes.get(id));
    }

    @Override
    public List<Votacao> listar() {
        return votacoes.values().stream()
                .sorted(Comparator.comparing(Votacao::criadaEm).reversed())
                .toList();
    }
}

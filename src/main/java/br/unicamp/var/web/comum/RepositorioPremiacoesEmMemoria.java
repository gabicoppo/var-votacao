package br.unicamp.var.web.comum;

import br.unicamp.var.top3.PremiacaoTop3;
import java.time.Clock;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

/**
 * {@link RepositorioPremiacoes} em memória e thread-safe.
 */
@Repository
public class RepositorioPremiacoesEmMemoria implements RepositorioPremiacoes {

    private final Map<String, PremiacaoCadastrada> premiacoes = new ConcurrentHashMap<>();
    private final Clock relogio;

    /**
     * Cria o repositório vazio.
     *
     * @param relogio relógio usado para datar os cadastros
     */
    public RepositorioPremiacoesEmMemoria(Clock relogio) {
        this.relogio = relogio;
    }

    @Override
    public PremiacaoCadastrada salvar(PremiacaoTop3 premiacao) {
        if (premiacao == null) {
            throw new IllegalArgumentException("premiacao nao pode ser nula");
        }
        PremiacaoCadastrada cadastro = new PremiacaoCadastrada(UUID.randomUUID().toString(), premiacao,
                relogio.instant());
        premiacoes.put(cadastro.id(), cadastro);
        return cadastro;
    }

    @Override
    public Optional<PremiacaoCadastrada> buscar(String id) {
        return id == null ? Optional.empty() : Optional.ofNullable(premiacoes.get(id));
    }

    @Override
    public List<PremiacaoCadastrada> listar() {
        return premiacoes.values().stream()
                .sorted(Comparator.comparing(PremiacaoCadastrada::criadaEm).reversed())
                .toList();
    }
}

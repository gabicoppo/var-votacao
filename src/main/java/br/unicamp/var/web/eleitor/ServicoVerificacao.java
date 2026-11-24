package br.unicamp.var.web.eleitor;

import br.unicamp.var.processo.Votacao;
import br.unicamp.var.web.comum.RepositorioVotacoes;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Conferência pública de comprovantes (história #28): diz só se o código foi emitido pela votação,
 * nunca qual foi o voto (RNF-01).
 */
@Service
public class ServicoVerificacao {

    private final RepositorioVotacoes votacoes;

    /**
     * Cria o serviço.
     *
     * @param votacoes repositório de votações
     */
    public ServicoVerificacao(RepositorioVotacoes votacoes) {
        this.votacoes = votacoes;
    }

    /**
     * Monta a página, conferindo o código se a votação e o código vierem preenchidos.
     *
     * @param votacaoId votação escolhida, ou {@code null}
     * @param codigo    código digitado, ou {@code null}
     * @return a página
     */
    public PaginaVerificacao verificar(String votacaoId, String codigo) {
        String id = limpo(votacaoId);
        String cod = limpo(codigo);
        List<PaginaVerificacao.OpcaoVotacao> opcoes = new ArrayList<>();
        for (Votacao votacao : votacoes.listar()) {
            synchronized (votacao) {
                if (!"rascunho".equals(votacao.status())) {
                    opcoes.add(new PaginaVerificacao.OpcaoVotacao(votacao.id(), votacao.pergunta()));
                }
            }
        }
        if (id.isEmpty() || cod.isEmpty()) {
            return new PaginaVerificacao(opcoes, id, cod, null, null);
        }
        Optional<Votacao> encontrada = votacoes.buscar(id);
        if (encontrada.isEmpty()) {
            return new PaginaVerificacao(opcoes, id, cod, PaginaVerificacao.Resultado.VOTACAO_INEXISTENTE, null);
        }
        Votacao votacao = encontrada.get();
        synchronized (votacao) {
            PaginaVerificacao.Resultado resultado = votacao.comprovanteValido(cod)
                    ? PaginaVerificacao.Resultado.VALIDO : PaginaVerificacao.Resultado.INVALIDO;
            return new PaginaVerificacao(opcoes, id, cod, resultado, votacao.pergunta());
        }
    }

    private static String limpo(String texto) {
        return texto == null ? "" : texto.strip();
    }
}

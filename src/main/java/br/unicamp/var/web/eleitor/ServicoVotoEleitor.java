package br.unicamp.var.web.eleitor;

import br.unicamp.var.processo.PeriodoVotacao;
import br.unicamp.var.processo.Votacao;
import br.unicamp.var.urna.Comprovante;
import br.unicamp.var.urna.Sigilo;
import br.unicamp.var.web.comum.Fuso;
import br.unicamp.var.web.comum.RepositorioVotacoes;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Voto do eleitor numa votação (histórias #26 e #28): monta a cédula e registra o voto. Toda leitura
 * e escrita da {@link Votacao} acontece dentro de {@code synchronized (votacao)}.
 */
@Service
public class ServicoVotoEleitor {

    private final RepositorioVotacoes votacoes;
    private final Clock relogio;

    /**
     * Cria o serviço.
     *
     * @param votacoes repositório de votações
     * @param relogio  relógio da aplicação
     */
    public ServicoVotoEleitor(RepositorioVotacoes votacoes, Clock relogio) {
        this.votacoes = votacoes;
        this.relogio = relogio;
    }

    /**
     * Cédula da votação para o eleitor.
     *
     * @param votacaoId id da votação
     * @param eleitorId id do eleitor logado
     * @return a cédula, com a situação (pode votar, já votou ou fechada)
     * @throws NaoEncontradoException  se a votação não existir
     * @throws EleitorNaoAptoException se o eleitor não for apto (RNF-05)
     */
    public CedulaVotacao cedula(String votacaoId, String eleitorId) {
        Votacao votacao = buscar(votacaoId);
        synchronized (votacao) {
            exigirApto(votacao, eleitorId);
            Instant agora = relogio.instant();
            Optional<PeriodoVotacao> periodo = votacao.periodo();
            return new CedulaVotacao(votacao.id(), votacao.pergunta(), votacao.descricao(), votacao.opcoes(),
                    votacao.sigilo() == Sigilo.SECRETO,
                    periodo.map(p -> TempoRestante.descrever(agora, p.fim(), Fuso.ZONA)).orElse("sem prazo definido"),
                    periodo.map(PeriodoVotacao::inicio).orElse(null), periodo.map(PeriodoVotacao::fim).orElse(null),
                    situacao(votacao, eleitorId, agora));
        }
    }

    /**
     * Registra o voto (RNF-02: um por eleitor).
     *
     * @param votacaoId id da votação
     * @param eleitorId id do eleitor logado
     * @param opcao     opção escolhida
     * @return o comprovante, sem a opção
     * @throws NaoEncontradoException   se a votação não existir
     * @throws EleitorNaoAptoException  se o eleitor não for apto
     * @throws IllegalArgumentException se a opção não existir
     * @throws IllegalStateException    se a votação não estiver aberta, estiver fora do período ou o
     *                                  eleitor já tiver votado
     */
    public ComprovanteVoto votar(String votacaoId, String eleitorId, String opcao) {
        Votacao votacao = buscar(votacaoId);
        synchronized (votacao) {
            exigirApto(votacao, eleitorId);
            if (opcao == null || !votacao.opcoes().contains(opcao)) {
                throw new IllegalArgumentException("opcao inexistente: " + opcao);
            }
            Comprovante comprovante = votacao.registrarVoto(eleitorId, opcao);
            return new ComprovanteVoto(votacao.id(), votacao.pergunta(), comprovante.codigo(),
                    comprovante.registradoEm());
        }
    }

    /**
     * Diz se o eleitor já votou.
     *
     * @param votacaoId id da votação
     * @param eleitorId id do eleitor
     * @return {@code true} se há voto do eleitor
     * @throws NaoEncontradoException se a votação não existir
     */
    public boolean jaVotou(String votacaoId, String eleitorId) {
        Votacao votacao = buscar(votacaoId);
        synchronized (votacao) {
            return votacao.jaVotou(eleitorId);
        }
    }

    private Votacao buscar(String votacaoId) {
        return votacoes.buscar(votacaoId).orElseThrow(() -> new NaoEncontradoException("votacao inexistente"));
    }

    private static void exigirApto(Votacao votacao, String eleitorId) {
        if (eleitorId == null || !votacao.eleitorado().ehApto(eleitorId)) {
            throw new EleitorNaoAptoException(votacao.pergunta());
        }
    }

    private static SituacaoCedula situacao(Votacao votacao, String eleitorId, Instant agora) {
        if (votacao.jaVotou(eleitorId)) {
            return SituacaoCedula.JA_VOTOU;
        }
        boolean noPeriodo = votacao.periodo().map(p -> p.contem(agora)).orElse(true);
        return "aberta".equals(votacao.status()) && noPeriodo ? SituacaoCedula.PODE_VOTAR : SituacaoCedula.FECHADA;
    }
}

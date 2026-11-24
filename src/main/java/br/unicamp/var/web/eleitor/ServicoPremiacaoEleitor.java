package br.unicamp.var.web.eleitor;

import br.unicamp.var.top3.CategoriaEleitor;
import br.unicamp.var.top3.CedulaTop3;
import br.unicamp.var.top3.PremiacaoTop3;
import br.unicamp.var.top3.StatusPremiacao;
import br.unicamp.var.web.comum.Formatador;
import br.unicamp.var.web.comum.PremiacaoCadastrada;
import br.unicamp.var.web.comum.RepositorioPremiacoes;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Cédula Top 3 do eleitor (história #23). {@link PremiacaoTop3} já é {@code synchronized}.
 */
@Service
public class ServicoPremiacaoEleitor {

    private final RepositorioPremiacoes premiacoes;
    private final Formatador fmt;

    /**
     * Cria o serviço.
     *
     * @param premiacoes repositório de premiações
     * @param fmt        formatador, para o peso do grupo
     */
    public ServicoPremiacaoEleitor(RepositorioPremiacoes premiacoes, Formatador fmt) {
        this.premiacoes = premiacoes;
        this.fmt = fmt;
    }

    /**
     * Cédula da premiação para o eleitor.
     *
     * @param premiacaoId id do cadastro
     * @param eleitorId   id do eleitor logado
     * @return a cédula, com a situação (pode votar, já votou ou fechada)
     * @throws NaoEncontradoException  se a premiação não existir
     * @throws EleitorNaoAptoException se o eleitor não tiver categoria na premiação
     */
    public CedulaPremiacao cedula(String premiacaoId, String eleitorId) {
        PremiacaoTop3 premiacao = buscar(premiacaoId).premiacao();
        CategoriaEleitor categoria = categoria(premiacao, eleitorId);
        SituacaoCedula situacao;
        if (premiacao.jaVotou(eleitorId)) {
            situacao = SituacaoCedula.JA_VOTOU;
        } else if (premiacao.status() == StatusPremiacao.ABERTA) {
            situacao = SituacaoCedula.PODE_VOTAR;
        } else {
            situacao = SituacaoCedula.FECHADA;
        }
        return new CedulaPremiacao(premiacaoId, premiacao.titulo(), premiacao.descricao(), premiacao.candidatos(),
                categoria.nome(), fmt.numero(categoria.peso()), situacao);
    }

    /**
     * Confere as três escolhas antes da revisão.
     *
     * @param cedula   cédula exibida
     * @param primeiro candidato em 1º lugar
     * @param segundo  candidato em 2º lugar
     * @param terceiro candidato em 3º lugar
     * @return a mensagem de erro para o eleitor, ou vazio se as escolhas forem válidas
     */
    public Optional<String> validar(CedulaPremiacao cedula, String primeiro, String segundo, String terceiro) {
        if (vazio(primeiro) || vazio(segundo) || vazio(terceiro)) {
            return Optional.of("Escolha o 1º, o 2º e o 3º lugar.");
        }
        if (!cedula.candidatos().contains(primeiro) || !cedula.candidatos().contains(segundo)
                || !cedula.candidatos().contains(terceiro)) {
            return Optional.of("Escolha apenas candidatos da lista.");
        }
        if (primeiro.equals(segundo) || primeiro.equals(terceiro) || segundo.equals(terceiro)) {
            return Optional.of("Escolha três candidatos diferentes.");
        }
        return Optional.empty();
    }

    /**
     * Registra a cédula (uma por eleitor).
     *
     * @param premiacaoId id do cadastro
     * @param eleitorId   id do eleitor logado
     * @param primeiro    candidato em 1º lugar
     * @param segundo     candidato em 2º lugar
     * @param terceiro    candidato em 3º lugar
     * @throws NaoEncontradoException   se a premiação não existir
     * @throws EleitorNaoAptoException  se o eleitor não for apto
     * @throws IllegalArgumentException se a cédula for inválida
     * @throws IllegalStateException    se a premiação não estiver aberta ou o eleitor já tiver votado
     */
    public void registrar(String premiacaoId, String eleitorId, String primeiro, String segundo, String terceiro) {
        PremiacaoTop3 premiacao = buscar(premiacaoId).premiacao();
        categoria(premiacao, eleitorId);
        premiacao.registrarCedula(eleitorId, new CedulaTop3(primeiro, segundo, terceiro));
    }

    /**
     * Diz se o eleitor já entregou a cédula.
     *
     * @param premiacaoId id do cadastro
     * @param eleitorId   id do eleitor
     * @return {@code true} se já votou
     * @throws NaoEncontradoException se a premiação não existir
     */
    public boolean jaVotou(String premiacaoId, String eleitorId) {
        return buscar(premiacaoId).premiacao().jaVotou(eleitorId);
    }

    private PremiacaoCadastrada buscar(String premiacaoId) {
        return premiacoes.buscar(premiacaoId).orElseThrow(() -> new NaoEncontradoException("premiacao inexistente"));
    }

    private static CategoriaEleitor categoria(PremiacaoTop3 premiacao, String eleitorId) {
        return premiacao.categoriaDe(eleitorId).orElseThrow(() -> new EleitorNaoAptoException(premiacao.titulo()));
    }

    private static boolean vazio(String texto) {
        return texto == null || texto.isBlank();
    }
}

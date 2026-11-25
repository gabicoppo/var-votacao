package br.unicamp.var.web.organizador;

import br.unicamp.var.apuracao.RegraApuracao;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Regra de apuração como o organizador a digitou (tipo, 2º turno, quórum e fração em texto), para
 * preencher de novo os formulários de "Alterar regra" e "Duplicar" (histórias #19 e #21).
 *
 * <p>O domínio não expõe os parâmetros das regras ({@code MaioriaAbsoluta} e
 * {@code MaioriaQualificada} só têm {@code descricao()}). Por isso o serviço guarda a configuração
 * de cada votação criada pela web; para as outras (dados de demonstração e 2º turno), ela é
 * deduzida da descrição, que mostra os percentuais com uma casa decimal.</p>
 *
 * @param tipo          tipo da regra
 * @param segundoTurno  se a maioria absoluta tem 2º turno
 * @param quorum        quórum mínimo, como digitado
 * @param fracaoExigida fração exigida, como digitada
 */
record ConfiguracaoRegra(TipoRegra tipo, boolean segundoTurno, String quorum, String fracaoExigida) {

    private static final Pattern PERCENTUAIS = Pattern.compile("quórum de ([\\d,]+%).* pelo menos ([\\d,]+%)");
    private static final double TOLERANCIA_DOIS_TERCOS = 0.0005;

    /**
     * Lê a configuração do formulário.
     *
     * @param f formulário com a regra válida
     * @return a configuração
     */
    static ConfiguracaoRegra de(FormularioVotacao f) {
        return new ConfiguracaoRegra(f.getTipoRegra(), f.isSegundoTurno(), f.getQuorum(), f.getFracaoExigida());
    }

    /**
     * Deduz a configuração a partir da descrição da regra.
     *
     * @param regra regra da votação
     * @return a configuração equivalente; regra desconhecida vira maioria simples
     */
    static ConfiguracaoRegra deduzir(RegraApuracao regra) {
        String descricao = regra.descricao();
        FormularioVotacao padrao = new FormularioVotacao();
        if (descricao.startsWith("Maioria absoluta")) {
            return new ConfiguracaoRegra(TipoRegra.ABSOLUTA, !descricao.contains("sem 2º turno"),
                    padrao.getQuorum(), padrao.getFracaoExigida());
        }
        Matcher percentuais = PERCENTUAIS.matcher(descricao);
        if (descricao.startsWith("Maioria qualificada") && percentuais.find()) {
            return new ConfiguracaoRegra(TipoRegra.QUALIFICADA, true,
                    comoFracao(percentuais.group(1)), comoFracao(percentuais.group(2)));
        }
        return new ConfiguracaoRegra(TipoRegra.SIMPLES, true, padrao.getQuorum(), padrao.getFracaoExigida());
    }

    /** "66,7%" volta a ser "2/3", para não mudar o limite exato da regra; os outros ficam em percentual. */
    private static String comoFracao(String percentual) {
        double valor = ConversorFormularioVotacao.lerFracao(percentual);
        if (Math.abs(valor - 2.0 / 3) < TOLERANCIA_DOIS_TERCOS) {
            return "2/3";
        }
        if (Math.abs(valor - 1.0 / 3) < TOLERANCIA_DOIS_TERCOS) {
            return "1/3";
        }
        return percentual.replace("%", "").strip() + "%";
    }

    /**
     * Copia a configuração para um formulário.
     *
     * @param f formulário a preencher
     */
    void aplicarEm(FormularioVotacao f) {
        f.setTipoRegra(tipo);
        f.setSegundoTurno(segundoTurno);
        f.setQuorum(quorum);
        f.setFracaoExigida(fracaoExigida);
    }
}

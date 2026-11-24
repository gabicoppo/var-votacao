package br.unicamp.var.web.organizador;

import br.unicamp.var.auditoria.TipoEvento;
import br.unicamp.var.urna.Sigilo;

/**
 * Textos em português, para a tela, de enums e detalhes do domínio.
 */
final class RotulosVotacao {

    private static final String PREFIXO_TURNO = "2º turno da votacao ";
    private static final int TAMANHO_ID_CURTO = 8;

    private RotulosVotacao() {
    }

    /**
     * Rótulo de um evento da auditoria (RNF-07).
     *
     * @param tipo tipo do evento
     * @return o rótulo em português
     */
    static String evento(TipoEvento tipo) {
        return switch (tipo) {
            case CRIADA -> "Votação criada";
            case REGRA_ALTERADA -> "Regra alterada";
            case ABERTA -> "Votação aberta";
            case ENCERRADA -> "Votação encerrada";
            case APURADA -> "Votação apurada";
            case SEGUNDO_TURNO_CRIADO -> "2º turno criado";
        };
    }

    /**
     * Detalhe de um evento, com acentos e ids encurtados.
     *
     * @param tipo    tipo do evento
     * @param detalhe detalhe registrado pelo domínio
     * @return o detalhe para a tela; vazio se não houver
     */
    static String detalhe(TipoEvento tipo, String detalhe) {
        if ("inicio do periodo".equals(detalhe)) {
            return "Automático: início do período";
        }
        if ("fim do periodo".equals(detalhe)) {
            return "Automático: fim do período";
        }
        if (detalhe.startsWith(PREFIXO_TURNO)) {
            return "2º turno da votação " + idCurto(detalhe.substring(PREFIXO_TURNO.length()));
        }
        if (tipo == TipoEvento.SEGUNDO_TURNO_CRIADO) {
            return "Nova votação " + idCurto(detalhe);
        }
        return detalhe;
    }

    /**
     * Rótulo do sigilo (história #22).
     *
     * @param sigilo sigilo da votação
     * @return "Secreto" ou "Identificado (voto nominal)"
     */
    static String sigilo(Sigilo sigilo) {
        return sigilo == Sigilo.SECRETO ? "Secreto" : "Identificado (voto nominal)";
    }

    /**
     * Nome curto da regra, para os cards: o trecho da descrição antes de ":".
     *
     * @param descricao descrição completa da regra
     * @return por exemplo, "Maioria absoluta"
     */
    static String regraResumida(String descricao) {
        int doisPontos = descricao.indexOf(':');
        return doisPontos < 0 ? descricao : descricao.substring(0, doisPontos);
    }

    /**
     * Encurta um UUID para exibição.
     *
     * @param id id completo
     * @return os 8 primeiros caracteres
     */
    static String idCurto(String id) {
        return id.length() <= TAMANHO_ID_CURTO ? id : id.substring(0, TAMANHO_ID_CURTO);
    }
}

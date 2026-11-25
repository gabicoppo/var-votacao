package br.unicamp.var.web.eleitor;

/**
 * Card de uma votação ou premiação no painel do eleitor, já com tudo calculado para a tela
 * (história #26).
 *
 * @param id             id da votação ou da premiação
 * @param pergunta       pergunta (ou título da premiação)
 * @param descricaoCurta descrição resumida em palavras inteiras, com no máximo
 *                       {@value PainelEleitor#TAMANHO_DESCRICAO} caracteres
 * @param status         status do domínio em minúsculas ("aberta", ...), para a classe do badge
 * @param textoStatus    status para exibição ("Aberta", ...)
 * @param prazo          prazo em texto ("encerra hoje às 18:00", ...)
 * @param modalidade     como o voto funciona: "Voto secreto" ou "Voto identificado" nas votações (#22) e
 *                       "Top 3 · peso 2" nas premiações, com o peso do grupo do eleitor (#23)
 * @param podeVotar      se o eleitor pode votar agora
 * @param jaVotou        se o eleitor já votou
 * @param link           destino do card, ou {@code null} se não houver ação
 * @param top3           se é uma premiação Top 3 (etiqueta "Top 3")
 * @param encerraHoje    se é a votação de prazo mais curto entre as que esperam o voto e encerra hoje
 *                       (selo "⏰ Encerra hoje")
 */
public record CartaoVotacao(String id, String pergunta, String descricaoCurta, String status, String textoStatus,
                            String prazo, String modalidade, boolean podeVotar, boolean jaVotou, String link,
                            boolean top3, boolean encerraHoje) {

    /**
     * Cópia deste card com o selo "⏰ Encerra hoje".
     *
     * @return o mesmo card, com {@code encerraHoje} verdadeiro
     */
    CartaoVotacao comSeloEncerraHoje() {
        return new CartaoVotacao(id, pergunta, descricaoCurta, status, textoStatus, prazo, modalidade, podeVotar,
                jaVotou, link, top3, true);
    }
}

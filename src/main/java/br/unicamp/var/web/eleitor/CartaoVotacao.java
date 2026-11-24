package br.unicamp.var.web.eleitor;

/**
 * Card de uma votação ou premiação no painel do eleitor, já com tudo calculado para a tela
 * (história #26).
 *
 * @param id             id da votação ou da premiação
 * @param pergunta       pergunta (ou título da premiação)
 * @param descricaoCurta descrição resumida, com no máximo {@value PainelEleitor#TAMANHO_DESCRICAO} caracteres
 * @param status         status do domínio em minúsculas ("aberta", ...), para a classe do badge
 * @param textoStatus    status para exibição ("Aberta", ...)
 * @param prazo          prazo em texto ("encerra hoje às 18:00", ...)
 * @param sigilo         "Voto secreto" ou "Voto identificado"
 * @param podeVotar      se o eleitor pode votar agora
 * @param jaVotou        se o eleitor já votou
 * @param link           destino do card, ou {@code null} se não houver ação
 * @param top3           se é uma premiação Top 3 (etiqueta "Top 3")
 */
public record CartaoVotacao(String id, String pergunta, String descricaoCurta, String status, String textoStatus,
                            String prazo, String sigilo, boolean podeVotar, boolean jaVotou, String link,
                            boolean top3) {
}

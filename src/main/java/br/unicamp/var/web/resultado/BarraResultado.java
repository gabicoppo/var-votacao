package br.unicamp.var.web.resultado;

/**
 * Uma barra do gráfico de resultado (história #29), já pronta para a tela.
 *
 * @param opcao           nome da opção
 * @param votos           votos absolutos
 * @param percentualTexto percentual sobre os votos válidos, ex.: {@code "66,7%"}
 * @param largura         largura da barra, de 0 a 100; a opção mais votada ocupa 100
 * @param destaque        vencedora, finalista do 2º turno ou nenhum
 * @param colocacao       colocação por votos; opções empatadas dividem a colocação (1, 2, 2, 4)
 */
public record BarraResultado(String opcao, int votos, String percentualTexto, double largura, Destaque destaque,
                             int colocacao) {

    /**
     * Rótulo dos votos, no singular ou no plural.
     *
     * @return ex.: {@code "1 voto"}, {@code "7 votos"}
     */
    public String votosTexto() {
        return GraficoResultado.votos(votos);
    }
}

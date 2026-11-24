package br.unicamp.var.top3;

import java.util.List;

/**
 * Escolhas de um eleitor em uma categoria de premiação (#23).
 *
 * @param primeiro candidato em 1º lugar
 * @param segundo candidato em 2º lugar
 * @param terceiro candidato em 3º lugar
 */
public record CedulaTop3(String primeiro, String segundo, String terceiro) {

    /** Exige três nomes preenchidos e distintos na mesma cédula (#23). */
    public CedulaTop3 {
        ValidacaoTop3.nome(primeiro, "primeiro candidato");
        ValidacaoTop3.nome(segundo, "segundo candidato");
        ValidacaoTop3.nome(terceiro, "terceiro candidato");
        if (primeiro.equals(segundo) || primeiro.equals(terceiro) || segundo.equals(terceiro)) {
            throw new IllegalArgumentException("candidato repetido na cedula");
        }
    }

    /**
     * Converte as escolhas em votos para a apuração (#23, #30).
     *
     * @param peso peso do grupo ao qual o eleitor pertence
     * @return três votos imutáveis na ordem da cédula
     */
    public List<VotoCategoria> comPeso(double peso) {
        ValidacaoTop3.validarPeso(peso);
        return List.of(new VotoCategoria(primeiro, Posicao.PRIMEIRO, peso),
                new VotoCategoria(segundo, Posicao.SEGUNDO, peso),
                new VotoCategoria(terceiro, Posicao.TERCEIRO, peso));
    }
}

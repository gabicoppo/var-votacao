package br.unicamp.var.eleitorado;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Lista imutável de eleitores aptos de uma votação (história #24).
 *
 * <p>É a base do RNF-02 (integridade): só quem está nesta lista pode votar, e o
 * total de aptos é o denominador do quórum. Os ids são únicos depois de
 * normalizados com {@link String#strip()}; a comparação diferencia maiúsculas de
 * minúsculas. Depois de criado, o eleitorado não muda: alterar a lista exige
 * criar outro (RNF-07, a lista não muda com a votação aberta).</p>
 */
public final class Eleitorado {

    private static final String SEPARADOR = ";";

    private final Map<String, Eleitor> porId;
    private final List<Eleitor> eleitores;

    private Eleitorado(Map<String, Eleitor> porId) {
        if (porId.isEmpty()) {
            throw new IllegalArgumentException("deve haver ao menos um eleitor");
        }
        this.porId = Collections.unmodifiableMap(new LinkedHashMap<>(porId));
        this.eleitores = List.copyOf(porId.values());
    }

    /**
     * Cria o eleitorado a partir de uma lista de eleitores (#24).
     *
     * @param eleitores ao menos um eleitor, sem ids repetidos
     * @return o eleitorado, na ordem da lista recebida
     * @throws IllegalArgumentException se a lista for nula ou vazia, tiver
     *                                  elemento nulo ou ids repetidos
     */
    public static Eleitorado de(List<Eleitor> eleitores) {
        if (eleitores == null) {
            throw new IllegalArgumentException("lista de eleitores nao pode ser nula");
        }
        Map<String, Eleitor> porId = new LinkedHashMap<>();
        for (Eleitor eleitor : eleitores) {
            if (eleitor == null) {
                throw new IllegalArgumentException("eleitor nao pode ser nulo");
            }
            adicionar(porId, eleitor);
        }
        return new Eleitorado(porId);
    }

    /**
     * Importa a lista colada pelo organizador (#24): uma linha por eleitor, no
     * formato {@code id;nome}.
     *
     * <p>Linhas em branco são ignoradas e os campos perdem os espaços nas pontas.
     * A numeração das linhas nas mensagens de erro conta todas as linhas do
     * texto, inclusive as em branco, para bater com o que o organizador vê.</p>
     *
     * @param texto conteúdo colado, com linhas separadas por qualquer quebra de linha
     * @return o eleitorado, na ordem das linhas
     * @throws IllegalArgumentException se o texto for nulo, não tiver nenhum
     *                                  eleitor ou tiver uma linha mal formada (sem
     *                                  exatamente um {@code ;}, com id ou nome vazio
     *                                  ou id repetido); a mensagem informa o número
     *                                  da linha
     */
    public static Eleitorado importar(String texto) {
        if (texto == null) {
            throw new IllegalArgumentException("texto da lista de eleitores nao pode ser nulo");
        }
        Map<String, Eleitor> porId = new LinkedHashMap<>();
        String[] linhas = texto.split("\\R", -1);
        for (int i = 0; i < linhas.length; i++) {
            if (linhas[i].isBlank()) {
                continue;
            }
            try {
                adicionar(porId, lerLinha(linhas[i]));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("linha " + (i + 1) + ": " + e.getMessage(), e);
            }
        }
        return new Eleitorado(porId);
    }

    private static Eleitor lerLinha(String linha) {
        String[] campos = linha.split(SEPARADOR, -1);
        if (campos.length != 2) {
            throw new IllegalArgumentException("formato esperado id;nome");
        }
        return new Eleitor(campos[0], campos[1]);
    }

    private static void adicionar(Map<String, Eleitor> porId, Eleitor eleitor) {
        if (porId.putIfAbsent(eleitor.id(), eleitor) != null) {
            throw new IllegalArgumentException("id de eleitor repetido: " + eleitor.id());
        }
    }

    /**
     * Número de eleitores aptos, usado no quórum e como limite de votos (RNF-02).
     *
     * @return total de eleitores, sempre maior que zero
     */
    public int total() {
        return eleitores.size();
    }

    /**
     * Diz se o eleitor está na lista de aptos (RNF-02).
     *
     * @param eleitorId id do eleitor; espaços nas pontas são ignorados
     * @return {@code true} se o id estiver na lista; {@code false} se for nulo ou desconhecido
     */
    public boolean ehApto(String eleitorId) {
        return eleitorId != null && porId.containsKey(eleitorId.strip());
    }

    /**
     * Busca um eleitor apto pelo id.
     *
     * @param eleitorId id do eleitor; espaços nas pontas são ignorados
     * @return o eleitor, ou vazio se o id for nulo ou desconhecido
     */
    public Optional<Eleitor> buscar(String eleitorId) {
        if (eleitorId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(porId.get(eleitorId.strip()));
    }

    /**
     * Lista os eleitores aptos na ordem em que foram informados.
     *
     * @return lista imutável
     */
    public List<Eleitor> listar() {
        return List.copyOf(eleitores);
    }
}

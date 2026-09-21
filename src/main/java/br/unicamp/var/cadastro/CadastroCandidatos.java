package br.unicamp.var.cadastro;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Cadastro em memoria de candidatos, identificados de forma unica pelo codigo.
 *
 * <p>Nomes repetidos sao permitidos (homonimos); codigos repetidos nao. A comparacao
 * de codigos e exata, diferenciando maiusculas de minusculas. Nao e thread-safe.
 */
public final class CadastroCandidatos {

    private final Map<String, Candidato> candidatos = new LinkedHashMap<>();

    /**
     * Cadastra um novo candidato.
     *
     * @param nome   nome do candidato
     * @param codigo codigo unico do candidato
     * @return o candidato cadastrado (com nome e codigo sem espacos nas pontas)
     * @throws IllegalArgumentException se nome ou codigo forem invalidos ou se o codigo ja existir
     */
    public Candidato cadastrar(String nome, String codigo) {
        // Cria (e valida) primeiro: se falhar, o cadastro nao e alterado.
        Candidato candidato = new Candidato(nome, codigo);
        if (candidatos.containsKey(candidato.codigo())) {
            throw new IllegalArgumentException("Ja existe candidato com o codigo " + candidato.codigo());
        }
        candidatos.put(candidato.codigo(), candidato);
        return candidato;
    }

    /**
     * Busca um candidato pelo codigo, ignorando espacos nas pontas.
     *
     * @param codigo codigo procurado (pode ser nulo)
     * @return o candidato, ou vazio se o codigo for nulo ou nao existir
     */
    public Optional<Candidato> buscarPorCodigo(String codigo) {
        if (codigo == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(candidatos.get(codigo.strip()));
    }

    /**
     * Lista os candidatos na ordem de cadastro.
     *
     * @return copia imutavel da lista; alteracoes no cadastro depois da chamada nao a afetam
     */
    public List<Candidato> listar() {
        return List.copyOf(candidatos.values());
    }
}
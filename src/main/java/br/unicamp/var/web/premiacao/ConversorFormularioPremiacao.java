package br.unicamp.var.web.premiacao;

import br.unicamp.var.eleitorado.Eleitor;
import br.unicamp.var.top3.CategoriaEleitor;
import br.unicamp.var.top3.PremiacaoTop3;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Valida o {@link FormularioPremiacao} e o converte numa {@link PremiacaoTop3} (história #23).
 *
 * <p>{@link #validar} nunca lança exceção: devolve no máximo um {@link ErroCampo} por campo, com a
 * frase em português e o número da linha. As linhas são contadas como o organizador as vê,
 * incluindo as em branco (que são ignoradas). Regras:</p>
 * <ul>
 *   <li>título preenchido;</li>
 *   <li>pelo menos três candidatos, sem repetir (sem diferenciar maiúsculas de minúsculas);</li>
 *   <li>pelo menos um grupo {@code Nome;peso}, com nome único e peso maior que zero; aceita vírgula ou
 *       ponto decimal ({@code 2}, {@code 1,5}, {@code 1.5});</li>
 *   <li>pelo menos um eleitor {@code id;nome;grupo}, com id único e grupo cadastrado (o nome do grupo
 *       pode vir com outra caixa: {@code jornalistas} encontra {@code Jornalistas}).</li>
 * </ul>
 */
public final class ConversorFormularioPremiacao {

    private static final int MINIMO_CANDIDATOS = 3;
    private static final Pattern PESO = Pattern.compile("\\d{1,6}(?:[.,]\\d{1,6})?");
    private static final Pattern QUEBRA_LINHA = Pattern.compile("\\R");
    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

    private ConversorFormularioPremiacao() {
    }

    /**
     * Confere o formulário inteiro.
     *
     * @param f formulário preenchido
     * @return os erros, na ordem dos campos; vazia se o formulário é válido
     */
    public static List<ErroCampo> validar(FormularioPremiacao f) {
        return ler(f).erros();
    }

    /**
     * Converte o formulário válido na premiação, em rascunho.
     *
     * @param f formulário válido
     * @return a premiação
     * @throws IllegalArgumentException se o formulário tiver erros
     */
    public static PremiacaoTop3 paraPremiacao(FormularioPremiacao f) {
        Leitura leitura = lerValido(f);
        Map<String, String> grupoPorEleitor = new LinkedHashMap<>();
        leitura.eleitores().forEach(e -> grupoPorEleitor.put(e.id(), e.grupo()));
        return new PremiacaoTop3(f.getTitulo().strip(), f.getDescricao().strip(), leitura.candidatos(),
                leitura.categorias(), grupoPorEleitor);
    }

    /**
     * Eleitores do formulário válido, para criar as contas de acesso.
     *
     * @param f formulário válido
     * @return os eleitores, na ordem digitada
     * @throws IllegalArgumentException se o formulário tiver erros
     */
    public static List<Eleitor> eleitores(FormularioPremiacao f) {
        return lerValido(f).eleitores().stream().map(e -> new Eleitor(e.id(), e.nome())).toList();
    }

    private static Leitura lerValido(FormularioPremiacao f) {
        Leitura leitura = ler(f);
        if (!leitura.erros().isEmpty()) {
            throw new IllegalArgumentException("formulario de premiacao invalido");
        }
        return leitura;
    }

    private record LinhaEleitor(String id, String nome, String grupo) {
    }

    private record Leitura(List<String> candidatos, List<CategoriaEleitor> categorias, List<LinhaEleitor> eleitores,
                           List<ErroCampo> erros) {
    }

    private static Leitura ler(FormularioPremiacao f) {
        List<ErroCampo> erros = new ArrayList<>();
        if (f.getTitulo().isBlank()) {
            erros.add(new ErroCampo("titulo", "Informe o título da premiação."));
        }
        List<String> candidatos = lerCandidatos(f.getCandidatos(), erros);
        int errosAntes = erros.size();
        List<CategoriaEleitor> categorias = lerCategorias(f.getCategorias(), erros);
        boolean categoriasValidas = erros.size() == errosAntes;
        List<LinhaEleitor> eleitores = lerEleitores(f.getEleitores(), categorias, categoriasValidas, erros);
        return new Leitura(candidatos, categorias, eleitores, List.copyOf(erros));
    }

    private static List<String> lerCandidatos(String texto, List<ErroCampo> erros) {
        List<String> candidatos = new ArrayList<>();
        Map<String, Integer> vistos = new HashMap<>();
        String[] linhas = QUEBRA_LINHA.split(texto, -1);
        for (int i = 0; i < linhas.length; i++) {
            String nome = linhas[i].strip();
            if (nome.isEmpty()) {
                continue;
            }
            if (vistos.putIfAbsent(chave(nome), i + 1) != null) {
                erros.add(new ErroCampo("candidatos", "Linha " + (i + 1) + ": o candidato \"" + nome
                        + "\" já apareceu na linha " + vistos.get(chave(nome)) + "."));
                return candidatos;
            }
            candidatos.add(nome);
        }
        if (candidatos.size() < MINIMO_CANDIDATOS) {
            erros.add(new ErroCampo("candidatos", "Informe pelo menos três candidatos, um por linha."));
        }
        return candidatos;
    }

    private static List<CategoriaEleitor> lerCategorias(String texto, List<ErroCampo> erros) {
        List<CategoriaEleitor> categorias = new ArrayList<>();
        Map<String, Integer> vistas = new HashMap<>();
        String[] linhas = QUEBRA_LINHA.split(texto, -1);
        for (int i = 0; i < linhas.length; i++) {
            if (linhas[i].isBlank()) {
                continue;
            }
            String erro = erroCategoria(linhas[i], i + 1, vistas);
            if (erro != null) {
                erros.add(new ErroCampo("categorias", erro));
                return categorias;
            }
            String[] partes = linhas[i].split(";", -1);
            categorias.add(new CategoriaEleitor(partes[0].strip(), peso(partes[1])));
        }
        if (categorias.isEmpty()) {
            erros.add(new ErroCampo("categorias",
                    "Informe pelo menos um grupo de votantes, no formato Nome;peso (ex.: Torcedores;1)."));
        }
        return categorias;
    }

    private static String erroCategoria(String linha, int numero, Map<String, Integer> vistas) {
        String[] partes = linha.split(";", -1);
        String prefixo = "Linha " + numero + ": ";
        if (partes.length != 2) {
            return prefixo + "use o formato Nome;peso, com um só ponto e vírgula (ex.: Jornalistas;2).";
        }
        String nome = partes[0].strip();
        String peso = partes[1].strip();
        if (nome.isEmpty()) {
            return prefixo + "informe o nome do grupo antes do ponto e vírgula.";
        }
        if (!PESO.matcher(peso).matches()) {
            return prefixo + "o peso \"" + peso + "\" não é um número. Use, por exemplo, 2 ou 1,5.";
        }
        if (peso(peso) <= 0) {
            return prefixo + "o peso deve ser maior que zero, para o voto do grupo contar.";
        }
        Integer anterior = vistas.putIfAbsent(chave(nome), numero);
        if (anterior != null) {
            return prefixo + "o grupo \"" + nome + "\" já apareceu na linha " + anterior + ".";
        }
        return null;
    }

    private static List<LinhaEleitor> lerEleitores(String texto, List<CategoriaEleitor> categorias,
                                                   boolean conferirGrupo, List<ErroCampo> erros) {
        Map<String, String> grupoPorChave = new HashMap<>();
        categorias.forEach(c -> grupoPorChave.put(chave(c.nome()), c.nome()));
        String grupos = categorias.stream().map(CategoriaEleitor::nome).collect(Collectors.joining(", "));
        List<LinhaEleitor> eleitores = new ArrayList<>();
        Map<String, Integer> vistos = new HashMap<>();
        String[] linhas = QUEBRA_LINHA.split(texto, -1);
        for (int i = 0; i < linhas.length; i++) {
            if (linhas[i].isBlank()) {
                continue;
            }
            String prefixo = "Linha " + (i + 1) + ": ";
            String[] partes = linhas[i].split(";", -1);
            String erro = erroEleitor(partes, prefixo, vistos, i + 1);
            if (erro == null && conferirGrupo && !grupoPorChave.containsKey(chave(partes[2].strip()))) {
                erro = prefixo + "o grupo \"" + partes[2].strip() + "\" não existe. Grupos cadastrados: "
                        + grupos + ".";
            }
            if (erro != null) {
                erros.add(new ErroCampo("eleitores", erro));
                return eleitores;
            }
            eleitores.add(new LinhaEleitor(partes[0].strip(), partes[1].strip(),
                    grupoPorChave.getOrDefault(chave(partes[2].strip()), partes[2].strip())));
        }
        if (eleitores.isEmpty()) {
            erros.add(new ErroCampo("eleitores",
                    "Informe pelo menos um eleitor, no formato id;nome;grupo (ex.: e01;Ana Ribeiro;Torcedores)."));
        }
        return eleitores;
    }

    private static String erroEleitor(String[] partes, String prefixo, Map<String, Integer> vistos, int numero) {
        if (partes.length != 3) {
            return prefixo
                    + "use o formato id;nome;grupo, com dois pontos e vírgulas (ex.: e01;Ana Ribeiro;Torcedores).";
        }
        String id = partes[0].strip();
        if (id.isEmpty()) {
            return prefixo + "informe o id do eleitor (o login dele).";
        }
        if (partes[1].isBlank()) {
            return prefixo + "informe o nome do eleitor.";
        }
        if (partes[2].isBlank()) {
            return prefixo + "informe o grupo do eleitor.";
        }
        Integer anterior = vistos.putIfAbsent(id, numero);
        if (anterior != null) {
            return prefixo + "o id \"" + id + "\" já apareceu na linha " + anterior + ".";
        }
        return null;
    }

    private static double peso(String texto) {
        return Double.parseDouble(texto.strip().replace(',', '.'));
    }

    private static String chave(String nome) {
        return nome.toLowerCase(PT_BR);
    }
}

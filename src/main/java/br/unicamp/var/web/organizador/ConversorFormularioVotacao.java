package br.unicamp.var.web.organizador;

import br.unicamp.var.apuracao.MaioriaAbsoluta;
import br.unicamp.var.apuracao.MaioriaQualificada;
import br.unicamp.var.apuracao.MaioriaSimples;
import br.unicamp.var.apuracao.RegraApuracao;
import br.unicamp.var.eleitorado.Eleitorado;
import br.unicamp.var.processo.PeriodoVotacao;
import br.unicamp.var.processo.Votacao;
import br.unicamp.var.web.comum.Fuso;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Valida o {@link FormularioVotacao} campo a campo e o converte numa {@link Votacao}
 * (histórias #19, #20, #21, #22 e #24).
 *
 * <p>{@link #validar} nunca lança exceção: devolve a lista de {@link ErroCampo} com frases em
 * português para a tela. {@link #paraVotacao} só deve ser chamado com o formulário válido.</p>
 */
public final class ConversorFormularioVotacao {

    private static final int MINIMO_OPCOES = 2;
    private static final double CEM_POR_CENTO = 100.0;
    private static final Pattern FRACAO = Pattern.compile("(\\d{1,9})\\s*/\\s*(\\d{1,9})");
    private static final Pattern PERCENTUAL = Pattern.compile("(\\d{1,9}(?:[.,]\\d{1,9})?)\\s*%?");
    private static final Pattern ERRO_LINHA = Pattern.compile("linha (\\d+): (.*)");
    private static final String FORMATO_FRACAO = "Use uma fração (2/3) ou um percentual (50%).";

    private ConversorFormularioVotacao() {
    }

    /**
     * Confere o formulário inteiro.
     *
     * @param f formulário preenchido
     * @return os erros encontrados, na ordem dos campos; vazia se o formulário é válido
     */
    public static List<ErroCampo> validar(FormularioVotacao f) {
        List<ErroCampo> erros = new ArrayList<>();
        if (f.getPergunta().isBlank()) {
            erros.add(new ErroCampo("pergunta", "Informe a pergunta da votação."));
        }
        validarOpcoes(f, erros);
        erros.addAll(validarRegra(f));
        if (f.getSigilo() == null) {
            erros.add(new ErroCampo("sigilo", "Escolha se o voto é secreto ou identificado."));
        }
        validarPeriodo(f, erros);
        validarEleitores(f, erros);
        return erros;
    }

    /**
     * Confere só os campos da regra (tipo, quórum e fração), para "Alterar regra" no rascunho.
     *
     * @param f formulário preenchido
     * @return os erros desses campos; vazia se a regra é válida
     */
    public static List<ErroCampo> validarRegra(FormularioVotacao f) {
        List<ErroCampo> erros = new ArrayList<>();
        if (f.getTipoRegra() == null) {
            erros.add(new ErroCampo("tipoRegra", "Escolha a regra de apuração."));
        } else if (f.getTipoRegra() == TipoRegra.QUALIFICADA) {
            validarFracao("quorum", "o quórum mínimo", f.getQuorum(), erros);
            validarFracao("fracaoExigida", "a fração exigida", f.getFracaoExigida(), erros);
        }
        return erros;
    }

    private static void validarOpcoes(FormularioVotacao f, List<ErroCampo> erros) {
        List<String> opcoes = linhas(f.getOpcoes());
        if (opcoes.size() < MINIMO_OPCOES) {
            erros.add(new ErroCampo("opcoes", "Informe pelo menos duas opções, uma por linha."));
            return;
        }
        Set<String> vistas = new HashSet<>();
        for (String opcao : opcoes) {
            if (!vistas.add(opcao)) {
                erros.add(new ErroCampo("opcoes", "A opção \"" + opcao + "\" aparece mais de uma vez."));
                return;
            }
        }
    }

    private static void validarFracao(String campo, String nome, String texto, List<ErroCampo> erros) {
        if (texto.isBlank()) {
            erros.add(new ErroCampo(campo, "Informe " + nome + "."));
            return;
        }
        double valor;
        try {
            valor = lerFracao(texto);
        } catch (IllegalArgumentException e) {
            erros.add(new ErroCampo(campo, FORMATO_FRACAO));
            return;
        }
        if (!(valor > 0 && valor <= 1)) {
            erros.add(new ErroCampo(campo, "Informe um valor maior que 0% e de no máximo 100%."));
        }
    }

    private static void validarPeriodo(FormularioVotacao f, List<ErroCampo> erros) {
        LocalDateTime inicio = f.getInicio();
        LocalDateTime fim = f.getFim();
        if (inicio == null && fim != null) {
            erros.add(new ErroCampo("inicio", "Informe também o início do período (ou apague o fim)."));
        } else if (inicio != null && fim == null) {
            erros.add(new ErroCampo("fim", "Informe também o fim do período (ou apague o início)."));
        } else if (inicio != null && !fim.isAfter(inicio)) {
            erros.add(new ErroCampo("fim", "O fim do período deve ser depois do início."));
        }
    }

    private static void validarEleitores(FormularioVotacao f, List<ErroCampo> erros) {
        if (f.getEleitores().isBlank()) {
            erros.add(new ErroCampo("eleitores", "Informe pelo menos um eleitor, no formato id;nome."));
            return;
        }
        try {
            Eleitorado.importar(f.getEleitores());
        } catch (IllegalArgumentException e) {
            erros.add(new ErroCampo("eleitores", traduzirErroEleitores(e.getMessage())));
        }
    }

    /**
     * Traduz a mensagem de {@link Eleitorado#importar} para a tela, mantendo o número da linha.
     *
     * @param mensagem mensagem técnica, como {@code "linha 3: formato esperado id;nome"}
     * @return frase em português, como {@code "Linha 3: use o formato id;nome."}
     */
    static String traduzirErroEleitores(String mensagem) {
        Matcher linha = ERRO_LINHA.matcher(mensagem == null ? "" : mensagem);
        if (!linha.matches()) {
            return "Lista de eleitores inválida. Use uma linha por eleitor, no formato id;nome.";
        }
        String motivo = linha.group(2);
        String traduzido;
        if (motivo.startsWith("formato esperado")) {
            traduzido = "use o formato id;nome, com um só ponto e vírgula";
        } else if (motivo.startsWith("id de eleitor repetido: ")) {
            traduzido = "o id \"" + motivo.substring("id de eleitor repetido: ".length()) + "\" já apareceu antes";
        } else if (motivo.startsWith("id do eleitor")) {
            traduzido = "falta o id do eleitor";
        } else if (motivo.startsWith("nome do eleitor")) {
            traduzido = "falta o nome do eleitor";
        } else {
            traduzido = "linha inválida";
        }
        return "Linha " + linha.group(1) + ": " + traduzido + ".";
    }

    /**
     * Lê uma fração ({@code 2/3}) ou um percentual ({@code 50}, {@code 50%}, {@code 66,7}).
     * A fração {@code a/b} vira exatamente {@code (double) a / b}, para {@code 2/3} coincidir com o
     * limite usado nos testes da maioria qualificada; o percentual {@code p} vira {@code p / 100}.
     * Não confere se o valor está entre 0 e 1.
     *
     * @param texto o texto digitado
     * @return o valor como fração de 1
     * @throws IllegalArgumentException se o texto não estiver em nenhum dos dois formatos ou se o
     *                                  denominador for zero
     */
    public static double lerFracao(String texto) {
        String limpo = texto == null ? "" : texto.strip();
        Matcher fracao = FRACAO.matcher(limpo);
        if (fracao.matches()) {
            long denominador = Long.parseLong(fracao.group(2));
            if (denominador == 0) {
                throw new IllegalArgumentException("denominador nao pode ser zero");
            }
            return (double) Long.parseLong(fracao.group(1)) / denominador;
        }
        Matcher percentual = PERCENTUAL.matcher(limpo);
        if (percentual.matches()) {
            return Double.parseDouble(percentual.group(1).replace(',', '.')) / CEM_POR_CENTO;
        }
        throw new IllegalArgumentException("formato invalido: " + limpo);
    }

    /**
     * Monta a votação em rascunho a partir de um formulário válido.
     *
     * @param f       formulário sem erros em {@link #validar}
     * @param relogio relógio da aplicação
     * @param autor   organizador que cria a votação (vai para a auditoria)
     * @return a nova votação
     * @throws IllegalArgumentException se o formulário for inválido
     */
    public static Votacao paraVotacao(FormularioVotacao f, Clock relogio, String autor) {
        Votacao.Builder builder = Votacao.nova()
                .pergunta(f.getPergunta())
                .descricao(f.getDescricao())
                .opcoes(linhas(f.getOpcoes()))
                .regra(regra(f))
                .eleitorado(Eleitorado.importar(f.getEleitores()))
                .sigilo(f.getSigilo())
                .relogio(relogio)
                .criadaPor(autor);
        if (f.getInicio() != null && f.getFim() != null) {
            builder.periodo(new PeriodoVotacao(
                    f.getInicio().atZone(Fuso.ZONA).toInstant(), f.getFim().atZone(Fuso.ZONA).toInstant()));
        }
        return builder.build();
    }

    /**
     * Descreve a regra escolhida, para a pré-visualização, usando a {@code descricao()} da própria regra.
     *
     * @param f formulário, válido ou não
     * @return a descrição da regra, ou um aviso se quórum ou fração ainda não forem válidos
     */
    public static String descricaoRegra(FormularioVotacao f) {
        try {
            return regra(f).descricao();
        } catch (IllegalArgumentException e) {
            return "Maioria qualificada: informe quórum e fração válidos para ver a regra.";
        }
    }

    /**
     * Monta a regra escolhida no formulário.
     *
     * @param f formulário com a regra válida
     * @return a regra de apuração
     * @throws IllegalArgumentException se quórum ou fração forem inválidos
     */
    static RegraApuracao regra(FormularioVotacao f) {
        TipoRegra tipo = f.getTipoRegra() == null ? TipoRegra.ABSOLUTA : f.getTipoRegra();
        return switch (tipo) {
            case SIMPLES -> new MaioriaSimples();
            case ABSOLUTA -> new MaioriaAbsoluta(f.isSegundoTurno());
            case QUALIFICADA -> new MaioriaQualificada(lerFracao(f.getQuorum()), lerFracao(f.getFracaoExigida()));
        };
    }

    /**
     * Separa um texto de várias linhas, sem espaços nas pontas e sem linhas em branco.
     *
     * @param texto texto de um textarea
     * @return as linhas preenchidas, na ordem
     */
    static List<String> linhas(String texto) {
        return texto.lines().map(String::strip).filter(linha -> !linha.isEmpty()).toList();
    }
}

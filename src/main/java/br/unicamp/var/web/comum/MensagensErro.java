package br.unicamp.var.web.comum;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Traduz as exceções do domínio (mensagens técnicas, em minúsculas e sem acento) em frases pt-BR
 * para o usuário. O domínio não muda: a tradução é feita pelo começo da mensagem.
 *
 * <p>Cobre as mensagens lançadas em {@code urna}, {@code eleitorado}, {@code processo},
 * {@code apuracao}, {@code relatorio}, {@code cadastro} e {@code top3}. As transições proibidas do
 * State ("não é possível abrir em votação aberta") viram "Esta ação não é permitida com a votação
 * aberta.", e os erros de linha da importação de eleitores ("linha 3: ...") indicam a linha.
 * Qualquer outra mensagem cai no texto genérico {@value #GENERICA}.</p>
 */
public final class MensagensErro {

    /** Texto usado quando a mensagem não é reconhecida. */
    public static final String GENERICA = "Não foi possível concluir a operação.";

    private static final String CAMPOS_OBRIGATORIOS = "Preencha todos os campos obrigatórios.";
    private static final String PESO_INVALIDO = "O peso de cada grupo deve ser um número maior que zero.";
    private static final String PERIODO_INCOMPLETO = "Informe o início e o fim do período.";
    private static final String LISTA_ELEITORES = "Informe a lista de eleitores.";

    private static final Pattern TRANSICAO_PROIBIDA = Pattern.compile("^não é possível .+ em votação (\\S+)$");
    private static final Pattern LINHA_IMPORTACAO = Pattern.compile("^linha (\\d+): (.*)$", Pattern.DOTALL);
    private static final Pattern CAMPO_VAZIO =
            Pattern.compile(".* nao pode (ser|estar) (vazio|vazia|nulo|nula|em branco).*", Pattern.DOTALL);

    /** Prefixo da mensagem do domínio → frase para o usuário; a ordem importa (mais específico antes). */
    private static final Map<String, String> POR_PREFIXO = new LinkedHashMap<>();

    static {
        // Urna e votação (histórias #22, #26, #28; RNF-01, RNF-02, RNF-05)
        POR_PREFIXO.put("eleitor nao apto", "Você não está na lista de eleitores aptos.");
        POR_PREFIXO.put("eleitor ja votou", "Você já votou nesta votação.");
        POR_PREFIXO.put("voto fora do periodo", "A votação não está recebendo votos neste horário.");
        POR_PREFIXO.put("voto secreto", "O voto é secreto: não é possível consultar o voto de um eleitor.");
        POR_PREFIXO.put("opcao inexistente", "A opção escolhida não existe nesta votação.");
        POR_PREFIXO.put("codigo do comprovante", "Informe o código do comprovante.");
        // Ciclo, 2º turno e relatório (histórias #20, #21, #31)
        POR_PREFIXO.put("segundo turno so existe",
                "O 2º turno só pode ser criado quando a apuração indica 2º turno.");
        POR_PREFIXO.put("segundo turno ja foi criado", "O 2º turno desta votação já foi criado.");
        POR_PREFIXO.put("relatorio so existe", "O relatório só fica disponível depois da apuração.");
        POR_PREFIXO.put("autor nao pode ser vazio", "Não foi possível identificar quem fez a operação.");
        // Configuração da votação (histórias #19, #20, #21, #24)
        POR_PREFIXO.put("pergunta nao pode ser vazia", "Informe a pergunta da votação.");
        POR_PREFIXO.put("deve haver ao menos 2 opcoes", "Informe pelo menos duas opções.");
        POR_PREFIXO.put("deve haver ao menos uma opcao", "Informe pelo menos duas opções.");
        POR_PREFIXO.put("opcao repetida", "Há opções repetidas: cada opção deve aparecer uma vez.");
        POR_PREFIXO.put("opcao nao pode ser", "Nenhuma opção pode ficar em branco.");
        POR_PREFIXO.put("fim do periodo deve ser depois do inicio", "O fim do período deve ser depois do início.");
        POR_PREFIXO.put("inicio e fim do periodo", PERIODO_INCOMPLETO);
        POR_PREFIXO.put("periodo nao pode ser nulo", PERIODO_INCOMPLETO);
        POR_PREFIXO.put("regra de apuracao nao pode ser nula", "Escolha a regra de apuração.");
        POR_PREFIXO.put("quorumMinimo deve estar em", "O quórum mínimo deve ser maior que 0% e no máximo 100%.");
        POR_PREFIXO.put("fracaoExigida deve estar em",
                "A fração exigida deve ser maior que 0% e no máximo 100%.");
        POR_PREFIXO.put("deve haver ao menos um eleitor", "Informe pelo menos um eleitor apto.");
        POR_PREFIXO.put("id de eleitor repetido", "Há eleitores repetidos na lista.");
        POR_PREFIXO.put("formato esperado id;nome", "Use uma linha por eleitor, no formato id;nome.");
        POR_PREFIXO.put("lista de eleitores nao pode ser nula", LISTA_ELEITORES);
        POR_PREFIXO.put("texto da lista de eleitores", LISTA_ELEITORES);
        POR_PREFIXO.put("Ja existe candidato com o codigo", "Já existe um candidato com este código.");
        // Premiação Top 3 (histórias #23, #30)
        POR_PREFIXO.put("operacao invalida no estado atual da premiacao",
                "Esta ação não é permitida no estado atual da premiação.");
        POR_PREFIXO.put("candidato inexistente", "O candidato escolhido não está na lista.");
        POR_PREFIXO.put("candidato repetido na cedula", "Escolha três candidatos diferentes.");
        POR_PREFIXO.put("candidato repetido", "Há candidatos repetidos.");
        POR_PREFIXO.put("deve haver ao menos tres candidatos", "Informe pelo menos três candidatos.");
        POR_PREFIXO.put("deve haver ao menos uma categoria", "Informe pelo menos um grupo de votantes.");
        POR_PREFIXO.put("nome de categoria repetido", "Há grupos de votantes com o mesmo nome.");
        POR_PREFIXO.put("categoria do eleitor inexistente", "Há eleitor associado a um grupo que não existe.");
        POR_PREFIXO.put("peso deve ser finito", PESO_INVALIDO);
        POR_PREFIXO.put("pesoCategoria deve ser finito", PESO_INVALIDO);
        POR_PREFIXO.put("titulo nao pode", "Informe o título da premiação.");
        POR_PREFIXO.put("pontuacao excede o limite numerico", "A pontuação passou do limite numérico.");
    }

    private MensagensErro() {
        // só métodos estáticos
    }

    /**
     * Frase amigável para uma exceção do domínio.
     *
     * @param erro exceção capturada (normalmente {@link IllegalArgumentException} ou
     *             {@link IllegalStateException})
     * @return a frase em pt-BR, ou {@value #GENERICA} se a mensagem não for reconhecida
     */
    public static String amigavel(RuntimeException erro) {
        if (erro == null || erro.getMessage() == null) {
            return GENERICA;
        }
        return traduzir(erro.getMessage().strip());
    }

    private static String traduzir(String mensagem) {
        Matcher transicao = TRANSICAO_PROIBIDA.matcher(mensagem);
        if (transicao.matches()) {
            return "Esta ação não é permitida com a votação " + comArtigo(transicao.group(1)) + ".";
        }
        Matcher linha = LINHA_IMPORTACAO.matcher(mensagem);
        if (linha.matches()) {
            return "Linha " + linha.group(1) + " da lista de eleitores: " + traduzir(linha.group(2).strip());
        }
        for (Map.Entry<String, String> entrada : POR_PREFIXO.entrySet()) {
            if (mensagem.startsWith(entrada.getKey())) {
                return entrada.getValue();
            }
        }
        if (CAMPO_VAZIO.matcher(mensagem).matches()) {
            return CAMPOS_OBRIGATORIOS;
        }
        return GENERICA;
    }

    private static String comArtigo(String estado) {
        return "rascunho".equals(estado) ? "em rascunho" : estado;
    }
}

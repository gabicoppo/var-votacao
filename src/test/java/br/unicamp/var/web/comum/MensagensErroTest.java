package br.unicamp.var.web.comum;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import br.unicamp.var.apuracao.MaioriaQualificada;
import br.unicamp.var.apuracao.MaioriaSimples;
import br.unicamp.var.eleitorado.Eleitor;
import br.unicamp.var.eleitorado.Eleitorado;
import br.unicamp.var.processo.Votacao;
import br.unicamp.var.top3.CategoriaEleitor;
import br.unicamp.var.top3.CedulaTop3;
import br.unicamp.var.top3.PremiacaoTop3;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Tradução das mensagens do domínio para frases pt-BR.
 */
class MensagensErroTest {

    private static String amigavel(String mensagem) {
        return MensagensErro.amigavel(new IllegalArgumentException(mensagem));
    }

    private static String amigavelDe(Executable operacao) {
        return MensagensErro.amigavel(assertThrows(RuntimeException.class, operacao));
    }

    @Nested
    @DisplayName("Cada mensagem do domínio mapeada")
    class Mapeamento {

        @ParameterizedTest(name = "\"{0}\"")
        @CsvSource(delimiter = '|', value = {
            "eleitor nao apto|Você não está na lista de eleitores aptos.",
            "eleitor nao apto: e99|Você não está na lista de eleitores aptos.",
            "eleitor ja votou: e01|Você já votou nesta votação.",
            "voto fora do periodo de votacao|A votação não está recebendo votos neste horário.",
            "voto secreto: nao e possivel consultar o voto de um eleitor|"
                    + "O voto é secreto: não é possível consultar o voto de um eleitor.",
            "opcao inexistente: Chapa Z|A opção escolhida não existe nesta votação.",
            "codigo do comprovante nao pode ser vazio|Informe o código do comprovante.",
            "segundo turno so existe quando a apuracao indica segundo turno|"
                    + "O 2º turno só pode ser criado quando a apuração indica 2º turno.",
            "segundo turno ja foi criado|O 2º turno desta votação já foi criado.",
            "relatorio so existe para votacao apurada|O relatório só fica disponível depois da apuração.",
            "autor nao pode ser vazio|Não foi possível identificar quem fez a operação.",
            "pergunta nao pode ser vazia|Informe a pergunta da votação.",
            "deve haver ao menos 2 opcoes|Informe pelo menos duas opções.",
            "deve haver ao menos uma opcao|Informe pelo menos duas opções.",
            "opcao repetida: Sim|Há opções repetidas: cada opção deve aparecer uma vez.",
            "opcao nao pode ser vazia|Nenhuma opção pode ficar em branco.",
            "opcao nao pode ser nula|Nenhuma opção pode ficar em branco.",
            "fim do periodo deve ser depois do inicio|O fim do período deve ser depois do início.",
            "inicio e fim do periodo nao podem ser nulos|Informe o início e o fim do período.",
            "periodo nao pode ser nulo|Informe o início e o fim do período.",
            "regra de apuracao nao pode ser nula|Escolha a regra de apuração.",
            "quorumMinimo deve estar em (0, 1]|O quórum mínimo deve ser maior que 0% e no máximo 100%.",
            "fracaoExigida deve estar em (0, 1]|A fração exigida deve ser maior que 0% e no máximo 100%.",
            "deve haver ao menos um eleitor|Informe pelo menos um eleitor apto.",
            "id de eleitor repetido: e01|Há eleitores repetidos na lista.",
            "formato esperado id;nome|Use uma linha por eleitor, no formato id;nome.",
            "lista de eleitores nao pode ser nula|Informe a lista de eleitores.",
            "texto da lista de eleitores nao pode ser nulo|Informe a lista de eleitores.",
            "Ja existe candidato com o codigo 10|Já existe um candidato com este código.",
            "operacao invalida no estado atual da premiacao|"
                    + "Esta ação não é permitida no estado atual da premiação.",
            "candidato inexistente|O candidato escolhido não está na lista.",
            "candidato repetido na cedula|Escolha três candidatos diferentes.",
            "candidato repetido|Há candidatos repetidos.",
            "deve haver ao menos tres candidatos|Informe pelo menos três candidatos.",
            "deve haver ao menos uma categoria|Informe pelo menos um grupo de votantes.",
            "nome de categoria repetido|Há grupos de votantes com o mesmo nome.",
            "categoria do eleitor inexistente|Há eleitor associado a um grupo que não existe.",
            "peso deve ser finito e maior que zero|O peso de cada grupo deve ser um número maior que zero.",
            "pesoCategoria deve ser finito e maior que zero, mas foi -1.0|"
                    + "O peso de cada grupo deve ser um número maior que zero.",
            "titulo nao pode estar em branco|Informe o título da premiação.",
            "pontuacao excede o limite numerico|A pontuação passou do limite numérico.",
            "descricao nao pode ser nula|Preencha todos os campos obrigatórios.",
            "eleitorado nao pode ser nulo|Preencha todos os campos obrigatórios.",
            "O nome nao pode ser nulo, vazio ou em branco|Preencha todos os campos obrigatórios.",
        })
        @DisplayName("prefixo conhecido vira frase pt-BR")
        void prefixoConhecido(String mensagem, String esperado) {
            assertEquals(esperado, amigavel(mensagem));
        }

        @ParameterizedTest(name = "{0} -> {1}")
        @CsvSource(delimiter = '|', value = {
            "não é possível abrir em votação aberta|Esta ação não é permitida com a votação aberta.",
            "não é possível registrar voto em votação encerrada|"
                    + "Esta ação não é permitida com a votação encerrada.",
            "não é possível apurar em votação rascunho|Esta ação não é permitida com a votação em rascunho.",
            "não é possível alterar a regra de apuração em votação apurada|"
                    + "Esta ação não é permitida com a votação apurada.",
        })
        @DisplayName("transição proibida do State cita o estado da votação")
        void transicaoProibida(String mensagem, String esperado) {
            assertEquals(esperado, MensagensErro.amigavel(new IllegalStateException(mensagem)));
        }

        @Test
        @DisplayName("erro de linha na importação indica a linha e traduz o motivo")
        void erroDeLinha() {
            assertEquals("Linha 3 da lista de eleitores: Use uma linha por eleitor, no formato id;nome.",
                    amigavel("linha 3: formato esperado id;nome"));
            assertEquals("Linha 2 da lista de eleitores: Não foi possível concluir a operação.",
                    amigavel("linha 2: algo inesperado"));
        }
    }

    @Nested
    @DisplayName("Fallback")
    class Fallback {

        @Test
        @DisplayName("mensagem desconhecida, nula ou exceção nula dão o texto genérico")
        void generico() {
            assertEquals(MensagensErro.GENERICA, amigavel("falha interna xyz"));
            assertEquals(MensagensErro.GENERICA, MensagensErro.amigavel(new IllegalStateException()));
            assertEquals(MensagensErro.GENERICA, MensagensErro.amigavel(null));
            assertEquals("Não foi possível concluir a operação.", MensagensErro.GENERICA);
        }
    }

    @Nested
    @DisplayName("Exceções reais do domínio")
    class DoDominio {

        private Votacao novaVotacao() {
            return Votacao.nova()
                    .pergunta("Quem deve presidir?")
                    .opcoes(List.of("Chapa A", "Chapa B"))
                    .regra(new MaioriaSimples())
                    .eleitorado(Eleitorado.de(List.of(new Eleitor("e01", "Ana Ribeiro"))))
                    .build();
        }

        @Test
        @DisplayName("votação: inapto, voto duplicado, opção inexistente e transição proibida")
        void votacao() {
            Votacao votacao = novaVotacao();
            assertEquals("Esta ação não é permitida com a votação em rascunho.",
                    amigavelDe(() -> votacao.registrarVoto("e01", "Chapa A")));
            votacao.abrir();
            assertEquals("Você não está na lista de eleitores aptos.",
                    amigavelDe(() -> votacao.registrarVoto("e99", "Chapa A")));
            assertEquals("A opção escolhida não existe nesta votação.",
                    amigavelDe(() -> votacao.registrarVoto("e01", "Chapa Z")));
            votacao.registrarVoto("e01", "Chapa A");
            assertEquals("Você já votou nesta votação.", amigavelDe(() -> votacao.registrarVoto("e01", "Chapa B")));
            assertEquals("O voto é secreto: não é possível consultar o voto de um eleitor.",
                    amigavelDe(() -> votacao.votoDe("e01")));
            assertEquals("O 2º turno só pode ser criado quando a apuração indica 2º turno.",
                    amigavelDe(votacao::criarSegundoTurno));
        }

        @Test
        @DisplayName("configuração: pergunta, opções, regra e eleitorado")
        void configuracao() {
            assertEquals("Informe a pergunta da votação.", amigavelDe(() -> Votacao.nova().build()));
            assertEquals("Informe pelo menos duas opções.", amigavelDe(() -> Votacao.nova().pergunta("P")
                    .regra(new MaioriaSimples()).eleitorado(Eleitorado.de(List.of(new Eleitor("e1", "A"))))
                    .opcoes(List.of("Só uma")).build()));
            assertEquals("O quórum mínimo deve ser maior que 0% e no máximo 100%.",
                    amigavelDe(() -> new MaioriaQualificada(0, 0.5)));
            assertEquals("Linha 1 da lista de eleitores: Use uma linha por eleitor, no formato id;nome.",
                    amigavelDe(() -> Eleitorado.importar("sem-separador")));
        }

        @Test
        @DisplayName("premiação: estado, candidato inexistente e cédula repetida")
        void premiacao() {
            PremiacaoTop3 premiacao = new PremiacaoTop3("Craque", "", List.of("A", "B", "C"),
                    List.of(new CategoriaEleitor("Torcedores", 1.0)), Map.of("e01", "Torcedores"));
            assertEquals("Esta ação não é permitida no estado atual da premiação.",
                    amigavelDe(() -> premiacao.registrarCedula("e01", new CedulaTop3("A", "B", "C"))));
            premiacao.abrir();
            assertEquals("O candidato escolhido não está na lista.",
                    amigavelDe(() -> premiacao.registrarCedula("e01", new CedulaTop3("A", "B", "Z"))));
            assertEquals("Escolha três candidatos diferentes.", amigavelDe(() -> new CedulaTop3("A", "A", "C")));
        }
    }
}

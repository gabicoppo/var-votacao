package br.unicamp.var.web.resultado;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import br.unicamp.var.apuracao.MaioriaAbsoluta;
import br.unicamp.var.apuracao.MaioriaQualificada;
import br.unicamp.var.apuracao.MaioriaSimples;
import br.unicamp.var.apuracao.RegraApuracao;
import br.unicamp.var.eleitorado.Eleitor;
import br.unicamp.var.eleitorado.Eleitorado;
import br.unicamp.var.processo.PeriodoVotacao;
import br.unicamp.var.processo.Votacao;
import br.unicamp.var.relatorio.RelatorioApuracao;
import br.unicamp.var.urna.Sigilo;
import br.unicamp.var.web.comum.Alerta;
import br.unicamp.var.web.comum.RepositorioVotacoes;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Telas de resultado, andamento e relatório (histórias #29 e #31): cada estado, cada situação, sigilo
 * antes da apuração (RNF-01), acesso por papel e por lista de aptos (RNF-05) e 404.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ResultadoControllerTest {

    private static final String ORGANIZADOR = "ORGANIZADOR";
    private static final String ELEITOR = "ELEITOR";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private RepositorioVotacoes votacoes;

    /** Votação com e01..e{aptos}, opções "Opção Azul", "Opção Verde" e "Opção Branca". */
    private Votacao nova(RegraApuracao regra, int aptos) {
        Eleitorado eleitorado = Eleitorado.de(IntStream.rangeClosed(1, aptos)
                .mapToObj(i -> new Eleitor(String.format("e%02d", i), "Eleitor " + i)).toList());
        return votacoes.salvar(Votacao.nova().pergunta("Quem leva a taça?").descricao("Votação de teste")
                .opcoes(List.of("Opção Azul", "Opção Verde", "Opção Branca")).regra(regra).eleitorado(eleitorado)
                .build());
    }

    /** Abre, registra um voto por eleitor (e01, e02, ...), encerra e apura. */
    private Votacao apurada(RegraApuracao regra, int aptos, String... votos) {
        Votacao votacao = nova(regra, aptos);
        votacao.abrir();
        for (int i = 0; i < votos.length; i++) {
            votacao.registrarVoto(String.format("e%02d", i + 1), votos[i]);
        }
        votacao.encerrar();
        votacao.apurar();
        return votacao;
    }

    private String url(Votacao votacao, String sufixo) {
        return "/votacoes/" + votacao.id() + sufixo;
    }

    @Nested
    @DisplayName("Antes da apuração: só a participação")
    @WithMockUser(username = "organizador", roles = ORGANIZADOR)
    class Andamento {

        @Test
        @DisplayName("Aberta: participação em destaque, prazo e polling, sem votos por opção (RNF-01)")
        void aberta() throws Exception {
            Instant agora = Instant.now();
            Votacao votacao = votacoes.salvar(Votacao.nova().pergunta("Enquete secreta").opcoes(List.of("Sim", "Não"))
                    .regra(new MaioriaSimples()).eleitorado(Eleitorado.importar("e01;Ana\ne02;Bia\ne03;Caio\ne04;Davi"))
                    .periodo(new PeriodoVotacao(agora.minus(Duration.ofHours(1)), agora.plus(Duration.ofDays(2))))
                    .build());
            votacao.atualizarPeloRelogio();
            votacao.registrarVoto("e01", "Sim");
            votacao.registrarVoto("e02", "Sim");

            mvc.perform(get(url(votacao, "/resultado")))
                    .andExpect(status().isOk())
                    .andExpect(view().name("resultado/resultado"))
                    .andExpect(content().string(allOf(
                            containsString("<title>Resultado · VAR</title>"),
                            containsString("Votação em andamento"),
                            containsString("50%"),
                            containsString("data-andamento-url=\"/votacoes/" + votacao.id() + "/andamento.json\""),
                            containsString("Encerra em 1 dia e 23 h"),
                            containsString("/js/resultado.js"),
                            not(containsString("Sim</span>")),
                            not(containsString("Votos por opção")),
                            not(containsString("Revisão do VAR")),
                            not(containsString("/relatorio")))));
        }

        @Test
        @DisplayName("Rascunho sem período avisa que ainda não abriu")
        void rascunho() throws Exception {
            Votacao votacao = nova(new MaioriaSimples(), 3);

            mvc.perform(get(url(votacao, "/resultado")))
                    .andExpect(status().isOk())
                    .andExpect(content().string(allOf(containsString("Ainda não foi aberta pelo organizador."),
                            containsString("badge-rascunho"), containsString("0%"))));
        }

        @Test
        @DisplayName("Abrir o resultado de uma votação encerrada não a apura nem muda a auditoria")
        void naoMudaStatus() throws Exception {
            Votacao votacao = nova(new MaioriaSimples(), 3);
            votacao.abrir();
            votacao.registrarVoto("e01", "Opção Azul");
            votacao.encerrar();
            int eventos = votacao.auditoria().size();

            mvc.perform(get(url(votacao, "/resultado")))
                    .andExpect(status().isOk())
                    .andExpect(content().string(allOf(containsString("Votação encerrada"),
                            containsString("Aguardando a apuração."), not(containsString("Opção Azul")))));
            mvc.perform(get(url(votacao, "/andamento.json"))).andExpect(status().isOk());
            mvc.perform(get(url(votacao, "/relatorio"))).andExpect(status().is3xxRedirection());

            assertEquals("encerrada", votacao.status());
            assertEquals(eventos, votacao.auditoria().size());
        }

        @Test
        @DisplayName("andamento.json devolve status, votantes e aptos")
        void andamentoJson() throws Exception {
            Votacao votacao = nova(new MaioriaSimples(), 4);
            votacao.abrir();
            votacao.registrarVoto("e03", "Opção Verde");

            mvc.perform(get(url(votacao, "/andamento.json")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("aberta"))
                    .andExpect(jsonPath("$.votantes").value(1))
                    .andExpect(jsonPath("$.aptos").value(4))
                    .andExpect(content().string(not(containsString("Opção Verde"))));
        }
    }

    @Nested
    @DisplayName("Depois da apuração: uma faixa para cada situação")
    @WithMockUser(username = "organizador", roles = ORGANIZADOR)
    class Apurada {

        @Test
        @DisplayName("Eleito: faixa verde, barra vencedora, métricas, regra, Momento VAR e links")
        void eleito() throws Exception {
            Votacao votacao = apurada(new MaioriaSimples(), 4, "Opção Azul", "Opção Azul", "Opção Verde");

            mvc.perform(get(url(votacao, "/resultado")))
                    .andExpect(status().isOk())
                    .andExpect(content().string(allOf(
                            containsString("situacao-eleito"),
                            containsString("Opção Azul venceu"),
                            containsString("Opção Azul recebeu 2 de 3 votos válidos (66,7%)."),
                            containsString("barra barra-vencedor"),
                            containsString("width: 100.0%"),
                            containsString("width: 50.0%"),
                            containsString("width: 0.0%"),
                            containsString("75%"),
                            containsString("Maioria simples: vence a opção mais votada"),
                            containsString("Secreto"),
                            containsString("Revisão do VAR"),
                            containsString("data-var-revisao"),
                            containsString("href=\"/votacoes/" + votacao.id() + "/relatorio\""),
                            containsString("href=\"/organizador/votacoes/" + votacao.id() + "\""))));
        }

        @Test
        @DisplayName("2º turno: faixa amarela, finalistas e link para o 2º turno criado")
        void segundoTurno() throws Exception {
            Votacao votacao = apurada(new MaioriaAbsoluta(true), 9, "Opção Azul", "Opção Azul", "Opção Verde",
                    "Opção Verde", "Opção Branca");
            Votacao segundo = votacoes.salvar(votacao.criarSegundoTurno());

            mvc.perform(get(url(votacao, "/resultado")))
                    .andExpect(content().string(allOf(
                            containsString("situacao-segundo-turno"),
                            containsString("2º turno entre Opção Azul e Opção Verde"),
                            containsString("barra barra-finalista"),
                            containsString("href=\"/votacoes/" + segundo.id() + "/resultado\""))));
            mvc.perform(get(url(segundo, "/resultado")))
                    .andExpect(content().string(allOf(containsString("2º turno</span>"),
                            containsString("href=\"/votacoes/" + votacao.id() + "/resultado\""))));
        }

        @Test
        @DisplayName("Sem vencedor: faixa cinza e explicação do empate")
        void semVencedor() throws Exception {
            Votacao votacao = apurada(new MaioriaSimples(), 4, "Opção Azul", "Opção Verde");

            mvc.perform(get(url(votacao, "/resultado")))
                    .andExpect(content().string(allOf(containsString("situacao-sem-vencedor"),
                            containsString("Sem vencedor"),
                            containsString("Opção Azul e Opção Verde empataram em primeiro lugar"))));
        }

        @Test
        @DisplayName("Quórum não atingido: faixa vermelha com votantes e aptos")
        void quorum() throws Exception {
            Votacao votacao = apurada(new MaioriaQualificada(0.5, 2.0 / 3), 10, "Opção Azul", "Opção Azul");

            mvc.perform(get(url(votacao, "/resultado")))
                    .andExpect(content().string(allOf(containsString("situacao-quorum-nao-atingido"),
                            containsString("Quórum não atingido"),
                            containsString("Votaram 2 de 10 aptos (20%), abaixo do quórum mínimo."))));
        }

        @Test
        @DisplayName("Votação identificada mostra o sigilo e nunca o voto de cada eleitor")
        void identificada() throws Exception {
            Votacao votacao = votacoes.salvar(Votacao.nova().pergunta("Assembleia")
                    .opcoes(List.of("Aprovar", "Rejeitar"))
                    .regra(new MaioriaSimples()).eleitorado(Eleitorado.importar("e01;Ana Ribeiro\ne02;Bia"))
                    .sigilo(Sigilo.IDENTIFICADO).build());
            votacao.abrir();
            votacao.registrarVoto("e01", "Aprovar");
            votacao.encerrar();
            votacao.apurar();

            mvc.perform(get(url(votacao, "/resultado")))
                    .andExpect(content().string(allOf(containsString("Identificado (voto nominal)"),
                            not(containsString("Ana Ribeiro")))));
        }
    }

    @Nested
    @DisplayName("Relatório de apuração (#31)")
    @WithMockUser(username = "organizador", roles = ORGANIZADOR)
    class Relatorio {

        @Test
        @DisplayName("Antes da apuração: alerta e volta ao resultado")
        void antes() throws Exception {
            Votacao votacao = nova(new MaioriaSimples(), 2);

            mvc.perform(get(url(votacao, "/relatorio")))
                    .andExpect(redirectedUrl(url(votacao, "/resultado")))
                    .andExpect(flash().attribute("alerta",
                            Alerta.info("O relatório de apuração fica disponível depois que a votação for apurada.")));
        }

        @Test
        @DisplayName("Depois: ata com o texto do relatório, data de emissão e botão de imprimir")
        void depois() throws Exception {
            Votacao votacao = apurada(new MaioriaSimples(), 3, "Opção Verde");

            mvc.perform(get(url(votacao, "/relatorio")))
                    .andExpect(status().isOk())
                    .andExpect(view().name("resultado/relatorio"))
                    .andExpect(content().string(allOf(
                            containsString("<title>Relatório de apuração · VAR</title>"),
                            containsString("class=\"ata-texto\""),
                            containsString("Regra: Maioria simples"),
                            containsString("Situação: Eleito: Opção Verde"),
                            containsString("Emitido em"),
                            containsString("Imprimir / salvar PDF"))));
        }
    }

    @Nested
    @DisplayName("Acesso (RNF-05)")
    class Acesso {

        @Test
        @WithMockUser(username = "e02", roles = ELEITOR)
        @DisplayName("Eleitor apto vê o resultado, sem o link de gestão")
        void eleitorApto() throws Exception {
            Votacao votacao = apurada(new MaioriaSimples(), 3, "Opção Azul");

            mvc.perform(get(url(votacao, "/resultado")))
                    .andExpect(status().isOk())
                    .andExpect(content().string(allOf(containsString("Opção Azul venceu"),
                            not(containsString("Voltar à gestão")))));
            mvc.perform(get(url(votacao, "/relatorio"))).andExpect(status().isOk());
        }

        @Test
        @WithMockUser(username = "e99", roles = ELEITOR)
        @DisplayName("Eleitor fora da lista recebe 403 no resultado, no JSON e no relatório")
        void eleitorNaoApto() throws Exception {
            Votacao votacao = apurada(new MaioriaSimples(), 3, "Opção Azul");

            mvc.perform(get(url(votacao, "/resultado")))
                    .andExpect(status().isForbidden())
                    .andExpect(view().name("error/403"));
            mvc.perform(get(url(votacao, "/andamento.json"))).andExpect(status().isForbidden());
            mvc.perform(get(url(votacao, "/relatorio"))).andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(username = "organizador", roles = ORGANIZADOR)
        @DisplayName("Votação inexistente dá 404 amigável")
        void inexistente() throws Exception {
            mvc.perform(get("/votacoes/nao-existe/resultado"))
                    .andExpect(status().isNotFound())
                    .andExpect(content().string(containsString("Votação não encontrada")));
            mvc.perform(get("/votacoes/nao-existe/andamento.json")).andExpect(status().isNotFound());
            mvc.perform(get("/votacoes/nao-existe/relatorio")).andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Sem login, vai para a página de login")
        void anonimo() throws Exception {
            mvc.perform(get("/votacoes/qualquer/resultado")).andExpect(redirectedUrlPattern("**/login"));
        }
    }

    @Nested
    @DisplayName("Telão da votação")
    class Telao {

        @Test
        @WithMockUser(username = "organizador", roles = ORGANIZADOR)
        @DisplayName("Aberta: tela própria, participação ao vivo, sem votos por opção (RNF-01)")
        void aberta() throws Exception {
            Votacao votacao = nova(new MaioriaSimples(), 4);
            votacao.abrir();
            votacao.registrarVoto("e01", "Opção Azul");

            mvc.perform(get(url(votacao, "/telao")))
                    .andExpect(status().isOk())
                    .andExpect(view().name("resultado/telao"))
                    .andExpect(content().string(allOf(
                            containsString("<body class=\"telao\">"),
                            containsString("data-telao=\"votacao\""),
                            containsString("Ao vivo"),
                            containsString("data-andamento-url=\"/votacoes/" + votacao.id() + "/andamento.json\""),
                            containsString("de <span data-aptos>4</span> votaram"),
                            containsString("Sair do telão"),
                            containsString("http-equiv=\"refresh\""),
                            not(containsString("class=\"topo\"")),
                            not(containsString("Opção Azul")),
                            not(containsString("Revisão do VAR")))));
            assertEquals("aberta", votacao.status());
        }

        @Test
        @WithMockUser(username = "organizador", roles = ORGANIZADOR)
        @DisplayName("Aberta com período: contagem regressiva até o fim")
        void contagem() throws Exception {
            Instant agora = Instant.now();
            Votacao votacao = votacoes.salvar(Votacao.nova().pergunta("Com prazo").opcoes(List.of("Sim", "Não"))
                    .regra(new MaioriaSimples()).eleitorado(Eleitorado.importar("e01;Ana"))
                    .periodo(new PeriodoVotacao(agora.minus(Duration.ofHours(1)), agora.plus(Duration.ofHours(3))))
                    .build());
            votacao.atualizarPeloRelogio();

            mvc.perform(get(url(votacao, "/telao")))
                    .andExpect(content().string(allOf(containsString("data-contagem"),
                            containsString("data-fim=\"" + votacao.periodo().orElseThrow().fim().toEpochMilli() + "\""),
                            containsString("Encerra em"))));
        }

        @Test
        @WithMockUser(username = "organizador", roles = ORGANIZADOR)
        @DisplayName("Encerrada aguarda a apuração sem apurar; apurada mostra o Momento VAR e a situação")
        void encerradaEApurada() throws Exception {
            Votacao encerrada = nova(new MaioriaSimples(), 3);
            encerrada.abrir();
            encerrada.registrarVoto("e01", "Opção Verde");
            encerrada.encerrar();
            mvc.perform(get(url(encerrada, "/telao")))
                    .andExpect(content().string(allOf(containsString("Votação encerrada. Aguardando apuração…"),
                            not(containsString("Opção Verde")))));
            assertEquals("encerrada", encerrada.status());

            Votacao apurada = apurada(new MaioriaSimples(), 3, "Opção Verde", "Opção Verde");
            mvc.perform(get(url(apurada, "/telao")))
                    .andExpect(content().string(allOf(containsString("var-revisao var-revisao-telao"),
                            containsString("data-sempre"), containsString("Decisão confirmada"),
                            containsString("situacao-eleito"), containsString("Opção Verde venceu"),
                            containsString("--ordem: 0"), containsString("--ordem: 2"),
                            not(containsString("http-equiv=\"refresh\"")))));
        }

        @Test
        @WithMockUser(username = "e01", roles = ELEITOR)
        @DisplayName("Eleitor, mesmo apto, recebe 403")
        void eleitor() throws Exception {
            Votacao votacao = nova(new MaioriaSimples(), 3);

            mvc.perform(get(url(votacao, "/telao"))).andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(username = "organizador", roles = ORGANIZADOR)
        @DisplayName("Votação inexistente dá 404")
        void inexistente() throws Exception {
            mvc.perform(get("/votacoes/nao-existe/telao")).andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser(username = "organizador", roles = ORGANIZADOR)
        @DisplayName("A página de resultado tem o botão Abrir no telão, em nova aba")
        void botao() throws Exception {
            Votacao votacao = nova(new MaioriaSimples(), 3);

            mvc.perform(get(url(votacao, "/resultado")))
                    .andExpect(content().string(allOf(containsString("Abrir no telão"),
                            containsString("href=\"/votacoes/" + votacao.id() + "/telao\""),
                            containsString("target=\"_blank\""))));
        }
    }

    @Nested
    @DisplayName("Contexto do 2º turno e selo do eleitor")
    class Contexto {

        @Test
        @WithMockUser(username = "organizador", roles = ORGANIZADOR)
        @DisplayName("O card do 1º turno aparece só no 2º turno; no 1º, o link para o 2º fica em destaque")
        void primeiroTurno() throws Exception {
            Votacao primeiro = apurada(new MaioriaAbsoluta(true), 9, "Opção Azul", "Opção Azul", "Opção Verde",
                    "Opção Branca");
            Votacao segundo = votacoes.salvar(primeiro.criarSegundoTurno());

            mvc.perform(get(url(segundo, "/resultado")))
                    .andExpect(content().string(allOf(containsString("id=\"titulo-primeiro-turno\""),
                            containsString("2º turno entre Opção Azul e Opção Verde"),
                            containsString("barras-compactas"),
                            containsString("Ver o resultado completo do 1º turno"))));
            mvc.perform(get(url(primeiro, "/resultado")))
                    .andExpect(content().string(allOf(not(containsString("id=\"titulo-primeiro-turno\"")),
                            containsString("O 2º turno já foi criado."),
                            containsString("btn btn-primario\" href=\"/votacoes/" + segundo.id() + "/resultado\""))));
        }

        @Test
        @WithMockUser(username = "organizador", roles = ORGANIZADOR)
        @DisplayName("2º turno cujo 1º turno não está no repositório não mostra o card")
        void primeiroTurnoAusente() throws Exception {
            Votacao primeiro = Votacao.nova().pergunta("Fora do repositório").opcoes(List.of("A", "B", "C"))
                    .regra(new MaioriaAbsoluta(true)).eleitorado(Eleitorado.importar("e01;Ana\ne02;Bia\ne03;Caio"))
                    .build();
            primeiro.abrir();
            primeiro.registrarVoto("e01", "A");
            primeiro.registrarVoto("e02", "B");
            primeiro.registrarVoto("e03", "C");
            primeiro.encerrar();
            primeiro.apurar();
            Votacao segundo = votacoes.salvar(primeiro.criarSegundoTurno());

            mvc.perform(get(url(segundo, "/resultado")))
                    .andExpect(status().isOk())
                    .andExpect(content().string(not(containsString("id=\"titulo-primeiro-turno\""))));
        }

        @Test
        @WithMockUser(username = "e01", roles = ELEITOR)
        @DisplayName("Eleitor que votou vê \"Seu voto foi contado\", sem dizer em quem")
        void votou() throws Exception {
            Votacao votacao = apurada(new MaioriaSimples(), 3, "Opção Branca", "Opção Azul");

            mvc.perform(get(url(votacao, "/resultado")))
                    .andExpect(content().string(allOf(containsString("✓ Seu voto foi contado"),
                            not(containsString("Você não votou")))));
        }

        @Test
        @WithMockUser(username = "e03", roles = ELEITOR)
        @DisplayName("Eleitor que não votou vê o aviso, e não o selo")
        void naoVotou() throws Exception {
            Votacao apurada = apurada(new MaioriaSimples(), 3, "Opção Azul");
            mvc.perform(get(url(apurada, "/resultado")))
                    .andExpect(content().string(allOf(containsString("Você não votou nesta votação."),
                            not(containsString("Seu voto foi contado")))));

            Votacao aberta = nova(new MaioriaSimples(), 3);
            aberta.abrir();
            mvc.perform(get(url(aberta, "/resultado")))
                    .andExpect(content().string(containsString("Você ainda não votou nesta votação.")));
        }

        @Test
        @WithMockUser(username = "organizador", roles = ORGANIZADOR)
        @DisplayName("Organizador não recebe selo de voto")
        void organizadorSemSelo() throws Exception {
            Votacao votacao = apurada(new MaioriaSimples(), 3, "Opção Azul");

            mvc.perform(get(url(votacao, "/resultado")))
                    .andExpect(content().string(allOf(not(containsString("Seu voto foi contado")),
                            not(containsString("não votou nesta votação")))));
        }

        @Test
        @WithMockUser(username = "organizador", roles = ORGANIZADOR)
        @DisplayName("Relatório: código de verificação do texto e assinaturas para a impressão")
        void relatorioComoAta() throws Exception {
            Votacao votacao = apurada(new MaioriaSimples(), 3, "Opção Azul");
            String codigo = CodigoVerificacao.de(RelatorioApuracao.de(votacao).formatar());

            mvc.perform(get(url(votacao, "/relatorio")))
                    .andExpect(content().string(allOf(
                            containsString("Código de verificação do relatório: <code>" + codigo + "</code>"),
                            containsString("Presidente da comissão eleitoral"),
                            containsString("Secretário(a)"))));
        }
    }
}

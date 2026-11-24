package br.unicamp.var.web.premiacao;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import br.unicamp.var.top3.CategoriaEleitor;
import br.unicamp.var.top3.CedulaTop3;
import br.unicamp.var.top3.PremiacaoTop3;
import br.unicamp.var.top3.StatusPremiacao;
import br.unicamp.var.web.comum.PremiacaoCadastrada;
import br.unicamp.var.web.comum.RepositorioPremiacoes;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Ranking da premiação (história #30): andamento antes da apuração, pódio (com empate), tabela com uma
 * coluna por grupo, critérios, JSON de andamento e acesso por lista de aptos.
 */
@SpringBootTest
@AutoConfigureMockMvc
class RankingControllerTest {

    private static final String JORNALISTAS = "Jornalistas";
    private static final String TORCEDORES = "Torcedores";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private RepositorioPremiacoes premiacoes;

    /** "Craque do Campeonato" dos testes do domínio: 3 jornalistas (peso 2) e 5 torcedores (peso 1). */
    private PremiacaoCadastrada craque(boolean apurar) {
        Map<String, String> eleitores = new LinkedHashMap<>();
        List.of("j1", "j2", "j3").forEach(id -> eleitores.put(id, JORNALISTAS));
        List.of("t1", "t2", "t3", "t4", "t5").forEach(id -> eleitores.put(id, TORCEDORES));
        PremiacaoTop3 p = new PremiacaoTop3("Craque do Campeonato", "Contribuição para o time",
                List.of("Elisa", "Davi", "Caio", "Bia", "Ana"),
                List.of(new CategoriaEleitor(JORNALISTAS, 2), new CategoriaEleitor(TORCEDORES, 1)), eleitores);
        p.abrir();
        p.registrarCedula("j1", new CedulaTop3("Ana", "Bia", "Caio"));
        p.registrarCedula("j2", new CedulaTop3("Bia", "Ana", "Davi"));
        p.registrarCedula("j3", new CedulaTop3("Ana", "Caio", "Bia"));
        p.registrarCedula("t1", new CedulaTop3("Bia", "Caio", "Ana"));
        p.registrarCedula("t2", new CedulaTop3("Caio", "Bia", "Davi"));
        p.registrarCedula("t3", new CedulaTop3("Bia", "Ana", "Caio"));
        p.registrarCedula("t4", new CedulaTop3("Davi", "Bia", "Ana"));
        p.registrarCedula("t5", new CedulaTop3("Ana", "Davi", "Bia"));
        if (apurar) {
            p.encerrar();
            p.apurar();
        }
        return premiacoes.salvar(p);
    }

    private static String url(PremiacaoCadastrada cadastro, String sufixo) {
        return "/premiacoes/" + cadastro.id() + sufixo;
    }

    @Nested
    @DisplayName("Ranking apurado")
    @WithMockUser(username = "organizador", roles = "ORGANIZADOR")
    class Apurado {

        @Test
        @DisplayName("Pódio, tabela com uma coluna por grupo, critérios e total de cédulas")
        void completo() throws Exception {
            PremiacaoCadastrada cadastro = craque(true);

            mvc.perform(get(url(cadastro, "/ranking")))
                    .andExpect(status().isOk())
                    .andExpect(view().name("premiacao/ranking"))
                    .andExpect(content().string(allOf(
                            containsString("<title>Ranking · VAR</title>"),
                            containsString("Contribuição para o time"),
                            containsString("degrau degrau-ouro"), containsString("degrau degrau-prata"),
                            containsString("degrau degrau-bronze"),
                            containsString("<strong>Ana</strong>"), containsString("36 pts"),
                            containsString("35 pts"), containsString("17 pts"),
                            containsString("coluna-grupo\">Jornalistas</th>"),
                            containsString("coluna-grupo\">Torcedores</th>"),
                            containsString("<td class=\"numero coluna-grupo\">26</td>"),
                            containsString("<td class=\"numero coluna-grupo\">10</td>"),
                            containsString("Elisa"),
                            containsString("Como os pontos foram calculados"),
                            containsString("1º lugar = 5 pontos"), containsString("Jornalistas: peso 2,0"),
                            containsString("cédulas apuradas"),
                            containsString("href=\"/organizador/premiacoes/" + cadastro.id() + "\""))));
            assertEquals(StatusPremiacao.APURADA, cadastro.premiacao().status());
        }

        @Test
        @DisplayName("Empate no 1º lugar: dois nomes no ouro e nenhuma prata")
        void podioComEmpate() throws Exception {
            PremiacaoTop3 p = new PremiacaoTop3("Empate", "", List.of("A", "B", "C", "D"),
                    List.of(new CategoriaEleitor(TORCEDORES, 1)), Map.of("e1", TORCEDORES, "e2", TORCEDORES));
            p.abrir();
            p.registrarCedula("e1", new CedulaTop3("A", "B", "C"));
            p.registrarCedula("e2", new CedulaTop3("B", "A", "D"));
            p.encerrar();
            p.apurar();
            PremiacaoCadastrada cadastro = premiacoes.salvar(p);

            mvc.perform(get(url(cadastro, "/ranking")))
                    .andExpect(content().string(allOf(containsString("degrau degrau-ouro"),
                            not(containsString("degrau degrau-prata")), containsString("degrau degrau-bronze"),
                            containsString("Desempate"))));
        }

        @Test
        @DisplayName("Sem cédulas: pódio vazio e tabela com todos em zero")
        void semCedulas() throws Exception {
            PremiacaoTop3 p = new PremiacaoTop3("Vazia", "", List.of("A", "B", "C"),
                    List.of(new CategoriaEleitor(TORCEDORES, 1)), Map.of("e1", TORCEDORES));
            p.abrir();
            p.encerrar();
            p.apurar();
            PremiacaoCadastrada cadastro = premiacoes.salvar(p);

            mvc.perform(get(url(cadastro, "/ranking")))
                    .andExpect(content().string(allOf(containsString("Nenhuma cédula registrada"),
                            not(containsString("class=\"podio\"")), containsString("<th scope=\"row\">C</th>"))));
        }
    }

    @Nested
    @DisplayName("Antes da apuração")
    @WithMockUser(username = "organizador", roles = "ORGANIZADOR")
    class Andamento {

        @Test
        @DisplayName("Aberta: participação ao vivo, sem pódio nem pontos")
        void aberta() throws Exception {
            PremiacaoCadastrada cadastro = craque(false);

            mvc.perform(get(url(cadastro, "/ranking")))
                    .andExpect(status().isOk())
                    .andExpect(content().string(allOf(containsString("Premiação em andamento"),
                            containsString("100%"), containsString("8</strong> de <strong"),
                            containsString("data-andamento-url=\"/premiacoes/" + cadastro.id() + "/andamento.json\""),
                            not(containsString("Pódio")), not(containsString(" pts")))));
            assertEquals(StatusPremiacao.ABERTA, cadastro.premiacao().status());
        }

        @Test
        @DisplayName("Rascunho avisa que ainda não abriu; encerrada não é apurada pela consulta")
        void rascunhoEEncerrada() throws Exception {
            PremiacaoCadastrada rascunho = premiacoes.salvar(new PremiacaoTop3("R", "", List.of("A", "B", "C"),
                    List.of(new CategoriaEleitor(TORCEDORES, 1)), Map.of("e1", TORCEDORES)));
            mvc.perform(get(url(rascunho, "/ranking")))
                    .andExpect(content().string(containsString("ainda não foi aberta")));

            PremiacaoCadastrada encerrada = craque(false);
            encerrada.premiacao().encerrar();
            mvc.perform(get(url(encerrada, "/ranking")))
                    .andExpect(content().string(containsString("Premiação encerrada")));
            mvc.perform(get(url(encerrada, "/andamento.json"))).andExpect(jsonPath("$.status").value("encerrada"));
            assertEquals(StatusPremiacao.ENCERRADA, encerrada.premiacao().status());
        }

        @Test
        @DisplayName("andamento.json devolve status, cédulas e eleitores")
        void json() throws Exception {
            PremiacaoCadastrada cadastro = craque(false);

            mvc.perform(get(url(cadastro, "/andamento.json")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("aberta"))
                    .andExpect(jsonPath("$.votantes").value(8))
                    .andExpect(jsonPath("$.aptos").value(8));
        }
    }

    @Nested
    @DisplayName("Acesso")
    class Acesso {

        @Test
        @WithMockUser(username = "t3", roles = "ELEITOR")
        @DisplayName("Eleitor apto vê o ranking, sem o link de gestão")
        void apto() throws Exception {
            PremiacaoCadastrada cadastro = craque(true);

            mvc.perform(get(url(cadastro, "/ranking")))
                    .andExpect(status().isOk())
                    .andExpect(content().string(allOf(containsString("degrau-ouro"),
                            not(containsString("Voltar à gestão")))));
        }

        @Test
        @WithMockUser(username = "e99", roles = "ELEITOR")
        @DisplayName("Eleitor fora da lista recebe 403 no ranking e no JSON")
        void naoApto() throws Exception {
            PremiacaoCadastrada cadastro = craque(true);

            mvc.perform(get(url(cadastro, "/ranking")))
                    .andExpect(status().isForbidden())
                    .andExpect(view().name("error/403"));
            mvc.perform(get(url(cadastro, "/andamento.json"))).andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(username = "organizador", roles = "ORGANIZADOR")
        @DisplayName("Premiação inexistente dá 404 amigável")
        void inexistente() throws Exception {
            mvc.perform(get("/premiacoes/nao-existe/ranking"))
                    .andExpect(status().isNotFound())
                    .andExpect(content().string(containsString("Premiação não encontrada")));
            mvc.perform(get("/premiacoes/nao-existe/andamento.json")).andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Telão da premiação e ranking no celular")
    class Telao {

        @Test
        @WithMockUser(username = "organizador", roles = "ORGANIZADOR")
        @DisplayName("Aberta: cédulas ao vivo, sem pódio nem pontos")
        void aberta() throws Exception {
            PremiacaoCadastrada cadastro = craque(false);

            mvc.perform(get(url(cadastro, "/telao")))
                    .andExpect(status().isOk())
                    .andExpect(view().name("premiacao/telao"))
                    .andExpect(content().string(allOf(containsString("<body class=\"telao\">"),
                            containsString("data-telao=\"premiacao\""),
                            containsString("de <span data-aptos>8</span> cédulas recebidas"),
                            containsString("/premiacoes/" + cadastro.id() + "/andamento.json"),
                            not(containsString("data-podio-telao")), not(containsString(" pts")))));
            assertEquals(StatusPremiacao.ABERTA, cadastro.premiacao().status());
        }

        @Test
        @WithMockUser(username = "organizador", roles = "ORGANIZADOR")
        @DisplayName("Apurada: pódio com os passos do 3º ao 1º e o botão do ranking completo")
        void apurada() throws Exception {
            PremiacaoCadastrada cadastro = craque(true);

            mvc.perform(get(url(cadastro, "/telao")))
                    .andExpect(content().string(allOf(containsString("data-podio-telao"),
                            containsString("degrau degrau-ouro\" data-passo=\"2\""),
                            containsString("degrau degrau-bronze\" data-passo=\"0\""),
                            containsString("36 pts"), containsString("data-proximo"),
                            containsString("Ver ranking completo"))));
        }

        @Test
        @WithMockUser(username = "organizador", roles = "ORGANIZADOR")
        @DisplayName("Encerrada aguarda a apuração; sem cédulas, o telão avisa que não há pódio")
        void encerradaESemCedulas() throws Exception {
            PremiacaoCadastrada encerrada = craque(false);
            encerrada.premiacao().encerrar();
            mvc.perform(get(url(encerrada, "/telao")))
                    .andExpect(content().string(containsString("Premiação encerrada. Aguardando apuração…")));

            PremiacaoTop3 vazia = new PremiacaoTop3("Vazia", "", List.of("A", "B", "C"),
                    List.of(new CategoriaEleitor(TORCEDORES, 1)), Map.of("e1", TORCEDORES));
            vazia.abrir();
            vazia.encerrar();
            vazia.apurar();
            mvc.perform(get(url(premiacoes.salvar(vazia), "/telao")))
                    .andExpect(content().string(containsString("Nenhuma cédula registrada")));
        }

        @Test
        @WithMockUser(username = "t1", roles = "ELEITOR")
        @DisplayName("Eleitor, mesmo apto, recebe 403")
        void eleitor() throws Exception {
            mvc.perform(get(url(craque(true), "/telao"))).andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(username = "organizador", roles = "ORGANIZADOR")
        @DisplayName("Premiação inexistente dá 404")
        void inexistente() throws Exception {
            mvc.perform(get("/premiacoes/nao-existe/telao")).andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser(username = "organizador", roles = "ORGANIZADOR")
        @DisplayName("Ranking: botão do telão e cartões para o celular com pontos por grupo")
        void cartoes() throws Exception {
            PremiacaoCadastrada cadastro = craque(true);

            mvc.perform(get(url(cadastro, "/ranking")))
                    .andExpect(content().string(allOf(
                            containsString("href=\"/premiacoes/" + cadastro.id() + "/telao\""),
                            containsString("class=\"ranking-cartoes\""),
                            containsString("card cartao-ranking cartao-ranking-podio"),
                            containsString("Jornalistas:"),
                            containsString("<strong>26</strong>"),
                            containsString("ranking-tabela"))));
        }
    }
}

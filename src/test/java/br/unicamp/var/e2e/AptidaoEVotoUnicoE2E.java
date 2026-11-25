package br.unicamp.var.e2e;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import br.unicamp.var.apuracao.MaioriaSimples;
import br.unicamp.var.eleitorado.Eleitor;
import br.unicamp.var.eleitorado.Eleitorado;
import br.unicamp.var.processo.Votacao;
import br.unicamp.var.web.comum.RepositorioVotacoes;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import java.time.Clock;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * RNF-05 (só aptos votam) e RNF-02 (um voto por eleitor), no navegador.
 */
@DisplayName("RNF-05 e RNF-02: aptidão e voto único")
class AptidaoEVotoUnicoE2E extends E2EBase {

    @Autowired
    private RepositorioVotacoes votacoes;

    @Autowired
    private Clock relogio;

    @Test
    @DisplayName("eleitor fora da lista vê a página de não apto")
    void naoApto() {
        Votacao restrita = Votacao.nova()
                .pergunta("Votação só do Conselho Deliberativo")
                .opcoes(List.of("Sim", "Não"))
                .regra(new MaioriaSimples())
                .eleitorado(Eleitorado.de(List.of(new Eleitor("e01", "Ana Ribeiro"))))
                .relogio(relogio)
                .build();
        restrita.abrir();
        votacoes.salvar(restrita);

        Page pagina = novaPagina(CELULAR);
        entrar(pagina, "e02", SENHA_ELEITOR);
        pagina.navigate("/eleitor/votacoes/" + restrita.id());

        assertThat(pagina.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions()
                .setName("Você não está na lista de eleitores desta votação"))).isVisible();
        capturar(pagina, "01-nao-apto");
    }

    @Test
    @DisplayName("segundo voto (de outra aba) é recusado com a mensagem amigável")
    void segundoVotoRecusado() {
        Page aba1 = novaPagina(CELULAR);
        entrar(aba1, "e03", SENHA_ELEITOR);
        abrirCartao(aba1, "Eleição presidencial");
        aba1.getByLabel("Chapa Tradição").check(new Locator.CheckOptions().setForce(true));
        aba1.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Revisar voto")).click();

        Page aba2 = aba1.context().newPage();
        aba2.navigate(aba1.url().replace("/revisar", ""));
        aba2.getByLabel("Chapa Arquibancada").check(new Locator.CheckOptions().setForce(true));
        aba2.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Revisar voto")).click();
        aba2.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Confirmar voto")).click();
        assertThat(aba2.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setName("Voto registrado")))
                .isVisible();

        aba2.close();
        aba1.bringToFront();
        aba1.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Confirmar voto")).click();
        assertThat(aba1.getByText("Você já votou nesta votação.")).isVisible();
        capturar(aba1, "02-segundo-voto-recusado");
    }
}

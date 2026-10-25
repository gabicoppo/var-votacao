package br.unicamp.var.processo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import br.unicamp.var.apuracao.ContagemVotos;
import br.unicamp.var.apuracao.MaioriaAbsoluta;
import br.unicamp.var.apuracao.RegraApuracao;
import br.unicamp.var.apuracao2turno.ResultadoApuracao;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class VotacaoTest {

    private static final List<String> OPCOES = List.of("Gabriela", "Fabricio", "Juliana");
    private static final RegraApuracao SEM_VENCEDOR = contagem -> new ResultadoApuracao.SemVencedor();

    private static Votacao novaVotacao(RegraApuracao regra, int totalAptos) {
        return new Votacao("Quem deve ser o presidente?", OPCOES, regra, totalAptos);
    }

    private static Votacao rascunho() {
        return novaVotacao(SEM_VENCEDOR, 10);
    }

    private static Votacao aberta() {
        Votacao votacao = rascunho();
        votacao.abrir();
        return votacao;
    }

    private static Votacao encerrada() {
        Votacao votacao = aberta();
        votacao.encerrar();
        return votacao;
    }

    private static Votacao apurada() {
        Votacao votacao = encerrada();
        votacao.apurar();
        return votacao;
    }

    // Transições válidas

    @Test
    void votacaoCriadaFicaEmRascunho() {
        assertEquals("rascunho", rascunho().status());
    }

    @Test
    void abrirLevaRascunhoParaAberta() {
        assertEquals("aberta", aberta().status());
    }

    @Test
    void encerrarLevaAbertaParaEncerrada() {
        assertEquals("encerrada", encerrada().status());
    }

    @Test
    void apurarLevaEncerradaParaApurada() {
        assertEquals("apurada", apurada().status());
    }

    @Test
    void alterarRegraEmRascunhoMantemRascunho() {
        Votacao votacao = rascunho();

        votacao.alterarRegra(new MaioriaAbsoluta(false));

        assertEquals("rascunho", votacao.status());
    }

    @Test
    void registrarVotoEmVotacaoAbertaMantemAberta() {
        Votacao votacao = aberta();

        votacao.registrarVoto("Gabriela");

        assertEquals("aberta", votacao.status());
    }

    // Operações proibidas em rascunho

    @Test
    void naoRegistraVotoEmVotacaoRascunho() {
        IllegalStateException erro = assertThrows(IllegalStateException.class,
                () -> rascunho().registrarVoto("Gabriela"));

        assertEquals("não é possível registrar voto em votação rascunho", erro.getMessage());
    }

    @Test
    void naoEncerraVotacaoRascunho() {
        assertThrows(IllegalStateException.class, () -> rascunho().encerrar());
    }

    @Test
    void naoApuraVotacaoRascunho() {
        assertThrows(IllegalStateException.class, () -> rascunho().apurar());
    }

    // Operações proibidas com a votação aberta

    @Test
    void naoAlteraRegraComVotacaoAberta() {
        assertThrows(IllegalStateException.class, () -> aberta().alterarRegra(new MaioriaAbsoluta(true)));
    }

    @Test
    void naoAbreVotacaoJaAberta() {
        assertThrows(IllegalStateException.class, () -> aberta().abrir());
    }

    @Test
    void naoApuraVotacaoAberta() {
        assertThrows(IllegalStateException.class, () -> aberta().apurar());
    }

    // Operações proibidas com a votação encerrada

    @Test
    void naoAlteraRegraComVotacaoEncerrada() {
        assertThrows(IllegalStateException.class, () -> encerrada().alterarRegra(new MaioriaAbsoluta(true)));
    }

    @Test
    void naoAbreVotacaoEncerrada() {
        assertThrows(IllegalStateException.class, () -> encerrada().abrir());
    }

    @Test
    void naoRegistraVotoDepoisDeEncerrada() {
        assertThrows(IllegalStateException.class, () -> encerrada().registrarVoto("Gabriela"));
    }

    @Test
    void naoEncerraVotacaoJaEncerrada() {
        assertThrows(IllegalStateException.class, () -> encerrada().encerrar());
    }

    // Operações proibidas com a votação apurada

    @Test
    void naoAlteraRegraComVotacaoApurada() {
        assertThrows(IllegalStateException.class, () -> apurada().alterarRegra(new MaioriaAbsoluta(true)));
    }

    @Test
    void naoAbreVotacaoApurada() {
        assertThrows(IllegalStateException.class, () -> apurada().abrir());
    }

    @Test
    void naoRegistraVotoDepoisDeApurada() {
        assertThrows(IllegalStateException.class, () -> apurada().registrarVoto("Gabriela"));
    }

    @Test
    void naoEncerraVotacaoApurada() {
        assertThrows(IllegalStateException.class, () -> apurada().encerrar());
    }

    // Validações do construtor

    @Test
    void guardaPerguntaSemEspacosNasPontas() {
        Votacao votacao = new Votacao("  Quem deve ser o presidente?  ", OPCOES, SEM_VENCEDOR, 10);

        assertEquals("Quem deve ser o presidente?", votacao.pergunta());
    }

    @Test
    void guardaOpcoesNaOrdemDeCadastro() {
        assertEquals(OPCOES, rascunho().opcoes());
    }

    @Test
    void alteracaoNaListaOriginalNaoAfetaOpcoes() {
        List<String> opcoes = new ArrayList<>(OPCOES);
        Votacao votacao = new Votacao("Pergunta", opcoes, SEM_VENCEDOR, 10);

        opcoes.add("Joao");

        assertEquals(OPCOES, votacao.opcoes());
    }

    @Test
    void opcoesDevolvidasSaoImutaveis() {
        assertThrows(UnsupportedOperationException.class, () -> rascunho().opcoes().add("Joao"));
    }

    @Test
    void rejeitaPerguntaNula() {
        assertThrows(IllegalArgumentException.class, () -> new Votacao(null, OPCOES, SEM_VENCEDOR, 10));
    }

    @Test
    void rejeitaPerguntaVazia() {
        assertThrows(IllegalArgumentException.class, () -> new Votacao("   ", OPCOES, SEM_VENCEDOR, 10));
    }

    @Test
    void rejeitaOpcoesNulas() {
        assertThrows(IllegalArgumentException.class, () -> new Votacao("Pergunta", null, SEM_VENCEDOR, 10));
    }

    @Test
    void rejeitaMenosDeDuasOpcoes() {
        assertThrows(IllegalArgumentException.class,
                () -> new Votacao("Pergunta", List.of("Gabriela"), SEM_VENCEDOR, 10));
    }

    @Test
    void rejeitaOpcaoVazia() {
        assertThrows(IllegalArgumentException.class,
                () -> new Votacao("Pergunta", List.of("Gabriela", " "), SEM_VENCEDOR, 10));
    }

    @Test
    void rejeitaOpcaoNula() {
        assertThrows(IllegalArgumentException.class,
                () -> new Votacao("Pergunta", Arrays.asList("Gabriela", null), SEM_VENCEDOR, 10));
    }

    @Test
    void rejeitaOpcaoRepetida() {
        assertThrows(IllegalArgumentException.class,
                () -> new Votacao("Pergunta", List.of("Gabriela", "Fabricio", "Gabriela"), SEM_VENCEDOR, 10));
    }

    @Test
    void rejeitaRegraNula() {
        assertThrows(IllegalArgumentException.class, () -> new Votacao("Pergunta", OPCOES, null, 10));
    }

    @Test
    void rejeitaTotalAptosZero() {
        assertThrows(IllegalArgumentException.class, () -> new Votacao("Pergunta", OPCOES, SEM_VENCEDOR, 0));
    }

    @Test
    void rejeitaTotalAptosNegativo() {
        assertThrows(IllegalArgumentException.class, () -> new Votacao("Pergunta", OPCOES, SEM_VENCEDOR, -1));
    }

    // Registro de votos

    @Test
    void naoRegistraVotoEmOpcaoInexistente() {
        assertThrows(IllegalArgumentException.class, () -> aberta().registrarVoto("Joao"));
    }

    @Test
    void naoRegistraVotoAlemDoTotalDeAptos() {
        Votacao votacao = novaVotacao(SEM_VENCEDOR, 2);
        votacao.abrir();
        votacao.registrarVoto("Gabriela");
        votacao.registrarVoto("Fabricio");

        assertThrows(IllegalStateException.class, () -> votacao.registrarVoto("Juliana"));
    }

    // Apuração

    @Test
    void apurarDelegaARegraComAContagemDosVotos() {
        List<ContagemVotos> recebidas = new ArrayList<>();
        ResultadoApuracao esperado = new ResultadoApuracao.Eleito("Gabriela");
        Votacao votacao = novaVotacao(contagem -> {
            recebidas.add(contagem);
            return esperado;
        }, 10);
        votacao.abrir();
        votacao.registrarVoto("Gabriela");
        votacao.registrarVoto("Gabriela");
        votacao.registrarVoto("Juliana");
        votacao.encerrar();

        ResultadoApuracao resultado = votacao.apurar();

        assertSame(esperado, resultado);
        assertEquals(List.of(new ContagemVotos(Map.of("Gabriela", 2, "Fabricio", 0, "Juliana", 1), 10)), recebidas);
    }

    @Test
    void apurarDeNovoDevolveOMesmoResultadoSemReapurar() {
        List<ContagemVotos> recebidas = new ArrayList<>();
        Votacao votacao = novaVotacao(contagem -> {
            recebidas.add(contagem);
            return new ResultadoApuracao.SemVencedor();
        }, 10);
        votacao.abrir();
        votacao.encerrar();

        ResultadoApuracao primeiro = votacao.apurar();
        ResultadoApuracao segundo = votacao.apurar();

        assertSame(primeiro, segundo);
        assertEquals(1, recebidas.size());
    }

    @Test
    void alterarRegraEmRascunhoTrocaARegraUsadaNaApuracao() {
        ResultadoApuracao esperado = new ResultadoApuracao.QuorumNaoAtingido();
        Votacao votacao = rascunho();
        votacao.alterarRegra(contagem -> esperado);
        votacao.abrir();
        votacao.encerrar();

        assertSame(esperado, votacao.apurar());
    }

    @Test
    void naoAlteraRegraParaNula() {
        assertThrows(IllegalArgumentException.class, () -> rascunho().alterarRegra(null));
    }

    @Test
    void votacaoDePontaAPontaComMaioriaAbsoluta() {
        Votacao votacao = novaVotacao(new MaioriaAbsoluta(true), 10);
        votacao.abrir();
        for (String voto : List.of("Gabriela", "Gabriela", "Gabriela", "Fabricio", "Fabricio", "Juliana")) {
            votacao.registrarVoto(voto);
        }
        votacao.encerrar();

        // Gabriela tem exatamente 50% dos votos válidos: não é maioria absoluta

        assertEquals(new ResultadoApuracao.SegundoTurno("Gabriela", "Fabricio"), votacao.apurar());
        assertEquals("apurada", votacao.status());
    }
}

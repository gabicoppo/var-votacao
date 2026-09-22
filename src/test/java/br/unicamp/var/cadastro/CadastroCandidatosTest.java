package br.unicamp.var.cadastro;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CadastroCandidatosTest {

    private CadastroCandidatos cadastro;

    @BeforeEach
    void criarCadastro() {
        cadastro = new CadastroCandidatos();
    }

    @Test
    void cadastraCandidatoEEncontraPeloCodigo() {
        Candidato cadastrado = cadastro.cadastrar("Ana Souza", "10");

        assertEquals(new Candidato("Ana Souza", "10"), cadastrado);
        assertEquals(Optional.of(cadastrado), cadastro.buscarPorCodigo("10"));
    }

    @Test
    void rejeitaCodigoDuplicadoSemAlterarOCadastro() {
        cadastro.cadastrar("Ana Souza", "10");

        assertThrows(IllegalArgumentException.class, () -> cadastro.cadastrar("Bruno Lima", "10"));

        assertEquals(List.of(new Candidato("Ana Souza", "10")), cadastro.listar());
    }

    @Test
    void trataCodigoComEspacosComoDuplicado() {
        cadastro.cadastrar("Ana Souza", "10");

        assertThrows(IllegalArgumentException.class, () -> cadastro.cadastrar("Bruno Lima", " 10 "));
    }

    @Test
    void permiteNomesRepetidosComCodigosDiferentes() {
        cadastro.cadastrar("Ana Souza", "10");
        cadastro.cadastrar("Ana Souza", "11");

        assertEquals(2, cadastro.listar().size());
    }

    @Test
    void diferenciaCodigosPorMaiusculasEMinusculas() {
        cadastro.cadastrar("Ana Souza", "P-01");
        cadastro.cadastrar("Bruno Lima", "p-01");

        assertEquals(2, cadastro.listar().size());
        assertEquals("Bruno Lima", cadastro.buscarPorCodigo("p-01").orElseThrow().nome());
    }

    @Test
    void nomeInvalidoNaoCadastraNada() {
        assertThrows(IllegalArgumentException.class, () -> cadastro.cadastrar("  ", "10"));

        assertTrue(cadastro.listar().isEmpty());
    }

    @Test
    void buscaPorCodigoInexistenteRetornaVazio() {
        cadastro.cadastrar("Ana Souza", "10");

        assertTrue(cadastro.buscarPorCodigo("99").isEmpty());
    }

    @Test
    void buscaComCodigoNuloRetornaVazio() {
        assertTrue(cadastro.buscarPorCodigo(null).isEmpty());
    }

    @Test
    void buscaIgnoraEspacosNasPontas() {
        cadastro.cadastrar("Ana Souza", "10");

        assertTrue(cadastro.buscarPorCodigo("  10 ").isPresent());
    }

    @Test
    void listarSemCandidatosRetornaListaVazia() {
        assertTrue(cadastro.listar().isEmpty());
    }

    @Test
    void listarMantemAOrdemDeCadastro() {
        Candidato primeiro = cadastro.cadastrar("Ana Souza", "20");
        Candidato segundo = cadastro.cadastrar("Bruno Lima", "10");
        Candidato terceiro = cadastro.cadastrar("Carla Dias", "30");

        assertEquals(List.of(primeiro, segundo, terceiro), cadastro.listar());
    }

    @Test
    void listaRetornadaNaoPermiteAlteracao() {
        cadastro.cadastrar("Ana Souza", "10");
        List<Candidato> lista = cadastro.listar();

        assertThrows(UnsupportedOperationException.class, () -> lista.add(new Candidato("Intruso", "99")));
    }
}
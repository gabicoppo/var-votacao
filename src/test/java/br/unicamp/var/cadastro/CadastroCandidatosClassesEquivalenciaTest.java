package br.unicamp.var.cadastro;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Teste funcional de {@link CadastroCandidatos} por particionamento em classes
 * de equivalência (Atividade 5, C1). Entradas: {@code nome} e {@code codigo}
 * de {@code cadastrar}, o {@code codigo} de {@code buscarPorCodigo} e o estado
 * do cadastro.
 *
 * <pre>
 * | ID   | Operação        | Classe de equivalência                                       | Válida?  |
 * |------|-----------------|--------------------------------------------------------------|----------|
 * | CE11 | cadastrar       | código novo → cadastrado                                     | válida   |
 * | CE12 | cadastrar       | código já existente → rejeitado                              | inválida |
 * | CE13 | cadastrar       | código igual a um existente após normalização → rejeitado    | inválida |
 * | CE14 | cadastrar       | código difere só por maiúsculas/minúsculas → aceito como     | válida   |
 * |      |                 | outro código (decisão atual, documentada no javadoc)         |          |
 * | CE15 | cadastrar       | mesmo nome com código diferente → aceito (homônimos)         | válida   |
 * | CE16 | cadastrar       | dado inválido → rejeitado e o cadastro não muda              | inválida |
 * | CE17 | buscarPorCodigo | código existente → candidato encontrado                      | válida   |
 * | CE18 | buscarPorCodigo | código existente com espaços nas bordas → encontrado         | válida   |
 * | CE19 | buscarPorCodigo | código inexistente → vazio                                   | válida   |
 * | CE20 | buscarPorCodigo | null → vazio, sem exceção                                    | válida   |
 * | CE21 | listar          | cadastro vazio → lista vazia                                 | válida   |
 * | CE22 | listar          | candidatos cadastrados → lista na ordem de inserção          | válida   |
 * | CE23 | listar          | a lista devolvida é imutável                                 | válida   |
 * </pre>
 */
class CadastroCandidatosClassesEquivalenciaTest {

    private CadastroCandidatos cadastro;

    @BeforeEach
    void criarCadastro() {
        cadastro = new CadastroCandidatos();
    }

    @Nested
    @DisplayName("Operação: cadastrar")
    class OperacaoCadastrar {

        @Test
        @DisplayName("CE11 (válida): código novo é cadastrado e encontrado")
        void ce11CadastraCandidatoComCodigoNovo() {
            Candidato cadastrado = cadastro.cadastrar("Ana Souza", "10");

            assertEquals(new Candidato("Ana Souza", "10"), cadastrado);
            assertEquals(Optional.of(cadastrado), cadastro.buscarPorCodigo("10"));
        }

        @Test
        @DisplayName("CE12 (inválida): código já existente é rejeitado")
        void ce12RejeitaCodigoDuplicado() {
            cadastro.cadastrar("Ana Souza", "10");

            assertThrows(IllegalArgumentException.class, () -> cadastro.cadastrar("Bruno Lima", "10"));

            assertEquals(List.of(new Candidato("Ana Souza", "10")), cadastro.listar());
        }

        @Test
        @DisplayName("CE13 (inválida): código igual a um existente após normalização é rejeitado")
        void ce13RejeitaCodigoDuplicadoComEspacosNasBordas() {
            cadastro.cadastrar("Ana Souza", "10");

            assertThrows(IllegalArgumentException.class, () -> cadastro.cadastrar("Bruno Lima", " 10 "));
        }

        @Test
        @DisplayName("CE14 (válida): código que difere só por maiúsculas/minúsculas é aceito como outro código")
        void ce14DiferenciaCodigosPorMaiusculasEMinusculas() {
            // Decisão atual do sistema, documentada no javadoc de CadastroCandidatos:
            // a comparação de códigos é exata, diferenciando maiúsculas de minúsculas.
            cadastro.cadastrar("Ana Souza", "P-01");
            cadastro.cadastrar("Bruno Lima", "p-01");

            assertEquals(2, cadastro.listar().size());
            assertEquals("Bruno Lima", cadastro.buscarPorCodigo("p-01").orElseThrow().nome());
        }

        @Test
        @DisplayName("CE15 (válida): mesmo nome com código diferente é aceito (homônimos)")
        void ce15PermiteNomesRepetidosComCodigosDiferentes() {
            cadastro.cadastrar("Ana Souza", "10");
            cadastro.cadastrar("Ana Souza", "11");

            assertEquals(2, cadastro.listar().size());
        }

        @ParameterizedTest
        @CsvSource(nullValues = "null", value = {"'  ', 10", "null, 10", "'Ana Souza', ''", "'Ana Souza', null"})
        @DisplayName("CE16 (inválida): dado inválido é rejeitado e o cadastro não muda")
        void ce16DadoInvalidoNaoCadastraNada(String nome, String codigo) {
            assertThrows(IllegalArgumentException.class, () -> cadastro.cadastrar(nome, codigo));

            assertTrue(cadastro.listar().isEmpty());
        }
    }

    @Nested
    @DisplayName("Operação: buscarPorCodigo")
    class OperacaoBuscarPorCodigo {

        @Test
        @DisplayName("CE17 (válida): código existente encontra o candidato")
        void ce17EncontraCandidatoPorCodigoExistente() {
            Candidato cadastrado = cadastro.cadastrar("Ana Souza", "10");

            assertEquals(Optional.of(cadastrado), cadastro.buscarPorCodigo("10"));
        }

        @Test
        @DisplayName("CE18 (válida): código existente com espaços nas bordas encontra o candidato")
        void ce18BuscaIgnoraEspacosNasBordas() {
            cadastro.cadastrar("Ana Souza", "10");

            assertTrue(cadastro.buscarPorCodigo("  10 ").isPresent());
        }

        @Test
        @DisplayName("CE19 (válida): código inexistente retorna vazio")
        void ce19BuscaPorCodigoInexistenteRetornaVazio() {
            cadastro.cadastrar("Ana Souza", "10");

            assertTrue(cadastro.buscarPorCodigo("99").isEmpty());
        }

        @Test
        @DisplayName("CE20 (válida): código nulo retorna vazio, sem exceção")
        void ce20BuscaComCodigoNuloRetornaVazio() {
            assertTrue(cadastro.buscarPorCodigo(null).isEmpty());
        }
    }

    @Nested
    @DisplayName("Operação: listar")
    class OperacaoListar {

        @Test
        @DisplayName("CE21 (válida): cadastro vazio retorna lista vazia")
        void ce21ListarSemCandidatosRetornaListaVazia() {
            assertTrue(cadastro.listar().isEmpty());
        }

        @Test
        @DisplayName("CE22 (válida): lista mantém a ordem de cadastro")
        void ce22ListarMantemAOrdemDeCadastro() {
            Candidato primeiro = cadastro.cadastrar("Ana Souza", "20");
            Candidato segundo = cadastro.cadastrar("Bruno Lima", "10");
            Candidato terceiro = cadastro.cadastrar("Carla Dias", "30");

            assertEquals(List.of(primeiro, segundo, terceiro), cadastro.listar());
        }

        @Test
        @DisplayName("CE23 (válida): a lista devolvida é imutável")
        void ce23ListaRetornadaNaoPermiteAlteracao() {
            cadastro.cadastrar("Ana Souza", "10");
            List<Candidato> lista = cadastro.listar();
            Candidato intruso = new Candidato("Intruso", "99");

            assertThrows(UnsupportedOperationException.class, () -> lista.add(intruso));
        }
    }
}

package br.unicamp.var.web.premiacao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import br.unicamp.var.eleitorado.Eleitor;
import br.unicamp.var.top3.CategoriaEleitor;
import br.unicamp.var.top3.PremiacaoTop3;
import br.unicamp.var.top3.StatusPremiacao;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Teste de {@link ConversorFormularioPremiacao} e {@link FormularioPremiacao} por classes de
 * equivalência e valores limite (história #23).
 *
 * <pre>
 * | ID   | Campo       | Classe                                                              | Válida?  |
 * |------|-------------|---------------------------------------------------------------------|----------|
 * | CE1  | titulo      | com texto                                                           | válida   |
 * | CE2  | titulo      | vazio ou só espaços                                                 | inválida |
 * | CE3  | candidatos  | 3 ou mais distintos (limite: exatamente 3), linhas em branco ignoradas | válida |
 * | CE4  | candidatos  | menos de 3 (limite: 2)                                              | inválida |
 * | CE5  | candidatos  | repetido, inclusive com outra caixa                                 | inválida |
 * | CE6  | categorias  | Nome;peso com peso inteiro, vírgula ou ponto decimal                | válida   |
 * | CE7  | categorias  | nenhuma linha                                                       | inválida |
 * | CE8  | categorias  | formato errado, nome vazio, peso não numérico, peso 0, repetida     | inválida |
 * | CE9  | eleitores   | id;nome;grupo com grupo existente (qualquer caixa)                  | válida   |
 * | CE10 | eleitores   | nenhuma linha                                                       | inválida |
 * | CE11 | eleitores   | formato errado, id/nome/grupo vazio, grupo inexistente, id repetido | inválida |
 * </pre>
 */
class ConversorFormularioPremiacaoTest {

    private FormularioPremiacao f;

    @BeforeEach
    void preencherValido() {
        f = new FormularioPremiacao();
        f.setTitulo("  Craque do Campeonato ");
        f.setDescricao(" Entrega ao time ");
        f.setCandidatos("Ana\n\nBia\r\nCaio\n");
        f.setCategorias("Jornalistas;2\nTorcedores;1,0");
        f.setEleitores("e01;Ana Ribeiro;Jornalistas\n e02 ; Bruno ; torcedores\n\ne03;Carla;TORCEDORES");
    }

    private ErroCampo unicoErro() {
        List<ErroCampo> erros = ConversorFormularioPremiacao.validar(f);
        assertEquals(1, erros.size(), () -> "erros: " + erros);
        return erros.getFirst();
    }

    private void assertErro(String campo, String mensagem) {
        assertEquals(new ErroCampo(campo, mensagem), unicoErro());
    }

    @Nested
    @DisplayName("Formulário válido")
    class Valido {

        @Test
        @DisplayName("CE1, CE3, CE6, CE9: converte na premiação em rascunho, com os grupos canônicos")
        void converte() {
            assertEquals(List.of(), ConversorFormularioPremiacao.validar(f));

            PremiacaoTop3 premiacao = ConversorFormularioPremiacao.paraPremiacao(f);

            assertEquals("Craque do Campeonato", premiacao.titulo());
            assertEquals("Entrega ao time", premiacao.descricao());
            assertEquals(List.of("Ana", "Bia", "Caio"), premiacao.candidatos());
            assertEquals(List.of(new CategoriaEleitor("Jornalistas", 2), new CategoriaEleitor("Torcedores", 1)),
                    premiacao.categorias());
            assertEquals(3, premiacao.totalEleitores());
            assertEquals(Optional.of(new CategoriaEleitor("Torcedores", 1)), premiacao.categoriaDe("e03"));
            assertEquals(StatusPremiacao.RASCUNHO, premiacao.status());
        }

        @Test
        @DisplayName("CE9: devolve os eleitores sem espaços nas pontas, na ordem digitada")
        void eleitores() {
            assertEquals(List.of(new Eleitor("e01", "Ana Ribeiro"), new Eleitor("e02", "Bruno"),
                    new Eleitor("e03", "Carla")), ConversorFormularioPremiacao.eleitores(f));
        }

        @ParameterizedTest
        @CsvSource(delimiter = '|', value = {"G;2|2.0", "G;1,5|1.5", "G;1.5|1.5", "G ; 0,25 |0.25", "G;007|7.0"})
        @DisplayName("CE6: aceita peso inteiro, com vírgula ou com ponto decimal")
        void pesos(String linha, double esperado) {
            f.setCategorias(linha);
            f.setEleitores("e01;Ana;G");

            assertEquals(esperado, ConversorFormularioPremiacao.paraPremiacao(f).categorias().getFirst().peso());
        }

        @Test
        @DisplayName("Getters nunca devolvem nulo")
        void settersNulos() {
            FormularioPremiacao vazio = new FormularioPremiacao();
            vazio.setTitulo(null);
            vazio.setDescricao(null);
            vazio.setCandidatos(null);
            vazio.setCategorias(null);
            vazio.setEleitores(null);

            assertEquals(List.of("", "", "", "", ""), List.of(vazio.getTitulo(), vazio.getDescricao(),
                    vazio.getCandidatos(), vazio.getCategorias(), vazio.getEleitores()));
        }

        @Test
        @DisplayName("Formulário vazio tem um erro em cada campo obrigatório, na ordem da tela")
        void vazio() {
            assertEquals(List.of("titulo", "candidatos", "categorias", "eleitores"),
                    ConversorFormularioPremiacao.validar(new FormularioPremiacao()).stream()
                            .map(ErroCampo::campo).toList());
        }

        @Test
        @DisplayName("Converter um formulário inválido lança exceção")
        void converterInvalido() {
            f.setTitulo("");

            assertThrows(IllegalArgumentException.class, () -> ConversorFormularioPremiacao.paraPremiacao(f));
            assertThrows(IllegalArgumentException.class, () -> ConversorFormularioPremiacao.eleitores(f));
        }
    }

    @Nested
    @DisplayName("Título e candidatos")
    class TituloECandidatos {

        @ParameterizedTest
        @ValueSource(strings = {"", "   "})
        @DisplayName("CE2: título vazio")
        void tituloVazio(String titulo) {
            f.setTitulo(titulo);
            assertErro("titulo", "Informe o título da premiação.");
        }

        @Test
        @DisplayName("CE4: dois candidatos (limite inferior inválido)")
        void doisCandidatos() {
            f.setCandidatos("Ana\n \nBia");
            assertErro("candidatos", "Informe pelo menos três candidatos, um por linha.");
        }

        @Test
        @DisplayName("CE5: candidato repetido com outra caixa, citando as duas linhas")
        void candidatoRepetido() {
            f.setCandidatos("Ana\nBia\n\nana\nCaio");
            assertErro("candidatos", "Linha 4: o candidato \"ana\" já apareceu na linha 1.");
        }
    }

    @Nested
    @DisplayName("Grupos e pesos")
    class Categorias {

        @Test
        @DisplayName("CE7: nenhum grupo")
        void nenhum() {
            f.setCategorias("\n  \n");
            List<ErroCampo> erros = ConversorFormularioPremiacao.validar(f);

            assertEquals(List.of(new ErroCampo("categorias",
                    "Informe pelo menos um grupo de votantes, no formato Nome;peso (ex.: Torcedores;1).")), erros);
        }

        @ParameterizedTest
        @CsvSource(delimiter = '|', value = {
            "Jornalistas|Linha 1: use o formato Nome;peso, com um só ponto e vírgula (ex.: Jornalistas;2).",
            "Jornalistas;2;3|Linha 1: use o formato Nome;peso, com um só ponto e vírgula (ex.: Jornalistas;2).",
            " ;2|Linha 1: informe o nome do grupo antes do ponto e vírgula.",
            "Jornalistas;dois|Linha 1: o peso \"dois\" não é um número. Use, por exemplo, 2 ou 1,5.",
            "Jornalistas;-1|Linha 1: o peso \"-1\" não é um número. Use, por exemplo, 2 ou 1,5.",
            "Jornalistas;|Linha 1: o peso \"\" não é um número. Use, por exemplo, 2 ou 1,5.",
            "Jornalistas;0|Linha 1: o peso deve ser maior que zero, para o voto do grupo contar.",
            "Jornalistas;0,0|Linha 1: o peso deve ser maior que zero, para o voto do grupo contar."
        })
        @DisplayName("CE8: linha de grupo inválida, com o número da linha")
        void linhaInvalida(String linha, String mensagem) {
            f.setCategorias(linha);
            assertErro("categorias", mensagem);
        }

        @Test
        @DisplayName("CE8: grupo repetido com outra caixa, contando as linhas em branco")
        void repetido() {
            f.setCategorias("Jornalistas;2\n\nTorcedores;1\njornalistas;3");
            assertErro("categorias", "Linha 4: o grupo \"jornalistas\" já apareceu na linha 1.");
        }
    }

    @Nested
    @DisplayName("Eleitores")
    class Eleitores {

        @Test
        @DisplayName("CE10: nenhum eleitor")
        void nenhum() {
            f.setEleitores(" ");
            assertErro("eleitores",
                    "Informe pelo menos um eleitor, no formato id;nome;grupo (ex.: e01;Ana Ribeiro;Torcedores).");
        }

        @ParameterizedTest
        @CsvSource(delimiter = '|', value = {
            "e01;Ana|Linha 1: use o formato id;nome;grupo, com dois pontos e vírgulas "
                    + "(ex.: e01;Ana Ribeiro;Torcedores).",
            "e01;Ana;Torcedores;x|Linha 1: use o formato id;nome;grupo, com dois pontos e vírgulas "
                    + "(ex.: e01;Ana Ribeiro;Torcedores).",
            " ;Ana;Torcedores|Linha 1: informe o id do eleitor (o login dele).",
            "e01; ;Torcedores|Linha 1: informe o nome do eleitor.",
            "e01;Ana; |Linha 1: informe o grupo do eleitor.",
            "e01;Ana;Imprensa|Linha 1: o grupo \"Imprensa\" não existe. Grupos cadastrados: Jornalistas, Torcedores."
        })
        @DisplayName("CE11: linha de eleitor inválida, com o número da linha")
        void linhaInvalida(String linha, String mensagem) {
            f.setEleitores(linha);
            assertErro("eleitores", mensagem);
        }

        @Test
        @DisplayName("CE11: id repetido depois de tirar os espaços")
        void idRepetido() {
            f.setEleitores("e01;Ana;Torcedores\ne02;Bia;Torcedores\n e01 ;Outra;Jornalistas");
            assertErro("eleitores", "Linha 3: o id \"e01\" já apareceu na linha 1.");
        }

        @Test
        @DisplayName("Com os grupos inválidos, o grupo do eleitor não é conferido (um erro só)")
        void naoConfereGrupoSemCategorias() {
            f.setCategorias("Jornalistas;0");
            f.setEleitores("e01;Ana;Qualquer");

            assertEquals(List.of("categorias"),
                    ConversorFormularioPremiacao.validar(f).stream().map(ErroCampo::campo).toList());
        }
    }
}

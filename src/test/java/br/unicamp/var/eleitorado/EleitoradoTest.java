package br.unicamp.var.eleitorado;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Testes de {@link Eleitorado} (história #24, RNF-02). */
class EleitoradoTest {

    private static final Eleitor ANA = new Eleitor("1", "Ana");
    private static final Eleitor BRUNO = new Eleitor("2", "Bruno");
    private static final Eleitor CARLA = new Eleitor("3", "Carla");

    private static Eleitorado tres() {
        return Eleitorado.de(List.of(ANA, BRUNO, CARLA));
    }

    @Nested
    @DisplayName("Operação: de")
    class OperacaoDe {

        @Test
        @DisplayName("lista válida gera eleitorado com o total e a ordem recebidos")
        void criaEleitoradoValido() {
            Eleitorado eleitorado = tres();

            assertEquals(3, eleitorado.total());
            assertEquals(List.of(ANA, BRUNO, CARLA), eleitorado.listar());
        }

        @Test
        @DisplayName("um único eleitor é suficiente")
        void aceitaUmEleitor() {
            assertEquals(1, Eleitorado.de(List.of(ANA)).total());
        }

        @Test
        @DisplayName("lista nula é rejeitada")
        void rejeitaListaNula() {
            assertThrows(IllegalArgumentException.class, () -> Eleitorado.de(null));
        }

        @Test
        @DisplayName("lista vazia é rejeitada")
        void rejeitaListaVazia() {
            IllegalArgumentException erro =
                    assertThrows(IllegalArgumentException.class, () -> Eleitorado.de(List.of()));
            assertEquals("deve haver ao menos um eleitor", erro.getMessage());
        }

        @Test
        @DisplayName("eleitor nulo na lista é rejeitado")
        void rejeitaEleitorNulo() {
            List<Eleitor> comNulo = Arrays.asList(ANA, null);

            assertThrows(IllegalArgumentException.class, () -> Eleitorado.de(comNulo));
        }

        @Test
        @DisplayName("ids repetidos são rejeitados, mesmo com nomes diferentes")
        void rejeitaIdRepetido() {
            List<Eleitor> repetidos = List.of(ANA, new Eleitor("1", "Outra Ana"));

            IllegalArgumentException erro =
                    assertThrows(IllegalArgumentException.class, () -> Eleitorado.de(repetidos));
            assertEquals("id de eleitor repetido: 1", erro.getMessage());
        }

        @Test
        @DisplayName("ids iguais depois do strip são repetidos")
        void rejeitaIdRepetidoAposStrip() {
            List<Eleitor> repetidos = List.of(ANA, new Eleitor("  1 ", "Outra Ana"));

            assertThrows(IllegalArgumentException.class, () -> Eleitorado.de(repetidos));
        }

        @Test
        @DisplayName("ids que diferem só por maiúsculas são eleitores diferentes")
        void diferenciaMaiusculas() {
            Eleitorado eleitorado = Eleitorado.de(List.of(new Eleitor("a1", "Ana"), new Eleitor("A1", "Ana")));

            assertEquals(2, eleitorado.total());
        }

        @Test
        @DisplayName("alterar a lista original depois não muda o eleitorado")
        void copiaListaRecebida() {
            List<Eleitor> lista = new ArrayList<>(List.of(ANA, BRUNO));
            Eleitorado eleitorado = Eleitorado.de(lista);

            lista.add(CARLA);

            assertEquals(2, eleitorado.total());
            assertFalse(eleitorado.ehApto("3"));
        }
    }

    @Nested
    @DisplayName("Operação: importar")
    class OperacaoImportar {

        @Test
        @DisplayName("texto válido gera um eleitor por linha, na ordem")
        void importaTextoValido() {
            Eleitorado eleitorado = Eleitorado.importar("1;Ana\n2;Bruno\n3;Carla");

            assertEquals(List.of(ANA, BRUNO, CARLA), eleitorado.listar());
        }

        @Test
        @DisplayName("linhas em branco e espaços nas bordas são ignorados")
        void ignoraLinhasEmBrancoEEspacos() {
            String texto = "\n  1 ; Ana  \n\n   \n\t2;Bruno\t\n";

            assertEquals(List.of(ANA, BRUNO), Eleitorado.importar(texto).listar());
        }

        @Test
        @DisplayName("aceita quebras de linha do Windows (CRLF)")
        void aceitaCrlf() {
            assertEquals(List.of(ANA, BRUNO), Eleitorado.importar("1;Ana\r\n2;Bruno\r\n").listar());
        }

        @Test
        @DisplayName("texto nulo é rejeitado")
        void rejeitaTextoNulo() {
            assertThrows(IllegalArgumentException.class, () -> Eleitorado.importar(null));
        }

        @ParameterizedTest(name = "texto sem eleitores: \"{0}\"")
        @ValueSource(strings = {"", "   ", "\n\n", " \n \t \n"})
        @DisplayName("texto sem nenhum eleitor é rejeitado")
        void rejeitaTextoSemEleitores(String texto) {
            IllegalArgumentException erro =
                    assertThrows(IllegalArgumentException.class, () -> Eleitorado.importar(texto));
            assertEquals("deve haver ao menos um eleitor", erro.getMessage());
        }

        @Test
        @DisplayName("linha sem ';' é rejeitada com o número da linha")
        void rejeitaLinhaSemSeparador() {
            IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                    () -> Eleitorado.importar("1;Ana\n2 Bruno"));
            assertEquals("linha 2: formato esperado id;nome", erro.getMessage());
        }

        @Test
        @DisplayName("linha com mais de um ';' é rejeitada")
        void rejeitaLinhaComCamposDemais() {
            IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                    () -> Eleitorado.importar("1;Ana;ana@exemplo.com"));
            assertEquals("linha 1: formato esperado id;nome", erro.getMessage());
        }

        @Test
        @DisplayName("id vazio é rejeitado com o número da linha")
        void rejeitaIdVazio() {
            IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                    () -> Eleitorado.importar("1;Ana\n  ;Bruno"));
            assertEquals("linha 2: id do eleitor nao pode ser vazio", erro.getMessage());
        }

        @Test
        @DisplayName("nome vazio é rejeitado com o número da linha")
        void rejeitaNomeVazio() {
            IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                    () -> Eleitorado.importar("1;Ana\n2;"));
            assertEquals("linha 2: nome do eleitor nao pode ser vazio", erro.getMessage());
        }

        @Test
        @DisplayName("id repetido (inclusive após strip) é rejeitado com o número da linha")
        void rejeitaIdRepetido() {
            IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                    () -> Eleitorado.importar("1;Ana\n2;Bruno\n 1 ;Outra Ana"));
            assertEquals("linha 3: id de eleitor repetido: 1", erro.getMessage());
        }

        @Test
        @DisplayName("o número da linha conta também as linhas em branco")
        void numeroDaLinhaContaLinhasEmBranco() {
            IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                    () -> Eleitorado.importar("\n1;Ana\n\n\nsem-separador"));
            assertTrue(erro.getMessage().startsWith("linha 5: "), erro.getMessage());
        }
    }

    @Nested
    @DisplayName("Operações: ehApto e buscar")
    class OperacoesConsulta {

        @Test
        @DisplayName("eleitor da lista é apto e encontrado")
        void eleitorConhecido() {
            Eleitorado eleitorado = tres();

            assertTrue(eleitorado.ehApto("2"));
            assertEquals(Optional.of(BRUNO), eleitorado.buscar("2"));
        }

        @Test
        @DisplayName("id com espaços nas bordas é normalizado")
        void idComEspacos() {
            Eleitorado eleitorado = tres();

            assertTrue(eleitorado.ehApto("  2 "));
            assertEquals(Optional.of(BRUNO), eleitorado.buscar("\t2\n"));
        }

        @Test
        @DisplayName("id desconhecido não é apto nem encontrado")
        void eleitorDesconhecido() {
            Eleitorado eleitorado = tres();

            assertFalse(eleitorado.ehApto("99"));
            assertEquals(Optional.empty(), eleitorado.buscar("99"));
        }

        @Test
        @DisplayName("null não é apto e a busca devolve vazio, sem exceção")
        void idNulo() {
            Eleitorado eleitorado = tres();

            assertFalse(eleitorado.ehApto(null));
            assertEquals(Optional.empty(), eleitorado.buscar(null));
        }
    }

    @Nested
    @DisplayName("Operação: listar")
    class OperacaoListar {

        @Test
        @DisplayName("a lista devolvida é imutável")
        void listaImutavel() {
            List<Eleitor> lista = tres().listar();

            assertThrows(UnsupportedOperationException.class, () -> lista.add(new Eleitor("4", "Davi")));
            assertThrows(UnsupportedOperationException.class, () -> lista.remove(0));
        }
    }
}

package br.unicamp.var.urna;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.unicamp.var.apuracao.ContagemVotos;
import br.unicamp.var.eleitorado.Eleitor;
import br.unicamp.var.eleitorado.Eleitorado;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.random.RandomGenerator;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/** Testes de {@link Urna} (histórias #22 e #28, RNF-01 e RNF-02). */
class UrnaTest {

    private static final Instant AGORA = Instant.parse("2026-11-24T12:00:00Z");
    private static final Clock RELOGIO = Clock.fixed(AGORA, ZoneOffset.UTC);
    private static final List<String> OPCOES = List.of("Chapa Azul", "Chapa Branca", "Chapa Verde");
    private static final Eleitorado ELEITORADO = Eleitorado.importar("carla;Carla\nana;Ana\nbruno;Bruno\ndavi;Davi");
    private static final String FORMATO_CODIGO = "[A-HJ-NP-Z2-9]{10}";

    private static Urna urna(Sigilo sigilo) {
        return new Urna(ELEITORADO, OPCOES, sigilo, RELOGIO);
    }

    private static Urna secreta() {
        return urna(Sigilo.SECRETO);
    }

    private static Eleitorado eleitoradoCom(int total) {
        return Eleitorado.de(IntStream.rangeClosed(1, total)
                .mapToObj(i -> new Eleitor("e" + i, "Eleitor " + i))
                .toList());
    }

    /** Gerador que devolve 0 nas primeiras chamadas e 1 depois, para forçar colisão de código. */
    private static final class GeradorComColisao implements RandomGenerator {
        private final int chamadasComZero;
        private int chamadas;

        GeradorComColisao(int chamadasComZero) {
            this.chamadasComZero = chamadasComZero;
        }

        @Override
        public int nextInt(int limite) {
            return chamadas++ < chamadasComZero ? 0 : 1;
        }

        @Override
        public long nextLong() {
            throw new UnsupportedOperationException();
        }
    }

    @Nested
    @DisplayName("Construção")
    class Construcao {

        @Test
        @DisplayName("urna nova começa vazia, com zero votos em cada opção")
        void urnaNovaVazia() {
            Urna urna = secreta();

            assertEquals(0, urna.totalVotos());
            assertEquals(Map.of("Chapa Azul", 0, "Chapa Branca", 0, "Chapa Verde", 0), urna.votosPorOpcao());
            assertTrue(urna.quemVotou().isEmpty());
        }

        @Test
        @DisplayName("opções perdem os espaços nas pontas")
        void normalizaOpcoes() {
            Urna urna = new Urna(ELEITORADO, List.of(" Sim ", "Nao\t"), Sigilo.SECRETO, RELOGIO);

            assertEquals(List.of("Sim", "Nao"), List.copyOf(urna.votosPorOpcao().keySet()));
        }

        @Test
        @DisplayName("argumentos nulos são rejeitados")
        void rejeitaNulos() {
            assertThrows(IllegalArgumentException.class, () -> new Urna(null, OPCOES, Sigilo.SECRETO, RELOGIO));
            assertThrows(IllegalArgumentException.class, () -> new Urna(ELEITORADO, null, Sigilo.SECRETO, RELOGIO));
            assertThrows(IllegalArgumentException.class, () -> new Urna(ELEITORADO, OPCOES, null, RELOGIO));
            assertThrows(IllegalArgumentException.class, () -> new Urna(ELEITORADO, OPCOES, Sigilo.SECRETO, null));
            assertThrows(IllegalArgumentException.class,
                    () -> new Urna(ELEITORADO, OPCOES, Sigilo.SECRETO, RELOGIO, null));
        }

        @Test
        @DisplayName("menos de duas opções é rejeitado")
        void rejeitaMenosDeDuasOpcoes() {
            assertThrows(IllegalArgumentException.class,
                    () -> new Urna(ELEITORADO, List.of(), Sigilo.SECRETO, RELOGIO));
            assertThrows(IllegalArgumentException.class,
                    () -> new Urna(ELEITORADO, List.of("Sim"), Sigilo.SECRETO, RELOGIO));
        }

        @Test
        @DisplayName("opção nula ou em branco é rejeitada")
        void rejeitaOpcaoVazia() {
            assertThrows(IllegalArgumentException.class,
                    () -> new Urna(ELEITORADO, Arrays.asList("Sim", null), Sigilo.SECRETO, RELOGIO));
            assertThrows(IllegalArgumentException.class,
                    () -> new Urna(ELEITORADO, List.of("Sim", "  "), Sigilo.SECRETO, RELOGIO));
        }

        @Test
        @DisplayName("opção repetida, inclusive após strip, é rejeitada")
        void rejeitaOpcaoRepetida() {
            IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                    () -> new Urna(ELEITORADO, List.of("Sim", "Nao", " Sim "), Sigilo.SECRETO, RELOGIO));
            assertEquals("opcao repetida: Sim", erro.getMessage());
        }

        @ParameterizedTest(name = "sigilo {0}")
        @EnumSource(Sigilo.class)
        @DisplayName("a urna informa o sigilo configurado")
        void informaSigilo(Sigilo sigilo) {
            assertEquals(sigilo, urna(sigilo).sigilo());
        }
    }

    @Nested
    @DisplayName("Operação: depositar")
    class OperacaoDepositar {

        @Test
        @DisplayName("voto válido é contado e o eleitor passa a constar como votante")
        void votoValido() {
            Urna urna = secreta();

            Comprovante comprovante = urna.depositar("ana", "Chapa Branca");

            assertEquals(1, urna.totalVotos());
            assertEquals(1, urna.votosPorOpcao().get("Chapa Branca"));
            assertTrue(urna.jaVotou("ana"));
            assertFalse(urna.jaVotou("bruno"));
            assertTrue(urna.comprovanteValido(comprovante.codigo()));
        }

        @Test
        @DisplayName("eleitor e opção com espaços nas pontas são normalizados")
        void normalizaEleitorEOpcao() {
            Urna urna = secreta();

            urna.depositar("  ana ", "\tChapa Branca  ");

            assertTrue(urna.jaVotou("ana"));
            assertEquals(Set.of("ana"), urna.quemVotou());
            assertEquals(1, urna.votosPorOpcao().get("Chapa Branca"));
        }

        @Test
        @DisplayName("eleitor não apto é rejeitado e nada muda")
        void rejeitaNaoApto() {
            Urna urna = secreta();

            IllegalArgumentException erro =
                    assertThrows(IllegalArgumentException.class, () -> urna.depositar("zeca", "Chapa Azul"));

            assertEquals("eleitor nao apto: zeca", erro.getMessage());
            assertEquals(0, urna.totalVotos());
            assertEquals(0, urna.votosPorOpcao().get("Chapa Azul"));
        }

        @Test
        @DisplayName("opção inexistente é rejeitada e o eleitor continua podendo votar")
        void rejeitaOpcaoInexistente() {
            Urna urna = secreta();

            IllegalArgumentException erro =
                    assertThrows(IllegalArgumentException.class, () -> urna.depositar("ana", "Chapa Roxa"));

            assertEquals("opcao inexistente: Chapa Roxa", erro.getMessage());
            assertFalse(urna.jaVotou("ana"));
            urna.depositar("ana", "Chapa Azul");
            assertEquals(1, urna.totalVotos());
        }

        @Test
        @DisplayName("eleitor ou opção nulos são rejeitados")
        void rejeitaNulos() {
            Urna urna = secreta();

            assertThrows(IllegalArgumentException.class, () -> urna.depositar(null, "Chapa Azul"));
            assertThrows(IllegalArgumentException.class, () -> urna.depositar("ana", null));
            assertEquals(0, urna.totalVotos());
        }

        @Test
        @DisplayName("voto duplicado lança IllegalStateException e mantém o primeiro voto (RNF-02)")
        void rejeitaVotoDuplicado() {
            Urna urna = urna(Sigilo.IDENTIFICADO);
            urna.depositar("ana", "Chapa Azul");

            IllegalStateException erro =
                    assertThrows(IllegalStateException.class, () -> urna.depositar("ana", "Chapa Verde"));

            assertEquals("eleitor ja votou: ana", erro.getMessage());
            assertEquals(1, urna.totalVotos());
            assertEquals(Map.of("Chapa Azul", 1, "Chapa Branca", 0, "Chapa Verde", 0), urna.votosPorOpcao());
            assertEquals(Optional.of("Chapa Azul"), urna.votoDe("ana"));
        }

        @Test
        @DisplayName("voto duplicado com espaços no id também é rejeitado")
        void rejeitaVotoDuplicadoComEspacos() {
            Urna urna = secreta();
            urna.depositar("ana", "Chapa Azul");

            assertThrows(IllegalStateException.class, () -> urna.depositar(" ana ", "Chapa Azul"));
            assertEquals(1, urna.totalVotos());
        }
    }

    @Nested
    @DisplayName("Operação: votosPorOpcao")
    class OperacaoVotosPorOpcao {

        @Test
        @DisplayName("inclui as opções com zero votos")
        void incluiZeros() {
            Urna urna = secreta();
            urna.depositar("ana", "Chapa Verde");
            urna.depositar("bruno", "Chapa Verde");

            assertEquals(Map.of("Chapa Azul", 0, "Chapa Branca", 0, "Chapa Verde", 2), urna.votosPorOpcao());
        }

        @Test
        @DisplayName("mantém a ordem das opções recebida no construtor")
        void mantemOrdemDasOpcoes() {
            List<String> opcoes = List.of("Zeca", "Ana", "Maria", "Bruno");
            Urna urna = new Urna(ELEITORADO, opcoes, Sigilo.SECRETO, RELOGIO);
            urna.depositar("ana", "Bruno");

            assertEquals(opcoes, List.copyOf(urna.votosPorOpcao().keySet()));
        }

        @Test
        @DisplayName("o mapa devolvido é imutável e não muda com votos posteriores")
        void mapaImutavel() {
            Urna urna = secreta();
            Map<String, Integer> antes = urna.votosPorOpcao();

            urna.depositar("ana", "Chapa Azul");

            assertThrows(UnsupportedOperationException.class, () -> antes.put("Chapa Azul", 99));
            assertEquals(0, antes.get("Chapa Azul"));
        }
    }

    @Nested
    @DisplayName("Operações: quemVotou e jaVotou")
    class OperacaoQuemVotou {

        @Test
        @DisplayName("sai em ordem alfabética, não na ordem dos votos (RNF-01)")
        void ordemAlfabetica() {
            Urna urna = secreta();
            urna.depositar("davi", "Chapa Azul");
            urna.depositar("bruno", "Chapa Branca");
            urna.depositar("carla", "Chapa Verde");
            urna.depositar("ana", "Chapa Azul");

            assertEquals(List.of("ana", "bruno", "carla", "davi"), List.copyOf(urna.quemVotou()));
        }

        @Test
        @DisplayName("o conjunto devolvido é imutável")
        void conjuntoImutavel() {
            Urna urna = secreta();
            urna.depositar("ana", "Chapa Azul");

            Set<String> quemVotou = urna.quemVotou();

            assertThrows(UnsupportedOperationException.class, () -> quemVotou.add("bruno"));
            assertThrows(UnsupportedOperationException.class, () -> quemVotou.remove("ana"));
        }

        @Test
        @DisplayName("jaVotou com null ou desconhecido devolve false")
        void jaVotouNuloOuDesconhecido() {
            Urna urna = secreta();
            urna.depositar("ana", "Chapa Azul");

            assertFalse(urna.jaVotou(null));
            assertFalse(urna.jaVotou("zeca"));
            assertTrue(urna.jaVotou(" ana "));
        }
    }

    @Nested
    @DisplayName("Operação: votoDe (#22)")
    class OperacaoVotoDe {

        @Test
        @DisplayName("em votação SECRETO lança IllegalStateException (RNF-01)")
        void secretoLancaExcecao() {
            Urna urna = secreta();
            urna.depositar("ana", "Chapa Azul");

            assertThrows(IllegalStateException.class, () -> urna.votoDe("ana"));
            assertThrows(IllegalStateException.class, () -> urna.votoDe("bruno"));
            assertThrows(IllegalStateException.class, () -> urna.votoDe(null));
        }

        @Test
        @DisplayName("em votação IDENTIFICADO devolve a opção de cada eleitor")
        void identificadoDevolveOpcao() {
            Urna urna = urna(Sigilo.IDENTIFICADO);
            urna.depositar("ana", "Chapa Azul");
            urna.depositar("bruno", " Chapa Verde ");

            assertEquals(Optional.of("Chapa Azul"), urna.votoDe("ana"));
            assertEquals(Optional.of("Chapa Verde"), urna.votoDe("  bruno "));
        }

        @Test
        @DisplayName("em votação IDENTIFICADO, eleitor que não votou ou id nulo devolve vazio")
        void identificadoSemVoto() {
            Urna urna = urna(Sigilo.IDENTIFICADO);
            urna.depositar("ana", "Chapa Azul");

            assertEquals(Optional.empty(), urna.votoDe("bruno"));
            assertEquals(Optional.empty(), urna.votoDe("zeca"));
            assertEquals(Optional.empty(), urna.votoDe(null));
        }
    }

    @Nested
    @DisplayName("Comprovante (#28)")
    class ComprovanteDaUrna {

        @Test
        @DisplayName("registradoEm vem do Clock injetado")
        void horarioDoRelogio() {
            assertEquals(AGORA, secreta().depositar("ana", "Chapa Azul").registradoEm());
        }

        @Test
        @DisplayName("código tem 10 caracteres maiúsculos, sem 0, O, 1 nem I")
        void formatoDoCodigo() {
            String codigo = secreta().depositar("ana", "Chapa Azul").codigo();

            assertTrue(codigo.matches(FORMATO_CODIGO), codigo);
        }

        @Test
        @DisplayName("1000 votos geram 1000 códigos únicos, todos validáveis")
        void codigosUnicos() {
            Eleitorado mil = eleitoradoCom(1000);
            Urna urna = new Urna(mil, OPCOES, Sigilo.SECRETO, RELOGIO);
            Set<String> codigos = new HashSet<>();

            for (Eleitor eleitor : mil.listar()) {
                Comprovante comprovante = urna.depositar(eleitor.id(), OPCOES.get(0));
                assertTrue(codigos.add(comprovante.codigo()), "codigo repetido: " + comprovante.codigo());
                assertTrue(comprovante.codigo().matches(FORMATO_CODIGO));
            }

            assertEquals(1000, codigos.size());
            assertTrue(codigos.stream().allMatch(urna::comprovanteValido));
        }

        @Test
        @DisplayName("código não é derivado do eleitor nem da opção")
        void codigoNaoDerivado() {
            Urna urna1 = secreta();
            Urna urna2 = secreta();

            String codigo1 = urna1.depositar("ana", "Chapa Azul").codigo();
            String codigo2 = urna2.depositar("ana", "Chapa Azul").codigo();

            assertNotEquals(codigo1, codigo2);
        }

        @Test
        @DisplayName("se o código sorteado já foi emitido, sorteia outro")
        void colisaoGeraOutroCodigo() {
            Urna urna = new Urna(ELEITORADO, OPCOES, Sigilo.SECRETO, RELOGIO, new GeradorComColisao(20));

            Comprovante primeiro = urna.depositar("ana", "Chapa Azul");
            Comprovante segundo = urna.depositar("bruno", "Chapa Azul");

            assertEquals("AAAAAAAAAA", primeiro.codigo());
            assertEquals("BBBBBBBBBB", segundo.codigo());
        }

        @Test
        @DisplayName("comprovanteValido aceita espaços e minúsculas digitados pelo eleitor")
        void validaCodigoDigitado() {
            Urna urna = secreta();
            String codigo = urna.depositar("ana", "Chapa Azul").codigo();

            assertTrue(urna.comprovanteValido("  " + codigo.toLowerCase() + " "));
        }

        @Test
        @DisplayName("comprovanteValido com código desconhecido, de outra urna ou null devolve false")
        void codigoInvalido() {
            Urna urna = secreta();
            urna.depositar("ana", "Chapa Azul");
            String deOutraUrna = secreta().depositar("ana", "Chapa Azul").codigo();

            assertFalse(urna.comprovanteValido("ZZZZZZZZZZ"));
            assertFalse(urna.comprovanteValido(""));
            assertFalse(urna.comprovanteValido(deOutraUrna));
            assertFalse(urna.comprovanteValido(null));
        }
    }

    @Nested
    @DisplayName("Integridade com a apuração (RNF-02)")
    class Integridade {

        @Test
        @DisplayName("quando todos os aptos votam, a contagem vira uma ContagemVotos válida")
        void todosVotamGeraContagemValida() {
            Urna urna = urna(Sigilo.IDENTIFICADO);
            urna.depositar("ana", "Chapa Azul");
            urna.depositar("bruno", "Chapa Verde");
            urna.depositar("carla", "Chapa Azul");
            urna.depositar("davi", "Chapa Azul");

            ContagemVotos contagem = new ContagemVotos(urna.votosPorOpcao(), ELEITORADO.total());

            assertEquals(ELEITORADO.total(), urna.totalVotos());
            assertEquals(urna.totalVotos(), contagem.totalVotosValidos());
            assertEquals(urna.quemVotou().size(), contagem.totalVotosValidos());
            assertEquals(Map.of("Chapa Azul", 3, "Chapa Branca", 0, "Chapa Verde", 1), contagem.votosPorOpcao());
        }

        @Test
        @DisplayName("depois que todos votaram, nenhum voto a mais é aceito")
        void naoAceitaVotoAlemDosAptos() {
            Urna urna = secreta();
            for (Eleitor eleitor : ELEITORADO.listar()) {
                urna.depositar(eleitor.id(), "Chapa Branca");
            }

            for (Eleitor eleitor : ELEITORADO.listar()) {
                assertThrows(IllegalStateException.class, () -> urna.depositar(eleitor.id(), "Chapa Azul"));
            }
            assertThrows(IllegalArgumentException.class, () -> urna.depositar("zeca", "Chapa Azul"));
            assertEquals(ELEITORADO.total(), urna.totalVotos());
            assertEquals(ELEITORADO.total(), urna.votosPorOpcao().get("Chapa Branca"));
        }
    }
}

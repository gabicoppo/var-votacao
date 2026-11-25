package br.unicamp.var.web.comum;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Contas em memória: código de acesso só na criação, sem caracteres ambíguos, e senhas em BCrypt.
 */
class ContasUsuariosEmMemoriaTest {

    /** BCrypt com custo mínimo, para o teste ser rápido. */
    private final PasswordEncoder codificador = new BCryptPasswordEncoder(4);
    private final ContasUsuariosEmMemoria contas = new ContasUsuariosEmMemoria(codificador, false);

    @Nested
    @DisplayName("garantirContaEleitor")
    class GarantirContaEleitor {

        @Test
        @DisplayName("cria a conta e devolve o código só na primeira vez")
        void codigoSoNaCriacao() {
            Optional<String> codigo = contas.garantirContaEleitor("e21", "Ana Ribeiro");

            assertTrue(codigo.isPresent());
            assertEquals(Optional.empty(), contas.garantirContaEleitor("e21", "Outro Nome"));
            assertEquals(Optional.of("Ana Ribeiro"), contas.nomeDe("e21"));
        }

        @Test
        @DisplayName("o código entra como senha do eleitor, guardada em BCrypt")
        void codigoViraSenha() {
            String codigo = contas.garantirContaEleitor("e21", "Ana Ribeiro").orElseThrow();
            UserDetails usuario = contas.loadUserByUsername("e21");

            assertTrue(codificador.matches(codigo, usuario.getPassword()));
            assertNotEquals(codigo, usuario.getPassword());
            assertEquals(Set.of("ROLE_ELEITOR"), papeis(usuario));
        }

        @Test
        @DisplayName("código com 8 caracteres, só maiúsculas e dígitos, sem 0, O, 1, I e L")
        void codigoSemAmbiguos() {
            Set<String> codigos = new HashSet<>();
            for (int i = 0; i < 200; i++) {
                String codigo = contas.garantirContaEleitor("x" + i, "Eleitor " + i).orElseThrow();
                assertTrue(codigo.matches("[A-Z2-9]{8}"), codigo);
                assertFalse(codigo.matches(".*[01OIL].*"), codigo);
                codigos.add(codigo);
            }
            assertTrue(codigos.size() > 190, "códigos deveriam ser aleatórios");
        }

        @Test
        @DisplayName("espaços nas pontas do id e do nome são removidos")
        void removeEspacos() {
            contas.garantirContaEleitor("  e30 ", "  Bruno Carvalho ");

            assertEquals(Optional.of("Bruno Carvalho"), contas.nomeDe("e30"));
        }

        @Test
        @DisplayName("id ou nome vazios são recusados")
        void validacoes() {
            assertThrows(IllegalArgumentException.class, () -> contas.garantirContaEleitor(null, "Ana"));
            assertThrows(IllegalArgumentException.class, () -> contas.garantirContaEleitor(" ", "Ana"));
            assertThrows(IllegalArgumentException.class, () -> contas.garantirContaEleitor("e1", null));
            assertThrows(IllegalArgumentException.class, () -> contas.garantirContaEleitor("e1", ""));
        }
    }

    @Nested
    @DisplayName("Consulta e login")
    class Consulta {

        @Test
        @DisplayName("nomeDe com login nulo ou desconhecido dá vazio")
        void nomeDesconhecido() {
            assertEquals(Optional.empty(), contas.nomeDe(null));
            assertEquals(Optional.empty(), contas.nomeDe("ninguem"));
        }

        @Test
        @DisplayName("login desconhecido ou nulo lança UsernameNotFoundException")
        void loginDesconhecido() {
            assertThrows(UsernameNotFoundException.class, () -> contas.loadUserByUsername("ninguem"));
            assertThrows(UsernameNotFoundException.class, () -> contas.loadUserByUsername(null));
        }

        @Test
        @DisplayName("sem demonstração, não há conta nenhuma")
        void semDemonstracao() {
            assertEquals(Optional.empty(), contas.nomeDe("organizador"));
        }

        @Test
        @DisplayName("com demonstração: organizador/var2026 e e01..e12/1234")
        void comDemonstracao() {
            ContasUsuariosEmMemoria demo = new ContasUsuariosEmMemoria(codificador, true);

            UserDetails organizador = demo.loadUserByUsername("organizador");
            assertTrue(codificador.matches("var2026", organizador.getPassword()));
            assertEquals(Set.of("ROLE_ORGANIZADOR"), papeis(organizador));
            assertTrue(codificador.matches("1234", demo.loadUserByUsername("e12").getPassword()));
            assertEquals(Optional.empty(), demo.garantirContaEleitor("e05", "Outro"));
        }
    }

    private static Set<String> papeis(UserDetails usuario) {
        Set<String> papeis = new HashSet<>();
        for (GrantedAuthority autoridade : usuario.getAuthorities()) {
            papeis.add(autoridade.getAuthority());
        }
        return papeis;
    }

    @Nested
    @DisplayName("Senha do organizador (deploy público)")
    @ExtendWith(OutputCaptureExtension.class)
    class SenhaOrganizador {

        @Test
        @DisplayName("usa a senha configurada (VAR_SENHA_ORGANIZADOR), e não a padrão")
        void senhaConfigurada() {
            ContasUsuariosEmMemoria demo = new ContasUsuariosEmMemoria(codificador, true, "segredo-da-banca");

            String hash = demo.loadUserByUsername("organizador").getPassword();
            assertTrue(codificador.matches("segredo-da-banca", hash));
            assertFalse(codificador.matches(ContasUsuariosEmMemoria.SENHA_ORGANIZADOR_PADRAO, hash));
        }

        @Test
        @DisplayName("sem senha configurada, gera uma aleatória, escreve no log e não aceita a padrão")
        void senhaGerada(CapturedOutput saida) {
            ContasUsuariosEmMemoria demo = new ContasUsuariosEmMemoria(codificador, true, " ");

            Matcher gerada = Pattern.compile("senha gerada para o usuario organizador: ([A-Z2-9]{16})")
                    .matcher(saida.getAll());
            assertTrue(gerada.find(), "a senha gerada deve aparecer no log");
            String hash = demo.loadUserByUsername("organizador").getPassword();
            assertTrue(codificador.matches(gerada.group(1), hash));
            assertFalse(codificador.matches(ContasUsuariosEmMemoria.SENHA_ORGANIZADOR_PADRAO, hash));
        }

        @Test
        @DisplayName("senha nula também gera uma aleatória; os eleitores continuam com 1234")
        void senhaNula() {
            ContasUsuariosEmMemoria demo = new ContasUsuariosEmMemoria(codificador, true, null);

            assertFalse(codificador.matches(ContasUsuariosEmMemoria.SENHA_ORGANIZADOR_PADRAO,
                    demo.loadUserByUsername("organizador").getPassword()));
            assertTrue(codificador.matches("1234", demo.loadUserByUsername("e01").getPassword()));
        }
    }

    @Nested
    @DisplayName("cadastrar (cadastro público)")
    class Cadastrar {

        @Test
        @DisplayName("cria a conta só se o login for novo; campos obrigatórios")
        void cadastrar() {
            assertTrue(contas.cadastrar("org.novo", "Org Novo", "senhaforte", Papel.ORGANIZADOR));
            assertFalse(contas.cadastrar("org.novo", "Outro", "outra", Papel.ELEITOR));
            assertEquals(Set.of("ROLE_ORGANIZADOR"), papeis(contas.loadUserByUsername("org.novo")));
            assertThrows(IllegalArgumentException.class, () -> contas.cadastrar(" ", "Nome", "s", Papel.ELEITOR));
            assertThrows(IllegalArgumentException.class, () -> contas.cadastrar("x1", null, "s", Papel.ELEITOR));
            assertThrows(IllegalArgumentException.class, () -> contas.cadastrar("x1", "Nome", "", Papel.ELEITOR));
            assertThrows(IllegalArgumentException.class, () -> contas.cadastrar("x1", "Nome", "s", null));
        }
    }
}

package br.unicamp.var.web.comum;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Cadastro público de contas: validação de cada campo e criação da conta.
 */
class ServicoCadastroTest {

    private final PasswordEncoder codificador = new BCryptPasswordEncoder(4);
    private final ContasUsuariosEmMemoria contas = new ContasUsuariosEmMemoria(codificador, false);
    private final ServicoCadastro servico = new ServicoCadastro(contas);

    private static FormularioCadastro valido(String usuario, String papel) {
        return new FormularioCadastro("Marina Souza", usuario, "segredo123", "segredo123", papel);
    }

    @Nested
    @DisplayName("Conta criada")
    class Criada {

        @Test
        @DisplayName("organizador: conta com papel ORGANIZADOR, nome e senha em BCrypt")
        void organizador() {
            assertTrue(servico.cadastrar(valido("marina", "ORGANIZADOR")).isEmpty());

            UserDetails usuario = contas.loadUserByUsername("marina");
            assertTrue(codificador.matches("segredo123", usuario.getPassword()));
            assertEquals("ROLE_ORGANIZADOR", usuario.getAuthorities().iterator().next().getAuthority());
            assertEquals(Optional.of("Marina Souza"), contas.nomeDe("marina"));
        }

        @Test
        @DisplayName("eleitor: conta com papel ELEITOR; espaços nas pontas são removidos")
        void eleitor() {
            FormularioCadastro f = new FormularioCadastro("  Paulo Lima ", " a02 ", "segredo123", "segredo123",
                    "ELEITOR");

            assertTrue(servico.cadastrar(f).isEmpty());

            assertEquals("ROLE_ELEITOR", contas.loadUserByUsername("a02").getAuthorities().iterator().next()
                    .getAuthority());
            assertEquals(Optional.of("Paulo Lima"), contas.nomeDe("a02"));
        }

        @Test
        @DisplayName("usuário já existente é recusado, sem trocar a senha da conta original")
        void usuarioExistente() {
            servico.cadastrar(valido("marina", "ORGANIZADOR"));

            Map<String, String> erros = servico.cadastrar(new FormularioCadastro("Outra", "marina", "outrasenha",
                    "outrasenha", "ELEITOR"));

            assertEquals("Este usuário já existe. Escolha outro ou entre com a sua senha.", erros.get("usuario"));
            assertTrue(codificador.matches("segredo123", contas.loadUserByUsername("marina").getPassword()));
        }
    }

    @Nested
    @DisplayName("Validação")
    class Validacao {

        @Test
        @DisplayName("campos vazios: um erro para cada campo")
        void vazios() {
            Map<String, String> erros = servico.cadastrar(new FormularioCadastro(null, " ", null, null, null));

            assertEquals("Informe o seu nome.", erros.get("nome"));
            assertEquals("Escolha um usuário.", erros.get("usuario"));
            assertEquals("Use pelo menos 8 caracteres.", erros.get("senha"));
            assertEquals("Escolha o tipo de conta.", erros.get("papel"));
        }

        @ParameterizedTest(name = "\"{0}\"")
        @ValueSource(strings = {"ab", "joão", "com espaço", "a@b", "x234567890123456789012345678901"})
        @DisplayName("usuário fora do formato (tamanho, acento, espaço, símbolo)")
        void usuarioInvalido(String usuario) {
            assertTrue(servico.cadastrar(valido(usuario, "ELEITOR")).get("usuario").startsWith("Use de 3 a 30"));
        }

        @ParameterizedTest(name = "\"{0}\"")
        @ValueSource(strings = {"sistema", "Sistema", "SISTEMA"})
        @DisplayName("\"sistema\" é reservado (votações sem dono)")
        void reservado(String usuario) {
            assertEquals("Este usuário é reservado. Escolha outro.",
                    servico.cadastrar(valido(usuario, "ORGANIZADOR")).get("usuario"));
        }

        @Test
        @DisplayName("senha curta, confirmação diferente, nome longo e papel desconhecido")
        void outrosCampos() {
            assertEquals("Use pelo menos 8 caracteres.",
                    servico.cadastrar(new FormularioCadastro("A", "abc", "1234567", "1234567", "ELEITOR"))
                            .get("senha"));
            assertEquals("As senhas não são iguais.",
                    servico.cadastrar(new FormularioCadastro("A", "abc", "12345678", "12345679", "ELEITOR"))
                            .get("confirmar"));
            assertEquals("Use no máximo 80 caracteres.",
                    servico.cadastrar(new FormularioCadastro("x".repeat(81), "abc", "12345678", "12345678",
                            "ELEITOR")).get("nome"));
            assertEquals("Escolha o tipo de conta.",
                    servico.cadastrar(valido("abc", "ADMIN")).get("papel"));
            assertTrue(contas.nomeDe("abc").isEmpty());
        }

        @Test
        @DisplayName("o formulário vazio começa com eleitor marcado, e semSenhas apaga as senhas")
        void formulario() {
            assertEquals("ELEITOR", FormularioCadastro.vazio().papel());
            FormularioCadastro sem = valido("abc", "ELEITOR").semSenhas();
            assertEquals("", sem.senha());
            assertEquals("", sem.confirmar());
            assertEquals("abc", sem.usuario());
        }
    }
}

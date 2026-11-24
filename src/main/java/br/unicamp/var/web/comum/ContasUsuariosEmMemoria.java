package br.unicamp.var.web.comum;

import java.security.SecureRandom;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Contas em memória; também é o {@link UserDetailsService} do login (RNF-05). Senhas e códigos de
 * acesso são guardados só como hash BCrypt.
 *
 * <p>O código de acesso do eleitor tem {@value #TAMANHO_CODIGO} caracteres sorteados com
 * {@link SecureRandom} entre maiúsculas e dígitos, sem os ambíguos (0/O, 1/I/L), para ser ditado
 * ou digitado no celular sem confusão.</p>
 *
 * <p>Com {@code var.demo.ativo=true}, cria no início a conta {@code organizador} (senha
 * {@code var2026}) e os eleitores {@code e01} a {@code e12} (senha {@code 1234}), com nomes
 * fictícios.</p>
 */
@Service
public class ContasUsuariosEmMemoria implements ContasUsuarios, UserDetailsService {

    /** Tamanho do código de acesso gerado para o eleitor. */
    public static final int TAMANHO_CODIGO = 8;
    /** Alfabeto do código de acesso: sem 0, O, 1, I e L. */
    public static final String ALFABETO_CODIGO = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";

    private static final List<String> NOMES_DEMO = List.of(
            "Ana Ribeiro", "Bruno Carvalho", "Carla Mendes", "Diego Fonseca", "Elisa Martins",
            "Fábio Teixeira", "Gabriel Rocha", "Helena Duarte", "Igor Pacheco", "Joana Moreira",
            "Kátia Nogueira", "Lucas Barreto");

    private record Conta(String login, String nome, String hashSenha, Papel papel) {
    }

    private final Map<String, Conta> contas = new ConcurrentHashMap<>();
    private final PasswordEncoder codificador;
    private final SecureRandom aleatorio = new SecureRandom();

    /**
     * Cria o cadastro de contas, com as contas de demonstração se {@code demoAtivo}.
     *
     * @param codificador codificador de senhas (BCrypt)
     * @param demoAtivo   valor de {@code var.demo.ativo}
     */
    public ContasUsuariosEmMemoria(PasswordEncoder codificador, @Value("${var.demo.ativo:false}") boolean demoAtivo) {
        this.codificador = codificador;
        if (demoAtivo) {
            criarConta("organizador", "Organização do VAR", "var2026", Papel.ORGANIZADOR);
            for (int i = 0; i < NOMES_DEMO.size(); i++) {
                criarConta(String.format("e%02d", i + 1), NOMES_DEMO.get(i), "1234", Papel.ELEITOR);
            }
        }
    }

    private boolean criarConta(String login, String nome, String senha, Papel papel) {
        Conta conta = new Conta(login, nome, codificador.encode(senha), papel);
        return contas.putIfAbsent(login, conta) == null;
    }

    @Override
    public Optional<String> garantirContaEleitor(String eleitorId, String nome) {
        if (eleitorId == null || eleitorId.isBlank()) {
            throw new IllegalArgumentException("id do eleitor nao pode ser vazio");
        }
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("nome do eleitor nao pode ser vazio");
        }
        String login = eleitorId.strip();
        if (contas.containsKey(login)) {
            return Optional.empty();
        }
        String codigo = gerarCodigo();
        return criarConta(login, nome.strip(), codigo, Papel.ELEITOR) ? Optional.of(codigo) : Optional.empty();
    }

    @Override
    public Optional<String> nomeDe(String login) {
        return login == null ? Optional.empty() : Optional.ofNullable(contas.get(login)).map(Conta::nome);
    }

    @Override
    public UserDetails loadUserByUsername(String login) {
        Conta conta = login == null ? null : contas.get(login);
        if (conta == null) {
            throw new UsernameNotFoundException("conta inexistente");
        }
        return User.withUsername(conta.login())
                .password(conta.hashSenha())
                .authorities(conta.papel().authority())
                .build();
    }

    private String gerarCodigo() {
        StringBuilder codigo = new StringBuilder(TAMANHO_CODIGO);
        for (int i = 0; i < TAMANHO_CODIGO; i++) {
            codigo.append(ALFABETO_CODIGO.charAt(aleatorio.nextInt(ALFABETO_CODIGO.length())));
        }
        return codigo.toString();
    }
}

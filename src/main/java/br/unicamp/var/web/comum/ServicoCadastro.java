package br.unicamp.var.web.comum;

import br.unicamp.var.processo.Votacao;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

/**
 * Cadastro público de contas (organizador ou eleitor).
 *
 * <ul>
 *   <li><b>Organizador:</b> cria votações e premiações e vê e gerencia só as que criou
 *       ({@link Donos}, {@link AcessoPorDono}).</li>
 *   <li><b>Eleitor:</b> vota nas votações e premiações em que o seu usuário estiver na lista de aptos
 *       (RNF-05). Quem já recebeu um código de acesso do organizador não precisa se cadastrar.</li>
 * </ul>
 *
 * <p>Regras: nome obrigatório (até {@value #MAX_NOME} caracteres); usuário de
 * {@value #MIN_USUARIO} a {@value #MAX_USUARIO} letras, dígitos, ponto, hífen ou sublinhado, ainda
 * não usado e diferente de {@value Votacao#AUTOR_SISTEMA} (reservado); senha com pelo menos
 * {@value #MIN_SENHA} caracteres, repetida na confirmação.</p>
 */
@Service
public class ServicoCadastro {

    /** Tamanho mínimo da senha. */
    public static final int MIN_SENHA = 8;
    /** Tamanho mínimo do usuário. */
    public static final int MIN_USUARIO = 3;
    /** Tamanho máximo do usuário. */
    public static final int MAX_USUARIO = 30;
    /** Tamanho máximo do nome. */
    public static final int MAX_NOME = 80;

    private static final Pattern USUARIO = Pattern.compile("[A-Za-z0-9._-]{" + MIN_USUARIO + "," + MAX_USUARIO + "}");
    private static final String JA_EXISTE = "Este usuário já existe. Escolha outro ou entre com a sua senha.";

    private final ContasUsuarios contas;

    /**
     * Cria o serviço.
     *
     * @param contas contas de usuário
     */
    public ServicoCadastro(ContasUsuarios contas) {
        this.contas = contas;
    }

    /**
     * Valida e cria a conta.
     *
     * @param formulario dados digitados
     * @return os erros por campo ({@code nome}, {@code usuario}, {@code senha}, {@code confirmar},
     *         {@code papel}); vazio se a conta foi criada
     */
    public Map<String, String> cadastrar(FormularioCadastro formulario) {
        Map<String, String> erros = validar(formulario);
        if (!erros.isEmpty()) {
            return erros;
        }
        boolean criada = contas.cadastrar(formulario.usuario().strip(), formulario.nome().strip(), formulario.senha(),
                Papel.valueOf(formulario.papel()));
        if (!criada) {
            erros.put("usuario", JA_EXISTE);
        }
        return erros;
    }

    static Map<String, String> validar(FormularioCadastro f) {
        Map<String, String> erros = new LinkedHashMap<>();
        String nome = limpo(f.nome());
        String usuario = limpo(f.usuario());
        if (nome.isEmpty()) {
            erros.put("nome", "Informe o seu nome.");
        } else if (nome.length() > MAX_NOME) {
            erros.put("nome", "Use no máximo " + MAX_NOME + " caracteres.");
        }
        if (usuario.isEmpty()) {
            erros.put("usuario", "Escolha um usuário.");
        } else if (!USUARIO.matcher(usuario).matches()) {
            erros.put("usuario", "Use de " + MIN_USUARIO + " a " + MAX_USUARIO
                    + " letras sem acento, números, ponto, hífen ou sublinhado.");
        } else if (Votacao.AUTOR_SISTEMA.equals(usuario.toLowerCase(Locale.ROOT))) {
            erros.put("usuario", "Este usuário é reservado. Escolha outro.");
        }
        if (f.senha() == null || f.senha().length() < MIN_SENHA) {
            erros.put("senha", "Use pelo menos " + MIN_SENHA + " caracteres.");
        } else if (!f.senha().equals(f.confirmar())) {
            erros.put("confirmar", "As senhas não são iguais.");
        }
        if (!Papel.ORGANIZADOR.name().equals(f.papel()) && !Papel.ELEITOR.name().equals(f.papel())) {
            erros.put("papel", "Escolha o tipo de conta.");
        }
        return erros;
    }

    private static String limpo(String texto) {
        return texto == null ? "" : texto.strip();
    }
}

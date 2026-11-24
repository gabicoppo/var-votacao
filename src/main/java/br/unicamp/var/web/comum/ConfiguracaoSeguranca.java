package br.unicamp.var.web.comum;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Segurança da aplicação web (RNF-05): login por formulário em {@code /login}, logout por POST em
 * {@code /logout}, CSRF ativo e autorização por papel.
 *
 * <ul>
 *   <li>{@code /organizador/**}: só {@link Papel#ORGANIZADOR};</li>
 *   <li>{@code /eleitor/**}: só {@link Papel#ELEITOR};</li>
 *   <li>{@code /votacoes/**} e {@code /premiacoes/**}: qualquer usuário autenticado (cada controller
 *       confere se o eleitor é apto);</li>
 *   <li>página inicial, login, verificação de comprovante, guia de estilo, recursos estáticos e
 *       {@code /error}: públicos.</li>
 * </ul>
 */
@Configuration
public class ConfiguracaoSeguranca {

    /** Rotas acessíveis sem login. */
    static final String[] ROTAS_PUBLICAS = {
        "/", "/login", "/verificar", "/estilo", "/css/**", "/js/**", "/img/**", "/favicon.svg", "/error",
    };

    /**
     * Cadeia de filtros do Spring Security.
     *
     * @param http configurador do Spring Security
     * @return a cadeia configurada
     * @throws Exception se a configuração falhar
     */
    @Bean
    public SecurityFilterChain cadeiaDeSeguranca(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(regras -> regras
                        .requestMatchers(ROTAS_PUBLICAS).permitAll()
                        .requestMatchers("/organizador/**").hasRole(Papel.ORGANIZADOR.name())
                        .requestMatchers("/eleitor/**").hasRole(Papel.ELEITOR.name())
                        .anyRequest().authenticated())
                .formLogin(login -> login
                        .loginPage("/login")
                        .defaultSuccessUrl("/", true)
                        .permitAll())
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?saiu")
                        .permitAll());
        return http.build();
    }

    /**
     * Codificador de senhas e códigos de acesso.
     *
     * @return BCrypt com o custo padrão
     */
    @Bean
    public PasswordEncoder codificadorSenhas() {
        return new BCryptPasswordEncoder();
    }
}

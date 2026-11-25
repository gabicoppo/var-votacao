package br.unicamp.var.web.comum;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuração do Spring MVC: registra o isolamento por dono ({@link AcessoPorDono}).
 */
@Configuration
public class ConfiguracaoWeb implements WebMvcConfigurer {

    private final AcessoPorDono acessoPorDono;

    /**
     * Cria a configuração.
     *
     * @param acessoPorDono interceptador de acesso por dono
     */
    public ConfiguracaoWeb(AcessoPorDono acessoPorDono) {
        this.acessoPorDono = acessoPorDono;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registro) {
        registro.addInterceptor(acessoPorDono).addPathPatterns(AcessoPorDono.ROTAS);
    }
}

package br.unicamp.var.web.comum;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Beans de infraestrutura compartilhados pela aplicação web.
 */
@Configuration
public class ConfiguracaoAplicacao {

    /**
     * Relógio da aplicação. Serviços e votações recebem este {@link Clock} por injeção, para que os
     * testes possam trocá-lo por um relógio fixo.
     *
     * @return relógio do sistema no fuso {@link Fuso#ZONA}
     */
    @Bean
    public Clock relogio() {
        return Clock.system(Fuso.ZONA);
    }
}

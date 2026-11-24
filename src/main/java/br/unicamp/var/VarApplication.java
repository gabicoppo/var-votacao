package br.unicamp.var;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Aplicação web do VAR (Fase 2): Spring Boot + Thymeleaf como adaptador sobre o domínio.
 *
 * <p>O domínio ({@code br.unicamp.var.*} fora de {@code web}) continua em Java puro e não
 * conhece o Spring; a camada {@code br.unicamp.var.web} é o adaptador de entrada (portas e
 * adaptadores, previsto na A4). O agendamento atualiza os períodos das votações (história #20).</p>
 */
@SpringBootApplication
@EnableScheduling
public class VarApplication {

    /**
     * Sobe a aplicação.
     *
     * @param args argumentos de linha de comando repassados ao Spring
     */
    public static void main(String[] args) {
        SpringApplication.run(VarApplication.class, args);
    }
}

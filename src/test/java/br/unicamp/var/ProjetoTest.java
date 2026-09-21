package br.unicamp.var;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ProjetoTest {

    @Test
    void retornaNomeDoProjeto() {
        assertEquals("VAR - Votacao, Apuracao e Resultados", Projeto.nome());
    }
}

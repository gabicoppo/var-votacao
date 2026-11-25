package br.unicamp.var.web.organizador;

import java.time.Instant;
import java.util.List;

/**
 * Tudo o que a tela de gestão de uma votação mostra, já calculado (histórias #19 a #24, RNF-07).
 * Votos por opção só aparecem depois da apuração, nas telas de resultado; aqui fica só a
 * participação. A escolha de cada eleitor só é preenchida em votação identificada já encerrada
 * ou apurada (RNF-01).
 *
 * @param id                  id da votação
 * @param pergunta            pergunta
 * @param descricao           descrição, possivelmente vazia
 * @param status              "rascunho", "aberta", "encerrada" ou "apurada"
 * @param turno               1 ou 2
 * @param turnoAnteriorId     id do 1º turno, ou {@code null}
 * @param configuracao        regra, sigilo, período e aptos
 * @param votantes            quantos já votaram
 * @param aptos               tamanho da lista de aptos
 * @param situacao            situação da apuração ("Eleito: ..."), ou {@code null} antes dela
 * @param podeCriarSegundoTurno se a apuração pede 2º turno e ele ainda não existe
 * @param segundoTurnoId      id do 2º turno já criado, ou {@code null}
 * @param mostraEscolhas      se a lista de presença mostra a escolha de cada eleitor
 * @param presenca            lista de presença
 * @param auditoria           trilha de auditoria, em ordem cronológica
 */
public record DetalheVotacao(String id, String pergunta, String descricao, String status, int turno,
                             String turnoAnteriorId, Configuracao configuracao, int votantes, int aptos,
                             String situacao, boolean podeCriarSegundoTurno, String segundoTurnoId,
                             boolean mostraEscolhas, List<Presenca> presenca, List<LinhaAuditoria> auditoria) {

    /** Cópias defensivas das listas. */
    public DetalheVotacao {
        presenca = List.copyOf(presenca);
        auditoria = List.copyOf(auditoria);
    }

    /**
     * Participação, de 0 a 1.
     *
     * @return votantes / aptos
     */
    public double participacao() {
        return aptos == 0 ? 0 : (double) votantes / aptos;
    }

    /** @return se a ação "Abrir votação" aparece */
    public boolean podeAbrir() {
        return "rascunho".equals(status);
    }

    /** @return se a ação "Encerrar votação" aparece */
    public boolean podeEncerrar() {
        return "aberta".equals(status);
    }

    /** @return se a ação "Apurar" aparece */
    public boolean podeApurar() {
        return "encerrada".equals(status);
    }

    /** @return se os links de resultado e relatório aparecem */
    public boolean apurada() {
        return "apurada".equals(status);
    }

    /**
     * Configuração da votação.
     *
     * @param regra  descrição completa da regra
     * @param sigilo rótulo do sigilo
     * @param inicio início do período, ou {@code null}
     * @param fim    fim do período, ou {@code null}
     */
    public record Configuracao(String regra, String sigilo, Instant inicio, Instant fim) {

        /** @return se há período definido */
        public boolean temPeriodo() {
            return inicio != null;
        }
    }

    /**
     * Linha da lista de presença.
     *
     * @param eleitorId id do eleitor
     * @param nome      nome
     * @param votou     se já votou
     * @param escolha   opção votada, só em votação identificada encerrada ou apurada; senão {@code null}
     */
    public record Presenca(String eleitorId, String nome, boolean votou, String escolha) {
    }

    /**
     * Linha da auditoria.
     *
     * @param quando  instante do evento
     * @param autor   quem fez
     * @param evento  rótulo do evento
     * @param detalhe detalhe, possivelmente vazio
     */
    public record LinhaAuditoria(Instant quando, String autor, String evento, String detalhe) {
    }
}

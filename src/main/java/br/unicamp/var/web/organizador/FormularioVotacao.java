package br.unicamp.var.web.organizador;

import br.unicamp.var.urna.Sigilo;
import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * Dados digitados no formulário "Nova votação" (histórias #19, #20, #21, #22 e #24).
 *
 * <p>Classe mutável, com getters e setters, para o Spring fazer o <em>binding</em>. Guarda o
 * texto como foi digitado: a validação e a conversão ficam no {@link ConversorFormularioVotacao}.
 * Os campos de texto nunca ficam nulos: {@code null} vira texto vazio.</p>
 */
public class FormularioVotacao {

    private String pergunta = "";
    private String descricao = "";
    private String opcoes = "";
    private TipoRegra tipoRegra = TipoRegra.ABSOLUTA;
    private boolean segundoTurno = true;
    private String quorum = "50%";
    private String fracaoExigida = "2/3";
    private Sigilo sigilo = Sigilo.SECRETO;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime inicio;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime fim;
    private String eleitores = "";

    private static String texto(String valor) {
        return valor == null ? "" : valor;
    }

    /** @return a pergunta da votação */
    public String getPergunta() {
        return pergunta;
    }

    /** @param pergunta a pergunta da votação */
    public void setPergunta(String pergunta) {
        this.pergunta = texto(pergunta);
    }

    /** @return a descrição, opcional */
    public String getDescricao() {
        return descricao;
    }

    /** @param descricao a descrição, opcional */
    public void setDescricao(String descricao) {
        this.descricao = texto(descricao);
    }

    /** @return as opções, uma por linha */
    public String getOpcoes() {
        return opcoes;
    }

    /** @param opcoes as opções, uma por linha */
    public void setOpcoes(String opcoes) {
        this.opcoes = texto(opcoes);
    }

    /** @return a regra de apuração escolhida */
    public TipoRegra getTipoRegra() {
        return tipoRegra;
    }

    /** @param tipoRegra a regra de apuração escolhida */
    public void setTipoRegra(TipoRegra tipoRegra) {
        this.tipoRegra = tipoRegra;
    }

    /** @return se a maioria absoluta leva a 2º turno */
    public boolean isSegundoTurno() {
        return segundoTurno;
    }

    /** @param segundoTurno se a maioria absoluta leva a 2º turno */
    public void setSegundoTurno(boolean segundoTurno) {
        this.segundoTurno = segundoTurno;
    }

    /** @return o quórum mínimo, como fração ({@code 2/3}) ou percentual ({@code 50%}) */
    public String getQuorum() {
        return quorum;
    }

    /** @param quorum o quórum mínimo, como fração ou percentual */
    public void setQuorum(String quorum) {
        this.quorum = texto(quorum);
    }

    /** @return a fração exigida dos votos válidos, como fração ou percentual */
    public String getFracaoExigida() {
        return fracaoExigida;
    }

    /** @param fracaoExigida a fração exigida dos votos válidos, como fração ou percentual */
    public void setFracaoExigida(String fracaoExigida) {
        this.fracaoExigida = texto(fracaoExigida);
    }

    /** @return o sigilo da votação */
    public Sigilo getSigilo() {
        return sigilo;
    }

    /** @param sigilo o sigilo da votação */
    public void setSigilo(Sigilo sigilo) {
        this.sigilo = sigilo;
    }

    /** @return o início do período, no fuso do organizador; pode ser nulo */
    public LocalDateTime getInicio() {
        return inicio;
    }

    /** @param inicio o início do período, no fuso do organizador */
    public void setInicio(LocalDateTime inicio) {
        this.inicio = inicio;
    }

    /** @return o fim do período, no fuso do organizador; pode ser nulo */
    public LocalDateTime getFim() {
        return fim;
    }

    /** @param fim o fim do período, no fuso do organizador */
    public void setFim(LocalDateTime fim) {
        this.fim = fim;
    }

    /** @return a lista de aptos, no formato {@code id;nome} por linha */
    public String getEleitores() {
        return eleitores;
    }

    /** @param eleitores a lista de aptos, no formato {@code id;nome} por linha */
    public void setEleitores(String eleitores) {
        this.eleitores = texto(eleitores);
    }
}

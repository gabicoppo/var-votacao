package br.unicamp.var.web.comum;

import br.unicamp.var.apuracao.MaioriaAbsoluta;
import br.unicamp.var.apuracao.MaioriaQualificada;
import br.unicamp.var.apuracao.MaioriaSimples;
import br.unicamp.var.eleitorado.Eleitor;
import br.unicamp.var.eleitorado.Eleitorado;
import br.unicamp.var.processo.PeriodoVotacao;
import br.unicamp.var.processo.Votacao;
import br.unicamp.var.top3.CategoriaEleitor;
import br.unicamp.var.top3.CedulaTop3;
import br.unicamp.var.top3.PremiacaoTop3;
import br.unicamp.var.urna.Sigilo;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Dados de demonstração, carregados no início quando {@code var.demo.ativo=true}. Usa só a API
 * pública do domínio, com autor {@value #ORGANIZADOR} para a auditoria ficar realista (RNF-07).
 *
 * <p>Tudo é fictício (clube "Associação Atlética VAR", chapas e jogadores inventados). Cria:</p>
 * <ul>
 *   <li>eleição presidencial <b>aberta</b> (maioria absoluta com 2º turno, voto secreto), com votos de
 *       e05 a e11 sem maioria absoluta; e01 a e04 ainda podem votar;</li>
 *   <li>eleição do Conselho Fiscal <b>apurada</b> com resultado de 2º turno;</li>
 *   <li>assembleia de reforma do estatuto <b>apurada</b> (maioria qualificada, voto nominal), com 7 de 9
 *       votos em "Aprovar";</li>
 *   <li>enquete em <b>rascunho</b>, para abrir ao vivo;</li>
 *   <li>premiação "Craque do Campeonato 2026" <b>aberta</b>, com cédulas de e05 a e09.</li>
 * </ul>
 */
@Component
@ConditionalOnProperty(name = "var.demo.ativo", havingValue = "true")
public class DadosDemonstracao implements ApplicationRunner {

    /** Autor das operações de demonstração. */
    public static final String ORGANIZADOR = "organizador";
    /** Quantidade de eleitores da demonstração (e01 a e12). */
    public static final int TOTAL_ELEITORES = 12;

    static final String PRESIDENCIAL = "Eleição presidencial — Triênio 2027–2029";
    static final String CONSELHO = "Eleição do Conselho Fiscal";
    static final String ASSEMBLEIA = "Assembleia: reforma do art. 12 do estatuto";
    static final String ENQUETE = "Enquete: o gol da final foi golaço?";
    static final String CRAQUE = "Craque do Campeonato 2026";

    private static final String CLUBE = "Associação Atlética VAR";
    private static final List<String> CHAPAS = List.of("Chapa Tradição", "Chapa Renovação", "Chapa Arquibancada");
    private static final List<String> JOGADORES =
            List.of("Rafa Lima", "Davi Prado", "Caio Torres", "Léo Andrade", "Nico Sales");

    private final RepositorioVotacoes votacoes;
    private final RepositorioPremiacoes premiacoes;
    private final ContasUsuarios contas;
    private final Clock relogio;

    /**
     * Cria o carregador.
     *
     * @param votacoes   repositório de votações
     * @param premiacoes repositório de premiações
     * @param contas     contas, para usar os mesmos nomes dos eleitores e01 a e12
     * @param relogio    relógio da aplicação
     */
    public DadosDemonstracao(RepositorioVotacoes votacoes, RepositorioPremiacoes premiacoes,
                             ContasUsuarios contas, Clock relogio) {
        this.votacoes = votacoes;
        this.premiacoes = premiacoes;
        this.contas = contas;
        this.relogio = relogio;
    }

    @Override
    public void run(ApplicationArguments argumentos) {
        carregar();
    }

    /**
     * Cria as quatro votações e a premiação de demonstração.
     */
    public void carregar() {
        votacoes.salvar(eleicaoPresidencial());
        votacoes.salvar(conselhoFiscal());
        votacoes.salvar(assembleia());
        votacoes.salvar(enquete());
        premiacoes.salvar(craqueDoCampeonato(), ORGANIZADOR);
    }

    private Votacao eleicaoPresidencial() {
        Instant agora = relogio.instant();
        Votacao votacao = base(PRESIDENCIAL, CHAPAS)
                .descricao("Escolha da diretoria da " + CLUBE + " para o triênio 2027–2029. Voto secreto; "
                        + "se nenhuma chapa tiver mais da metade dos votos válidos, há 2º turno.")
                .regra(new MaioriaAbsoluta(true))
                .periodo(new PeriodoVotacao(agora.minus(Duration.ofDays(1)), agora.plus(Duration.ofDays(3))))
                .build();
        votacao.abrir(ORGANIZADOR);
        // e05..e11: 3 x 2 x 2, ninguém passa de 50%
        votar(votacao, 5, CHAPAS.get(0), CHAPAS.get(1), CHAPAS.get(0), CHAPAS.get(2), CHAPAS.get(1),
                CHAPAS.get(0), CHAPAS.get(2));
        return votacao;
    }

    private Votacao conselhoFiscal() {
        Votacao votacao = base(CONSELHO, List.of("Chapa Transparência", "Chapa Contas Abertas", "Chapa Fiscaliza"))
                .descricao("Escolha dos três membros do Conselho Fiscal da " + CLUBE + ".")
                .regra(new MaioriaAbsoluta(true))
                .build();
        votacao.abrir(ORGANIZADOR);
        // e01..e09: 4 x 3 x 2 → 2º turno entre as duas primeiras
        votar(votacao, 1, "Chapa Transparência", "Chapa Contas Abertas", "Chapa Transparência", "Chapa Fiscaliza",
                "Chapa Contas Abertas", "Chapa Transparência", "Chapa Fiscaliza", "Chapa Contas Abertas",
                "Chapa Transparência");
        votacao.encerrar(ORGANIZADOR);
        votacao.apurar(ORGANIZADOR);
        return votacao;
    }

    private Votacao assembleia() {
        Votacao votacao = base(ASSEMBLEIA, List.of("Aprovar", "Rejeitar"))
                .descricao("Proposta de reforma do art. 12 do estatuto (mandato da diretoria). Exige quórum de "
                        + "metade dos conselheiros e 2/3 dos votos válidos. Voto nominal.")
                .regra(new MaioriaQualificada(0.5, 2.0 / 3))
                .sigilo(Sigilo.IDENTIFICADO)
                .build();
        votacao.abrir(ORGANIZADOR);
        votar(votacao, 1, "Aprovar", "Aprovar", "Rejeitar", "Aprovar", "Aprovar", "Aprovar", "Rejeitar",
                "Aprovar", "Aprovar");
        votacao.encerrar(ORGANIZADOR);
        votacao.apurar(ORGANIZADOR);
        return votacao;
    }

    private Votacao enquete() {
        return base(ENQUETE, List.of("Sim", "Não"))
                .descricao("Enquete rápida com a torcida, para abrir ao vivo na apresentação.")
                .regra(new MaioriaSimples())
                .build();
    }

    private PremiacaoTop3 craqueDoCampeonato() {
        Map<String, String> grupos = new LinkedHashMap<>();
        IntStream.rangeClosed(1, TOTAL_ELEITORES)
                .forEach(i -> grupos.put(id(i), i <= 3 ? "Jornalistas" : "Torcedores"));
        PremiacaoTop3 premiacao = new PremiacaoTop3(CRAQUE,
                "Escolha, em ordem, os três melhores jogadores do campeonato. Jornalistas têm peso 2.",
                JOGADORES, List.of(new CategoriaEleitor("Jornalistas", 2.0), new CategoriaEleitor("Torcedores", 1.0)),
                grupos);
        premiacao.abrir();
        premiacao.registrarCedula(id(5), new CedulaTop3("Rafa Lima", "Davi Prado", "Caio Torres"));
        premiacao.registrarCedula(id(6), new CedulaTop3("Davi Prado", "Rafa Lima", "Léo Andrade"));
        premiacao.registrarCedula(id(7), new CedulaTop3("Rafa Lima", "Caio Torres", "Nico Sales"));
        premiacao.registrarCedula(id(8), new CedulaTop3("Caio Torres", "Rafa Lima", "Davi Prado"));
        premiacao.registrarCedula(id(9), new CedulaTop3("Rafa Lima", "Léo Andrade", "Davi Prado"));
        return premiacao;
    }

    private Votacao.Builder base(String pergunta, List<String> opcoes) {
        return Votacao.nova()
                .pergunta(pergunta)
                .opcoes(opcoes)
                .eleitorado(eleitorado())
                .relogio(relogio)
                .criadaPor(ORGANIZADOR);
    }

    private Eleitorado eleitorado() {
        return Eleitorado.de(IntStream.rangeClosed(1, TOTAL_ELEITORES)
                .mapToObj(i -> new Eleitor(id(i), contas.nomeDe(id(i)).orElse("Eleitor " + id(i))))
                .toList());
    }

    /** Registra um voto por eleitor, a partir de {@code primeiro} (ex.: 5 → e05, e06, ...). */
    private static void votar(Votacao votacao, int primeiro, String... opcoes) {
        for (int i = 0; i < opcoes.length; i++) {
            votacao.registrarVoto(id(primeiro + i), opcoes[i]);
        }
    }

    private static String id(int numero) {
        return String.format("e%02d", numero);
    }
}

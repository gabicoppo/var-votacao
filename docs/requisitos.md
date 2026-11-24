# Requisitos: rastreabilidade final

Estado de cada história (#19 a #32) e de cada requisito não funcional (RNF-01 a RNF-07) na versão
final, conferido no código e nos testes. Legenda: ✅ feito · ⚠️ parcial · ❌ não feito.

Os números de testes desta página são da versão final: **1.071 testes** no `./mvnw verify`
(domínio e web) e **10 cenários E2E** no `./mvnw verify -Pe2e`. Os testes ficam em `src/test/java`,
no mesmo pacote da classe testada; os E2E, em `src/test/java/br/unicamp/var/e2e`.

## Resumo

| | ✅ | ⚠️ | ❌ |
|---|---|---|---|
| Histórias (14) | 10 | 2 (#27, #28) | 2 (#25, #32) |
| RNFs (7) | 6 | — | 1 (RNF-06) |

## Histórias de usuário

| # | História | Status | Onde está | Como foi verificado |
|---|---|---|---|---|
| [#19](https://github.com/gabicoppo/var-votacao/issues/19) | Criar votação com pergunta, opções/candidatos e descrição | ✅ | `processo.Votacao` (builder, validação de opções), `cadastro`; tela `GET/POST /organizador/votacoes/nova`; a descrição aparece na cédula. Fica em rascunho até abrir (State). | `VotacaoTest`, `ConversorFormularioVotacaoTest`, `OrganizadorControllerTest`, `PolimentoControllerTest`; E2E `RoteiroDemoE2E` (gestão de uma votação em rascunho). |
| [#20](https://github.com/gabicoppo/var-votacao/issues/20) | Definir período de votação | ✅ | `processo.PeriodoVotacao`, `Votacao.atualizarPeloRelogio`; `web.comum.AtualizadorPeriodos` abre e encerra pelo relógio a cada 30 s; a cédula mostra o prazo (`TempoRestante`). | `PeriodoVotacaoTest`, `VotacaoTest`, `AtualizadorPeriodosTest`, `TempoRestanteTest`; `EleitorWebTest` (voto fora do período recusado). |
| [#21](https://github.com/gabicoppo/var-votacao/issues/21) | Escolher a regra de apuração | ✅ | `apuracao` (Strategy): `MaioriaSimples`, `MaioriaAbsoluta` (2º turno opcional), `MaioriaQualificada` (quórum e fração); escolha no formulário (`ConversorFormularioVotacao.regra`); troca só em rascunho. | `MaioriaQualificadaClassesEquivalenciaTest`, `MaioriaQualificadaValorLimiteTest`, `MaioriaAbsolutaTest`, `ApuracaoMajoritariaTest`, `VotacaoSegundoTurnoTest`; E2E `SegundoTurnoE2E`; PIT em `processo`. |
| [#22](https://github.com/gabicoppo/var-votacao/issues/22) | Definir voto secreto ou identificado | ✅ | `urna.Sigilo` e `urna.Urna` (presença separada da contagem no voto secreto); padrão secreto; aviso na cédula. Em votação identificada, a escolha de cada eleitor aparece ao organizador na lista de presença da gestão, depois da apuração. | `UrnaTest`, `EleitorWebTest` (sigilo depois do voto), `GestaoVotacoesTest`. |
| [#23](https://github.com/gabicoppo/var-votacao/issues/23) | Configurar premiação Top 3 com pesos por grupo | ✅ | `top3.PremiacaoTop3`, `CategoriaEleitor`, `CedulaTop3`; `web.premiacao` (nova premiação, gestão); cédula por toque em `web.eleitor`. | `PremiacaoTop3Test`, `PremiacaoTop3AptosClassesEquivalenciaTest`, `CedulaTop3Test`, `PremiacaoOrganizadorControllerTest`; E2E `PremiacaoE2E`. |
| [#24](https://github.com/gabicoppo/var-votacao/issues/24) | Cadastrar lista de eleitores aptos | ✅ | `eleitorado.Eleitorado` (importação `id;nome`), campo "Lista de aptos" da nova votação; total de aptos na gestão e no quórum; lista congelada depois da abertura. **Adaptações:** o eleitor é identificado por um id, e não por e-mail (o sistema não envia e-mail, ADR-6); uma linha inválida recusa a importação, indicando o número da linha, em vez de ser ignorada. | `EleitoradoTest`, `EleitorTest`, `MensagensErroTest` (erro com o número da linha), `GestaoVotacoesTest`. |
| [#25](https://github.com/gabicoppo/var-votacao/issues/25) | Definir critérios de elegibilidade do estatuto | ❌ | Não implementada: não há critérios (idade, tempo de associação, mensalidades) nem importação do cadastro de sócios. O organizador importa a lista de aptos **já filtrada** (#24). | — |
| [#26](https://github.com/gabicoppo/var-votacao/issues/26) | Votar pelo celular ou computador | ✅ | `web.eleitor`: painel, cédula → revisão → comprovante (`VotoEleitorController`), mobile-first; só aptos; voto único. | `EleitorWebTest`, `PainelEleitorTest`, `EleitorPolimentoWebTest`; E2E `UsabilidadeCelularE2E`, `AptidaoEVotoUnicoE2E`, `RoteiroDemoE2E`. |
| [#27](https://github.com/gabicoppo/var-votacao/issues/27) | Verificar identidade antes de votar | ⚠️ | Existe só a autenticação básica: login com id e **código de acesso** de 8 caracteres, gerado para cada eleitor novo e mostrado uma vez ao organizador (`ContasUsuariosEmMemoria`, tela `/organizador/votacoes/{id}/acessos`). Não há verificação reforçada (biometria, tentativas, bloqueio). Decisão no ADR-6. | `ContasUsuariosEmMemoriaTest`, `LoginDemonstracaoTest`, `AplicacaoWebTest`. |
| [#28](https://github.com/gabicoppo/var-votacao/issues/28) | Receber comprovante de voto | ⚠️ | `urna.Comprovante` (código único com data/hora, sem a opção); tela do comprovante; conferência pública em `GET /verificar` ("comprovante não encontrado" para código errado). **Falta:** a cópia por e-mail. | `ComprovanteTest`, `UrnaTest`, `PainelPremiacaoVerificacaoWebTest`, `EleitorWebTest`; E2E `RoteiroDemoE2E` (vota, recebe o código e verifica). |
| [#29](https://github.com/gabicoppo/var-votacao/issues/29) | Ver resultado com votos absolutos e percentuais | ✅ | `resultado.ResultadoDetalhado`; `GET /votacoes/{id}/resultado` (barras, situação, participação) e o telão; durante a votação, só a participação (RNF-01). | `ResultadoDetalhadoTest`, `GraficoResultadoTest`, `ResultadoControllerTest`; E2E `RoteiroDemoE2E`; carga `CargaRnf03Test`. |
| [#30](https://github.com/gabicoppo/var-votacao/issues/30) | Ver ranking completo com pontuação e critérios | ✅ | `top3.RankingTop3` (pontos por grupo, critérios, desempate); `GET /premiacoes/{id}/ranking` com pódio e colunas por grupo. | `RankingTop3Test`, `PodioTest`, `RankingControllerTest`; E2E `PremiacaoE2E`. |
| [#31](https://github.com/gabicoppo/var-votacao/issues/31) | Gerar relatório de apuração | ✅ | `relatorio.RelatorioApuracao`; `GET /votacoes/{id}/relatorio` (ata imprimível com a auditoria e o código de verificação SHA-256 do texto, `CodigoVerificacao`). | `RelatorioApuracaoTest`, `CodigoVerificacaoTest`, `ResultadoControllerTest`; E2E `RoteiroDemoE2E`. |
| [#32](https://github.com/gabicoppo/var-votacao/issues/32) | Ver resultado por faixa etária | ❌ | Não implementada: depende da data de nascimento do cadastro de sócios (#25), que não existe. A demanda veio de uma única fonte (D7) e foi tratada como hipótese na priorização. | — |

## Requisitos não funcionais

| RNF | Status | Onde está | Como foi verificado |
|---|---|---|---|
| **RNF-01 Sigilo** | ✅ | `Urna` guarda quem votou separado da contagem no voto secreto e só expõe `votoDe` no identificado; `RegistroAuditoria` não tem evento de voto; o comprovante não traz a opção; durante a votação só aparece a participação. | `UrnaTest`, `RegistroAuditoriaTest`, `VotacaoAuditoriaTest`, `EleitorWebTest` (nenhuma página do eleitor mostra a opção depois do voto); E2E `RoteiroDemoE2E` (o comprovante não contém a opção). |
| **RNF-02 Integridade** | ✅ | `Urna`: um voto por eleitor apto, votos de não aptos recusados, soma da contagem igual ao total de votantes. | `UrnaTest`, `VotacaoTest`; `CargaRnf03Test` (20.000 votos: soma = votantes = aptos; o 20.001º voto é recusado); E2E `AptidaoEVotoUnicoE2E` (segundo voto, de outra aba, recusado). |
| **RNF-03 Tempo de resultado** (até 5 min para até 20.000 votos) | ✅ | Apuração em memória, chamada na hora pelo organizador. | `CargaRnf03Test` (PR #69): 20.000 votos registrados em 53–61 ms; do encerramento ao relatório pronto, **16,1 ms** (JVM fria) e **0,3 ms** (dentro do `verify`). Escopo: domínio em memória, sem HTTP. |
| **RNF-04 Usabilidade no celular** | ✅ | Design system mobile-first (`var.css`), cédula em 3 telas. | E2E `UsabilidadeCelularE2E`: a 360 × 740, do link da cédula à confirmação são **3 telas**, e **nenhuma das 15 páginas principais** tem rolagem horizontal; E2E `AcessibilidadeE2E` (rótulos, `alt`, um `h1`). |
| **RNF-05 Autenticação** | ✅ | Spring Security (sessão, papéis `ORGANIZADOR`/`ELEITOR`, CSRF); cédula só para aptos (403 com página própria). A verificação reforçada é a #27 (⚠️). | `AplicacaoWebTest`, `EleitorWebTest` (não apto recebe 403), `PerfilProducaoTest`; E2E `AptidaoEVotoUnicoE2E`. |
| **RNF-06 Privacidade demográfica** | ❌ | Não se aplica sem o recorte por faixa etária (#32). | — |
| **RNF-07 Auditoria** | ✅ | `auditoria.RegistroAuditoria` (só acréscimo, data/hora e autor); `Votacao` registra criação, regra, abertura, encerramento, apuração e 2º turno; configuração congelada depois da abertura (State); auditoria na gestão e no relatório. | `RegistroAuditoriaTest`, `VotacaoAuditoriaTest`, `RelatorioApuracaoTest`; PIT em `processo`: **113 de 113 mutantes mortos**. |

## Descobertas da elicitação (A3)

| Descoberta | Atendida? | Como |
|---|---|---|
| D1: sócios distantes nunca votaram, porque o voto é presencial | ✅ | Voto remoto pela web (#26). |
| D2: voto pelo celular só com verificação forte de identidade | ⚠️ | Só login com código de acesso; verificação reforçada adiada (#27, ADR-6). |
| D3: aptidão definida pelo estatuto | ⚠️ | O organizador importa a lista já filtrada (#24); critérios automáticos não feitos (#25). |
| D4: confiança depende de transparência e de comissão independente | ✅ | Auditoria, relatório com código de verificação, comprovante e verificação pública. |
| D5: sigilo essencial na eleição do clube | ✅ | Voto secreto como padrão (RNF-01). |
| D6: resultado no mesmo dia, com absolutos e percentuais | ✅ | Resultado na hora da apuração (RNF-03, #29). |
| D7: resultado por faixa etária (uma única fonte) | ❌ | Hipótese não priorizada (#32). |
| D8: 2º turno não é universal | ✅ | 2º turno opcional na maioria absoluta (#21). |
| D9: falta canal oficial com datas, candidatos e propostas | ✅ | Descrição e período na cédula (#19, #20). |
| D10, D20: votações simples, rápidas de criar; automatizar a contagem | ✅ | Maioria simples, formulário com padrões, duplicar votação, apuração automática e telão. |
| D11, D12: ordem por posição e pesos por grupo | ✅ | Premiação Top 3 com pesos (#23). |
| D13: em premiações e enquetes o sigilo é indiferente | ✅ | Sigilo configurável por votação (#22). |
| D14, D19: duração de uma reunião a semanas; votações adiadas por falta de tempo | ✅ | Período livre, com abertura e encerramento automáticos (#20). |
| D15, D16: ranking completo, pontuação e critérios; poder verificar | ✅ | Ranking com colunas por grupo e critérios (#30); comprovante (#28). |
| D17: na atlética, votam os membros da gestão | ✅ | Lista de aptos por votação (#24). |
| D18: atlética com voto identificado e resultado em ata para ausentes | ✅ | Voto identificado (#22) e relatório/ata (#31). |
| D21: verificação reforçada opcional em grupos pequenos | ✅ | Sem verificação reforçada, grupos pequenos usam só o código de acesso. |

## Fechamento do backlog

Ações feitas no GitHub ao fim do projeto (comentário em cada issue, com os PRs):

- **Fechadas como concluídas:** #19, #20, #21, #22, #24 (as #23, #26, #29, #30 e #31 já estavam fechadas).
- **Deixadas abertas, com comentário explicando o que existe e o que falta:** #25, #27, #28, #32.
- **Épicos:** #15 (Configuração) fechado; #16 (Eleitorado, falta #25), #17 (Votação remota, faltam #27 e #28) e #18 (Apuração, falta #32) abertos, com o progresso comentado.

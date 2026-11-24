# Changelog

Entregas do VAR, montadas a partir do `git log` e dos pull requests. Cada entrega tem uma tag e
um merge na `main`.

## [Não lançado]: entrega final

Primeira versão com **aplicação web**. A tag e a versão do `pom.xml` desta entrega serão definidas
na release.

### Domínio
- `Proporcao` extraída das regras (Extract Class, resolve o code smell da A4); descrição das regras
  em pt-BR; pacote `auditoria` (RNF-07) ([#57](https://github.com/gabicoppo/var-votacao/pull/57)).
- Eleitorado com importação `id;nome`, urna com voto secreto ou identificado, um voto por eleitor
  e comprovante (#22, #24, #28) ([#58](https://github.com/gabicoppo/var-votacao/pull/58)).
- Votação com descrição, período e resultado detalhado (#19, #20, #29)
  ([#59](https://github.com/gabicoppo/var-votacao/pull/59)); eleitores, sigilo e comprovante na
  votação (#22, #24, #26, #28) ([#60](https://github.com/gabicoppo/var-votacao/pull/60)).
- Auditoria no ciclo da votação, 2º turno e relatório de apuração (#29, #31)
  ([#61](https://github.com/gabicoppo/var-votacao/pull/61)).
- Premiação Top 3 com pesos por grupo, ranking auditável e consulta de aptos (#23, #30)
  ([#62](https://github.com/gabicoppo/var-votacao/pull/62),
  [#64](https://github.com/gabicoppo/var-votacao/pull/64)).

### Aplicação web
- Fundação Spring Boot 3.5: login, papéis, layout, design system e repositórios em memória
  ([#63](https://github.com/gabicoppo/var-votacao/pull/63)); dados de demonstração, página inicial,
  páginas de erro ([#65](https://github.com/gabicoppo/var-votacao/pull/65)).
- Área do organizador (#19, #20, #21, #22, #24)
  ([#66](https://github.com/gabicoppo/var-votacao/pull/66),
  [#72](https://github.com/gabicoppo/var-votacao/pull/72)).
- Área do eleitor: voto em 3 telas, cédula Top 3 e verificação de comprovante (#26)
  ([#67](https://github.com/gabicoppo/var-votacao/pull/67),
  [#71](https://github.com/gabicoppo/var-votacao/pull/71)).
- Resultado, relatório e ranking (#29, #30, #31)
  ([#68](https://github.com/gabicoppo/var-votacao/pull/68)); modo telão, contexto do 2º turno e ata
  com código de verificação ([#73](https://github.com/gabicoppo/var-votacao/pull/73)).
- Design system no celular e diálogo de confirmação (RNF-04)
  ([#70](https://github.com/gabicoppo/var-votacao/pull/70)).

### Qualidade, entrega e documentação
- Teste de carga do RNF-03 com 20.000 votos
  ([#69](https://github.com/gabicoppo/var-votacao/pull/69)).
- Testes de ponta a ponta com Playwright e job `e2e` no CI
  ([#74](https://github.com/gabicoppo/var-votacao/pull/74)).
- Deploy contínuo no Render: `Dockerfile`, perfil `prod`, senha do organizador por variável de
  ambiente, health check e job `docker` no CI
  ([#75](https://github.com/gabicoppo/var-votacao/pull/75)).
- Documentação final: arquitetura (C4 níveis 1 a 3, ADR-4 a ADR-8), requisitos, manual, roteiro da
  demonstração, README e este changelog.

## [a5-v1.0.0]: 2026-11-16 (`pom.xml` 1.2.0)

Testes e qualidade (Atividade 5).

- Classes de equivalência e valor limite na maioria qualificada
  ([#44](https://github.com/gabicoppo/var-votacao/pull/44)), no cadastro de candidatos
  ([#48](https://github.com/gabicoppo/var-votacao/pull/48)) e na pontuação do Top 3
  ([#54](https://github.com/gabicoppo/var-votacao/pull/54)).
- Teste de mutação com PIT e mutante sobrevivente morto
  ([#46](https://github.com/gabicoppo/var-votacao/pull/46)).
- Resultado detalhado com votos absolutos e percentuais
  ([#50](https://github.com/gabicoppo/var-votacao/pull/50)) e cobertura estrutural
  ([#52](https://github.com/gabicoppo/var-votacao/pull/52)).
- Release: [#55](https://github.com/gabicoppo/var-votacao/pull/55),
  [#56](https://github.com/gabicoppo/var-votacao/pull/56).

## [a4-v1.0.0]: 2026-10-25 (`pom.xml` 1.1.0)

Arquitetura e padrões de projeto (Atividade 4).

- Strategy: regra de apuração intercambiável
  ([#38](https://github.com/gabicoppo/var-votacao/pull/38)).
- State: ciclo de vida da votação ([#39](https://github.com/gabicoppo/var-votacao/pull/39)).
- Refatoração: posição do Top 3 como enum `Posicao`
  ([#34](https://github.com/gabicoppo/var-votacao/pull/34)).
- Documentação da arquitetura em C4 nível 3
  ([#40](https://github.com/gabicoppo/var-votacao/pull/40)).
- Release: [#41](https://github.com/gabicoppo/var-votacao/pull/41),
  [#42](https://github.com/gabicoppo/var-votacao/pull/42).

## [a2-v1.0.0]: 2026-09-21 (`pom.xml` 1.0.0)

Primeiras funcionalidades e pipeline (Atividade 2).

- Estrutura inicial com build, lint (Checkstyle, SpotBugs), testes e CI no GitHub Actions.
- Cadastro simples de candidatos ([#3](https://github.com/gabicoppo/var-votacao/pull/3)).
- Quórum e maioria qualificada ([#8](https://github.com/gabicoppo/var-votacao/pull/8)).
- Apuração com 2º turno ([#9](https://github.com/gabicoppo/var-votacao/pull/9)).
- Top 3 ponderado e cálculo de pontos ([#11](https://github.com/gabicoppo/var-votacao/pull/11)).
- Release: [#12](https://github.com/gabicoppo/var-votacao/pull/12),
  [#13](https://github.com/gabicoppo/var-votacao/pull/13),
  [#14](https://github.com/gabicoppo/var-votacao/pull/14).

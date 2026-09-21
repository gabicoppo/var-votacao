# VAR — Votação, Apuração e Resultados

Plataforma configurável para processos de votação e decisão coletiva no futebol
(MC656 — Engenharia de Software).

## Requisitos
- JDK 21
- Maven 3.9+ (ou use o wrapper `./mvnw`)

## Comandos
```bash
./mvnw verify                        # compila, roda testes e gera cobertura
./mvnw checkstyle:check              # lint (Checkstyle)
./mvnw compile spotbugs:check        # análise estática (SpotBugs)
```
O relatório de cobertura (JaCoCo) fica em `target/site/jacoco/index.html`.

## Fluxo de trabalho (Git)
- `main`: versões entregues (protegida)
- `develop`: integração (protegida)
- `feature/nome-da-feature`: uma por funcionalidade, criada a partir de `develop`
- `release/*`: preparação de versão, a partir de `develop`, com PR para `main`

Toda mudança entra por Pull Request, com 1 aprovação de outro integrante,
discussões resolvidas e checks `build`, `lint` e `test` verdes.

Mensagens de commit: `tipo: descrição curta (#issue)`, por exemplo
`feat: calcula maioria absoluta (#12)`.

## Equipe
- Fabrício Karasz Novaes Silva (281168)
- Gabriela Vieira Coppo (167163)
- João Rafael Silva de Azeredo (288818)
- Juliana Travenzolli Silva Martins (245913)

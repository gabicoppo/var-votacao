# Roteiro da demonstração (10 min)

Demonstração ao vivo com os dados de demonstração. Cada passo indica o requisito que ele mostra.

## Preparação (antes de começar)

- **Deploy:** abra `https://<seu-servico>.onrender.com/actuator/health` **5 minutos antes**. O plano
  gratuito dorme, e o primeiro acesso leva perto de um minuto ([deploy.md](deploy.md)).
- **Telão:** um computador projetando, logado como `organizador`. No deploy, a senha é a de
  `VAR_SENHA_ORGANIZADOR`; localmente, `var2026`.
- **Celulares:** colegas com o login pronto: `e01`, `e02`, `e03` e `e04` (senha `1234`). Na
  eleição presidencial, só eles ainda não votaram; `e05` a `e11` já votaram.
- **Dados:** cada deploy ou reinício volta os dados de demonstração ao estado inicial. Para ensaiar
  de novo, basta reiniciar.

## Passo a passo

| Min | Passo | O que mostrar e dizer | Requisito |
|---|---|---|---|
| 0:00 | Abertura | Página inicial: o problema (votações presenciais, sem transparência, resultado demorado) e os três contextos. | — |
| 0:45 | Painel do organizador | Métricas e votações por status. "Tudo o que o organizador precisa num lugar só." | #19 |
| 1:15 | Abrir a enquete | Card "Enquete: o gol da final foi golaço?" → gestão em rascunho → "Abrir votação" → diálogo de confirmação. A configuração fica congelada. | #20, RNF-07 |
| 2:00 | Convite e telão | "Copiar link" do convite e "Abrir no telão": a participação aparece ao vivo, sem votos por opção. | RNF-01 |
| 2:30 | Colegas votam pelo celular | Cada colega abre o link, entra com `e01`…`e04`, escolhe, revisa e confirma: 3 telas. O telão atualiza a participação. | #26, RNF-04, RNF-05 |
| 3:30 | Comprovante e verificação | Num celular: o código do comprovante, sem a opção; "Verificar depois" confirma que o voto está na contagem. Tentar votar de novo: "Você já votou nesta votação." | #28, RNF-01, RNF-02 |
| 4:30 | Encerrar e apurar | Na gestão: "Encerrar votação" e "Apurar". A apuração é instantânea (20.000 votos em milissegundos no teste de carga). | #21, RNF-03 |
| 5:00 | Momento VAR | "Ver resultado": a animação "Revisão do VAR..." e a faixa com a situação, as barras e a participação. Mostrar também no telão. | #29 |
| 6:00 | Relatório | "Relatório": ata com a auditoria (quem abriu, encerrou e apurou, e quando) e o **código de verificação**. Explicar o que ele prova (texto não alterado) e o que não prova (não é assinatura digital). | #31, RNF-07 |
| 7:00 | 2º turno | "Eleição do Conselho Fiscal" (apurada, sem maioria absoluta) → "Criar 2º turno": nova votação com os dois mais votados e o link para o 1º turno. | #21 |
| 8:00 | Craque do Campeonato | Num celular: cédula Top 3 por toque, com o peso do grupo ("Você vota como Jornalistas, peso 2"). Na gestão: encerrar e "Apurar e publicar ranking" → pódio, ranking e pontos por grupo. | #23, #30 |
| 9:15 | Limitações e fechamento | Dados em memória; verificação reforçada (#27), critérios do estatuto (#25) e faixa etária (#32) ficaram de fora ([requisitos.md](requisitos.md)). | — |

## Plano B

- **Deploy dormindo ou fora do ar:** rode localmente com `./mvnw spring-boot:run` e projete
  `http://localhost:8080`. Os colegas votam na mesma rede, usando o IP da máquina
  (`http://<ip>:8080`), ou abrem abas anônimas no mesmo computador.
- **Sem rede para os celulares:** faça os votos em abas anônimas, com `e01`…`e04`.
- **Algo deu errado no meio:** reiniciar volta os dados de demonstração. Os passos 7 e 8 não
  dependem dos anteriores.

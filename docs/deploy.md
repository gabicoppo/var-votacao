# Deploy do VAR

A aplicação é publicada no [Render](https://render.com), em container, a partir do `Dockerfile` da raiz.
O `render.yaml` (blueprint) liga o **deploy automático à branch `main`**: cada merge na `main` gera uma
nova versão no ar. É o "deploy após o merge na `main`" previsto no processo da Atividade 1.

## Como a imagem é montada

| Arquivo | Papel |
|---|---|
| `Dockerfile` | Build em dois estágios: `maven:3.9-eclipse-temurin-21` compila e empacota o jar (`mvn -DskipTests package`); `eclipse-temurin:21-jre` roda o jar com um usuário sem privilégios. |
| `.dockerignore` | Deixa fora da imagem `target/`, `.git/`, `docs/` e os `.md`. |
| `render.yaml` | Serviço web `var-votacao`, `runtime: docker`, plano gratuito, `autoDeploy` na `main`, health check em `/actuator/health`. |
| `src/main/resources/application-prod.properties` | Perfil `prod`: porta de `PORT`, cabeçalhos do proxy (links com `https`), cookie de sessão `secure` e `http-only`, cache do Thymeleaf, erros sem detalhes internos. |
| `.github/workflows/ci.yml` (job `docker`) | Faz `docker build` em todo PR, sem publicar, para o `Dockerfile` não quebrar em silêncio. |

## Variáveis de ambiente

| Variável | Valor | Observação |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `prod` | Já definida no `render.yaml` e no `Dockerfile`. |
| `VAR_SENHA_ORGANIZADOR` | senha do usuário `organizador` | **Defina no painel do Render.** Sem ela, a aplicação gera uma senha aleatória e a escreve **uma vez** no log de inicialização (`senha gerada para o usuario organizador: ...`). |
| `PORT` | definida pelo Render | Não é preciso configurar. |
| `JAVA_OPTS` | `-XX:MaxRAMPercentage=75 -XX:+UseSerialGC -Xss512k` | Padrão do `Dockerfile`, pensado para os 512 MB do plano gratuito. |

As contas dos eleitores de demonstração (`e01` a `e12`, senha `1234`) continuam públicas: elas são o
objetivo da demonstração. No perfil `prod`, o quadro "Contas de demonstração" do login **não mostra** a
senha do organizador.

## Passo a passo no Render (feito por uma pessoa)

1. Crie uma conta em <https://render.com> (dá para entrar com o GitHub).
2. No painel, clique em **New → Blueprint**.
3. Conecte o GitHub e escolha o repositório `gabicoppo/var-votacao`. O Render lê o `render.yaml` da
   branch `main` e mostra o serviço `var-votacao`.
4. Quando ele pedir o valor de **`VAR_SENHA_ORGANIZADOR`** (marcada com `sync: false`), digite a senha
   do organizador. Não a escreva no repositório.
5. Clique em **Apply**. O primeiro build leva alguns minutos (download das dependências do Maven).
6. Quando o status ficar **Live**, a URL aparece no topo da página do serviço, no formato
   `https://<seu-servico>.onrender.com`. Confira:
   - `https://<seu-servico>.onrender.com/actuator/health` → `{"status":"UP"}`;
   - `https://<seu-servico>.onrender.com/login` → página de login.
7. Atualize o link do deploy no `README.md`.

A partir daí, todo merge na `main` dispara um novo deploy automaticamente (aba **Events** do serviço).

## Limites do plano gratuito

- **O serviço dorme** depois de cerca de 15 minutos sem acesso, e o primeiro acesso depois disso leva
  perto de um minuto. **Abra a URL alguns minutos antes da apresentação** e deixe a página aberta.
- **Os dados ficam em memória** (não há banco de dados, veja o ADR-5 em `docs/arquitetura.md`). Cada
  deploy, reinício ou volta do sono **apaga** as votações criadas e restaura os dados de demonstração.
- 512 MB de RAM e CPU compartilhada: o suficiente para a demonstração, não para uso real.

## Plano B: rodar localmente

```bash
./mvnw spring-boot:run                      # perfil padrão, http://localhost:8080 (organizador/var2026)
./mvnw -DskipTests package && java -jar target/var-*.jar
docker build -t var-votacao . && docker run -p 8080:8080 -e PORT=8080 -e VAR_SENHA_ORGANIZADOR=minha-senha var-votacao
```

No perfil padrão (sem `SPRING_PROFILES_ACTIVE=prod`), o organizador entra com `var2026`, e o quadro do
login mostra todas as contas de demonstração.

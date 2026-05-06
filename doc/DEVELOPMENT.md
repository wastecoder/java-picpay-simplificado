# Desenvolvimento

Tudo o que você precisa para rodar o projeto, alterar código e diagnosticar problemas comuns.

## Sumário

- [1. Pré-requisitos](#1-pré-requisitos)
- [2. Subir o ambiente](#2-subir-o-ambiente)
- [3. Comandos úteis](#3-comandos-úteis)
- [4. Profiles](#4-profiles)
- [5. Variáveis de ambiente](#5-variáveis-de-ambiente)
- [6. Configuração de Feign + Resilience4j](#6-configuração-de-feign--resilience4j)
- [7. Migrations (Flyway)](#7-migrations-flyway)
- [8. Docker](#8-docker)
- [9. Troubleshooting](#9-troubleshooting)
- [10. Onde achar as coisas no código](#10-onde-achar-as-coisas-no-código)

---

## 1. Pré-requisitos

- **JDK 21** (Eclipse Temurin recomendado — é a base do Dockerfile).
- **Docker** + Docker Compose (Postgres roda em container).
- **Git**.
- IDE de preferência (IntelliJ IDEA testado).

> Não precisa instalar Gradle: o projeto usa o wrapper (`./gradlew` no bash, `gradlew.bat` no PowerShell/cmd).

---

## 2. Subir o ambiente

### 2.1 Modo dev (app local + Postgres em container)

```bash
docker compose up -d postgres
./gradlew bootRun
```

- Postgres em `localhost:5433` (db `picpay`, usuário/senha `postgres/postgres`).
- Swagger UI: <http://localhost:8080/swagger-ui.html>.

### 2.2 Modo full container (app + Postgres)

```bash
docker compose up -d
```

Sobe **postgres** + **app** juntos. `app` aguarda `postgres` ficar saudável (healthcheck `pg_isready`) antes de iniciar.

### 2.3 Parar tudo

```bash
docker compose down            # mantém o volume de dados
docker compose down -v         # apaga o volume picpay-postgres-volume (reseta o DB)
```

---

## 3. Comandos úteis

| Comando | O que faz |
|---|---|
| `./gradlew build` | Compila + roda testes unitários + verificação JaCoCo. |
| `./gradlew bootRun` | Sobe a aplicação (profile `docker` por padrão). |
| `./gradlew bootJar` | Gera o jar executável em `build/libs/`. |
| `./gradlew test` | Testes unitários e de adapter (exclui `*IntegrationTest`). |
| `./gradlew integrationTest` | Suite de integração (Testcontainers + WireMock). |
| `./gradlew test --tests "FQN.Classe.metodo"` | Roda um teste específico. |
| `./gradlew jacocoTestReport` | Gera relatório em `build/reports/jacoco/test/html/index.html`. |
| `./gradlew check` | Testes + verificação de cobertura (falha se thresholds < 85%/60%). |
| `./gradlew pitest` | Mutation testing — relatório em `build/reports/pitest/`. |
| `./gradlew dependencies` | Árvore completa de dependências. |
| `./gradlew --refresh-dependencies build` | Força revalidação do cache. |

> **Detalhe sobre `./gradlew test` no JaCoCo:** essa task exclui `*IntegrationTest`, então o relatório de cobertura sai apenas com os testes unitários e de adapter. Ver [TESTS.md §8](TESTS.md#8-jacoco-cobertura-de-linhas).

---

## 4. Profiles

A aplicação tem dois arquivos de config:

- **`application.yaml`** — base.
  - Define `spring.profiles.active: docker` (profile padrão), config dos clients Feign (`notify-user`, `transfer-validation`) e do Resilience4j.
- **`application-docker.yml`** — ativado pelo profile `docker`.
  - Define `spring.datasource.*`, JPA (`ddl-auto: validate`, `show-sql: true`), Flyway e `security.jwt.*`.

Para rodar com outro profile (não há outros prontos hoje, mas a infra já está pronta):

```bash
./gradlew bootRun --args='--spring.profiles.active=local'
SPRING_PROFILES_ACTIVE=test ./gradlew bootRun
```

---

## 5. Variáveis de ambiente

Todas têm defaults razoáveis para desenvolvimento local — **não confie nesses defaults em produção**.

### 5.1 Banco de dados

| Variável | Default (em `application-docker.yml`) | Default (no `docker-compose.yml`) | Quando mudar |
|---|---|---|---|
| `DATABASE_URL` | `jdbc:postgresql://localhost:5433/picpay` | `jdbc:postgresql://postgres:5432/picpay` | Apontando para outro Postgres. |
| `DATABASE_USERNAME` | `postgres` | `postgres` | — |
| `DATABASE_PASSWORD` | `postgres` | `postgres` | **Sempre em prod.** |

### 5.2 Segurança / JWT

| Variável | Default | Mapeia para | Observação |
|---|---|---|---|
| `JWT_GENERATOR_SIGNATURE_SECRET` | `secretJWTGenerator` | `security.jwt.secret` | Segredo HS512. **Trocar em prod.** |
| `APP_JWT_ISSUER` | `PICPAY-CHALLENGE` | `security.jwt.issuer` | Vai para o claim `iss`. |
| _(fixo)_ | `600` | `security.jwt.expires-after` | TTL do token em **segundos** (10 min). Editar `application-docker.yml` para mudar. |

### 5.3 Clients externos (Feign)

| Variável | Default | Mapeia para |
|---|---|---|
| `CLIENT_NOTIFY_SENDER` | `http://o4d9z.mocklab.io/notify` | `spring.cloud.openfeign.client.config.notify-user.url` |
| `CLIENT_TRANSFER_VALIDATOR` | `https://run.mocky.io/v3/8fafdd68-a090-496f-8c9a-3442cf30dae6` | `spring.cloud.openfeign.client.config.transfer-validation.url` |

### 5.4 Postgres (apenas no `docker-compose`)

| Variável | Default |
|---|---|
| `POSTGRES_DB` | `picpay` |
| `POSTGRES_USER` | `postgres` |
| `POSTGRES_PASSWORD` | `postgres` |

---

## 6. Configuração de Feign + Resilience4j

Cada client externo tem **três pontos de configuração com o mesmo nome** (em `application.yaml`):

```yaml
spring:
  cloud:
    openfeign:
      client:
        config:
          <name>:
            url: ...
            connectTimeout: 5000   # ms
            readTimeout: 5000      # ms

resilience4j:
  circuitbreaker:
    instances:
      <name>:
        baseConfig: default
```

Onde `<name>` é `notify-user` ou `transfer-validation`. **Os três `<name>` precisam bater** ou o Spring Cloud falha ao subir.

Config `default` do circuit breaker (compartilhada pelas duas instâncias):

| Parâmetro | Valor | O que significa |
|---|---|---|
| `slidingWindowType` | `COUNT_BASED` | Janela conta as N últimas chamadas. |
| `minimumNumberOfCalls` | `5` | Mínimo antes de calcular taxa de falha. |
| `failureRateThreshold` | `50%` | Acima disso, abre o breaker. |
| `waitDurationInOpenState` | `60s` | Quanto tempo fica `OPEN` antes de testar. |
| `permittedNumberOfCallsInHalfOpenState` | `3` | Chamadas de teste no estado `HALF_OPEN`. |
| `automaticTransitionFromOpenToHalfOpenEnabled` | `true` | Transiciona automaticamente após `waitDurationInOpenState`. |
| `ignore-exceptions` | `feign.FeignException$BadRequest` | 400s **não** contam como falha (são erros do cliente, não do serviço). |

Para adicionar um novo client externo, crie entrada nos **três lugares** com o mesmo nome:
- `*Client` Feign em `<modulo>/adapter/client/`
- `spring.cloud.openfeign.client.config.<n>` em `application.yaml`
- `resilience4j.circuitbreaker.instances.<n>` em `application.yaml`

→ Decisão registrada em [adr/0002-circuit-breaker-por-client-externo.md](adr/0002-circuit-breaker-por-client-externo.md).

---

## 7. Migrations (Flyway)

- Local: `src/main/resources/db/migration/V{N}__nome.sql`.
- `spring.jpa.hibernate.ddl-auto=validate` → schema só muda por nova migration. JPA **não** auto-altera tabelas.
- A cada `./gradlew bootRun`, Flyway aplica as migrations pendentes.

### 7.1 Como criar uma migration nova

1. Olhe o último `V{N}` em `db/migration/` e use `V{N+1}`.
2. Nome no padrão `V{N}__descricao_curta.sql` (dois underscores).
3. SQL Postgres-flavored (o projeto já usa enums Postgres, ver V1).
4. Reinicie a app para aplicar.

### 7.2 Migrations atuais

| Versão | Conteúdo |
|---|---|
| `V1__create_users.sql` | Enum `user_type`, tabela `users` com `external_id` UUID e `CHECK (balance >= 0)`. |
| `V2__create_transactions.sql` | Tabela `transactions` com FK `from_user_id` / `target_user_id` (`ON DELETE CASCADE`). |
| `V3__create_transactions_indexes.sql` | Índices em `from_user_id` e `target_user_id`. |

### 7.3 Erro de validação Flyway

Migration já aplicada **não pode ser editada**. Se um arquivo `V{N}` existente mudou, Flyway falha com `FlywayValidateException`. Soluções:
- **Recomendado:** criar `V{N+1}` para corrigir o que falta.
- Em desenvolvimento: `docker compose down -v && docker compose up -d` zera o banco e reaplica tudo do zero.

---

## 8. Docker

### 8.1 Dockerfile

Multi-stage usando Eclipse Temurin Alpine:

- **Stage 1 (`builder`):** `eclipse-temurin:21-jdk-alpine` — copia wrapper, baixa dependências e roda `./gradlew bootJar -x test`.
- **Stage 2 (runtime):** `eclipse-temurin:21-jre-alpine` — copia o jar, cria usuário não-root `app`, expõe `8080`, executa `java -jar /app/app.jar`.

Os testes são pulados no build da imagem (`-x test`); rode `./gradlew test` localmente antes de buildar.

### 8.2 docker-compose.yml

| Serviço | Imagem | Portas | Notas |
|---|---|---|---|
| `postgres` | `postgres:16-alpine` | `5433:5432` | Healthcheck via `pg_isready`. Volume `picpay-postgres-volume` persiste dados. |
| `app` | build local (`Dockerfile`) | `8080:8080` | `depends_on` aguarda `postgres` ficar `healthy`. Recebe env vars (DATABASE_URL aponta para o hostname `postgres`, porta interna `5432`). |

### 8.3 Comandos comuns

```bash
docker compose up -d                    # tudo em background
docker compose up -d postgres           # só o banco (para rodar a app via bootRun)
docker compose logs -f app              # acompanhar logs da app
docker compose ps                       # status dos serviços
docker compose down                     # para tudo (volume preservado)
docker compose down -v                  # para tudo e apaga volume
docker compose build app --no-cache     # rebuild da imagem da app
```

---

## 9. Troubleshooting

| Sintoma | Causa provável | Resolução |
|---|---|---|
| `port 5433 already allocated` | Outro Postgres ou container antigo está usando a porta. | `docker ps` → `docker rm -f <id>` ou mude o mapeamento em `docker-compose.yml` para `5434:5432`. |
| `FlywayValidateException` ao subir | Migration aplicada foi editada, ou há gap na sequência `V{N}`. | Crie nova migration `V{N+1}` em vez de editar a existente. Em dev, `docker compose down -v` zera o banco. |
| `SchemaManagementException ... not validate` | Entidade JPA ficou divergente do schema (campo novo sem migration, tipo errado). | Ou crie a migration que reflete a entidade, ou ajuste a entidade ao schema. **Não** mude `ddl-auto` para `update`. |
| `CircuitBreaker 'transfer-validation' is OPEN` | Cliente externo respondeu com falha em ≥ 50% das últimas 5 chamadas. | Verifique se `CLIENT_TRANSFER_VALIDATOR` está acessível. Reset manual: aguardar 60s ou reiniciar a app. |
| `notify-user` cai mas a transferência foi feita | Comportamento esperado — notificação é best-effort. | Sem ação. Ver [ARCHITECTURE.md §4.3](ARCHITECTURE.md#43-transferência). |
| App reclama de `connection refused` para Postgres ao subir via `docker compose up app` | Postgres ainda inicializando. | `app` já tem `depends_on` com healthcheck — se persistir, verifique `docker compose logs postgres`. |
| Testes de integração travam no `BeforeEach` | Imagem `postgres:16-alpine` ainda baixando ou Docker daemon down. | `docker info` para checar; `docker pull postgres:16-alpine` para baixar antes. |
| `Address already in use: bind` na porta 8080 | App já rodando em outro processo. | `lsof -i :8080` (Linux/Mac) / `netstat -ano \| findstr :8080` (Windows) → mate o processo. |
| Build da imagem demora muito | Wrapper baixando Gradle + dependências do zero. | A linha `RUN ./gradlew --no-daemon dependencies \|\| true` já é uma tentativa de cache; acelera nos rebuilds. |

---

## 10. Onde achar as coisas no código

| O que você quer mexer | Onde está |
|---|---|
| Adicionar/alterar endpoint HTTP | `**/adapter/controller/*Controller.java` + `request/` e `response/` records |
| Mudar regra de negócio | `**/usecases/*UseCaseImpl.java` |
| Domínio puro (modelos, ports, exceções) | `**/domain/` |
| Adicionar client HTTP externo | `<modulo>/adapter/client/` (Feign) + `domain/ports/output/*Gateway` + entrada em `application.yaml` (Feign + Resilience4j) |
| Persistência (entidade JPA, query) | `<modulo>/adapter/repository/database/*Database.java` (Spring Data) e `entity/`, `mapper/` |
| Schema do banco | `src/main/resources/db/migration/V*__*.sql` |
| Bean compartilhado (Clock, Security, JWT) | `common/adapter/` |
| Tratamento global de erros | `common/adapter/controller/GlobalExceptionHandler.java` |
| Configurações de profile | `src/main/resources/application.yaml` (base) e `application-docker.yml` (profile `docker`) |
| Builds, plugins, JaCoCo, PIT | `build.gradle.kts` |
| Tests / Mothers | `src/test/java/**/*Mother.java` e `*Test.java` (espelha estrutura de `src/main`) |

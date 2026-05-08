# Testes

## Sumário

- [1. Estratégia](#1-estratégia)
- [2. Estrutura de pastas](#2-estrutura-de-pastas)
- [3. Object Mother (Martin Fowler)](#3-object-mother-martin-fowler)
- [4. Tasks Gradle](#4-tasks-gradle)
- [5. Testes unitários](#5-testes-unitários)
- [6. Testes de adapters (gateway e controller)](#6-testes-de-adapters-gateway-e-controller)
- [7. Testes de integração](#7-testes-de-integração)
- [8. JaCoCo (cobertura de linhas)](#8-jacoco-cobertura-de-linhas)
- [9. PIT (mutation testing)](#9-pit-mutation-testing)
- [10. Como adicionar um teste novo](#10-como-adicionar-um-teste-novo)
- [11. Race conditions](#11-race-conditions)

---

## 1. Estratégia

Pirâmide de testes adaptada à arquitetura hexagonal:

- **Base — use cases puros.**
  - Mockito mocka os ports (`UserRepository`, `CryptoGateway`, etc.).
  - Não sobe contexto Spring.
- **Meio — adapters.**
  - **Gateway com circuit breaker:** verifica fallback do Resilience4j.
  - **Controller (web layer):** `@WebMvcTest` + MockMvc isolando o controller; mocks dos use cases.
- **Topo — integração.** 
  - `@SpringBootTest` + Testcontainers (Postgres real) + WireMock (clients externos).
  - Roda fluxos ponta a ponta, inclusive cenários de concorrência.

Distribuição atual:

| Tipo | Classes | Tecnologias | Task Gradle |
|---|---|---|---|
| Unit (use case) | 6 | JUnit 5 + Mockito + AssertJ | `./gradlew test` |
| Adapter (gateway) | 2 | Mockito + Resilience4j | `./gradlew test` |
| Adapter (web layer) | 3 | `@WebMvcTest` + MockMvc | `./gradlew test` |
| Integração | 4 | `@SpringBootTest` + Testcontainers + WireMock | `./gradlew integrationTest` |
| Object Mothers | 6 (auxiliares) | — | — |

---

## 2. Estrutura de pastas

`src/test/` espelha `src/main/` para que cada classe de produção fique lado a lado com seu(s) teste(s):

```
src/test/java/com/wastecoder/picpay/
├── PicpaySimplificadoApplicationIntegrationTest.java
├── transaction/
│   ├── TransactionMother.java
│   ├── TransferIntegrationTest.java
│   ├── usecases/
│   │   └── TransferUseCaseImplTest.java
│   └── adapter/
│       ├── client/
│       │   └── TransferValidationGatewayImplTest.java
│       └── controller/
│           ├── TransferRequestMother.java
│           └── TransactionControllerTest.java
└── user/
    ├── UserMother.java
    ├── NotificationMother.java
    ├── DepositIntegrationTest.java
    ├── ListUsersIntegrationTest.java
    ├── usecases/
    │   ├── CreateUserUseCaseImplTest.java
    │   ├── LoginUserUseCaseImplTest.java
    │   ├── DepositUseCaseImplTest.java
    │   ├── ListUsersUseCaseImplTest.java
    │   └── GetUserByIdUseCaseImplTest.java
    └── adapter/
        ├── client/
        │   └── NotifyUserGatewayImplTest.java
        └── controller/
            ├── CreateUserRequestMother.java
            ├── LoginUserRequestMother.java
            ├── UserControllerTest.java     ← cobre create + deposit + list + getById
            └── AuthControllerTest.java
```

> Os testes web dos endpoints novos (deposit / list / getById) reaproveitam `UserMother` e construção inline do payload — não há `DepositRequestMother` separada hoje.

Não há `application-test.yml` — propriedades são injetadas em runtime via `@DynamicPropertySource` (ver §7).

---

## 3. Object Mother (Martin Fowler)

Padrão criado por Martin Fowler para construção de fixtures de teste: builders centralizados que retornam instâncias prontas, com defaults sensatos, evitando duplicação e ruído nos testes. Cada teste só sobrescreve o que importa para o cenário.

→ Decisão registrada em [adr/0004-testes-com-object-mother-pattern.md](adr/0004-testes-com-object-mother-pattern.md).

**Mothers existentes:**

| Mother | Para quê | Exemplos de método |
|---|---|---|
| `UserMother` | Modelo `User` (domínio) | `validCommonUser()`, `commonUserWith(id, balance)`, `commonUserWith(id, balance, email, document)`, `merchantUserWith(id, balance)`, `validMerchantUser()`, `userWithBlankFullName()` |
| `TransactionMother` | Modelo `Transaction` | `transactionOf(from, target, value)` |
| `NotificationMother` | Payloads de `notify-user` | builders de `NotifyUserRequest` |
| `CreateUserRequestMother` | DTO HTTP `CreateUserRequest` | `valid()`, variações |
| `LoginUserRequestMother` | DTO HTTP `LoginUserRequest` | `valid()`, variações |
| `TransferRequestMother` | DTO HTTP `TransferRequest` | `valid()`, `withTargetIdAndValue(target, value)`, `withBlankTargetId()`, `withZeroValue()`, ... |

**Convenções adotadas no projeto:**

- Classe `final`, construtor privado, **só métodos estáticos** — não se instancia uma Mother.
- Constantes `*_DEFAULT` no topo do arquivo concentram os valores padrão.
- `valid*()` para o caso feliz; `*With*(args)` para variações controladas; `*WithBlank*` / `*WithZero*` / `*Invalid*` para casos negativos.
- Mother retorna o tipo final (modelo de domínio ou record HTTP); não vaza detalhes do builder Lombok.

---

## 4. Tasks Gradle

| Task | O que faz |
|---|---|
| `./gradlew test` | Roda **só os testes unitários e de adapter**. Configurado em `build.gradle.kts` para excluir `**/*IntegrationTest.class`. Encadeia `jacocoTestReport`. |
| `./gradlew integrationTest` | Custom task registrada em `build.gradle.kts`. Inclui **somente** `**/*IntegrationTest.class`. Encadeia `jacocoTestReport`. |
| `./gradlew jacocoTestReport` | Gera relatório de cobertura em `build/reports/jacoco/test/html/index.html`. |
| `./gradlew jacocoTestCoverageVerification` | Falha o build se thresholds não forem atendidos. Encadeada por `./gradlew check`. |
| `./gradlew check` | Roda testes + verificação de cobertura. |
| `./gradlew pitest` | Mutation testing (depende de `tasks.test`). |
| `./gradlew test --tests "FQN.Classe.metodo"` | Roda um teste específico. |

---

## 5. Testes unitários

Todos em `**/usecases/*Test.java`. Padrão:

- `@ExtendWith(MockitoExtension.class)`
- `@Mock` nos ports de saída (`UserRepository`, `CryptoGateway`, `TokenGateway`, `NotifyUserGateway`, `TransferValidationGateway`, `TransactionRepository`, `Clock`).
- `@InjectMocks` no `*UseCaseImpl`.
- AAA (Arrange / Act / Assert) com AssertJ.
- Mothers para construir os modelos de entrada.

Cobrem casos felizes e variantes de erro (ex.: `CreateUserUseCaseImplTest` valida senha curta, e-mail/documento duplicados; `TransferUseCaseImplTest` cobre `MERCHANT` enviando, saldo insuficiente, validador negando, etc.).

---

## 6. Testes de adapters (gateway e controller)

### 6.1 Gateway com circuit breaker

`TransferValidationGatewayImplTest`, `NotifyUserGatewayImplTest`:

- Mockam o `*Client` Feign.
- Verificam:
  - Caminho normal — chamada delega para o client e mapeia o resultado.
  - Falha técnica — exceção do client cai no `fallbackMethod` e retorna o valor seguro (`DENIED` no validator, no-op no notifier).

### 6.2 Web layer

`UserControllerTest`, `AuthControllerTest`, `TransactionControllerTest`:

- `@WebMvcTest(SeuController.class)` — sobe **só** o slice web (sem JPA, sem clients).
- `@MockBean` para os use cases.
- `MockMvc` faz a chamada HTTP simulada.
- Verificam:
  - Status HTTP (201, 200, 4xx).
  - Headers (ex.: `Location` em `POST /api/v1/users`).
  - Corpo de resposta (JSON path).
  - Validação de bean — request inválido → 400 com `messages` esperadas.
  - Mapeamento de `ApplicationException` → status correto via `GlobalExceptionHandler` (importar o handler no slice quando necessário).

---

## 7. Testes de integração

### 7.1 `PicpaySimplificadoApplicationIntegrationTest`

Smoke: `@SpringBootTest` + Testcontainers (Postgres 16) garante que o contexto sobe com schema real e Flyway aplicado.

### 7.2 `TransferIntegrationTest`

Suite ponta a ponta do fluxo de transferência:

- `@SpringBootTest(webEnvironment = RANDOM_PORT)`
- `@Testcontainers` com `PostgreSQLContainer<>("postgres:16-alpine")`
- `@RegisterExtension` com `WireMockExtension` (porta dinâmica) para `transfer-validation` e `notify-user`
- `@DynamicPropertySource` injeta JDBC URL/credenciais e overrides dos URLs Feign:
  - `spring.datasource.url`, `spring.datasource.username`, `spring.datasource.password`
  - `spring.cloud.openfeign.client.config.transfer-validation.url`
  - `spring.cloud.openfeign.client.config.notify-user.url`
- `@BeforeEach` reseta WireMock e limpa as tabelas (`DELETE FROM transactions; DELETE FROM users`).
- `TestRestTemplate` faz as chamadas HTTP reais.

Cenários:

| Teste | O que valida |
|---|---|
| **Happy path** | Validador autoriza → saldos atualizados, 1 linha em `transactions`, `notify-user` recebeu 1 chamada com `email` correto. |
| **Validador nega** | WireMock devolve `403 Forbidden` → resposta 422, **rollback** (saldos intactos, 0 linhas em `transactions`, `notify-user` não foi chamado). |
| **Concorrência** | 10 threads disparam transferência de R$ 10 a partir de saldo R$ 99 → saldo final ≥ 0, soma sender+target == saldo inicial, número de sucessos entre 1 e 9, e `transactions` tem exatamente N linhas (N = sucessos). |

---

## 8. JaCoCo (cobertura de linhas)

- **Versão:** `0.8.12`
- **Thresholds (em `jacocoTestCoverageVerification`):**
  - `com.wastecoder.picpay.*.usecases.*` → mínimo **85 %** (regra `CLASS`).
  - Global → mínimo **60 %**.
- **Excludes** (não contam para cobertura):
  - `**/adapter/controller/request/**`
  - `**/adapter/controller/response/**`
  - `**/domain/viewmodels/**`
  - `**/adapter/repository/entity/**`
  - `PicpaySimplificadoApplication.class`
  - `JwtTokenConfiguration.class`
- **Como rodar:**
  - `./gradlew test jacocoTestReport` → relatório em `build/reports/jacoco/test/html/index.html`.
  - `./gradlew check` → roda verificação de thresholds (falha o build se não atender).
- **Importante:** `tasks.test` exclui `**/*IntegrationTest.class`, então a cobertura do `jacocoTestReport` reflete apenas os testes unitários e de adapter. Para incluir integração, rodar `./gradlew integrationTest jacocoTestReport`.

---

## 9. PIT (mutation testing)

Mutation testing complementa o JaCoCo: introduz mutações no bytecode (mudar `>` para `>=`, remover invocações, negar booleans...) e verifica se algum teste falha. Mutação não-detectada = teste fraco.

- **Plugin:** `info.solidsoft.pitest 1.19.0-rc.1`, `pitestVersion = 1.17.4`, `junit5PluginVersion = 1.2.1`.
- **Mutadores:** `STRONGER` (conjunto agressivo).
- **Thresholds:**
  - Mutation: **80 %**
  - Coverage: **80 %**
- **Targets (classes mutadas):**
  - `com.wastecoder.picpay.user.usecases.*`
  - `com.wastecoder.picpay.transaction.usecases.*`
  - `com.wastecoder.picpay.user.adapter.client.*`
  - `com.wastecoder.picpay.transaction.adapter.client.*`
  - `UserController`, `AuthController`, `TransactionController`
- **Target tests:** os respectivos `*Test` dessas classes.
- **Saída:** `build/reports/pitest/index.html` (HTML + XML).
- **Como rodar:** `./gradlew pitest` (depende de `tasks.test`).
- **Como abrir relatório:** `start build/reports/pitest/index.html` no terminal.

---

## 10. Como adicionar um teste novo

Receita por camada:

1. **Use case (unit):**
   - Crie/reuse Mothers em `*/UserMother.java`, `*/TransactionMother.java`.
   - `@ExtendWith(MockitoExtension.class)` + `@Mock` ports + `@InjectMocks` use case.
   - Cubra happy path **e** todos os ramos de exceção do use case (cada `throw` é um teste).

2. **Gateway:**
   - Mocke o `*Client`.
   - Verifique caminho normal e fallback (forçando `*Client` a lançar exceção).

3. **Controller:**
   - `@WebMvcTest(SeuController.class)`, `@MockBean` para o use case.
   - Importe `GlobalExceptionHandler` no slice se for testar mapeamento de `ApplicationException`.
   - Crie/reuse uma `*RequestMother` para os cenários inválidos.

4. **Integração:**
   - Estenda o padrão de `TransferIntegrationTest` (Testcontainers + WireMock + `@DynamicPropertySource`).
   - Sufixo `*IntegrationTest` é obrigatório (a task `integrationTest` filtra por esse padrão).
   - Lembre de `resetAll()` no WireMock e `DELETE FROM` nas tabelas no `@BeforeEach`.

---

## 11. Race conditions

Transferências concorrentes do mesmo remetente são uma classe de bug clássica em sistemas de pagamento. A defesa do projeto tem três camadas, e uma delas é o teste de concorrência:

- **Camada 1 — Banco:**
  - constraint `CHECK (balance >= 0)` em `users.balance` (V1__create_users.sql).
- **Camada 2 — Aplicação:**
  - `UserEntityDatabase.updateBalanceWithMinusOperation` é `@Modifying` JPQL — `UPDATE users SET balance = balance - ?` direto, sem read-modify-write.
  - Combinado com `@Transactional` no `TransferUseCaseImpl.execute`, qualquer falha pós-débito faz rollback.
- **Camada 3 — Teste:**
  - `TransferIntegrationTest.preventsNegativeBalance_underContention` dispara 10 threads simultâneas tentando transferir R$ 10 a partir de R$ 99.
  - Asserts:
    - `senderBalance.signum() >= 0` (nunca fica negativo).
    - `senderBalance + targetBalance == initialBalance` (não some nem aparece dinheiro).
    - `successes ∈ [1, 9]` (pelo menos 1 passa, pelo menos 1 é negada).
    - Linhas em `transactions` == número de sucessos.

→ Detalhes da decisão em [adr/0003-atomic-balance-update-via-jpql.md](adr/0003-atomic-balance-update-via-jpql.md).

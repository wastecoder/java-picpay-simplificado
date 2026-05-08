# Documentação — Solução técnica

Esta API expõe endpoints para **criar usuário**, **listar / consultar usuário**, **depositar saldo**, **autenticar** e **efetuar transferências** entre usuários. Outros endpoints possíveis no escopo do desafio original não foram implementados.

## Sumário

- [1. Stack](#1-stack)
- [2. Arquitetura](#2-arquitetura)
- [3. Schema do banco de dados](#3-schema-do-banco-de-dados)
- [4. Erros HTTP](#4-erros-http)
- [5. Endpoints](#5-endpoints)
  - [5.1 Usuário](#51-usuário)
    - [5.1.1 POST /api/v1/users](#511-post-apiv1users)
    - [5.1.2 POST /api/v1/users/{user_id}/deposit](#512-post-apiv1usersuser_iddeposit)
    - [5.1.3 GET /api/v1/users](#513-get-apiv1users)
    - [5.1.4 GET /api/v1/users/{user_id}](#514-get-apiv1usersuser_id)
  - [5.2 Autenticação](#52-autenticação)
  - [5.3 Transferências](#53-transferências)
- [6. OpenAPI / Swagger UI](#6-openapi--swagger-ui)

---

## 1. Stack

- **Linguagem:** Java 21
- **Framework:** Spring Boot 3.2.6
- **Spring Cloud:** 2023.0.0 — OpenFeign (HTTP client) + Resilience4j (circuit breaker)
- **Persistência:** Spring Data JPA + Hibernate
- **Migrations:** Flyway
- **Segurança:** Spring Security + `com.auth0:java-jwt` (JWT HS512)
- **Hash de senha:** BCrypt
- **Validação de entrada:** Bean Validation (`jakarta.validation`)
- **Documentação OpenAPI:** springdoc-openapi 2.x (Swagger UI)
- **Banco:** PostgreSQL 16 (runtime), H2 (no classpath, não usado por padrão)
- **Testes:** JUnit 5, Mockito, AssertJ, Testcontainers, WireMock
- **Cobertura:** JaCoCo (linhas) + PIT (mutation testing)

---

## 2. Arquitetura

Hexagonal por feature module — ver [ARCHITECTURE.md](ARCHITECTURE.md) para detalhes, diagramas e decisões.

**Módulos:**
- `user/` — cadastro, login, autenticação JWT.
- `transaction/` — transferências entre usuários.
- `common/` — exceções base, persistência base, segurança, configurações compartilhadas.

**Camadas (em cada módulo):**
- `domain/` — núcleo puro (modelos, ports, exceptions, viewmodels).
- `usecases/` — implementações dos use cases (`@Service`).
- `adapter/` — controllers, repositórios, clients Feign, crypto, token.

---

## 3. Schema do banco de dados

```mermaid
erDiagram
    USERS {
        bigserial id PK
        uuid external_id
        varchar full_name
        varchar document
        varchar email
        varchar password
        user_type type
        numeric balance
        timestamp created_at
        timestamp updated_at
    }
    TRANSACTIONS {
        bigserial id PK
        bigint from_user_id FK
        bigint target_user_id FK
        numeric value
        text description
        timestamp created_at
        timestamp updated_at
    }
    USERS ||--o{ TRANSACTIONS : "envia (from_user_id)"
    USERS ||--o{ TRANSACTIONS : "recebe (target_user_id)"
```

**Observações:**
- Enum Postgres `user_type` com valores `COMMON`, `MERCHANT`.
- `users.external_id` é o `UUID` que cruza a fronteira da aplicação (URLs, JWT `sub`); `users.id` é interno.
- Constraint `BALANCE_NONNEGATIVE`: `CHECK (balance >= 0)` — saldo nunca fica negativo, mesmo sob concorrência.
- `transactions.from_user_id` e `target_user_id` têm `ON DELETE CASCADE`.
- `created_at` / `updated_at` são preenchidos pelo `AbstractJpaPersistable<Long>` (em `common/adapter/repository/`).

---

## 4. Erros HTTP

Todos os erros são serializados pelo `GlobalExceptionHandler` (`@RestControllerAdvice`) no formato `ErrorResponse`:

```json
{
  "timestamp": "2026-05-05T12:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "messages": ["email can't be blank."]
}
```

| Status | Origem | Quando ocorre |
|---|---|---|
| **400 Bad Request** | `MethodArgumentNotValidException` | Validação de bean falhou (`@NotBlank`, `@Positive`, etc). `messages` traz uma string por campo inválido. |
| **400 Bad Request** | `HttpMessageNotReadableException` | JSON malformado ou ausente. `messages: ["Malformed JSON"]`. |
| **400 Bad Request** | `ConstraintViolationException` / `IllegalArgumentException` | Violação de constraint em parâmetros (`@Validated` em path/query) ou path param UUID inválido (ex.: `{user_id}` mal-formado em deposit / get-by-id). |
| **400 Bad Request** | `UserTypeNotFoundException` | `type` enviado em `POST /api/v1/users` não bate com `COMMON` ou `MERCHANT`. |
| **404 Not Found** | `UserNotFoundException` | Usuário (remetente, destinatário, alvo do depósito, login ou lookup direto) não existe. |
| **409 Conflict** | `EmailAlreadyRegisteredException` | E-mail já cadastrado. |
| **409 Conflict** | `DocumentAlreadyRegisteredException` | Documento já cadastrado. |
| **412 Precondition Failed** | `IncorrectPasswordException` | Senha incorreta no login. |
| **412 Precondition Failed** | `InsufficientBalanceException` | Saldo do remetente é menor que o valor da transferência. |
| **412 Precondition Failed** | `UserCantTransferException` | Usuário do tipo `MERCHANT` tentou enviar transferência. |
| **422 Unprocessable Entity** | `TransferNotAllowedException` | Serviço externo `transfer-validation` retornou `DENIED` (ou caiu — fallback nega por segurança). |
| **500 Internal Server Error** | `TransferValidationFailException` | Falha técnica ao consultar `transfer-validation` (não capturada pelo fallback do circuit breaker). |
| **500 Internal Server Error** | `Exception` (catch-all) | Qualquer outra exceção não tratada. `messages: ["Internal server error"]`. |

> ⚠️ **Estado atual da segurança:** `SecurityConfiguration` está com `anyRequest().permitAll()` e sessões `STATELESS`. Apesar de `TransactionController` ter `@SecurityRequirement(name = "Bearer Token")` (anotação Swagger), **nenhum endpoint exige token de fato**. O `POST /api/v1/auth/login` continua emitindo JWT corretamente, mas a checagem do header em rotas protegidas ainda não foi implementada.

---

## 5. Endpoints

### 5.1 Usuário

#### 5.1.1 `POST /api/v1/users`

Cria um usuário (`COMMON` ou `MERCHANT`).

**Request body:**

| Campo | Tipo | Validação | Descrição |
|---|---|---|---|
| `full_name` | string | `@NotBlank` | Nome completo. |
| `document` | string | `@NotBlank` | CPF (para `COMMON`) ou CNPJ (para `MERCHANT`). Deve ser único. |
| `email` | string | `@NotBlank` | E-mail. Deve ser único. |
| `password` | string | `@NotBlank` | Texto puro; armazenado como hash BCrypt. |
| `type` | string | `@NotBlank` + `COMMON \| MERCHANT` | Tipo de usuário. |

**Exemplo de request:**

```json
{
  "full_name": "Joana Silva",
  "document": "000.000.000-00",
  "email": "joana@example.com",
  "password": "password123",
  "type": "COMMON"
}
```

**Respostas:**

| Status | Body | Quando |
|---|---|---|
| **201 Created** | _(vazio)_ | Sucesso. Header `Location: /api/v1/users/{id}` aponta para o usuário criado. |
| **400 Bad Request** | `ErrorResponse` | Algum campo inválido (`@NotBlank`) ou `type` fora do enum. |
| **409 Conflict** | `ErrorResponse` | E-mail ou documento já cadastrados. |

---

#### 5.1.2 `POST /api/v1/users/{user_id}/deposit`

Credita um valor positivo no saldo do usuário identificado por `user_id`. Reusa o mesmo mecanismo atômico de saldo do `transfer` (`updateBalanceWithPlusOperation`, `@Modifying` JPQL — ver [ADR-0003](adr/0003-atomic-balance-update-via-jpql.md)), só que sem `transfer-validation` nem `notify-user`.

> ⚠️ Hoje `DepositUseCaseImpl` **não restringe por tipo de usuário** — tanto `COMMON` quanto `MERCHANT` podem depositar. Confirmar regra de negócio antes de bloquear `MERCHANT` se for o caso.

**Path param:**

| Param | Tipo | Descrição |
|---|---|---|
| `user_id` | UUID (string) | `external_id` do usuário a creditar. |

**Request body:**

| Campo | Tipo | Validação | Descrição |
|---|---|---|---|
| `value` | decimal | `@NotNull` + `@Positive` | Valor a creditar. Deve ser maior que zero. |

**Exemplo de request:**

```http
POST /api/v1/users/9c2b1f3e-…/deposit
Content-Type: application/json

{
  "value": 100.00
}
```

**Resposta 200 OK:**

| Campo | Tipo | Formato | Descrição |
|---|---|---|---|
| `user_id` | UUID (string) | — | `external_id` do usuário creditado. |
| `new_balance` | decimal | — | Saldo do usuário após o crédito (re-lido do banco). |
| `deposited_at` | timestamp | `yyyy-MM-dd'T'HH:mm` | Momento em que o depósito foi confirmado (gerado com `Clock` injetado). |

```json
{
  "user_id": "9c2b1f3e-…",
  "new_balance": 250.00,
  "deposited_at": "2026-05-08T12:30"
}
```

**Erros:**

| Status | Causa | Quando |
|---|---|---|
| **400 Bad Request** | Validação | `value` ausente, zero ou negativo. |
| **400 Bad Request** | UUID inválido | `user_id` não é um UUID válido. |
| **404 Not Found** | `UserNotFoundException` | Usuário com esse `external_id` não existe. |

---

#### 5.1.3 `GET /api/v1/users`

Lista paginada de usuários expondo apenas dados públicos (sem `email`, `document`, `password` ou `balance`). Útil para descobrir IDs sem precisar logar com cada usuário.

**Query params:**

| Param | Tipo | Default | Descrição |
|---|---|---|---|
| `page` | int | `0` | Índice da página (0-based). |
| `size` | int | `20` | Tamanho da página. |
| `sort` | string | `fullName,asc` | Formato `<campo>,<asc\|desc>` (case-insensitive na direção). Ex.: `sort=fullName,desc`. |

**Exemplo de request:**

```http
GET /api/v1/users?page=0&size=10&sort=fullName,asc
```

**Resposta 200 OK** — `PageResponse<UserSummaryResponse>`:

| Campo | Tipo | Descrição |
|---|---|---|
| `content` | array | Itens da página. Cada item é `UserSummaryResponse`. |
| `page` | int | Índice da página devolvida. |
| `size` | int | Tamanho da página. |
| `total_elements` | long | Total de usuários no banco. |
| `total_pages` | int | Total de páginas. |
| `last` | bool | `true` se for a última página. |

`UserSummaryResponse` (cada item de `content`):

| Campo | Tipo | Descrição |
|---|---|---|
| `id` | UUID (string) | `external_id` do usuário. |
| `full_name` | string | Nome completo. |
| `type` | string | `COMMON` ou `MERCHANT`. |

```json
{
  "content": [
    { "id": "9c2b1f3e-…", "full_name": "Joana Silva", "type": "COMMON" },
    { "id": "f1a2b3c4-…", "full_name": "Padaria Central", "type": "MERCHANT" }
  ],
  "page": 0,
  "size": 10,
  "total_elements": 2,
  "total_pages": 1,
  "last": true
}
```

> Itens **não** expõem `email`, `document`, `password` ou `balance` — para isso use [§5.1.4 `GET /api/v1/users/{user_id}`](#514-get-apiv1usersuser_id).

---

#### 5.1.4 `GET /api/v1/users/{user_id}`

Devolve o payload completo do usuário (sem senha), incluindo `balance`. Útil para conferir saldo antes/depois de transferência ou depósito sem inspecionar o banco.

**Path param:**

| Param | Tipo | Descrição |
|---|---|---|
| `user_id` | UUID (string) | `external_id` do usuário. |

**Resposta 200 OK** — `UserResponse`:

| Campo | Tipo | Descrição |
|---|---|---|
| `id` | UUID (string) | `external_id` do usuário. |
| `full_name` | string | Nome completo. |
| `document` | string | CPF ou CNPJ. |
| `email` | string | E-mail cadastrado. |
| `type` | string | `COMMON` ou `MERCHANT`. |
| `balance` | decimal | Saldo atual. |

```json
{
  "id": "9c2b1f3e-…",
  "full_name": "Joana Silva",
  "document": "000.000.000-00",
  "email": "joana@example.com",
  "type": "COMMON",
  "balance": 250.00
}
```

**Erros:**

| Status | Causa | Quando |
|---|---|---|
| **400 Bad Request** | UUID inválido | `user_id` não é um UUID válido. |
| **404 Not Found** | `UserNotFoundException` | Usuário com esse `external_id` não existe. |

---

### 5.2 Autenticação

#### `POST /api/v1/auth/login`

Autentica um usuário e devolve um JWT.

**Request body:**

| Campo | Tipo | Validação | Descrição |
|---|---|---|---|
| `email` | string | `@NotBlank` | E-mail cadastrado. |
| `password` | string | `@NotBlank` | Senha em texto puro. |

**Exemplo de request:**

```json
{
  "email": "joana@example.com",
  "password": "password123"
}
```

**Resposta 200 OK:**

| Campo | Tipo | Descrição |
|---|---|---|
| `user_id` | UUID (string) | `external_id` do usuário (mesmo valor que vai no `sub` do JWT). |
| `access_token` | string | JWT assinado com HS512. |
| `expires_in` | long | Tempo de expiração em **segundos** (configurado em `security.jwt.expires-after`). |

```json
{
  "user_id": "9c2b1f3e-…",
  "access_token": "eyJhbGciOiJIUzUxMiJ9…",
  "expires_in": 600
}
```

**Erros:**

| Status | Quando |
|---|---|
| **400 Bad Request** | Campo `email` ou `password` em branco. |
| **404 Not Found** | Usuário não encontrado (`UserNotFoundException`). |
| **412 Precondition Failed** | Senha incorreta (`IncorrectPasswordException`). |

**Estrutura do JWT:**
- `iss` = valor de `security.jwt.issuer`
- `sub` = `external_id` do usuário (UUID)
- `exp` = agora + `security.jwt.expires-after` segundos (default `600` = 10 min)
- Algoritmo: **HS512**, segredo em `security.jwt.secret`

---

### 5.3 Transferências

#### `POST /api/v1/users/{user_id}/transfer`

Envia uma transferência do usuário identificado por `user_id` para outro usuário.

**Headers (planejado, ver §4):**
- `Authorization: Bearer <access_token>` — anotado como obrigatório no Swagger; **atualmente o filtro de autenticação não está ativo**, então a requisição passa sem header.

**Path param:**

| Param | Tipo | Descrição |
|---|---|---|
| `user_id` | UUID (string) | `external_id` do remetente. |

**Request body:**

| Campo | Tipo | Validação | Descrição |
|---|---|---|---|
| `target_id` | UUID (string) | `@NotBlank` | `external_id` do destinatário. |
| `value` | decimal | `@Positive` | Valor da transferência. Deve ser maior que zero. |
| `description` | string | `@NotBlank` | Texto livre anexado à transferência. |

**Exemplo de request:**

```http
POST /api/v1/users/9c2b1f3e-…/transfer
Content-Type: application/json
Authorization: Bearer eyJhbGciOiJIUzUxMiJ9…

{
  "target_id": "f1a2b3c4-…",
  "value": 150.00,
  "description": "almoço"
}
```

**Resposta 200 OK:**

| Campo | Tipo | Formato | Descrição |
|---|---|---|---|
| `sent_date` | timestamp | `yyyy-MM-dd'T'HH:mm` | Momento em que a transferência foi confirmada (gerado com `Clock` injetado). |
| `message` | string | — | Mensagem fixa: `"Transfer sent successfully."`. |

```json
{
  "sent_date": "2026-05-05T12:30",
  "message": "Transfer sent successfully."
}
```

**Erros:**

| Status | Causa | Quando |
|---|---|---|
| **400 Bad Request** | Validação | `target_id` em branco, `value` ausente/zero/negativo, `description` em branco, `user_id` ou `target_id` não são UUID válidos. |
| **404 Not Found** | `UserNotFoundException` | Remetente ou destinatário não encontrado. |
| **412 Precondition Failed** | `UserCantTransferException` | Remetente é `MERCHANT` (só pode receber). |
| **412 Precondition Failed** | `InsufficientBalanceException` | Saldo do remetente é menor que `value`. |
| **422 Unprocessable Entity** | `TransferNotAllowedException` | `transfer-validation` retornou `DENIED` ou está fora do ar (fallback nega por segurança). |
| **500 Internal Server Error** | `TransferValidationFailException` | Falha técnica ao chamar `transfer-validation` que escapou do circuit breaker. |

> Nota sobre notificação: ao final de uma transferência bem-sucedida, a API tenta chamar `notify-user`. Se o serviço estiver fora ou o circuit breaker abrir, **a transferência continua válida** — a notificação é best-effort. Ver fluxo completo em [ARCHITECTURE.md §4.3](ARCHITECTURE.md#43-transferência).

---

## 6. OpenAPI / Swagger UI

Com a aplicação rodando, a especificação OpenAPI 3 fica disponível em:

- Swagger UI: <http://localhost:8080/swagger-ui.html>
- JSON da spec: <http://localhost:8080/v3/api-docs>

Os controllers usam `@Tag`, `@Operation` e `@ApiResponse` (springdoc-openapi) — então a Swagger UI traz exemplos de payload e descrições já anotados nos próprios `*Request` records (ex.: `@Schema(example = "user@email.com")`).

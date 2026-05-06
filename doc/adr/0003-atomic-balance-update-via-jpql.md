# ADR-0003: Atualização atômica de saldo via JPQL `@Modifying`

## Status

Aceito.

## Contexto

O fluxo de transferência debita o saldo do remetente e credita o do destinatário. Numa abordagem ingênua:

```java
User from = userRepository.findById(...);
if (from.balance().compareTo(value) < 0) throw ...;
from.setBalance(from.balance().subtract(value));   // read-modify-write
userRepository.save(from);
```

Esse padrão **read-modify-write** falha sob concorrência:

1. Thread A lê saldo R$ 99.
2. Thread B lê saldo R$ 99.
3. Ambas validam que R$ 99 ≥ R$ 10 e calculam saldo final R$ 89.
4. Ambas persistem R$ 89 — saldo deveria ser R$ 79.

Em sistemas de pagamento isso é inaceitável: um remetente pode gastar mais do que tem. Lock pessimista (`SELECT ... FOR UPDATE`) resolve, mas serializa transações inteiras e cria pontos de contenção pesados.

## Decisão

A atualização de saldo **não passa pela aplicação como cálculo**. O delta vai direto ao banco em um único `UPDATE` parametrizado, declarado no `*Database` (Spring Data) com `@Modifying`:

```java
// UserEntityDatabase
@Modifying
@Query("UPDATE UserEntity u SET u.balance = u.balance - :value WHERE u.id = :id")
void updateBalanceWithMinusOperation(@Param("id") Long id, @Param("value") BigDecimal value);

@Modifying
@Query("UPDATE UserEntity u SET u.balance = u.balance + :value WHERE u.id = :id")
void updateBalanceWithPlusOperation(@Param("id") Long id, @Param("value") BigDecimal value);
```

Combinado com:

- **Constraint de banco:** `CHECK (balance >= 0)` em `users.balance` (ver `V1__create_users.sql`). Saldo nunca fica negativo, mesmo se a aplicação falhar em validar.
- **Transação Spring:** `TransferUseCaseImpl.execute` é `@Transactional`. Débito + crédito + `INSERT transactions` rodam na mesma transação; qualquer exceção causa rollback automático.
- **Ordem de operações:** débito antes do crédito. Se o débito violar o `CHECK`, o banco lança `DataIntegrityViolationException`, a transação faz rollback, e o crédito nunca acontece.

Concorrência fica resolvida pelo banco: o `UPDATE` é atômico no nível do row lock do Postgres, e o `CHECK` aborta qualquer caminho que tentaria deixar o saldo negativo.

## Alternativas consideradas

- **Read-modify-write com `@Transactional`.** Rejeitada: como descrito no contexto, transações com `READ_COMMITTED` (default Postgres) não detectam o conflito; ambas leem o mesmo valor inicial.
- **Lock pessimista (`@Lock(LockModeType.PESSIMISTIC_WRITE)`).** Rejeitada: serializa transferências do mesmo remetente bloqueando a row inteira pela duração da transação (incluindo as chamadas externas de validação e notificação, que podem ser lentas). O JPQL `UPDATE` é mais rápido — segura o lock só durante a operação.
- **Lock otimista (`@Version`).** Rejeitada: funciona, mas em alta concorrência gera muitos retries. E não previne saldo negativo por si só — ainda precisaria do `CHECK` de qualquer jeito. Combinar lock otimista com `CHECK` seria redundante.
- **Apenas `CHECK` no banco, sem `@Modifying`.** Rejeitada: o `CHECK` impediria o saldo negativo, mas `from.setBalance(...)` + `save()` seria interpretado pelo Hibernate como replace completo da entidade (`UPDATE users SET balance = ?, full_name = ?, ...`). O cálculo continuaria sendo feito em memória — concorrência ainda quebra (duas threads sobrescrevem o mesmo valor calculado).

## Consequências

**Positivas:**
- Concorrência resolvida no nível certo (banco), sem locks pessimistas longos na aplicação.
- Saldo negativo é impossível mesmo na presença de bugs na lógica de validação da aplicação — o `CHECK` é a última linha de defesa.
- Testável: `TransferIntegrationTest.preventsNegativeBalance_underContention` dispara 10 threads simultâneas tentando transferir R$ 10 a partir de R$ 99 e valida que (a) saldo final ≥ 0, (b) sender + target == saldo inicial, (c) `transactions` tem exatamente N linhas (N = sucessos).

**Negativas / custos:**
- O domínio (`User`) não reflete o saldo atualizado após `updateBalanceWithMinusOperation` — o objeto em memória continua com o valor antigo. Quem precisar do saldo atualizado depois precisa recarregar do banco. Hoje, ninguém depende disso no fluxo, mas é uma armadilha em código futuro.
- Quando o `CHECK` dispara, a exceção que sobe é `DataIntegrityViolationException` (Spring), não `InsufficientBalanceException` (domínio). O fluxo de transferência valida saldo na aplicação **antes** do débito justamente para gerar a exception correta com a mensagem certa — então o `CHECK` é defesa em profundidade, não a checagem primária.
- Exige conhecimento explícito de que `@Modifying` desabilita o cache de primeiro nível para a entidade alterada — `entityManager.refresh()` ou `clearAutomatically = true` se o domínio precisar do valor atualizado.

→ Detalhes do fluxo completo em [ARCHITECTURE.md §4.3](../ARCHITECTURE.md#43-transferência) e do teste em [TESTS.md §11](../TESTS.md#11-race-conditions).

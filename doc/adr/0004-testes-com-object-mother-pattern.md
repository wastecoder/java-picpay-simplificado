# ADR-0004: Testes com Object Mother pattern

## Status

Aceito.

## Contexto

À medida que a suíte de testes cresce, a construção de fixtures vira ruído:

- Cada teste constrói um `User` ou `TransferRequest` com 5+ campos, mesmo quando o cenário só se importa com um deles.
- Mudar a assinatura do construtor (ex.: adicionar um campo a `User`) vira refactor em dezenas de arquivos de teste.
- Cenários "quase iguais com uma diferença" — válido vs. com nome em branco vs. com saldo zero — repetem 90% do código.

A intenção do teste fica enterrada sob boilerplate de criação de objeto. Padrões comuns para resolver:

- Construtor "telescópico" com defaults — vira ambíguo quando há muitos overloads.
- Builder fluente em cada teste — verboso e duplicado.
- `@BeforeEach` montando objetos compartilhados — esconde o setup, dificulta entender qual cenário cada teste exercita.

## Decisão

Adotar o **Object Mother pattern** descrito por Martin Fowler para construção de fixtures de teste.

Cada modelo de domínio ou DTO HTTP que aparece em testes ganha uma classe `*Mother` correspondente:

```
src/test/java/com/wastecoder/picpay/
├── user/
│   ├── UserMother.java                                  ← model: User
│   ├── NotificationMother.java                          ← payload: NotifyUserRequest
│   └── adapter/controller/
│       ├── CreateUserRequestMother.java                 ← DTO HTTP
│       └── LoginUserRequestMother.java                  ← DTO HTTP
└── transaction/
    ├── TransactionMother.java                           ← model: Transaction
    └── adapter/controller/
        └── TransferRequestMother.java                   ← DTO HTTP
```

**Convenções adotadas:**

- Classe `final`, construtor privado, **só métodos estáticos** — Mothers não se instanciam.
- Constantes `*_DEFAULT` no topo concentram os valores padrão (`FULL_NAME_DEFAULT`, `EMAIL_DEFAULT`, etc.). Quem mudar um default afeta toda a suíte de uma vez.
- Padrão de nomes:
  - `valid*()` para o caso feliz (ex.: `validCommonUser()`, `validMerchantUser()`).
  - `*With*(args)` para variações controladas (ex.: `commonUserWith(id, balance)`, `withTargetIdAndValue(target, value)`).
  - `*WithBlank*` / `*WithZero*` / `*Invalid*` para casos negativos (ex.: `userWithBlankFullName()`).
- Cada Mother retorna o **tipo final** (modelo de domínio ou record HTTP), não builder Lombok — o teste recebe o objeto pronto.

## Alternativas consideradas

- **Builder Lombok inline em cada teste.** Rejeitada: cada teste define seus defaults; mudar um campo do modelo cascateia em N arquivos.
- **`@BeforeEach` com fixtures compartilhadas.** Rejeitada: esconde o setup e torna obscuro qual cenário cada teste exercita. Difícil reutilizar entre classes de teste.
- **Test Data Builder (Nat Pryce).** Considerada — é uma evolução do Mother com builder fluente. Mais flexível, mas adiciona uma camada para um projeto onde os modelos têm poucos campos. Mother é mais simples e cabe bem.
- **Frameworks como Instancio/EasyRandom.** Rejeitada: random data esconde regressões (um teste pode passar por sorte). Para domínio de pagamento, dados explícitos são preferíveis.

## Consequências

**Positivas:**
- Testes leem como cenários: `UserMother.commonUserWith(id, BigDecimal.valueOf(99))` deixa óbvio que o valor relevante é o saldo.
- Mudar o construtor de `User` afeta uma única `UserMother` — os testes seguem inalterados se os defaults derem conta.
- Casos negativos ficam explícitos e reutilizáveis (ex.: `withBlankTargetId()` é uma fixture compartilhada entre `TransactionControllerTest` e `TransferUseCaseImplTest`).
- Convenção de nomes (`valid*`, `*With*`, `invalid*`) torna previsível o que esperar ao olhar uma Mother.

**Negativas / custos:**
- Mais arquivos no `src/test/`. Para um projeto pequeno isso é overhead aceitável; para projetos enormes, pode escalar mal sem disciplina (Mothers de Mothers).
- Os defaults nas Mothers podem mascarar problemas se forem "sempre válidos" mesmo quando regras mudam. Disciplina: revisar Mothers quando uma regra de domínio muda (ex.: validação de e-mail, tamanho mínimo de senha).
- Mother que cobre muitos cenários vira "deus": viola SRP da própria Mother. Sinal de que vale dividir em duas (ex.: `UserMother` puro × `MerchantUserMother`) — não é o caso atual, mas vigiar.

→ Detalhes da estrutura e exemplos em [TESTS.md §3](../TESTS.md#3-object-mother-martin-fowler).

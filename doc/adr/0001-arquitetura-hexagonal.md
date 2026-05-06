# ADR-0001: Arquitetura hexagonal por feature module

## Status

Aceito.

## Contexto

O projeto é um exercício de aprendizado de arquitetura. Os objetivos pedagógicos incluem:

- Isolar regra de negócio de framework (Spring/JPA), para que a lógica seja testável sem subir contexto e independente de detalhes de infraestrutura.
- Tornar explícita a fronteira entre o que é "núcleo" da aplicação e o que é "plumbing" (persistência, HTTP, segurança, etc.).
- Permitir que cada feature evolua sem acoplamento estrutural com as outras.

Soluções comuns no ecossistema Java/Spring tendem a organizar o código em **camadas técnicas globais** (`controllers/`, `services/`, `repositories/`), mas isso mistura features distintas no mesmo pacote e diluiu o limite domínio↔framework.

## Decisão

Adotar **arquitetura hexagonal por feature module**:

- Cada feature (`user`, `transaction`) é um pacote de topo com suas próprias camadas internas:
  ```
  <feature>/
    domain/      ← núcleo puro (modelos, ports, exceptions, viewmodels)
    usecases/    ← implementações dos use cases (@Service)
    adapter/    ← controllers, repositórios JPA, clients HTTP, crypto, token
  ```
- O pacote `common/` agrupa o que **cruza features**: exceções base, persistência base (`AbstractJpaPersistable`), segurança, configurações compartilhadas.
- **Regra de dependência:** sempre apontando para dentro — `adapter/` pode importar `domain/` e `usecases/`; `domain/` **nunca** importa `adapter/`. `usecases/` importa apenas `domain/`.
- Convenção de nomes carrega o papel arquitetural (ver tabela "Suffix → Papel → Localização" em [ARCHITECTURE.md §2](../ARCHITECTURE.md#2-estrutura-de-pacotes)).

## Alternativas consideradas

- **Camadas técnicas globais (`controllers/`, `services/`, `repositories/`).** Rejeitada: acopla features distintas no mesmo pacote, dilui a fronteira domínio↔framework e atrapalha a refatoração modular.
- **DDD com bounded contexts em módulos Gradle separados.** Rejeitada: overhead alto para um projeto de estudo. Pacotes Java oferecem o isolamento necessário sem o custo de gerenciar múltiplos `build.gradle`.
- **Camadas dentro de cada feature, mas sem separar `domain/` de `adapter/`.** Rejeitada: perde-se exatamente o ponto da hexagonal — manter o núcleo livre de framework.

## Consequências

**Positivas:**
- Domínio é Java puro, testável com Mockito e sem `@SpringBootTest`.
- Trocar Spring por outro framework afeta principalmente `adapter/` — o núcleo continua válido.
- Cada feature é localmente coesa: ao mexer em "transferências", quase tudo está em `transaction/`.
- A direção de dependência é fácil de auditar (basta procurar imports cruzando para fora do `domain/`).

**Negativas / custos:**
- Mais arquivos por feature em comparação ao layout clássico Spring.
- Convenção de sufixos (`*UseCase`, `*UseCaseImpl`, `*Database`, `*Entity`, `*Gateway`, `*GatewayImpl`, `*Client`, `*Command`, `*Result`, `*Request`, `*Response`) vira contrato — quem não a conhece se perde. Documentado em [ARCHITECTURE.md §2](../ARCHITECTURE.md#2-estrutura-de-pacotes).
- A separação modelo de domínio ↔ entidade JPA exige mappers explícitos (`adapter/repository/mapper/`). É código a mais, mas é o que mantém o domínio puro.

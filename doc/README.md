# Documentação

Esta pasta concentra a documentação técnica do projeto `picpay-simplificado`.

## Documentos

| Documento | Para quê serve |
|---|---|
| [ARCHITECTURE.md](ARCHITECTURE.md) | Visão geral da arquitetura hexagonal: estrutura de pacotes, decisões, fluxos críticos (criar usuário, login, transferência) e diagramas Mermaid. |
| [DOCUMENTATION.md](DOCUMENTATION.md) | Referência da API REST: stack, schema do banco, erros HTTP, especificação de cada endpoint com payload e respostas. |
| [TESTS.md](TESTS.md) | Estratégia de testes: pirâmide adaptada, Object Mother, tasks Gradle, JaCoCo, PIT, integração com Testcontainers + WireMock, race conditions. |
| [DEVELOPMENT.md](DEVELOPMENT.md) | Setup local: pré-requisitos, comandos, profiles, variáveis de ambiente, Docker, Flyway, troubleshooting. |
| [adr/](adr/) | Architecture Decision Records — registro de decisões técnicas com contexto, alternativas e consequências. |

## ADRs

| ADR | Tema |
|---|---|
| [0001](adr/0001-arquitetura-hexagonal.md) | Arquitetura hexagonal por feature module |
| [0002](adr/0002-circuit-breaker-por-client-externo.md) | Circuit breaker por client externo (Resilience4j) |
| [0003](adr/0003-atomic-balance-update-via-jpql.md) | Atualização atômica de saldo via JPQL `@Modifying` + `CHECK` |
| [0004](adr/0004-testes-com-object-mother-pattern.md) | Testes com Object Mother pattern |

## Por onde começar

- Quero **usar a API** → [DOCUMENTATION.md](DOCUMENTATION.md)
- Quero **rodar localmente** → [DEVELOPMENT.md](DEVELOPMENT.md)
- Quero **entender o código** → [ARCHITECTURE.md](ARCHITECTURE.md)
- Quero **mexer ou adicionar testes** → [TESTS.md](TESTS.md) + [ARCHITECTURE.md](ARCHITECTURE.md)
- Quero **entender por que uma decisão foi tomada** → [adr/](adr/)

## Convenções de manutenção

- **Mermaid sempre que possível** — diagramas viram código versionável e renderizam direto no GitHub.
- **Tabelas em vez de prosa** ao enumerar campos, comandos, variáveis ou status HTTP.
- **Linkar entre docs** quando um conceito aparece em mais de um lugar (ex.: `ARCHITECTURE.md §4.3` → `DOCUMENTATION.md §5.3`), em vez de duplicar conteúdo.
- **Datar decisões nos ADRs** quando o status mudar (`Aceito` → `Substituído por ADR-NNNN` em DD/MM/AAAA).
- **Adicionar ADR antes de mudar uma decisão estrutural** — não silenciosamente. O histórico das alternativas é tão valioso quanto a decisão final.

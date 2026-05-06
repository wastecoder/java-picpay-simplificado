# ADR-0002: Circuit breaker por client externo

## Status

Aceito.

## Contexto

A aplicação depende de dois serviços HTTP externos:

- **`transfer-validation`** — autorizador que decide se uma transferência pode prosseguir. Bloqueante: a transferência só ocorre se ele responder `ALLOWED`.
- **`notify-user`** — notifica o destinatário após a transferência. Best-effort: a transferência é válida mesmo se a notificação falhar.

Riscos típicos com dependências HTTP externas:

- Latência alta acumulando threads do servidor.
- Falhas em cascata: se o autorizador fica lento, todas as transferências travam esperando timeout.
- Tempestades de retry quando o serviço externo volta de uma queda.

Sem proteção, basta uma indisponibilidade do `transfer-validation` para inutilizar a feature de transferências e, dependendo do volume, derrubar a aplicação.

## Decisão

Cada client externo é envolvido em **três camadas com o mesmo nome lógico** (`notify-user`, `transfer-validation`):

1. **`*Client` Feign** — interface declarativa do contrato HTTP cru (ex.: `TransferValidationClient`, `NotifyUserClient`), em `<feature>/adapter/client/`.
2. **`*GatewayImpl`** — `@Component` que implementa o output port do domínio, encapsula o `*Client` e adiciona `@CircuitBreaker(name = "<n>", fallbackMethod = "...")` do Resilience4j. Cada gateway tem um `fallbackMethod(... , Throwable ex)` que loga a falha e devolve um valor seguro:
   - `TransferValidationGatewayImpl.transferValidationFallback` → retorna `TransferValidationResult.DENIED` (segurança em primeiro lugar: serviço fora = transferência negada).
   - `NotifyUserGatewayImpl.*Fallback` → no-op (notificação é best-effort).
3. **Configuração** em `application.yaml` com o **mesmo nome** em duas seções:
   - `spring.cloud.openfeign.client.config.<n>` (URL + timeouts)
   - `resilience4j.circuitbreaker.instances.<n>` (`baseConfig: default`)

Config `default` compartilhada (ver [DEVELOPMENT.md §6](../DEVELOPMENT.md#6-configuração-de-feign--resilience4j)): `COUNT_BASED`, taxa de falha 50%, mínimo 5 chamadas, 60s em `OPEN`, 3 chamadas em `HALF_OPEN`, `feign.FeignException$BadRequest` ignorado (400 do cliente não conta como falha do serviço).

## Alternativas consideradas

- **`RestTemplate` + retry manual + `try/catch`.** Rejeitada: código repetido em cada gateway, sem isolamento de falhas em cascata, sem janela deslizante de avaliação.
- **`WebClient` reativo.** Rejeitada: introduziria modelo reativo numa stack inteiramente bloqueante (Servlet + JPA), aumentando complexidade sem ganho proporcional para o escopo.
- **Circuit breaker centralizado (single instance global).** Rejeitada: uma falha do `notify-user` derrubaria também o `transfer-validation`. Instância por client é o nível certo de granularidade.
- **Hystrix.** Rejeitada: deprecated. Resilience4j é o sucessor recomendado pelo ecossistema Spring Cloud.

## Consequências

**Positivas:**
- Falha em um client externo não vaza para os outros — o blast radius fica contido.
- Fallback **explícito** força a decisão consciente sobre o que acontece quando o serviço cai (negar? ignorar? retornar cache?). Isso ficou óbvio na decisão entre `transfer-validation` (negar) e `notify-user` (ignorar).
- Config declarativa em `application.yaml` permite tunar timeouts e thresholds sem tocar no código.
- Testável: `TransferValidationGatewayImplTest` e `NotifyUserGatewayImplTest` verificam o fallback forçando o `*Client` a lançar exceção.

**Negativas / custos:**
- "Três pontos com o mesmo nome" é frágil: errar o nome em qualquer um dos lugares (`@CircuitBreaker(name = ...)`, Feign config, Resilience4j instance) faz o Spring Cloud falhar no boot ou desabilita silenciosamente o breaker.
- O fallback do `transfer-validation` retornar `DENIED` significa que **uma queda do autorizador derruba 100% das transferências**. É a escolha certa por segurança, mas precisa estar documentada no runbook de oncall.
- O `notify-user` está dentro de um método `@Transactional`. Se uma exceção escapar do fallback (raro, mas possível em modos `OPEN` durante transição), a transferência inteira sofre rollback. Vigiar.

# Digital Bank API

API REST simplificada para um banco digital: gestão de contas, transferências entre contas com controle de concorrência, notificações após transferências, autenticação JWT, papel de administrador com acesso irrestrito, e documentação interativa via Swagger. Java 21, Spring Boot 4, PostgreSQL, Docker (banco de dados), com frontend estático servido pelo próprio Spring Boot.

## Índice

- [Stack tecnológica](#stack-tecnológica)
- [Como executar](#como-executar)
- [Solução de problemas com Docker e Testcontainers](#solução-de-problemas-com-docker-e-testcontainers)
- [Contas de demonstração](#contas-de-demonstração)
- [Estrutura do projeto](#estrutura-do-projeto)
- [Decisões de arquitetura e implementação](#decisões-de-arquitetura-e-implementação)
- [Endpoints da API](#endpoints-da-api)
- [Testes](#testes)
- [Limitações conhecidas e próximos passos](#limitações-conhecidas-e-próximos-passos)

## Stack tecnológica

- **Linguagem/Framework:** Java 21, Spring Boot 4 (Spring Framework 7, Jakarta EE 11)
- **Persistência:** Spring Data JPA (Hibernate), PostgreSQL 16
- **Migrations:** Flyway
- **Segurança:** Spring Security 7, JWT (biblioteca `jjwt`)
- **Documentação da API:** springdoc-openapi (Swagger UI), disponível em `/swagger-ui.html`
- **Build:** Maven, com Maven Wrapper (`./mvnw`)
- **Containerização:** Docker Compose (aplicação Spring Boot e PostgreSQL)
- **Testes:** JUnit 5, Mockito, AssertJ, Testcontainers

## Como executar

### Stack completa, apenas com Docker (forma recomendada)

Nenhuma instalação local de Java ou Maven é necessária, apenas Docker.

```bash
cp .env.example .env
sed -i "s|replace-with-output-of-openssl-rand-base64-48|$(openssl rand -base64 48)|" .env
docker compose up --build
```

A aplicação sobe em `http://localhost:8080`. O frontend estático (login, registro, transferências, notificações) fica disponível na raiz (`/`), e a documentação interativa da API em `/swagger-ui.html`. O primeiro build baixa e compila as dependências Maven, o que pode levar alguns minutos, nas próximas vezes fica bem mais rápido devido ao cache de camadas do Docker.

Para derrubar tudo, incluindo o volume do banco de dados:

```bash
docker compose down -v
```

### Alternativa para desenvolvimento local (via Maven Wrapper)

Útil para iteração rápida no código sem reconstruir a imagem a cada mudança. Requer Java 21 instalado localmente.

```bash
docker compose up -d postgres
./mvnw spring-boot:run
```

Os testes de integração usam Testcontainers, portanto o Docker precisa estar em execução e acessível pelo usuário que executa o Maven. Se a suíte de testes falhar com `Could not find a valid Docker environment`, veja a seção de solução de problemas abaixo antes de investigar o código da aplicação.

## Solução de problemas com Docker e Testcontainers

Esta seção cobre os erros mais comuns ao executar a aplicação com Docker Compose ou ao rodar os testes de integração com Testcontainers.

### Docker daemon está ativo, mas o terminal aponta para o socket errado

Sintoma típico:

```bash
Cannot connect to the Docker daemon at unix:///home/<usuario>/.docker/desktop/docker.sock. Is the docker daemon running?
```

Esse erro normalmente indica que o Docker CLI está apontando para o contexto do Docker Desktop, mas o Docker Desktop não está em execução, ou não é o backend que você pretende usar. Em Linux com Docker Engine local, verifique o contexto ativo:

```bash
docker context ls
docker context use default
unset DOCKER_HOST
```

Depois valide novamente:

```bash
docker info
```

### Docker daemon está rodando, mas o usuário não tem permissão

Sintoma típico ao rodar `docker info`, `docker compose up --build` ou `./mvnw test`:

```bash
permission denied while trying to connect to the Docker daemon socket at unix:///var/run/docker.sock
```

Ou, durante os testes com Testcontainers:

```bash
DOCKER_HOST unix:///var/run/docker.sock is not listening
Caused by: com.sun.jna.LastErrorException: [13] Permission denied
Could not find a valid Docker environment
```

Nesse caso, o Docker está ativo, mas o usuário atual não pertence ao grupo `docker`. Confirme primeiro se o daemon está em execução:

```bash
sudo systemctl status docker
```

Se estiver parado:

```bash
sudo systemctl start docker
```

Adicione o usuário atual ao grupo `docker`:

```bash
sudo usermod -aG docker $USER
newgrp docker
```

Confirme que o grupo foi aplicado:

```bash
id -nG
docker info
docker run --rm hello-world
```

Se `docker` ainda não aparecer em `id -nG`, feche e abra o terminal novamente, ou encerre e reabra a sessão do usuário. Depois rode:

```bash
./mvnw test
docker compose up --build
```

Como solução temporária, é possível subir a stack com `sudo`:

```bash
sudo docker compose up --build
```

É recomendável evitar usar `sudo ./mvnw test` como solução definitiva, porque Maven pode criar arquivos em `target/` ou no cache local com dono `root`, causando problemas de permissão depois.

### Testcontainers falha mesmo após o Docker funcionar sem sudo

Se `docker info` e `docker run --rm hello-world` funcionarem sem `sudo`, mas os testes ainda falharem com `Could not find a valid Docker environment`, então o problema pode ser de compatibilidade entre Testcontainers, `docker-java` e a versão do Docker Engine. Este projeto inclui o arquivo:

```text
src/test/resources/docker-java.properties
```

com:

```properties
api.version=1.44
```

Esse arquivo força o cliente Docker usado pelo Testcontainers a negociar uma versão de API compatível com Docker Engine recente. Se o arquivo for removido, versões recentes do Docker podem falhar antes de iniciar o container de teste.

## Contas de demonstração

Criadas automaticamente na primeira inicialização:

| Usuário | Senha | Papel |
|---|---|---|
| `alice` | `password123` | USER |
| `bruno` | `password123` | USER |
| `carla` | `password123` | USER |
| `admin` | `admin` | ADMIN |

**Atenção:** as credenciais são deliberadamente fracas, servem apenas para este projeto de demonstração.

## Estrutura do projeto

```
src/main/java/com/example/digitalbank/
├── DigitalBankApplication.java
├── config/          (OpenApiConfig, seeders de dados de demonstração)
├── security/        (JwtService, JwtAuthenticationFilter, SecurityConfig, AccountUserDetailsService)
│   └── utils/       (SecurityUtils)
├── domain/          (Entidades JPA: Account, TransferRecord, Notification)
├── dto/
│   ├── request/     (DTOs de entrada: CreateAccountRequest, TransferRequest, LoginRequest, etc.)
│   └── response/    (DTOs de saída: AccountResponse, AccountSummaryResponse, etc.)
├── repository/      (Interfaces Spring Data JPA)
├── service/         (TransferService e TransferTransactionalOps, lógica de transferência)
├── event/           (TransferCompletedEvent e NotificationListener)
├── controller/      (REST controllers)
└── exception/       (Exceções de domínio e GlobalExceptionHandler)

src/main/resources/
├── application.yml
├── db/migration/    (Migrations Flyway, V1 a V5)
└── static/          (Frontend estático em HTML, CSS e JS puro, sem build step)

src/test/java/com/example/digitalbank/
├── service/         (Testes unitários da lógica de transferência)
├── security/        (Testes unitários do JwtService)
└── integration/     (Testes de integração com Testcontainers)
```

## Decisões de arquitetura e implementação

### Modelagem de dados e migrations

O schema é gerenciado exclusivamente via Flyway, nunca por `ddl-auto=update` do Hibernate (configurado como `validate`, que apenas confere se as entidades JPA correspondem ao schema real, sem nunca alterá-lo). Cada mudança estrutural é um novo arquivo de migration versionado:

- **V1:** tabelas `accounts`, `transfers` e `notifications`, com `CHECK` constraints no próprio banco (saldo nunca negativo, valor de transferência sempre positivo) como última linha de defesa, independente da lógica da aplicação.
- **V2:** três contas de demonstração pré carregadas (Alice, Bruno, Carla), com UUIDs gerados via `gen_random_uuid()`.
- **V3:** separação do campo único `owner_name` em `first_name` e `last_name`.
- **V4:** colunas `username` e `password_hash`, nulas propositalmente (populadas em runtime pelo seeder de credenciais de demonstração, não fixadas diretamente no SQL).
- **V5:** coluna `role`, com `DEFAULT 'USER'` para as contas já existentes.

Regra do projeto: qualquer alteração de schema é sempre uma nova migration, nunca uma alteração manual via `psql` ou ferramenta de sincronização de IDE. Isso garante que o banco de qualquer ambiente fique idêntico ao schema descrito no código.

### Controle de concorrência nas transferências

Requisito central do desafio original. Estratégia adotada: bloqueio pessimista (`SELECT ... FOR UPDATE`), com ordenação determinística. Ambas as contas envolvidas em uma transferência são bloqueadas sempre na mesma ordem (a de UUID menor primeiro), independentemente da direção da transferência. Isso é toda a prevenção de deadlock: duas transferências concorrentes em direções opostas entre as mesmas duas contas tentam adquirir os bloqueios na mesma ordem, então uma espera a outra em vez de ambas ficarem presas esperando uma pela outra.

Comprovado por `ConcurrentTransferIntegrationTest`: 50 transferências concorrentes disparadas contra as mesmas duas contas, verificando que o saldo total do sistema permanece exatamente conservado e que nenhuma conta jamais fica negativa.

### Separação entre a camada pública e a transacional

`TransferService` (pública) delega para `TransferTransactionalOps`, que tem visibilidade de pacote (package private, não pública) e é anotada com `@Transactional`. Isso impede que qualquer código fora do pacote `service` chame a lógica transacional diretamente e pule a camada pública, o que é relevante caso uma política de retry sobre falhas de lock venha a ser adicionada no futuro.

### Notificações via evento após o commit

Após uma transferência ser persistida, um `TransferCompletedEvent` é publicado. O `NotificationListener` usa `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)`, executando apenas após a transação já ter sido efetivamente commitada. Assim, uma transferência que falhe nunca gera notificação.

Detalhe descoberto durante o desenvolvimento: métodos `AFTER_COMMIT` executam em um momento em que a transação original já terminou. Chamar `repository.save(...)` nesse ponto, sem cuidado extra, pode silenciosamente não persistir nada, sem lançar exceção alguma. A correção foi adicionar `@Transactional(propagation = Propagation.REQUIRES_NEW)` ao método do listener, forçando uma transação genuinamente nova para a gravação da notificação.

### Autenticação: JWT, não OAuth2

Optou se por JWT emitido pela própria aplicação, em vez de um fluxo OAuth2 completo. OAuth2 se justifica quando há um servidor de autorização separado ou múltiplos clientes consumindo a mesma identidade. Uma única API de primeira parte, emitindo tokens para seus próprios clientes, não precisa dessa camada extra de infraestrutura.

### Autorização por conta e papel de administrador

Toda rota que expõe dados de uma conta específica verifica que o solicitante é o próprio titular (ou um administrador), através de um único ponto reutilizado: `SecurityUtils.canAccess`. Endpoints exclusivos de administração vivem sob `/api/admin/**`, protegidos no nível da configuração de segurança (`hasRole("ADMIN")`).

**Detalhe de implementação encontrado durante o desenvolvimento:** ao negar acesso a `/api/admin/**` para um usuário sem papel de administrador, o Spring Security lança `AccessDeniedException`, que deveria resultar em `403`. Porém, o Tomcat intercepta a chamada `response.sendError()` e realiza um forward interno para `/error`, ou seja, uma segunda passagem completa pelo filtro de segurança, já sem o contexto de autenticação original. Sem uma regra explícita liberando `/error`, essa segunda passagem falha como anônima, resultando em `401` e mascarando a resposta real (`403`) que o Spring Security já havia decidido corretamente. Corrigido adicionando `/error` à lista de caminhos públicos na configuração de segurança.

### Tratamento de erros

Um único `@RestControllerAdvice` (`GlobalExceptionHandler`) traduz cada exceção de domínio para o status HTTP correto: conta não encontrada (`404`), saldo insuficiente ou transferência inválida (`422`), acesso não autorizado (`403`), credenciais inválidas (`401`), nome de usuário já em uso (`409`), e erros de validação de campos (`400`).

## Endpoints da API

Documentação interativa completa disponível em `/swagger-ui.html` após subir a aplicação. Resumo:

| Método | Rota | Autenticação | Descrição |
|---|---|---|---|
| POST | `/api/auth/login` | Pública | Login, retorna um JWT |
| POST | `/api/accounts` | Pública | Registro de nova conta |
| GET | `/api/accounts` | JWT | Lista contas (sem saldo, apenas nome e id) |
| GET | `/api/accounts/{id}` | JWT (própria conta ou admin) | Detalhe completo de uma conta, incluindo saldo |
| POST | `/api/transfers` | JWT (própria conta de origem ou admin) | Realiza uma transferência. Aceita um header opcional `Idempotency-Key`: uma segunda chamada com a mesma chave retorna o resultado da primeira, sem processar a transferência de novo |
| GET | `/api/notifications/{accountId}` | JWT (própria conta ou admin) | Lista notificações de uma conta |
| GET | `/api/admin/accounts` | JWT com papel ADMIN | Lista todas as contas, com saldo |
| GET | `/api/admin/transfers` | JWT com papel ADMIN | Lista todas as transferências do sistema |

## Testes

- **Testes unitários** (`service`, `security`): lógica de transferência (`TransferTransactionalOpsTest`) e geração e validação de JWT (`JwtServiceTest`), sem contexto Spring e sem banco de dados. Rodam em milissegundos.
- **Testes de integração** (`integration`), usando Testcontainers para subir um PostgreSQL real por teste:
  - `ConcurrentTransferIntegrationTest`: dispara 50 transferências concorrentes entre duas contas e verifica que o saldo total do sistema permanece o mesmo, e que nenhuma conta jamais fica negativa. É o teste que efetivamente comprova a estratégia de bloqueio descrita acima, e não apenas a lógica de negócio isolada.
  - `AuthorizationIntegrationTest`: prova via HTTP real que uma conta não pode transferir a partir da conta de outra pessoa, e que uma conta sem papel de administrador não consegue acessar `/api/admin/**`.

**Nota sobre Testcontainers e Docker:** se os testes de integração falharem com `Could not find a valid Docker environment`, primeiro diferencie os dois cenários mais comuns:

1. Se o log contiver `Permission denied` ao acessar `unix:///var/run/docker.sock`, o problema é permissão do usuário no Docker. A solução é adicionar o usuário ao grupo `docker`, conforme descrito na seção [Solução de problemas com Docker e Testcontainers](#solução-de-problemas-com-docker-e-testcontainers).
2. Se `docker info` e `docker run --rm hello-world` funcionarem sem `sudo`, mas o Testcontainers ainda falhar, verifique o arquivo `src/test/resources/docker-java.properties`. Versões recentes do Docker Engine (29 ou superior) exigem API mínima 1.44, enquanto combinações recentes de Testcontainers e `docker-java` podem tentar negociar a partir de uma versão antiga. O projeto contorna isso com `api.version=1.44`.

## Limitações conhecidas e próximos passos

- Os testes não são executados durante a construção da imagem Docker (`mvn package` roda com `-DskipTests` no `Dockerfile`), já que os testes de integração precisam de acesso ao próprio Docker (via Testcontainers), o que normalmente não está disponível dentro do processo de build de uma imagem. Rode `./mvnw test` separadamente, como parte do fluxo de desenvolvimento ou de um pipeline de integração contínua.
- Sem limitação de taxa (*rate limiting*) no endpoint de login.
- Sem *refresh tokens*. O JWT expira (padrão de 60 minutos) e exige novo login.
- O token é armazenado em `localStorage` no frontend, o que é funcional para esta demonstração, mas um cookie `httpOnly` seria a escolha mais robusta contra XSS em um ambiente de produção real.
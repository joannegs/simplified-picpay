# PicPay Simplificado

<p>
  <img src="https://img.shields.io/badge/Java-17-orange?logo=openjdk&logoColor=white" alt="Java 17"/>
  <img src="https://img.shields.io/badge/Spring%20Boot-3.2.4-brightgreen?logo=springboot&logoColor=white" alt="Spring Boot 3.2.4"/>
  <img src="https://img.shields.io/badge/Maven-build-C71A36?logo=apachemaven&logoColor=white" alt="Maven"/>
  <img src="https://img.shields.io/badge/H2-database-blue?logo=h2&logoColor=white" alt="H2 Database"/>
  <img src="https://img.shields.io/badge/JUnit%205-tests-25A162?logo=junit5&logoColor=white" alt="JUnit 5"/>
  <img src="https://img.shields.io/badge/status-em%20estudo-yellow" alt="Status"/>
</p>

Uma API REST que simula o backend de um serviço de pagamentos peer-to-peer nos moldes do PicPay: cadastro de usuários (comuns e lojistas), transferência de dinheiro entre contas e validações de negócio como saldo, tipo de usuário e autorização externa da transação.

## Sobre o projeto

Este repositório foi construído como um projeto de estudo focado em aprofundar conhecimentos em **Spring Boot** e nas práticas comuns de uma API backend em Java: arquitetura em camadas, injeção de dependência, persistência com JPA, tratamento centralizado de exceções, integração com serviços externos via `RestTemplate` e testes unitários com mocks.

Mais do que "fazer funcionar", o objetivo foi entender o *porquê* de cada decisão — como separar responsabilidades entre `controller`, `service` e `repository`, como modelar regras de negócio no domínio, e como cobrir esse comportamento com testes isolados e confiáveis.

## Funcionalidades

- Cadastro de usuários, com dois perfis distintos: `COMMON` (pode enviar e receber dinheiro) e `MERCHANT` / lojista (pode apenas receber)
- Listagem de todos os usuários cadastrados
- Transferência de dinheiro entre dois usuários, com:
  - Validação de saldo suficiente do remetente
  - Bloqueio de envio por parte de lojistas
  - Autorização externa da transação via serviço mock (simulando um serviço antifraude)
  - Notificação assíncrona ao remetente e ao destinatário após a conclusão
- Tratamento centralizado de erros, com respostas HTTP e mensagens padronizadas para cada tipo de falha

## Tecnologias utilizadas

| Categoria         | Tecnologia                                    |
|-------------------|------------------------------------------------|
| Linguagem         | Java 17                                         |
| Framework         | Spring Boot 3.2.4 (Web, Data JPA)               |
| Persistência      | Spring Data JPA + H2 (banco em memória)         |
| Build             | Maven (com Maven Wrapper)                       |
| Redução de boilerplate | Lombok                                     |
| Integração HTTP   | RestTemplate (serviços externos de autorização e notificação) |
| Testes            | JUnit 5, Mockito, AssertJ                       |

## Arquitetura

O projeto segue uma arquitetura em camadas, organizada por responsabilidade:

```
src/main/java/com/picpaysimplificado
├── controllers/     # Endpoints REST (UserController, TransactionController)
├── services/        # Regras de negócio (UserService, TransactionService, NotificationService)
├── repositories/     # Acesso a dados via Spring Data JPA
├── domain/           # Entidades JPA (User, Transaction, UserType)
├── DTOs/              # Records usados como contrato de entrada/saída da API
├── exception/          # Exceções de negócio customizadas
└── infra/                # Configurações e tratamento global de exceções
```

Essa separação mantém os controllers finos (apenas orquestram requisição/resposta), concentra as regras de negócio nos services e isola o acesso a dados nos repositories — facilitando tanto a manutenção quanto a escrita de testes unitários isolados por camada.

## Regras de negócio

1. Um usuário do tipo `MERCHANT` não pode enviar dinheiro, apenas receber.
2. O remetente precisa ter saldo igual ou superior ao valor da transação.
3. Toda transação passa por um serviço externo de autorização; se ele negar, a transação é bloqueada.
4. Após uma transação autorizada, o saldo de remetente e destinatário é atualizado e ambos recebem uma notificação. Toda a operação é executada dentro de uma transação (`@Transactional`), garantindo atomicidade: qualquer falha no meio do processo desfaz as alterações já feitas.
5. Falhas em qualquer etapa (usuário inexistente, saldo insuficiente, não autorizado, serviço de notificação indisponível) interrompem a operação e retornam uma mensagem de erro clara.

## Endpoints da API

### Usuários

| Método | Rota      | Descrição                     |
|--------|-----------|--------------------------------|
| POST   | `/users`  | Cria um novo usuário           |
| GET    | `/users`  | Lista todos os usuários        |

**Exemplo de requisição — `POST /users`**
```json
{
  "firstName": "Joanne",
  "lastName": "Silva",
  "document": "12345678900",
  "balance": 500.00,
  "email": "joanne@email.com",
  "password": "senha123",
  "userType": "COMMON"
}
```

### Transações

| Método | Rota          | Descrição                          |
|--------|---------------|--------------------------------------|
| POST   | `/transactions` | Realiza uma transferência entre dois usuários |

**Exemplo de requisição — `POST /transactions`**
```json
{
  "value": 100.00,
  "senderId": 1,
  "receiverId": 2
}
```

## Como executar o projeto

**Pré-requisitos:** Java 17+ instalado. O Maven não precisa estar instalado globalmente — o projeto já inclui o Maven Wrapper.

```bash
# clone o repositório
git clone https://github.com/joannegs/picpay-simplificado.git
cd picpay-simplificado

# execute a aplicação (Windows)
.\mvnw.cmd spring-boot:run

# execute a aplicação (Linux/macOS)
./mvnw spring-boot:run
```

A API sobe por padrão em `http://localhost:8080`.

O projeto utiliza um banco H2 em memória, sem necessidade de configuração adicional. O console do H2 fica disponível em `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:mem:testbd`, usuário: `sa`, sem senha).

## Como rodar os testes

```bash
# Windows
.\mvnw.cmd test

# Linux/macOS
./mvnw test
```

O projeto conta com testes unitários organizados por classe, cobrindo as regras de negócio dos services, o comportamento dos controllers e o tratamento global de exceções — usando Mockito para isolar dependências externas (repositórios e chamadas HTTP).

```
src/test/java/com/picpaysimplificado
├── controllers/
│   ├── UserControllerTest.java
│   └── TransactionControllerTest.java
├── services/
│   ├── UserServiceTest.java
│   ├── TransactionServiceTest.java
│   └── NotificationServiceTest.java
└── infra/
    └── ControllerExceptionHandlerTest.java
```

## Possíveis evoluções

Ideias para continuar evoluindo o projeto como exercício de aprendizado:

- Autenticação e autorização com Spring Security + JWT
- Documentação interativa da API com Swagger/OpenAPI
- Migração do H2 para um banco relacional persistente (PostgreSQL) em produção
- Testes de integração com `@SpringBootTest` e `@DataJpaTest`
- Containerização com Docker e pipeline de CI/CD

## Autor

Desenvolvido por **Joanne Silva** como projeto de estudo em Spring Boot.

- GitHub: [@joannegs](https://github.com/joannegs)
- E-mail: outtblues@gmail.com

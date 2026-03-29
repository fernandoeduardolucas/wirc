# Relatório Técnico — WIRC

## 1. Enquadramento do projeto

O **WIRC** é uma aplicação de chat multi-salas desenvolvida no âmbito da unidade curricular de **Paradigmas Emergentes para o Desenvolvimento Web e Mobile**. A solução foi construída para cumprir os objetivos do enunciado, combinando:

- **GraphQL** para operações de leitura e mutação orientadas ao domínio.
- **WebSockets** para entrega de notificações e mensagens em tempo real.
- **Front-end web** com Angular.
- **Back-end** com Spring Boot.
- **Persistência relacional** com PostgreSQL, JPA e Liquibase.

A aplicação suporta autenticação básica, criação de utilizadores, gestão de salas, envio de mensagens, pesquisa textual, estatísticas por sala, ranking de utilizadores e notificação de atividade em canais não focados.

---

## 2. Objetivos funcionais concretizados

A solução implementa os principais requisitos funcionais do projeto:

1. **Comunicação em tempo real** através de WebSocket.
2. **Camada de API estruturada com GraphQL** para consultas e mutações.
3. **Arquitetura separada entre front-end e back-end**.
4. **Aplicação explícita de design patterns** no domínio e na infraestrutura.
5. **Persistência de utilizadores, salas e estado da aplicação** em base de dados.
6. **Capacidade de evolução** para novas regras, validações e tipos de notificação.

---

## 3. Tecnologias abordadas

### 3.1 Front-end

#### Angular 20
O front-end foi implementado com **Angular 20**, recorrendo a **standalone components** em vez de módulos tradicionais. Esta opção simplifica a composição da interface e reduz o acoplamento entre artefactos de apresentação.

**Principais vantagens no projeto:**
- Estrutura moderna e modular da interface.
- Separação clara entre componentes visuais e gestão de estado.
- Boa integração com formulários, eventos e programação reativa.
- Facilidade de manutenção e escalabilidade.

#### TypeScript
A utilização de **TypeScript** trouxe tipagem estática para componentes, serviços, modelos e fluxos de estado, reduzindo erros de integração entre front-end e back-end.

#### RxJS
O estado da aplicação no cliente é coordenado com **RxJS**, usando `BehaviorSubject`, `combineLatest`, `switchMap`, `tap` e `catchError`.

**Benefícios obtidos:**
- Atualização reativa do ecrã.
- Propagação consistente de notificações, erro, utilizador autenticado e sala ativa.
- Menor duplicação de lógica assíncrona.

#### HTML + CSS
A camada visual foi construída com templates Angular e folhas de estilo CSS dedicadas por componente, permitindo uma organização mais clara da interface.

### 3.2 Back-end

#### Spring Boot 4
O servidor foi desenvolvido com **Spring Boot 4**, que oferece uma base robusta para aplicações empresariais, com suporte a injeção de dependências, configuração automática e integração com várias tecnologias.

**Uso no projeto:**
- Exposição da API GraphQL.
- Configuração do endpoint WebSocket.
- Organização do domínio de chat em serviços, controladores, repositórios e configuração.

#### Spring GraphQL
Foi usada a stack **Spring for GraphQL** para definir um contrato claro entre cliente e servidor.

**Vantagens para este projeto:**
- O cliente obtém apenas os dados de que necessita.
- Queries e mutations estão centradas no domínio da aplicação.
- Boa evolução da API sem múltiplos endpoints REST.

#### Spring WebSocket
O envio de mensagens e notificações em tempo real foi implementado com **Spring WebSocket**, permitindo manter sessões ativas com os clientes ligados.

#### Spring Data JPA
A persistência usa **JPA** para mapear entidades como utilizadores, salas, membros e mensagens para o modelo relacional.

#### Liquibase
A evolução do esquema da base de dados é controlada com **Liquibase**, garantindo versionamento das alterações estruturais e seed inicial.

### 3.3 Base de dados e infraestrutura

#### PostgreSQL
A solução usa **PostgreSQL** como sistema de gestão de base de dados relacional.

#### Docker e Docker Compose
Existe suporte para execução da infraestrutura do back-end com **Docker** e **Docker Compose**, facilitando a criação do ambiente de desenvolvimento.

### 3.4 Comunicação entre camadas

#### GraphQL
GraphQL é usado para:
- autenticação;
- criação de utilizadores;
- listagem de salas;
- consulta de mensagens;
- pesquisa;
- estatísticas;
- criação de salas;
- gestão de membros;
- foco de sala.

#### WebSockets
WebSockets são usados para:
- envio de mensagens de chat;
- difusão de notificações em tempo real;
- reação imediata da interface a novas mensagens.

---

## 4. Arquitetura da solução

A aplicação segue uma arquitetura cliente-servidor em camadas.

```mermaid
flowchart LR
    A[Angular Front-end] -->|GraphQL queries/mutations| B[Spring Boot Back-end]
    A -->|WebSocket SEND_MESSAGE / notificações| B
    B --> C[Serviço de aplicação / domínio do chat]
    C --> D[(PostgreSQL)]
    C --> E[Gateway de notificações WebSocket]
    D --> C
    E --> A
```

### 4.1 Camada de apresentação
No front-end, os componentes tratam da interação com o utilizador:
- `IdentityComponent` para autenticação e criação de utilizadores.
- `RoomsComponent` para seleção/criação de salas.
- `ChatComponent` para mensagens, pesquisa e estatísticas.
- `CanalComponent` para contexto do canal e gestão de membros.
- `AppComponent` como composição principal da interface.

### 4.2 Camada de estado e integração no front-end
A classe `ChatStore` centraliza o estado reativo da aplicação e funciona como ponto de coordenação entre componentes e o serviço HTTP/WebSocket. Já `ChatService` encapsula os detalhes de comunicação com o servidor.

### 4.3 Camada de entrada no back-end
A classe `WircController` expõe as operações GraphQL. O `ChatWebSocketHandler` recebe mensagens via WebSocket, valida o tipo de comando e encaminha o processamento para o serviço de aplicação.

### 4.4 Camada de aplicação e domínio
A interface `ChatApplication` e a implementação `ChatApplicationImpl` concentram as regras principais:
- autenticação;
- criação de utilizadores;
- gestão de salas;
- envio de mensagens;
- cálculo de estatísticas;
- autorização por utilizador/sala.

### 4.5 Camada de persistência
O acesso à base de dados é feito com repositórios JPA e entidades persistentes. O estado das salas e respetivas mensagens é reconstruído a partir da base de dados e snapshots persistidos.


### 4.6 Modelo ER da base de dados
O modelo de dados persistente organiza-se em torno de utilizadores, salas, associação de membros, mensagens e estado de sessão da sala. Abaixo apresenta-se uma visão ER simplificada alinhada com as entidades JPA e os scripts Liquibase.

```mermaid
erDiagram
    APP_USER ||--o{ CHAT_ROOM_MEMBER : participa
    CHAT_ROOM ||--o{ CHAT_ROOM_MEMBER : contem
    CHAT_ROOM ||--o{ CHAT_MESSAGE : possui
    APP_USER ||--o{ CHAT_MESSAGE : envia
    CHAT_ROOM ||--|| ROOM_SESSION_STATE : estado_atual

    APP_USER {
        string username PK
        string display_name
        string password
    }

    CHAT_ROOM {
        string id PK
        string name
    }

    CHAT_ROOM_MEMBER {
        string room_id PK, FK
        string username PK, FK
    }

    CHAT_MESSAGE {
        string id PK
        string room_id FK
        string username FK
        string message
        datetime sent_at
        boolean highlighted
    }

    ROOM_SESSION_STATE {
        string room_id PK, FK
        string state
        long unread_messages
    }
```

### 4.7 Diagrama de classes da solução
O diagrama seguinte resume as classes mais relevantes do domínio e da integração aplicacional. O objetivo não é representar todos os ficheiros do projeto, mas destacar as dependências e responsabilidades principais.

```mermaid
classDiagram
    class AppComponent
    class ChatStore
    class ChatService
    class WircController
    class ChatWebSocketHandler
    class ChatApplication
    class ChatApplicationImpl
    class ChatRoomFactory
    class MessageValidationHandler
    class RequiredFieldValidationHandler
    class ParticipantValidationHandler
    class MessageLengthValidationHandler
    class RoomSession
    class RoomState
    class FocusedRoomState
    class NotifiedRoomState
    class WebSocketNotificationGateway
    class ChatRoomEntity
    class AppUserEntity
    class ChatMessageEntity
    class ChatRoomMemberEntity
    class RoomSessionStateEntity

    AppComponent --> ChatStore : consome estado
    ChatStore --> ChatService : invoca API
    ChatStore --> ChatService : abre WebSocket
    WircController --> ChatApplication : delega queries/mutations
    ChatWebSocketHandler --> ChatApplication : delega envio
    ChatWebSocketHandler --> WebSocketNotificationGateway : gere sessoes
    ChatApplication <|.. ChatApplicationImpl
    ChatApplicationImpl --> ChatRoomFactory : recria salas
    ChatApplicationImpl --> MessageValidationHandler : valida comandos
    MessageValidationHandler <|-- RequiredFieldValidationHandler
    MessageValidationHandler <|-- ParticipantValidationHandler
    MessageValidationHandler <|-- MessageLengthValidationHandler
    ChatApplicationImpl --> RoomSession : gere estado em memoria
    RoomSession --> RoomState : delega comportamento
    RoomState <|.. FocusedRoomState
    RoomState <|.. NotifiedRoomState
    ChatApplicationImpl --> ChatRoomEntity : persiste salas
    ChatApplicationImpl --> AppUserEntity : persiste utilizadores
    ChatApplicationImpl --> ChatMessageEntity : persiste mensagens
    ChatRoomEntity --> ChatRoomMemberEntity : membros
    ChatRoomMemberEntity --> AppUserEntity : utilizador
    RoomSessionStateEntity --> ChatRoomEntity : estado de leitura
```

### 4.8 Diagrama de fluxo do utilizador
O fluxo seguinte representa a jornada principal do utilizador desde a autenticação até à interação em tempo real com as salas de chat.

```mermaid
flowchart TD
    A[Entrar na aplicação] --> B{Já tem conta?}
    B -->|Sim| C[Autenticar com utilizador e password]
    B -->|Não| D[Criar utilizador]
    D --> E[Autenticação automática]
    C --> F[Listar salas disponíveis]
    E --> F
    F --> G[Selecionar sala]
    G --> H[Carregar mensagens e estatísticas]
    H --> I{Ação pretendida}
    I -->|Enviar mensagem| J[Enviar comando por WebSocket]
    J --> K[Back-end valida e persiste]
    K --> L[Difundir notificação e atualizar interface]
    L --> I
    I -->|Pesquisar mensagens| M[Executar query GraphQL]
    M --> N[Mostrar resultados]
    N --> I
    I -->|Criar sala| O[Mutation GraphQL createRoom]
    O --> P[Atualizar lista de salas]
    P --> I
    I -->|Adicionar membro| Q[Mutation GraphQL addMemberToRoom]
    Q --> R[Atualizar participantes e permissões]
    R --> I
```

---

## 4.9 Utilização do WebSocket na aplicação

O WebSocket é usado exclusivamente para a componente **tempo real** da aplicação, isto é, para o envio de mensagens e para a difusão imediata de notificações para todos os clientes ligados. No front-end, a ligação é aberta para `ws://localhost:8080/wirc/chat`. No back-end, esse endpoint é registado na configuração WebSocket e tratado por `ChatWebSocketHandler`.

### Como o fluxo funciona
1. O front-end abre a ligação WebSocket quando a `ChatStore` é construída.
2. O servidor conclui o handshake, regista a sessão no gateway e envia uma mensagem inicial do tipo `CONNECTED`.
3. Quando o utilizador envia uma mensagem, o Angular envia um payload JSON do tipo `SEND_MESSAGE`.
4. O `ChatWebSocketHandler` recebe o payload, converte-o para `WebSocketChatCommand` e delega o processamento em `ChatApplication`.
5. O serviço valida a mensagem, persiste o estado, cria a notificação e pede ao `WebSocketNotificationGateway` para a difundir.
6. Todos os clientes com socket aberto recebem a notificação `NEW_MESSAGE`.

### Contrato de entrada do WebSocket
O cliente envia mensagens JSON com a seguinte estrutura:

```json
{
  "type": "SEND_MESSAGE",
  "roomId": "room-equipa",
  "activeUser": "Ana",
  "user": "Ana",
  "message": "Bruno, valida o endpoint GraphQL comigo.",
  "focusedRoom": true
}
```

**Significado dos campos:**
- `type`: tipo do comando WebSocket; atualmente a aplicação aceita `SEND_MESSAGE`.
- `roomId`: identificador da sala onde a mensagem será enviada.
- `activeUser`: utilizador autenticado no cliente.
- `user`: utilizador em cujo nome a mensagem é enviada.
- `message`: conteúdo textual da mensagem.
- `focusedRoom`: indica se a sala está atualmente em foco no cliente emissor.

### Contrato de saída do WebSocket
Depois de processar uma mensagem, o servidor envia notificações JSON como as seguintes:

```json
{
  "roomId": "room-equipa",
  "roomName": "Equipa",
  "preview": "Bruno, valida o endpoint GraphQL comigo.",
  "user": "Ana",
  "type": "NEW_MESSAGE",
  "messageId": "<uuid>",
  "sentAt": "2026-03-22T15:00:00Z",
  "highlighted": true
}
```

Além disso, no arranque da ligação o cliente recebe uma mensagem deste género:

```json
{
  "type": "CONNECTED",
  "preview": "Ligação websocket estabelecida.",
  "highlighted": false
}
```

Se existir erro de validação ou payload inválido, o servidor responde com uma mensagem do tipo `ERROR`.

### Exemplo prático com Ana e Bruno
Considere o seguinte cenário:
- A **Ana** e o **Bruno** estão ligados à aplicação.
- Ambos abriram a sala `room-equipa`.
- A Ana envia a mensagem: `Bruno, valida o endpoint GraphQL comigo.`

#### O que acontece no front-end
1. A `ChatStore` da Ana chama `chatService.sendMessage(...)`.
2. O `ChatService` envia o JSON `SEND_MESSAGE` pelo socket.
3. O back-end processa a mensagem.
4. O gateway difunde a notificação para a sessão da Ana e para a sessão do Bruno.
5. O front-end recebe o evento e atualiza mensagens, estatísticas, top utilizadores e estado das salas.

#### Exemplo representativo dos logs do back-end
Os valores de `sessionId` e `messageId` variam em cada execução, mas o formato observado nos logs será semelhante a este:

```text
INFO  WebSocket handshake concluído: sessionId=ana-session, remoteAddress=/127.0.0.1:53001, uri=ws://localhost:8080/wirc/chat
INFO  Sessão WebSocket registada: sessionId=ana-session, totalSessoes=1
INFO  Mensagem CONNECTED enviada após handshake: sessionId=ana-session

INFO  WebSocket handshake concluído: sessionId=bruno-session, remoteAddress=/127.0.0.1:53002, uri=ws://localhost:8080/wirc/chat
INFO  Sessão WebSocket registada: sessionId=bruno-session, totalSessoes=2
INFO  Mensagem CONNECTED enviada após handshake: sessionId=bruno-session

INFO  Mensagem WebSocket recebida: sessionId=ana-session, payload={"type":"SEND_MESSAGE","roomId":"room-equipa","activeUser":"Ana","user":"Ana","message":"Bruno, valida o endpoint GraphQL comigo.","focusedRoom":true}
INFO  Encaminhando mensagem entre utilizadores: sessionId=ana-session, roomId=room-equipa, user=Ana, focusedRoom=true
INFO  Processando mensagem entre utilizadores: roomId=room-equipa, roomName=Equipa, user=Ana, focusedRoom=true, messageLength=41
INFO  Broadcast WebSocket de mensagem: type=NEW_MESSAGE, roomId=room-equipa, roomName=Equipa, user=Ana, messageId=<uuid>, highlighted=true, sessoesAbertas=2
INFO  Mensagem persistida e difundida: roomId=room-equipa, messageId=<uuid>, user=Ana, highlighted=true
```

### Porque é que `highlighted` pode surgir como `true`
No serviço de aplicação, uma mensagem fica marcada como destacada quando contém palavras-chave como `graphql` ou `websocket`. No exemplo acima, a mensagem contém a palavra `GraphQL`, pelo que a notificação resultante pode surgir com `highlighted=true`.

### Vantagens desta abordagem
- O envio de mensagens acontece sem polling.
- Todos os clientes recebem a atualização quase em tempo real.
- A lógica de receção e broadcast fica centralizada no back-end.
- O front-end separa bem operações reativas (WebSocket) de operações de consulta/gestão (GraphQL).

## 4.10 Endpoint GraphQL, contratos e forma de utilização

A API GraphQL da aplicação está disponível em `http://localhost:8080/wirc`. O front-end usa sempre pedidos `POST` com `Content-Type: application/json`, enviando um corpo com o formato:

```json
{
  "query": "query($activeUser: String!) { rooms(activeUser: $activeUser) { id name participants state unreadMessages canManageMembers } }",
  "variables": {
    "activeUser": "Ana"
  }
}
```

### Estrutura geral do contrato GraphQL
O schema define dois grandes grupos de operações:
- `Query`: operações de leitura.
- `Mutation`: operações que alteram o estado da aplicação.

### Queries disponíveis

#### `users`
Lista os utilizadores conhecidos pela aplicação.

#### `rooms(activeUser: String!)`
Devolve apenas as salas acessíveis ao utilizador autenticado. O front-end usa esta query para montar a barra lateral de canais.

#### `messagesByRoom(roomId: String!, activeUser: String!)`
Obtém o histórico de mensagens de uma sala. É chamada quando o utilizador seleciona uma sala.

#### `searchMessages(term: String!, activeUser: String!)`
Pesquisa mensagens nas salas às quais o utilizador pertence.

#### `roomStats(roomId: String!, activeUser: String!)`
Obtém estatísticas agregadas da sala, como total de mensagens, mensagens destacadas e utilizador mais ativo.

#### `topUsers(activeUser: String!)`
Calcula o top 3 de utilizadores com mais mensagens nas salas visíveis para o utilizador atual.

### Mutations disponíveis

#### `signIn(user: String!, password: String!)`
Autentica um utilizador e devolve os seus dados básicos.

#### `createUser(displayName: String!, password: String!)`
Cria um novo utilizador e devolve o utilizador criado.

#### `sendMessage(...)`
Existe no schema, mas nesta aplicação o envio principal de mensagens é feito por **WebSocket**, para garantir comportamento em tempo real. A mutation mantém o contrato do domínio e pode servir como extensão futura ou fallback.

#### `focusRoom(roomId: String!, activeUser: String!)`
Atualiza o estado da sala quando o utilizador a coloca em foco, limpando contadores de leitura quando aplicável.

#### `createRoom(name: String!, activeUser: String!, participants: [String!]!)`
Cria uma nova sala e define os participantes iniciais.

#### `addMemberToRoom(roomId: String!, member: String!, activeUser: String!)`
Adiciona um utilizador a uma sala existente, desde que o utilizador autenticado tenha permissão para isso.

### Tipos principais do contrato

#### `AppUser`
Representa o utilizador devolvido ao front-end:
- `username`
- `displayName`

#### `ChatRoom`
Representa a sala listada na interface:
- `id`
- `name`
- `participants`
- `state`
- `unreadMessages`
- `canManageMembers`

#### `ChatMessage`
Representa cada mensagem do histórico:
- `id`
- `roomId`
- `user`
- `message`
- `sentAt`
- `highlighted`

#### `RoomStats`
Representa os indicadores estatísticos de uma sala:
- `roomId`
- `roomName`
- `totalMessages`
- `highlightedMessages`
- `busiestUser`

#### `UserMessageCount`
Representa um utilizador e o seu volume de mensagens para o ranking.

### Como o front-end usa concretamente o GraphQL
- `loadUsers()` chama a query `users`.
- `signIn()` chama a mutation `signIn`.
- `createUser()` chama a mutation `createUser`.
- `loadRooms()` chama a query `rooms`.
- `loadMessages()` chama a query `messagesByRoom`.
- `searchMessages()` chama a query `searchMessages`.
- `loadRoomStats()` chama a query `roomStats`.
- `loadTopUsers()` chama a query `topUsers`.
- `focusRoom()` chama a mutation `focusRoom`.
- `createRoom()` chama a mutation `createRoom`.
- `addMemberToRoom()` chama a mutation `addMemberToRoom`.

### Exemplo de utilização com Ana
Depois de autenticar a Ana, o front-end pode chamar a query de salas assim:

```graphql
query($activeUser: String!) {
  rooms(activeUser: $activeUser) {
    id
    name
    participants
    state
    unreadMessages
    canManageMembers
  }
}
```

Com as variáveis:

```json
{
  "activeUser": "Ana"
}
```

Em seguida, quando a Ana seleciona `room-equipa`, a aplicação usa `messagesByRoom` e `roomStats` para preencher a zona central do chat e os indicadores estatísticos.

### Separação de responsabilidades entre GraphQL e WebSocket
A solução usa as duas tecnologias de forma complementar:
- **GraphQL**: leitura, autenticação, gestão de salas, pesquisa, estatísticas e ações de administração.
- **WebSocket**: envio e propagação imediata de mensagens/notificações.

Esta separação melhora a clareza da arquitetura e cumpre o objetivo do projeto de combinar consulta estruturada com comunicação em tempo real.


## 5. Design patterns utilizados

## 5.1 Facade
O projeto apresenta uma **fachada de aplicação** através da interface `ChatApplication`, que fornece um ponto único de entrada para as principais operações do chat.

### Como foi aplicado
Em vez de espalhar a lógica por múltiplos controladores ou handlers, o sistema concentra as funcionalidades centrais num serviço de aplicação único, consumido tanto pelo controlador GraphQL como pelo handler WebSocket.

### Benefícios
- Redução do acoplamento entre camadas de entrada e lógica de negócio.
- Reutilização da mesma regra funcional por diferentes canais de comunicação.
- Melhor legibilidade arquitetural.

### Exemplo prático
Tanto a mutation GraphQL `sendMessage` como o `ChatWebSocketHandler` delegam o processamento para `ChatApplication`.

## 5.2 Chain of Responsibility
A validação das mensagens foi implementada com **Chain of Responsibility**.

### Como foi aplicado
Existe uma classe abstrata `MessageValidationHandler` que define a ligação entre validadores. Sobre essa base, foram criados validadores especializados:
- `RequiredFieldValidationHandler`;
- `ParticipantValidationHandler`;
- `MessageLengthValidationHandler`.

Cada elemento valida uma regra e encaminha o pedido para o próximo handler apenas se a sua própria validação tiver sucesso.

### Benefícios
- Facilidade em adicionar novas regras sem alterar o fluxo existente.
- Separação de responsabilidades.
- Melhor testabilidade unitária.

### Exemplo prático
Antes de aceitar uma mensagem, o sistema verifica campos obrigatórios, pertença do utilizador à sala e comprimento máximo permitido.

## 5.3 State
A gestão do comportamento das salas foi implementada com **State**.

### Como foi aplicado
O contrato `RoomState` define o comportamento associado aos eventos:
- envio de mensagem;
- foco da sala.

As implementações concretas são:
- `FocusedRoomState`;
- `NotifiedRoomState`.

Assim, o comportamento da sala muda consoante o seu estado interno, sem necessidade de condicional complexa espalhada pelo código.

### Benefícios
- Encapsulamento do comportamento dependente do estado.
- Código mais limpo e extensível.
- Evolução simples para novos estados futuros.

### Exemplo prático
Quando a sala está focada, uma nova mensagem pode limpar contadores; quando está em estado de notificação, o número de mensagens não lidas é incrementado.

### Significado prático dos estados `FOCUSED` e `NOTIFIED`
- **`FOCUSED`**: representa uma sala atualmente em foco para o utilizador ativo. Neste estado, ao interagir na sala em foco, o contador de não lidas é limpo e a experiência mantém-se "sincronizada" com o que está visível no ecrã.
- **`NOTIFIED`**: representa uma sala com atividade não lida. Sempre que chegam mensagens fora de foco, o contador de não lidas aumenta para sinalizar que existe conteúdo por ler.

Na prática, estes estados não vivem apenas no front-end: fazem parte do domínio no back-end e também são persistidos na base de dados (campo `state` em `room_session_state`), o que permite recuperar corretamente o contexto de leitura após reinício da aplicação. O front-end limita-se a refletir esse estado recebido por GraphQL e a apresentar indicadores visuais (por exemplo, badge de mensagens não lidas).

## 5.4 Factory
A criação de sessões de sala foi implementada com **Factory** através de `ChatRoomFactory`.

### Como foi aplicado
A fábrica recebe snapshots persistidos e decide que instância de estado deve ser criada para cada sala, reconstruindo corretamente o objeto `RoomSession`.

### Benefícios
- Centralização da lógica de criação.
- Evita duplicação na reconstrução do domínio.
- Permite alterar a política de criação sem impactar consumidores.

### Exemplo prático
O método `createFromSnapshot` converte o nome do estado persistido (`FOCUSED` ou `NOTIFIED`) na respetiva implementação concreta.

## 5.5 Observer
Embora o enunciado peça dois ou mais padrões, o projeto também explora um comportamento do tipo **Observer**.

### Como foi aplicado no front-end
A `ChatStore` usa subjects e observables para propagar alterações de estado aos componentes inscritos.

### Como foi aplicado no back-end
O gateway e o handler WebSocket mantêm sessões ativas e difundem eventos para os clientes ligados.

### Benefícios
- Atualização reativa da interface.
- Baixo acoplamento entre produtores e consumidores de eventos.
- Boa adequação a cenários de tempo real.

---

## 6. Outros padrões e princípios observáveis

### 6.1 Repository
Os repositórios JPA abstraem o acesso à base de dados, separando persistência de lógica de negócio.

### 6.2 MVC / Controller-Service separation
O back-end está organizado de forma próxima de **Controller + Service + Repository**, o que ajuda a estruturar responsabilidades.

### 6.3 Dependency Injection
A aplicação tira partido da **injeção de dependências** do Spring e do Angular, promovendo baixo acoplamento e maior facilidade de teste.

### 6.4 Single Responsibility Principle
A solução demonstra preocupação com responsabilidade única:
- componentes visuais tratam da apresentação;
- o serviço Angular comunica com a API;
- a store gere estado;
- o controller expõe operações;
- o serviço de aplicação contém regras de negócio;
- os handlers validam regras específicas.

---

## 7. Fluxos principais da aplicação

## 7.1 Autenticação
1. O utilizador seleciona ou introduz credenciais no front-end.
2. O Angular invoca a mutation `signIn`.
3. O back-end valida o utilizador e devolve os dados autenticados.
4. A `ChatStore` atualiza o estado global da sessão.

## 7.2 Consulta de salas e mensagens
1. Após autenticação, o front-end pede salas via GraphQL.
2. Ao selecionar uma sala, são pedidas mensagens e estatísticas.
3. A interface atualiza-se reativamente com base no estado da store.

## 7.3 Envio de mensagens em tempo real
1. O utilizador escreve a mensagem no chat.
2. O front-end envia um comando WebSocket.
3. O `ChatWebSocketHandler` interpreta o payload.
4. O serviço de aplicação valida e persiste a mensagem.
5. O gateway difunde a notificação para os clientes ativos.
6. O front-end recebe o evento e faz refresh dos dados relevantes.

---

## 8. Estrutura funcional do sistema

### 8.1 Gestão de utilizadores
- criação de utilizador;
- autenticação;
- distinção entre utilizador selecionado e autenticado.

### 8.2 Gestão de salas
- listagem de salas visíveis para o utilizador;
- criação de novas salas;
- associação de participantes;
- adição de membros por quem tem permissão.

### 8.3 Gestão de mensagens
- envio de mensagens;
- pesquisa textual em mensagens;
- destaque de mensagens com palavras-chave relevantes;
- histórico por sala.

### 8.4 Estatísticas
- total de mensagens por sala;
- número de mensagens destacadas;
- utilizador mais ativo;
- top 3 de utilizadores.

### 8.5 Notificações
- aviso de novas mensagens em tempo real;
- controlo de mensagens não lidas;
- transição entre salas focadas e notificadas.

---

## 9. Justificação técnica das escolhas

### Angular + Spring Boot
A combinação de Angular no front-end e Spring Boot no back-end foi adequada porque oferece:
- forte separação entre cliente e servidor;
- frameworks maduras e amplamente documentadas;
- boa capacidade de evolução;
- integração simples com GraphQL e WebSockets.

### GraphQL + WebSockets
Esta combinação foi especialmente relevante para o enunciado.

- **GraphQL** resolve consultas estruturadas e operações de gestão.
- **WebSockets** respondem à necessidade de atualização imediata e comunicação bidirecional.

Assim, a aplicação usa cada tecnologia no contexto mais adequado.

### PostgreSQL + JPA + Liquibase
Estas tecnologias permitiram manter um modelo de dados relacional, persistente e versionado, com controlo explícito da evolução da base de dados.

---

## 10. Pontos fortes da solução

- Cumprimento do requisito de uso conjunto de **GraphQL** e **WebSockets**.
- Aplicação explícita e justificável de vários **design patterns**.
- Separação clara de responsabilidades entre interface, estado, serviço, controlo e persistência.
- Modelo de chat multi-sala com permissões e notificações.
- Estrutura preparada para evolução funcional.
- Código relativamente modular e fácil de testar em partes isoladas.

---

## 11. Limitações e melhorias futuras

Apesar de a solução cumprir bem os objetivos, existem evoluções possíveis:

1. **Segurança reforçada**
   - hashing de passwords;
   - autenticação baseada em token/JWT;
   - autorização mais granular.

2. **Melhorias de escalabilidade**
   - broker de mensagens para WebSockets;
   - cache para consultas frequentes;
   - paginação de mensagens.

3. **Experiência do utilizador**
   - indicadores de utilizador online;
   - typing indicators;
   - upload de ficheiros;
   - filtros de pesquisa mais avançados.

4. **Observabilidade e qualidade**
   - mais testes end-to-end;
   - métricas técnicas;
   - dashboards de monitorização.

---

## 12. Conclusão

O projeto **WIRC** demonstra uma implementação coerente de uma aplicação web moderna orientada a tempo real. A solução articula com sucesso **Angular**, **Spring Boot**, **GraphQL**, **WebSockets** e **PostgreSQL**, suportando um conjunto relevante de funcionalidades de chat multi-salas.

Do ponto de vista académico, o projeto também evidencia a aplicação prática de padrões como **Facade**, **Chain of Responsibility**, **State**, **Factory** e **Observer**, não apenas como conceitos teóricos, mas como escolhas de desenho com impacto real na organização e manutenção do código.

Em síntese, trata-se de uma solução tecnicamente consistente, alinhada com os objetivos do enunciado e adequada para demonstrar competências em arquiteturas web modernas, comunicação em tempo real e desenho orientado a padrões.

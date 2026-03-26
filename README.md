# WIRC - Chat multi-salas com Spring Boot + Angular 20 + GraphQL + WebSockets

Aplicação de chat com várias salas, notificações em tempo real e estatísticas de utilização.

## Funcionalidades

- Salas de chat múltiplas com participantes diferentes.
- Envio e notificações de mensagens em tempo real via WebSocket.
- Listagem de salas, histórico, pesquisa textual, estatísticas por sala e top 3 utilizadores via GraphQL.
- Notificações instantâneas de mensagens em salas não focadas via WebSocket.
- Uso explícito dos padrões **Facade**, **Chain of Responsibility**, **State** e **Factory**, identificados com comentários no código.

## Arquitetura

- **Backend**: Spring Boot 4, GraphQL e WebSocket.
- **Frontend**: Angular 20 standalone.
- **Comunicação**:
  - GraphQL em `http://localhost:8080/wirc`
  - WebSocket em `ws://localhost:8080/wirc/chat`

## Design Patterns usados

- **Facade**: `ChatApplicationFacade` concentra a lógica da aplicação.
- **Chain of Responsibility**: cadeia de validação antes de enviar mensagens.
- **State**: salas alternam entre `FOCUSED` e `NOTIFIED` consoante foco/notificações.
- **Factory**: `ChatRoomFactory` cria salas iniciais de forma uniforme.

## Rodar tudo junto (Linux/macOS)

```bash
./start-all.sh
```

## Rodar tudo junto (Windows)

```bat
start-all.bat
```

## Rodar separado

### Backend

Requer Java 25.

```bash
cd backend
./mvnw spring-boot:run
```

### Frontend

```bash
cd frontend
npm install
npm start
```

## Exemplos GraphQL

### Listar salas

```graphql
query {
  rooms {
    id
    name
    state
    unreadMessages
  }
}
```

### Enviar mensagem por WebSocket

Envie o seguinte JSON para `ws://localhost:8080/wirc/chat`:

```json
{
  "type": "SEND_MESSAGE",
  "roomId": "room-equipa",
  "user": "Ana",
  "message": "Vamos validar GraphQL e WebSocket hoje",
  "focusedRoom": true
}
```


## Relatório técnico

- Documento completo em `docs/RELATORIO.md`.

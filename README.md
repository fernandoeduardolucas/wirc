# WIRC - Chat simples com Spring Boot + frontend separado

Este exemplo sobe backend e frontend ao mesmo tempo:

- Backend Spring Boot + WebSocket em `ws://localhost:8080/ws/chat`
- Frontend simples em `http://localhost:4200`

## Rodar tudo junto

```bash
./start-all.sh
```

## Rodar separado

Backend:

```bash
cd backend
./mvnw spring-boot:run
```

Frontend:

```bash
cd frontend
python3 -m http.server 4200
```

# WIRC - Chat simples com Spring Boot + AngularJS + GraphQL

Este exemplo sobe backend e frontend ao mesmo tempo:

- Backend Spring Boot com endpoint GraphQL em `http://localhost:8080/graphql`
- Frontend AngularJS em `http://localhost:4200`

## Rodar tudo junto (Linux/macOS)

```bash
./start-all.sh
```

## Rodar tudo junto (Windows)

```bat
start-all.bat
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

## Exemplo de query GraphQL

```graphql
query {
  messages {
    user
    message
  }
}
```

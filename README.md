# runpack-api

Backend Spring Boot do RunPack.

## Requisitos

- Java 17+
- Maven 3.9+
- Docker (para PostgreSQL e Redis locais)

## Setup local

```bash
# Subir banco e redis
docker compose up -d

# Rodar o backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

## Estrutura de pacotes

```
com.runpack.api/
  controller/     HTTP endpoints (thin)
  service/        Regras de negócio + @Transactional
  repository/     Spring Data JPA
  entity/         JPA entities
  dto/
    request/      Payloads de entrada
    response/     Payloads de saída (nunca expor entity)
  websocket/      Handlers STOMP
  security/       JWT filter + Spring Security config
  exception/      GlobalExceptionHandler + exceções de domínio
  config/         WebSocket, CORS, Firebase, etc.
```

## Banco de dados

Migrations em `src/main/resources/db/migration/` — Flyway controla o schema.  
Nunca usar DDL manual ou `ddl-auto=update` fora de dev.

## Referência

Ver `../runpack-docs/CLAUDE.md` para contratos de API, modelo de dados e regras de negócio.

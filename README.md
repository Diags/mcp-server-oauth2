# Architecture Microservices MCP + OAuth2

Architecture microservices complète pour MCP (Model Context Protocol) avec sécurité OAuth2, basée sur Spring Boot 3.4.0 et Spring AI 1.0.1.

## Architecture

```
┌─────────────────┐
│  Keycloak       │  Port 9000
│  (Auth Server)  │
└────────┬────────┘
         │
         │ OAuth2 JWT
         │
    ┌────┴────┐
    │         │
┌───▼───┐ ┌──▼────┐
│ MCP   │ │ MCP   │
│Server │ │Client │  Ports 8080, 8081
└───────┘ └───────┘
```

### Composants

1. **Keycloak** : Serveur d'autorisation OAuth2 (port 9000)
2. **mcp-server** : Serveur MCP protégé avec OAuth2 Resource Server (port 8080)
3. **mcp-client** : Client MCP avec OAuth2 Client et intégration LLM (port 8081)

## Prérequis

- Java 21+
- Maven 3.9+
- Docker et Docker Compose
- Clé API OpenAI (pour le client MCP)

## Configuration Keycloak

### 1. Démarrer Keycloak

```bash
docker run -p 9000:8080 \
  -e KEYCLOAK_ADMIN=admin \
  -e KEYCLOAK_ADMIN_PASSWORD=admin \
  quay.io/keycloak/keycloak:26.0 start-dev
```

### 2. Configuration du Realm

1. Accéder à http://localhost:9000
2. Se connecter avec `admin/admin`
3. Créer un nouveau realm : `mcp-realm`

### 3. Configuration du Client

1. Dans le realm `mcp-realm`, aller dans **Clients**
2. Créer un nouveau client :
   - **Client ID** : `mcp-client`
   - **Client authentication** : `ON`
   - **Valid redirect URIs** : `http://localhost:8081/*`
   - **Web origins** : `http://localhost:8081`
3. Dans l'onglet **Credentials**, copier le **Client secret** : `secret`
4. Dans l'onglet **Client scopes**, ajouter :
   - `mcp:read`
   - `mcp:write`

### 4. Configuration des Scopes

1. Aller dans **Client scopes**
2. Créer un scope `mcp:read`
3. Créer un scope `mcp:write`
4. Les ajouter au client `mcp-client`

## Démarrage Local

### Option 1 : Docker Compose (Recommandé)

```bash
# Définir la clé API OpenAI
export OPENAI_API_KEY=your-api-key-here

# Démarrer tous les services
docker-compose up -d

# Vérifier les logs
docker-compose logs -f
```

### Option 2 : Démarrage Manuel

#### 1. Démarrer Keycloak

```bash
docker run -p 9000:8080 \
  -e KEYCLOAK_ADMIN=admin \
  -e KEYCLOAK_ADMIN_PASSWORD=admin \
  quay.io/keycloak/keycloak:26.0 start-dev
```

#### 2. Démarrer le Serveur MCP

```bash
cd mcp-server
mvn spring-boot:run
```

#### 3. Démarrer le Client MCP

```bash
cd mcp-client
export OPENAI_API_KEY=your-api-key-here
mvn spring-boot:run
```

## Configuration

### Variables d'environnement

#### mcp-server
- `SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI` : URI du serveur d'autorisation (défaut: `http://localhost:9000/realms/mcp-realm`)

#### mcp-client
- `OPENAI_API_KEY` : Clé API OpenAI (requis)
- `SPRING_SECURITY_OAUTH2_CLIENT_PROVIDER_AUTHSERVER_ISSUER_URI` : URI du serveur d'autorisation
- `SPRING_AI_MCP_CLIENT_STREAMABLE_HTTP_CONNECTIONS_MATH_SERVER_URL` : URL du serveur MCP

## Utilisation

### Tester le Serveur MCP

```bash
# Obtenir un token JWT depuis Keycloak
curl -X POST http://localhost:9000/realms/mcp-realm/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=mcp-client" \
  -d "client_secret=secret" \
  -d "grant_type=client_credentials" \
  -d "scope=mcp:read mcp:write"

# Utiliser le token pour appeler le serveur MCP
curl -X POST http://localhost:8080/mcp \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","method":"tools/list","id":1}'
```

### Tester le Client MCP

```bash
# Poser une question via l'API
curl "http://localhost:8081/api/ask?question=What%20is%202%20plus%202?"
```

## Architecture des Microservices

### mcp-server

**Endpoints :**
- `POST /mcp` : Endpoint principal MCP (protégé)

**Outils disponibles :**
- `add` : Addition de deux nombres (scope: `mcp:read`)
- `subtract` : Soustraction de deux nombres (scope: `mcp:read`)
- `multiply` : Multiplication de deux nombres (scope: `mcp:write`)
- `divide` : Division de deux nombres (scope: `mcp:write`)
- `power` : Puissance d'un nombre (scope: `mcp:write`)

**Sécurité :**
- OAuth2 Resource Server avec validation JWT
- Validation de l'audience
- Autorisation fine par outil avec `@PreAuthorize`

### mcp-client

**Endpoints :**
- `GET /api/ask?question=...` : Poser une question au LLM avec outils MCP

**Fonctionnalités :**
- Connexion automatique au serveur MCP avec authentification OAuth2
- Flux hybrid : `authorization_code` pour les requêtes utilisateur, `client_credentials` pour les appels système
- Intégration LLM (OpenAI) avec outils MCP automatiques

## Sécurité

### Flux OAuth2 Hybrid

Le client MCP utilise un flux hybrid :
- **authorization_code** : Pour les requêtes authentifiées par l'utilisateur
- **client_credentials** : Pour les appels système au démarrage

### Validation des Scopes

Les outils MCP sont protégés par des scopes :
- `mcp:read` : Opérations de lecture (add, subtract)
- `mcp:write` : Opérations d'écriture (multiply, divide, power)

### Validation de l'Audience

Le serveur MCP valide l'audience du JWT pour s'assurer que le token est destiné au bon service.

## Tests

### Exécuter les tests

```bash
# Tous les tests
mvn test

# Tests du serveur uniquement
cd mcp-server && mvn test

# Tests du client uniquement
cd mcp-client && mvn test
```

## Développement

### Structure du projet

```
mcp-microservices/
├── pom.xml                 # POM parent
├── docker-compose.yml      # Orchestration Docker
├── mcp-server/            # Microservice serveur MCP
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/
│   │   │   └── resources/
│   │   └── test/
│   └── Dockerfile
└── mcp-client/            # Microservice client MCP
    ├── src/
    │   ├── main/
    │   │   ├── java/
    │   │   └── resources/
    │   └── test/
    └── Dockerfile
```

### Ajouter un nouvel outil MCP

Dans `mcp-server/src/main/java/com/mcp/server/service/MathTools.java` :

```java
@McpTool(description = "Your tool description")
@PreAuthorize("hasAuthority('SCOPE_mcp:write')")  // Optionnel
public ReturnType yourMethod(@McpToolParam ParamType param) {
    // Implémentation
}
```

## Dépannage

### Problèmes de connexion OAuth2

1. Vérifier que Keycloak est démarré : `curl http://localhost:9000/health`
2. Vérifier la configuration du client dans Keycloak
3. Vérifier les logs : `docker-compose logs mcp-server` ou `docker-compose logs mcp-client`

### Problèmes de token

1. Vérifier que le token contient les scopes nécessaires
2. Vérifier l'expiration du token
3. Vérifier la validation de l'audience

### Problèmes de connexion MCP

1. Vérifier que le serveur MCP est accessible : `curl http://localhost:8080/actuator/health`
2. Vérifier la configuration de l'URL dans le client
3. Vérifier les logs d'authentification

## Technologies

- **Spring Boot** 3.4.0
- **Java** 21
- **Spring AI** 1.0.1
- **Spring Security** OAuth2
- **Keycloak** 26.0
- **OpenAI API** (via Spring AI)
- **Maven** 3.9+
- **Docker** & Docker Compose

## Licence

Ce projet est fourni à titre d'exemple pour l'architecture MCP avec OAuth2.


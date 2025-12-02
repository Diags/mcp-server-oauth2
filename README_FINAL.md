# Architecture Microservices MCP + OAuth2 + Gestion de Documents

Architecture microservices complète pour MCP (Model Context Protocol) avec sécurité OAuth2, gestion de documents vectoriels, et stockage distribué.

## 🏗️ Architecture

L'architecture respecte le diagramme fourni avec tous les composants :

```
┌─────────────────┐
│  Docker Compose │
└────────┬────────┘
         │
    ┌────┴────────────────────────┐
    │                             │
┌───▼────────┐          ┌────────▼─────┐
│  Keycloak  │  OAuth2  │  Client MCP  │
│  Port 9000 │◄─────────┤  Port 8081   │
└─────┬──────┘   JWT    └──────┬───────┘
      │                         │
      │                    ┌────▼────────┐
      └───────────────────►│ Server MCP  │
           JWT Token       │  Port 8080  │
                          └──┬──┬───┬───┘
                             │  │   │
              ┌──────────────┘  │   └──────────────┐
              │                 │                   │
      ┌───────▼────────┐ ┌─────▼──────┐  ┌────────▼────────┐
      │ PostgreSQL     │ │ PostgreSQL │  │ MinIO / S3      │
      │ + pgvector     │ │ Métadonnées│  │ Fichiers        │
      │ Port 5433      │ │ Port 5434  │  │ Ports 9001/9002 │
      └────────────────┘ └────────────┘  └─────────────────┘
```

## 🚀 Démarrage rapide

### 1. Prérequis
- Docker Desktop démarré
- Java 21+
- Maven 3.9+
- OpenAI API Key (optionnel pour tests de base)

### 2. Génération des certificats SSL
```bash
./generate-certs.sh
```

### 3. Démarrage des services
```bash
# Définir la clé API OpenAI (optionnel)
export OPENAI_API_KEY=your-api-key-here

# Démarrer tous les services
docker compose up -d

# Vérifier l'état
docker compose ps
```

### 4. Initialisation de Keycloak
```bash
# Attendre que Keycloak soit prêt (environ 2 minutes)
./init-keycloak.sh
```

### 5. Test de 5 X 5
```bash
./test-5x5-simple.sh
```

**Résultat attendu :**
```
✅ TEST RÉUSSI! 5 X 5 = 25
```

## 📦 Composants

### Services Docker
- **PostgreSQL + pgvector** : Base de données vectorielle pour embeddings (port 5433)
- **PostgreSQL** : Base de données métadonnées (port 5434)
- **Keycloak** : Serveur d'authentification OAuth2 avec HTTPS (port 9000)
- **MinIO** : Stockage d'objets S3-compatible (ports 9001, 9002)
- **MCP Server** : Serveur MCP avec outils (port 8080)
- **MCP Client** : Client MCP avec intégration LLM (port 8081)

### Outils MCP disponibles

**MathTools :**
- `add(a, b)` - Addition (scope: mcp:read)
- `subtract(a, b)` - Soustraction (scope: mcp:read)
- `multiply(a, b)` - Multiplication (scope: mcp:write) ✅ Testé
- `divide(a, b)` - Division (scope: mcp:write)
- `power(base, exponent)` - Puissance (scope: mcp:write)

**DocumentTools :**
- `uploadDocument(base64Content, filename, tags)` - Upload et vectorisation de documents (scope: mcp:write)
- `searchDocuments(query, limit)` - Recherche sémantique (scope: mcp:read)

## 🧪 Tests

### Tests end-to-end
```bash
# Tests du serveur
cd mcp-server
mvn test

# Tests du client
cd mcp-client
mvn test
```

### Tests d'intégration
- `ChatApiEndToEndTest` : Tests depuis l'endpoint `/chat/ask`
- `DocumentToolsIntegrationTest` : Tests des outils de documents
- Testcontainers : Tous les services démarrés automatiquement

## 📝 Scripts disponibles

- `generate-certs.sh` : Génère les certificats SSL pour Keycloak
- `init-keycloak.sh` : Initialise le realm Keycloak avec client et scopes
- `test-5x5-simple.sh` : Teste 5 X 5 via REST API ✅
- `test-math-tool.sh` : Teste via le client MCP
- `setup-keycloak.sh` : Instructions de configuration manuelle

## 🔧 Configuration

### Variables d'environnement

**Pour docker-compose :**
- `OPENAI_API_KEY` : Clé API OpenAI (requis pour le client)

**Pour développement local :**
- `JAVA_HOME` : Java 21
- `SPRING_DATASOURCE_URL` : URL base métadonnées
- `SPRING_VECTOR_DATASOURCE_URL` : URL base vectorielle
- `SPRING_MINIO_URL` : URL MinIO

## 🔐 Sécurité

- OAuth2 avec Keycloak
- JWT tokens
- Scopes : `mcp:read`, `mcp:write`
- HTTPS avec certificats auto-signés (développement)
- Configuration SSL pour accepter les certificats auto-signés

## 📚 Technologies

- Spring Boot 3.4.0
- Spring AI 1.1.0
- Spring Security OAuth2
- Keycloak 26.0.5
- PostgreSQL 16 + pgvector
- MinIO
- PDFBox 3.0.3
- Testcontainers 1.20.4
- Java 21
- Maven 3.9+

## 🛠️ Commandes utiles

```bash
# Arrêter tous les services
docker compose down

# Arrêter et supprimer les volumes
docker compose down -v

# Voir les logs
docker compose logs -f

# Voir les logs d'un service spécifique
docker compose logs -f keycloak
docker compose logs -f mcp-server

# Rebuild sans cache
docker compose build --no-cache

# Recompiler le projet
mvn clean package -DskipTests
```

## 📊 Endpoints

### Serveur MCP (port 8080)
- `POST /mcp` : Endpoint MCP principal (protégé OAuth2)
- `GET /api/test/multiply?a=5&b=5` : Test direct multiplication
- `GET /api/test/add?a=10&b=5` : Test direct addition
- `GET /actuator/health` : Health check

### Client MCP (port 8081)
- `GET /chat/ask?question=...` : Poser une question au LLM avec outils MCP
- `GET /actuator/health` : Health check

### Keycloak (port 9000)
- `https://localhost:9000` : Interface admin (admin/admin)
- `https://localhost:9000/realms/mcp-realm` : Realm MCP

## ✅ Tests effectués

- ✅ Compilation du projet (BUILD SUCCESS)
- ✅ Démarrage de Keycloak avec HTTPS
- ✅ Configuration du realm mcp-realm
- ✅ Obtention de token OAuth2
- ✅ Appel de MathTools.multiply(5, 5) = 25
- ✅ Authentification et autorisation OAuth2

## 📁 Structure du projet

```
ServerMCP/
├── docker-compose.yml          # Orchestration complète
├── pom.xml                     # POM parent
├── generate-certs.sh           # Génération certificats SSL
├── init-keycloak.sh            # Initialisation Keycloak
├── test-5x5-simple.sh          # Test 5 X 5 ✅
├── test-math-tool.sh           # Test via client
├── setup-keycloak.sh           # Instructions manuelles
├── START_AND_TEST.md           # Guide de démarrage
├── certs/                      # Certificats SSL (gitignored)
├── keycloak/
│   └── realm-config.json       # Configuration realm
├── mcp-server/                 # Serveur MCP
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/
│   │   │   │   └── com/mcp/server/
│   │   │   │       ├── config/
│   │   │   │       │   ├── McpServerSecurity.java
│   │   │   │       │   ├── VectorDatabaseConfig.java
│   │   │   │       │   └── SslConfig.java
│   │   │   │       ├── controller/
│   │   │   │       │   └── TestController.java
│   │   │   │       ├── entity/
│   │   │   │       │   ├── DocumentMetadata.java
│   │   │   │       │   └── DocumentChunk.java
│   │   │   │       ├── repository/
│   │   │   │       │   ├── DocumentMetadataRepository.java
│   │   │   │       │   └── VectorRepository.java
│   │   │   │       ├── service/
│   │   │   │       │   ├── MathTools.java
│   │   │   │       │   └── DocumentTools.java
│   │   │   │       ├── dto/
│   │   │   │       │   ├── DocumentUploadResponse.java
│   │   │   │       │   └── DocumentSearchResult.java
│   │   │   │       └── McpServerApplication.java
│   │   │   └── resources/
│   │   │       └── application.yml
│   │   └── test/
│   │       └── java/
│   │           └── com/mcp/server/
│   │               ├── integration/
│   │               │   └── DocumentToolsIntegrationTest.java
│   │               └── service/
│   │                   └── MathToolsTest.java
│   ├── Dockerfile
│   └── pom.xml
└── mcp-client/                 # Client MCP
    ├── src/
    │   ├── main/
    │   │   ├── java/
    │   │   │   └── com/mcp/client/
    │   │   │       ├── config/
    │   │   │       │   ├── McpOAuth2HybridExchangeFilterFunction.java
    │   │   │       │   └── ChatClientConfig.java
    │   │   │       ├── controller/
    │   │   │       │   └── ChatApi.java
    │   │   │       └── McpClientApplication.java
    │   │   └── resources/
    │   │       └── application.yml
    │   └── test/
    │       ├── java/
    │       │   └── com/mcp/client/
    │       │       ├── config/
    │       │       │   └── TestContainersConfig.java
    │       │       ├── controller/
    │       │       │   └── ChatApiEndToEndTest.java
    │       │       └── util/
    │       │           └── TestUtils.java
    │       └── resources/
    │           ├── application-test.yml
    │           └── keycloak-test-realm.json
    ├── Dockerfile
    └── pom.xml
```

## 🎯 Prochaines étapes

1. Tester l'upload de documents : `DocumentTools.uploadDocument()`
2. Tester la recherche sémantique : `DocumentTools.searchDocuments()`
3. Tester via le client MCP avec OpenAI
4. Déployer en production avec Let's Encrypt

## 📖 Documentation

- `START_AND_TEST.md` : Guide de démarrage détaillé
- `README.md` : Documentation originale
- Code commenté et auto-documenté

## ✨ Fonctionnalités implémentées

- ✅ Architecture microservices complète
- ✅ OAuth2 avec Keycloak
- ✅ Base de données vectorielle (pgvector)
- ✅ Stockage de fichiers (MinIO)
- ✅ Outils MCP (Math + Documents)
- ✅ Tests end-to-end
- ✅ HTTPS avec certificats SSL
- ✅ Docker Compose complet
- ✅ Scripts de test automatisés

---

**Projet prêt pour le développement et les tests!** 🚀


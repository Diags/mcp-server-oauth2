# Guide d'utilisation du Gateway Server

## 🏗️ Architecture avec Gateway

```
Utilisateur/Client
    ↓
Gateway Server (port 8082)
    ├─► OAuth2 Authentication (Keycloak)
    ├─► Rate Limiting (Redis)
    ├─► Circuit Breaker (Resilience4j)
    ├─► Correlation ID tracking
    │
    ├─► Route: /mcp/chat/** → Client MCP (8081)
    ├─► Route: /mcp/test/** → Serveur MCP (8080)
    └─► Route: /mcp/server/** → Serveur MCP (8080)
```

## 🚀 Services démarrés

| Service | Port | État |
|---------|------|------|
| Gateway Server | 8082 | ✅ UP |
| Client MCP | 8081 | ✅ UP |
| Serveur MCP | 8080 | ✅ UP |
| Keycloak HTTPS | 9000 | ✅ UP |
| Redis | 6379 | ✅ UP |
| PostgreSQL Vector | 5433 | ✅ UP |
| PostgreSQL Meta | 5434 | ✅ UP |
| MinIO | 9001, 9002 | ✅ UP |

## 📋 Endpoints disponibles

### Via Gateway (port 8082)

#### 1. Chat avec LLM et outils MCP
```bash
# Sans authentification (en développement)
curl -G "http://localhost:8082/mcp/chat/ask" \
  --data-urlencode "question=What is 5 multiplied by 5?"
```

#### 2. Tests directs des outils Math
```bash
# Avec token OAuth2
TOKEN=$(curl -k -s -X POST https://localhost:9000/realms/mcp-realm/protocol/openid-connect/token \
  -d "client_id=mcp-client" \
  -d "client_secret=secret" \
  -d "grant_type=client_credentials" \
  -d "scope=mcp:read mcp:write" | jq -r .access_token)

# Multiplication
curl "http://localhost:8082/mcp/test/multiply?a=5&b=5" \
  -H "Authorization: Bearer $TOKEN"

# Addition
curl "http://localhost:8082/mcp/test/add?a=10&b=5" \
  -H "Authorization: Bearer $TOKEN"
```

### Direct (sans Gateway)

#### Serveur MCP direct
```bash
# Fonctionne parfaitement ✅
./test-5x5-simple.sh
```

## 🔄 Processus d'authentification

### Option 1 : Client Credentials (Machine-to-Machine)
```
Client → Gateway → Token OAuth2 → Serveur MCP
```

**Utilisé pour :** Appels API backend-to-backend

### Option 2 : Authorization Code (Utilisateur) - À configurer
```
1. Utilisateur → http://localhost:8082/mcp/chat/ask
2. Gateway → Pas de session → Redirection Keycloak
3. Utilisateur → Formulaire login Keycloak
4. Keycloak → Token JWT → Redirection Gateway
5. Gateway → Route vers services
```

**Pour activer :** Créer un utilisateur dans Keycloak et configurer authorization_code

## 🛠️ Fonctionnalités du Gateway

### 1. Rate Limiting (Redis)
- 1 requête par seconde par utilisateur
- Burst capacity: 1
- Key: header "user" ou "anonymous"

### 2. Circuit Breaker (Resilience4j)
- Sliding window: 10 requêtes
- Failure threshold: 50%
- Wait duration: 10 secondes
- Fallback endpoints: `/fallback/client`, `/fallback/server`

### 3. Retry Policy
- 3 tentatives pour les requêtes GET
- Backoff exponentiel: 100ms → 1000ms

### 4. Correlation ID
- Header automatique: `mcp-correlation-id`
- Ajouté à chaque requête et réponse
- Permet le traçage des requêtes

## 📊 État actuel

### ✅ Fonctionnel
- Gateway démarré et accessible
- Routes configurées
- Redis connecté
- Serveur MCP répond directement (test-5x5-simple.sh)

### ⚠️ À configurer
- Certificat SSL Keycloak pour le Gateway (actuellement problème PKIX)
- Utilisateurs Keycloak pour authorization_code flow
- Configuration OpenAI API key valide pour le client

## 🔧 Solutions aux problèmes courants

### Problème: Gateway retourne 500
**Cause :** Certificat SSL Keycloak non reconnu par le Gateway

**Solution :**
1. Ajouter le certificat au truststore du Gateway
2. Ou utiliser HTTP en développement (modifier issuer-uri)
3. Ou importer le certificat dans le keystore Java

### Problème: Rate limiting trop strict
**Solution :** Modifier dans `application.yml` :
```yaml
redis-rate-limiter:
  replenishRate: 100  # Plus de requêtes
  burstCapacity: 200
```

### Problème: Circuit breaker s'ouvre trop vite
**Solution :** Ajuster dans `application.yml` :
```yaml
resilience4j:
  circuitbreaker:
    configs:
      default:
        failureRateThreshold: 80  # Plus tolérant
```

## 🎯 Tests recommandés

```bash
# 1. Test Gateway health
curl http://localhost:8082/actuator/health

# 2. Test direct serveur (fonctionne)
./test-5x5-simple.sh

# 3. Test Gateway avec token
./test-gateway.sh

# 4. Test Chat (nécessite OpenAI API key)
export OPENAI_API_KEY="your-real-key"
curl -G "http://localhost:8082/mcp/chat/ask" \
  --data-urlencode "question=Calculate 7 times 8"
```

## 📦 Prochaines étapes

1. Résoudre le problème SSL Gateway ↔ Keycloak
2. Créer des utilisateurs dans Keycloak
3. Tester le flux authorization_code avec formulaire de login
4. Configurer une vraie clé OpenAI pour tester le chat complet

---

**Le Gateway est opérationnel et prêt à router les requêtes!** 🚀


# 📮 Configuration Postman pour le Gateway MCP

## 🔐 Configuration OAuth2

### Informations de connexion Keycloak

| Paramètre | Valeur |
|-----------|--------|
| **Client ID** | `mcp-client` |
| **Client Secret** | `secret` |
| **Grant Type** | `Password Credentials` ou `Client Credentials` |
| **Access Token URL** | `https://localhost:9000/realms/mcp-realm/protocol/openid-connect/token` |
| **Scope** | `mcp:read mcp:write` |

### Utilisateurs disponibles

#### Utilisateur standard (ROLE_MCP_USER)
- **Username:** `testuser`
- **Password:** `password123`
- **Rôle:** MCP_USER
- **Accès:** `/mcp/chat/**`

#### Utilisateur admin (ROLE_MCP_ADMIN)
- **Username:** `adminuser`
- **Password:** `admin123`
- **Rôle:** MCP_ADMIN
- **Accès:** `/mcp/test/**`, `/mcp/server/**`

## 📝 Configuration Postman - Étape par étape

### 1. Créer une nouvelle Collection

1. Ouvrir Postman
2. Créer une nouvelle Collection : "MCP Gateway API"
3. Aller dans l'onglet **Authorization**

### 2. Configurer OAuth 2.0 (Password Grant)

**Type:** OAuth 2.0

**Configuration:**
```
Grant Type: Password Credentials
Access Token URL: https://localhost:9000/realms/mcp-realm/protocol/openid-connect/token
Client ID: mcp-client
Client Secret: secret
Username: testuser
Password: password123
Scope: mcp:read mcp:write
Client Authentication: Send client credentials in body
```

**⚠️ Important:** Désactiver la vérification SSL dans Postman
- Settings → General → SSL certificate verification → OFF

### 3. Obtenir le token

1. Cliquer sur "Get New Access Token"
2. Postman va obtenir le token automatiquement
3. Cliquer sur "Use Token"

## 🎯 Endpoints à tester

### Collection Postman - Requêtes

#### 1. Health Check Gateway
```
Method: GET
URL: http://localhost:8082/actuator/health
Authorization: No Auth
```

#### 2. Gateway Routes
```
Method: GET
URL: http://localhost:8082/actuator/gateway/routes
Authorization: No Auth
```

#### 3. Chat Ask (nécessite ROLE_MCP_USER)
```
Method: GET
URL: http://localhost:8082/mcp/chat/ask
Params:
  - question: What is 5 multiplied by 5?
Authorization: OAuth 2.0 (hérite de la collection)
```

#### 4. Test Multiply (nécessite ROLE_MCP_ADMIN)
```
Method: GET
URL: http://localhost:8082/mcp/test/multiply
Params:
  - a: 5
  - b: 5
Authorization: OAuth 2.0 (utiliser adminuser)
```

#### 5. Test Add (nécessite ROLE_MCP_ADMIN)
```
Method: GET
URL: http://localhost:8082/mcp/test/add
Params:
  - a: 10
  - b: 5
Authorization: OAuth 2.0 (utiliser adminuser)
```

#### 6. Serveur Direct - Multiply
```
Method: GET
URL: http://localhost:8080/api/test/multiply
Params:
  - a: 5
  - b: 5
Authorization: OAuth 2.0
```

## 📋 Configuration JSON pour import Postman

```json
{
  "info": {
    "name": "MCP Gateway API",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
  },
  "auth": {
    "type": "oauth2",
    "oauth2": [
      {
        "key": "accessTokenUrl",
        "value": "https://localhost:9000/realms/mcp-realm/protocol/openid-connect/token",
        "type": "string"
      },
      {
        "key": "clientId",
        "value": "mcp-client",
        "type": "string"
      },
      {
        "key": "clientSecret",
        "value": "secret",
        "type": "string"
      },
      {
        "key": "grant_type",
        "value": "password_credentials",
        "type": "string"
      },
      {
        "key": "username",
        "value": "testuser",
        "type": "string"
      },
      {
        "key": "password",
        "value": "password123",
        "type": "string"
      },
      {
        "key": "scope",
        "value": "mcp:read mcp:write",
        "type": "string"
      },
      {
        "key": "addTokenTo",
        "value": "header",
        "type": "string"
      }
    ]
  },
  "item": [
    {
      "name": "Gateway Health",
      "request": {
        "method": "GET",
        "url": "http://localhost:8082/actuator/health"
      }
    },
    {
      "name": "Chat Ask (MCP_USER)",
      "request": {
        "method": "GET",
        "url": {
          "raw": "http://localhost:8082/mcp/chat/ask?question=What is 5 multiplied by 5?",
          "query": [
            {
              "key": "question",
              "value": "What is 5 multiplied by 5?"
            }
          ]
        }
      }
    },
    {
      "name": "Test Multiply (MCP_ADMIN)",
      "request": {
        "method": "GET",
        "url": {
          "raw": "http://localhost:8082/mcp/test/multiply?a=5&b=5",
          "query": [
            {
              "key": "a",
              "value": "5"
            },
            {
              "key": "b",
              "value": "5"
            }
          ]
        }
      }
    }
  ]
}
```

## 🔧 Configuration manuelle Headers

Si vous préférez ne pas utiliser OAuth2 automatique :

**Header à ajouter manuellement:**
```
Authorization: Bearer <votre-token>
```

**Obtenir le token via curl puis copier dans Postman:**
```bash
curl -k -s -X POST https://localhost:9000/realms/mcp-realm/protocol/openid-connect/token \
  -d "client_id=mcp-client" \
  -d "client_secret=secret" \
  -d "grant_type=password" \
  -d "username=testuser" \
  -d "password=password123" \
  -d "scope=mcp:read mcp:write" | jq -r .access_token
```

## 📊 Résumé des credentials

### Pour Keycloak OAuth2
- **Client ID:** `mcp-client`
- **Client Secret:** `secret`
- **Token URL:** `https://localhost:9000/realms/mcp-realm/protocol/openid-connect/token`
- **Scopes:** `mcp:read mcp:write`

### Utilisateurs
- **testuser** / password123 (ROLE_MCP_USER)
- **adminuser** / admin123 (ROLE_MCP_ADMIN)

### Endpoints Gateway
- **Base URL:** `http://localhost:8082`
- **Chat:** `/mcp/chat/ask`
- **Test:** `/mcp/test/multiply`, `/mcp/test/add`
- **Health:** `/actuator/health`

## ⚙️ Settings Postman requis

1. **Désactiver SSL verification:**
   - Settings → General → SSL certificate verification → OFF

2. **Timeout:**
   - Settings → General → Request timeout → 30000 ms

3. **Suivre les redirections:**
   - Settings → General → Automatically follow redirects → ON


# Comment appeler /chat/ask - Processus détaillé

## 🔍 Architecture actuelle

### Endpoint
```
GET http://localhost:8081/chat/ask?question=<votre-question>
```

### Problème actuel
L'endpoint `/chat/ask` est **protégé par Spring Security** et nécessite une authentification. Actuellement, il redirige vers `/login` car :
1. Le client utilise OAuth2 `client_credentials` (machine-to-machine)
2. Aucune session utilisateur n'est établie
3. Spring Security bloque l'accès non authentifié

## 📋 Processus actuel (Backend-to-Backend)

```
┌─────────────┐
│   Appel     │
│  /chat/ask  │
└──────┬──────┘
       │
       ▼
┌─────────────────┐
│ Spring Security │──► Pas de session ──► Redirection vers /login
│   (Client MCP)  │
└─────────────────┘
```

## 🔧 Solutions possibles

### Solution 1 : Désactiver la sécurité pour /chat/ask (DÉVELOPPEMENT UNIQUEMENT)

Créer une configuration de sécurité pour permettre l'accès sans authentification :

```java
// mcp-client/src/main/java/com/mcp/client/config/SecurityConfig.java
package com.mcp.client.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        http
            .authorizeExchange(exchanges -> exchanges
                .pathMatchers("/chat/**").permitAll()  // Permettre /chat/ask sans auth
                .pathMatchers("/actuator/**").permitAll()
                .anyExchange().authenticated()
            )
            .oauth2Client();  // Garder OAuth2 pour les appels MCP
        
        return http.build();
    }
}
```

**Puis tester :**
```bash
curl -G "http://localhost:8081/chat/ask" \
  --data-urlencode "question=What is 5 multiplied by 5?"
```

### Solution 2 : Utiliser l'authentification OAuth2 (PRODUCTION)

#### Étape 1 : Ajouter authorization_code

Modifier `application.yml` :
```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          # Client credentials (existant)
          authserver-client-credentials:
            client-id: mcp-client
            client-secret: secret
            authorization-grant-type: client_credentials
            provider: authserver
            scope: mcp:read,mcp:write
          
          # Authorization code (NOUVEAU - pour utilisateurs)
          authserver-authorization-code:
            client-id: mcp-client
            client-secret: secret
            authorization-grant-type: authorization_code
            provider: authserver
            scope: openid,profile,email,mcp:read,mcp:write
            redirect-uri: "{baseUrl}/login/oauth2/code/{registrationId}"
```

#### Étape 2 : Accéder via navigateur

1. Ouvrir : http://localhost:8081/chat/ask?question=Hello
2. Redirection automatique vers Keycloak
3. Se connecter (créer un utilisateur dans Keycloak)
4. Redirection vers l'application avec token
5. Réponse affichée

### Solution 3 : Utiliser un token Bearer directement (API)

#### Obtenir un token
```bash
TOKEN=$(curl -k -s -X POST https://localhost:9000/realms/mcp-realm/protocol/openid-connect/token \
  -d "client_id=mcp-client" \
  -d "client_secret=secret" \
  -d "grant_type=client_credentials" \
  -d "scope=mcp:read mcp:write" | jq -r .access_token)
```

#### Appeler avec le token
```bash
curl -G "http://localhost:8081/chat/ask" \
  --data-urlencode "question=What is 5 multiplied by 5?" \
  -H "Authorization: Bearer $TOKEN"
```

## 🎯 Processus complet avec Solution 1 (Recommandé pour test)

```
1. Utilisateur
   │
   ▼
2. GET /chat/ask?question=What is 5 multiplied by 5?
   │
   ▼
3. ChatApi.ask(question)
   │
   ├─► ChatClient (Spring AI)
   │   │
   │   ├─► Appel au LLM OpenAI
   │   │   └─► Analyse de la question
   │   │
   │   └─► Détection qu'un outil MCP est nécessaire (multiply)
   │
   ▼
4. Client MCP appelle le serveur MCP
   │
   ├─► Obtention token OAuth2 (client_credentials)
   │   └─► Keycloak: https://localhost:9000
   │
   ├─► POST http://localhost:8080/mcp
   │   └─► Headers: Authorization: Bearer <token>
   │
   └─► Appel de l'outil: multiply(5, 5)
   │
   ▼
5. Serveur MCP
   │
   ├─► Validation du JWT token
   │   └─► Vérification des scopes (mcp:write)
   │
   ├─► MathTools.multiply(5, 5)
   │   └─► Calcul: 5 * 5 = 25
   │
   └─► Retour du résultat
   │
   ▼
6. Client MCP reçoit: 25
   │
   ▼
7. ChatClient (LLM) formule la réponse
   │
   ▼
8. Réponse à l'utilisateur: "5 multiplied by 5 equals 25"
```

## 🚀 Implémentation rapide (Solution 1)

Voulez-vous que je :
- **A)** Implémente la Solution 1 (désactiver auth pour /chat/ask en dev)
- **B)** Implémente la Solution 2 (authorization_code avec formulaire de login)
- **C)** Crée un script de test avec token Bearer (Solution 3)

La **Solution A** est la plus rapide pour tester immédiatement.


# 🔑 Commandes curl pour tester avec tokens

## 1️⃣ Obtenir un token avec rôle MCP_USER

```bash
TOKEN=$(curl -k -s -X POST https://localhost:9000/realms/mcp-realm/protocol/openid-connect/token \
  -d "client_id=mcp-client" \
  -d "client_secret=secret" \
  -d "grant_type=password" \
  -d "username=testuser" \
  -d "password=password123" \
  -d "scope=mcp:read mcp:write" | jq -r .access_token)

echo "Token: $TOKEN"
```

## 2️⃣ Tester /mcp/chat/** (nécessite ROLE_MCP_USER)

```bash
curl -G "http://localhost:8082/mcp/chat/ask" \
  --data-urlencode "question=What is 5 multiplied by 5?" \
  -H "Authorization: Bearer $TOKEN"
```

## 3️⃣ Obtenir un token client_credentials (sans rôle utilisateur)

```bash
TOKEN_CLIENT=$(curl -k -s -X POST https://localhost:9000/realms/mcp-realm/protocol/openid-connect/token \
  -d "client_id=mcp-client" \
  -d "client_secret=secret" \
  -d "grant_type=client_credentials" \
  -d "scope=mcp:read mcp:write" | jq -r .access_token)

echo "Token client: $TOKEN_CLIENT"
```

## 4️⃣ Tester le serveur direct (fonctionne toujours)

```bash
curl "http://localhost:8080/api/test/multiply?a=5&b=5" \
  -H "Authorization: Bearer $TOKEN_CLIENT"
```

**Résultat attendu :** `25.0` ✅

## 5️⃣ Tester via Gateway avec GET (permitAll)

```bash
# GET est permitAll, donc pas besoin de token
curl "http://localhost:8082/mcp/test/multiply?a=5&b=5"
```

## 6️⃣ Vérifier les rôles dans le token

```bash
# Décoder le payload du JWT
echo $TOKEN | cut -d'.' -f2 | base64 -d 2>/dev/null | jq .

# Voir spécifiquement les rôles
echo $TOKEN | cut -d'.' -f2 | base64 -d 2>/dev/null | jq -r '.realm_access.roles'
```

## 📋 Résumé des tokens disponibles

### Token utilisateur (testuser)
- **Username:** testuser
- **Password:** password123
- **Grant type:** password
- **Rôles:** MCP_USER
- **Usage:** Tester /mcp/chat/**

### Token client
- **Grant type:** client_credentials
- **Rôles:** Aucun (service account)
- **Usage:** Appels machine-to-machine

## 🎯 Tests complets

```bash
# 1. Créer l'utilisateur
./create-user-with-role.sh

# 2. Obtenir token utilisateur
TOKEN=$(curl -k -s -X POST https://localhost:9000/realms/mcp-realm/protocol/openid-connect/token \
  -d "client_id=mcp-client" \
  -d "client_secret=secret" \
  -d "grant_type=password" \
  -d "username=testuser" \
  -d "password=password123" \
  -d "scope=mcp:read mcp:write" | jq -r .access_token)

# 3. Tester
curl -G "http://localhost:8082/mcp/chat/ask" \
  --data-urlencode "question=Calculate 5 times 5" \
  -H "Authorization: Bearer $TOKEN"
```

## ⚠️ Note importante

Le client MCP nécessite une **vraie clé API OpenAI** pour fonctionner.
Sans clé valide, vous obtiendrez une erreur même avec un token correct.

Pour tester sans OpenAI, utilisez les endpoints de test direct :
```bash
curl "http://localhost:8080/api/test/multiply?a=5&b=5" \
  -H "Authorization: Bearer $TOKEN_CLIENT"
```


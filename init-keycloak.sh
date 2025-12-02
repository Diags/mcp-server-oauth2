#!/bin/bash

echo "=== Initialisation de Keycloak ==="
echo ""

# Obtenir un token admin
echo "1. Connexion à Keycloak en tant qu'admin..."
ADMIN_TOKEN=$(curl -k -s -X POST https://localhost:9000/realms/master/protocol/openid-connect/token \
  -d "client_id=admin-cli" \
  -d "username=admin" \
  -d "password=admin" \
  -d "grant_type=password" | jq -r .access_token)

if [ "$ADMIN_TOKEN" == "null" ] || [ -z "$ADMIN_TOKEN" ]; then
    echo "❌ Impossible d'obtenir le token admin"
    exit 1
fi

echo "✓ Token admin obtenu"
echo ""

# Créer le realm
echo "2. Création du realm 'mcp-realm'..."
curl -k -s -X POST https://localhost:9000/admin/realms \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "realm": "mcp-realm",
    "enabled": true
  }'
echo "✓ Realm créé"
echo ""

# Créer le client
echo "3. Création du client 'mcp-client'..."
curl -k -s -X POST https://localhost:9000/admin/realms/mcp-realm/clients \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "clientId": "mcp-client",
    "enabled": true,
    "clientAuthenticatorType": "client-secret",
    "secret": "secret",
    "redirectUris": ["http://localhost:8081/*", "https://localhost:8081/*"],
    "webOrigins": ["http://localhost:8081", "https://localhost:8081"],
    "protocol": "openid-connect",
    "publicClient": false,
    "serviceAccountsEnabled": true,
    "directAccessGrantsEnabled": true,
    "standardFlowEnabled": true
  }'
echo "✓ Client créé"
echo ""

# Créer les client scopes
echo "4. Création des scopes 'mcp:read' et 'mcp:write'..."
curl -k -s -X POST https://localhost:9000/admin/realms/mcp-realm/client-scopes \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "mcp:read",
    "description": "Read access to MCP tools",
    "protocol": "openid-connect",
    "attributes": {
      "include.in.token.scope": "true",
      "display.on.consent.screen": "false"
    }
  }'

curl -k -s -X POST https://localhost:9000/admin/realms/mcp-realm/client-scopes \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "mcp:write",
    "description": "Write access to MCP tools",
    "protocol": "openid-connect",
    "attributes": {
      "include.in.token.scope": "true",
      "display.on.consent.screen": "false"
    }
  }'
echo "✓ Scopes créés"
echo ""

# Assigner les scopes au client
echo "5. Attribution des scopes au client..."
# Récupérer l'ID du client
CLIENT_ID=$(curl -k -s https://localhost:9000/admin/realms/mcp-realm/clients \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq -r '.[] | select(.clientId=="mcp-client") | .id')

# Récupérer les IDs des scopes
SCOPE_READ_ID=$(curl -k -s https://localhost:9000/admin/realms/mcp-realm/client-scopes \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq -r '.[] | select(.name=="mcp:read") | .id')

SCOPE_WRITE_ID=$(curl -k -s https://localhost:9000/admin/realms/mcp-realm/client-scopes \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq -r '.[] | select(.name=="mcp:write") | .id')

# Assigner les scopes
curl -k -s -X PUT https://localhost:9000/admin/realms/mcp-realm/clients/$CLIENT_ID/default-client-scopes/$SCOPE_READ_ID \
  -H "Authorization: Bearer $ADMIN_TOKEN"

curl -k -s -X PUT https://localhost:9000/admin/realms/mcp-realm/clients/$CLIENT_ID/default-client-scopes/$SCOPE_WRITE_ID \
  -H "Authorization: Bearer $ADMIN_TOKEN"

echo "✓ Scopes assignés"
echo ""

echo "=== Configuration Keycloak terminée! ==="
echo ""
echo "Vous pouvez maintenant tester avec:"
echo "./test-5x5-direct.sh"


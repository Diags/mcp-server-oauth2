#!/bin/bash

# Script pour créer un utilisateur avec le rôle MCP_USER dans Keycloak

echo "=== Création d'un utilisateur avec rôle MCP_USER ==="
echo ""

# 1. Obtenir token admin
echo "1. Connexion admin Keycloak..."
ADMIN_TOKEN=$(curl -k -s -X POST https://localhost:9000/realms/master/protocol/openid-connect/token \
  -d "client_id=admin-cli" \
  -d "username=admin" \
  -d "password=admin" \
  -d "grant_type=password" | jq -r .access_token)

if [ "$ADMIN_TOKEN" == "null" ] || [ -z "$ADMIN_TOKEN" ]; then
    echo "❌ Erreur: Token admin non reçu"
    exit 1
fi

echo "✓ Token admin obtenu"
echo ""

# 2. Créer le rôle MCP_USER
echo "2. Création du rôle MCP_USER..."
curl -k -s -X POST https://localhost:9000/admin/realms/mcp-realm/roles \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "MCP_USER",
    "description": "Role for MCP chat access"
  }'
echo "✓ Rôle créé"
echo ""

# 3. Créer le rôle MCP_ADMIN
echo "3. Création du rôle MCP_ADMIN..."
curl -k -s -X POST https://localhost:9000/admin/realms/mcp-realm/roles \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "MCP_ADMIN",
    "description": "Role for MCP admin access"
  }'
echo "✓ Rôle créé"
echo ""

# 4. Créer un utilisateur
echo "4. Création de l'utilisateur 'testuser'..."
curl -k -s -X POST https://localhost:9000/admin/realms/mcp-realm/users \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "enabled": true,
    "email": "testuser@example.com",
    "firstName": "Test",
    "lastName": "User",
    "credentials": [{
      "type": "password",
      "value": "password123",
      "temporary": false
    }]
  }'
echo "✓ Utilisateur créé (username: testuser, password: password123)"
echo ""

# 5. Récupérer l'ID de l'utilisateur
echo "5. Récupération de l'ID utilisateur..."
USER_ID=$(curl -k -s https://localhost:9000/admin/realms/mcp-realm/users?username=testuser \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq -r '.[0].id')

echo "✓ User ID: $USER_ID"
echo ""

# 6. Assigner le rôle MCP_USER à l'utilisateur
echo "6. Attribution du rôle MCP_USER..."
curl -k -s -X POST https://localhost:9000/admin/realms/mcp-realm/users/$USER_ID/role-mappings/realm \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '[{
    "name": "MCP_USER"
  }]'
echo "✓ Rôle assigné"
echo ""

echo "=== ✅ Configuration terminée! ==="
echo ""
echo "Utilisateur créé:"
echo "  Username: testuser"
echo "  Password: password123"
echo "  Rôle: MCP_USER"
echo ""
echo "Pour obtenir un token:"
echo '  TOKEN=$(curl -k -s -X POST https://localhost:9000/realms/mcp-realm/protocol/openid-connect/token \'
echo '    -d "client_id=mcp-client" \'
echo '    -d "client_secret=secret" \'
echo '    -d "grant_type=password" \'
echo '    -d "username=testuser" \'
echo '    -d "password=password123" \'
echo '    -d "scope=mcp:read mcp:write" | jq -r .access_token)'
echo ""
echo "Puis tester:"
echo '  curl "http://localhost:8082/mcp/chat/ask?question=Hello" -H "Authorization: Bearer $TOKEN"'


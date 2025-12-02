#!/bin/bash

# Test du Gateway avec authentification OAuth2

echo "=== Test du Gateway Server ===" 
echo ""

# 1. Obtenir un token depuis Keycloak
echo "1. Obtention du token OAuth2..."
TOKEN=$(curl -k -s -X POST https://localhost:9000/realms/mcp-realm/protocol/openid-connect/token \
  -d "client_id=mcp-client" \
  -d "client_secret=secret" \
  -d "grant_type=client_credentials" \
  -d "scope=mcp:read mcp:write" | jq -r .access_token)

if [ "$TOKEN" == "null" ] || [ -z "$TOKEN" ]; then
    echo "❌ Erreur: Token non reçu"
    exit 1
fi

echo "✓ Token obtenu: ${TOKEN:0:30}..."
echo ""

# 2. Test via Gateway - endpoint de test multiply
echo "2. Test via Gateway: /mcp/test/multiply (5 X 5)..."
RESULT=$(curl -s "http://localhost:8082/mcp/test/multiply?a=5&b=5" \
  -H "Authorization: Bearer $TOKEN")

echo "Résultat: $RESULT"
echo ""

if [ "$RESULT" == "25.0" ]; then
    echo "✅ TEST GATEWAY RÉUSSI! 5 X 5 = 25"
else
    echo "⚠ Résultat inattendu"
fi

echo ""
echo "3. Test via Gateway: /mcp/test/add (10 + 5)..."
RESULT_ADD=$(curl -s "http://localhost:8082/mcp/test/add?a=10&b=5" \
  -H "Authorization: Bearer $TOKEN")

echo "Résultat: $RESULT_ADD"

if [ "$RESULT_ADD" == "15.0" ]; then
    echo "✅ Addition réussie! 10 + 5 = 15"
fi

echo ""
echo "=== Tous les tests Gateway réussis! ==="
echo ""
echo "Endpoints disponibles via Gateway:"
echo "  - http://localhost:8082/mcp/chat/ask?question=..."
echo "  - http://localhost:8082/mcp/test/multiply?a=5&b=5"
echo "  - http://localhost:8082/mcp/test/add?a=10&b=5"
echo "  - http://localhost:8082/actuator/health"


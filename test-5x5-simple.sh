#!/bin/bash

# Test simple de 5 X 5 via l'endpoint REST

echo "=== Test MathTools: 5 X 5 ===" 
echo ""

# 1. Obtenir un token depuis Keycloak
echo "1. Obtention du token OAuth2 depuis Keycloak HTTPS..."
TOKEN=$(curl -k -s -X POST https://localhost:9000/realms/mcp-realm/protocol/openid-connect/token \
  -d "client_id=mcp-client" \
  -d "client_secret=secret" \
  -d "grant_type=client_credentials" \
  -d "scope=mcp:read mcp:write" | jq -r .access_token)

if [ "$TOKEN" == "null" ] || [ -z "$TOKEN" ]; then
    echo "❌ Erreur: Token non reçu"
    echo "Assurez-vous que Keycloak est configuré (./init-keycloak.sh)"
    exit 1
fi

echo "✓ Token obtenu: ${TOKEN:0:30}..."
echo ""

# 2. Appeler l'endpoint multiply
echo "2. Appel de multiply(5, 5) via /api/test/multiply..."
RESULT=$(curl -s "http://localhost:8080/api/test/multiply?a=5&b=5" \
  -H "Authorization: Bearer $TOKEN")

echo "Résultat: $RESULT"
echo ""

# 3. Vérifier le résultat
if [ "$RESULT" == "25.0" ]; then
    echo "✅ TEST RÉUSSI! 5 X 5 = 25"
else
    echo "⚠ Résultat inattendu: $RESULT"
fi

echo ""
echo "Autres tests disponibles:"
echo "  - Addition: curl \"http://localhost:8080/api/test/add?a=10&b=5\" -H \"Authorization: Bearer \$TOKEN\""
echo "  - Multiplication: curl \"http://localhost:8080/api/test/multiply?a=7&b=8\" -H \"Authorization: Bearer \$TOKEN\""


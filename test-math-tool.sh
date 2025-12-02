#!/bin/bash

# Script pour tester le MathTool avec "5 X 5"
# Assurez-vous que Docker est démarré et que les services sont lancés avec: docker-compose up -d

echo "Test du MathTool: 5 X 5"
echo "======================"
echo ""

# Attendre que les services soient prêts
echo "Vérification de la disponibilité des services..."
MAX_RETRIES=30
RETRY_COUNT=0

while [ $RETRY_COUNT -lt $MAX_RETRIES ]; do
    if curl -s http://localhost:8081/actuator/health > /dev/null 2>&1; then
        echo "✓ Client MCP est prêt"
        break
    fi
    RETRY_COUNT=$((RETRY_COUNT + 1))
    echo "En attente du client MCP... ($RETRY_COUNT/$MAX_RETRIES)"
    sleep 2
done

if [ $RETRY_COUNT -eq $MAX_RETRIES ]; then
    echo "❌ Le client MCP n'est pas disponible. Assurez-vous que docker-compose up a été exécuté."
    exit 1
fi

echo ""
echo "Envoi de la requête: What is 5 multiplied by 5?"
echo ""

# Envoyer la requête (avec -k pour accepter les certificats auto-signés si nécessaire)
RESPONSE=$(curl -k -s -G "http://localhost:8081/chat/ask" \
    --data-urlencode "question=What is 5 multiplied by 5?")

echo "Réponse reçue:"
echo "$RESPONSE"
echo ""

# Vérifier si la réponse contient "25"
if echo "$RESPONSE" | grep -qi "25\|twenty-five"; then
    echo "✓ Test réussi! La réponse contient le résultat attendu (25)"
else
    echo "⚠ La réponse ne contient pas clairement '25'. Réponse complète ci-dessus."
fi


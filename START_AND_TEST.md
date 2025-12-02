# Guide de démarrage et test du MathTool

## Prérequis
- Docker Desktop doit être démarré
- Variable d'environnement `OPENAI_API_KEY` définie (optionnel pour les tests de base)

## Étapes pour démarrer et tester

### 1. Démarrer Docker Desktop
Assurez-vous que Docker Desktop est lancé sur votre machine.

### 2. Démarrer tous les services
```bash
cd /Users/diaguily/wokspace/sources/ServerMCP
docker-compose up -d
```

### 3. Vérifier que les services sont prêts
Attendez environ 1-2 minutes que tous les services soient démarrés, puis vérifiez :
```bash
docker-compose ps
```

Vous devriez voir tous les services avec le statut "Up" :
- postgres-vector
- postgres-meta
- keycloak
- minio
- mcp-server
- mcp-client

### 4. Tester le MathTool avec "5 X 5"

#### Option 1 : Utiliser le script de test
```bash
./test-math-tool.sh
```

#### Option 2 : Utiliser curl directement
```bash
curl -G "http://localhost:8081/chat/ask" \
  --data-urlencode "question=What is 5 multiplied by 5?"
```

#### Option 3 : Utiliser curl avec formatage JSON
```bash
curl -G "http://localhost:8081/chat/ask" \
  --data-urlencode "question=Calculate 5 times 5" \
  | jq .
```

### 5. Autres tests possibles

#### Test d'addition
```bash
curl -G "http://localhost:8081/chat/ask" \
  --data-urlencode "question=What is 10 plus 5?"
```

#### Test de division
```bash
curl -G "http://localhost:8081/chat/ask" \
  --data-urlencode "question=What is 20 divided by 4?"
```

#### Test avec plusieurs opérations
```bash
curl -G "http://localhost:8081/chat/ask" \
  --data-urlencode "question=Calculate 5 plus 3, then multiply the result by 2"
```

## Vérification des logs

Si quelque chose ne fonctionne pas, vérifiez les logs :
```bash
# Logs de tous les services
docker-compose logs -f

# Logs d'un service spécifique
docker-compose logs -f mcp-client
docker-compose logs -f mcp-server
docker-compose logs -f keycloak
```

## Arrêter les services
```bash
docker-compose down
```

## Nettoyage complet
```bash
docker-compose down -v  # Supprime aussi les volumes
```


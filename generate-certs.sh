#!/bin/bash

# Script pour générer des certificats SSL auto-signés pour Keycloak

echo "=== Génération des certificats SSL pour Keycloak ==="
echo ""

# Créer le répertoire certs s'il n'existe pas
mkdir -p certs

# Générer la clé privée et le certificat auto-signé
echo "1. Génération de la clé privée et du certificat..."
openssl req -x509 -newkey rsa:2048 -nodes \
  -keyout certs/key.pem \
  -out certs/cert.pem \
  -days 365 \
  -subj "/CN=localhost/O=MCP/C=FR"

if [ $? -ne 0 ]; then
    echo "❌ Erreur lors de la génération des certificats"
    exit 1
fi

echo "✓ Certificats générés"
echo ""

# Créer le keystore PKCS12
echo "2. Création du keystore PKCS12..."
openssl pkcs12 -export \
  -in certs/cert.pem \
  -inkey certs/key.pem \
  -out certs/keycloak.p12 \
  -name keycloak \
  -passout pass:changeit

if [ $? -ne 0 ]; then
    echo "❌ Erreur lors de la création du keystore"
    exit 1
fi

echo "✓ Keystore créé"
echo ""

# Afficher les informations du certificat
echo "3. Informations du certificat:"
openssl x509 -in certs/cert.pem -text -noout | grep -A 2 "Subject:"
echo ""

echo "=== Certificats générés avec succès! ==="
echo ""
echo "Fichiers créés:"
echo "  - certs/key.pem       : Clé privée"
echo "  - certs/cert.pem      : Certificat SSL"
echo "  - certs/keycloak.p12  : Keystore PKCS12"
echo ""
echo "Vous pouvez maintenant redémarrer Keycloak avec HTTPS:"
echo "  docker compose down"
echo "  docker compose up -d"


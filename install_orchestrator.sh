#!/bin/bash

# Stratus Orchestrator Setup Script
# Copyright (c) 2026 Slam Studios

set -e

COLOR_BLUE='\033[0;34m'
COLOR_GREEN='\033[0;32m'
COLOR_RED='\033[0;31m'
COLOR_NC='\033[0m' # No Color

echo -e "${COLOR_BLUE}--- Stratus Orchestrator Setup ---${COLOR_NC}"

# 1. Environment Configuration
echo -e "\n${COLOR_BLUE}--- Configuration ---${COLOR_NC}"

# 1. Environment Configuration
echo -e "\n${COLOR_BLUE}--- Configuration (Press Enter for Defaults) ---${COLOR_NC}"

read -p "Stratus API Token [$(openssl rand -hex 16 2>/dev/null || echo "changeme")]: " STRATUS_TOKEN
STRATUS_TOKEN=${STRATUS_TOKEN:-$(openssl rand -hex 16 2>/dev/null || echo "changeme")}

read -p "Database Host [localhost]: " DB_HOST
DB_HOST=${DB_HOST:-localhost}

read -p "Database Port [3306]: " DB_PORT
DB_PORT=${DB_PORT:-3306}

read -p "Database Name [stratus]: " DB_NAME
DB_NAME=${DB_NAME:-stratus}

read -p "Database User [stratus]: " DB_USER
DB_USER=${DB_USER:-stratus}

read -p "Database Password [changeme]: " DB_PASS
DB_PASS=${DB_PASS:-changeme}

read -p "Redis Host [localhost]: " REDIS_HOST
REDIS_HOST=${REDIS_HOST:-localhost}

read -p "Redis Port [6379]: " REDIS_PORT
REDIS_PORT=${REDIS_PORT:-6379}

read -p "Pterodactyl Panel URL [https://panel.example.com]: " PTERO_URL
PTERO_URL=${PTERO_URL:-https://panel.example.com}

read -p "Pterodactyl Application API Key [changeme]: " PTERO_KEY
PTERO_KEY=${PTERO_KEY:-changeme}

read -p "Pterodactyl Owner User ID [1]: " PTERO_OWNER
PTERO_OWNER=${PTERO_OWNER:-1}

read -p "Public Orchestrator URL [http://$(curl -s https://ifconfig.me || echo "localhost"):8081]: " ORCH_URL
ORCH_URL=${ORCH_URL:-http://localhost:8081}

# 2. Generate config.yml file
echo -e "\n${COLOR_BLUE}--- Generating config.yml ---${COLOR_NC}"

cat <<EOT > config.yml
# Stratus Orchestrator Configuration
# Generated on $(date)

token: "$STRATUS_TOKEN"

database:
  host: "$DB_HOST"
  port: $DB_PORT
  name: "$DB_NAME"
  user: "$DB_USER"
  password: "$DB_PASS"
  poolSize: 10

redis:
  host: "$REDIS_HOST"
  port: $REDIS_PORT
  password: ""

pterodactyl:
  baseUrl: "$PTERO_URL"
  apiKey: "$PTERO_KEY"
  ownerId: $PTERO_OWNER
  orchestratorUrl: "$ORCH_URL"
EOT

echo -e "${COLOR_GREEN}✔ config.yml generated.${COLOR_NC}"

# 3. Final Instructions
echo -e "\n${COLOR_GREEN}Setup Complete!${COLOR_NC}"
echo "You can now run the orchestrator using:"
echo "  Option A (Development): ./gradlew run"
echo "  Option B (Production):  java -jar build/libs/stratus-orchestrator-all.jar"
echo ""
echo "Make sure you have Java 17+, MariaDB, and Redis installed and running."
echo "The orchestrator is listening on port 8081."

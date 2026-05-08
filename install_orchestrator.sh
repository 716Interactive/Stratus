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

read -p "Stratus API Token (Secure Secret): " STRATUS_TOKEN
read -p "Database Host (e.g., localhost): " DB_HOST
read -p "Database Port (default 3306): " DB_PORT
DB_PORT=${DB_PORT:-3306}
read -p "Database Name (default stratus): " DB_NAME
DB_NAME=${DB_NAME:-stratus}
read -p "Database User: " DB_USER
read -sp "Database Password: " DB_PASS
echo ""
read -p "Redis Host (default localhost): " REDIS_HOST
REDIS_HOST=${REDIS_HOST:-localhost}
read -p "Redis Port (default 6379): " REDIS_PORT
REDIS_PORT=${REDIS_PORT:-6379}
read -p "Pterodactyl Panel URL (e.g., https://panel.example.com): " PTERO_URL
read -p "Pterodactyl Application API Key: " PTERO_KEY
read -p "Pterodactyl Owner User ID (default 1): " PTERO_OWNER
PTERO_OWNER=${PTERO_OWNER:-1}
read -p "Public Orchestrator URL (e.g., http://1.2.3.4:8080): " ORCH_URL

# 2. Generate .env file
echo -e "\n${COLOR_BLUE}--- Generating Environment File ---${COLOR_NC}"

cat <<EOT > .env
STRATUS_TOKEN=$STRATUS_TOKEN
DATABASE_HOST=$DB_HOST
DATABASE_PORT=$DB_PORT
DATABASE_NAME=$DB_NAME
DATABASE_USER=$DB_USER
DATABASE_PASSWORD=$DB_PASS
REDIS_HOST=$REDIS_HOST
REDIS_PORT=$REDIS_PORT
PTERODACTYL_URL=$PTERO_URL
PTERODACTYL_API_KEY=$PTERO_KEY
PTERODACTYL_OWNER_ID=$PTERO_OWNER
STRATUS_URL=$ORCH_URL
EOT

echo -e "${COLOR_GREEN}✔ .env file generated.${COLOR_NC}"

# 3. Final Instructions
echo -e "\n${COLOR_GREEN}Setup Complete!${COLOR_NC}"
echo "You can now run the orchestrator using: ./gradlew run"
echo "Make sure you have Java 17+, MariaDB, and Redis installed and running."

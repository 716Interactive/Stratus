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

# 3. Build Project
echo -e "\n${COLOR_BLUE}--- Building Stratus Project ---${COLOR_NC}"
read -p "Would you like to build the project now? (y/n) [y]: " BUILD_NOW
BUILD_NOW=${BUILD_NOW:-y}

if [[ $BUILD_NOW =~ ^[Yy]$ ]]; then
    echo -e "${COLOR_BLUE}Running Gradle build (this may take a few minutes)...${COLOR_NC}"
    chmod +x gradlew
    ./gradlew shadowJar :stratus-plugin:build -x test
    echo -e "${COLOR_GREEN}✔ Build successful! Artifacts generated in build/libs folders.${COLOR_NC}"
else
    echo -e "${COLOR_RED}Build skipped. You will need to build manually before running.${COLOR_NC}"
fi

# 4. Systemd Service
echo -e "\n${COLOR_BLUE}--- Systemd Service Installation ---${COLOR_NC}"
read -p "Would you like to install the systemd service? (y/n) [n]: " INSTALL_SERVICE
INSTALL_SERVICE=${INSTALL_SERVICE:-n}

if [[ $INSTALL_SERVICE =~ ^[Yy]$ ]]; then
    echo -e "${COLOR_BLUE}Deploying to system directories... (requires sudo)${COLOR_NC}"
    
    # 1. Create Directories
    sudo mkdir -p /opt/stratus
    sudo mkdir -p /etc/stratus
    sudo mkdir -p /var/log/stratus
    
    # 2. Copy Files
    if [ -f "build/libs/stratus-orchestrator-all.jar" ]; then
        sudo cp -v build/libs/stratus-orchestrator-all.jar /opt/stratus/
    fi
    sudo cp -v config.yml /etc/stratus/config.yml
    
    # 3. Fix Permissions
    CUR_USER=$(whoami)
    sudo chown -R $CUR_USER:$CUR_USER /opt/stratus
    sudo chown -R $CUR_USER:$CUR_USER /etc/stratus
    sudo chown -R $CUR_USER:$CUR_USER /var/log/stratus

    # 4. Generate Service
    JAVA_PATH=$(which java)

    cat <<EOT > stratus.service
[Unit]
Description=Stratus Orchestrator
After=network.target mysql.service redis.service

[Service]
User=$CUR_USER
WorkingDirectory=/opt/stratus
Environment="STRATUS_LOG_DIR=/var/log/stratus"
ExecStart=$JAVA_PATH -jar /opt/stratus/stratus-orchestrator-all.jar
Restart=always
RestartSec=5

[Install]
WantedBy=multi-user.target
EOT

    sudo mv stratus.service /etc/systemd/system/stratus.service
    sudo systemctl daemon-reload
    sudo systemctl enable stratus
    echo -e "${COLOR_GREEN}✔ Service installed and enabled.${COLOR_NC}"
    echo "You can start it using: sudo systemctl start stratus"
    echo "Logs are available at: /var/log/stratus/stratus.log"
    echo "Config is available at: /etc/stratus/config.yml"
else
    echo "Service installation skipped."
fi

# 5. Final Instructions
echo -e "\n${COLOR_GREEN}Setup Complete!${COLOR_NC}"
echo "You can now start the orchestrator using:"
if [[ $INSTALL_SERVICE =~ ^[Yy]$ ]]; then
    echo "  sudo systemctl start stratus"
    echo "  tail -f /var/log/stratus/stratus.log"
else
    echo "  java -jar build/libs/stratus-orchestrator-all.jar"
fi
echo ""
echo "Plugins for game servers are located in:"
echo "  stratus-plugin/[spigot|velocity|bungeecord]/build/libs/"
echo ""
echo "To update in the future, run: bash update_orchestrator.sh"

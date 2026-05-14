#!/bin/bash

# Stratus Orchestrator Update Script
# Copyright (c) 2026 Slam Studios

set -e

COLOR_BLUE='\033[0;34m'
COLOR_GREEN='\033[0;32m'
COLOR_RED='\033[0;31m'
COLOR_NC='\033[0m' # No Color

echo -e "${COLOR_BLUE}--- Stratus Orchestrator Updater ---${COLOR_NC}"

# 1. Pull latest changes
echo -e "\n${COLOR_BLUE}Checking for updates...${COLOR_NC}"
git fetch origin
LOCAL=$(git rev-parse HEAD)
REMOTE=$(git rev-parse @{u})

if [ $LOCAL = $REMOTE ]; then
    echo -e "${COLOR_GREEN}✔ Already up to date.${COLOR_NC}"
    read -p "Force rebuild anyway? (y/n) [n]: " FORCE_REBUILD
    FORCE_REBUILD=${FORCE_REBUILD:-n}
    if [[ ! $FORCE_REBUILD =~ ^[Yy]$ ]]; then
        exit 0
    fi
else
    echo -e "${COLOR_BLUE}Updates found! Pulling changes...${COLOR_NC}"
    git pull
fi

# 2. Rebuild the project
echo -e "\n${COLOR_BLUE}--- Rebuilding Stratus Project ---${COLOR_NC}"
chmod +x gradlew
./gradlew shadowJar :stratus-plugin:build -x test --no-build-cache

# 3. Handle Migration and Deployment
if [ ! -d "/opt/stratus" ]; then
    echo -e "\n${COLOR_BLUE}--- Migrating to Professional Structure ---${COLOR_NC}"
    echo "This will move your configuration to /etc/stratus and application to /opt/stratus."
    
    # Create Directories
    sudo mkdir -p /opt/stratus
    sudo mkdir -p /etc/stratus
    sudo mkdir -p /var/log/stratus
    
    # Move Config if exists
    if [ -f "config.yml" ]; then
        sudo cp -v config.yml /etc/stratus/config.yml
    fi
    
    # Fix Permissions
    CUR_USER=$(whoami)
    sudo chown -R $CUR_USER:$CUR_USER /opt/stratus
    sudo chown -R $CUR_USER:$CUR_USER /etc/stratus
    sudo chown -R $CUR_USER:$CUR_USER /var/log/stratus

    # Re-install Service (with new paths)
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
    echo -e "${COLOR_GREEN}✔ Migration complete.${COLOR_NC}"
fi

# Deploy new JAR
echo -e "${COLOR_BLUE}Deploying new JAR to /opt/stratus...${COLOR_NC}"
sudo cp -v build/libs/stratus-orchestrator-all.jar /opt/stratus/

echo -e "\n${COLOR_GREEN}✔ Update and Build Complete!${COLOR_NC}"

# 4. Restart Service
if systemctl list-unit-files | grep -q "^stratus.service"; then
    echo -e "${COLOR_BLUE}Restarting Stratus service...${COLOR_NC}"
    sudo systemctl restart stratus
    echo -e "${COLOR_GREEN}✔ Service restarted.${COLOR_NC}"
else
    echo "Stratus service not found. Start it manually if needed."
fi

echo "Updated plugins are in their respective build/libs folders."

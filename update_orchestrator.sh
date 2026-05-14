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
./gradlew shadowJar :stratus-plugin:build -x test

# 3. Deploy new JAR
if [ -d "/opt/stratus" ]; then
    echo -e "${COLOR_BLUE}Deploying new JAR to /opt/stratus...${COLOR_NC}"
    sudo cp -v build/libs/stratus-orchestrator-all.jar /opt/stratus/
fi

echo -e "\n${COLOR_GREEN}✔ Update and Build Complete!${COLOR_NC}"

if systemctl is-active --quiet stratus; then
    echo -e "${COLOR_BLUE}Restarting Stratus service...${COLOR_NC}"
    sudo systemctl restart stratus
    echo -e "${COLOR_GREEN}✔ Service restarted.${COLOR_NC}"
else
    echo "Restart your orchestrator service manually to apply changes."
fi

echo "Updated plugins are in their respective build/libs folders."

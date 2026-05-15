#!/bin/bash

# Stratus Panel Installation Script
# Copyright (c) 2026 Slam Studios

set -e

PANEL_PATH="/var/www/pterodactyl"
COLOR_BLUE='\033[0;34m'
COLOR_GREEN='\033[0;32m'
COLOR_RED='\033[0;31m'
COLOR_NC='\033[0m' # No Color

echo -e "${COLOR_BLUE}--- Stratus Panel Installer ---${COLOR_NC}"

# 1. Check for Pterodactyl Installation
if [ ! -d "$PANEL_PATH" ]; then
    echo -e "${COLOR_RED}Error: Pterodactyl installation not found at $PANEL_PATH${COLOR_NC}"
    echo "Please make sure you are running this on your Pterodactyl Panel server."
    exit 1
fi

if [ ! -f "$PANEL_PATH/artisan" ]; then
    echo -e "${COLOR_RED}Error: artisan not found in $PANEL_PATH. Is Pterodactyl fully installed?${COLOR_NC}"
    exit 1
fi

echo -e "${COLOR_GREEN}✔ Pterodactyl installation detected.${COLOR_NC}"

# 2. Configuration
echo -e "\n${COLOR_BLUE}--- Configuration ---${COLOR_NC}"
read -p "Enter Stratus Orchestrator URL (e.g., http://1.2.3.4:8081): " STRATUS_URL
read -p "Enter Stratus API Token: " STRATUS_TOKEN

if [ -z "$STRATUS_URL" ] || [ -z "$STRATUS_TOKEN" ]; then
    echo -e "${COLOR_RED}Error: URL and Token cannot be empty.${COLOR_NC}"
    exit 1
fi

# 3. Copying Files
echo -e "\n${COLOR_BLUE}--- Copying Stratus Files ---${COLOR_NC}"

# Ensure directories exist
mkdir -p "$PANEL_PATH/app/Services/Stratus"
mkdir -p "$PANEL_PATH/app/Http/Controllers/Admin/Stratus"
mkdir -p "$PANEL_PATH/app/Listeners/Stratus"
mkdir -p "$PANEL_PATH/resources/views/admin/stratus/groups"
mkdir -p "$PANEL_PATH/resources/views/admin/stratus/templates"
mkdir -p "$PANEL_PATH/resources/views/admin/stratus/proxies"
mkdir -p "$PANEL_PATH/storage/app/stratus"

# Copy files from the repo to the panel
cp -rv Pterodactyl/panel/app/Services/Stratus/* "$PANEL_PATH/app/Services/Stratus/"
cp -rv Pterodactyl/panel/app/Http/Controllers/Admin/Stratus/* "$PANEL_PATH/app/Http/Controllers/Admin/Stratus/"
cp -rv Pterodactyl/panel/app/Listeners/Stratus/* "$PANEL_PATH/app/Listeners/Stratus/"
cp -v Pterodactyl/panel/config/stratus.php "$PANEL_PATH/config/"
cp -rv Pterodactyl/panel/resources/views/admin/stratus/* "$PANEL_PATH/resources/views/admin/stratus/"
cp -rv Pterodactyl/panel/resources/scripts/* "$PANEL_PATH/resources/scripts/"
cp -v Pterodactyl/panel/app/Providers/EventServiceProvider.php "$PANEL_PATH/app/Providers/"
cp -v Pterodactyl/panel/routes/admin.php "$PANEL_PATH/routes/"
cp -v Pterodactyl/panel/resources/views/layouts/admin.blade.php "$PANEL_PATH/resources/views/layouts/"

echo -e "${COLOR_GREEN}✔ Files copied.${COLOR_NC}"

# 3.1 Build Orchestrator and Plugins
echo -e "\n${COLOR_BLUE}--- Building Stratus Components ---${COLOR_NC}"
if [ -f "./gradlew" ]; then
    chmod +x ./gradlew
    ./gradlew shadowJar :stratus-plugin:build -x test --no-build-cache
    cp -v build/libs/stratus-orchestrator-all.jar ./stratus-orchestrator.jar
    # Copy plugin to panel storage for auto-install
    cp -v stratus-plugin/spigot/build/libs/stratus-plugin-spigot-all.jar "$PANEL_PATH/storage/app/stratus/StratusPluginSpigot.jar"
    cp -v stratus-plugin/velocity/build/libs/stratus-plugin-velocity-all.jar "$PANEL_PATH/storage/app/stratus/StratusPluginVelocity.jar"
    cp -v stratus-plugin/bungeecord/build/libs/stratus-plugin-bungeecord-all.jar "$PANEL_PATH/storage/app/stratus/StratusPluginBungee.jar"
    echo -e "${COLOR_GREEN}✔ Components built and placed.${COLOR_NC}"
else
    echo -e "${COLOR_RED}Warning: gradlew not found. Skipping build. Make sure you have built the JARs manually.${COLOR_NC}"
fi

# 4. Updating .env
echo -e "\n${COLOR_BLUE}--- Updating Environment Variables ---${COLOR_NC}"

if grep -q "STRATUS_URL" "$PANEL_PATH/.env"; then
    sed -i "s|STRATUS_URL=.*|STRATUS_URL=$STRATUS_URL|" "$PANEL_PATH/.env"
else
    echo -e "\nSTRATUS_URL=$STRATUS_URL" >> "$PANEL_PATH/.env"
fi

if grep -q "STRATUS_TOKEN" "$PANEL_PATH/.env"; then
    sed -i "s|STRATUS_TOKEN=.*|STRATUS_TOKEN=$STRATUS_TOKEN|" "$PANEL_PATH/.env"
else
    echo -e "STRATUS_TOKEN=$STRATUS_TOKEN" >> "$PANEL_PATH/.env"
fi

echo -e "${COLOR_GREEN}✔ .env updated.${COLOR_NC}"

# 5. Client-side Assets (React)
echo -e "\n${COLOR_BLUE}--- Building Client Assets ---${COLOR_NC}"
cd "$PANEL_PATH"
if [ -f "package.json" ]; then
    yarn build:production || npm run build:production
    echo -e "${COLOR_GREEN}✔ Client assets built.${COLOR_NC}"
fi

# 6. Finalizing
echo -e "\n${COLOR_BLUE}--- Finalizing ---${COLOR_NC}"
php artisan view:clear
php artisan config:clear
chown -R www-data:www-data "$PANEL_PATH"
chown -R www-data:www-data "$PANEL_PATH/storage/app/stratus"

echo -e "\n${COLOR_GREEN}Stratus Full-Stack Installation Complete!${COLOR_NC}"
echo "1. The Orchestrator is ready to run (java -jar stratus-orchestrator.jar)."
echo "2. The Panel is integrated with Groups, Templates, Proxies, and Backups."
echo "3. Auto-installation of plugins is enabled for new servers."


#!/bin/bash

# Stratus Panel Non-Interactive Updater Script
# Copyright (c) 2026 Slam Studios

set -e

PANEL_PATH="/var/www/pterodactyl"
COLOR_BLUE='\033[0;34m'
COLOR_GREEN='\033[0;32m'
COLOR_RED='\033[0;31m'
COLOR_NC='\033[0m' # No Color

echo -e "${COLOR_BLUE}--- Stratus Panel Updater (Fast-Update) ---${COLOR_NC}"

# 1. Check for Pterodactyl Installation
if [ ! -d "$PANEL_PATH" ] || [ ! -f "$PANEL_PATH/.env" ]; then
    echo -e "${COLOR_RED}Error: Pterodactyl installation or .env file not found at $PANEL_PATH${COLOR_NC}"
    exit 1
fi

echo -e "${COLOR_GREEN}✔ Pterodactyl installation detected.${COLOR_NC}"

# 2. Extract existing configuration from .env
echo -e "\n${COLOR_BLUE}--- Reading Configuration ---${COLOR_NC}"
STRATUS_URL=$(sudo grep "^STRATUS_URL=" "$PANEL_PATH/.env" | cut -d'=' -f2-)
STRATUS_TOKEN=$(sudo grep "^STRATUS_TOKEN=" "$PANEL_PATH/.env" | cut -d'=' -f2-)

if [ -z "$STRATUS_URL" ] || [ -z "$STRATUS_TOKEN" ]; then
    echo -e "${COLOR_RED}Warning: Stratus variables not fully configured in Pterodactyl's .env file.${COLOR_NC}"
    echo "Falling back to default installer to gather credentials..."
    bash install_panel.sh
    exit 0
fi

echo -e "Stratus URL: ${COLOR_GREEN}$STRATUS_URL${COLOR_NC}"
echo -e "Stratus Token: ${COLOR_GREEN}Loaded successfully${COLOR_NC}"

# 3. Copying Files
echo -e "\n${COLOR_BLUE}--- Copying Updated Stratus Files ---${COLOR_NC}"

# Ensure directories exist
sudo mkdir -p "$PANEL_PATH/app/Services/Stratus"
sudo mkdir -p "$PANEL_PATH/app/Http/Controllers/Admin/Stratus"
sudo mkdir -p "$PANEL_PATH/app/Http/Controllers/Api/Client/Stratus"
sudo mkdir -p "$PANEL_PATH/app/Listeners/Stratus"
sudo mkdir -p "$PANEL_PATH/resources/views/admin/stratus/groups"
sudo mkdir -p "$PANEL_PATH/resources/views/admin/stratus/templates"
sudo mkdir -p "$PANEL_PATH/resources/views/admin/stratus/proxies"
sudo mkdir -p "$PANEL_PATH/storage/app/stratus"

# Copy files from the repository to the panel path
sudo cp -rv Pterodactyl/panel/app/Services/Stratus/* "$PANEL_PATH/app/Services/Stratus/"
sudo cp -rv Pterodactyl/panel/app/Http/Controllers/Admin/Stratus/* "$PANEL_PATH/app/Http/Controllers/Admin/Stratus/"
sudo cp -rv Pterodactyl/panel/app/Http/Controllers/Api/Client/Stratus/* "$PANEL_PATH/app/Http/Controllers/Api/Client/Stratus/"
sudo cp -rv Pterodactyl/panel/app/Listeners/Stratus/* "$PANEL_PATH/app/Listeners/Stratus/"
sudo cp -v Pterodactyl/panel/config/stratus.php "$PANEL_PATH/config/"
sudo cp -rv Pterodactyl/panel/resources/views/admin/stratus/* "$PANEL_PATH/resources/views/admin/stratus/"
sudo cp -rv Pterodactyl/panel/resources/scripts/* "$PANEL_PATH/resources/scripts/"
sudo cp -v Pterodactyl/panel/app/Providers/EventServiceProvider.php "$PANEL_PATH/app/Providers/"
sudo cp -v Pterodactyl/panel/routes/admin.php "$PANEL_PATH/routes/"
sudo cp -v Pterodactyl/panel/routes/api-client.php "$PANEL_PATH/routes/"
sudo cp -v Pterodactyl/panel/resources/views/layouts/admin.blade.php "$PANEL_PATH/resources/views/layouts/"

echo -e "${COLOR_GREEN}✔ Files copied successfully.${COLOR_NC}"

# 3.1 Build Plugins for Distribution
echo -e "\n${COLOR_BLUE}--- Rebuilding Stratus Plugins ---${COLOR_NC}"
if [ -f "./gradlew" ]; then
    chmod +x ./gradlew
    if ./gradlew :stratus-plugin:spigot:shadowJar :stratus-plugin:velocity:shadowJar :stratus-plugin:bungeecord:shadowJar -x test --no-build-cache; then
        sudo mkdir -p "$PANEL_PATH/storage/app/stratus"
        [ -f "stratus-plugin/spigot/build/libs/stratus-plugin-spigot-all.jar" ] && sudo cp -v stratus-plugin/spigot/build/libs/stratus-plugin-spigot-all.jar "$PANEL_PATH/storage/app/stratus/StratusPluginSpigot.jar" || echo "Warning: Spigot plugin not found."
        [ -f "stratus-plugin/velocity/build/libs/stratus-plugin-velocity-all.jar" ] && sudo cp -v stratus-plugin/velocity/build/libs/stratus-plugin-velocity-all.jar "$PANEL_PATH/storage/app/stratus/StratusPluginVelocity.jar" || echo "Warning: Velocity plugin not found."
        [ -f "stratus-plugin/bungeecord/build/libs/stratus-plugin-bungeecord-all.jar" ] && sudo cp -v stratus-plugin/bungeecord/build/libs/stratus-plugin-bungeecord-all.jar "$PANEL_PATH/storage/app/stratus/StratusPluginBungee.jar" || echo "Warning: Bungee plugin not found."
        echo -e "${COLOR_GREEN}✔ Plugins rebuilt and placed in storage.${COLOR_NC}"
    else
        echo -e "${COLOR_RED}Error: Plugin build failed. Skipping plugin distribution.${COLOR_NC}"
    fi
else
    echo -e "${COLOR_RED}Warning: gradlew not found. Skipping plugin build.${COLOR_NC}"
fi

# 4. Client-side Assets (React)
echo -e "\n${COLOR_BLUE}--- Rebuilding Client Assets ---${COLOR_NC}"
cd "$PANEL_PATH"

if ! command -v yarn &> /dev/null && ! command -v npm &> /dev/null; then
    echo -e "${COLOR_RED}Node.js and Yarn are not installed. Installing automatically...${COLOR_NC}"
    curl -fsSL https://deb.nodesource.com/setup_22.x | sudo -E bash -
    sudo apt-get install -y nodejs
    sudo npm install -g yarn
fi

if [ -f "package.json" ]; then
    echo "Installing node modules (if needed)..."
    yarn install || npm install --legacy-peer-deps
    echo "Recompiling custom panel assets..."
    (yarn build:production || npm run build:production) || echo -e "${COLOR_RED}Warning: Asset build failed. Re-run manually if needed.${COLOR_NC}"
    echo -e "${COLOR_GREEN}✔ Client assets rebuilt.${COLOR_NC}"
fi

# 5. Finalizing Cache & Permissions
echo -e "\n${COLOR_BLUE}--- Clearing Caches & Resetting Ownership ---${COLOR_NC}"
sudo php artisan view:clear
sudo php artisan config:clear
sudo chown -R www-data:www-data "$PANEL_PATH"
sudo chown -R www-data:www-data "$PANEL_PATH/storage/app/stratus"

echo -e "\n${COLOR_GREEN}Stratus Panel Update Completed Successfully!${COLOR_NC}"

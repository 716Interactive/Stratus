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
read -p "Enter Stratus Orchestrator URL (e.g., http://1.2.3.4:8080): " STRATUS_URL
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
mkdir -p "$PANEL_PATH/resources/views/admin/stratus/groups"
mkdir -p "$PANEL_PATH/resources/views/admin/stratus/templates"

# Copy files from the repo to the panel (Assumes script is run from repo root)
cp -rv Pterodactyl/panel/app/Services/Stratus/* "$PANEL_PATH/app/Services/Stratus/"
cp -rv Pterodactyl/panel/app/Http/Controllers/Admin/Stratus/* "$PANEL_PATH/app/Http/Controllers/Admin/Stratus/"
cp -v Pterodactyl/panel/config/stratus.php "$PANEL_PATH/config/"
cp -rv Pterodactyl/panel/resources/views/admin/stratus/* "$PANEL_PATH/resources/views/admin/stratus/"

echo -e "${COLOR_GREEN}✔ Files copied.${COLOR_NC}"

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

# 5. Route Integration (Manual check)
echo -e "\n${COLOR_BLUE}--- Patching Routes ---${COLOR_NC}"
if grep -q "Stratus Routes" "$PANEL_PATH/routes/admin.php"; then
    echo "Routes already patched."
else
    cat <<EOT >> "$PANEL_PATH/routes/admin.php"

/*
|--------------------------------------------------------------------------
| Stratus Routes
|--------------------------------------------------------------------------
*/
use Pterodactyl\Http\Controllers\Admin\Stratus;
Route::group(['prefix' => 'stratus'], function () {
    Route::get('/', [Stratus\OrchestratorController::class, 'index'])->name('admin.stratus.orchestrator');
    
    Route::group(['prefix' => 'groups'], function () {
        Route::get('/', [Stratus\GroupController::class, 'index'])->name('admin.stratus.groups');
        Route::get('/new', [Stratus\GroupController::class, 'create'])->name('admin.stratus.groups.new');
        Route::post('/new', [Stratus\GroupController::class, 'store']);
        Route::get('/view/{group}', [Stratus\GroupController::class, 'view'])->name('admin.stratus.groups.view');
        Route::post('/view/{group}', [Stratus\GroupController::class, 'update']);
    });

    Route::group(['prefix' => 'templates'], function () {
        Route::get('/', [Stratus\TemplateController::class, 'index'])->name('admin.stratus.templates');
        Route::get('/new', [Stratus\TemplateController::class, 'create'])->name('admin.stratus.templates.new');
        Route::post('/new', [Stratus\TemplateController::class, 'store']);
        Route::get('/view/{template}', [Stratus\TemplateController::class, 'view'])->name('admin.stratus.templates.view');
        Route::post('/view/{template}/versions', [Stratus\TemplateController::class, 'storeVersion']);
    });
});
EOT
    echo -e "${COLOR_GREEN}✔ Routes patched.${COLOR_NC}"
fi

# 6. Finalizing
echo -e "\n${COLOR_BLUE}--- Finalizing ---${COLOR_NC}"
cd "$PANEL_PATH"
php artisan view:clear
php artisan config:clear
chown -R www-data:www-data "$PANEL_PATH"

echo -e "\n${COLOR_GREEN}Stratus Panel Integration Complete!${COLOR_NC}"
echo "You can now find the Stratus tab in your Pterodactyl Admin sidebar."

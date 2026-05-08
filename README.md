# Stratus ☁️
Minecraft Network Orchestrator using Pterodactyl Panel/Wings.

## 📖 What is Stratus?
Stratus is an advanced, high-performance orchestration layer designed specifically for high-traffic Minecraft networks. It bridges the gap between manual server management and a fully automated, elastic cloud infrastructure. 

By leveraging the **Pterodactyl Panel API**, Stratus automatically provisions, monitors, and scales server instances based on real-time player demand. It eliminates the need for manual "warmup" of servers and ensures that your network always has the perfect amount of capacity—reducing hosting costs while maintaining a seamless player experience.

### Key Features:
- **Real-Time Autoscaling**: Automatically creates and deletes server instances based on "free slot" targets.
- **Server Groups**: Organize your network into logical clusters (e.g., Skywars, Bedwars, Lobbies) with individual scaling constraints.
- **Immutable Versioning & Templates**: Define server blueprints with granular resource limits and deploy rolling updates across the network.
- **Fast Allocation**: Redis-backed matchmaking allows proxies to find and reserve servers in sub-milliseconds.
- **Pterodactyl Native**: Integrates directly into your existing Pterodactyl Admin Panel.
- **Watchdog Recovery**: Automatically identifies and terminates stuck or unresponsive instances.

## 👥 Contributors
- **SlamTheHam** - Lead Architect & Developer
- **Pterodactyl Team** - Core Panel & API Infrastructure

---

## 🚀 Installation Guide

### 1. Prerequisites
- **Java 17+** (JDK)
- **MariaDB** (Database)
- **Redis** (Fast Caching & Events)
- **Existing Pterodactyl Panel** installation.

---

### 2. Orchestrator Setup (Backend)
The orchestrator is the "brain" that monitors traffic and provisions servers.

#### 📦 Automatic Installation (Recommended)
1.  **Clone and Setup**:
    ```bash
    git clone https://github.com/SlamStudios/Stratus.git
    cd Stratus
    bash install_orchestrator.sh
    ```
2.  **Run**: `./gradlew run`

#### 🛠️ Manual Installation
1.  **Environment Setup**: Create a `.env` file in the root directory:
    ```env
    STRATUS_TOKEN=your-secret-token
    DATABASE_HOST=localhost
    DATABASE_PORT=3306
    DATABASE_NAME=stratus
    DATABASE_USER=stratus
    DATABASE_PASSWORD=your-password
    REDIS_HOST=localhost
    REDIS_PORT=6379
    PTERODACTYL_URL=https://panel.example.com
    PTERODACTYL_API_KEY=your-application-api-key
    PTERODACTYL_OWNER_ID=1
    STRATUS_URL=http://your-orchestrator-ip:8080
    ```
2.  **Run**: `./gradlew run`

---

### 3. Panel Integration (Frontend)
Adds the "Stratus" tab to your Pterodactyl Admin area.

#### 📦 Automatic Installation (Recommended)
1.  **Run Installer**:
    ```bash
    bash install_panel.sh
    ```
    *This script must be run on the server where Pterodactyl is installed.*

#### 🛠️ Manual Installation
1.  **Copy Files**: Move the following directories from `Pterodactyl/panel/` to your panel root (`/var/www/pterodactyl`):
    - `app/Services/Stratus/`
    - `app/Http/Controllers/Admin/Stratus/`
    - `config/stratus.php`
    - `resources/views/admin/stratus/`
2.  **Patch Routes**: Manually add the Stratus route group to `routes/admin.php`.
3.  **Update .env**: Add `STRATUS_URL` and `STRATUS_TOKEN` to your panel's `.env`.
4.  **Clear Cache**: `php artisan view:clear && php artisan config:clear`

---

### 4. Developer SDK Integration
Stratus provides a lightweight SDK (`stratus-sdk`) that you can shade into your game plugins (Spigot, Velocity, Bungee) to handle communication with the orchestrator.

#### Gradle Implementation:
```kotlin
dependencies {
    implementation("com.slamstudios.stratus:stratus-sdk:1.0-SNAPSHOT")
}
```

#### Usage:
```kotlin
// Automatically detects environment variables (STRATUS_URL, etc.)
val api = StratusAPI.get()

// Report state change
api.updateState("READY")

// Periodic heartbeat
api.heartbeat(playerCount, "map:de_dust2")
```

## 🛠️ Components
- **Orchestrator**: Kotlin/Ktor service for background logic.
- **Panel**: PHP/Laravel additions for Pterodactyl.
- **SDK**: A standalone library JAR for game server integration.
- **Fast Allocation**: Redis-backed matchmaking system.
- **Watchdog**: Automatic recovery of stuck servers.

---
© 2026 Slam Studios. All rights reserved.

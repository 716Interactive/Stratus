plugins {
    kotlin("jvm")
    kotlin("kapt")
}

allprojects {
    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
        maven("https://hub.spigotmc.org/nexus/content/repositories/public/")
        maven("https://oss.sonatype.org/content/groups/public/")
        maven("https://oss.sonatype.org/content/repositories/snapshots")
    }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "org.jetbrains.kotlin.kapt")
    
    kotlin {
        jvmToolchain(17)
    }
    
    dependencies {
        implementation(project(":stratus-sdk"))
        if (name != "common") {
            implementation(project(":stratus-plugin:common"))
        }

        when (name) {
            "spigot" -> {
                compileOnly("org.spigotmc:spigot-api:1.20.1-R0.1-SNAPSHOT")
            }
            "velocity" -> {
                compileOnly("com.velocitypowered:velocity-api:3.2.0-SNAPSHOT")
                kapt("com.velocitypowered:velocity-api:3.2.0-SNAPSHOT")
            }
            "bungeecord" -> {
                compileOnly("net.md-5:bungeecord-api:1.20-R0.1-SNAPSHOT")
            }
        }

        implementation("redis.clients:jedis:5.1.0")
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")
        implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    }
}

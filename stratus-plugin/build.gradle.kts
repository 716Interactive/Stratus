plugins {
    kotlin("jvm") version "2.1.0"
}

allprojects {
    repositories {
        mavenCentral()
        maven("https://repo.velocitypowered.com/snapshots/")
        maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
        maven("https://oss.sonatype.org/content/repositories/snapshots")
    }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    
    dependencies {
        implementation(project(":stratus-sdk"))
        implementation("redis.clients:jedis:5.1.0")
    }
}

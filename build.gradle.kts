plugins {
    kotlin("jvm") version "2.1.10"
    kotlin("plugin.serialization") version "2.1.10"
    id("io.ktor.plugin") version "3.0.3"
}

group = "com.slamstudios.stratus"
version = "1.0.0"

application {
    mainClass.set("com.slamstudios.stratus.ApplicationKt")
}

kotlin {
    jvmToolchain(17)
}

repositories {
    mavenCentral()
}

val ktorVersion = "3.0.3"
val exposedVersion = "0.58.0"
val flywayVersion = "11.2.0"
val kotlinxCoroutinesVersion = "1.10.1"

dependencies {
    // ── Ktor Server ─────────────────────────────────────────────────────────
    implementation("io.ktor:ktor-server-core-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-netty-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-auth-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-call-logging-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-default-headers-jvm:$ktorVersion")

    // ── Ktor Client (Pterodactyl integration) ────────────────────────────────
    implementation("io.ktor:ktor-client-core-jvm:$ktorVersion")
    implementation("io.ktor:ktor-client-cio-jvm:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation-jvm:$ktorVersion")
    implementation("io.ktor:ktor-client-logging-jvm:$ktorVersion")

    // ── Exposed ORM ──────────────────────────────────────────────────────────
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-json:$exposedVersion")

    // ── Database ─────────────────────────────────────────────────────────────
    implementation("com.zaxxer:HikariCP:5.1.0")
    implementation("org.mariadb.jdbc:mariadb-java-client:3.4.0")

    // ── Flyway Migrations ────────────────────────────────────────────────────
    implementation("org.flywaydb:flyway-core:$flywayVersion")
    implementation("org.flywaydb:flyway-mysql:$flywayVersion")

    // ── Redis ────────────────────────────────────────────────────────────────
    implementation("redis.clients:jedis:5.1.3")

    // ── Kotlin ───────────────────────────────────────────────────────────────
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinxCoroutinesVersion")

    // ── Logging ──────────────────────────────────────────────────────────────
    implementation("ch.qos.logback:logback-classic:1.5.6")

    // ── Testing ──────────────────────────────────────────────────────────────
    testImplementation("io.ktor:ktor-server-test-host-jvm:$ktorVersion")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:1.9.25")
    testImplementation("com.h2database:h2:2.2.224")
}



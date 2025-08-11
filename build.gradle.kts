plugins {
    kotlin("jvm") version "2.2.20-Beta2"
    id("com.gradleup.shadow") version "8.3.0"
    id("xyz.jpenilla.run-paper") version "2.3.1"
}

group = "me.andrew"
version = "1.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/") {
        name = "papermc-repo"
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    
    // Discord JDA
    implementation("net.dv8tion:JDA:5.1.1")
    
    // YAML Configuration
    implementation("org.yaml:snakeyaml:2.2")
    
    // JSON for Mojang API
    implementation("com.google.code.gson:gson:2.10.1")
    
    // HTTP Client for API requests
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
}

tasks {
    runServer {
        // Configure the Minecraft version for our task.
        // This is the only required configuration besides applying the plugin.
        // Your plugin's jar (or shadowJar if present) will be used automatically.
        minecraftVersion("1.21")
    }
}

val targetJavaVersion = 21
kotlin {
    jvmToolchain(targetJavaVersion)
}

// Build the shaded (fat) jar as the main artifact so servers load the correct jar
tasks.shadowJar {
    // Replace the default -all classifier; produce MaxyGames-LinkBot-1.0.jar as shaded jar
    archiveClassifier.set("")
    // If you want a smaller jar you can enable minimize(), but it may break reflective libs
    // minimize()
}

// Disable the plain (thin) jar to avoid confusion
tasks.named<Jar>("jar") {
    enabled = false
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

tasks.processResources {
    val props = mapOf("version" to version)
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand(props)
    }
}

// core-sdk build.gradle.kts
plugins {
    `java-library`
    kotlin("jvm") version "2.3.0" // Kotlin version to use
}

// Configuração explícita do Java 21
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }

}

dependencies {
    implementation("ch.qos.logback:logback-classic:1.5.18")

    api("com.fasterxml.jackson.core:jackson-core:2.19.1")
    api("com.fasterxml.jackson.core:jackson-databind:2.19.1")
    api("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.19.1")
    api("com.fasterxml.jackson.module:jackson-module-parameter-names:2.19.1")
    api("com.github.luben:zstd-jni:1.5.7-3")

    api("org.mongodb:mongodb-driver-sync:5.5.0")
    api("redis.clients:jedis:6.0.0")

    api("net.kyori:adventure-api:4.22.0")
    api("net.kyori:adventure-text-serializer-gson:4.22.0")

    api("com.github.ben-manes.caffeine:caffeine:3.2.1")

    implementation("com.google.code.gson:gson:2.11.0")
    implementation("io.github.cdimascio:dotenv-java:3.2.0")
    implementation("it.unimi.dsi:fastutil:8.5.8")

    compileOnly("com.google.guava:guava:33.3.1-jre")

    compileOnly("net.luckperms:api:5.4")
}

tasks.withType<JavaCompile> {
    options.release.set(21)
    options.encoding = "UTF-8"
}

// Configuração para resolver problemas de módulos
tasks.withType<Jar> {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

plugins {
  `java-library`
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

  api("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.19.1")
  api("com.fasterxml.jackson.module:jackson-module-parameter-names:2.19.1")
  api("com.github.luben:zstd-jni:1.5.7-3")

  api("org.mongodb:mongodb-driver-sync:5.5.0")
  api("redis.clients:jedis:6.0.0")

  api("com.github.ben-manes.caffeine:caffeine:3.2.1")

  implementation("io.github.cdimascio:dotenv-java:3.2.0")
  implementation("it.unimi.dsi:fastutil:8.5.8")
}

tasks.withType<JavaCompile> {
  options.release.set(21)
  options.encoding = "UTF-8"
}

// Configuração para resolver problemas de módulos
tasks.withType<Jar> {
  duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
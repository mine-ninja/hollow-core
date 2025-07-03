plugins {
  `java-library`
}


dependencies {
  api("ch.qos.logback:logback-classic:1.4.8")
  api("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.15.3")
  api("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.15.3")
  api("com.fasterxml.jackson.module:jackson-module-parameter-names:2.19.1")
  api("com.github.luben:zstd-jni:1.5.7-3")

  api("org.mongodb:mongodb-driver-sync:5.5.0")
  api("redis.clients:jedis:6.0.0")

  api("com.github.ben-manes.caffeine:caffeine:3.2.1")


  implementation("io.github.cdimascio:dotenv-java:3.2.0")
  implementation("it.unimi.dsi:fastutil:8.5.8")
}
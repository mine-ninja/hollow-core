plugins {
  `java-library`
}


dependencies {
  api("ch.qos.logback:logback-classic:1.4.8")


  api("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.19.1")
  api("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.19.1")

  api("org.mongodb:mongodb-driver-sync:5.5.0")
  api("io.lettuce:lettuce-core:6.6.0.RELEASE")
}
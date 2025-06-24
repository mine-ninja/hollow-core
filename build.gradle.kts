plugins {
  java
  id("maven-publish")
}

allprojects {
  group = "net.warcane.core"
  version = "0.1.7"
}

subprojects {
  apply(plugin = "java-library")
  apply(plugin = "maven-publish")

  java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }

  val artifactIdValue = "lugin-${project.name}"
  val repositoryUser = (project.findProperty("luginUser") ?: "lugin") as String
  val repositoryPassword = (project.findProperty("luginPassword") ?: "lugin") as String


  repositories {
    mavenCentral()
  }

  dependencies {
    compileOnly("org.projectlombok:lombok:1.18.38")
    annotationProcessor("org.projectlombok:lombok:1.18.38")

    compileOnly("org.jetbrains:annotations:24.0.0")
    annotationProcessor("org.jetbrains:annotations:24.0.0")
  }

  publishing {
    repositories {
      maven {
        name = "lugin"
        url = uri("http://repo.luginbr.net/releases")
        credentials {
          username = repositoryUser
          password = repositoryPassword
        }
        isAllowInsecureProtocol = true
      }
    }
    publications {
      create<MavenPublication>("mavenJava") {
        from(components["java"])
        artifactId = artifactIdValue
      }
    }
  }
}
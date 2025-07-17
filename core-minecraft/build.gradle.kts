plugins {
  `java-library`
  // shadow
  id("com.gradleup.shadow") version ("9.0.0-rc1")
  id("io.papermc.paperweight.userdev") version ("1.7.1")

}

repositories {
  mavenCentral()
  maven { url = uri("https://jitpack.io") }
  maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots") }
  maven { url = uri("https://repo.dmulloy2.net/repository/public/") }
  maven { url = uri("https://repo.codemc.io/repository/maven-releases/") }

  maven {
    name = "papermc"
    url = uri("https://repo.papermc.io/repository/maven-public/")
  }

  maven {
    url = uri("https://oss.sonatype.org/content/groups/public/")
  }

  val repositoryUser = (project.findProperty("luginUser") ?: "lugin") as String
  val repositoryPassword = (project.findProperty("luginPassword") ?: "lugin") as String

  maven {
    name = "reposiliteRepositoryReleases"
    url = uri("http://node.luginbr.net:19133/private")
    isAllowInsecureProtocol = true
    credentials {
      username = repositoryUser
      password = repositoryPassword
    }
  }
}

dependencies {
  api(project(":core-sdk"))
  api("fr.mrmicky:fastboard:2.1.5")

  compileOnlyApi("com.github.retrooper:packetevents-spigot:2.9.1")

  paperweight.paperDevBundle("1.21-R0.1-SNAPSHOT")
}

tasks.shadowJar {
  archiveVersion = ""
  mergeServiceFiles()
  minimize()
}
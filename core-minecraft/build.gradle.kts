plugins {
  `java-library`
  // shadow
  id("com.github.johnrengelman.shadow") version ("7.1.1")
}

repositories {
  maven { url = uri("https://repo.dmulloy2.net/repository/public/") }
  maven { url = uri("https://repo.codemc.io/repository/maven-releases/") }

  maven {
    name = "reposiliteRepositoryReleases"
    url = uri("http://node.luginbr.net:19133/releases")
    isAllowInsecureProtocol = true
  }
}

dependencies {
  api(project(":core-sdk"))

  api("fr.mrmicky:fastboard:2.1.5")

  compileOnly("net.warcane.severum:severum-server:1.8") {
    exclude(group = "net.md-5", module = "bungeecord-chat")
  }
}

tasks.shadowJar {
  mergeServiceFiles()
  minimize()
}
plugins {
  `java-library`
  // shadow
  id("com.github.johnrengelman.shadow") version ("7.1.1")
}

repositories {
  maven { url = uri("https://repo.dmulloy2.net/repository/public/") }
  maven { url = uri("https://repo.codemc.io/repository/maven-releases/") }

  maven {
    name = "lugin"
    url = uri("http://repo.luginbr.net/releases")
    isAllowInsecureProtocol = true
  }
}

dependencies {
  api(project(":core-sdk"))
  compileOnly("net.warcane.severum:severum-server:1.8") {
    exclude(group = "net.md-5", module = "bungeecord-chat")
  }
}

tasks.shadowJar {
  mergeServiceFiles()
  minimize()
}
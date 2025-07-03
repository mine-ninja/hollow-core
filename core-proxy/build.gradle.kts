plugins {
  `java-library`
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
  compileOnly("net.md-5:bungeecord-api:1.21-R0.3")
}
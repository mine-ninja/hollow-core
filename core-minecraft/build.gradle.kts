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
  api("io.github.juliarn:npc-lib-bukkit:3.0.0-beta12") {
    // remove io.papermc:paperlib:1.0.8 to avoid conflicts with Severum
    exclude(group = "io.papermc", module = "paperlib")
  }

  compileOnlyApi("net.warcane.severum:severum-api:1.8") {
    exclude(group = "net.md-5", module = "bungeecord-chat")
  }

  compileOnlyApi("net.warcane.severum:severum-server:1.8")
  compileOnlyApi("com.comphenix.protocol:ProtocolLib:5.3.0")
  compileOnlyApi("com.github.retrooper:packetevents-spigot:2.7.0")
}
plugins {
    `java-library`
    id("com.gradleup.shadow") version ("9.0.0-rc1")
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.19"
    id("xyz.jpenilla.run-paper") version "2.3.1"
}

base {
    archivesName.set("core-minigames")
}

repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
    maven { url = uri("https://oss.sonatype.org/content/groups/public/") }
    maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots") }
    maven { url = uri("https://repo.dmulloy2.net/repository/public/") }
    maven { url = uri("https://repo.codemc.io/repository/maven-releases/") }
    maven { url = uri("https://repo.extendedclip.com/releases/") }
    maven {
        name = "papermc"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }

    val repositoryUser = (project.findProperty("luginUser") ?: "lugin") as String
    val repositoryPassword = (project.findProperty("luginPassword") ?: "lugin") as String

    maven {
        name = "reposiliteRepositoryReleases"
        url = uri("https://repo.luginbr.net/private")
        isAllowInsecureProtocol = true
        credentials {
            username = repositoryUser
            password = repositoryPassword
        }
    }
}

dependencies {
    compileOnly(project(":core-minecraft"))
    compileOnly(project(":core-sdk"))
    compileOnly("com.github.ben-manes.caffeine:caffeine:3.1.8")

    implementation("net.kyori:adventure-platform-bukkit:4.4.1")
    implementation("net.kyori:adventure-text-minimessage:4.24.0")

    paperweight.paperDevBundle("1.21-R0.1-SNAPSHOT")
}

tasks.runServer {
    minecraftVersion("1.21.1")
    systemProperty("com.mojang.eula.agree", "true")

    downloadPlugins {
        modrinth("packetevents", "2.9.5+spigot")
        modrinth("placeholderapi", "2.11.6")
        modrinth("tab-was-taken", "5.2.5")
        url("https://github.com/MilkBowl/Vault/releases/download/1.7.3/Vault.jar")
        url("https://github.com/dmulloy2/ProtocolLib/releases/download/5.4.0/ProtocolLib.jar")
    }
}

tasks.withType(xyz.jpenilla.runtask.task.AbstractRun::class) {
    javaLauncher = javaToolchains.launcherFor {
        vendor = JvmVendorSpec.JETBRAINS
        languageVersion = JavaLanguageVersion.of(21)
    }
    jvmArgs("-XX:+AllowEnhancedClassRedefinition")
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21

    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks.shadowJar {
    archiveBaseName.set("core-minigames")

    archiveClassifier.set("")
    archiveVersion.set("")
    mergeServiceFiles()
    minimize()
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

tasks.processResources {
    val props = mapOf("version" to project.version)
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand(props)
    }
}

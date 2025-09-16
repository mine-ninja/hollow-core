plugins {
    `java-library`
    id("com.gradleup.shadow") version ("9.0.0-rc1")
    id("io.papermc.paperweight.userdev") version ("1.7.1")
    id("xyz.jpenilla.run-paper") version "2.3.1"
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
        url = uri("http://node.luginbr.net:19133/private")
        isAllowInsecureProtocol = true
        credentials {
            username = repositoryUser
            password = repositoryPassword
        }
    }
}

dependencies {
    api(project(":core-minecraft"))
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

tasks.shadowJar {
    archiveBaseName.set("core-minigames")
    mergeServiceFiles()
    minimize()
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

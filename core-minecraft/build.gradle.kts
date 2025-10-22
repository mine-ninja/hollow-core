import org.gradle.kotlin.dsl.invoke

plugins {
    `java-library`
    id("com.gradleup.shadow") version ("9.0.0-rc1")
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.19"
    id("xyz.jpenilla.run-paper") version "2.3.1"
}

base {
    archivesName.set("core-minecraft")
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
    paperweight.paperDevBundle("1.21.4-R0.1-SNAPSHOT")
    api(project(":core-sdk"))
    api("fr.mrmicky:fastboard:2.1.5")

    implementation("net.kyori:adventure-platform-bukkit:4.4.1")
    implementation("net.kyori:adventure-text-minimessage:4.24.0")

    implementation("com.squareup.okhttp3:okhttp:5.1.0")
    implementation("de.tr7zw:item-nbt-api:2.15.3")
    implementation("io.socket:socket.io-client:2.1.1") {
        exclude("org.json", "json")
        exclude("com.squareup.okhttp3", "okhttp")
    }

    compileOnlyApi("com.github.retrooper:packetevents-spigot:2.9.5")
    compileOnly("com.github.MilkBowl:VaultAPI:1.7") { isTransitive = false }
    compileOnly("me.clip:placeholderapi:2.11.6") { isTransitive = false }
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21

    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks.runServer {
    minecraftVersion("1.21.4")
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
    relocate("okhttp3", "net.warcane.lugin.core.libs.okhttp3")

    archiveClassifier.set("")
    archiveVersion.set("")
    mergeServiceFiles()
    minimize()
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

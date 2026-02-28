

plugins {
    `java-library`
    id("com.gradleup.shadow") version ("9.0.0-rc1")
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.19"
    id("xyz.jpenilla.run-paper") version "2.3.1"
}

base {
    archivesName.set("leaderboard")
}

repositories {
    mavenCentral()
    maven("https://maven.pvphub.me/tofaa")
    maven { url = uri("https://repo.codemc.io/repository/maven-releases/") }
    maven { url = uri("https://jitpack.io") }
    maven { url = uri("https://oss.sonatype.org/content/groups/public/") }
    maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots") }
    maven { url = uri("https://repo.dmulloy2.net/repository/public/") }
    maven { url = uri("https://repo.extendedclip.com/releases/") }
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.tcoded.com/releases")
}

dependencies {
    compileOnly(project(":core-minecraft"))
    compileOnly("me.clip:placeholderapi:2.11.6")
    implementation("io.github.tofaa2:spigot:3.1.0-SNAPSHOT")
    paperweight.paperDevBundle("1.21.4-R0.1-SNAPSHOT")
}

java {
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
    archiveClassifier.set("")
    archiveVersion.set("")
    mergeServiceFiles()
    minimize()
}

tasks.build {
    dependsOn(tasks.shadowJar)
}


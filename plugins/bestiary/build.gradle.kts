plugins {
    `java-library`
    id("com.gradleup.shadow") version ("9.0.0-rc1")
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.19"
    id("xyz.jpenilla.run-paper") version "2.3.1"
}

base {
    archivesName.set("bestiary")
}

repositories {
    maven("https://maven.pvphub.me/tofaa")
    maven { url = uri("https://repo.codemc.io/repository/maven-releases/") }
    maven { url = uri("https://oss.sonatype.org/content/groups/public/") }
    maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots") }
    maven { url = uri("https://repo.dmulloy2.net/repository/public/") }
    maven { url = uri("https://repo.extendedclip.com/releases/") }
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://maven.enginehub.org/repo/")
    maven("https://repo.tcoded.com/releases")
    maven("https://repo.papermc.io/repository/maven-public/")
    mavenCentral()
    maven { url = uri("https://jitpack.io") }

}


dependencies {
    api("io.github.tofaa2:spigot:3.1.0-SNAPSHOT")
    compileOnly(project(":core-minecraft"))

    // use the "leaf-api.jar" inside the core-minecraft/libs/ directory as a compileOnly dependency

    paperweight.paperDevBundle("1.21.4-R0.1-SNAPSHOT")
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
    relocate("okhttp3", "io.github.minehollow.core.libs.okhttp3")

    // relocate exp4j so we can update it without breaking plugins that depend on older versions
    relocate("net.objecthunter.exp4j", "io.github.minehollow.core.libs.exp4j")

    archiveClassifier.set("")
    archiveVersion.set("")

    mergeServiceFiles()
    minimize()
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

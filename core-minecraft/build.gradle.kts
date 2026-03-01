// core-minecraft build.gradle.kts
plugins {
    id("com.gradleup.shadow") version "9.0.0-rc1"
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.19"
}

base {
    archivesName.set("core-minecraft")
}

dependencies {
    paperweight.paperDevBundle("1.21.4-R0.1-SNAPSHOT")

    api(project(":core-sdk"))
    api("fr.mrmicky:fastboard:2.1.5")

    implementation("net.kyori:adventure-platform-bukkit:4.4.1") {
        exclude(group = "com.fasterxml.jackson.core")
        exclude(group = "com.fasterxml.jackson.datatype")
    }
    implementation("net.kyori:adventure-text-minimessage:4.24.0") {
        exclude(group = "com.fasterxml.jackson.core")
        exclude(group = "com.fasterxml.jackson.datatype")
    }
    implementation("com.squareup.okhttp3:okhttp:5.1.0") {
        exclude(group = "com.fasterxml.jackson.core")
        exclude(group = "com.fasterxml.jackson.datatype")
    }
    implementation("de.tr7zw:item-nbt-api:2.15.3") {
        exclude(group = "com.fasterxml.jackson.core")
        exclude(group = "com.fasterxml.jackson.datatype")
    }
    implementation("io.socket:socket.io-client:2.1.1") {
        exclude(group = "org.json", module = "json")
        exclude(group = "com.squareup.okhttp3", module = "okhttp")
        exclude(group = "com.fasterxml.jackson.core")
        exclude(group = "com.fasterxml.jackson.datatype")
    }

    compileOnlyApi("com.github.retrooper:packetevents-spigot:2.11.1")
    compileOnly("com.github.MilkBowl:VaultAPI:1.7") { isTransitive = false }
    compileOnly("me.clip:placeholderapi:2.11.6") { isTransitive = false }
}

tasks.shadowJar {
    relocate("okhttp3", "io.github.minehollow.core.libs.okhttp3")
    relocate("com.fasterxml.jackson", "io.github.minehollow.core.libs.jackson")


    mergeServiceFiles()
    minimize()
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

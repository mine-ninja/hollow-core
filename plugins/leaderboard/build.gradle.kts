plugins {
    id("com.gradleup.shadow") version "9.0.0-rc1"
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.19"
}

base {
    archivesName.set("leaderboard")
}

repositories {
    maven("https://maven.pvphub.me/tofaa")
}

dependencies {
    compileOnly(project(":core-minecraft"))
    compileOnly("me.clip:placeholderapi:2.11.6")
    paperweight.paperDevBundle("1.21.4-R0.1-SNAPSHOT")
    implementation("io.github.tofaa2:spigot:3.1.0-SNAPSHOT")
}

tasks.shadowJar {
    mergeServiceFiles()
    minimize()
}

tasks.build {
    dependsOn(tasks.shadowJar)
}


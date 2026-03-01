plugins {
    id("com.gradleup.shadow") version "9.0.0-rc1"
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.19"
}

base {
    archivesName.set("ranks")
}

repositories {
    maven("https://maven.enginehub.org/repo/")
}

dependencies {
    compileOnly(project(":core-minecraft"))
    paperweight.paperDevBundle("1.21.4-R0.1-SNAPSHOT")

    implementation("net.objecthunter:exp4j:0.4.8")

    implementation(platform("com.intellectualsites.bom:bom-newest:1.55"))
    compileOnly("com.fastasyncworldedit:FastAsyncWorldEdit-Core")
    compileOnly("com.fastasyncworldedit:FastAsyncWorldEdit-Bukkit") { isTransitive = false }
}

tasks.shadowJar {
    relocate("net.objecthunter.exp4j", "io.github.minehollow.core.libs.exp4j")
    mergeServiceFiles()
    minimize()
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

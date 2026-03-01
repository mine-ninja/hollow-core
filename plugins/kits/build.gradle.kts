plugins {
    id("com.gradleup.shadow") version "9.0.0-rc1"
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.19"
}

base {
    archivesName.set("kits")
}

dependencies {
    compileOnly(project(":core-minecraft"))
    paperweight.paperDevBundle("1.21.4-R0.1-SNAPSHOT")
}


tasks.shadowJar {
    mergeServiceFiles()
    minimize()
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

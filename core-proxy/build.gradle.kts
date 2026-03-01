// core-proxy build.gradle.kts
plugins {
    id("com.gradleup.shadow") version "9.0.0-rc1"
}

dependencies {
    api(project(":core-sdk"))
    compileOnly("com.velocitypowered:velocity-api:3.4.0-SNAPSHOT")
    annotationProcessor("com.velocitypowered:velocity-api:3.4.0-SNAPSHOT")
}

tasks.shadowJar {
    mergeServiceFiles()
    minimize()
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

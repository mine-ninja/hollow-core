/*
 * Copyright (c) 2024-2025.
 *
 * Authored by the Nerdzone Team: https://github.com/orgs/nerdzonegg
 */

extra["usePaperweight"] = true

repositories {
    mavenCentral()
    maven("https://maven.enginehub.org/repo/")
}

dependencies {
    implementation(project(":plugins:mining:common:api"))
    implementation(project(":plugins:mining:common:shared"))
    implementation(project(":plugins:mining:common:services"))
    implementation(project(":plugins:mining:common:eventbus"))
    implementation(project(":plugins:mining:common:configuration"))
    implementation(project(":plugins:mining:features:themes"))

    compileOnly(project(":plugins:economy:api"))
    compileOnly(project(":plugins:economy:shared"))

    compileOnly(project(":sdk:core"))

    //compileOnly(libs.stonegenie)
    compileOnly(libs.protocolLib)
    compileOnly(libs.packetevents)
    compileOnly(libs.entitylib)

    compileOnlyApi("com.fastasyncworldedit:FastAsyncWorldEdit-Bukkit:2.14.0") {
        exclude("io.papermc.paper", "paper-api")
    }
}

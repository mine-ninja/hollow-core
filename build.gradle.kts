// root build.gradle.kts
plugins {
    java
}

allprojects {
    group = "io.github.minehollow"
    version = "0.1.1-alpha"
}

subprojects {
    apply(plugin = "java-library")

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }

    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://repo.codemc.io/repository/maven-releases/")
        maven("https://jitpack.io")
        maven("https://oss.sonatype.org/content/groups/public/")
        maven("https://oss.sonatype.org/content/repositories/snapshots")
        maven("https://repo.dmulloy2.net/repository/public/")
        maven("https://repo.extendedclip.com/releases/")
        maven("https://repo.tcoded.com/releases")
    }

    dependencies {
        compileOnly("org.projectlombok:lombok:1.18.38")
        annotationProcessor("org.projectlombok:lombok:1.18.38")
        compileOnly("org.jetbrains:annotations:26.0.0")
        annotationProcessor("org.jetbrains:annotations:26.0.0")
    }

    tasks.withType<JavaCompile> {
        options.release.set(21)
        options.encoding = "UTF-8"
        options.compilerArgs.addAll(listOf("-parameters"))
    }

    tasks.withType<Jar> {
        archiveClassifier.set("")
        archiveVersion.set("")
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }

    // Force consistent Jackson versions across all modules
    configurations.all {
        resolutionStrategy {
            force("com.fasterxml.jackson.core:jackson-core:2.19.1")
            force("com.fasterxml.jackson.core:jackson-databind:2.19.1")
            force("com.fasterxml.jackson.core:jackson-annotations:2.19.1")
            force("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.19.1")
            force("com.fasterxml.jackson.module:jackson-module-parameter-names:2.19.1")
        }
    }

    // Output all shadowJar artifacts into root/dist/
    afterEvaluate {
        tasks.findByName("shadowJar")?.let { shadow ->
            (shadow as AbstractArchiveTask).destinationDirectory.set(rootProject.layout.projectDirectory.dir("dist"))
            shadow.archiveBaseName.set(project.name)
            shadow.archiveClassifier.set("")
        }
    }
}

tasks.register<Delete>("deleteDist") {
    delete("dist")
}

tasks.named("clean") {
    dependsOn("deleteDist")
}

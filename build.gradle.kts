plugins {
    java
    id("maven-publish")
}

allprojects {
    group = "io.github.minehollow"
    version = "0.1.1-alpha"
}

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "maven-publish")

    val artifactIdValue = "hollow-${project.name}"

    java {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
        withSourcesJar()
    }

    repositories {
        mavenCentral()
    }

    dependencies {
        compileOnly("org.projectlombok:lombok:1.18.38")
        annotationProcessor("org.projectlombok:lombok:1.18.38")

        compileOnly("org.jetbrains:annotations:26.0.0")
        annotationProcessor("org.jetbrains:annotations:26.0.0")
    }

    publishing {

        publications {
            create<MavenPublication>("mavenJava") {
                from(components["java"])
                artifactId = artifactIdValue
            }
        }
    }

    tasks.jar {
        archiveClassifier.set("")
        archiveVersion.set("")
    }

    tasks.named<Jar>("sourcesJar") {
        archiveVersion.set("")
    }

}

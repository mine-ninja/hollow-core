plugins {
    java
    id("maven-publish")
}

allprojects {
    group = "net.warcane.core"
    version = "0.7.11-DEV"
}

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "maven-publish")

    val artifactIdValue = "lugin-${project.name}"
    val repositoryUser = (project.findProperty("luginUser") ?: "lugin") as String
    val repositoryPassword = (project.findProperty("luginPassword") ?: "lugin") as String

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
        repositories {
            maven {
                name = "Private"
                url = uri("http://node.luginbr.net:19133/private")
                credentials {
                    username = repositoryUser
                    password = repositoryPassword
                }
                isAllowInsecureProtocol = true
            }
            maven {
                name = "Snapshots"
                url = uri("http://node.luginbr.net:19133/snapshots")
                credentials {
                    username = repositoryUser
                    password = repositoryPassword
                }
                isAllowInsecureProtocol = true
            }
        }
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

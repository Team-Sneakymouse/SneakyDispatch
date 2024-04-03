plugins {
    java
    id("io.papermc.paperweight.userdev") version "1.5.10"
    id("xyz.jpenilla.run-paper") version "2.2.2"
}

repositories {
    maven {
        url = uri("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    }
    mavenCentral()
}

dependencies {
    paperweight.paperDevBundle("1.20.4-R0.1-SNAPSHOT")
    compileOnly("me.clip:placeholderapi:2.11.5")

    implementation(fileTree("libs") {
        include("*.jar")
    })
}

configure<JavaPluginConvention> {
    sourceSets {
        main {
            resources.srcDir(file("src/resources"))
        }
    }
}

tasks {
    runServer {
        minecraftVersion("1.20.2")
    }
}

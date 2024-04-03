plugins {
	java
	id("org.jetbrains.kotlin.jvm") version "2.0.0-Beta5"
	id("io.papermc.paperweight.userdev") version "1.5.10"
	id("xyz.jpenilla.run-paper") version "2.2.2"
}

repositories {
	maven {
		url = uri("https://plugins.gradle.org/m2/")
	}
	maven {
		url = uri("https://repo.extendedclip.com/content/repositories/placeholderapi/")
	}
	mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.6.0")
	paperweight.paperDevBundle("1.20.4-R0.1-SNAPSHOT")
	compileOnly("me.clip:placeholderapi:2.11.5")
}

configure<JavaPluginConvention> {
	sourceSets {
		main {
			java.srcDir("src/main/kotlin")
			resources.srcDir(file("src/resources"))
		}
	}
}

tasks {
	runServer {
		minecraftVersion("1.20.2")
	}
}

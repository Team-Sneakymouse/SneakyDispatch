plugins {
	java
	id("org.jetbrains.kotlin.jvm") version "2.0.0-Beta5"
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
	maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
	implementation("org.jetbrains.kotlin:kotlin-stdlib:1.6.0")
    compileOnly("io.papermc.paper:paper-api:1.20.6-R0.1-SNAPSHOT")
	compileOnly("me.clip:placeholderapi:2.11.5")
}

tasks.jar {
	manifest {
		attributes["Main-Class"] = "net.sneakydispatch.SneakyDispatch"
	}

	from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
}

configure<JavaPluginConvention> {
	sourceSets {
		main {
			java.srcDir("src/main/kotlin")
			resources.srcDir(file("src/resources"))
		}
	}
}

java {
	toolchain {
		languageVersion.set(JavaLanguageVersion.of(21))
	}
}

tasks {
	runServer {
		minecraftVersion("1.20.6")
	}
}

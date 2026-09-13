import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
	java
	id("org.jetbrains.kotlin.jvm") version "2.3.0"
	id("xyz.jpenilla.run-paper") version "2.3.1"
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
	implementation("org.jetbrains.kotlin:kotlin-stdlib:2.3.0")
	compileOnly("io.papermc.paper:paper-api:26.2.build.+")
	compileOnly("me.clip:placeholderapi:2.11.5")
}

tasks.jar {
	manifest {
		attributes["Main-Class"] = "net.sneakydispatch.SneakyDispatch"
	}

	from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
}

sourceSets {
	main {
		java.srcDir("src/main/kotlin")
		resources.srcDir(file("src/resources"))
	}
}

java {
	toolchain {
		languageVersion.set(JavaLanguageVersion.of(25))
	}
}

kotlin {
	compilerOptions {
		jvmTarget.set(JvmTarget.JVM_25)
	}
}

tasks {
	runServer {
		minecraftVersion("26.2")
	}
}

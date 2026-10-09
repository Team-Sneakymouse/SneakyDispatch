import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
	java
	kotlin("jvm") version "2.3.0"
	id("xyz.jpenilla.run-paper") version "2.3.1"
	`maven-publish`
}

group = "io.github.team-sneakymouse"

version = providers.exec {
	workingDir(rootDir)
	commandLine("git", "show", "-s", "--format=%ct:%h", "--abbrev=12", "HEAD")
}.standardOutput.asText.map { commit ->
	val (timestamp, hash) = commit.trim().split(":", limit = 2)
	val date = DateTimeFormatter.ofPattern("yyyy.MM.dd").withZone(ZoneOffset.UTC)
		.format(Instant.ofEpochSecond(timestamp.toLong()))
	"$date-$hash"
}.get()

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

tasks.processResources {
	inputs.property("version", project.version.toString())
	filesMatching("paper-plugin.yml") {
		expand("version" to project.version.toString())
	}
}

tasks.jar {
	archiveBaseName.set(rootProject.name)
	manifest {
		attributes["Main-Class"] = "net.sneakydispatch.SneakyDispatch"
	}

	from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
	duplicatesStrategy = DuplicatesStrategy.EXCLUDE
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
	withSourcesJar()
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

publishing {
	publications {
		create<MavenPublication>("maven") {
			artifactId = rootProject.name
			from(components["java"])
			pom {
				name.set("SneakyDispatch")
				description.set("Paper plugin for dispatching Paladins on the LoM2 server")
				url.set("https://github.com/Team-Sneakymouse/SneakyDispatch")
				scm {
					url.set("https://github.com/Team-Sneakymouse/SneakyDispatch")
					connection.set("scm:git:https://github.com/Team-Sneakymouse/SneakyDispatch.git")
				}
			}
		}
	}
	repositories {
		maven {
			name = "sneakyrp"
			url = uri("https://maven.sneakyrp.com/releases")
			credentials(PasswordCredentials::class)
			authentication {
				create<org.gradle.authentication.http.BasicAuthentication>("basic")
			}
		}
	}
}

tasks.withType<PublishToMavenRepository>().configureEach {
	dependsOn(tasks.check)
}

plugins {
	kotlin("jvm") version "2.0.21" apply false
	kotlin("plugin.spring") version "2.0.21" apply false
	id("org.springframework.boot") version "3.5.3" apply false
	id("io.spring.dependency-management") version "1.1.7" apply false
	id("org.jlleitschuh.gradle.ktlint") version "12.1.0" apply false
}

// Configurações compartilhadas para todos os módulos
allprojects {
	group = "com.homeassistant"
	version = "0.0.1-SNAPSHOT"
	
	repositories {
		mavenCentral()
	}
}

subprojects {
	apply(plugin = "org.jlleitschuh.gradle.ktlint")
	
	// Configure ktlint for all modules
	configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
		version.set("1.0.1")
		debug.set(false)
		verbose.set(true)
		android.set(false)
		outputToConsole.set(true)
		ignoreFailures.set(false)
		
		filter {
			exclude("**/generated/**")
			include("**/kotlin/**")
		}
	}
}
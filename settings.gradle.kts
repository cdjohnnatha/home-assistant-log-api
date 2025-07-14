plugins {
	id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

rootProject.name = "home-assistant-monorepo"

// Incluindo os módulos do monorepo
include(
	":logs-api"
)

// Configuração opcional para organizar os módulos
project(":logs-api").projectDir = file("logs-api")

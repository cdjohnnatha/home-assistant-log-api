# Home Assistant Monorepo

A monorepo architecture for Home Assistant microservices, built with Spring Boot and Kotlin.

## 🏗️ Architecture Overview

This project is structured as a monorepo containing multiple microservices that can be developed, tested, and deployed independently while sharing common code and dependencies.

```
home-assistant-monorepo/
├── 📁 logs-api/                    (Logs microservice)
│   ├── build.gradle.kts
│   └── src/main/kotlin/com/homeassistant/
│       ├── domain/
│       │   ├── model/
│       │   │   └── EventLog.kt
│       │   └── enum/
│       │       └── EventLogType.kt
│       ├── application/
│       │   └── ProcessEventUseCase.kt
│       ├── interfaces/controller/
│       │   ├── dto/
│       │   │   └── EventLogRequest.kt
│       │   └── EventLogController.kt
│       └── logs_api/
│           └── LogsApiApplication.kt
│
├── 📁 build-logic/                 (Future: custom plugins)
├── build.gradle.kts               (Root configuration)
├── settings.gradle.kts           (Module definitions)
└── gradle.properties            (Global properties)
```

## 🎯 Why Monorepo?

### ✅ **Code Reusability**
- **Domain models**: Each service has its own domain with clear boundaries
- **Common utilities**: Can be shared when needed (future shared module)
- **Consistent interfaces**: Common patterns and structures across services

### ✅ **Dependency Management**
- **Centralized versions**: All dependencies managed in one place
- **Consistent tooling**: Same build tools, testing frameworks, and configurations
- **Easier updates**: Update library versions once for all services

### ✅ **Development Efficiency**
- **Atomic changes**: Refactor across multiple services in a single commit
- **Coordinated deployments**: Easy to coordinate changes between services
- **Simplified setup**: Clone once, build everything

### ✅ **Build Optimization**
- **Incremental builds**: Only modified modules are recompiled
- **Parallel execution**: Multiple services can be built simultaneously
- **Shared cache**: Gradle build cache shared across all modules

## 🏛️ Clean Architecture

Each microservice follows **Clean Architecture** principles, ensuring maintainable, testable, and independent code. This architecture promotes separation of concerns and makes services more resilient to changes.

### 📐 Architecture Layers

```
src/main/kotlin/com/homeassistant/
├── 📁 domain/                     (Enterprise Business Rules)
│   ├── model/                     (Entities)
│   ├── enum/                      (Domain enums)
│   └── repository/                (Repository interfaces)
│
├── 📁 application/                (Application Business Rules)
│   ├── usecase/                   (Use cases/Interactors)
│   ├── service/                   (Application services)
│   └── port/                      (Input/Output ports)
│
├── 📁 infrastructure/             (Frameworks & Drivers)
│   ├── persistence/               (Database adapters)
│   ├── external/                  (External API clients)
│   └── config/                    (Configuration)
│
├── 📁 interfaces/                 (Interface Adapters)
│   ├── controller/                (REST controllers)
│   ├── dto/                       (Data Transfer Objects)
│   └── mapper/                    (DTO mappers)
│
└── 📁 {service_name}/             (Main & Configuration)
    └── Application.kt             (Spring Boot main class)
```

### 🔄 Dependency Flow

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Controllers   │───▶│   Use Cases     │───▶│   Domain Model  │
│   (Interface)   │    │ (Application)   │    │   (Entities)    │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                        │                        ▲
         │                        ▼                        │
         │              ┌─────────────────┐                │
         │              │   Repositories  │                │
         │              │ (Infrastructure)│────────────────┘
         │              └─────────────────┘
         │
         ▼
┌─────────────────┐
│   External APIs │
│ (Infrastructure)│
└─────────────────┘
```

### 🎯 Benefits for Microservices

✅ **Independence**: Each service can evolve independently  
✅ **Testability**: Business logic isolated from frameworks  
✅ **Flexibility**: Easy to swap implementations  
✅ **Maintainability**: Clear separation of concerns  
✅ **Scalability**: Each layer can be scaled independently  

### 📋 Layer Responsibilities

| Layer | Responsibility | Examples |
|-------|----------------|----------|
| **Domain** | Core business logic, entities | `EventLog`, `EventLogType` |
| **Application** | Use cases, business workflows | `ProcessEventUseCase` |
| **Infrastructure** | External concerns (DB, APIs) | `EventLogRepository`, `NotificationClient` |
| **Interface** | Adapters for external access | `EventLogController`, `EventLogRequest` |

### 🏗️ Implementation Example

```kotlin
// Domain Layer (logs-api/)
data class EventLog(
    val source: String,
    val eventType: EventLogType,
    val timestamp: Instant,
    val payload: Map<String, Any>
)

// Application Layer (logs-api/)
@Service
class ProcessEventUseCase(
    private val eventLogRepository: EventLogRepository
) {
    fun execute(event: EventLog): EventLog {
        // Business logic here
        return eventLogRepository.save(event)
    }
}

// Interface Layer (logs-api/)
@RestController
@RequestMapping("/api/v1/events")
class EventLogController(
    private val processEventUseCase: ProcessEventUseCase
) {
    @PostMapping
    fun createEvent(@RequestBody request: EventLogRequest): ResponseEntity<Void> {
        val event = request.toEventLog()
        processEventUseCase.execute(event)
        return ResponseEntity.accepted().build()
    }
}
```

## 🚀 Getting Started

### Prerequisites
- Java 21 or higher
- Kotlin 1.9.25+
- Gradle 8.14.3+

### Building the Project

```bash
# Build entire monorepo
./gradlew build

# Build specific module
./gradlew :logs-api:build

# Build with parallel execution (faster)
./gradlew build --parallel

# Clean and rebuild everything
./gradlew clean build
```

### Running Services

```bash
# Run logs-api service
./gradlew :logs-api:bootRun

# Run with specific profile
./gradlew :logs-api:bootRun --args="--spring.profiles.active=local"

# Run in background
./gradlew :logs-api:bootRun &
```

### Testing

```bash
# Run all tests
./gradlew test

# Run tests for specific module
./gradlew :logs-api:test

# Run tests with coverage
./gradlew test jacocoTestReport
```

## 📦 Adding New Microservices

### Step 1: Create Module Structure

```bash
# Create new service directory
mkdir notification-api

# Create Clean Architecture structure
mkdir -p notification-api/src/main/kotlin/com/homeassistant/notification
mkdir -p notification-api/src/main/kotlin/com/homeassistant/domain/repository
mkdir -p notification-api/src/main/kotlin/com/homeassistant/application/usecase
mkdir -p notification-api/src/main/kotlin/com/homeassistant/application/service
mkdir -p notification-api/src/main/kotlin/com/homeassistant/infrastructure/persistence
mkdir -p notification-api/src/main/kotlin/com/homeassistant/infrastructure/external
mkdir -p notification-api/src/main/kotlin/com/homeassistant/infrastructure/config
mkdir -p notification-api/src/main/kotlin/com/homeassistant/interfaces/controller
mkdir -p notification-api/src/main/kotlin/com/homeassistant/interfaces/dto
mkdir -p notification-api/src/main/kotlin/com/homeassistant/interfaces/mapper

# Create test structure
mkdir -p notification-api/src/test/kotlin/com/homeassistant/application/usecase
mkdir -p notification-api/src/test/kotlin/com/homeassistant/interfaces/controller
```

### Step 2: Configure Build

Create `notification-api/build.gradle.kts`:

```kotlin
plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

dependencies {
    // Basic dependencies (inherited from root)
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    
    // Service-specific dependencies
    implementation("org.springframework.boot:spring-boot-starter-web")
    
    // Optional: dependency on other services (when needed)
    // implementation(project(":logs-api"))
}

tasks.withType<Test> {
    useJUnitPlatform()
}
```

### Step 3: Update Settings

Add to `settings.gradle.kts`:

```kotlin
include(
    ":logs-api",
    ":notification-api"  // Add new module
)

// Configure module locations
project(":logs-api").projectDir = file("logs-api")
project(":notification-api").projectDir = file("notification-api")
```

### Step 4: Create Application Class

Create `notification-api/src/main/kotlin/com/homeassistant/notification/NotificationApplication.kt`:

```kotlin
package com.homeassistant.notification

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class NotificationApplication

fun main(args: Array<String>) {
    runApplication<NotificationApplication>(*args)
}
```

### Step 5: Use Shared Code Following Clean Architecture

**Application Layer - Use Case:**
```kotlin
// notification-api/src/main/kotlin/com/homeassistant/application/usecase/ProcessNotificationUseCase.kt
package com.homeassistant.application.usecase

import com.homeassistant.domain.model.EventLog  // ✅ Own domain models
import com.homeassistant.domain.enum.EventLogType
import com.homeassistant.domain.repository.NotificationRepository
import org.springframework.stereotype.Service

@Service
class ProcessNotificationUseCase(
    private val notificationRepository: NotificationRepository
) {
    
    fun execute(event: EventLog) {
        when (event.eventType) {
            EventLogType.ERROR -> handleErrorEvent(event)
            EventLogType.WARNING -> handleWarningEvent(event)
            else -> handleInfoEvent(event)
        }
    }
    
    private fun handleErrorEvent(event: EventLog) {
        // Business logic for error notifications
        notificationRepository.sendCriticalNotification(event)
    }
    
    private fun handleWarningEvent(event: EventLog) {
        // Business logic for warning notifications
        notificationRepository.sendWarningNotification(event)
    }
    
    private fun handleInfoEvent(event: EventLog) {
        // Business logic for info notifications
        notificationRepository.logEvent(event)
    }
}
```

**Domain Layer - Repository Interface:**
```kotlin
// notification-api/src/main/kotlin/com/homeassistant/domain/repository/NotificationRepository.kt
package com.homeassistant.domain.repository

import com.homeassistant.domain.model.EventLog

interface NotificationRepository {
    fun sendCriticalNotification(event: EventLog)
    fun sendWarningNotification(event: EventLog)
    fun logEvent(event: EventLog)
}
```

**Infrastructure Layer - Repository Implementation:**
```kotlin
// notification-api/src/main/kotlin/com/homeassistant/infrastructure/persistence/NotificationRepositoryImpl.kt
package com.homeassistant.infrastructure.persistence

import com.homeassistant.domain.repository.NotificationRepository
import com.homeassistant.domain.model.EventLog
import org.springframework.stereotype.Repository

@Repository
class NotificationRepositoryImpl : NotificationRepository {
    
    override fun sendCriticalNotification(event: EventLog) {
        // External API call or message queue
        println("CRITICAL: ${event.source} - ${event.payload}")
    }
    
    override fun sendWarningNotification(event: EventLog) {
        // External API call or message queue
        println("WARNING: ${event.source} - ${event.payload}")
    }
    
    override fun logEvent(event: EventLog) {
        // Database or logging system
        println("INFO: ${event.source} - ${event.payload}")
    }
}
```

**Interface Layer - Controller:**
```kotlin
// notification-api/src/main/kotlin/com/homeassistant/interfaces/controller/NotificationController.kt
package com.homeassistant.interfaces.controller

import com.homeassistant.application.usecase.ProcessNotificationUseCase
import com.homeassistant.domain.model.EventLog
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/notifications")
class NotificationController(
    private val processNotificationUseCase: ProcessNotificationUseCase
) {
    
    @PostMapping("/process")
    fun processEvent(@RequestBody event: EventLog): ResponseEntity<Void> {
        processNotificationUseCase.execute(event)
        return ResponseEntity.accepted().build()
    }
}
```

### Step 6: Build and Test

```bash
# Build new service
./gradlew :notification-api:build

# Run new service
./gradlew :notification-api:bootRun

# Test integration
./gradlew build
```

## 🔧 Development Commands

### Build Commands

```bash
# Build everything
./gradlew build

# Build specific modules
./gradlew :logs-api:build :notification-api:build

# Build with parallel execution
./gradlew build --parallel

# Clean build
./gradlew clean build
```

### Development Commands

```bash
# Run specific service
./gradlew :logs-api:bootRun
./gradlew :notification-api:bootRun

# Run with specific profile
./gradlew :logs-api:bootRun --args="--spring.profiles.active=dev"

# Run multiple services (in different terminals)
./gradlew :logs-api:bootRun &
./gradlew :notification-api:bootRun &
```

### Testing Commands

```bash
# Run all tests
./gradlew test

# Run tests for specific module
./gradlew :logs-api:test

# Run tests with detailed output
./gradlew test --info

# Run specific test class
./gradlew :logs-api:test --tests "ProcessEventUseCaseTest"
```

### Maintenance Commands

```bash
# Check for dependency updates
./gradlew dependencyUpdates

# Generate dependency tree
./gradlew dependencies

# Check project structure
./gradlew projects

# Stop all running services
pkill -f "bootRun"
```

## 📊 Module Structure

### `logs-api/` Module
- **Purpose**: Event logging microservice
- **Contains**: Domain models, use cases, controllers, application configuration
- **Dependencies**: Spring Boot Web
- **Architecture**: Clean Architecture with domain, application, infrastructure, and interface layers

### Future Modules
- `notification-api/` - Notification service
- `gateway/` - API Gateway
- `config-server/` - Configuration server
- `discovery-server/` - Service discovery
- `shared/` - Common utilities and contracts (when needed)

## 🌟 Best Practices

### 1. **Module Independence**
- Each service should be independently deployable
- Minimize cross-service dependencies
- Use shared module for common code only

### 2. **Clean Architecture Guidelines**
- **Domain Layer**: No external dependencies, pure business logic
- **Application Layer**: Orchestrates domain objects, contains use cases
- **Infrastructure Layer**: Implements repositories, external APIs
- **Interface Layer**: Controllers, DTOs, adapters for external access

### 3. **Dependency Rules**
- **Inward Dependencies Only**: Outer layers depend on inner layers
- **Stable Dependencies**: Depend on abstractions, not concretions
- **Interface Segregation**: Small, focused interfaces in domain layer

### 4. **Package Structure**
```
com.homeassistant.{service}/
├── domain/           # Entities, value objects, repository interfaces
├── application/      # Use cases, application services
├── infrastructure/   # Database, external APIs, configurations
└── interfaces/      # Controllers, DTOs, mappers
```

### 5. **Dependency Management**
- Keep versions centralized in root `build.gradle.kts`
- Use `api` vs `implementation` appropriately
- Avoid circular dependencies between modules

### 6. **Testing Strategy**
- **Unit Tests**: Domain and application layers (fast, isolated)
- **Integration Tests**: Infrastructure layer (database, external APIs)
- **Controller Tests**: Interface layer (REST endpoints)
- **End-to-End Tests**: Full workflows at the root level

### 7. **Code Organization**
- **Use Cases**: One per business operation
- **Repository Interfaces**: In domain layer
- **Repository Implementations**: In infrastructure layer
- **DTOs**: Only in interface layer, never in domain

### 8. **Error Handling**
- **Domain Exceptions**: Business rule violations
- **Application Exceptions**: Use case failures
- **Infrastructure Exceptions**: Technical failures
- **Controller Advice**: Global exception handling

### 9. **Build Optimization**
- Use `--parallel` for faster builds
- Leverage Gradle build cache
- Keep modules focused and lightweight

## 📚 Additional Resources

### Architecture & Design
- [Clean Architecture by Uncle Bob](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html)
- [Hexagonal Architecture](https://alistair.cockburn.us/hexagonal-architecture/)
- [Domain-Driven Design](https://martinfowler.com/bliki/DomainDrivenDesign.html)

### Spring Boot & Kotlin
- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Kotlin Documentation](https://kotlinlang.org/docs/)
- [Spring Boot with Kotlin](https://spring.io/guides/tutorials/spring-boot-kotlin/)

### Build & Deployment
- [Gradle Multi-Project Builds](https://docs.gradle.org/current/userguide/multi_project_builds.html)
- [Monorepo Best Practices](https://monorepo.tools/)
- [Microservices Patterns](https://microservices.io/patterns/)

### Testing
- [Test-Driven Development](https://martinfowler.com/bliki/TestDrivenDevelopment.html)
- [Testing Strategies for Microservices](https://martinfowler.com/articles/microservice-testing/)
- [Spring Boot Testing](https://spring.io/guides/gs/testing-web/)

## 🤝 Contributing

1. Create a feature branch
2. Add/modify services as needed
3. Update this README if adding new patterns
4. Ensure all tests pass: `./gradlew test`
5. Submit a pull request

## 📄 License

This project is licensed under the MIT License. 
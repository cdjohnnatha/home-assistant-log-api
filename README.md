# Home Assistant Monorepo

A monorepo for Home Assistant microservices using Spring Boot and Kotlin.

## Project Structure

This is a multi-module project where each microservice can be developed and deployed independently while sharing common dependencies.

```
home-assistant-monorepo/
├── logs-api/                      (Event logging service)
│   ├── build.gradle.kts
│   └── src/main/kotlin/com/homeassistant/
│       ├── domain/
│       │   ├── model/
│       │   │   └── EventLog.kt
│       │   └── enum/
│       │       └── EventLogType.kt
│       ├── application/
│       │   └── usecases/
│       │       └── ProcessEventUseCase.kt
│       ├── interfaces/controller/
│       │   ├── dto/
│       │   │   └── EventLogRequest.kt
│       │   └── EventLogController.kt
│       └── logs_api/
│           └── LogsApiApplication.kt
│
├── build-logic/                   (Future: custom plugins)
├── build.gradle.kts              (Root configuration)
├── settings.gradle.kts           (Module definitions)
└── gradle.properties            (Global properties)
```

## Why Monorepo?

**Shared Dependencies**: All services use the same versions of Kotlin, Spring Boot, and other libraries, managed centrally.

**Code Sharing**: Common domain models and utilities can be shared between services when needed.

**Coordinated Changes**: You can refactor across multiple services in a single commit and ensure compatibility.

**Build Efficiency**: Gradle only rebuilds what changed, and modules can be built in parallel.

## Architecture

Each service follows Clean Architecture principles with clear separation between layers:

- **Domain**: Business entities and rules (EventLog, EventLogType)
- **Application**: Use cases and business workflows (ProcessEventUseCase)  
- **Infrastructure**: External concerns like databases and APIs
- **Interface**: REST controllers and DTOs

Dependencies flow inward - outer layers depend on inner layers, never the reverse.

### Dependency Flow

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

### Code Organization

```
src/main/kotlin/com/homeassistant/
├── domain/                        (Business entities and rules)
│   ├── model/                     (EventLog, User, etc.)
│   ├── enum/                      (EventLogType, Status, etc.)
│   └── repository/                (Repository interfaces)
│
├── application/                   (Business workflows)
│   ├── usecases/                  (ProcessEventUseCase, etc.)
│   └── service/                   (Application services)
│
├── infrastructure/               (External integrations)
│   ├── persistence/               (Database implementations)
│   ├── external/                  (External API clients)
│   └── config/                    (Configuration classes)
│
├── interfaces/                    (API layer)
│   ├── controller/                (REST endpoints)
│   ├── dto/                       (Request/Response objects)
│   └── mapper/                    (DTO converters)
│
└── {service_name}/               (Main application)
    └── Application.kt             (Spring Boot entry point)
```

### Example Structure

```kotlin
// Domain - what the business cares about
data class EventLog(
    val source: String,
    val eventType: EventLogType,
    val timestamp: Instant,
    val payload: Map<String, Any>
)

// Application - what the app does with the business logic
@Service
class ProcessEventUseCase {
    fun execute(event: EventLog) {
        // Process the event
        logger.info("Processing event from ${event.source}")
    }
}

// Interface - how external clients interact
@RestController
@RequestMapping("/api/v1/events")
class EventLogController(private val processEventUseCase: ProcessEventUseCase) {
    
    @PostMapping
    fun createEvent(@RequestBody request: EventLogRequest): ResponseEntity<Void> {
        val event = EventLog(request.source, request.eventType, Instant.now(), request.payload)
        processEventUseCase.execute(event)
        return ResponseEntity.accepted().build()
    }
}
```

## Getting Started

### Requirements
- Java 21+
- Gradle 8.14+

### Build and Run

```bash
# Build everything
./gradlew build

# Run the logs API
./gradlew :logs-api:bootRun
```

The API will start on http://localhost:8080

### Testing

```bash
# Run tests
./gradlew test

# Test specific module
./gradlew :logs-api:test
```

## Using the Logs API

Start the service:

```bash
./gradlew :logs-api:bootRun
```

The API runs on http://localhost:8080

### Endpoints

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/v1/events/health` | Health check |
| `POST` | `/api/v1/events` | Create event log |

### Quick Test

```bash
curl http://localhost:8080/api/v1/events/health
```

### Event Types

- `USER_ACTION` - User interactions (lights, switches)
- `SYSTEM_EVENT` - System operations (backups, automation)  
- `ERROR` - Failures and errors
- `WARNING` - Non-critical issues
- `INFO` - General information

### Examples

User action:
```json
{
    "source": "home-assistant",
    "eventType": "USER_ACTION",
    "payload": {
        "action": "light_turned_on",
        "device_id": "living_room_light",
        "user": "john"
    }
}
```

System event:
```json
{
    "source": "automation", 
    "eventType": "SYSTEM_EVENT",
    "payload": {
        "event": "backup_completed",
        "size": "150MB"
    }
}
```

Error:
```json
{
    "source": "sensor_network",
    "eventType": "ERROR", 
    "payload": {
        "error": "sensor_offline",
        "sensor_id": "temp_sensor_01"
    }
}
```

### Using cURL

```bash
# Test health
curl http://localhost:8080/api/v1/events/health

# Create event
curl -X POST http://localhost:8080/api/v1/events \
  -H "Content-Type: application/json" \
  -d '{
    "source": "home-assistant",
    "eventType": "USER_ACTION", 
    "payload": {"action": "light_on"}
  }'
```

### Request Format

```json
{
    "source": "string (required)",
    "eventType": "enum (required)",  
    "timestamp": "ISO8601 (optional)",
    "payload": "object (optional)"
}
```

## Adding New Services

To add a new microservice:

1. Create the module directory (e.g., `notification-api/`)
2. Copy the folder structure from `logs-api/`
3. Add the module to `settings.gradle.kts`
4. Create a `build.gradle.kts` for the new module
5. Build and run: `./gradlew :notification-api:bootRun`

## Development

### Common Commands

```bash
# Build and run
./gradlew build
./gradlew :logs-api:bootRun

# Testing  
./gradlew test
./gradlew :logs-api:test

# Clean build
./gradlew clean build
```

### Current Modules

- `logs-api/` - Event logging service

## License

MIT License 
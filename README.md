# Home Assistant Log API

A microservice for Home Assistant event logging with AWS SNS notifications, built with Spring Boot and Kotlin following Clean Architecture principles.

## 🚀 Features

- **Event Logging**: RESTful API for Home Assistant event processing
- **AWS SNS Integration**: Automatic notifications for processed events
- **Clean Architecture**: Proper separation of concerns and testable design
- **Comprehensive Testing**: 90% test coverage with unit and integration tests
- **Docker Support**: Containerized deployment with health checks
- **Environment Configuration**: Secure AWS credential management

## 📁 Project Structure

```
home-assistant-log-api/
├── logs-api/                      (Main service module)
│   ├── build.gradle.kts
│   └── src/main/kotlin/com/homeassistant/
│       ├── domain/                (Business entities and rules)
│       │   ├── model/             (EventLog)
│       │   ├── enum/              (EventLogType)
│       │   └── port/              (NotificationPublisherPort)
│       ├── application/           (Business workflows)
│       │   └── usecases/          (ProcessEventUseCase, NotificationPublisherUseCase)
│       ├── infra/                 (External integrations)
│       │   ├── aws/               (SNS adapter)
│       │   ├── config/            (AWS configuration)
│       │   └── extensions/        (Logging utilities)
│       ├── presentation/          (API layer)
│       │   ├── controller/        (REST endpoints)
│       │   └── dto/               (Request/Response objects)
│       └── logsapi/               (Main application)
│           └── LogsApiApplication.kt
│
├── docker-compose.yml             (Container orchestration)
├── Dockerfile                     (Container definition)
├── build.gradle.kts              (Root configuration)
└── settings.gradle.kts           (Module definitions)
```

## 🏗️ Architecture

This service follows **Clean Architecture** principles with clear separation between layers:

- **Domain**: Business entities and rules (EventLog, EventLogType, Ports)
- **Application**: Use cases and business workflows (ProcessEventUseCase, NotificationPublisherUseCase)  
- **Infrastructure**: External concerns (AWS SNS, Configuration, Extensions)
- **Presentation**: REST controllers and DTOs

Dependencies flow inward - outer layers depend on inner layers, never the reverse.

### Dependency Flow

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Controllers   │───▶│   Use Cases     │───▶│   Domain Model  │
│ (Presentation)  │    │ (Application)   │    │   (Entities)    │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                        │                        ▲
         │                        ▼                        │
         │              ┌─────────────────┐                │
         │              │   Adapters      │                │
         │              │(Infrastructure) │────────────────┘
         │              └─────────────────┘
         │                        │
         ▼                        ▼
┌─────────────────┐    ┌─────────────────┐
│   External APIs │    │    AWS SNS      │
│ (Infrastructure)│    │ (Infrastructure)│
└─────────────────┘    └─────────────────┘
```

## 🔔 Notifications

The API automatically sends notifications via **AWS SNS** when events are processed, ensuring real-time visibility into Home Assistant activities.

### Notification Flow

1. **Event Received** → API endpoint processes the request
2. **Business Logic** → `ProcessEventUseCase` handles the event
3. **Notification Triggered** → `NotificationPublisherUseCase` formats the message
4. **AWS SNS** → `NotificationPublisherAdapter` sends to configured topic

### Message Format

Notifications are sent with the following structure:

```
New Home Assistant Event:
Source: sensor_temperature
Type: USER_ACTION
Timestamp: 2024-01-15T10:30:00Z
Payload: {"temperature": 23.5, "location": "living_room"}
```

## ⚙️ Configuration

### Environment Variables

| Variable | Description | Required | Example |
|----------|-------------|----------|---------|
| `AWS_REGION` | AWS region for SNS | Yes | `us-east-1` |
| `AWS_ACCESS_KEY` | AWS access key ID | Yes | `AKIAIOSFODNN7EXAMPLE` |
| `AWS_SECRET_KEY` | AWS secret access key | Yes | `wJalrXUtnFEMI/K7MDENG/bPxRfiCY...` |
| `AWS_SNS_TOPIC_ARN` | SNS topic ARN for notifications | Yes | `arn:aws:sns:us-east-1:123456789012:home-assistant-events` |
| `SPRING_PROFILE_ACTIVE` | Spring profile | No | `prod` (default: `dev`) |
| `LOGS_API_PORT` | Server port | No | `8080` (default) |
| `JAVA_OPTS` | JVM options | No | `-Xmx512m -Xms256m` |

### AWS Setup

1. **Create SNS Topic**:
   ```bash
   aws sns create-topic --name home-assistant-events
   ```

2. **Create IAM User** with SNS publish permissions:
   ```json
   {
     "Version": "2012-10-17",
     "Statement": [
       {
         "Effect": "Allow",
         "Action": [
           "sns:Publish"
         ],
         "Resource": "arn:aws:sns:*:*:home-assistant-events"
       }
     ]
   }
   ```

3. **Get Credentials**:
   ```bash
   aws iam create-access-key --user-name home-assistant-api
   ```

## 🏃‍♂️ Getting Started

### Requirements
- **Java 21+**
- **Gradle 8.14+**
- **AWS Account** (for SNS notifications)
- **Docker** (optional, for containerized deployment)

### Local Development

1. **Clone the repository**:
   ```bash
   git clone <repository-url>
   cd home-assistant-log-api
   ```

2. **Set environment variables**:
   ```bash
   export AWS_REGION=us-east-1
   export AWS_ACCESS_KEY=your-access-key
   export AWS_SECRET_KEY=your-secret-key
   export AWS_SNS_TOPIC_ARN=your-topic-arn
   ```

3. **Build and run**:
   ```bash
   # Verify code quality first
   ./gradlew ktlintCheck
   
   # Build and start the application
   ./gradlew build
   ./gradlew :logs-api:bootRun
   ```

4. **Verify health**:
   ```bash
   curl http://localhost:8080/api/v1/events/health
   ```

### First-time Setup

For new contributors to maintain code quality:

```bash
# 1. Clone and navigate to project
git clone <repository-url>
cd home-assistant-log-api

# 2. Verify code style compliance
./gradlew ktlintCheck

# 3. Run tests to ensure everything works
./gradlew test

# 4. Start development server
./gradlew :logs-api:bootRun
```

### Docker Deployment

1. **Create environment file** (`.env`):
   ```bash
   AWS_REGION=us-east-1
   AWS_ACCESS_KEY=your-access-key
   AWS_SECRET_KEY=your-secret-key
   AWS_SNS_TOPIC_ARN=your-topic-arn
   SPRING_PROFILE_ACTIVE=prod
   SERVER_PORT=8080
   JAVA_OPTS=-Xmx512m -Xms256m
   ```

2. **Run with Docker Compose**:
   ```bash
   docker-compose up -d
   ```

3. **Check logs**:
   ```bash
   docker-compose logs -f logs-api
   ```

## 📡 API Reference

### Endpoints

| Method | Path | Description | Request Body |
|--------|------|-------------|--------------|
| `GET` | `/api/v1/events/health` | Health check | None |
| `POST` | `/api/v1/events` | Create event log | `EventLogRequest` |

### Event Types

- **`USER_ACTION`** - User interactions (lights, switches, manual triggers)
- **`SYSTEM_EVENT`** - System operations (backups, automation, schedules)
- **`ERROR`** - Failures and critical errors
- **`WARNING`** - Non-critical issues and warnings
- **`INFO`** - General information and status updates

### Request Format

```json
{
  "source": "string (required) - Event source identifier",
  "eventType": "enum (required) - One of: USER_ACTION, SYSTEM_EVENT, ERROR, WARNING, INFO",
  "timestamp": "string (optional) - ISO8601 timestamp, defaults to current time",
  "payload": "object (optional) - Additional event data"
}
```

### Examples

**User Action**:
```bash
curl -X POST http://localhost:8080/api/v1/events \
  -H "Content-Type: application/json" \
  -d '{
    "source": "home-assistant",
    "eventType": "USER_ACTION",
    "payload": {
      "action": "light_turned_on",
      "device_id": "living_room_light",
      "user": "john"
    }
  }'
```

**System Event**:
```bash
curl -X POST http://localhost:8080/api/v1/events \
  -H "Content-Type: application/json" \
  -d '{
    "source": "automation",
    "eventType": "SYSTEM_EVENT",
    "payload": {
      "event": "backup_completed",
      "size": "150MB",
      "duration": "2m30s"
    }
  }'
```

**Error Event**:
```bash
curl -X POST http://localhost:8080/api/v1/events \
  -H "Content-Type: application/json" \
  -d '{
    "source": "sensor_network",
    "eventType": "ERROR",
    "payload": {
      "error": "sensor_offline",
      "sensor_id": "temp_sensor_01",
      "last_seen": "2024-01-15T09:45:00Z"
    }
  }'
```

## 🧪 Testing

### Running Tests

```bash
# Run all tests
./gradlew test

# Run specific test classes
./gradlew test --tests="*NotificationPublisher*"

# Run tests with coverage
./gradlew test jacocoTestReport
```

### Test Structure

- **Unit Tests**: Business logic and individual components
- **Integration Tests**: Spring Boot context and API endpoints
- **Mock Tests**: AWS SNS integration with mocked services

### Test Coverage

- **~90% Business Logic Coverage**
- **Comprehensive SNS Testing**
- **Error Scenario Validation**
- **Environment Configuration Testing**

## 🛠️ Development

### Code Quality & Linting

This project uses **[ktlint](https://pinterest.github.io/ktlint/)** for Kotlin code formatting and style enforcement.

#### Why ktlint?

- **Consistency**: Enforces uniform code style across the entire codebase
- **Automation**: Can automatically fix most formatting issues
- **Standards**: Based on official Kotlin coding conventions
- **CI/CD Ready**: Integrates seamlessly with build pipelines
- **Zero Configuration**: Works out-of-the-box with sensible defaults
- **Team Collaboration**: Eliminates style debates and ensures clean PRs

#### Linting Commands

```bash
# Check code formatting (non-invasive)
./gradlew ktlintCheck

# Auto-fix formatting issues
./gradlew ktlintFormat

# Build with all quality checks (includes ktlint)
./gradlew build

# Check specific source sets
./gradlew ktlintMainSourceSetCheck    # Main code only
./gradlew ktlintTestSourceSetCheck    # Test code only
```

#### Rules Applied

- **Official Kotlin conventions**: Based on kotlinlang.org guidelines
- **Import organization**: Automatic import sorting and cleanup
- **Indentation**: 4 spaces, no tabs
- **Line length**: 120 characters maximum
- **Trailing spaces**: Automatically removed
- **Final newlines**: Enforced at end of files

#### Quality Gates

The build will **fail** if ktlint check doesn't pass, ensuring:
- ✅ Consistent code formatting across all contributors
- ✅ Clean git diffs (no formatting noise)
- ✅ Professional code quality in production
- ✅ Easier code reviews and maintenance

### Common Commands

```bash
# Clean build
./gradlew clean build

# Run in development mode
./gradlew :logs-api:bootRun

# Package for deployment
./gradlew :logs-api:bootJar

# Run with specific profile
SPRING_PROFILES_ACTIVE=prod ./gradlew :logs-api:bootRun
```

### Adding New Features

1. **Domain First**: Add entities and business rules to `domain/`
2. **Use Cases**: Implement business workflows in `application/usecases/`
3. **Infrastructure**: Add external integrations in `infra/`
4. **Presentation**: Expose via REST API in `presentation/`
5. **Tests**: Comprehensive testing for all layers

## 🐳 Docker

### Building Images

```bash
# Build Docker image
docker build -t home-assistant-log-api .

# Run container
docker run -p 8080:8080 --env-file .env home-assistant-log-api
```

### Health Checks

The Docker container includes health checks:

```yaml
healthcheck:
  test: ["CMD", "curl", "-f", "http://localhost:8080/api/v1/events/health"]
  interval: 30s
  timeout: 10s
  retries: 3
  start_period: 40s
```

## 🔍 Monitoring

### Logs

The application provides structured logging:

```bash
# View application logs
docker-compose logs -f logs-api

# Filter for errors
docker-compose logs logs-api | grep ERROR

# Follow SNS notifications
docker-compose logs logs-api | grep "Notification sent"
```

### Health Check

```bash
# Application health
curl http://localhost:8080/api/v1/events/health

# Docker health status
docker-compose ps
```

## 🚀 Deployment

### Production Considerations

1. **Environment Variables**: Use secure secret management
2. **AWS Credentials**: Consider IAM roles instead of access keys
3. **Monitoring**: Set up CloudWatch logs and metrics
4. **Scaling**: Configure horizontal pod autoscaling if using Kubernetes
5. **Security**: Use HTTPS and proper network policies

### CI/CD Integration

The project enforces quality through automated pipeline with sequential validation stages:

### Pipeline Flow

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│  Code Changes   │───▶│   Compile Code  │───▶│  ktlint Check   │───▶│   Run Tests     │───▶│  Build & Package│
│  (Push/PR)      │    │ (Syntax Check)  │    │ (Code Style)    │    │ (Quality Gate)  │    │   (Artifacts)   │
└─────────────────┘    └─────────────────┘    └─────────────────┘    └─────────────────┘    └─────────────────┘
                                │                        │                        │                        │
                                ▼                        ▼                        ▼                        ▼
                       ┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
                       │ Compile Errors  │    │ Style Issues    │    │ Test Failures   │    │ Build Errors    │
                       │   (Fix & Retry) │    │   (Fix & Retry) │    │   (Fix & Retry) │    │   (Fix & Retry) │
                       └─────────────────┘    └─────────────────┘    └─────────────────┘    └─────────────────┘
```

**Quality Gates:**
- **Compilation**: Code must compile without syntax errors
- **Code Style**: ktlint ensures consistent formatting
- **Test Coverage**: All unit and integration tests must pass  
- **Build Success**: Final artifacts must be generated successfully

## 📚 Additional Resources

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Kotlin Documentation](https://kotlinlang.org/docs/)
- [AWS SNS Documentation](https://docs.aws.amazon.com/sns/)
- [Clean Architecture Guide](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html)
- [ktlint - Kotlin Linter](https://pinterest.github.io/ktlint/)
- [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)

## 📄 License

MIT License 
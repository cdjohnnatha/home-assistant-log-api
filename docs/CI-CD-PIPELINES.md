# CI/CD Pipelines - Monorepo Isolation Strategy

This document explains how our CI/CD pipelines are organized to run efficiently based on changed files.

## 🎯 Pipeline Isolation Strategy

Our monorepo uses **path-based conditional pipelines** to ensure that only relevant workflows run when specific parts of the codebase change.

## 🏗️ Pipeline Architecture

```
.github/workflows/
├── pipeline.yml                 # 🚀 API Pipeline (Spring Boot)
├── sync-home-assistant.yml      # 🏠 Home Assistant Pipeline  
├── infrastructure-pipeline.yml  # 🏗️ Infrastructure Pipeline (Terraform)
└── CI-CD-PIPELINES.md          # 📚 This documentation
```

## 📋 Pipeline Triggers

### 🚀 **API Pipeline** (`pipeline.yml`)

**Triggers when changes affect:**
- `logs-api/**` - Spring Boot application code
- `build.gradle.kts` - Root build configuration
- `settings.gradle.kts` - Gradle settings
- `gradle/**` - Gradle wrapper files
- `gradlew*` - Gradle wrapper scripts
- `Dockerfile` - Container configuration
- `.dockerignore` - Docker ignore patterns

**What it does:**
1. ⚙️ **Compile** Kotlin code
2. 📝 **Lint** code style (ktlint)
3. 🧪 **Run tests** with quality gates
4. 🔨 **Build & package** application

**Branches:** `main`, `develop`

### 🏠 **Home Assistant Pipeline** (`sync-home-assistant.yml`)

**Triggers when changes affect:**
- `home-assistant-scripts/**` - HA configuration files
- `.github/workflows/sync-home-assistant.yml` - This workflow itself

**What it does:**

**On Pull Requests:**
1. 🔍 **Validate YAML** syntax in all config files
2. 📁 **Check directory** structure integrity
3. ✅ **Ensure** required directories exist

**On Main Branch Push:**
1. 🔄 **Sync** `home-assistant-scripts/` → `home-assistant-config/custom/`
2. 📝 **Commit** synced changes automatically
3. 📊 **Generate** sync summary report

**Branches:** `main` (sync), `main` (validation on PRs)

### 🏗️ **Infrastructure Pipeline** (`infrastructure-pipeline.yml`)

**Triggers when changes affect:**
- `terraform/**` - Infrastructure as Code
- `.github/workflows/infrastructure-pipeline.yml` - This workflow itself

**What it does:**
1. 🔧 **Format check** Terraform files
2. 🏗️ **Initialize** Terraform (backend=false)
3. ✅ **Validate** Terraform configuration
4. 📋 **Plan** infrastructure changes (dry run)

**Branches:** `main`, `develop`

## 🎯 Efficiency Benefits

### **⚡ Performance**
- **No unnecessary runs** - Only affected pipelines execute
- **Faster feedback** - Developers see relevant results quickly
- **Resource optimization** - GitHub Actions minutes used efficiently

### **🔍 Clear Feedback**
- **Focused results** - Each pipeline shows status for specific component
- **Isolated failures** - API issues don't block HA deployments
- **Component-specific** - Easy to identify which part has issues

### **🔄 Parallel Execution**
- **Independent workflows** - Can run simultaneously
- **Non-blocking** - Infrastructure changes don't wait for API tests
- **Scalable** - Easy to add new components/pipelines

## 📊 Pipeline Matrix

| Change Type | API Pipeline | HA Pipeline | Infrastructure Pipeline |
|-------------|--------------|-------------|-------------------------|
| `logs-api/` code | ✅ Runs | ❌ Skipped | ❌ Skipped |
| `home-assistant-scripts/` | ❌ Skipped | ✅ Runs | ❌ Skipped |
| `terraform/` files | ❌ Skipped | ❌ Skipped | ✅ Runs |
| `docs/` changes | ❌ Skipped | ❌ Skipped | ❌ Skipped |
| Multiple components | ✅ Multiple pipelines run in parallel |

## 🚀 Usage Examples

### **Scenario 1: API Development**
```bash
# Only API pipeline runs
git add logs-api/src/main/kotlin/NewController.kt
git commit -m "Add new API endpoint"
git push
```

### **Scenario 2: Home Assistant Config Update**
```bash
# Only HA pipeline runs
git add home-assistant-scripts/automations/new-automation.yaml
git commit -m "Add energy monitoring automation" 
git push
```

### **Scenario 3: Infrastructure Update**
```bash
# Only Infrastructure pipeline runs
git add terraform/main.tf
git commit -m "Update EC2 instance type"
git push
```

### **Scenario 4: Multi-Component Change**
```bash
# Multiple pipelines run in parallel
git add logs-api/src/ home-assistant-scripts/ terraform/
git commit -m "Update API, HA configs, and infrastructure"
git push
```

## 🔧 Adding New Pipelines

To add a new component pipeline:

1. **Create workflow file** in `.github/workflows/`
2. **Add path filters** for your component directory
3. **Define appropriate** validation/build steps
4. **Update this documentation**

### **Template for New Pipeline:**
```yaml
name: Your Component Pipeline

on:
  push:
    branches: [main, develop]
    paths:
      - 'your-component/**'
      - '.github/workflows/your-pipeline.yml'
  pull_request:
    branches: [main, develop]
    paths:
      - 'your-component/**'
      - '.github/workflows/your-pipeline.yml'

jobs:
  your-job:
    name: 🎯 Your Component
    runs-on: ubuntu-latest
    steps:
      # Your build/test/deploy steps
```

## 🎛️ Manual Triggers

All pipelines support manual triggering via:
1. **GitHub** → **Actions** tab
2. **Select** your pipeline
3. **"Run workflow"** button

This is useful for:
- **Testing** pipeline changes
- **Force running** without code changes
- **Debugging** pipeline issues

---

**🎯 This architecture ensures efficient, scalable, and maintainable CI/CD for our monorepo while keeping components isolated and independently deployable.** 
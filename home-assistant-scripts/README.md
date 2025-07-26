# Custom Home Assistant Configurations

A modular configuration structure for organized, maintainable, and scalable Home Assistant setups.

<!-- Test sync - $(date) -->

## 🏗️ Directory Structure

```
/config/custom/
├── README.md              # This file - Overview and documentation  
├── statistics/            # Energy monitoring and statistical sensors
├── templates/             # Custom template sensors and entities
├── automations/           # Complex custom automations
├── scripts/               # Reusable action sequences
└── integrations/          # Custom component configurations
```

---

## 📊 **Statistics** (`/statistics/`)

**Purpose**: Energy monitoring, consumption tracking, and statistical calculations.

**Use Cases**:
- ✅ Daily/weekly/monthly energy consumption sensors
- ✅ Statistical analysis (min, max, average, total)
- ✅ Cost calculations and billing estimates
- ✅ Performance metrics and trend analysis

**Integration in `configuration.yaml`:**
```yaml
sensor: !include_dir_merge_list custom/statistics/
```

**Example Files**: `energy-monitoring.yaml`, `climate-stats.yaml`, `device-performance.yaml`

---

## 🔧 **Templates** (`/templates/`)

**Purpose**: Custom template sensors, binary sensors, and dynamic calculations.

**Use Cases**:
- ✅ Complex mathematical calculations from multiple sensors
- ✅ Status aggregation from multiple devices  
- ✅ Custom state transformations and formatting
- ✅ Conditional logic for sensor values

**Integration in `configuration.yaml`:**
```yaml
template: !include_dir_merge_list custom/templates/
```

**Example Files**: `calculated-sensors.yaml`, `status-aggregators.yaml`, `custom-formatters.yaml`

---

## 🤖 **Automations** (`/automations/`)

**Purpose**: Complex automations requiring advanced logic or multiple steps.

**Use Cases**:
- ✅ Multi-condition business logic automations
- ✅ Advanced scheduling with complex triggers
- ✅ Integration between multiple systems
- ✅ Custom notification and alert systems

**Integration in `configuration.yaml`:**
```yaml
automation manual: !include_dir_merge_list custom/automations/
```

**Example Files**: `energy-alerts.yaml`, `climate-control.yaml`, `security-system.yaml`

---

## 📜 **Scripts** (`/scripts/`)

**Purpose**: Reusable sequences of actions that can be called from automations or manually triggered.

**Use Cases**:
- ✅ Common action sequences used across multiple automations
- ✅ Manual control scripts for complex device orchestration
- ✅ Maintenance and system management routines
- ✅ Emergency shutdown or startup procedures

**Integration in `configuration.yaml`:**
```yaml
script: !include_dir_merge_named custom/scripts/
```

**Example Files**: `device-control.yaml`, `system-maintenance.yaml`, `emergency-procedures.yaml`

---

## 🔌 **Integrations** (`/integrations/`)

**Purpose**: Configuration for custom components, integrations, and external service connections.

**Use Cases**:
- ✅ Third-party service API configurations
- ✅ Custom component setup and configuration
- ✅ External database connections
- ✅ Custom notification service setups

**Integration Method (varies by integration type):**
```yaml
# Examples:
notify: !include custom/integrations/notification-services.yaml
rest_command: !include custom/integrations/rest-commands.yaml
```

**Example Files**: `notification-services.yaml`, `external-apis.yaml`, `database-connections.yaml`

---

## 🚀 **Getting Started**

### 1. **Choose Your Use Case**
Identify which type of configuration you need and navigate to the appropriate directory.

### 2. **Create Configuration Files**
Follow the naming conventions and refer to examples in each directory.

### 3. **Include in Main Configuration**
Add the appropriate `!include` statement to your main `configuration.yaml` file.

### 4. **Validate Configuration**
Always test your configuration using:
```
Developer Tools → YAML → Check Configuration
```

---

## 📖 **Best Practices**

### **File Organization**
- ✅ Use descriptive, lowercase filenames with hyphens
- ✅ Group related configurations in the same file
- ✅ Keep files focused on single functionality areas
- ❌ Avoid mixing unrelated configurations in the same file

### **Documentation**
- ✅ Include comments explaining complex logic
- ✅ Document entity dependencies and requirements
- ✅ Note any external integrations or add-ons required
- ✅ Include example usage in automation descriptions

### **Testing**
- ✅ Always validate YAML syntax before restarting
- ✅ Test automations with trace functionality
- ✅ Monitor logs for errors after changes
- ✅ Keep backups of working configurations

---

## 🔗 **Integration with Main Configuration**

Add these lines to your main `/config/configuration.yaml` to enable the modular structure:

```yaml
# Modular Custom Configurations
sensor: !include_dir_merge_list custom/statistics/
template: !include_dir_merge_list custom/templates/
automation manual: !include_dir_merge_list custom/automations/
script: !include_dir_merge_named custom/scripts/

# Add integration includes as needed
# notify: !include custom/integrations/notification-services.yaml
```

---

## 🆘 **Troubleshooting**

**Common Issues**:
- **YAML Syntax Errors**: Use YAML validators and check indentation
- **Include Failures**: Verify file paths and YAML structure
- **Entity Conflicts**: Ensure unique entity IDs across all files
- **Missing Dependencies**: Check that required integrations are installed

**Debugging Steps**:
1. Check Home Assistant logs: `Configuration → Logs`
2. Validate YAML: `Developer Tools → YAML → Check Configuration`
3. Test individual files by temporarily disabling includes
4. Use automation trace to debug logic flow

---

**📝 Note**: This structure follows Home Assistant best practices for modular configuration management. Each directory contains its own README with specific examples and implementation details. 
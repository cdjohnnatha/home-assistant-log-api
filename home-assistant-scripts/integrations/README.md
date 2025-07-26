# Integrações Específicas

Configurações de integrações que precisam de setup específico.

## 📖 Como Usar

Inclua individualmente conforme necessário:
```yaml
# Exemplo para sensores específicos
mqtt: !include custom/integrations/mqtt-sensors.yaml
rest: !include custom/integrations/api-sensors.yaml
```

## 🔗 Tipos de Integrações

### APIs Externas
- Sensores de clima
- Cotações
- Status de serviços

### Dispositivos MQTT
- Sensores DIY
- Dispositivos customizados
- Bridges de protocolo

### Integrações REST
- Webhooks
- APIs personalizadas
- Scraping de dados

## 🔧 Exemplos

### MQTT Sensor
```yaml
sensor:
  - name: "Sensor MQTT Custom"
    state_topic: "home/sensor/data"
    unit_of_measurement: "°C"
    value_template: "{{ value_json.temperature }}"
```

### REST Sensor  
```yaml
sensor:
  - platform: rest
    name: "API Externa"
    resource: "https://api.exemplo.com/data"
    value_template: "{{ value_json.valor }}"
```
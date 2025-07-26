# Statistics Sensors

Sensores de estatísticas para monitoramento de consumo, médias, máximos/mínimos, etc.

## 📖 Como Usar

Os arquivos aqui são incluídos no `configuration.yaml` via:
```yaml
sensor: !include_dir_merge_list custom/statistics/
```

## 📊 Exemplos de Sensores

### Monitoramento de Energia
- Consumo diário/semanal/mensal
- Médias de potência
- Picos de consumo

### Estatísticas Ambientais
- Temperatura média
- Umidade máxima/mínima
- Variações climáticas

## 🔧 Template Básico

```yaml
- platform: statistics
  name: "Nome do Sensor"
  entity_id: sensor.entidade_origem
  state_characteristic: total  # ou mean, max, min
  max_age:
    hours: 24  # ou days: 7, days: 30
  precision: 2
```

## 📁 Arquivos Atuais

- `energy-monitoring.yaml` - Monitoramento de energia do ventilador
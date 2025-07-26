# Template Sensors

Template sensors para cálculos personalizados e transformações de dados.

## 📖 Como Usar

Inclua no `configuration.yaml`:
```yaml
template: !include_dir_merge_list custom/templates/
```

## 💡 Exemplos de Uso

### Cálculos de Custo
```yaml
- sensor:
    - name: "Custo Energia Mensal"
      unit_of_measurement: "R$"
      state: >
        {% set consumo = states('sensor.consumo_mensal') | float(0) %}
        {% set tarifa = 0.75 %}
        {{ (consumo * tarifa) | round(2) }}
```

### Conversões de Unidade
```yaml
- sensor:
    - name: "Temperatura Fahrenheit"
      unit_of_measurement: "°F"
      state: >
        {% set celsius = states('sensor.temperatura') | float(0) %}
        {{ (celsius * 9/5 + 32) | round(1) }}
```

### Status Combinados
```yaml
- binary_sensor:
    - name: "Casa Ocupada"
      state: >
        {{ is_state('person.joao', 'home') or 
           is_state('person.maria', 'home') }}
```
```

### **4. `/config/custom/automations/README.md`**
```markdown
# Automações Customizadas

Automações complexas organizadas por função/área.

## 📖 Como Usar

### Opção 1: Substituir automations.yaml
```yaml
automation: !include_dir_merge_list custom/automations/
```

### Opção 2: Manter UI + Custom (Recomendado)
```yaml
# Automações da UI
automation ui: !include automations.yaml

# Automações customizadas
automation custom: !include_dir_merge_list custom/automations/
```

## 🏠 Organização por Área

```
automations/
├── energia/        # Automações de energia
├── seguranca/      # Automações de segurança  
├── iluminacao/     # Automações de luz
├── clima/          # Automações de temperatura
└── notificacoes/   # Automações de alertas
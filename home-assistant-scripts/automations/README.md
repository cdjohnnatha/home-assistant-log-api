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

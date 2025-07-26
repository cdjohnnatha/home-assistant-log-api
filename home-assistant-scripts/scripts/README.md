# Scripts Reutilizáveis

Scripts para ações que podem ser chamadas por automações ou manualmente.

## 📖 Como Usar

```yaml
script: !include_dir_merge_named custom/scripts/
```

## 🛠️ Tipos de Scripts

### Scripts de Ação
- Sequências de comandos
- Rotinas de casa
- Configurações de cena

### Scripts de Notificação
- Alertas personalizados
- Relatórios automáticos
- Mensagens formatadas

## 🔧 Template Básico

```yaml
nome_do_script:
  alias: "Nome Amigável"
  description: "O que o script faz"
  fields:
    parametro:
      description: "Descrição do parâmetro"
      example: "valor exemplo"
  sequence:
    - action: light.turn_on
      target:
        entity_id: light.exemplo
    - delay:
        seconds: 5
    - action: light.turn_off
      target:
        entity_id: light.exemplo
```

## 📞 Como Chamar

```yaml
# Em automação
- action: script.nome_do_script
  data:
    parametro: "valor"

# Via serviço
script.nome_do_script
```
# Home Assistant Integration

Este diretório contém a documentação e arquivos de configuração necessários para integrar a **Home Assistant Log API** com o **Home Assistant**.

## 📋 Overview

A integração permite que o Home Assistant envie alertas de temperatura diretamente para a API no EC2, que por sua vez publica notificações via Amazon SNS.

## 🏗️ Arquitetura da Integração

### **Fluxo Básico:**
```
┌─────────────────┐    HTTP POST     ┌─────────────────┐    SNS Publish    ┌─────────────────┐
│  Home Assistant │ ──────────────►  │   EC2 API       │ ──────────────►   │   Amazon SNS    │
│  (Raspberry Pi) │                  │  (Spring Boot)  │                   │   (Email/SMS)   │
└─────────────────┘                  └─────────────────┘                   └─────────────────┘
```

### **Fluxo Completo com Tratamento de Erros:**

![Mermaid diagram showing the complete integration flow with error handling, health checks, and robust templates for handling unavailable sensors]

**🎯 Características principais:**
- **Templates robustos** lidam com sensores offline
- **Health check on-demand** minimiza custos AWS
- **Notificação local sempre funciona** independente da API
- **Envio condicional** para API apenas quando disponível

## 📁 Arquivos Neste Diretório

- **`rest-commands.yaml`** - Configuração dos REST commands para o Home Assistant
- **`README.md`** - Esta documentação

## 🔧 Configuração

### 1. Pre-requisitos

- Home Assistant instalado (testado no Raspberry Pi 5)
- API rodando no EC2 (veja `/docs/DEPLOYMENT.md`)
- File Editor Add-on instalado no Home Assistant

### 2. Instalação dos REST Commands

1. **Copie o conteúdo** de `rest-commands.yaml`
2. **No Home Assistant**, crie o diretório: `/config/api-integrations/`
3. **Crie o arquivo**: `/config/api-integrations/home-assistant-logs.api.yaml`
4. **Cole o conteúdo** copiado
5. **Edite** `/config/configuration.yaml` e adicione:
   ```yaml
   # REST Commands - API Integrations
   rest_command: !include api-integrations/home-assistant-logs.api.yaml
   ```
6. **Reinicie** o Home Assistant

### 3. Criação da Automação

1. **Configurações** → **Automações e Cenas** → **Criar Automação**
2. **Configure o trigger** para detectar diferença de temperatura > 5°C
3. **Adicione ações sequenciais**:
   - Notificação persistente local
   - Health check da API (`rest_command.check_sns_api_health`)
   - Envio condicional para API (`rest_command.send_temp_alert_to_sns_api`)

### 4. Configuração de Sensores

Substitua pelos nomes reais dos seus sensores:
- `sensor.temperatura_quarto`
- `sensor.temperatura_escritorio`

## 🧪 Testes

### Teste Manual dos REST Commands

1. **Ferramentas de Desenvolvedor** → **Ações**
2. **Health Check**:
   ```yaml
   # Serviço: rest_command.check_sns_api_health
   # (sem parâmetros)
   ```
3. **Envio de Alerta**:
   ```yaml
   # Serviço: rest_command.send_temp_alert_to_sns_api
   source: "teste-manual"
   eventType: "INFO"
   quarto_temp: 22.5
   escritorio_temp: 25.0
   temp_diff: 2.5
   ```

### Verificação de Logs

```bash
# Logs da API no EC2
ssh ec2-user@<IP_EC2> "docker-compose logs -f logs-api"

# Deve mostrar:
# - event processed: EventLog(source=teste-manual...)
# - Notification sent successfully...
```

## 📊 Monitoramento

### Indicadores de Sucesso

- ✅ **Home Assistant**: Notificação persistente aparece
- ✅ **API**: Logs mostram "event processed" e "Notification sent"
- ✅ **SNS**: Email/SMS recebido no destino configurado

### Troubleshooting

| Problema | Possível Causa | Solução |
|----------|----------------|---------|
| REST command não aparece | Sintaxe YAML incorreta | Verificar indentação e !include |
| Erro 400 | JSON malformado | Verificar templates no rest-commands.yaml |
| Erro 500 | API indisponível | Verificar status do EC2 e Docker |
| Sem email | SNS mal configurado | Verificar subscriptions no AWS Console |
| **Template error: float got invalid input 'unavailable'** | **Sensor indisponível/offline** | **Ver seção [Templates Robustos](#-templates-robustos)** |

## 💡 Templates Robustos

### **Problema Comum: Sensores Indisponíveis**

Quando sensores estão offline ou indisponíveis, eles retornam `'unavailable'` em vez de valores numéricos. Isso causa erros nos templates como:

```
Template error: float got invalid input 'unavailable' when rendering template
```

### **❌ Template Básico (Pode Falhar):**
```yaml
quarto_temp: "{{ states('sensor.temperatura_quarto') | float }}"
```

### **✅ Template Robusto (Recomendado):**
```yaml
quarto_temp: "{{ states('sensor.temperatura_quarto') | float(0) if is_number(states('sensor.temperatura_quarto')) else 0 }}"
```

### **🛡️ Template Ainda Mais Robusto:**
```yaml
quarto_temp: >-
  {% if has_value('sensor.temperatura_quarto') and is_number(states('sensor.temperatura_quarto')) %}
    {{ states('sensor.temperatura_quarto') | float }}
  {% else %}
    0
  {% endif %}
```

### **🔧 Implementação Completa:**

Para automações de temperatura, use este padrão na seção `data:` do REST command:

```yaml
action: rest_command.send_temp_alert_to_sns_api
metadata: {}
data:
  source: "home-assistant-raspberry-pi"
  eventType: "WARNING"
  quarto_temp: "{{ states('sensor.temperatura_quarto') | float(0) if is_number(states('sensor.temperatura_quarto')) else 0 }}"
  escritorio_temp: "{{ states('sensor.temperatura_escritorio') | float(0) if is_number(states('sensor.temperatura_escritorio')) else 0 }}"
  temp_diff: "{{ (states('sensor.temperatura_quarto') | float(0) - states('sensor.temperatura_escritorio') | float(0)) | abs if is_number(states('sensor.temperatura_quarto')) and is_number(states('sensor.temperatura_escritorio')) else 0 }}"
```

### **🧪 Testar Templates:**

1. **Ferramentas de Desenvolvedor** → **Template**
2. **Cole este código para verificar sensores:**

```yaml
Sensor Quarto: {{ states('sensor.temperatura_quarto') }}
É número? {{ is_number(states('sensor.temperatura_quarto')) }}
Tem valor? {{ has_value('sensor.temperatura_quarto') }}

Lista de sensores de temperatura:
{% for state in states.sensor %}
  {% if 'temperatura' in state.entity_id %}
    {{ state.entity_id }} = {{ state.state }}
  {% endif %}
{% endfor %}
```

### **💡 Dicas de Templates:**

- **`| float(default)`**: Define valor padrão se conversão falhar
- **`is_number(value)`**: Verifica se é um valor numérico válido
- **`has_value(sensor)`**: Verifica se sensor existe e tem valor
- **Sempre use valores padrão** para sensores críticos

## 🔗 Links Relacionados

- [Documentação de Deployment](/docs/DEPLOYMENT.md)
- [Monitoramento de Custos](/docs/COST-MONITORING.md)
- [Troubleshooting Geral](/docs/TROUBLESHOOTING.md)

## 📝 Histórico de Versões

- **v1.1** - Templates robustos para sensores indisponíveis
  - Adicionado tratamento de erro para sensores offline
  - Implementados templates com valores padrão
  - Documentação completa de troubleshooting
- **v1.0** - Integração inicial com health check on-demand
  - Configuração de REST commands modulares
  - Automação via interface gráfica
  - Integração completa Home Assistant → EC2 → SNS → Email 
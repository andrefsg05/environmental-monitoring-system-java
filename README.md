# Environmental Monitoring System 

## Descrição Breve e Principais Funcionalidades

Este é um sistema distribuído de **monitorização ambiental** com suporte a múltiplos protocolos de comunicação (gRPC, MQTT, REST), que coleta métricas de temperatura e humidade de múltiplos sensores/gateways distribuídos, centralizando os dados em um servidor Spring Boot com persistência em PostgreSQL.

O projeto demonstra:
- **Arquitetura em Microsserviços** com clientes independentes
- **Múltiplos Protocolos de Comunicação** (gRPC, MQTT, REST) operando em paralelo
- **Escalabilidade Horizontal** via supervisores dinâmicos
- **Cache em Memória** com refresh automático
- **Agregação de Dados** por localização (sala, departamento, andar, prédio)
- **Tolerância a Falhas** com retry automático e reconexão

### Principais Funcionalidades

| Funcionalidade | Descrição |
|---|---|
| **Ingestão de Métricas** | Recebe dados de temperatura/humidade via gRPC, MQTT e REST |
| **Validação de Dispositivos** | Garante que apenas dispositivos ativos possam enviar dados |
| **Agregação de Dados** | Calcula médias, mínimos e máximos por localização |
| **Cache Dinâmico** | Mantém lista de dispositivos ativos em RAM com refresh periódico (10s) |
| **Supervisores Dinâmicos** | Adapta-se automaticamente a dispositivos novos/removidos |
| **Monitoramento MQTT** | Subscribe a tópicos com QoS 1 (at-least-once) |
| **Gestão CRUD** | Endpoints REST para gerenciar dispositivos |
| **Admin CLI** | Interface de linha de comando para administração |

---

## Tech Stack

### Linguagem & Framework
- **Java 17** — Linguagem principal
- **Spring Boot 3.2** — Framework web com auto-configuração
- **Spring Data JPA** — ORM para acesso a BD

### Protocolos & Comunicação
- **gRPC 1.59** com **Protocol Buffers 3.25** — RPC de alta performance com serialização binária
- **MQTT (Paho 1.2.5)** — Pub/Sub otimizado para IoT
- **REST/HTTP** — Integração com clientes HTTP5
- **Spring Integration** — Orquestração de mensagens

### Persistência & Base de Dados
- **PostgreSQL 15** — BD relacional com suporte a tipos avançados
- **JPA/Hibernate** — Mapeamento objeto-relacional

### Infraestrutura
- **Docker & Docker Compose** — Containerização (Mosquitto + PostgreSQL)
- **Maven 3.8+** — Build tool com multi-módulos

### Bibliotecas Auxiliares
- **Jackson** — Serialização JSON
- **SLF4J + Logback** — Logging estruturado
- **Apache HttpClient 5** — Cliente HTTP com retry

### Versões Chave
```xml
Java: 17
Spring Boot: 3.2.0
gRPC: 1.59.0
Protocol Buffers: 3.25.1
PostgreSQL: 15-alpine
Eclipse Mosquitto: 2
```

---

## Estrutura do Projeto e Ficheiros

### Organização Geral

```
SD/
├── proto/                          # Definições Protocol Buffer
│   └── metrics.proto              # Serviço gRPC e mensagens
│
├── server/                        # Servidor Spring Boot central
│   ├── pom.xml                   # Dependências (gRPC, MQTT, PostgreSQL)
│   └── src/main/java/com/sd/server/
│       ├── ServerApplication.java # Ponto de entrada Spring
│       ├── config/
│       │   ├── GrpcConfig.java   # Setup servidor gRPC na porta 9090
│       │   └── MqttConfig.java   # Setup MQTT com Spring Integration
│       ├── controller/
│       │   ├── DeviceController.java    # Endpoints CRUD para dispositivos
│       │   └── MetricController.java    # Endpoints de ingestão e consulta
│       ├── service/
│       │   ├── DeviceService.java       # Lógica CRUD dispositivos
│       │   ├── DeviceMonitorService.java # Cache em RAM com refresh (10s)
│       │   └── MetricService.java        # Ingestão, validação, agregação
│       ├── entity/
│       │   ├── Device.java       # Entidade JPA (id, protocolo, localização, status)
│       │   └── Metric.java       # Entidade JPA (temperature, humidity, timestamp)
│       ├── repository/
│       │   ├── DeviceRepository.java    # Acesso a BD para dispositivos
│       │   └── MetricRepository.java    # Acesso a BD para métricas
│       ├── mqtt/
│       │   └── MqttMessageHandler.java  # Desserializa e ingere mensagens MQTT
│       ├── grpc/
│       │   └── MetricsGrpcService.java  # Implementação do serviço gRPC
│       └── dto/
│           ├── DeviceDTO.java
│           ├── MetricDTO.java
│           └── AverageMetricDTO.java
│
├── client-grpc/                   # Cliente gRPC com supervisor dinâmico
│   ├── pom.xml
│   └── src/main/java/com/sd/grpc/
│       ├── GrpcSensorSimulator.java     # Simulador de sensor (envia a cada 5s)
│       ├── GrpcSuperviser.java          # Gerencia múltiplos simuladores (sincroniza a cada 10s)
│       └── config/
│           └── GrpcClientConfig.java    # Configuração: serverHost, serverPort, intervalos
│
├── client-mqtt/                   # Cliente MQTT com supervisor dinâmico
│   ├── pom.xml
│   └── src/main/java/com/sd/mqtt/
│       ├── MqttSensorSimulator.java     # Simulador de sensor (envia a cada 5s)
│       ├── MqttSuperviser.java          # Gerencia múltiplos simuladores
│       └── config/
│           └── MqttClientConfig.java    # Configuração: broker URL, topic, intervalos
│
├── client-rest/                   # Cliente REST com supervisor dinâmico
│   ├── pom.xml
│   └── src/main/java/com/sd/rest/
│       ├── RestSensorSimulator.java     # Simulador de sensor (envia a cada 5s, com retry)
│       ├── RestSuperviser.java          # Gerencia múltiplos simuladores
│       └── config/
│           └── RestClientConfig.java    # Configuração: serverUrl, intervalos
│
├── admin-cli/                     # Interface CLI para administração
│   ├── pom.xml
│   └── src/main/java/com/sd/cli/
│       └── (classes para gerenciar dispositivos via linha de comando)
│
├── mosquitto/                     # Configuração do broker MQTT
│   ├── config/mosquitto.conf
│   ├── data/                      # Persistência de dados MQTT
│   └── log/                       # Logs do Mosquitto
│
├── docker-compose.yml             # Orquestração: Mosquitto + PostgreSQL
├── .env                           # Variáveis de ambiente (portas, credenciais)
└── README.md                      # Documentação técnica

```

### Ficheiros Chave e Responsabilidades

#### **1. Configuração (config/**)**

| Ficheiro | Responsabilidade |
|---|---|
| `GrpcConfig.java` | Inicializa servidor gRPC na porta 9090, registra `MetricsGrpcService` |
| `MqttConfig.java` | Setup broker MQTT, canal de mensagens, adaptador e handler |

#### **2. Controllers (controller/**)**

| Endpoint | Método | Descrição |
|---|---|---|
| `GET /api/devices` | `getAllDevices()` | Lista todos dispositivos |
| `GET /api/devices/active` | `getActiveDevices()` | Lista apenas ativos |
| `GET /api/devices/active/{protocol}` | `getActiveDevicesByProtocol()` | Filtra por protocolo |
| `POST /api/devices` | `createDevice()` | Cria novo dispositivo |
| `PUT /api/devices/{id}` | `updateDevice()` | Atualiza localização/status |
| `DELETE /api/devices/{id}` | `deleteDevice()` | Deleta dispositivo + métricas |
| `POST /api/metrics/ingest` | `ingestMetric()` | Ingere métrica (REST) |
| `GET /api/metrics/average` | `getAverageMetrics()` | Calcula médias por localização |
| `GET /api/metrics/raw` | `getRawMetrics()` | Retorna histórico bruto |

#### **3. Services (service/**)**

| Serviço | Responsabilidade |
|---|---|
| `DeviceService.java` | CRUD dispositivos, queries por protocolo |
| `DeviceMonitorService.java` | **Cache em RAM** agrupado por protocolo, refresh a cada 10s |
| `MetricService.java` | Validação, ingestão, agregação (média/min/max) |

#### **4. Entities (entity/**)**

```java
Device {
  id: String,              // Identificador único (ex: "sensor-01")
  protocol: MQTT|GRPC|REST // Tipo de comunicação
  room: String,            // Localização: sala
  department: String,      // Localização: departamento
  floor: String,           // Localização: andar
  building: String,        // Localização: prédio
  active: Boolean,         // Status do dispositivo
  createdAt: LocalDateTime,
  updatedAt: LocalDateTime
}

Metric {
  id: Long,                 // Auto-incrementado
  device: Device,           // FK para Device
  temperature: Double,      // °C
  humidity: Double,         // %
  timestamp: LocalDateTime, // Quando foi gerado
  receivedAt: LocalDateTime // Quando foi recebido
}
```

#### **5. Repositories (repository/**)**

```java
DeviceRepository extends JpaRepository<Device, String> {
  List<Device> findByActive(boolean active);
  List<Device> findByProtocolAndActive(ProtocolType, boolean);
  // ... mais queries customizadas
}

MetricRepository extends JpaRepository<Metric, Long> {
  List<Metric> findByDeviceIdAndTimestampBetween(...);
  Object[] getAverageByRoom(String roomId, LocalDateTime from, to);
  Object[] getAverageByDepartment(String deptId, ...);
  Object[] getAverageByFloor(String floorId, ...);
  Object[] getAverageByBuilding(String buildingId, ...);
}
```

---

## 🔗 Lógica do Projeto e Como as Coisas se Ligam

### Arquitetura de Alto Nível

```
┌─────────────────────────────────────────────────────────────────────┐
│                          CLIENTS (Simuladores)                      │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐ │
│  │  gRPC Supervisor │  │ MQTT Supervisor  │  │ REST Supervisor  │ │
│  │ (a cada 10s)     │  │ (a cada 10s)     │  │ (a cada 10s)     │ │
│  │                  │  │                  │  │                  │ │
│  │ Sincroniza via   │  │ Sincroniza via   │  │ Sincroniza via   │ │
│  │ HTTP GET         │  │ HTTP GET         │  │ HTTP GET         │ │
│  │ /api/devices/    │  │ /api/devices/    │  │ /api/devices/    │ │
│  │ active/GRPC      │  │ active/MQTT      │  │ active/REST      │ │
│  │                  │  │                  │  │                  │ │
│  └────────┬─────────┘  └────────┬─────────┘  └────────┬─────────┘ │
│           │                     │                     │            │
│     ┌─────┴───────┐    ┌────────┴────────┐    ┌──────┴──────┐    │
│     │ N Simuladores   │ N Simuladores   │  N Simuladores   │    │
│     │ (a cada 5s)    │ (a cada 5s)     │ (a cada 5s)      │    │
│     └─────┬─────────┘    └────────┬────────┘    └──────┴──────┘    │
│           │                     │                     │            │
└───────────┼─────────────────────┼─────────────────────┼────────────┘
            │                     │                     │
    ┌───────▼──────────────────────▼─────────────────────▼──────────────┐
    │                         NETWORK LAYER                             │
    │  gRPC (Port 9090)   │  MQTT (Port 1883)  │  REST/HTTP (Port 8080)│
    └───────┬──────────────────────┬─────────────────────┬──────────────┘
            │                     │                     │
    ┌───────▼─────────────────────▼──────────────────────▼──────────────┐
    │                    SERVER (Spring Boot)                           │
    │                     (Port 8080)                                   │
    ├──────────────────────────────────────────────────────────────────┤
    │                                                                  │
    │  ┌─────────────┐  ┌─────────────┐  ┌──────────────────────┐   │
    │  │  Endpoints  │  │   Services  │  │  DeviceMonitorService│   │
    │  │  (REST API) │  │             │  │  (Cache em RAM)      │   │
    │  ├─────────────┤  ├─────────────┤  ├──────────────────────┤   │
    │  │ /devices    │  │ DeviceService  Refresh: 10s            │   │
    │  │ /metrics    │  │ MetricService                          │   │
    │  └─────────────┘  │ (Validação +  Map<Protocol,           │   │
    │                   │  Ingestão)    Map<DeviceId, Device>>   │   │
    │  ┌─────────────┐  └─────────────┘  └──────────────────────┘   │
    │  │   gRPC      │  ┌─────────────┐                             │
    │  │ (Port 9090) │  │ MqttMessage │ Listeners automáticos:      │
    │  │             │  │  Handler    │ Spring Integration         │
    │  └─────────────┘  └─────────────┘ (Adaptador MQTT)           │
    │                                                                │
    └───────┬─────────────────────────────────────────────┬─────────┘
            │                                             │
    ┌───────▼──────────────────────────────────────┬─────▼──────────┐
    │         PERSISTÊNCIA & INFRAESTRUTURA        │                │
    ├────────────────────────────────────────────────────────────────┤
    │                                                                │
    │  ┌────────────────────┐      ┌───────────────────────┐       │
    │  │   PostgreSQL 15    │      │ Eclipse Mosquitto     │       │
    │  │   (Port 5432)      │      │ (Port 1883)           │       │
    │  ├────────────────────┤      ├───────────────────────┤       │
    │  │ Table: devices     │      │ Topic: sensors/#      │       │
    │  │ Table: metrics     │      │ QoS: 1 (at-least-once)│      │
    │  └────────────────────┘      └───────────────────────┘       │
    │                                                                │
    │  ┌─────────────────────────────────────────┐                 │
    │  │ Data Flow:                              │                 │
    │  │ 1. Métrica chega via MQTT/gRPC/REST    │                 │
    │  │ 2. DeviceService valida se ativo      │                 │
    │  │ 3. MetricService ingere e armazena    │                 │
    │  │ 4. REST API permite consultar/agregar │                 │
    │  └─────────────────────────────────────────┘                 │
    │                                                                │
    └────────────────────────────────────────────────────────────────┘
```

### Fluxo 1: Envio de Métrica via gRPC

```
1. GrpcSensorSimulator (cliente)
   ├─ Gera temperatura/humidade aleatória com variação gradual
   ├─ Constrói MetricRequest (protobuf)
   └─ Chama blockingStub.sendMetric(request) → RPC SÍNCRONA

2. Server recebe em MetricsGrpcService.sendMetric()
   ├─ Desserializa MetricRequest
   ├─ Converte para MetricDTO
   └─ Chama metricService.ingestMetric(dto)

3. MetricService.ingestMetric()
   ├─ Valida: device existe?
   ├─ Valida: device está ativo?
   ├─ Se sim → persiste em BD via MetricRepository.save()
   └─ Retorna true/false

4. MetricsGrpcService constrói MetricResponse
   └─ Retorna ao cliente: {success: true/false, message: "..."}
```

### Fluxo 2: Envio de Métrica via MQTT

```
1. MqttSensorSimulator (cliente)
   ├─ Gera temperatura/humidade aleatória
   ├─ Serializa para JSON (MetricDTO)
   └─ Publica em tópico: sensors/{deviceId} com QoS=1

2. Mosquitto (broker)
   └─ Recebe mensagem, garante entrega (QoS 1)

3. Spring Integration (servidor)
   ├─ Adaptador MQTT detecta publicação
   └─ Enruta para mqttInputChannel

4. MqttMessageHandler.handleMessage()
   ├─ Desserializa JSON do payload
   ├─ Converte para MetricDTO
   └─ Chama metricService.ingestMetric(dto)

5. MetricService valida e persiste (igual a gRPC)
```

### Fluxo 3: Envio de Métrica via REST

```
1. RestSensorSimulator (cliente)
   ├─ Gera temperatura/humidade
   ├─ Serializa para JSON
   └─ HTTP POST para /api/metrics/ingest com retry automático

2. MetricController.ingestMetric()
   ├─ Recebe @RequestBody MetricDTO
   └─ Valida @Valid automática

3. Chama metricService.ingestMetric(dto)
   ├─ Valida device
   ├─ Persiste em BD
   └─ Retorna {status, message}

4. Resposta HTTP: 200 OK ou 400 Bad Request
```

### Fluxo 4: Descoberta Dinâmica de Dispositivos

```
GrpcSuperviser (ou MQTT/REST)
├─ Inicialmente: syncWithRegistry() imediatamente
├─ Depois: scheduler.scheduleAtFixedRate(..., 10s, 10s)
│
└─ Cada 10 segundos:
   ├─ HTTP GET http://localhost:8080/api/devices/active/GRPC
   │  └─ Retorna lista de DeviceDTO ativos para protocolo GRPC
   │
   ├─ Compara com simuladores em execução (cache local)
   │
   ├─ Se novo dispositivo:
   │  └─ Cria novo GrpcSensorSimulator(deviceId, serverHost, serverPort)
   │     └─ Inicia: scheduler.scheduleAtFixedRate(sendMetric, 0, 5s)
   │
   └─ Se dispositivo removido/inativo:
      └─ Para o simulador: channel.shutdown()
```

### Fluxo 5: Cache DeviceMonitorService

```
ServerApplication inicia
├─ Spring cria bean DeviceMonitorService
│  └─ @PostConstruct public void start()
│     ├─ refreshCache() imediatamente
│     │  ├─ deviceService.getActiveDevices() (query BD)
│     │  └─ Agrupa em Map<Protocol, Map<DeviceId, DeviceDTO>>
│     │
│     └─ scheduler.scheduleAtFixedRate(safeRefresh, 0, 10s)
│        └─ A cada 10s, executa refreshCache() novamente
│
├─ Cache pronto em RAM (atualizado a cada 10s)
│  └─ activeCache = {
│        MQTT: {sensor-01: DeviceDTO, sensor-02: DeviceDTO},
│        GRPC: {device-03: DeviceDTO},
│        REST: {app-01: DeviceDTO}
│      }
│
└─ Disponível para consultas rápidas (não persiste no código atual,
   mas preparado para uso futuro)
```

---

## Como Começar

### Pré-requisitos
- Java 17+
- Maven 3.8+
- Docker & Docker Compose

### Passos

```bash
# 1. Clone/abra o projeto
cd /home/andre/SD2\ WORK/SD

# 2. Inicie infraestrutura
docker-compose up -d

# 3. Compile tudo
mvn clean package

# 4. Inicie servidor
cd server && mvn spring-boot:run

# 5. Em novo terminal, inicie clientes
cd ../client-grpc && mvn exec:java
cd ../client-mqtt && mvn exec:java
cd ../client-rest && mvn exec:java

# 6. Teste endpoints
curl http://localhost:8080/api/devices
curl http://localhost:8080/api/metrics/average?level=room&id=sala-101
```

---

## Âmbito

Este projeto foi desenvolvido como trabalho académico para a disciplina Sistemas Distribuídos na Universidade de Évora.

---

## Autores
- André Gonçalves
- [André Zhan](https://github.com/andr-zhan)

package com.sd.server.mqtt;

import java.time.LocalDateTime; // Data/hora

import org.slf4j.Logger; // Interface de logging
import org.slf4j.LoggerFactory; // Factory de loggers
import org.springframework.beans.factory.annotation.Autowired; // Injeção de dependências
import org.springframework.messaging.Message; // Mensagem Spring
import org.springframework.messaging.MessageHandler; // Interface de handler
import org.springframework.messaging.MessagingException; // Exceção de mensagens
import org.springframework.stereotype.Component; // Marca como componente Spring

import com.fasterxml.jackson.core.JsonProcessingException; // Exceção JSON
import com.fasterxml.jackson.databind.ObjectMapper; // Serialização JSON
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule; // Suporte Java 8 Time
import com.sd.server.dto.MetricDTO; // DTO de métrica
import com.sd.server.service.MetricService; // Serviço de métricas

/**
 * Handler de mensagens MQTT que processa métricas recebidas.
 * Desserializa JSON de mensagem MQTT e armazena via MetricService.
 * Integrado ao Spring Integration para receber mensagens automaticamente.
 */
@Component
public class MqttMessageHandler implements MessageHandler {

    private static final Logger logger = LoggerFactory.getLogger(MqttMessageHandler.class);

    @Autowired
    private MetricService metricService;

    private final ObjectMapper objectMapper;

    /**
     * Inicializa handler com ObjectMapper configurado para LocalDateTime.
     */
    public MqttMessageHandler() {
        this.objectMapper = new ObjectMapper();
        // Registra módulo para desserializar LocalDateTime
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    /**
     * Processa mensagem MQTT recebida via Spring Integration.
     * Extrai payload, desserializa JSON e armazena métrica.
     * 
     * @param message Message contendo payload JSON e header com tópico
     * @throws MessagingException em caso de erro de processamento
     */
    @Override
    public void handleMessage(Message<?> message) throws MessagingException {
        String payload = message.getPayload().toString();
        String topic = message.getHeaders().get("mqtt_receivedTopic", String.class);
        
        logger.debug("Received MQTT message on topic {}: {}", topic, payload);

        try {
            // Desserializa JSON em objeto intermediário
            MqttMetricMessage mqttMessage = objectMapper.readValue(payload, MqttMetricMessage.class);
            
            MetricDTO metricDTO = new MetricDTO();
            metricDTO.setDeviceId(mqttMessage.getDeviceId());
            metricDTO.setTemperature(mqttMessage.getTemperature());
            metricDTO.setHumidity(mqttMessage.getHumidity());
            // Usa timestamp da mensagem ou timestamp atual se não fornecido
            metricDTO.setTimestamp(mqttMessage.getTimestamp() != null ? 
                    mqttMessage.getTimestamp() : LocalDateTime.now());

            // Armazena métrica via service
            metricService.ingestMetric(metricDTO);
        } catch (JsonProcessingException e) {
            logger.error("Failed to parse MQTT message: {}", e.getMessage());
        } catch (Exception e) {
            logger.error("Error processing MQTT message: {}", e.getMessage());
        }
    }

    /**
     * Classe interna para desserializar payload JSON da mensagem MQTT.
     * Contém campos: deviceId, temperature, humidity, timestamp.
     */
    private static class MqttMetricMessage {
        private String deviceId;
        private Double temperature;
        private Double humidity;
        private LocalDateTime timestamp;

        public String getDeviceId() { return deviceId; }
        public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
        
        public Double getTemperature() { return temperature; }
        public void setTemperature(Double temperature) { this.temperature = temperature; }
        
        public Double getHumidity() { return humidity; }
        public void setHumidity(Double humidity) { this.humidity = humidity; }
        
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    }
}
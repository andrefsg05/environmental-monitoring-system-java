package com.sd.mqtt;

import java.time.LocalDateTime; // Data/hora atual
import java.time.format.DateTimeFormatter; // Formatar datas 
import java.util.Random; // Gerar valores aleatórios
import java.util.concurrent.ScheduledExecutorService; // Agenda tarefas periódicas
import java.util.concurrent.TimeUnit; // Unidades de tempo

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken; // Token de entrega MQTT
import org.eclipse.paho.client.mqttv3.MqttCallback; // Callbacks MQTT
import org.eclipse.paho.client.mqttv3.MqttClient; // Cliente MQTT
import org.eclipse.paho.client.mqttv3.MqttConnectOptions; // Opções de conexão
import org.eclipse.paho.client.mqttv3.MqttException; // Exceção MQTT
import org.eclipse.paho.client.mqttv3.MqttMessage; // Mensagem MQTT
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence; // Persistência em memória
import org.slf4j.Logger; // Interface de logging
import org.slf4j.LoggerFactory; // Factory de loggers

import com.fasterxml.jackson.core.JsonProcessingException; // Exceção JSON
import com.fasterxml.jackson.databind.ObjectMapper; // Serialização JSON
import com.sd.mqtt.config.MqttClientConfig; // Config MQTT
import com.sd.mqtt.dto.MetricDTO; // DTO de métrica

/**
 * Simulador de sensor para protocolo MQTT.
 * Conecta a um broker MQTT, gera métricas simuladas de temperatura e humidade,
 * e as envia periodicamente a um tópico MQTT específico do dispositivo.
 * As métricas são geradas com variações graduais realistas.
 */
public class MqttSensorSimulator {

    private static final Logger logger = LoggerFactory.getLogger(MqttSensorSimulator.class);
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final Random random = new Random();

    private final String brokerUrl;
    private final String deviceId;
    // topic MQTT para publicas as metricas do dispositivo
    private final String topic;
    private final int intervalSeconds;
    
    private MqttClient client;
    private final ScheduledExecutorService scheduler;
    
    private double currentTemperature;
    private double currentHumidity;

    public MqttSensorSimulator(String brokerUrl, String deviceId, int intervalSeconds, ScheduledExecutorService scheduler) {
        this.brokerUrl = brokerUrl;
        this.deviceId = deviceId;
        this.topic = "sensors/" + deviceId;
        this.intervalSeconds = intervalSeconds;
        this.scheduler = scheduler;
        // estado inicial realista
        this.currentTemperature = 18.0 + random.nextDouble() * 10.0; //18-28
        this.currentHumidity = 40.0 + random.nextDouble() * 30.0; // 40%-70%
    }

    /**
     * Inicia a conexão MQTT e agenda o envio periódico de métricas.
     */
    public void start() {
        try {
            client = new MqttClient(brokerUrl, deviceId, new MemoryPersistence());
            
            MqttConnectOptions options = new MqttConnectOptions();

            
            options.setAutomaticReconnect(true); // reconexao automatica
            options.setCleanSession(true); // nao guarda estado antigo
            options.setConnectionTimeout(10); // timeout de conexao 10s
            
            // mensagens de aviso do estado 
            client.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    logger.warn("Connection lost: {}", cause.getMessage());
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) {
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    logger.debug("Message delivered successfully");
                }
            });

            client.connect(options);
            logger.info("Connected to MQTT broker: {}", brokerUrl);

            scheduler.scheduleAtFixedRate(this::sendMetric, 0, intervalSeconds, TimeUnit.SECONDS);
            
            logger.info("Sensor {} started, sending metrics every {} seconds to topic: {}", 
                    deviceId, intervalSeconds, topic);

        } catch (MqttException e) {
            logger.error("Failed to connect to MQTT broker: {}", e.getMessage());
        }
    }

    /**
     * Envia uma métrica única ao broker MQTT.
     * Gera novos valores de temperatura e humidade com variação gradual,
     * serializa para JSON e publica no tópico do dispositivo.
     */
    private void sendMetric() {
        try {
            // Gera valores com variação gradual para simular comportamento real
            currentTemperature = generateGradualValue(currentTemperature, 15.0, 30.0, 0.5); // +-0.5
            currentHumidity = generateGradualValue(currentHumidity, 30.0, 80.0, 2.0); // +- 2.0

            // constroi DTO JSON 
            MetricDTO metric = new MetricDTO();
            metric.setDeviceId(deviceId);
            metric.setTemperature(Math.round(currentTemperature * 100.0) / 100.0);
            metric.setHumidity(Math.round(currentHumidity * 100.0) / 100.0);
            metric.setTimestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

            String payload = mapper.writeValueAsString(metric);
            MqttMessage message = new MqttMessage(payload.getBytes());
            // configura QoS 1 (At least once)
            message.setQos(1);

            client.publish(topic, message);
            logger.info("Published: {} -> temp={}, humidity={}", 
                    deviceId, metric.getTemperature(), metric.getHumidity());

        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize metric for {}: {}", deviceId, e.getMessage());
        } catch (MqttException e) {
            logger.error("Failed to publish metric: {}", e.getMessage());
        }
    }

    /**
     * Gera um valor gradual dentro de um intervalo com variação controlada.
     * Simula comportamento realista de sensores com mudanças suaves.
     */
    private double generateGradualValue(double current, double min, double max, double maxChange) {
        double change = (random.nextDouble() - 0.5) * 2 * maxChange;
        double newValue = current + change;
        return Math.max(min, Math.min(max, newValue));
    }

    /**
     * Para o simulador interrompendo a transferência de dados e encerrando a conexão MQTT.
     */
    public void stop() {
        try {
            if (client != null) {
                client.disconnect();
                client.close();
            }
            logger.info("Device {} stopped", deviceId);
        } catch (MqttException e) {
            logger.error("Error stopping device: {}", e.getMessage());
        }
    }

    /**
     * Ponto de entrada para execução do simulador como aplicação standalone.
     * Cria um supervisor que gerencia múltiplos dispositivos simulados.
     * Executa por 30 segundos e depois encerra automaticamente (se ativado via AUTO_SHUTDOWN_ENABLED).
     */
    public static void main(String[] args) {
        MqttClientConfig config = new MqttClientConfig();
        MqttSuperviser supervisor = new MqttSuperviser(config);
        supervisor.start();

        if (config.enableTimedExecution) {
            logger.info("Client MQTT will run for {} seconds", config.runDurationSeconds);
            
            try {
                // Aguarda antes de encerrar
                Thread.sleep(config.runDurationSeconds * 1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            logger.info("Stopping MQTT client after {} seconds", config.runDurationSeconds);
            supervisor.stop();
            System.exit(0);
        } else {
            logger.info("Client MQTT running indefinitely (auto-shutdown disabled)");
            
            // Mantém aplicação ativa sem encerramento automático
            Runtime.getRuntime().addShutdownHook(new Thread(supervisor::stop));
            try {
                Thread.currentThread().join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}

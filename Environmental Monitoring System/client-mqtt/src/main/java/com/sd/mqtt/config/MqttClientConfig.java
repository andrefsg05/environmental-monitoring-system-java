package com.sd.mqtt.config;

/**
 * Configuração do cliente MQTT.
 * Define parâmetros de conexão ao broker MQTT e intervalo de polling com o servidor de registro de dispositivos.
 */
public class MqttClientConfig {

    public final String brokerUrl = "tcp://localhost:1883";
    
    /** Intervalo em segundos para envio de métricas ao broker MQTT */
    public final int sendIntervalSeconds = 5;
    
    /** Intervalo em segundos para sincronização com registro de dispositivos */
    public final int pollIntervalSeconds = 10;
    
    /** URL do servidor de registro de dispositivos */
    public final String registryUrl = "http://localhost:8080";
    
    /** Número de threads para processamento concorrente */
    public final int numberOfThreads = 5;
    
    /** Tempo de execução do cliente em segundos */
    public final int runDurationSeconds = 60;
    
    /** Ativar/desativar execução por tempo limitado (true = ativa timer, false = execução contínua) */
    public final boolean enableTimedExecution = false;
    
}

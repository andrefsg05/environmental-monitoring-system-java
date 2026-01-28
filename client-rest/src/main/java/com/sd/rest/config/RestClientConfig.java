package com.sd.rest.config;

/**
 * Configuração centralizada para o cliente REST.
 * Define parâmetros de conexão ao servidor REST, intervalo de envio de métricas,
 * número de tentativas de reenvio e intervalo de polling com o servidor de registro.
 */
public class RestClientConfig {

    public final String serverUrl = "http://localhost:8080";
    
    // Intervalo de tempo para envio de métricas ao servidor
    public final int sendIntervalSeconds = 5;
    
    // Número máximo de tentativas de reenvio em caso de falha
    public final int maxRetries = 3;
    
    // Intervalo de tempo para sincronização com registro de dispositivos
    public final int pollIntervalSeconds = 10;
    
    // Número de threads para processamento concorrente
    public final int numberOfThreads = 5;
    
    // Tempo de execução do cliente em segundos
    public final int runDurationSeconds = 60;
    
    // Ativar/desativar execução por tempo limitado (true = ativa timer, false = execução contínua)
    public final boolean enableTimedExecution = false;
    
}


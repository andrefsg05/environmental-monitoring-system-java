package com.sd.grpc.config;

/**
 * Configuração centralizada para o cliente gRPC.
 * Define parâmetros de conexão ao servidor gRPC, intervalo de envio de métricas
 * e intervalo de polling com o servidor de registro de dispositivos.
 */
public class GrpcClientConfig {

    public final String serverHost = "localhost";
    public final int serverPort = 9090;
    
    // Intervalo de tempo para envio de métricas ao servidor gRPC
    public final int sendIntervalSeconds = 5;
    
    // Intervalo de tempo para sincronização com registro de dispositivos
    public final int pollIntervalSeconds = 10;
    
    // URL do servidor de registro de dispositivos
    public final String registryUrl = "http://localhost:8080";
    
    // Número de threads para processamento concorrente
    public final int numberOfThreads = 5;
    
    // Tempo de execução do cliente em segundos
    public final int runDurationSeconds = 60;
    
    // Ativar/desativar execução por tempo limitado (true = ativa timer, false = execução contínua)
    public final boolean enableTimedExecution = false;
    
}

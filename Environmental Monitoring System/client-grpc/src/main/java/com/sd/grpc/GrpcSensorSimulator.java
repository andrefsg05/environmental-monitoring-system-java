package com.sd.grpc;

import java.time.LocalDateTime; // Data/hora atual
import java.time.format.DateTimeFormatter; // Formatar datas 
import java.util.Random; // Gerar valores aleatórios
import java.util.concurrent.ScheduledExecutorService; // Agenda tarefas periódicas
import java.util.concurrent.TimeUnit; // Unidades de tempo

import org.slf4j.Logger; // Interface de logging
import org.slf4j.LoggerFactory; // Factory de loggers

import com.sd.grpc.config.GrpcClientConfig; // Config gRPC

import io.grpc.ManagedChannel; // Canal gRPC
import io.grpc.ManagedChannelBuilder; // Builder do canal
import io.grpc.StatusRuntimeException; // Exceção gRPC

/**
 * Simulador de sensor para protocolo gRPC.
 * Conecta a um servidor gRPC, gera métricas simuladas de temperatura e humidade,
 * e as envia periodicamente via chamadas RPC síncronas (blocking stub).
 * As métricas são geradas com variações graduais realistas.
 */

public class GrpcSensorSimulator {

    private static final Logger logger = LoggerFactory.getLogger(GrpcSensorSimulator.class);
    private static final Random random = new Random();

    private final String serverHost;
    private final int serverPort;
    private final String deviceId;
    private final int intervalSeconds;

    //canal de comunicação gRPC cli/serv para enviar chamadas RPC ao servidor
    private ManagedChannel channel;
    // para comunicaçao sincrona com o servidor
    private MetricsServiceGrpc.MetricsServiceBlockingStub blockingStub;
    private final ScheduledExecutorService scheduler;

    private double currentTemperature;
    private double currentHumidity;

    public GrpcSensorSimulator(String serverHost, int serverPort, String deviceId,
                                int intervalSeconds, ScheduledExecutorService scheduler) {
        this.serverHost = serverHost;
        this.serverPort = serverPort;
        this.deviceId = deviceId;
        this.intervalSeconds = intervalSeconds;
        this.scheduler = scheduler;
        // estado inicial realista
        this.currentTemperature = 18.0 + random.nextDouble() * 10.0; //18-28
        this.currentHumidity = 40.0 + random.nextDouble() * 30.0; // 40%-70%
    }

    public GrpcSensorSimulator(String deviceId, int intervalSeconds, ScheduledExecutorService scheduler, String serverHost, int serverPort) {
        this.deviceId = deviceId;
        this.intervalSeconds = intervalSeconds;
        this.scheduler = scheduler;
        this.serverHost = serverHost;
        this.serverPort = serverPort;
    }

    /**
     * Inicia a conexão gRPC e agenda o envio periódico de métricas.
     * Estabelece channel gRPC em modo plaintext e cria stub síncrono.
     */
    public void start() {
        try {
            channel = ManagedChannelBuilder.forAddress(serverHost, serverPort)
                    .usePlaintext()
                    .build();
                    
            blockingStub = MetricsServiceGrpc.newBlockingStub(channel);

            scheduler.scheduleAtFixedRate(this::sendMetric, 0, intervalSeconds, TimeUnit.SECONDS);

            logger.info("Device {} started, sending metrics every {} seconds", deviceId, intervalSeconds);
        } catch (Exception e) {
            logger.error("Failed to connect to gRPC server: {}", e.getMessage());
        }
    }

    /**
     * Envia uma métrica única via gRPC.
     * Gera novos valores de temperatura e humidade com variação gradual,
     * constrói MetricRequest e envia via blocking stub (chamada síncrona).
     */
    private void sendMetric() {
        try {
            // Gera valores com variação gradual para simular comportamento real
            currentTemperature = generateGradualValue(currentTemperature, 15.0, 30.0, 0.5); // +-0.5
            currentHumidity = generateGradualValue(currentHumidity, 30.0, 80.0, 2.0); // +- 2.0

            // constroi request gRPC protobuffer
            MetricRequest request = MetricRequest.newBuilder()
                    .setDeviceId(deviceId)
                    .setTemperature(Math.round(currentTemperature * 100.0) / 100.0)
                    .setHumidity(Math.round(currentHumidity * 100.0) / 100.0)
                    .setTimestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                    .build();

            MetricResponse response = blockingStub.sendMetric(request);
            if (response.getSuccess()) {
                logger.info("gRPC metric sent: {} -> temp={}, humidity={}", deviceId, request.getTemperature(), request.getHumidity());
            } else {
                logger.warn("gRPC metric rejected: {}", response.getMessage());
            }
        } catch (StatusRuntimeException e) {
            logger.error("gRPC call failed: {}", e.getStatus());
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
     * Para o simulador interrompendo a transferência de dados e encerrando o channel gRPC.
     */
    public void stop() {
        try {
            if (channel != null) channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
            logger.info("Device {} stopped", deviceId);
        } catch (InterruptedException e) {
            logger.error("Error stopping device: {}", e.getMessage());
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Ponto de entrada para execução do simulador como aplicação standalone.
     * Cria um supervisor que gerencia múltiplos dispositivos simulados.
     * Executa por X segundos e depois encerra automaticamente (se ativado via AUTO_SHUTDOWN_ENABLED).
     * ENCERRAMENTO AUTOMÁTICO PARA TESTE DE PERFORMANCE
     */
    public static void main(String[] args) {
        GrpcClientConfig config = new GrpcClientConfig();
        GrpcSuperviser supervisor = new GrpcSuperviser(config);
        supervisor.start();

        if (config.enableTimedExecution) {
            logger.info("Client gRPC will run for {} seconds", config.runDurationSeconds);
            
            // Agenda parada automática após o tempo especificado
            ScheduledExecutorService scheduler = java.util.concurrent.Executors.newScheduledThreadPool(1);
            scheduler.schedule(() -> {
                logger.info("Stopping gRPC client after {} seconds", config.runDurationSeconds);
                supervisor.stop();
                scheduler.shutdownNow();
                System.exit(0);
            }, config.runDurationSeconds, TimeUnit.SECONDS);
        } else {
            logger.info("Client gRPC running indefinitely (auto-shutdown disabled)");
            
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

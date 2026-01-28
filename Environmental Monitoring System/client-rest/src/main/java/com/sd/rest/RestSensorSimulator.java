package com.sd.rest;


import com.sd.rest.config.*; // Config REST
import com.sd.rest.dto.*; // DTOs

import java.time.LocalDateTime; // Data/hora atual
import java.time.format.DateTimeFormatter; // Formatar datas 
import java.util.Random; // Gerar valores aleatórios
import java.util.concurrent.ScheduledExecutorService; // Agenda tarefas periódicas
import java.util.concurrent.ScheduledFuture; // Representa uma tarefa agendada
import java.util.concurrent.TimeUnit; // Unidades de tempo

import org.apache.hc.client5.http.classic.methods.HttpPost; // Método POST
import org.apache.hc.client5.http.config.RequestConfig; // Config de requisição
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient; // Cliente HTTP
import org.apache.hc.client5.http.impl.classic.HttpClients; // Factory HTTP
import org.apache.hc.core5.http.ContentType; // Tipo de conteúdo
import org.apache.hc.core5.http.io.entity.EntityUtils; // Utilidades para corpos
import org.apache.hc.core5.http.io.entity.StringEntity; // Corpo em string
import org.apache.hc.core5.util.Timeout; // Configuração de timeout
import org.slf4j.Logger; // Interface de logging
import org.slf4j.LoggerFactory; // Factory de loggers

import com.fasterxml.jackson.core.JsonProcessingException; // Exceção JSON
import com.fasterxml.jackson.databind.ObjectMapper; // Serialização JSON

/**
 * Simulador de sensor para protocolo REST.
 * Conecta a um servidor REST, gera métricas simuladas de temperatura e humidade,
 * e as envia periodicamente via HTTP POST com suporte a retry automático.
 * Implementa backoff exponencial para tentativas falhadas.
 */
public class RestSensorSimulator {

    private static final Logger logger = LoggerFactory.getLogger(RestSensorSimulator.class);
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final Random random = new Random();

    private final String serverUrl; // URL base do servidor REST
    private final String deviceId;
    private final int intervalSeconds;
    private final int maxRetries;
    
    private CloseableHttpClient httpClient; 
    private final ScheduledExecutorService scheduler;
    private ScheduledFuture<?> sendTask;
    
    private double currentTemperature;
    private double currentHumidity;

    public RestSensorSimulator(String serverUrl, String deviceId, int intervalSeconds, int maxRetries, ScheduledExecutorService scheduler) {
        this.serverUrl = serverUrl;
        this.deviceId = deviceId;
        this.intervalSeconds = intervalSeconds;
        this.maxRetries = maxRetries;
        this.scheduler = scheduler;
        
        this.currentTemperature = 18.0 + random.nextDouble() * 10.0;
        this.currentHumidity = 40.0 + random.nextDouble() * 30.0;
    }

    /**
     * Inicia o simulador configurando cliente HTTP com timeouts e agendando envios periódicos.
     * Define timeouts de 5 segundos para conexão e 10 segundos para resposta.
     */
    public void start() {
        try {
            RequestConfig config = RequestConfig.custom()
                    .setConnectionRequestTimeout(Timeout.ofSeconds(5)) // timeout conexao
                    .setResponseTimeout(Timeout.ofSeconds(10)) // timeout resposta
                    .build();
            
                httpClient = HttpClients.custom()
                    .setDefaultRequestConfig(config)
                    .build();
            
            logger.info("HTTP client configured for: {}", serverUrl);

                sendTask = scheduler.scheduleAtFixedRate(this::sendMetricWithRetry, 0, intervalSeconds, TimeUnit.SECONDS);
            
            logger.info("REST device {} started, sending metrics every {} seconds", 
                    deviceId, intervalSeconds);
        } catch (Exception e) {
            logger.error("Failed to start REST simulator: {}", e.getMessage());
        }
    }

    /**
     * Envia métrica com retry automático e backoff de 1s.
     * Tenta enviar até maxRetries vezes, aguardando incrementalmente mais entre tentativas.
     */
    private void sendMetricWithRetry() {
        int retries = 0;
        boolean success = false;

        while (!success && retries <= maxRetries) {
            try {
                success = sendMetric();
                if (!success && retries < maxRetries) {
                    logger.warn("Retrying... attempt {}/{}", retries + 1, maxRetries);
                    // aguarda 1s
                    Thread.sleep(1000);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                logger.error("Attempt {} failed: {}", retries + 1, e.getMessage());
            }
            retries++;
        }

        if (!success) {
            logger.error("Failed to send metric after {} attempts", maxRetries + 1);
        }
    }

    /**
     * Envia uma métrica única ao servidor REST.
     * Gera novos valores de temperatura e humidade com variação gradual,
     * serializa para JSON e publica via HTTP POST para /api/metrics/ingest.
     */
    private boolean sendMetric() {
        try {
            // Gera valores com variação gradual para simular comportamento real
            currentTemperature = generateGradualValue(currentTemperature, 15.0, 30.0, 0.5); // +-0.5
            currentHumidity = generateGradualValue(currentHumidity, 30.0, 80.0, 2.0); // +- 2.0


            // constrói DTO JSON
            MetricDTO metric = new MetricDTO();
            metric.setDeviceId(deviceId);
            metric.setTemperature(Math.round(currentTemperature * 100.0) / 100.0);
            metric.setHumidity(Math.round(currentHumidity * 100.0) / 100.0);
            metric.setTimestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

            String payload = mapper.writeValueAsString(metric);
            
            HttpPost request = new HttpPost(serverUrl + "/api/metrics/ingest");
            request.setEntity(new StringEntity(payload, ContentType.APPLICATION_JSON));

            return httpClient.execute(request, response -> {
                int statusCode = response.getCode();
                String responseBody = EntityUtils.toString(response.getEntity());
                
                // 200 = ok, 400 = bad request (rejeitado)
                if (statusCode == 200) {
                    logger.info("REST metric sent: {} -> temp={}, humidity={}", 
                            deviceId, metric.getTemperature(), metric.getHumidity());
                    return true;
                } else if (statusCode == 400) {
                    logger.warn("REST metric rejected (400): {}", responseBody);
                    return true;
                } else {
                    logger.warn("HTTP {} response: {}", statusCode, responseBody);
                    return false;
                }
            });

        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize REST metric for {}: {}", deviceId, e.getMessage());
            return false;
        } catch (Exception e) {
            logger.error("Failed to send REST metric: {}", e.getMessage());
            return false;
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
     * Para o simulador encerrando a transferência de dados e fechando cliente HTTP.
     */
    public void stop() {
        try {
            if (sendTask != null) {
                sendTask.cancel(true);
            }
            if (httpClient != null) {
                httpClient.close();
            }
            logger.info("Device {} stopped", deviceId);
        } catch (Exception e) {
            logger.error("Error stopping device: {}", e.getMessage());
        }
    }

    /**
     * Ponto de entrada para execução do simulador como aplicação standalone.
     * Cria um supervisor que gerencia múltiplos dispositivos simulados.
     * Executa por 30 segundos e depois encerra automaticamente (se ativado via AUTO_SHUTDOWN_ENABLED).
     */
    public static void main(String[] args) {
        RestClientConfig config = new RestClientConfig();
        RestSuperviser supervisor = new RestSuperviser(config);
        supervisor.start();

        if (config.enableTimedExecution) {
            logger.info("Client REST will run for {} seconds", config.runDurationSeconds);
            
            // Agenda parada automática após o tempo especificado
            ScheduledExecutorService scheduler = java.util.concurrent.Executors.newScheduledThreadPool(1);
            scheduler.schedule(() -> {
                logger.info("Stopping REST client after {} seconds", config.runDurationSeconds);
                supervisor.stop();
                scheduler.shutdownNow();
                System.exit(0);
            }, config.runDurationSeconds, TimeUnit.SECONDS);
        } else {
            logger.info("Client REST running indefinitely (auto-shutdown disabled)");
            
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

package com.sd.mqtt;

import com.sd.mqtt.dto.*; // DTOs
import com.sd.mqtt.config.*; // Config MQTT

import com.fasterxml.jackson.core.type.TypeReference; // Desserialização genérica
import com.fasterxml.jackson.databind.ObjectMapper; // Serialização JSON
import org.slf4j.Logger; // Interface de logging
import org.slf4j.LoggerFactory; // Factory de loggers

import java.net.URI; // URI para HTTP
import java.net.http.HttpClient; // Cliente HTTP
import java.net.http.HttpRequest; // Requisição HTTP
import java.net.http.HttpResponse; // Resposta HTTP
import java.util.HashSet; // Conjuntos de hash
import java.util.List; // Listas
import java.util.Map; // Mapas
import java.util.Set; // Interfaces de conjunto
import java.util.concurrent.ConcurrentHashMap; // Mapa thread-safe
import java.util.concurrent.Executors; // Factory de executores
import java.util.concurrent.ScheduledExecutorService; // Serviço com agendamento
import java.util.concurrent.ThreadFactory; // Factory de threads
import java.util.concurrent.TimeUnit; // Unidades de tempo
import java.util.concurrent.atomic.AtomicInteger; // Utilizado para contagem atômica

/**
 * Supervisor que gerencia múltiplos simuladores MQTT dinamicamente.
 * Periodicamente sincroniza com o servidor para obter lista de dispositivos ativos,
 * iniciando novos simuladores para dispositivos adicionados e parando aqueles removidos ou desativados.
 * Utiliza cache local para manter estado dos simuladores em execução.
 */
public class MqttSuperviser {

    private static final Logger logger = LoggerFactory.getLogger(MqttSuperviser.class);

    private final MqttClientConfig config;
    //Armazena simuladores ativos por deviceId
    private final Map<String, MqttSensorSimulator> simulators = new ConcurrentHashMap<>();
    private final HttpClient registryClient;
    private final ObjectMapper mapper;
    private final ScheduledExecutorService poller;
    private final ScheduledExecutorService sharedPool;

    /**
     * Construtor que inicializa o supervisor com configuração.
     * Cria clientes HTTP e thread pools para gerenciamento de dispositivos.
     * 
     * @param config configuração MQTT com URLs e intervalos
     */
    public MqttSuperviser(MqttClientConfig config) {
        this.config = config;
        this.registryClient = HttpClient.newHttpClient();
        this.mapper = new ObjectMapper();
        this.poller = Executors.newSingleThreadScheduledExecutor(new CustomThread("S"));
        this.sharedPool = Executors.newScheduledThreadPool(config.numberOfThreads, new CustomThread("D"));
    }

    /**
     * Inicia o supervisor realizando sincronização inicial e agendando polling periódico.
     * Sincroni za imediatamente com o servidor antes de começar o polling automático.
     */
    public void start() {
        syncWithRegistry();
        poller.scheduleAtFixedRate(this::safeSync, 
            config.pollIntervalSeconds, config.pollIntervalSeconds, TimeUnit.SECONDS);
        logger.info("MQTT supervisor started. Polling {} every {} seconds", 
            config.registryUrl, config.pollIntervalSeconds);
    }

    /**
     * Para o supervisor encerrando todos os simuladores e thread pools.
     * Realiza shutdown limpo de todos os recursos.
     */
    public void stop() {
        poller.shutdownNow();
        simulators.values().forEach(MqttSensorSimulator::stop);
        simulators.clear();
        sharedPool.shutdownNow();
    }

    /**
     * Realiza sincronização segura com o servidor, capturando exceções.
     * Utilizado para execução periódica com tratamento de erros.
     */
    private void safeSync() {
        try {
            syncWithRegistry();
        } catch (Exception e) {
            logger.warn("Failed to sync device registry: {}", e.getMessage());
        }
    }

    /**
     * Sincroniza estado local de simuladores com lista de dispositivos ativos do servidor.
     * Inicia novos simuladores para dispositivos não encontrados localmente,
     * e para simuladores cujos dispositivos foram removidos ou desativados.
     */
    private void syncWithRegistry() {
        List<DeviceDTO> devices = fetchDevices();
        logger.info("Sync: found {} MQTT devices", devices.size());

        Set<String> activeIds = new HashSet<>();
        for (DeviceDTO device : devices) {
            if (device.isActive()) {
                String deviceId = device.getId();
                activeIds.add(deviceId);
                simulators.computeIfAbsent(deviceId, missingId -> {
                    logger.info("Starting MQTT device thread for {}", missingId);
                    MqttSensorSimulator simulator = new MqttSensorSimulator(
                        config.brokerUrl,
                        missingId,
                        config.sendIntervalSeconds,
                        sharedPool
                    );
                    simulator.start();
                    return simulator;
                });
            }
        }

        // Remove simuladores de dispositivos que não estão mais ativos
        simulators.entrySet().removeIf(entry -> {
            if (!activeIds.contains(entry.getKey())) {
                logger.info("Stopping MQTT device thread for {} (removed or inactive)", entry.getKey());
                entry.getValue().stop();
                return true;
            }
            return false;
        });
    }

    /**
     * Obtém lista de dispositivos MQTT ativos do servidor de registro.
     * Realiza chamada HTTP GET para endpoint /api/devices/active/MQTT.
     */
    private List<DeviceDTO> fetchDevices() {
        try {
            String url = config.registryUrl + "/api/devices/active/MQTT";
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

            HttpResponse<String> response = registryClient.send(request, 
                HttpResponse.BodyHandlers.ofString());

            // 200 = ok
            if (response.statusCode() == 200) {
                return mapper.readValue(response.body(), 
                    new TypeReference<List<DeviceDTO>>() {});
            } else {
                logger.warn("Failed to fetch devices: HTTP {}", response.statusCode());
                return List.of();
            }
        } catch (Exception e) {
            logger.error("Error fetching devices: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * Threads nomeadas com padrão consistente.
     * Threads são marcadas como daemon para encerrar com a aplicação.
     */
    private static class CustomThread implements ThreadFactory {
        private final AtomicInteger counter = new AtomicInteger(1);
        private final String prefix;

        CustomThread(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r);
            thread.setName(prefix + "-t" + counter.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        }
    }
}

package com.sd.rest;

import com.sd.rest.config.*; // Config REST
import com.sd.rest.dto.*; // DTOs

import java.io.IOException; // Exceções de I/O
import java.util.HashSet; // Conjuntos hash
import java.util.List; // Listas
import java.util.Map; // Mapas
import java.util.Set; // Interfaces de conjunto
import java.util.concurrent.ConcurrentHashMap; // Mapa thread-safe
import java.util.concurrent.Executors; // Factory de executores
import java.util.concurrent.ScheduledExecutorService; // Serviço com agendamento
import java.util.concurrent.ThreadFactory; // Factory de threads
import java.util.concurrent.TimeUnit; // Unidades de tempo
import java.util.concurrent.atomic.AtomicInteger; // Integer atômico para contagem

import org.apache.hc.client5.http.classic.methods.HttpGet; // Método GET
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient; // Cliente HTTP
import org.apache.hc.client5.http.impl.classic.HttpClients; // Factory HTTP
import org.apache.hc.core5.http.io.entity.EntityUtils; // Utilidades para corpos
import org.slf4j.Logger; // Interface de logging
import org.slf4j.LoggerFactory; // Factory de loggers

import com.fasterxml.jackson.core.type.TypeReference; // Desserialização genérica
import com.fasterxml.jackson.databind.ObjectMapper; // Serialização JSON

/**
 * Supervisor que gerencia múltiplos simuladores REST dinamicamente.
 * Periodicamente sincroniza com o servidor para obter lista de dispositivos ativos,
 * iniciando novos simuladores para dispositivos adicionados e parando aqueles removidos ou desativados.
 * Utiliza cache local para manter estado dos simuladores em execução.
 */
public class RestSuperviser {

    private static final Logger logger = LoggerFactory.getLogger(RestSuperviser.class);

    private final RestClientConfig config;
    //Armazena simuladores ativos por deviceId
    private final Map<String, RestSensorSimulator> simulators = new ConcurrentHashMap<>();
    private final CloseableHttpClient registryClient;
    private final ObjectMapper mapper;
    private final ScheduledExecutorService poller;
    private final ScheduledExecutorService sharedPool;

    /**
     * Construtor que inicializa o supervisor com configuração.
     * Cria clientes HTTP e thread pools para gerenciamento de dispositivos.
     * 
     * @param config configuração REST com URLs e intervalos
     */
    public RestSuperviser(RestClientConfig config) {
        this.config = config;
        this.registryClient = HttpClients.createDefault();
        this.mapper = new ObjectMapper();
        this.poller = Executors.newSingleThreadScheduledExecutor(new CustomThread("S"));
        this.sharedPool = Executors.newScheduledThreadPool(config.numberOfThreads, new CustomThread("D"));
    }

    /**
     * Inicia o supervisor realizando sincronização inicial e agendando polling periódico.
     * Sincroniza imediatamente com o servidor antes de começar o polling automático.
     */
    public void start() {
        syncWithRegistry();
        poller.scheduleAtFixedRate(this::safeSync, 
            config.pollIntervalSeconds, config.pollIntervalSeconds, TimeUnit.SECONDS);
        logger.info("REST supervisor started. Polling {} every {} seconds", 
            config.serverUrl, config.pollIntervalSeconds);
    }

    /**
     * Para o supervisor encerrando todos os simuladores e thread pools.
     * Realiza shutdown limpo de todos os recursos incluindo cliente HTTP.
     */
    public void stop() {
        poller.shutdownNow();
        simulators.values().forEach(RestSensorSimulator::stop);
        simulators.clear();
        sharedPool.shutdownNow();
        try {
            registryClient.close();
        } catch (IOException e) {
            logger.warn("Failed to close registry client: {}", e.getMessage());
        }
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
        logger.info("Sync: found {} REST devices", devices.size());

        Set<String> activeIds = new HashSet<>();
        for (DeviceDTO device : devices) {
            if (device.isActive()) {
                String deviceId = device.getId();
                activeIds.add(deviceId);
                simulators.computeIfAbsent(deviceId, missingId -> {
                    logger.info("Starting REST device thread for {}", missingId);
                    RestSensorSimulator simulator = new RestSensorSimulator(
                        config.serverUrl,
                        missingId,
                        config.sendIntervalSeconds,
                        config.maxRetries,
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
                logger.info("Stopping REST device thread for {} (removed or inactive)", entry.getKey());
                entry.getValue().stop();
                return true;
            }
            return false;
        });
    }

    /**
     * Obtém lista de dispositivos REST ativos do servidor de registro.
     * Realiza chamada HTTP GET para endpoint /api/devices/active/REST.
     */
    private List<DeviceDTO> fetchDevices() {
        try {
            String url = config.serverUrl + "/api/devices/active/REST";
            HttpGet request = new HttpGet(url);

            return registryClient.execute(request, response -> {
                int statusCode = response.getCode();
                // 200 = ok
                if (statusCode == 200) {
                    String body = EntityUtils.toString(response.getEntity());
                    return mapper.readValue(body, new TypeReference<List<DeviceDTO>>() {});
                } else {
                    logger.warn("Failed to fetch devices: HTTP {}", statusCode);
                    return List.of();
                }
            });
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

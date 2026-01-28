package com.sd.server.service;

import java.util.EnumMap; // Mapa otimizado para enums
import java.util.List; // Listas
import java.util.Map; // Mapas
import java.util.concurrent.ConcurrentHashMap; // Mapa thread-safe 
import java.util.concurrent.Executors; // Factory de executores
import java.util.concurrent.ScheduledExecutorService; // Serviço com agendamento
import java.util.concurrent.TimeUnit; // Unidades de tempo

import org.slf4j.Logger; // Interface de logging
import org.slf4j.LoggerFactory; // Factory de loggers
import org.springframework.beans.factory.annotation.Value; // Injeção de valores (properties)
import org.springframework.stereotype.Service; // Marca como serviço Spring

import com.sd.server.dto.DeviceDTO; // DTO de dispositivo
import com.sd.server.entity.Device; // Entidade dispositivo

import jakarta.annotation.PostConstruct; // Callback após construção
import jakarta.annotation.PreDestroy; // Callback antes de destruição


/**
 * Service que mantém cache em memória de dispositivos ativos por protocolo.
 * Realiza refresh periódico para sincronização com banco de dados.
 * Utilizado pelos supervisores para descoberta dinâmica de dispositivos.
 */
@Service
public class DeviceMonitorService {

    private static final Logger logger = LoggerFactory.getLogger(DeviceMonitorService.class);

    private final DeviceService deviceService;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    // Map: Protocol -> (DeviceId -> DeviceDTO)
    private final Map<Device.ProtocolType, Map<String, DeviceDTO>> activeCache = new ConcurrentHashMap<>();

    @Value("${devices.monitor.refresh-seconds:10}")
    private long refreshSeconds;

    public DeviceMonitorService(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    /**
     * Chamado ao iniciar o Spring Bean.
     * Realiza primeiro refresh do cache e inicia scheduler periódico.
     */
    @PostConstruct
    public void start() {
        refreshCache();
        scheduler.scheduleAtFixedRate(this::safeRefresh, refreshSeconds, refreshSeconds, TimeUnit.SECONDS);
        logger.info("Device monitor started with refresh interval of {} seconds", refreshSeconds);
    }

    /**
     * Chamado ao destruir o Bean.
     * Encerra scheduler e libera recursos.
     */
    @PreDestroy
    public void stop() {
        scheduler.shutdownNow();
    }

    /**
     * Wrapper seguro para refresh que captura exceções e registra em log.
     */
    private void safeRefresh() {
        try {
            refreshCache();
        } catch (Exception e) {
            logger.warn("Failed to refresh device cache: {}", e.getMessage());
        }
    }

    /**
     * Atualiza o cache a partir do banco de dados.
     * Agrupa dispositivos ativos por protocolo.
     */
    private void refreshCache() {
        List<DeviceDTO> activeDevices = deviceService.getActiveDevices();
        // Cria novo map com estrutura vazia para cada protocolo
        Map<Device.ProtocolType, Map<String, DeviceDTO>> next = new EnumMap<>(Device.ProtocolType.class);
        for (Device.ProtocolType protocol : Device.ProtocolType.values()) {
            next.put(protocol, new ConcurrentHashMap<>());
        }
        // Agrupa dispositivos por protocolo
        for (DeviceDTO dto : activeDevices) {
            next.get(dto.getProtocol()).put(dto.getId(), dto);
        }

        // Substitui cache atomicamente
        activeCache.clear();
        activeCache.putAll(next);
        logger.debug("Device cache refreshed: {} active devices", activeDevices.size());
    }
}

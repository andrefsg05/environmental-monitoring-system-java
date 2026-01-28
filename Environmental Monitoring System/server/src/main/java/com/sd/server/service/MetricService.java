
package com.sd.server.service;

import java.time.LocalDateTime; // Data/hora
import java.time.format.DateTimeFormatter; // Formatar datas
import java.util.ArrayList; // Listas dinâmicas
import java.util.List; // Listas

import org.slf4j.Logger; // Interface de logging
import org.slf4j.LoggerFactory; // Factory de loggers
import org.springframework.beans.factory.annotation.Autowired; // Injeção de dependências
import org.springframework.stereotype.Service; // Marca como serviço Spring

import com.sd.server.dto.AverageMetricDTO; // DTO de métrica agregada
import com.sd.server.dto.MetricDTO; // DTO de métrica
import com.sd.server.entity.Device; // Entidade dispositivo
import com.sd.server.entity.Metric; // Entidade métrica
import com.sd.server.repository.MetricRepository; // Repositório de métricas

/**
 * Service para gerenciamento de métricas.
 * Fornece ingestão, agregação e consultas de dados de temperatura e humidade.
 * Valida dispositivos antes de armazenar métricas.
 */
@Service
public class MetricService {

    private static final Logger logger = LoggerFactory.getLogger(MetricService.class);

    @Autowired
    private MetricRepository metricRepository;

    @Autowired
    private DeviceService deviceService;

    /**
     * Ingere uma métrica recebida de um cliente.
     * Valida se dispositivo existe e está ativo antes de armazenar.
     * 
     * @param metricDTO dados da métrica (deviceId, temperature, humidity, timestamp)
     * @return true se métrica foi armazenada com sucesso, false se validação falhou
     */
    public boolean ingestMetric(MetricDTO metricDTO) {
        String deviceId = metricDTO.getDeviceId();
        Device device = deviceService.getDeviceEntity(deviceId).orElse(null);

        if (device == null) {
            logger.warn("Metric discarded: Device {} not registered", deviceId);
            return false;
        }

        if (!device.isActive()) {
            logger.warn("Metric discarded: Device {} is inactive", deviceId);
            return false;
        }

        Metric metric = metricDTO.toEntity(device);
        metricRepository.save(metric);

        logger.info("Metric saved for device {}: temp={}, humidity={}", 
                deviceId, metricDTO.getTemperature(), metricDTO.getHumidity());
        return true;
    }

    /**
     * Obtém métricas brutas (não agregadas) em um intervalo de tempo.
     * Se datas não informadas, retorna últimas 24 horas.
     * 
     * @param deviceId identificador do dispositivo
     * @param from data/hora inicial (opcional)
     * @param to data/hora final (opcional)
     * @return lista de MetricDTO dentro do intervalo
     */
    public List<MetricDTO> getRawMetrics(String deviceId, LocalDateTime from, LocalDateTime to) {
        if (from == null || to == null) {
            from = LocalDateTime.now().minusHours(24);
            to = LocalDateTime.now();
        }

        List<Metric> metrics = metricRepository.findByDeviceIdAndTimestampBetween(deviceId, from, to);
        List<MetricDTO> metricDTOs = new ArrayList<>();

        for (Metric metric : metrics) {
            metricDTOs.add(MetricDTO.fromEntity(metric));
        }

        return metricDTOs;
    }

    /**
     * Calcula métricas agregadas (médias, mínimos e máximos) de temperatura e umidade por nível especificado.
     * Suporta agregação por sala, departamento, andar ou prédio.
     * 
     * @param level tipo de agregação ("sala", "departamento", "piso", "edificio" ou em inglês)
     * @param id identificador da localidade (ex: número da sala, nome do dept)
     * @param from data/hora inicial (opcional, padrão: últimas 24h)
     * @param to data/hora final (opcional)
     * @return AverageMetricDTO com agregados calculados, ou null se nível inválido
     */
    public AverageMetricDTO getAverageMetrics(String level, String id, LocalDateTime from, LocalDateTime to) {
        if (from == null || to == null) {
            from = LocalDateTime.now().minusHours(24);
            to = LocalDateTime.now();
        }

        Object[] result;
        switch (level.toLowerCase()) {
            case "sala":
            case "room":
                result = metricRepository.getAverageByRoom(id, from, to);
                break;
            case "departamento":
            case "department":
                result = metricRepository.getAverageByDepartment(id, from, to);
                break;
            case "piso":
            case "floor":
                result = metricRepository.getAverageByFloor(id, from, to);
                break;
            case "edificio":
            case "building":
                result = metricRepository.getAverageByBuilding(id, from, to);
                break;
            default:
                return null;
        }

        if (result == null || result.length == 0) {
            logger.info("No result returned for level={}, id={}", level, id);
            return null;
        }

        logger.info("Query result for level={}, id={}: length={}, content={}", 
                level, id, result.length, java.util.Arrays.toString(result));

        Object[] actualResult = result;
        // Desembrulha result aninhado se necessário
        if (result.length == 1 && result[0] instanceof Object[]) {
            actualResult = (Object[]) result[0];
            logger.info("Unpacked nested result array: length={}", actualResult.length);
        }

        if (actualResult.length < 7) {
            logger.warn("Query returned incomplete result array for level={}, id={}, length={}", 
                level, id, actualResult.length);
            return null;
        }

        AverageMetricDTO dto = new AverageMetricDTO();
        dto.setLevel(level);
        dto.setId(id);
        
        Object temp = actualResult[0];
        Object hum = actualResult[1];
        Object cnt = actualResult[2];
        Object minTemp = actualResult[3];
        Object maxTemp = actualResult[4];
        Object minHum = actualResult[5];
        Object maxHum = actualResult[6];

        logger.debug("Parsed values: temp={}, hum={}, cnt={}, minTemp={}, maxTemp={}, minHum={}, maxHum={}",
            temp, hum, cnt, minTemp, maxTemp, minHum, maxHum);
        
        long count = cnt != null ? ((Number) cnt).longValue() : 0L;
        
        if (count == 0) {
            logger.info("No metrics found for level={}, id={}", level, id);
            return null;
        }
        
        dto.setAverageTemperature(temp != null ? ((Number) temp).doubleValue() : 0.0);
        dto.setAverageHumidity(hum != null ? ((Number) hum).doubleValue() : 0.0);
        dto.setMinTemperature(minTemp != null ? ((Number) minTemp).doubleValue() : 0.0);
        dto.setMaxTemperature(maxTemp != null ? ((Number) maxTemp).doubleValue() : 0.0);
        dto.setMinHumidity(minHum != null ? ((Number) minHum).doubleValue() : 0.0);
        dto.setMaxHumidity(maxHum != null ? ((Number) maxHum).doubleValue() : 0.0);
        dto.setCount(count);
        dto.setFromDate(from.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        dto.setToDate(to.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        logger.info("Successfully built AverageMetricDTO for level={}, id={}, temp(avg/min/max)={}/{}/{}, hum(avg/min/max)={}/{}/{}, count={}",
            level, id, dto.getAverageTemperature(), dto.getMinTemperature(), dto.getMaxTemperature(),
            dto.getAverageHumidity(), dto.getMinHumidity(), dto.getMaxHumidity(), count);

        return dto;
    }

    /**
     * Conta total de métricas recebidas por protocolo.
     * 
     * @param protocol tipo de protocolo (MQTT, GRPC, REST)
     * @return número total de métricas para o protocolo
     */
    public Long countMetricsByProtocol(String protocol) {
        Long count = metricRepository.countMetricsByProtocol(protocol);
        return count != null ? count : 0L;
    }

    /**
     * Calcula a latência média entre criação da métrica no cliente e recebimento no servidor.
     * Representa o tempo médio de comunicação/processamento por protocolo.
     * 
     * @param protocol tipo de protocolo (MQTT, GRPC, REST)
     * @return latência média em milissegundos, ou 0.0 se sem dados
     */
    public Double getAverageLatencyByProtocol(String protocol) {
        Double latency = metricRepository.getAverageLatencyByProtocol(protocol);
        return latency != null ? latency : 0.0;
    }

    /**
     * Deleta todas as métricas da base de dados.
     * Operação irreversível - deve ser usada com cautela.
     */
    public void deleteAllMetrics() {
        long count = metricRepository.count();
        metricRepository.deleteAllMetrics();
        logger.info("Deleted all metrics from database. Total metrics deleted: {}", count);
    }
}

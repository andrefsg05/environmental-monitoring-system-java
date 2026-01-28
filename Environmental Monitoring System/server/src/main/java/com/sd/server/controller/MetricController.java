
package com.sd.server.controller;

import java.time.LocalDateTime; // Data/hora
import java.util.List; // Listas
import java.util.Map; // Mapas

import org.springframework.beans.factory.annotation.Autowired; // Injeção de dependências
import org.springframework.format.annotation.DateTimeFormat; // Formato de data/hora
import org.springframework.http.ResponseEntity; // Resposta HTTP
import org.springframework.web.bind.annotation.DeleteMapping; // Mapeamento DELETE
import org.springframework.web.bind.annotation.GetMapping; // Mapeamento GET
import org.springframework.web.bind.annotation.PostMapping; // Mapeamento POST
import org.springframework.web.bind.annotation.RequestBody; // Corpo da requisição
import org.springframework.web.bind.annotation.RequestMapping; // Mapeamento base
import org.springframework.web.bind.annotation.RequestParam; // Parâmetro da requisição
import org.springframework.web.bind.annotation.RestController; // Controller REST

import com.sd.server.dto.AverageMetricDTO; // DTO de métrica agregada
import com.sd.server.dto.MetricDTO; // DTO de métrica
import com.sd.server.service.MetricService; // Serviço de métricas

import jakarta.validation.Valid; // Validação automática

/**
 * REST Controller para gerenciamento e consulta de métricas.
 * Fornece endpoints para ingestão de métricas e consultas de dados agregados.
 * Integra com clients MQTT, REST e gRPC para coleta de dados.
 */
@RestController
@RequestMapping("/api/metrics")
public class MetricController {

    @Autowired
    private MetricService metricService;

    /**
     * Ingere uma métrica enviada por um cliente.
     * Valida se dispositivo está ativo antes de armazenar.
     * 
     * @param metricDTO dados da métrica (deviceId, temperature, humidity, timestamp)
     * @return resposta com status de sucesso/erro
     */
    @PostMapping("/ingest")
    public ResponseEntity<?> ingestMetric(@Valid @RequestBody MetricDTO metricDTO) {
        boolean success = metricService.ingestMetric(metricDTO);
        if (success) {
            return ResponseEntity.ok(Map.of("status", "success", "message", "Metric ingested successfully"));
        } else {
            return ResponseEntity.badRequest()
                    .body(Map.of("status", "error", "message", "Device not registered or inactive"));
        }
    }

    /**
     * Obtém métricas agregadas (médias) de temperatura e umidade.
     * Suporta agregação por dispositivo, server ou global.
     * 
     * @param level tipo de agregação ("device", "server", ou "global")
     * @param id identificador (deviceId para "device", serverId para "server")
     * @param from período inicial (opcional, padrão: toda história)
     * @param to período final (opcional)
     * @return AverageMetricDTO com médias calculadas ou 400 se level inválido
     */
    @GetMapping("/average")
    public ResponseEntity<AverageMetricDTO> getAverageMetrics(
            @RequestParam String level,
            @RequestParam String id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        
        AverageMetricDTO result = metricService.getAverageMetrics(level, id, from, to);
        if (result == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(result);
    }

    /**
     * Obtém métricas brutas (não agregadas) de um dispositivo.
     * Permite consultas com intervalo de tempo específico.
     * 
     * @param deviceId identificador do dispositivo
     * @param from período inicial (opcional)
     * @param to período final (opcional)
     * @return lista de MetricDTO dentro do intervalo ou todas se sem datas
     */
    @GetMapping("/raw")
    public ResponseEntity<List<MetricDTO>> getRawMetrics(
            @RequestParam String deviceId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        
        List<MetricDTO> metrics = metricService.getRawMetrics(deviceId, from, to);
        return ResponseEntity.ok(metrics);
    }

    /**
     * Obtém contagem total de métricas por protocolo.
     * 
     * @param protocol tipo de protocolo (MQTT, GRPC, REST)
     * @return número total de métricas recebidas pelo protocolo
     */
    @GetMapping("/count-by-protocol")
    public ResponseEntity<Map<String, Object>> countByProtocol(@RequestParam String protocol) {
        Long count = metricService.countMetricsByProtocol(protocol);
        return ResponseEntity.ok(Map.of("protocol", protocol, "count", count));
    }

    /**
     * Obtém latência média entre criação da métrica no cliente e recebimento no servidor.
     * 
     * @param protocol tipo de protocolo (MQTT, GRPC, REST)
     * @return latência média em milissegundos
     */
    @GetMapping("/average-latency-by-protocol")
    public ResponseEntity<Map<String, Object>> averageLatencyByProtocol(@RequestParam String protocol) {
        Double latency = metricService.getAverageLatencyByProtocol(protocol);
        return ResponseEntity.ok(Map.of("protocol", protocol, "averageLatencyMs", latency));
    }

    /**
     * PARA TESTES DE PERFORMANCE
     * Deleta todas as métricas da base de dados.  
     * Operação irreversível - todas as métricas serão permanentemente removidas.
     * 
     * @return resposta com status de sucesso
     */
    @DeleteMapping
    public ResponseEntity<Map<String, String>> deleteAllMetrics() {
        metricService.deleteAllMetrics();
        return ResponseEntity.ok(Map.of("status", "success", "message", "All metrics deleted successfully"));
    }
}

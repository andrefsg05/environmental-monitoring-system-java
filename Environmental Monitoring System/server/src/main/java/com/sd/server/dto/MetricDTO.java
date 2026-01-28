
package com.sd.server.dto;
import java.time.LocalDateTime; // Data/hora

import com.sd.server.entity.Device; // Entidade dispositivo
import com.sd.server.entity.Metric; // Entidade métrica

import jakarta.validation.constraints.NotBlank; // Validação não vazio
import jakarta.validation.constraints.NotNull; // Validação não nulo

/**
 * DTO para transferência de dados de métricas.
 * Usado para recepcionar métricas de clientes via REST, MQTT, ou gRPC.
 * Contém validações para campos obrigatórios (deviceId, temperature, humidity).
 */
public class MetricDTO {

    @NotBlank(message = "Device ID is required")
    private String deviceId;

    @NotNull(message = "Temperature is required")
    private Double temperature;

    @NotNull(message = "Humidity is required")

    private Double humidity;

    private LocalDateTime timestamp;

    public MetricDTO() {
    }

    public MetricDTO(String deviceId, Double temperature, Double humidity) {
        this.deviceId = deviceId;
        this.temperature = temperature;
        this.humidity = humidity;
        this.timestamp = LocalDateTime.now();
    }

    public MetricDTO(String deviceId, Double temperature, Double humidity, LocalDateTime timestamp) {
        this.deviceId = deviceId;
        this.temperature = temperature;
        this.humidity = humidity;
        this.timestamp = timestamp;
    }

    /**
     * Converte entidade Metric em DTO.
     */
    public static MetricDTO fromEntity(Metric metric) {
        MetricDTO dto = new MetricDTO();
        dto.setDeviceId(metric.getDeviceId());
        dto.setTemperature(metric.getTemperature());
        dto.setHumidity(metric.getHumidity());
        dto.setTimestamp(metric.getTimestamp());
        return dto;
    }

    /**
     * Converte DTO em entidade Metric.
     * Usa timestamp atual se não definido no DTO.
     */
    public Metric toEntity(Device device) {
        Metric metric = new Metric();
        metric.setDevice(device);
        metric.setTemperature(this.temperature);
        metric.setHumidity(this.humidity);
        metric.setTimestamp(this.timestamp != null ? this.timestamp : LocalDateTime.now());
        return metric;
    }

    // Getters e Setters
    public String getDeviceId() {return deviceId;}
    public void setDeviceId(String deviceId) {this.deviceId = deviceId;}

    public Double getTemperature() {return temperature;}
    public void setTemperature(Double temperature) {this.temperature = temperature;}

    public Double getHumidity() {return humidity;}
    public void setHumidity(Double humidity) {this.humidity = humidity;}

    public LocalDateTime getTimestamp() {return timestamp;}
    public void setTimestamp(LocalDateTime timestamp) {this.timestamp = timestamp;}
}

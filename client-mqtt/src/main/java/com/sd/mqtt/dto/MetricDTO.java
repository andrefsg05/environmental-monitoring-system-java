package com.sd.mqtt.dto;

/**
 * Data Transfer Object (DTO) para métricas de sensores.
 * Representa uma leitura de sensor com temperatura, umidade e timestamp associado a um dispositivo.
 */
public class MetricDTO {

    private String deviceId;
    private double temperature;
    private double humidity;
    private String timestamp;


    public MetricDTO() {
    }

    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    public double getTemperature() { return temperature; }
    public void setTemperature(double temperature) { this.temperature = temperature; }
    public double getHumidity() { return humidity; }
    public void setHumidity(double humidity) { this.humidity = humidity; }
    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
}

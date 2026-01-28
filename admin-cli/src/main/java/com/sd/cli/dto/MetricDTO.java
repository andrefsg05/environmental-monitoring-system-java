package com.sd.cli;

/**
 * DTO (Data Transfer Object) que representa uma métrica individual de um sensor.
 * Contém leitura de temperatura e humidade com timestamp associado.
 */
public class MetricDTO {

    public String deviceId;
    public double temperature;
    public double humidity;
    public String timestamp;
}

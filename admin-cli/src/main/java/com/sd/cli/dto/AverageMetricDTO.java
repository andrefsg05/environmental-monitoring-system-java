package com.sd.cli;

/**
 * DTO (Data Transfer Object) que representa métricas agregadas (médias, mínimos e máximos).
 * Contém médias, valores mínimo e máximo de temperatura e humidade para um grupo de dispositivos.
 */

public class AverageMetricDTO {
    // Nível de agregação: "sala", "departamento", "piso" ou "edifício"
    public String level;
    public String id;
    public double averageTemperature;
    public double averageHumidity;
    public double minTemperature;
    public double maxTemperature;
    public double minHumidity;
    public double maxHumidity;
    public long count;
    public String fromDate;
    public String toDate;
}

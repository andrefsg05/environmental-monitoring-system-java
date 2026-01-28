
package com.sd.server.dto;

/**
 * DTO para retornar dados agregados (médias, mínimos e máximos) de métricas.
 * Utilizado por endpoint /api/metrics/average para fornecer
 * resumos de dados por sala, departamento, andar ou prédio.
 */
public class AverageMetricDTO {

    private String level; // Nível de agregação: "sala", "departamento", "piso", "edificio"
    private String id;
    private Double averageTemperature;
    private Double averageHumidity;
    private Double minTemperature;
    private Double maxTemperature;
    private Double minHumidity;
    private Double maxHumidity;
    private Long count;     // Quantidade de métricas utilizadas no cálculo das médias
    private String fromDate; 
    private String toDate;

    public AverageMetricDTO() {
    }

    public AverageMetricDTO(String level, String id, Double averageTemperature, Double averageHumidity, Long count) {
        this.level = level;
        this.id = id;
        this.averageTemperature = averageTemperature;
        this.averageHumidity = averageHumidity;
        this.count = count;
    }

    // Getters e Setters
    public String getLevel() {return level;}
    public void setLevel(String level) {this.level = level;}

    public String getId() {return id;}
    public void setId(String id) {this.id = id;}

    public Double getAverageTemperature() {return averageTemperature;}
    public void setAverageTemperature(Double averageTemperature) {this.averageTemperature = averageTemperature;}

    public Double getAverageHumidity() {return averageHumidity;}
    public void setAverageHumidity(Double averageHumidity) {this.averageHumidity = averageHumidity;}

    public Double getMinTemperature() {return minTemperature;}
    public void setMinTemperature(Double minTemperature) {this.minTemperature = minTemperature;}

    public Double getMaxTemperature() {return maxTemperature;}
    public void setMaxTemperature(Double maxTemperature) {this.maxTemperature = maxTemperature;}

    public Double getMinHumidity() {return minHumidity;}
    public void setMinHumidity(Double minHumidity) {this.minHumidity = minHumidity;}

    public Double getMaxHumidity() {return maxHumidity;}
    public void setMaxHumidity(Double maxHumidity) {this.maxHumidity = maxHumidity;}

    public Long getCount() {return count;}
    public void setCount(Long count) {this.count = count;}

    public String getFromDate() {return fromDate;}
    public void setFromDate(String fromDate) {this.fromDate = fromDate;}

    public String getToDate() {return toDate;}
    public void setToDate(String toDate) {this.toDate = toDate;}
}

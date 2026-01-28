
package com.sd.server.entity;

import java.time.LocalDateTime; // Data/hora

import jakarta.persistence.Column; // Mapeia coluna da tabela
import jakarta.persistence.Entity; // Define classe como entidade JPA
import jakarta.persistence.FetchType; // Estratégia de carregamento (uso de FetchType.LAZY)
import jakarta.persistence.GeneratedValue; // Valor gerado automaticamente
import jakarta.persistence.GenerationType; // Estratégia de geração de ID
import jakarta.persistence.Id; // Define chave primária
import jakarta.persistence.JoinColumn; // Coluna de junção FK
import jakarta.persistence.ManyToOne; // Relacionamento N:1
import jakarta.persistence.PrePersist; // Callback antes de inserir
import jakarta.persistence.Table; // Mapeia tabela do BD

/**
 * Entidade JPA que representa uma métrica de dispositivo.
 * Armazena valores de temperatura e umidade com timestamps.
 * Mantém relação many-to-one com Device para rastreabilidade.
 */
@Entity
@Table(name = "metrics")
public class Metric {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // geraçao automatica de IDs pela bd
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)  // carrega dispositivo associado imediatamente, LAZY carrega sob demanda
    @JoinColumn(name = "device_id", referencedColumnName = "id", nullable = false)
    private Device device;

    @Column(nullable = false)
    private Double temperature;

    @Column(nullable = false)
    private Double humidity;

    @Column(nullable = false)
    private LocalDateTime timestamp; // momento em que a métrica foi gerada pelo dispositivo

    @Column(name = "received_at")
    private LocalDateTime receivedAt; // momento em que a métrica foi recebida pelo servidor

    public Metric() {
    }

    public Metric(Device device, Double temperature, Double humidity, LocalDateTime timestamp) {
        this.device = device;
        this.temperature = temperature;
        this.humidity = humidity;
        this.timestamp = timestamp;
        this.receivedAt = LocalDateTime.now();
    }

    // Callback antes de persistir: inicializa receivedAt se não definido.
    @PrePersist
    protected void onCreate() {
        if (receivedAt == null) {
            receivedAt = LocalDateTime.now();
        }
    }

    // Getters e Setters
    public Long getId() {return id;}
    public void setId(Long id) {this.id = id; }
    
    // Obtém ID do dispositivo sem carregar a entidade completa 
    public String getDeviceId() {return device != null ? device.getId() : null;}

    public Double getTemperature() {return temperature;}
    public void setTemperature(Double temperature) {this.temperature = temperature;}

    public Double getHumidity() {return humidity;}
    public void setHumidity(Double humidity) {this.humidity = humidity;}

    public LocalDateTime getTimestamp() {return timestamp;}
    public void setTimestamp(LocalDateTime timestamp) {this.timestamp = timestamp;}

    public LocalDateTime getReceivedAt() {return receivedAt;}
    public void setReceivedAt(LocalDateTime receivedAt) {this.receivedAt = receivedAt;}

    public Device getDevice() {return device;}
    public void setDevice(Device device) {this.device = device;}
}

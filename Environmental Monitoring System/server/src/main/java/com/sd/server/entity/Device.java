
package com.sd.server.entity;

import java.time.LocalDateTime; // Data/hora

import jakarta.persistence.Column; // Mapeia coluna da tabela
import jakarta.persistence.Entity; // Define classe como entidade JPA
import jakarta.persistence.EnumType; // Tipo de armazenamento de enum
import jakarta.persistence.Enumerated; // Mapeia enum para BD
import jakarta.persistence.Id; // Define chave primária
import jakarta.persistence.PrePersist; // Callback antes de inserir
import jakarta.persistence.PreUpdate; // Callback antes de atualizar
import jakarta.persistence.Table; // Mapeia tabela do BD

/**
 * Entidade JPA que representa um dispositivo de IoT.
 * Armazena informações de localização (sala, departamento, andar, prédio),
 * protocolo de comunicação (MQTT/gRPC/REST) e status ativo.
 * Inclui timestamps de criação e atualização para auditoria.
 */
@Entity
@Table(name = "devices")
public class Device {

    @Id
    private String id;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ProtocolType protocol;

    @Column(nullable = false)
    private String room;

    @Column(nullable = false)
    private String department;

    @Column(nullable = false)
    private String floor;

    @Column(nullable = false)
    private String building;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    
    //Enum para tipos de protocolo suportados.
 
    public enum ProtocolType {
        MQTT, GRPC, REST
    }

    public Device() {
    }

    public Device(String id, ProtocolType protocol, String room, String department, String floor, String building) {
        this.id = id;
        this.protocol = protocol;
        this.room = room;
        this.department = department;
        this.floor = floor;
        this.building = building;
        this.active = true;
    }

    /**
     * Callback chamado antes de persistir novo registro.
     * Inicializa timestamps de criação e atualização.
     */
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    /**
     * Callback chamado antes de atualizar registro.
     * Atualiza timestamp de modificação.
     */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getters e Setters
    public String getId() {return id;}
    public void setId(String id) {this.id = id;}
    
    public ProtocolType getProtocol() {return protocol;}
    public void setProtocol(ProtocolType protocol) {this.protocol = protocol;}
    
    public String getRoom() {return room;}
    public void setRoom(String room) {this.room = room;}
    
    public String getDepartment() {return department;}
    public void setDepartment(String department) {this.department = department;}
    
    public String getFloor() {return floor;}
    public void setFloor(String floor) {this.floor = floor;}
    
    public String getBuilding() {return building;}
    public void setBuilding(String building) {this.building = building; }
    
    public boolean isActive() {return active; }
    public void setActive(boolean active) {this.active = active; }
    
    public LocalDateTime getCreatedAt() {return createdAt;}
    public void setCreatedAt(LocalDateTime createdAt) {this.createdAt = createdAt;}
    
    public LocalDateTime getUpdatedAt() {return updatedAt;}
    public void setUpdatedAt(LocalDateTime updatedAt) {this.updatedAt = updatedAt;}
}

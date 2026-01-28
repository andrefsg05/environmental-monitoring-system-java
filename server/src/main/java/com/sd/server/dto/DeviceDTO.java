
package com.sd.server.dto;
import com.sd.server.entity.Device; // Entidade dispositivo

import jakarta.validation.constraints.NotBlank; // Validação não vazio
import jakarta.validation.constraints.NotNull; // Validação não nulo

/**
 * DTO (Data Transfer Object) para transferência de dados de dispositivos.
 * Usado para API REST e comunicação entre controller e service.
 * Contém validações Jakarta Validation para garantir dados obrigatórios.
 */
public class DeviceDTO {

    @NotBlank(message = "Device ID is required")
    private String id;

    @NotNull(message = "Protocol is required")
    private Device.ProtocolType protocol;

    @NotBlank(message = "Room is required")
    private String room;

    @NotBlank(message = "Department is required")
    private String department;

    @NotBlank(message = "Floor is required")
    private String floor;

    @NotBlank(message = "Building is required")
    private String building;

    // Status do dispositivo: null = não alterado, true/false = valor específico
    private Boolean active;

    public DeviceDTO() {
    }

    public DeviceDTO(String id, Device.ProtocolType protocol, String room, String department, String floor, String building) {
        this.id = id;
        this.protocol = protocol;
        this.room = room;
        this.department = department;
        this.floor = floor;
        this.building = building;
        this.active = true;
    }

    /**
     * Converte entidade Device em DTO.
     */
    public static DeviceDTO fromEntity(Device device) {
        DeviceDTO dto = new DeviceDTO();
        dto.setId(device.getId());
        dto.setProtocol(device.getProtocol());
        dto.setRoom(device.getRoom());
        dto.setDepartment(device.getDepartment());
        dto.setFloor(device.getFloor());
        dto.setBuilding(device.getBuilding());
        dto.setActive(device.isActive());
        return dto;
    }

    /**
     * Converte DTO em entidade Device.
     */
    public Device toEntity() {
        Device device = new Device();
        device.setId(this.id);
        device.setProtocol(this.protocol);
        device.setRoom(this.room);
        device.setDepartment(this.department);
        device.setFloor(this.floor);
        device.setBuilding(this.building);
        device.setActive(this.active != null ? this.active : true);
        return device;
    }

    // Getters e Setters
    public String getId() {return id;}
    public void setId(String id) {this.id = id;}

    public Device.ProtocolType getProtocol() {return protocol;}
    public void setProtocol(Device.ProtocolType protocol) {this.protocol = protocol;}

    public String getRoom() {return room;}
    public void setRoom(String room) {this.room = room;}

    public String getDepartment() {return department;}
    public void setDepartment(String department) {this.department = department;}

    public String getFloor() {return floor;}
    public void setFloor(String floor) {this.floor = floor;}

    public String getBuilding() {return building;}
    public void setBuilding(String building) {this.building = building;}
    
    public Boolean getActive() {return active;}
    public void setActive(Boolean active) {this.active = active;}
}

package com.sd.cli;

/**
 * DTO (Data Transfer Object) que representa um dispositivo IoT no sistema.
 * Contém informações descritivas e de localização de um sensor.
 */
public class DeviceDTO {

    public String id;
    public String protocol;
    public String room;
    public String department;
    public String floor;
    public String building;
    public boolean active;
}

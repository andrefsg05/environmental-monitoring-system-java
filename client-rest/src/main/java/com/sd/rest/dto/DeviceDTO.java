package com.sd.rest.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties; // Ignora propriedades desconhecidas

/**
 * Data Transfer Object que representa um registro de dispositivo no servidor.
 * Contém informações básicas para sincronização entre cliente e servidor.
 * Ignora propriedades desconhecidas para compatibilidade com versões diferentes.
 */

@JsonIgnoreProperties(ignoreUnknown = true)
public class DeviceDTO {

    private String id;   
    private String protocol;
    private Boolean active;

    public String getId() { return id; }
    public String getProtocol() { return protocol; }
    public Boolean getActive() { return active; }
    
    /**
     * Verifica se o dispositivo está ativo.
     * Considera null como ativo (padrão true).
     * @return true se dispositivo está ativo ou não foi definido, false caso contrário
     */
    public boolean isActive() { 
        return active == null || active; 
    }
}
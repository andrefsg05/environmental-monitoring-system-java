
package com.sd.server.repository;

import java.util.List; // Listas

import org.springframework.data.jpa.repository.JpaRepository; // Repositório JPA base
import org.springframework.stereotype.Repository; // Marca como repositório

import com.sd.server.entity.Device; // Entidade dispositivo

/**
 * Repositório Spring Data JPA para entidade Device.
 * Fornece acesso ao banco de dados com queries derivadas de método.
 */
@Repository
public interface DeviceRepository extends JpaRepository<Device, String> {
    
    /**
     * Encontra todos os dispositivos em uma sala específica.
     * @param room identificador da sala
     * @return lista de dispositivos na sala
     */
    List<Device> findByRoom(String room);
    
    /**
     * Encontra todos os dispositivos de um departamento específico.
     * @param department nome do departamento
     * @return lista de dispositivos do departamento
     */
    List<Device> findByDepartment(String department);
    
    /**
     * Encontra todos os dispositivos em um andar específico.
     * @param floor identificador do andar
     * @return lista de dispositivos no andar
     */
    List<Device> findByFloor(String floor);
    
    /**
     * Encontra todos os dispositivos em um prédio específico.
     * @param building identificador do prédio
     * @return lista de dispositivos no prédio
     */
    List<Device> findByBuilding(String building);
    
    /**
     * Encontra dispositivos por status ativo/inativo.
     * @param active true para ativos, false para inativos
     * @return lista de dispositivos com status especificado
     */
    List<Device> findByActive(boolean active);
    
    /**
     * Encontra todos os dispositivos usando um protocolo específico.
     * @param protocol tipo de protocolo (MQTT, REST, GRPC)
     * @return lista de dispositivos do protocolo
     */
    List<Device> findByProtocol(Device.ProtocolType protocol);

    /**
     * Encontra dispositivos ativos de um protocolo específico.
     * Combinação de filtros de protocolo e status ativo.
     * Utilizado pelos supervisores para sincronização.
     * 
     * @param protocol tipo de protocolo (MQTT, REST, GRPC)
     * @param active deve ser true para obter dispositivos ativos
     * @return lista de dispositivos ativos do protocolo
     */
    List<Device> findByProtocolAndActive(Device.ProtocolType protocol, boolean active);
}

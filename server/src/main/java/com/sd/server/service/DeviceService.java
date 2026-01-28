
package com.sd.server.service;

import java.util.ArrayList; // Listas dinâmicas
import java.util.List; // Listas
import java.util.Optional; // Valor opcional (pode não existir)

import org.springframework.beans.factory.annotation.Autowired; // Injeção de dependências
import org.springframework.stereotype.Service; // Marca como serviço Spring

import com.sd.server.dto.DeviceDTO; // DTO de dispositivo
import com.sd.server.entity.Device; // Entidade dispositivo
import com.sd.server.repository.DeviceRepository; // Repositório de dispositivos
import com.sd.server.repository.MetricRepository; // Repositório de métricas

/**
 * Service para gerenciamento de dispositivos.
 * Fornece operações CRUD (Create, Read, Update, Delete) de dispositivos.
 * Gerencia consistência entre dispositivos e suas métricas associadas.
 * Para consultas de dispositivos ativos, utilize DeviceMonitorService que fornece cache otimizado.
 */
@Service
public class DeviceService {

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private MetricRepository metricRepository;

    /**
     * Obtém todos os dispositivos cadastrados.
     * @return lista completa de DeviceDTO
     */
    public List<DeviceDTO> getAllDevices() {
        List<Device> devices = deviceRepository.findAll();
        List<DeviceDTO> deviceDTOs = new ArrayList<>();
        for (Device device : devices) {
            deviceDTOs.add(DeviceDTO.fromEntity(device));
        }
        return deviceDTOs;
    }

    /**
     * Obtém apenas dispositivos com status ativo.
     * @return lista de DeviceDTO filtrada por status ativo
     */
    public List<DeviceDTO> getActiveDevices() {
        List<Device> activeDevices = deviceRepository.findByActive(true);
        List<DeviceDTO> deviceDTOs = new ArrayList<>();
        for (Device device : activeDevices) {
            deviceDTOs.add(DeviceDTO.fromEntity(device));
        }
        return deviceDTOs;
    }

    /**
     * Obtém dispositivos ativos de um protocolo específico.
     * Utilizado pelos supervisores para sincronização dinâmica.
     * 
     * @param protocol tipo de protocolo (MQTT, REST, GRPC)
     * @return lista de DeviceDTO ativos para o protocolo
     */
    public List<DeviceDTO> getActiveDevicesByProtocol(Device.ProtocolType protocol) {
        List<Device> devices = deviceRepository.findByProtocolAndActive(protocol, true);
        List<DeviceDTO> deviceDTOs = new ArrayList<>();
        for (Device device : devices) {
            deviceDTOs.add(DeviceDTO.fromEntity(device));
        }
        return deviceDTOs;
    }

    /**
     * Obtém detalhes de um dispositivo pelo ID.
     * @param id identificador único do dispositivo
     * @return Optional com DeviceDTO se encontrado
     */
    public Optional<DeviceDTO> getDeviceById(String id) {
        return deviceRepository.findById(id)
                .map(DeviceDTO::fromEntity);
    }

    /**
     * Cria um novo dispositivo no banco de dados.
     * @param deviceDTO dados do novo dispositivo
     * @return DeviceDTO do dispositivo criado
     */
    public DeviceDTO createDevice(DeviceDTO deviceDTO) {
        Device device = deviceDTO.toEntity();
        Device savedDevice = deviceRepository.save(device);
        return DeviceDTO.fromEntity(savedDevice);
    }

    /**
     * Atualiza um dispositivo existente.
     * Permite alterar protocolo, localização (sala, departamento, andar, prédio) e status.
     * 
     * @param id identificador do dispositivo
     * @param deviceDTO novos dados a atualizar
     * @return Optional com DeviceDTO atualizado, ou vazio se não encontrado
     */
    public Optional<DeviceDTO> updateDevice(String id, DeviceDTO deviceDTO) {
        return deviceRepository.findById(id)
                .map(existingDevice -> {
                    existingDevice.setProtocol(deviceDTO.getProtocol());
                    existingDevice.setRoom(deviceDTO.getRoom());
                    existingDevice.setDepartment(deviceDTO.getDepartment());
                    existingDevice.setFloor(deviceDTO.getFloor());
                    existingDevice.setBuilding(deviceDTO.getBuilding());
                    if (deviceDTO.getActive() != null) {
                        existingDevice.setActive(deviceDTO.getActive());
                    }
                    Device updatedDevice = deviceRepository.save(existingDevice);
                    return DeviceDTO.fromEntity(updatedDevice);
                });
    }

    /**
     * Deleta um dispositivo e todas as suas métricas associadas.
     * Garante limpeza completa para não deixar dados.
     * 
     * @param id identificador do dispositivo a deletar
     * @return true se deletado com sucesso, false se não encontrado
     */
    public boolean deleteDevice(String id) {
        if (deviceRepository.existsById(id)) {
            metricRepository.deleteByDeviceId(id);
            deviceRepository.deleteById(id);
            return true;
        }
        return false;
    }

    /**
     * Verifica se um dispositivo existe no banco de dados.
     * @param id identificador do dispositivo
     * @return true se existe, false caso contrário
     */
    public boolean deviceExists(String id) {
        return deviceRepository.existsById(id);
    }

    /**
     * Verifica se um dispositivo está ativo.
     * @param id identificador do dispositivo
     * @return true se ativo, false se inativo ou não encontrado
     */
    public boolean isDeviceActive(String id) {
        return deviceRepository.findById(id)
                .map(Device::isActive)
                .orElse(false);
    }
    
    /**
     * Obtém a entidade Device completa pelo ID.
     * @param id identificador do dispositivo
     * @return Optional com Device se encontrado
     */
    public Optional<Device> getDeviceEntity(String id) {
        return deviceRepository.findById(id);
    }
}

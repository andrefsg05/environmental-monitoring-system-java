
package com.sd.server.controller;

import java.util.List; // Listas

import org.springframework.beans.factory.annotation.Autowired; // Injeção de dependências
import org.springframework.http.HttpStatus; // Status HTTP
import org.springframework.http.ResponseEntity; // Resposta HTTP
import org.springframework.web.bind.annotation.DeleteMapping; // Mapeamento DELETE
import org.springframework.web.bind.annotation.GetMapping; // Mapeamento GET
import org.springframework.web.bind.annotation.PathVariable; // Variável no caminho
import org.springframework.web.bind.annotation.PostMapping; // Mapeamento POST
import org.springframework.web.bind.annotation.PutMapping; // Mapeamento PUT
import org.springframework.web.bind.annotation.RequestBody; // Corpo da requisição
import org.springframework.web.bind.annotation.RequestMapping; // Mapeamento base
import org.springframework.web.bind.annotation.RestController; // Controller REST

import com.sd.server.dto.DeviceDTO; // DTO de dispositivo
import com.sd.server.entity.Device; // Entidade dispositivo
import com.sd.server.service.DeviceMonitorService; // Serviço de monitoramento
import com.sd.server.service.DeviceService; // Serviço de dispositivos

import jakarta.validation.Valid; // Validação automática

/**
 * REST Controller para gerenciamento de dispositivos.
 * Fornece endpoints CRUD.
 * Funciona como registry central para sincronização com clientes.
 */
@RestController
@RequestMapping("/api/devices") // rota base para todos endpoints deste controller
public class DeviceController {

    @Autowired
    private DeviceService deviceService; // Gerencia operações CRUD para dispositivos.

    @Autowired
    private DeviceMonitorService deviceMonitorService; // Gerencia dispositivos ativos e consultas relacionadas.

    /**
     * Obtém lista de todos os dispositivos cadastrados.
     * @return lista completa de DeviceDTO
     */
    @GetMapping
    public ResponseEntity<List<DeviceDTO>> getAllDevices() {
        List<DeviceDTO> devices = deviceService.getAllDevices();
        return ResponseEntity.ok(devices);
    }

    /**
     * Obtém lista de todos os dispositivos ativos.
     * @return lista de DeviceDTO para dispositivos com status ativo
     */
    @GetMapping("/active")
    public ResponseEntity<List<DeviceDTO>> getActiveDevices() {
        return ResponseEntity.ok(deviceService.getActiveDevices());
    }

    /**
     * Obtém lista de dispositivos ativos filtrando por protocolo.
     * Endpoint utilizado pelos supervisores para sincronização dinâmica.
     * 
     * @param protocol tipo de protocolo (MQTT, REST, GRPC)
     * @return lista de DeviceDTO para o protocolo especificado
     */
    @GetMapping("/active/{protocol}")
    public ResponseEntity<List<DeviceDTO>> getActiveDevicesByProtocol(@PathVariable String protocol) {
        try {
            Device.ProtocolType parsed = Device.ProtocolType.valueOf(protocol.toUpperCase());
            return ResponseEntity.ok(deviceService.getActiveDevicesByProtocol(parsed));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Obtém detalhes de um dispositivo específico pelo ID.
     * 
     * @param id identificador único do dispositivo
     * @return DeviceDTO se encontrado, 404 caso contrário
     */
    @GetMapping("/{id}")
    public ResponseEntity<DeviceDTO> getDeviceById(@PathVariable String id) {
        return deviceService.getDeviceById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Cria um novo dispositivo.
     * Verifica se dispositivo já existe antes de criar.
     * 
     * @param deviceDTO dados do novo dispositivo
     * @return DeviceDTO criado com status 201, ou 409 se já existe
     */
    @PostMapping
    public ResponseEntity<DeviceDTO> createDevice(@Valid @RequestBody DeviceDTO deviceDTO) {
        if (deviceService.deviceExists(deviceDTO.getId())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
        DeviceDTO createdDevice = deviceService.createDevice(deviceDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdDevice);
    }

    /**
     * Atualiza um dispositivo existente.
     * 
     * @param id identificador do dispositivo a atualizar
     * @param deviceDTO novos dados do dispositivo
     * @return DeviceDTO atualizado, ou 404 se não encontrado
     */
    @PutMapping("/{id}")
    public ResponseEntity<DeviceDTO> updateDevice(@PathVariable String id, @Valid @RequestBody DeviceDTO deviceDTO) {
        return deviceService.updateDevice(id, deviceDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Deleta um dispositivo e suas métricas associadas.
     * 
     * @param id identificador do dispositivo a deletar
     * @return 204 No Content se sucesso, 404 se não encontrado
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDevice(@PathVariable String id) {
        if (deviceService.deleteDevice(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}

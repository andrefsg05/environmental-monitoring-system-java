package com.sd.cli;

import java.io.IOException; // Exceções de I/O
import java.util.List; // Lista de dispositivos
import java.util.Scanner; // Ler input do utilizador
import org.apache.hc.client5.http.classic.methods.HttpDelete; // Método DELETE
import org.apache.hc.client5.http.classic.methods.HttpGet; // Método GET
import org.apache.hc.client5.http.classic.methods.HttpPost; // Método POST
import org.apache.hc.client5.http.classic.methods.HttpPut; // Método PUT
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient; // Cliente HTTP
import org.apache.hc.core5.http.ContentType; // Tipo de conteúdo 
import org.apache.hc.core5.http.io.entity.EntityUtils; // Utilidades para corpos de resposta
import org.apache.hc.core5.http.io.entity.StringEntity; // Corpo de pedido em string
import com.fasterxml.jackson.core.type.TypeReference; // Desserialização genérica
import com.fasterxml.jackson.databind.ObjectMapper; // Serialização JSON

/**
 * Menu de gestão de dispositivos do sistema.
 * Oferece operações CRUD completas para dispositivos IoT.
 */
public class DeviceMenu {

    private final String serverUrl;
    private final CloseableHttpClient httpClient;
    private final Scanner scanner;
    private final ObjectMapper objectMapper;
    private final int sensorsPerProtocol = 100;

    public DeviceMenu(String serverUrl, CloseableHttpClient httpClient, Scanner scanner, ObjectMapper objectMapper) {
        this.serverUrl = serverUrl;
        this.httpClient = httpClient;
        this.scanner = scanner;
        this.objectMapper = objectMapper;
    }

    /**
     * Inicia o menu interativo de gestão de dispositivos (CRUD operations).
     * Permanece em loop até o utilizador escolher voltar (opção 0).
     * Permite: listar, visualizar, adicionar, atualizar e remover dispositivos.
     */
    public void show() {
        boolean back = false;
        while (!back) {
            displayDeviceMenu();
            String choice = scanner.nextLine().trim();
            back = handleOption(choice);
        }
    }

    /**
     * Exibe o menu de gestao de dispositivos.
     */
    private void displayDeviceMenu() {
        System.out.println("\n┌──────────────────────────────────┐");
        System.out.println("│      GESTÃO DE DISPOSITIVOS      │");
        System.out.println("├──────────────────────────────────┤");
        System.out.println("│ 1. Listar todos os dispositivos  │");
        System.out.println("│ 2. Visualizar detalhes           │");
        System.out.println("│ 3. Adicionar novo dispositivo    │");
        System.out.println("│ 4. Atualizar dispositivo         │");
        System.out.println("│ 5. Remover dispositivo           │");
        System.out.println("│ 0. Voltar                        │");
        System.out.println("└──────────────────────────────────┘");
        System.out.print("Escolha uma opção: ");
    }

    private boolean handleOption(String choice) {
        switch (choice) {
            case "1" -> listDevices();
            case "2" -> viewDeviceDetails();
            case "3" -> addDevice();
            case "4" -> updateDevice();
            case "5" -> removeDevice();
        // case "9" -> createDevicesForProtocols(); // PARA TESTES DE PERFORMANCE   
            case "0" -> { return true; }
            default -> System.out.println("⚠️ Opção inválida!");
        }
        return false;
    }

    /**
     * Lista todos os dispositivos registados no sistema.
     * Faz GET request a /api/devices e exibe uma tabela formatada.
     * Mostra: ID, Protocolo, Localização (sala/departamento/piso/edifício) e status.
     */
    private void listDevices() {
        try {
            HttpGet request = new HttpGet(serverUrl + "/api/devices");
            String response = httpClient.execute(request, res -> {
                if (res.getCode() == 200) {
                    return EntityUtils.toString(res.getEntity());
                }
                return null;
            });

            if (response != null) {
                List<DeviceDTO> devices = objectMapper.readValue(response,
                    new TypeReference<List<DeviceDTO>>(){});

                if (devices.isEmpty()) {
                    System.out.println("\nNenhum dispositivo registado.");
                } else {
                    displayDeviceTable(devices);
                }
            } else {
                System.out.println("⚠️ Erro ao obter lista de dispositivos.");
            }
        } catch (IOException e) {
            System.out.println("⚠️ Erro de comunicação: " + e.getMessage());
        }
    }

    /**
     * Exibe uma tabela formatada com os dispositivos.
     */
    private void displayDeviceTable(List<DeviceDTO> devices) {

        System.out.println("\n┌─────────────────────────────────────────────────────────────────────────────────────┐");
        System.out.printf("│ %-20s │ %-8s │ %-8s │ %-15s │ %-8s │ %-8s │%n",
                "ID", "Protocolo", "Sala", "Departamento", "Piso", "Estado");
        System.out.println("├─────────────────────────────────────────────────────────────────────────────────────┤");
        for (DeviceDTO d : devices) {
            System.out.printf("│ %-20s │ %-9s │ %-8s │ %-15s │ %-8s │ %-8s │%n",
                    truncate(d.id, 20), d.protocol,
                    truncate(d.room, 8), truncate(d.department, 15),
                    d.floor, d.active ? "Ativo" : "Inativo");
        }
        System.out.println("└─────────────────────────────────────────────────────────────────────────────────────┘");
        System.out.println("Total: " + devices.size() + " dispositivo(s)");
    }

    /**
     * Solicita o ID de um dispositivo e exibe os seus detalhes completos.
     */
    private void viewDeviceDetails() {
        System.out.print("\nID do dispositivo: ");
        String id = scanner.nextLine().trim();

        try {
            HttpGet request = new HttpGet(serverUrl + "/api/devices/" + id);
            httpClient.execute(request, res -> {

                // 200 = ok, 400 = bad request (rejeitado)
                if (res.getCode() == 200) {
                    String body = EntityUtils.toString(res.getEntity());
                    DeviceDTO d = objectMapper.readValue(body, DeviceDTO.class);
                    displayDeviceDetails(d);
                } else if (res.getCode() == 404) {
                    System.out.println("❌ Dispositivo não encontrado.");
                } else {
                    System.out.println("⚠️ Erro: " + res.getCode());
                }
                return null;
            });
        } catch (IOException e) {
            System.out.println("⚠️ Erro de comunicação: " + e.getMessage());
        }
    }

    /**
     * Exibe os detalhes completos de um dispositivo.
     */
    private void displayDeviceDetails(DeviceDTO d) {
        System.out.println("\n╔══════════════════════════════════════╗");
        System.out.println("║       DETALHES DO DISPOSITIVO        ║");
        System.out.println("╠══════════════════════════════════════╣");
        System.out.printf("║ ID:           %-22s ║%n", d.id);
        System.out.printf("║ Protocolo:    %-22s ║%n", d.protocol);
        System.out.printf("║ Sala:         %-22s ║%n", d.room);
        System.out.printf("║ Departamento: %-22s ║%n", d.department);
        System.out.printf("║ Piso:         %-22s ║%n", d.floor);
        System.out.printf("║ Edifício:     %-22s ║%n", d.building);
        System.out.printf("║ Estado:       %-22s ║%n", d.active ? "Ativo" : "Inativo");
        System.out.println("╚══════════════════════════════════════╝");
    }

    /**
     * Pede dados do dispositivo a adicionar envia um POST para o servidor.
     */
    private void addDevice() {
        System.out.println("\n=== ADICIONAR NOVO DISPOSITIVO ===");

        System.out.print("ID do dispositivo: sensor-");
        String id = "sensor-" + scanner.nextLine().trim();

        System.out.print("Protocolo (MQTT, GRPC, REST): ");
        String protocol = scanner.nextLine().trim().toUpperCase();
        if (!protocol.matches("MQTT|GRPC|REST")) {
            System.out.println("⚠️ Erro: Protocolo inválido. Use MQTT, GRPC ou REST.");
            return;
        }

        System.out.print("Sala: ");
        String room = scanner.nextLine().trim();

        System.out.print("Departamento: ");
        String department = scanner.nextLine().trim();

        System.out.print("Piso: ");
        String floor = scanner.nextLine().trim();

        System.out.print("Edifício: ");
        String building = scanner.nextLine().trim();

        DeviceDTO device = new DeviceDTO();
        device.id = id;
        device.protocol = protocol;
        device.room = room;
        device.department = department;
        device.floor = floor;
        device.building = building;
        device.active = true;

        sendCreateDevice(device);
    }

    /**
     * Envia um POST request para criar um novo dispositivo.
     */
    private void sendCreateDevice(DeviceDTO device) {
        try {
            HttpPost request = new HttpPost(serverUrl + "/api/devices");
            request.setEntity(new StringEntity(objectMapper.writeValueAsString(device), ContentType.APPLICATION_JSON));

            httpClient.execute(request, res -> {
                // 201 = created
                if (res.getCode() == 201) {
                    System.out.println("\n✅ Dispositivo criado com sucesso!");
                // 409 = conflict
                } else if (res.getCode() == 409) {
                    System.out.println("\n✗ Erro: Dispositivo com este ID já existe.");
                } else {
                    String body = EntityUtils.toString(res.getEntity());
                    System.out.println("\n⚠️ Erro: " + res.getCode() + " - " + body);
                }
                return null;
            });
        } catch (IOException e) {
            System.out.println("⚠️ Erro de comunicação: " + e.getMessage());
        }
    }

    /**
     * Obtém os dados atuais, permite edição e envia um PUT para o servidor.
     */
    private void updateDevice() {
        System.out.print("\nID do dispositivo a atualizar: ");
        String id = scanner.nextLine().trim();

        try {
            HttpGet getRequest = new HttpGet(serverUrl + "/api/devices/" + id);
            DeviceDTO device = httpClient.execute(getRequest, res -> {
                if (res.getCode() == 200) {
                    return objectMapper.readValue(EntityUtils.toString(res.getEntity()), DeviceDTO.class);
                }
                return null;
            });

            if (device == null) {
                System.out.println("Dispositivo não encontrado.");
                return;
            }

            promptUpdatedFields(device);
            sendUpdateDevice(id, device);
        } catch (IOException e) {
            System.out.println("⚠️ Erro de comunicação: " + e.getMessage());
        }
    }

    /**
     * Solicita ao utilizador novos valores para os campos de um dispositivo.
     * Mantém valores atuais se o utilizador apenas pressionar ENTER.
     */
    private void promptUpdatedFields(DeviceDTO device) {
        System.out.println("\nPressione ENTER para manter o valor atual.");

        System.out.printf("Protocolo [%s]: ", device.protocol);
        String protocol = scanner.nextLine().trim();
        if (!protocol.isEmpty()) device.protocol = protocol.toUpperCase();

        System.out.printf("Sala [%s]: ", device.room);
        String room = scanner.nextLine().trim();
        if (!room.isEmpty()) device.room = room;

        System.out.printf("Departamento [%s]: ", device.department);
        String department = scanner.nextLine().trim();
        if (!department.isEmpty()) device.department = department;

        System.out.printf("Piso [%s]: ", device.floor);
        String floor = scanner.nextLine().trim();
        if (!floor.isEmpty()) device.floor = floor;

        System.out.printf("Edifício [%s]: ", device.building);
        String building = scanner.nextLine().trim();
        if (!building.isEmpty()) device.building = building;

        System.out.printf("Ativo [%s] (s/n): ", device.active ? "sim" : "não");
        String active = scanner.nextLine().trim().toLowerCase();
        if (!active.isEmpty()) device.active = active.startsWith("s");
    }

    /**
     * Envia um PUT request para atualizar um dispositivo existente.
     */
    private void sendUpdateDevice(String id, DeviceDTO device) {
        try {
            HttpPut putRequest = new HttpPut(serverUrl + "/api/devices/" + id);
            putRequest.setEntity(new StringEntity(objectMapper.writeValueAsString(device), ContentType.APPLICATION_JSON));

            httpClient.execute(putRequest, res -> {
                if (res.getCode() == 200) {
                    System.out.println("\n✅ Dispositivo atualizado com sucesso!");
                } else {
                    System.out.println("\n⚠️ Erro ao atualizar: " + res.getCode());
                }
                return null;
            });
        } catch (IOException e) {
            System.out.println("⚠️Erro de comunicação: " + e.getMessage());
        }
    }

    /**
     * Guia o utilizador através do processo de remover um dispositivo.
     * Solicita confirmação antes de prosseguir com a eliminação.
     */
    private void removeDevice() {
        System.out.print("\nID do dispositivo a remover: ");
        String id = scanner.nextLine().trim();

        System.out.print("Tem certeza? (s/n): ");
        String confirm = scanner.nextLine().trim().toLowerCase();

        if (!confirm.startsWith("s")) {
            System.out.println("Operação cancelada.");
            return;
        }

        try {
            HttpDelete request = new HttpDelete(serverUrl + "/api/devices/" + id);
            httpClient.execute(request, res -> {
                if (res.getCode() == 204) {
                    System.out.println("\n✅ Dispositivo removido com sucesso!");
                } else if (res.getCode() == 404) {
                    System.out.println("\n❌ Dispositivo não encontrado.");
                } else {
                    System.out.println("\n⚠️ Erro: " + res.getCode());
                }
                return null;
            });
        } catch (IOException e) {
            System.out.println("⚠️Erro de comunicação: " + e.getMessage());
        }
    }

    /**
     * Cria X dispositivos para cada protocolo (MQTT, GRPC, REST) com IDs e atributos específicos.
     * PARA TESTES DE PERFORMANCE   
     */
    private void createDevicesForProtocols() {
        createDevicesBatch("MQTT", sensorsPerProtocol);
        createDevicesBatch("REST", sensorsPerProtocol);
        createDevicesBatch("GRPC", sensorsPerProtocol);
        System.out.println("✅ Criado com sucesso.");
    }

    /**
     * Cria um lote de dispositivos para um protocolo específico.
     * PARA TESTES DE PERFORMANCE   
     */
    private void createDevicesBatch(String protocol, int count) {
        for (int i = 1; i <= count; i++) {
            DeviceDTO device = new DeviceDTO();
            device.id = "sensor-" + i + "-" + protocol;
            device.protocol = protocol;
            device.room = String.valueOf(i);
            device.department = String.valueOf(i);
            device.floor = String.valueOf(i);
            device.building = String.valueOf(i);
            device.active = true;

            sendCreateDevice(device);
        }
    }

    /**
     * Trunca uma string para um comprimento máximo, adicionando "..." se necessário.
     * Útil para manter a tabela formatada quando os valores são muito longos.
     */
    private String truncate(String s, int maxLen) {
        if (s == null) return "";
        return s.length() <= maxLen ? s : s.substring(0, maxLen - 3) + "...";
    }
}

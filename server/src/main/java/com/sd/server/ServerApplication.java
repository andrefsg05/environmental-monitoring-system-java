
package com.sd.server;

import org.springframework.boot.SpringApplication; // Inicialização Spring Boot
import org.springframework.boot.autoconfigure.SpringBootApplication; // Auto-configuração Spring

/**
 * Aplicação principal Spring Boot para o servidor de monitoramento de dispositivos.
 * Inicializa o contexto Spring e ativa configurações de gRPC e MQTT.
 * Funciona como hub central para receber métricas de sensores via múltiplos protocolos.
 */
@SpringBootApplication
public class ServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ServerApplication.class, args);
    }
}

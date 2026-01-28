
package com.sd.server.config;

import java.io.IOException; // Exceções de I/O

import org.slf4j.Logger; // Interface de logging
import org.slf4j.LoggerFactory; // Factory de loggers
import org.springframework.beans.factory.annotation.Autowired; // Injeção de dependências
import org.springframework.beans.factory.annotation.Value; // Injeção de valores (properties)
import org.springframework.context.annotation.Configuration; // Classe de configuração

import com.sd.server.grpc.MetricsGrpcService; // Serviço gRPC de métricas

import io.grpc.Server; // Servidor gRPC
import io.grpc.ServerBuilder; // Builder do servidor gRPC
import jakarta.annotation.PostConstruct; // Callback após construção
import jakarta.annotation.PreDestroy; // Callback antes de destruição

/**
 * Configuração para servidor gRPC.
 * Inicializa servidor na porta configurada em application.properties
 * e registra serviço de métricas para ingestão de dados.
 */
@Configuration
public class GrpcConfig {

    private static final Logger logger = LoggerFactory.getLogger(GrpcConfig.class);

    @Value("${grpc.server.port}")
    private int grpcPort;

    @Autowired //injeta instancia do serviço gRPC
    private MetricsGrpcService metricsGrpcService;

    private Server server;

    /**
     * Inicializa servidor gRPC ao criar o Bean.
     * Conecta MetricsGrpcService e registra shutdown hook para limpeza.
     */
    @PostConstruct
    public void startGrpcServer() {
        try {
            server = ServerBuilder.forPort(grpcPort)
                    .addService(metricsGrpcService)
                    .build()
                    .start();
            logger.info("gRPC server started on port {}", grpcPort);

            // Garante encerramento limpo do servidor ao desligar a aplicação
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger.info("Shutting down gRPC server");
                if (server != null) {
                    server.shutdown();
                }
            }));

        } catch (IOException e) {
            logger.error("Failed to start gRPC server: {}", e.getMessage());
        }
    }

    /**
     * Encerra servidor gRPC ao destruir o Bean.
     * Chamado durante shutdown do Spring.
     */
    @PreDestroy
    public void stopGrpcServer() {
        if (server != null) {
            server.shutdown();
            logger.info("gRPC server stopped");
        }
    }
}

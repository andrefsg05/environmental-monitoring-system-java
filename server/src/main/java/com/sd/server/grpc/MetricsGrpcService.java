
package com.sd.server.grpc;

import java.time.LocalDateTime; // Data/hora

import org.slf4j.Logger; // Interface de logging
import org.slf4j.LoggerFactory; // Factory de loggers
import org.springframework.beans.factory.annotation.Autowired; // Injeção de dependências
import org.springframework.stereotype.Service; // Marca como serviço Spring

import com.sd.grpc.MetricRequest; // Request gRPC gerado do protobuf
import com.sd.grpc.MetricResponse; // Response gRPC gerado do protobuf
import com.sd.grpc.MetricsServiceGrpc; // Service stub gRPC gerado
import com.sd.server.dto.MetricDTO; // DTO de métrica
import com.sd.server.service.MetricService; // Serviço de métricas

import io.grpc.stub.StreamObserver; // Observer para resposta assíncrona gRPC

/**
 * Implementação gRPC do serviço de métricas.
 * Recebe métrica via RPC e valida contra banco de dados.
 * Retorna sucesso/falha em MetricResponse.
 */
@Service
public class MetricsGrpcService extends MetricsServiceGrpc.MetricsServiceImplBase {

    private static final Logger logger = LoggerFactory.getLogger(MetricsGrpcService.class);

    @Autowired
    private MetricService metricService;

    /**
     * Endpoint gRPC para envio de uma métrica de um dispositivo.
     * Converte MetricRequest em MetricDTO, valida e armazena.
     * Responde com success=true ou false com mensagem de erro.
     * 
     * @param request MetricRequest com deviceId, temperature, humidity, timestamp
     * @param responseObserver observer para enviar resposta ao cliente
     */
    @Override
    public void sendMetric(MetricRequest request, StreamObserver<MetricResponse> responseObserver) {
        logger.debug("Received gRPC metric from device: {}", request.getDeviceId());

        try {
            MetricDTO metricDTO = new MetricDTO();
            metricDTO.setDeviceId(request.getDeviceId());
            metricDTO.setTemperature(request.getTemperature());
            metricDTO.setHumidity(request.getHumidity());
            
            // Usa timestamp do cliente ou timestamp atual se vazio
            if (!request.getTimestamp().isEmpty()) {
                metricDTO.setTimestamp(LocalDateTime.parse(request.getTimestamp()));
            } else {
                metricDTO.setTimestamp(LocalDateTime.now());
            }

            boolean success = metricService.ingestMetric(metricDTO);

            MetricResponse response = MetricResponse.newBuilder()
                    .setSuccess(success)
                    .setMessage(success ? "Metric ingested successfully" : "Device not registered or inactive")
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            logger.error("Error processing gRPC metric: {}", e.getMessage());
            MetricResponse response = MetricResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Error: " + e.getMessage())
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }
}

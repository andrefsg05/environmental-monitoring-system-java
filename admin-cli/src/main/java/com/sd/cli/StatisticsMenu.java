package com.sd.cli;

import java.io.IOException; // Exceções de I/O
import java.net.URLEncoder; // Codificar parâmetros de URL
import java.nio.charset.StandardCharsets; // Charset padrão (UTF-8)

import org.apache.hc.client5.http.classic.methods.HttpGet; // Método GET
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient; // Cliente HTTP
import org.apache.hc.core5.http.io.entity.EntityUtils; // Utilidades para corpos de resposta

import com.fasterxml.jackson.core.type.TypeReference; // Desserialização genérica
import com.fasterxml.jackson.databind.ObjectMapper; // Serialização JSON

/**
 * Menu responsável por exibir estatísticas agregadas do sistema.
 */
public class StatisticsMenu {

	private final String serverUrl;
	private final CloseableHttpClient httpClient;
	private final ObjectMapper objectMapper;

	public StatisticsMenu(String serverUrl, CloseableHttpClient httpClient, ObjectMapper objectMapper) {
		this.serverUrl = serverUrl;
		this.httpClient = httpClient;
		this.objectMapper = objectMapper;
	}

	/**
	 * Obtém estatísticas do servidor e exibe-as formatadas.
	 */
	public void show() {
		try {
			HttpGet request = new HttpGet(serverUrl + "/api/devices");
			String response = httpClient.execute(request, res -> {
				if (res.getCode() == 200) {
					return EntityUtils.toString(res.getEntity());
				}
				return null;
			});

			if (response != null) {
				var devices = objectMapper.readValue(response, new TypeReference<java.util.List<DeviceDTO>>(){});
				displayStatistics(devices);
			} else {
				System.out.println("Erro ao obter estatísticas: resposta vazia.");
			}
		} catch (IOException e) {
			System.out.println("Erro de comunicação: " + e.getMessage());
		}
	}

	/**
	 * Exibe as estatísticas do sistema de forma formatada.
	 * Calcula e mostra: total, ativos/inativos, contagem por protocolo, e métricas por protocolo.
	 */
	private void displayStatistics(java.util.List<DeviceDTO> devices) {
		// Calcula estatísticas dos dispositivos
		int total = devices.size();
		int ativos = (int) devices.stream().filter(d -> d.active).count();
		int mqtt = (int) devices.stream().filter(d -> "MQTT".equalsIgnoreCase(d.protocol)).count();
		int grpc = (int) devices.stream().filter(d -> "GRPC".equalsIgnoreCase(d.protocol)).count();
		int rest = (int) devices.stream().filter(d -> "REST".equalsIgnoreCase(d.protocol)).count();

		// Obtém contagem de métricas por protocolo
		long metricsMqtt = getMetricsCountByProtocol("MQTT");
		long metricsGrpc = getMetricsCountByProtocol("GRPC");
		long metricsRest = getMetricsCountByProtocol("REST");

		// Obtém latência média por protocolo
		double latencyMqtt = getAverageLatencyByProtocol("MQTT");
		double latencyGrpc = getAverageLatencyByProtocol("GRPC");
		double latencyRest = getAverageLatencyByProtocol("REST");

		System.out.println("\n╔═══════════════════════════════════════╗");
		System.out.println("║        ESTATÍSTICAS DO SISTEMA        ║");
		System.out.println("╠═══════════════════════════════════════╣");
		System.out.printf("║ Total de Dispositivos:     %-10d ║%n", total);
		System.out.printf("║ Dispositivos Ativos:       %-10d ║%n", ativos);
		System.out.printf("║ Dispositivos Inativos:     %-10d ║%n", total - ativos);
		System.out.println("╠═══════════════════════════════════════╣");
		System.out.println("║        Sensores Por Protocolo         ║");
		System.out.printf("║ MQTT:                      %-10d ║%n", mqtt);
		System.out.printf("║ gRPC:                      %-10d ║%n", grpc);
		System.out.printf("║ REST:                      %-10d ║%n", rest);
		System.out.println("╠═══════════════════════════════════════╣");
		System.out.println("║        Métricas Recebidas             ║");
		System.out.printf("║ MQTT:                      %-10d ║%n", metricsMqtt);
		System.out.printf("║ gRPC:                      %-10d ║%n", metricsGrpc);
		System.out.printf("║ REST:                      %-10d ║%n", metricsRest);
		System.out.printf("║ Total:                     %-10d ║%n", metricsMqtt + metricsGrpc + metricsRest);
		System.out.println("╠═══════════════════════════════════════╣");
		System.out.println("║     Latência Média (ms)               ║");
		System.out.printf("║ MQTT:                      %-10.2f ║%n", latencyMqtt);
		System.out.printf("║ gRPC:                      %-10.2f ║%n", latencyGrpc);
		System.out.printf("║ REST:                      %-10.2f ║%n", latencyRest);
		System.out.println("╚═══════════════════════════════════════╝");
	}

	/**
	 * Obtém a contagem de métricas recebidas para um protocolo específico.
	 */
	private long getMetricsCountByProtocol(String protocol) {
		try {
			String encodedProtocol = URLEncoder.encode(protocol, StandardCharsets.UTF_8);
			HttpGet request = new HttpGet(serverUrl + "/api/metrics/count-by-protocol?protocol=" + encodedProtocol);
			String response = httpClient.execute(request, res -> {
				if (res.getCode() == 200) {
					return EntityUtils.toString(res.getEntity());
				}
				return null;
			});

			if (response != null) {
				var result = objectMapper.readValue(response, new TypeReference<java.util.Map<String, Object>>(){});
				Object countObj = result.get("count");
				if (countObj != null) {
					if (countObj instanceof Integer) {
						return ((Integer) countObj).longValue();
					} else if (countObj instanceof Long) {
						return (Long) countObj;
					}
				}
			}
		} catch (IOException e) {
			System.err.println("Aviso: Não foi possível obter contagem de métricas para " + protocol);
		}
		return 0;
	}

	/**
	 * Obtém a latência média entre criação da métrica no cliente e recebimento no servidor.
	 */
	private double getAverageLatencyByProtocol(String protocol) {
		try {
			String encodedProtocol = URLEncoder.encode(protocol, StandardCharsets.UTF_8);
			HttpGet request = new HttpGet(serverUrl + "/api/metrics/average-latency-by-protocol?protocol=" + encodedProtocol);
			String response = httpClient.execute(request, res -> {
				if (res.getCode() == 200) {
					return EntityUtils.toString(res.getEntity());
				}
				return null;
			});

			if (response != null) {
				var result = objectMapper.readValue(response, new TypeReference<java.util.Map<String, Object>>(){});
				Object latencyObj = result.get("averageLatencyMs");
				if (latencyObj != null) {
					if (latencyObj instanceof Number) {
						return ((Number) latencyObj).doubleValue();
					}
				}
			}
		} catch (IOException e) {
			System.err.println("Aviso: Não foi possível obter latência média para " + protocol);
		}
		return 0.0;
	}
}

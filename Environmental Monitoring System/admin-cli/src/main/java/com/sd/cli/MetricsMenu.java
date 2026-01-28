package com.sd.cli;

import java.io.IOException; // Exceções de I/O
import java.net.URLEncoder; // Codificar parâmetros de URL
import java.nio.charset.StandardCharsets; // Charset padrão (UTF-8)
import java.util.List; // Listas de métricas
import java.util.Scanner; // Ler input do utilizador

import org.apache.hc.client5.http.classic.methods.HttpGet; // Método GET
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient; // Cliente HTTP
import org.apache.hc.core5.http.io.entity.EntityUtils; // Utilidades para corpos de resposta

import com.fasterxml.jackson.core.type.TypeReference; // Desserialização genérica
import com.fasterxml.jackson.databind.ObjectMapper; // Serialização JSON

/**
 * Menu dedicado à consulta de métricas do sistema.
 * Permite consultar métricas agregadas (médias) por sala, departamento, piso ou edifício.
 * Também permite consultar métricas brutas por dispositivo individual.
 */
public class MetricsMenu {

	private final String serverUrl;
	private final CloseableHttpClient httpClient;
	private final Scanner scanner;
	private final ObjectMapper objectMapper;

	public MetricsMenu(String serverUrl, CloseableHttpClient httpClient, Scanner scanner, ObjectMapper objectMapper) {
		this.serverUrl = serverUrl;
		this.httpClient = httpClient;
		this.scanner = scanner;
		this.objectMapper = objectMapper;
	}

	/**
	 * Loop principal do menu de métricas.
	 * Permanece ativo até o utilizador escolher voltar (opção 0).
	 */
	public void show() {
		boolean back = false;
		while (!back) {
			exibirMenuMetricas();
			String choice = scanner.nextLine().trim();
			back = processarOpcao(choice);
		}
	}

	/**
	 * Exibe o menu de consulta de métricas.
	 */
	private void exibirMenuMetricas() {
		System.out.println("\n┌──────────────────────────────────┐");
		System.out.println("│       CONSULTA DE MÉTRICAS       │");
		System.out.println("├──────────────────────────────────┤");
		System.out.println("│ 1. Consultar por Sala            │");
		System.out.println("│ 2. Consultar por Departamento    │");
		System.out.println("│ 3. Consultar por Piso            │");
		System.out.println("│ 4. Consultar por Edifício        │");
		System.out.println("│ 5. Métricas brutas por Device    │");
		System.out.println("│ 0. Voltar                        │");
		System.out.println("└──────────────────────────────────┘");
		System.out.print("Escolha uma opção: ");
	}

	private boolean processarOpcao(String choice) {
		switch (choice) {
			case "1" -> consultarMetricasAgregadas("sala");
			case "2" -> consultarMetricasAgregadas("departamento");
			case "3" -> consultarMetricasAgregadas("piso");
			case "4" -> consultarMetricasAgregadas("edificio");
			case "5" -> consultarMetricasBrutas();
			// case "9" -> cleanMetrics(); // PARA TESTES DE PERFORMANCE   
			case "0" -> { return true; }
			default -> System.out.println("Opção inválida!");
		}
		return false;
	}

	/**
	 * Consulta métricas agregadas (médias de temperatura e humidade) por categoria.
	 * Categorias: sala, departamento, piso, edifício.
	 * Permite filtrar por intervalo de tempo (últimas 24h se não especificado).
	 */
	private void consultarMetricasAgregadas(String categoria) {
		System.out.printf("\n%s: ", categoria);
		String id = scanner.nextLine().trim();

		String from = null;
		String to = null;

		System.out.print("Data inicial (YYYY-MM-DDTHH:MM:SS ou ENTER para últimas 24h): ");
		String fromInput = scanner.nextLine().trim();
		if (!fromInput.isEmpty()) {
			from = fromInput;
			System.out.print("Data final (YYYY-MM-DDTHH:MM:SS): ");
			to = scanner.nextLine().trim();
		}

		try {
			StringBuilder url = new StringBuilder(serverUrl + "/api/metrics/average?level=" +
				URLEncoder.encode(categoria, StandardCharsets.UTF_8) + "&id=" +
				URLEncoder.encode(id, StandardCharsets.UTF_8));
			if (from != null && to != null) {
				url.append("&from=").append(URLEncoder.encode(from, StandardCharsets.UTF_8))
				   .append("&to=").append(URLEncoder.encode(to, StandardCharsets.UTF_8));
			}

			HttpGet request = new HttpGet(url.toString());
			httpClient.execute(request, res -> {
				if (res.getCode() == 200) {
					String body = EntityUtils.toString(res.getEntity());
					AverageMetricDTO avg = objectMapper.readValue(body, AverageMetricDTO.class);
					exibirMetricasAgregadas(avg);
				} else if (res.getCode() == 400) {
					System.out.println("⚠️ Nenhuma métrica encontrada.");
				} else {
					System.out.println("⚠️ Erro ao obter métricas: " + res.getCode());
				}
				return null;
			});
		} catch (IOException e) {
			System.out.println("⚠️ Erro de comunicação: " + e.getMessage());
		}
	}

	/**
	 * Consulta métricas brutas (não agregadas) de um dispositivo específico.
	 * Permite filtrar por intervalo de tempo (todas se não especificado).
	 */
	private void consultarMetricasBrutas() {
		System.out.print("\nID do dispositivo: ");
		String deviceId = scanner.nextLine().trim();

		String from = null;
		String to = null;

		System.out.print("Data inicial (YYYY-MM-DDTHH:MM:SS ou ENTER para últimas 24h): ");
		String fromInput = scanner.nextLine().trim();
		if (!fromInput.isEmpty()) {
			from = fromInput;
			System.out.print("Data final (YYYY-MM-DDTHH:MM:SS): ");
			to = scanner.nextLine().trim();
		}

		try {
			StringBuilder url = new StringBuilder(serverUrl + "/api/metrics/raw?deviceId=" +
				URLEncoder.encode(deviceId, StandardCharsets.UTF_8));
			if (from != null && to != null) {
				url.append("&from=").append(URLEncoder.encode(from, StandardCharsets.UTF_8))
				   .append("&to=").append(URLEncoder.encode(to, StandardCharsets.UTF_8));
			}

			HttpGet request = new HttpGet(url.toString());
			httpClient.execute(request, res -> {
				if (res.getCode() == 200) {
					String body = EntityUtils.toString(res.getEntity());
					List<MetricDTO> metrics = objectMapper.readValue(body,
						new TypeReference<List<MetricDTO>>(){});
					exibirMetricasBrutas(metrics);
				} else if (res.getCode() == 404) {
					System.out.println("⚠️ Dispositivo não encontrado.");
				} else {
					System.out.println("⚠️ Erro ao obter métricas: " + res.getCode());
				}
				return null;
			});
		} catch (IOException e) {
			System.out.println("⚠️ Erro de comunicação: " + e.getMessage());
		}
	}

	// Exibe métricas agregadas formatadas
	private void exibirMetricasAgregadas(AverageMetricDTO avg) {
		System.out.println("\n╔═══════════════════════════════════════════════════════╗");
		System.out.println("║                     MÉTRICAS AGREGADAS                ║");
		System.out.println("╠═══════════════════════════════════════════════════════╣");
		System.out.printf("║ Categoria:             %-30s ║%n", avg.level);
		System.out.printf("║ ID:                    %-30s ║%n", avg.id);
		System.out.printf("║ Temperatura Média:     %-30.2f ║%n", avg.averageTemperature);
		System.out.printf("║ Temperatura Máx:       %-30.2f ║%n", avg.maxTemperature);
		System.out.printf("║ Temperatura Mín:       %-30.2f ║%n", avg.minTemperature);
		System.out.printf("║ Humidade Média:        %-30.2f ║%n", avg.averageHumidity);
		System.out.printf("║ Humidade Máx:          %-30.2f ║%n", avg.maxHumidity);
		System.out.printf("║ Humidade Mín:          %-30.2f ║%n", avg.minHumidity);
		System.out.printf("║ Nº de Leituras:        %-30d ║%n", avg.count);
		System.out.printf("║ De:                    %-30s ║%n", truncate(avg.fromDate, 30));
		System.out.printf("║ Até:                   %-30s ║%n", truncate(avg.toDate, 30));
		System.out.println("╚═══════════════════════════════════════════════════════╝");
	}

	// Exibe métricas brutas formatadas
	private void exibirMetricasBrutas(List<MetricDTO> metrics) {
		if (metrics == null || metrics.isEmpty()) {
			System.out.println("\nNenhuma métrica encontrada.");
			return;
		}

		System.out.println("\n┌──────────────────────────────────────────────────────────────────────┐");
		System.out.printf("│ %-25s │ %-12s │ %-12s │ %-12s │%n",
				"Timestamp", "Temperatura", "Humidade", "Device");
		System.out.println("├──────────────────────────────────────────────────────────────────────┤");
		for (MetricDTO m : metrics) {
			System.out.printf("│ %-25s │ %12.2f │ %12.2f │ %-12s │%n",
					truncate(m.timestamp, 25), m.temperature, m.humidity,
					truncate(m.deviceId, 12));
		}
		System.out.println("└──────────────────────────────────────────────────────────────────────┘");
		System.out.println("Total: " + metrics.size() + " leitura(s)");
	}

	/**
	 * Limpa todas as métricas da base de dados.
	 * PARA TESTES DE PERFORMANCE   
	 */
	private void cleanMetrics() {
		try {
			String url = serverUrl + "/api/metrics";
			org.apache.hc.client5.http.classic.methods.HttpDelete request = 
				new org.apache.hc.client5.http.classic.methods.HttpDelete(url);
			
			httpClient.execute(request, res -> {
				if (res.getCode() == 200 || res.getCode() == 204) {
					System.out.println("✅ Todas as métricas foram eliminadas com sucesso.");
				} else {
					System.out.println("⚠️ Erro ao eliminar métricas: HTTP " + res.getCode());
				}
				return null;
			});
		} catch (IOException e) {
			System.out.println("⚠️ Erro de comunicação: " + e.getMessage());
		}
	}

	// Trunca strings longas para exibição
	private String truncate(String s, int maxLen) {
		if (s == null) return "";
		return s.length() <= maxLen ? s : s.substring(0, maxLen - 3) + "...";
	}
}

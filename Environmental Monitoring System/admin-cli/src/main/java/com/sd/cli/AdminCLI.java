package com.sd.cli;

import java.io.IOException; // Exceções de I/O
import java.util.Scanner; // Ler input do utilizador

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient; // Cliente HTTP
import org.apache.hc.client5.http.impl.classic.HttpClients; // Factory HTTP

import com.fasterxml.jackson.databind.ObjectMapper; // Serialização JSON
import com.fasterxml.jackson.databind.SerializationFeature; // Config JSON

/**
 * Classe principal da aplicação de administração.
 * Gerencia o fluxo geral da CLI, inicializa recursos (HTTP client, scanner, menus)
 * e coordena a navegação entre diferentes menus (Dispositivos, Métricas, Estatísticas).
 */
public class AdminCLI {

    private static final String SERVER_URL = "http://localhost:8080";
    private static final ObjectMapper objectMapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);
    private static CloseableHttpClient httpClient;
    private static Scanner scanner;

    private static DeviceMenu deviceMenu;        // Gestão de dispositivos
    private static MetricsMenu metricsMenu;      // Consulta de métricas
    private static StatisticsMenu statisticsMenu; // Estatísticas do sistema

    public static void main(String[] args) {
        initializeResources();
        welcome();
        executeMainMenu();
        closeResources();
    }

    /**
     * Inicializa todos os recursos necessários:
     * - Cliente HTTP para comunicação com servidor
     * - Scanner para ler entrada do utilizador
     * - Instâncias dos menus funcionais
     */
    private static void initializeResources() {
        httpClient = HttpClients.createDefault();
        scanner = new Scanner(System.in);
        deviceMenu = new DeviceMenu(SERVER_URL, httpClient, scanner, objectMapper);
        metricsMenu = new MetricsMenu(SERVER_URL, httpClient, scanner, objectMapper);
        statisticsMenu = new StatisticsMenu(SERVER_URL, httpClient, objectMapper);
    }

    /**
     * Exibe mensagem de boas-vindas formatada na consola.
     */
    private static void welcome() {
        System.out.println("╔══════════════════════════════════════════════════════╗");
        System.out.println("║     Sistema de Monitorização Ambiental - UÉvora      ║");
        System.out.println("║              Cliente de Administração                ║");
        System.out.println("╚══════════════════════════════════════════════════════╝");
        System.out.println();
    }

    /**
     * Loop principal da CLI.
     * Mantém a aplicação ativa até o utilizador escolher sair (opção 0).
     */
    private static void executeMainMenu() {
        boolean running = true;
        while (running) {
            running = displayMainMenu();
        }
    }

    /**
     * Fecha todos os recursos antes de sair:
     * - Cliente HTTP
     * - Scanner
     */
    private static void closeResources() {
        try {
            httpClient.close();
        } catch (IOException e) {
        }
        scanner.close();
        System.out.println("\nAdeus!");
    }

    /**
     * Exibe o menu principal com opções e processa escolha do utilizador.
     * Retorna false se o utilizador quer sair.
     */
    private static boolean displayMainMenu() {
        System.out.println("\n┌──────────────────────────────────┐");
        System.out.println("│         MENU PRINCIPAL           │");
        System.out.println("├──────────────────────────────────┤");
        System.out.println("│ 1. Gestão de Dispositivos        │");
        System.out.println("│ 2. Consulta de Métricas          │");
        System.out.println("│ 3. Estatísticas do Sistema       │");
        System.out.println("│ 0. Sair                          │");
        System.out.println("└──────────────────────────────────┘");
        System.out.print("Escolha uma opção: ");

        String choice = scanner.nextLine().trim();
        switch (choice) {
            case "1" -> deviceMenu.show();
            case "2" -> metricsMenu.show();
            case "3" -> statisticsMenu.show();
            case "0" -> { return false; }
            default -> System.out.println("Opção inválida!");
        }
        return true;
    }
}

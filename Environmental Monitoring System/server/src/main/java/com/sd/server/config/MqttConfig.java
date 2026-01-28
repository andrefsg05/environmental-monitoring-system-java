
package com.sd.server.config;

import org.eclipse.paho.client.mqttv3.MqttConnectOptions; // Opções de conexão MQTT
import org.springframework.beans.factory.annotation.Value; // Injeção de valores (properties)
import org.springframework.context.annotation.Bean; // Definição de bean
import org.springframework.context.annotation.Configuration; // Classe de configuração
import org.springframework.integration.annotation.ServiceActivator; // Ativador de serviço
import org.springframework.integration.channel.DirectChannel; // Canal direto (síncrono)
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory; // Factory MQTT padrão
import org.springframework.integration.mqtt.core.MqttPahoClientFactory; // Interface factory MQTT
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter; // Adaptador de entrada MQTT
import org.springframework.integration.mqtt.support.DefaultPahoMessageConverter; // Conversor de mensagens MQTT
import org.springframework.messaging.MessageChannel; // Interface de canal
import org.springframework.messaging.MessageHandler; // Interface de handler

import com.sd.server.mqtt.MqttMessageHandler; // Handler de mensagens MQTT

/**
 * Configuração de integração MQTT .
 * Estabelece conexão com broker MQTT e configura listeners para tópicos.
 * As mensagens recebidas são roteadas para MqttMessageHandler.
 */
@Configuration
public class MqttConfig {

    @Value("${mqtt.broker.url}")
    private String brokerUrl;

    @Value("${mqtt.client.id}")
    private String clientId;

    @Value("${mqtt.topic}")
    private String topic;

    @Value("${mqtt.qos}")
    private int qos;

    /**
     * Configura factory de clientes MQTT com opções de reconexão automática.
     * Define timeout de conexão de 10 segundos e clean session habilitada.
     */
    @Bean
    public MqttPahoClientFactory mqttClientFactory() {
        DefaultMqttPahoClientFactory factory = new DefaultMqttPahoClientFactory();
        MqttConnectOptions options = new MqttConnectOptions();
        options.setServerURIs(new String[]{brokerUrl}); //define url do broker
        options.setAutomaticReconnect(true); // reconexao automatica
        options.setCleanSession(true); // historico passado apagado
        options.setConnectionTimeout(10); // timeout 10s
        factory.setConnectionOptions(options);
        return factory;
    }

    /**
     * Cria canal de mensagens direto para entrada de mensagens MQTT.
     * Mensagens recebidas do broker entram neste canal
     * DirectChannel = sem fila, processamento síncrono.
     */
    @Bean
    public MessageChannel mqttInputChannel() {
        return new DirectChannel();
    }

    /**
     * Configura adaptador de canal para receber mensagens MQTT.
     * Subscreve ao tópico configurado e converte para Message format.
     * Timeout de conclusão: 5 segundos.
     */
    @Bean
    public MqttPahoMessageDrivenChannelAdapter inboundAdapter(MqttPahoClientFactory mqttClientFactory) {
        MqttPahoMessageDrivenChannelAdapter adapter =
                new MqttPahoMessageDrivenChannelAdapter(clientId, mqttClientFactory, topic); // adaptador que se conecta ao broker MQTT e recebe mensagens
        adapter.setCompletionTimeout(5000); // timeout de conclusao 5s
        adapter.setConverter(new DefaultPahoMessageConverter()); // converte mensagens MQTT para objetos
        adapter.setQos(qos); // define o QoS, = 1 At least once
        adapter.setOutputChannel(mqttInputChannel()); // canal de saida paras a mensagens recebidas 
        return adapter;
    }

    /**
     * Conecta handler de mensagens ao canal de entrada MQTT.
     * Mensagens recebidas são processadas pelo MqttMessageHandler.
     */
    @Bean
    @ServiceActivator(inputChannel = "mqttInputChannel")
    public MessageHandler mqttInboundHandler(MqttMessageHandler handler) {
        return handler;
    }
}

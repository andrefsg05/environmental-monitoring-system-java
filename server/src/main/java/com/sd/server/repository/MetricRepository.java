
package com.sd.server.repository;

import java.time.LocalDateTime; // Data/hora
import java.util.List; // Listas

import org.springframework.data.jpa.repository.JpaRepository; // Repositório JPA base
import org.springframework.data.jpa.repository.Modifying; // Marca query que modifica dados
import org.springframework.data.jpa.repository.Query; // Define query customizada
import org.springframework.data.repository.query.Param; // Parâmetro nomeado
import org.springframework.stereotype.Repository; // Marca como repositório
import org.springframework.transaction.annotation.Transactional; // Controle transacional

import com.sd.server.entity.Metric; // Entidade métrica

/**
 * Repositório Spring Data JPA para entidade Metric.
 * Fornece queries complexas para busca e agregação de métricas.
 * Inclui queries nativas SQL para cálculos de médias, mínimos, máximos e contagens.
 */
@Repository
public interface MetricRepository extends JpaRepository<Metric, Long> {

    /**
     * Encontra todas as métricas de um dispositivo específico.
     * 
     * @param deviceId identificador do dispositivo
     * @return lista de todas as métricas do dispositivo
     */
    @Query(
        "SELECT m " +
        "FROM Metric m " +
        "WHERE m.device.id = :deviceId"
    )



    List<Metric> findByDeviceId(@Param("deviceId") String deviceId);
    /**
     * Deleta todas as métricas de um dispositivo.
     * Chamado ao remover um dispositivo para limpeza de dados.
     * 
     * @param deviceId identificador do dispositivo
     */
    @Modifying
    @Transactional
    @Query(
        "DELETE FROM Metric m " +
        "WHERE m.device.id = :deviceId"
    )
    void deleteByDeviceId(@Param("deviceId") String deviceId);



    /**
     * Deleta todas as métricas da base de dados.
     * Usado para limpeza completa dos dados de métricas.
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM Metric m")
    void deleteAllMetrics();



    /**
     * Encontra métricas de um dispositivo em intervalo de tempo.
     */
    @Query(
        "SELECT m " +
        "FROM Metric m " +
        "WHERE m.device.id = :deviceId " +
        "AND m.timestamp BETWEEN :from AND :to"
    )
    List<Metric> findByDeviceIdAndTimestampBetween(
        @Param("deviceId") String deviceId,
        @Param("from") LocalDateTime from,
        @Param("to") LocalDateTime to
    );



    /**
     * Encontra métricas de uma sala em intervalo de tempo.
     * 
     * @param room identificador da sala
     * @param from data/hora inicial
     * @param to data/hora final
     * @return lista de métricas de dispositivos na sala
     */
    @Query(
        "SELECT m " +
        "FROM Metric m " +
        "JOIN m.device d " +
        "WHERE d.room = :room " +
        "AND m.timestamp BETWEEN :from AND :to"
    )
    List<Metric> findByRoomAndTimestampBetween(
        @Param("room") String room,
        @Param("from") LocalDateTime from,
        @Param("to") LocalDateTime to
    );



    /**
     * Encontra métricas de um departamento em intervalo de tempo.
     * 
     * @param department nome do departamento
     * @param from data/hora inicial
     * @param to data/hora final
     * @return lista de métricas de dispositivos do departamento
     */
    @Query(
        "SELECT m " +
        "FROM Metric m " +
        "JOIN m.device d " +
        "WHERE d.department = :department " +
        "AND m.timestamp BETWEEN :from AND :to"
    )
    List<Metric> findByDepartmentAndTimestampBetween(
        @Param("department") String department,
        @Param("from") LocalDateTime from,
        @Param("to") LocalDateTime to
    );



    /**
     * Encontra métricas de um andar em intervalo de tempo.
     * 
     * @param floor identificador do andar
     * @param from data/hora inicial
     * @param to data/hora final
     * @return lista de métricas de dispositivos no andar
     */
    @Query(
        "SELECT m " +
        "FROM Metric m " +
        "JOIN m.device d " +
        "WHERE d.floor = :floor " +
        "AND m.timestamp BETWEEN :from AND :to"
    )
    List<Metric> findByFloorAndTimestampBetween(
        @Param("floor") String floor,
        @Param("from") LocalDateTime from,
        @Param("to") LocalDateTime to
    );



    /**
     * Encontra métricas de um prédio em intervalo de tempo.
     * 
     * @param building identificador do prédio
     * @param from data/hora inicial
     * @param to data/hora final
     * @return lista de métricas de dispositivos no prédio
     */
    @Query(
        "SELECT m " +
        "FROM Metric m " +
        "JOIN m.device d " +
        "WHERE d.building = :building " +
        "AND m.timestamp BETWEEN :from AND :to"
    )
    List<Metric> findByBuildingAndTimestampBetween(
        @Param("building") String building,
        @Param("from") LocalDateTime from,
        @Param("to") LocalDateTime to
    );



    /**
    * Calcula média, mínimos, máximos de temperatura/humidade e contagem por sala.
    * Query nativa SQL retorna Object[7]: {avgTemp, avgHumidity, count, minTemp, maxTemp, minHum, maxHum}
     * 
     * @param room identificador da sala
     * @param from data/hora inicial
     * @param to data/hora final
     * @return array com [avgTemperature, avgHumidity, count] ou null se sem dados
     */
    @Query(
        value = "SELECT AVG(m.temperature), " +
                "AVG(m.humidity), " +
                "COUNT(m), " +
                "MIN(m.temperature), " +
                "MAX(m.temperature), " +
                "MIN(m.humidity), " +
                "MAX(m.humidity) " +
                "FROM metrics m " +
                "JOIN devices d ON m.device_id = d.id " +
                "WHERE d.room = :room " +
                "AND m.timestamp BETWEEN :from AND :to",
        nativeQuery = true
    )
    Object[] getAverageByRoom(
        @Param("room") String room,
        @Param("from") LocalDateTime from,
        @Param("to") LocalDateTime to
    );



    @Query(
        value = "SELECT AVG(m.temperature), " +
                "AVG(m.humidity), " +
                "COUNT(m), " +
                "MIN(m.temperature), " +
                "MAX(m.temperature), " +
                "MIN(m.humidity), " +
                "MAX(m.humidity) " +
                "FROM metrics m " +
                "JOIN devices d ON m.device_id = d.id " +
                "WHERE d.department = :department " +
                "AND m.timestamp BETWEEN :from AND :to",
        nativeQuery = true
    )
    Object[] getAverageByDepartment(
        @Param("department") String department,
        @Param("from") LocalDateTime from,
        @Param("to") LocalDateTime to
    );



    /**
    * Calcula média, mínimos, máximos de temperatura/humidade e contagem por andar.
     * 
     * @param floor identificador do andar
     * @param from data/hora inicial
     * @param to data/hora final
     * @return array com [avgTemperature, avgHumidity, count]
     */
    @Query(
        value = "SELECT AVG(m.temperature), " +
                "AVG(m.humidity), " +
                "COUNT(m), " +
                "MIN(m.temperature), " +
                "MAX(m.temperature), " +
                "MIN(m.humidity), " +
                "MAX(m.humidity) " +
                "FROM metrics m " +
                "JOIN devices d ON m.device_id = d.id " +
                "WHERE d.floor = :floor " +
                "AND m.timestamp BETWEEN :from AND :to",
        nativeQuery = true
    )
    Object[] getAverageByFloor(
        @Param("floor") String floor,
        @Param("from") LocalDateTime from,
        @Param("to") LocalDateTime to
    );



    /**
    * Calcula média, mínimos, máximos de temperatura/humidade e contagem por prédio.
     * 
     * @param building identificador do prédio
     * @param from data/hora inicial
     * @param to data/hora final
     * @return array com [avgTemperature, avgHumidity, count]
     */
    @Query(
        value = "SELECT AVG(m.temperature), " +
                "AVG(m.humidity), " +
                "COUNT(m), " +
                "MIN(m.temperature), " +
                "MAX(m.temperature), " +
                "MIN(m.humidity), " +
                "MAX(m.humidity) " +
                "FROM metrics m " +
                "JOIN devices d ON m.device_id = d.id " +
                "WHERE d.building = :building " +
                "AND m.timestamp BETWEEN :from AND :to",
        nativeQuery = true
    )
    Object[] getAverageByBuilding(
        @Param("building") String building,
        @Param("from") LocalDateTime from,
        @Param("to") LocalDateTime to
    );



    /**
     * Conta o número total de métricas recebidas por protocolo.
     * 
     * @param protocol tipo de protocolo (MQTT, GRPC, REST)
     * @return número de métricas para o protocolo especificado
     */
    @Query(
        value = "SELECT COUNT(m) " +
                "FROM metrics m " +
                "JOIN devices d ON m.device_id = d.id " +
                "WHERE d.protocol = :protocol",
        nativeQuery = true
    )
    Long countMetricsByProtocol(@Param("protocol") String protocol);



    /**
     * Calcula a latência média (em milissegundos) entre o timestamp da métrica e o receivedAt.
     * Representa o tempo médio entre a criação da métrica no cliente e o recebimento no servidor.
     * 
     * @param protocol tipo de protocolo (MQTT, GRPC, REST)
     * @return latência média em milissegundos, ou null se sem dados
     */
    @Query(
        value = "SELECT AVG(EXTRACT(EPOCH FROM (m.received_at - m.timestamp)) * 1000.0) " +
                "FROM metrics m " +
                "JOIN devices d ON m.device_id = d.id " +
                "WHERE d.protocol = :protocol",
        nativeQuery = true
    )
    Double getAverageLatencyByProtocol(@Param("protocol") String protocol);
}

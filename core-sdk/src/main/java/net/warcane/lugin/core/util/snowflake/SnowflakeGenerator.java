package net.warcane.lugin.core.util.snowflake;

import lombok.Getter;

import java.time.Instant;

/**
 * Gerador de Snowflake compacto e thread-safe
 * Formato: 1 bit (sinal) + 41 bits (timestamp) + 10 bits (machine) + 12 bits (sequence)
 * Garante até 4096 IDs por milissegundo por máquina
 */
public class SnowflakeGenerator {

    private static final class InstanceHolder {
        private static final SnowflakeGenerator instance = new SnowflakeGenerator();
    }

    public static SnowflakeGenerator getInstance() {
        return InstanceHolder.instance;
    }

    // Epoch personalizado (1 de janeiro de 2020)
    private static final long CUSTOM_EPOCH = 1577836800000L;

    // Bits para cada componente
    private static final int MACHINE_BITS = 10;
    private static final int SEQUENCE_BITS = 12;

    // Valores máximos
    private static final long MAX_MACHINE_ID = (1L << MACHINE_BITS) - 1;
    private static final long MAX_SEQUENCE = (1L << SEQUENCE_BITS) - 1;

    // Shifts para posicionamento dos bits
    private static final int MACHINE_SHIFT = SEQUENCE_BITS;
    private static final int TIMESTAMP_SHIFT = SEQUENCE_BITS + MACHINE_BITS;

    // Estado do gerador
    private final long machineId;
    private long lastTimestamp = -1L;
    private long sequence = 0L;

    /**
     * Construtor com ID da máquina automático baseado no hashCode do hostname
     */
    public SnowflakeGenerator() {
        this(generateMachineId());
    }

    /**
     * Construtor com ID da máquina específico
     *
     * @param machineId ID único da máquina (0-1023)
     */
    public SnowflakeGenerator(long machineId) {
        if (machineId < 0 || machineId > MAX_MACHINE_ID) {
            throw new IllegalArgumentException(
              "Machine ID deve estar entre 0 e " + MAX_MACHINE_ID);
        }
        this.machineId = machineId;
    }

    /**
     * Gera um novo ID Snowflake único
     *
     * @return ID único de 64 bits
     */
    public synchronized long nextId() {
        long currentTimestamp = getCurrentTimestamp();
        if (currentTimestamp < lastTimestamp) {
            throw new RuntimeException("Relógio do sistema moveu para trás. " +
                                       "Último timestamp: " + lastTimestamp +
                                       ", atual: " + currentTimestamp);
        }

        if (currentTimestamp == lastTimestamp) {
            sequence = (sequence + 1) & MAX_SEQUENCE;
            if (sequence == 0) {
                currentTimestamp = waitNextMillis(currentTimestamp);
            }
        } else {
            sequence = 0L;
        }

        lastTimestamp = currentTimestamp;

        // Constrói o ID final
        return ((currentTimestamp - CUSTOM_EPOCH) << TIMESTAMP_SHIFT) |
               (machineId << MACHINE_SHIFT) |
               sequence;
    }

    /**
     * Gera um ID como String em formato hexadecimal
     *
     * @return ID único em formato hex
     */
    public String nextIdHex() {
        return Long.toHexString(nextId()).toUpperCase();
    }

    /**
     * Extrai informações de um ID Snowflake
     *
     * @param id ID para analisar
     * @return Informações do ID
     */
    public SnowflakeInfo parse(long id) {
        long timestamp = ((id >> TIMESTAMP_SHIFT) + CUSTOM_EPOCH);
        long machineId = (id >> MACHINE_SHIFT) & MAX_MACHINE_ID;
        long sequence = id & MAX_SEQUENCE;

        return new SnowflakeInfo(timestamp, machineId, sequence);
    }

    private long getCurrentTimestamp() {
        return Instant.now().toEpochMilli();
    }

    private long waitNextMillis(long lastTimestamp) {
        long timestamp = getCurrentTimestamp();
        while (timestamp <= lastTimestamp) {
            timestamp = getCurrentTimestamp();
        }
        return timestamp;
    }

    private static long generateMachineId() {
        try {
            String hostname = java.net.InetAddress.getLocalHost().getHostName();
            return Math.abs(hostname.hashCode()) % (MAX_MACHINE_ID + 1);
        } catch (Exception e) {
            // Fallback: usa hashCode da thread atual + timestamp
            return Math.abs((Thread.currentThread().getName().hashCode() +
                             System.nanoTime())) % (MAX_MACHINE_ID + 1);
        }
    }

    // Classe para informações do Snowflake
    @Getter
    public static class SnowflakeInfo {
        private final long timestamp;
        private final long machineId;
        private final long sequence;

        public SnowflakeInfo(long timestamp, long machineId, long sequence) {
            this.timestamp = timestamp;
            this.machineId = machineId;
            this.sequence = sequence;
        }

        @Override
        public String toString() {
            return String.format("SnowflakeInfo{timestamp=%d (%s), machineId=%d, sequence=%d}",
              timestamp, Instant.ofEpochMilli(timestamp), machineId, sequence);
        }
    }
}
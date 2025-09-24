package net.warcane.lugin.core.minecraft.punish.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import net.warcane.lugin.core.minecraft.punish.utils.MessageUtils;
import org.bson.codecs.pojo.annotations.BsonIgnore;

import java.util.List;
import java.util.UUID;

/**
 * @author Rok, Pedro Lucas nmm. Created on 30/06/2025
 * @project punish
 */
@Setter
@Getter
public class PunishedDTO {

    private String name;
    private UUID uuid;
    private List<Punishment> punishments;

    public PunishedDTO() {}

    public PunishedDTO(String name, UUID uuid, List<Punishment> punishments) {
        this.name = name;
        this.uuid = uuid;
        this.punishments = punishments;
    }

    @Setter
    @Getter
    public static class Punishment {
        private int id;
        private String ipAddress;
        private int repeatCount;
        private int punishmentInfoId;
        private String evidence;
        private long appliedAt;
        private long expiresAt;
        private UUID punisherUuid;
        private PunishmentStatus status;

        private UUID revokerUuid;
        private long revokedAt;
        private String revokeReason;

        public Punishment() {}

        public Punishment(int id, String ipAddress, int repeatCount, int punishmentInfoId, String evidence,
                          long appliedAt, long expiresAt, UUID punisherUuid) {
            this.id = id;
            this.ipAddress = ipAddress;
            this.repeatCount = repeatCount;
            this.punishmentInfoId = punishmentInfoId;
            this.evidence = evidence;
            this.appliedAt = appliedAt;
            this.expiresAt = expiresAt;
            this.punisherUuid = punisherUuid;
        }

        @JsonIgnore
        @BsonIgnore
        public String getAppliedAtFormatted() {
            return MessageUtils.getFormattedTime(appliedAt);
        }

        @JsonIgnore
        @BsonIgnore
        public String getExpiresAtFormatted() {
            if (expiresAt == -1) {
                return "Permanente";
            }
            return MessageUtils.getFormattedTime(expiresAt);
        }

        @JsonIgnore
        @BsonIgnore
        public String getRevokedAtFormatted() {
            if (revokedAt == 0) {
                return "Não revogada";
            }
            return MessageUtils.getFormattedTime(revokedAt);
        }

        @JsonIgnore
        public String serializeToRedis() {
            return String.format("%d|%d|%d|%s|%d|%s|%s|%d|%s",
                    id, repeatCount, punishmentInfoId, evidence,
                    expiresAt, punisherUuid.toString(),
                    status.name(), revokedAt, revokeReason != null ? revokeReason : "");
        }

        @JsonIgnore
        public static Punishment fromRedis(String serialized) {
            String[] parts = serialized.split("\\|");
            if (parts.length < 6) {
                throw new IllegalArgumentException("Serialized punishment data is invalid.");
            }

            int id = Integer.parseInt(parts[0]);
            int repeatCount = Integer.parseInt(parts[1]);
            int punishmentInfoId = Integer.parseInt(parts[2]);
            String evidence = parts[3];
            long expiresAt = Long.parseLong(parts[4]);
            UUID punisherUuid = UUID.fromString(parts[5]);
            PunishmentStatus status = PunishmentStatus.valueOf(parts[6]);
            long revokedAt = parts.length > 7 ? Long.parseLong(parts[7]) : 0;
            String revokeReason = parts.length > 8 ? parts[8] : null;
            Punishment punishment = new Punishment(id, "", repeatCount, punishmentInfoId, evidence,
                    System.currentTimeMillis(), expiresAt, punisherUuid);
            punishment.setStatus(status);
            punishment.setRevokedAt(revokedAt);
            punishment.setRevokeReason(revokeReason);


            return punishment;
        }

        @Override
        public String toString() {
            return "Punishment{" +
                    "id=" + id +
                    ", ipAddress='" + ipAddress + '\'' +
                    ", repeatCount=" + repeatCount +
                    ", punishmentInfoId=" + punishmentInfoId +
                    ", evidence='" + evidence + '\'' +
                    ", appliedAt=" + appliedAt +
                    ", expiresAt=" + expiresAt +
                    ", punisherUuid=" + punisherUuid +
                    ", status=" + status +
                    ", revokerUuid=" + revokerUuid +
                    ", revokedAt=" + revokedAt +
                    ", revokeReason='" + revokeReason + '\'' +
                    '}';
        }
    }

    @Override
    public String toString() {
        return "PunishedDTO{" +
                "name='" + name + '\'' +
                ", uuid=" + uuid +
                ", punishments=" + punishments +
                '}';
    }
}

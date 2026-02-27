package io.github.minehollow.minecraft.wallet;

import java.time.Instant;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record WalletTransactionContext(
    @Nullable UUID initiatorId,
    @Nullable  UUID targetId,
    @Nullable String reason,
    @NotNull Instant timestamp
) {

    public static Builder builder() {
        return new Builder();
    }

    public boolean isSystemInitiated() {
        return initiatorId == null;
    }

    public boolean isSystemTargeted() {
        return targetId == null;
    }

    public static class Builder {
        private UUID initiatorId;
        private UUID targetId;
        private String reason;
        private Instant timestamp = Instant.now();

        public Builder withInitiatorId(@Nullable UUID initiatorId) {
            this.initiatorId = initiatorId;
            return this;
        }

        public Builder withTargetId(@Nullable UUID targetId) {
            this.targetId = targetId;
            return this;
        }

        public Builder withReason(@Nullable String reason) {
            this.reason = reason;
            return this;
        }

        public Builder withTimestamp(@NotNull Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public WalletTransactionContext build() {
            return new WalletTransactionContext(initiatorId, targetId, reason, timestamp);
        }
    }
}
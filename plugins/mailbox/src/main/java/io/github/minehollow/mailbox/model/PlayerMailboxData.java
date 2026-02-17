package io.github.minehollow.mailbox.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.codecs.pojo.annotations.BsonId;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlayerMailboxData {
    @BsonId
    private UUID playerId;
    private List<MailboxItem> boxes = new ArrayList<>();

    public static PlayerMailboxData createNew(@NotNull UUID playerId) {
        return new PlayerMailboxData(playerId, new ArrayList<>());
    }

    public int getPendingCount() {
        return boxes.size();
    }

    public MailboxItem getBoxById(@NotNull String boxId) {
        return boxes.stream()
                .filter(box -> box.getId().equals(boxId))
                .findFirst()
                .orElse(null);
    }

    public boolean removeBox(@NotNull String boxId) {
        return boxes.removeIf(box -> box.getId().equals(boxId));
    }
}

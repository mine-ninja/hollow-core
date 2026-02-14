package io.github.minehollow.quests.quest;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.codecs.pojo.annotations.BsonId;
import org.jetbrains.annotations.Nullable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuestTemplate {
    @BsonId
    private String id;
    private String displayName;
    private QuestType type;
    @Nullable
    private String targetFilter;
}

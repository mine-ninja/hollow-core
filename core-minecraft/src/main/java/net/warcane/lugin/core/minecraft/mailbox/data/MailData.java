package net.warcane.lugin.core.minecraft.mailbox.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * @author Rok, Pedro Lucas nmm. Created on 21/10/2025
 * @project LUGIN
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MailData {
    private UUID uniqueId;
    private List<MailItem> mails = new ArrayList<>();

    public static MailData create(UUID uniqueId) {
        return new MailData(uniqueId, new ArrayList<>());
    }
}

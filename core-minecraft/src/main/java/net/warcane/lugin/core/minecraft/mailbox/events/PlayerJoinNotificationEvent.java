package net.warcane.lugin.core.minecraft.mailbox.events;

import lombok.RequiredArgsConstructor;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.warcane.lugin.core.minecraft.BukkitPlatform;
import net.warcane.lugin.core.minecraft.BukkitPlatformPlugin;
import net.warcane.lugin.core.minecraft.mailbox.MailManager;
import net.warcane.lugin.core.minecraft.task.Tasks;
import net.warcane.lugin.core.minecraft.util.message.StringUtils;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * @author Rok, Pedro Lucas nmm. Created on 21/10/2025
 * @project LUGIN
 */
@RequiredArgsConstructor
public class PlayerJoinNotificationEvent implements Listener {

    private final MailManager mailManager;

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        mailManager.getMailData(event.getPlayer().getUniqueId()).whenCompleteAsync((mailData, throwable) -> {
            if (throwable != null) {
                return;
            }
            if (mailData == null) return;
            if (mailData.getMails() == null || mailData.getMails().isEmpty()) return;
            boolean isSameServer = false;
            String serverId = BukkitPlatform.getInstance().getGameServer().serverId();
            for (var mail : mailData.getMails()) {
                if (serverId.startsWith(mail.getServerId())) {
                    isSameServer = true;
                    break;
                }
            }
            if (!isSameServer) return;
            Audience audience = BukkitPlatformPlugin.getInstance().adventure().player(event.getPlayer());
            StringUtils.send(audience, "<l-info>Você tem <l-yellow>" + mailData.getMails().size() + " <l-gray>item(s) na sua caixa de correio. Use <l-yellow>/mail<l-gray> para acessá-la.");
            audience.playSound(Sound.sound().type(Key.key("entity.player.levelup")).pitch(2).volume(0.5f).build());
        }, r -> Tasks.runAsyncLater(r, 20L * 5));
    }
}

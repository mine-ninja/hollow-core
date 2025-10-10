package net.warcane.lugin.core.minecraft.menu.input;

import net.warcane.lugin.core.minecraft.BukkitPlatform;
import net.warcane.lugin.core.minecraft.menu.PlayerMenuContext;
import net.warcane.lugin.core.minecraft.menu.SimpleMenu;
import net.warcane.lugin.core.minecraft.menu.SimpleMenuManager;
import net.warcane.lugin.core.minecraft.menu.config.MenuConfig;
import org.bukkit.entity.Player;

import lombok.Builder;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import java.util.Map;
import java.util.function.Consumer;

@Getter
public class SignInputMenu extends SimpleMenu {
    public boolean onPreOpen(@NotNull SignInputContext ctx, @NotNull MenuConfig openHandler) {
        if (ctx.get("response") == null) {
            ctx.getPlayer().sendMessage("§cErro ao abrir o menu de input, tente novamente mais tarde.");
            ctx.getMenuConfig().playCloseSound(ctx.getPlayer());
            return false;
        }
        
        return super.onPreOpen(ctx, openHandler);
    }
    
    @Override
    protected void openToPlayer(@NotNull SimpleMenuManager manager, @NotNull Player player, @NotNull Map<String, Object> initialData) {
        final var openHandler = new MenuConfig(defaultConfig);
        
        final SignInputContext ctx = new SignInputContext(player, initialData, openHandler, this, manager);
        if (!this.onPreOpen(ctx, openHandler)) { return; }
        
        playerContexts.put(player.getUniqueId(), ctx);
        ctx.open();
    }
    
    public static SignInputContext remove(Player player) {
        PlayerMenuContext context = BukkitPlatform.getInstance().getMenuManager().getMenu(SignInputMenu.class).getPlayerContexts().remove(player.getUniqueId());
        return context == null ? null : (SignInputContext) context;
    }
    
    @Builder
    public static final class Response {
        public static final Response EMPTY = Response.builder().build();
        
        @Builder.Default
        public final Consumer<SignInputContext> handler = ctx -> { };
    }
}

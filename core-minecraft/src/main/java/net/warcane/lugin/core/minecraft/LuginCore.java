package net.warcane.lugin.core.minecraft;

import lombok.extern.slf4j.Slf4j;
import net.warcane.lugin.core.minecraft.command.SimpleCommand;
import net.warcane.lugin.core.minecraft.command.context.CommandContext;
import net.warcane.lugin.core.minecraft.command.exception.CommandFailedException;
import net.warcane.lugin.core.minecraft.hologram.HologramBuilder;
import net.warcane.lugin.core.minecraft.hologram.HologramManager;
import net.warcane.lugin.core.minecraft.npc.NpcBuilder;
import net.warcane.lugin.core.minecraft.npc.NpcManager;
import net.warcane.lugin.core.minecraft.npc.provider.NpcSkinProvider;
import net.warcane.lugin.core.minecraft.plugin.SimplePlugin;
import net.warcane.lugin.core.minecraft.task.Tasks;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ThreadLocalRandom;

@Slf4j
public class LuginCore extends SimplePlugin {

    private static final ThreadLocalRandom RANDOM = ThreadLocalRandom.current();

    private NpcManager npcManager;
    private HologramManager hologramManager;


    @Override
    public void onEnable() {
        this.npcManager = new NpcManager(this);
        this.hologramManager = new HologramManager(this);
        registerCommands("npctest", new SimpleNpcCommand());
    }


    class SimpleNpcCommand extends SimpleCommand {
        public SimpleNpcCommand() {
            super("npctest");
            this.requiredPermission = "lugin.npctest";
        }

        @Override
        public void performCommand(@NotNull CommandContext ctx) throws CommandFailedException {
            var skinName = ctx.getRawArgOrThrow(0, "§cInforme o nome do skin do NPC.");

            Tasks.runAsync(() -> {
                var npcLocation = ctx.getSenderAsPlayer().getLocation();


                var npcHologram = new HologramBuilder(npcLocation.clone().add(0, 2.3, 0))
                  .withAutoUpdate(false)
                  .withLine("§3§LNPC CONTADOR")
                  .withLine(player -> {
                      var randomColor = "§" + RANDOM.nextInt(0, 9);
                      return randomColor + "§lCLIQUE AQUI";
                  })
                  .build(hologramManager);

                var newNpc = new NpcBuilder(npcLocation)
                  .withSkinProvider(NpcSkinProvider.fromPlayerName(skinName))
                  .withInteractListener((npc, player, clickType) -> {
                      int counter = npc.getMetadata("clicks", 0) + 1;
                      npc.setMetadata("clicks", counter);


                      npcHologram.updateAllLines();
                      player.sendMessage("§aVocê clicou no NPC %d %d vezes.".formatted(npc.getEntityId(), counter));
                  })
                  .build(npcManager);

                ctx.sendMessage("§aNPC created successfully! " + newNpc.getEntityId());
            });
        }
    }
}

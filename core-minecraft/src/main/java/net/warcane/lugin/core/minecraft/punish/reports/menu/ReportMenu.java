package net.warcane.lugin.core.minecraft.punish.reports.menu;

import lombok.extern.slf4j.Slf4j;
import net.kyori.adventure.audience.Audience;
import net.warcane.lugin.core.minecraft.BukkitPlatformPlugin;
import net.warcane.lugin.core.minecraft.menu.PlayerMenuContext;
import net.warcane.lugin.core.minecraft.menu.SimpleMenu;
import net.warcane.lugin.core.minecraft.menu.config.MenuConfig;
import net.warcane.lugin.core.minecraft.punish.api.PunishManager;
import net.warcane.lugin.core.minecraft.punish.reports.ReportManager;
import net.warcane.lugin.core.minecraft.task.Tasks;
import net.warcane.lugin.core.minecraft.util.message.StringUtils;
import net.warcane.lugin.core.minecraft.util.message.input.ChatInput;
import net.warcane.lugin.core.player.account.PlayerAccount;
import net.warcane.lugin.core.punish.data.PunishmentInfo;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Rok, Pedro Lucas nmm. 04/01/2026
 * @project lugin-core
 */
@Slf4j
public class ReportMenu extends SimpleMenu {

    public static final String EVIDENCE_KEY = "evidence";
    public static final String REASON_KEY = "reason";
    public static final String REPORTED_ACCOUNT_KEY = "reported";

    @Override
    public boolean onPreOpen(@NotNull PlayerMenuContext ctx, @NotNull MenuConfig openHandler) {
        final var target = ctx.get(REPORTED_ACCOUNT_KEY);
        final var audience = BukkitPlatformPlugin.getInstance().adventure().player(ctx.getPlayer());
        if (target == null) {
            StringUtils.send(audience, "<l-error>Não foi possível abrir o menu de reports, contate um administrador.");
            log.error("ReportMenu: target player account is null");
            return false;
        }

        openHandler.setLayout(
            "         ",
            " XXXXXXX ",
            " XXXXXXX ",
            " XXXXXXX ",
            "         ",
            " C  E  S "
        );
        openHandler.setTitle("§8Reportando §l"+((PlayerAccount) target).playerName());

        setupReportReasonItems(ctx, (PlayerAccount) target);
        setupActionItems(ctx, (PlayerAccount) target);
        setupEvidenceItem(ctx, audience);
        return true;
    }

    private void setupReportReasonItems(@NotNull PlayerMenuContext ctx, @NotNull PlayerAccount reported) {
        final var reason = ctx.get(REASON_KEY);
        List<PunishmentInfo> reportReasons = new ArrayList<>();
        for (PunishmentInfo punishment : PunishmentInfo.PUNISHMENTS) {
            if (!punishment.reportable()) continue;
            reportReasons.add(punishment);
        }
        ctx.setItem('X', (index, builder) -> {
            if (index >= reportReasons.size()) {
                return;
            }
            PunishmentInfo report = reportReasons.get(index);

            builder.renderer(player -> {
                boolean isSelected = reason != null && reason.equals(report);
                ItemStack itemStack = ItemStack.of(isSelected ? Material.EMERALD : Material.PAPER);
                ItemMeta itemMeta = itemStack.getItemMeta();

                String displayText =
                    isSelected ?
                        "§a§lReportando §e§l" + reported.playerName() + " §a§lpor §e§l" + report.title() :
                        "§7Reportar §e" + reported.playerName() + " §7por §e" + report.title();
                itemMeta.setDisplayName(displayText);

                String loreSelectText =
                    isSelected ?
                        "§aMotivo selecionado!" :
                        "§eClique para selecionar este motivo de denúncia.";
                itemMeta.setLore(List.of("", "§7Descrição:", "§f" + report.description(), "", loreSelectText));
                itemStack.setItemMeta(itemMeta);
                return itemStack;
            });

            builder.clickHandler(click -> {
                ctx.put(REASON_KEY, report);
                ctx.update();
            });
        });
    }

    private void setupActionItems(@NotNull PlayerMenuContext ctx, PlayerAccount reported) {
        final var reason = ctx.get(REASON_KEY);

        ctx.setItem('C', (index, builder) -> {
            builder.renderer(player -> {
                ItemStack itemStack = ItemStack.of(Material.BARRIER);
                ItemMeta itemMeta = itemStack.getItemMeta();
                itemMeta.setDisplayName("§c§lCancelar Report");
                itemMeta.setLore(List.of("", "§7Clique para cancelar o report e fechar este menu."));
                itemStack.setItemMeta(itemMeta);
                return itemStack;
            });

            builder.clickHandler(click -> {
                ctx.getPlayer().closeInventory();
            });
        });

        ctx.setItem('S', (index, builder) -> {
            builder.renderer(player -> {
                boolean emptyReason = reason == null;
                ItemStack itemStack = ItemStack.of(emptyReason ? Material.GRAY_STAINED_GLASS_PANE : Material.LIME_STAINED_GLASS_PANE);
                ItemMeta itemMeta = itemStack.getItemMeta();
                itemMeta.setDisplayName("§a§lEnviar Report");
                if (emptyReason) {
                    itemMeta.setLore(List.of("", "§7Selecione um motivo para o report antes de enviar."));
                } else {
                    itemMeta.setLore(List.of("", "§7Clique para enviar o report contra §e" + reported.playerName() + "§7."));
                }
                itemStack.setItemMeta(itemMeta);
                return itemStack;
            });

            builder.clickHandler(click -> {
                final var audience = BukkitPlatformPlugin.getInstance().adventure().player(ctx.getPlayer());
                final var evidence = ctx.get(EVIDENCE_KEY);
                if (reason == null) {
                    StringUtils.send(audience, "<l-error>Selecione um motivo para o report antes de enviar.");
                    return;
                }
                ctx.close();
                ReportManager.get().reportPlayer(reported, ctx.getPlayer(), (PunishmentInfo) reason, (String) evidence);
            });
        });
    }

    private void setupEvidenceItem(@NotNull PlayerMenuContext ctx, Audience audience) {
        final var evidence = ctx.get(EVIDENCE_KEY);

        ctx.setItem('E', (index, builder) -> {
            builder.renderer(player -> {
                boolean emptyEvidence = evidence == null;
                ItemStack itemStack = ItemStack.of(emptyEvidence ? Material.FEATHER : Material.PAPER);
                ItemMeta itemMeta = itemStack.getItemMeta();
                itemMeta.setDisplayName("§e§lEvidência");
                if (emptyEvidence) {
                    itemMeta.setLore(List.of("", "§7Clique para adicionar uma evidência (link).", "§7Nenhuma evidência adicionada."));
                } else {
                    itemMeta.setLore(List.of("", "§7Clique para alterar a evidência (link).", "§aEvidência adicionada: §f" + evidence));
                }
                itemStack.setItemMeta(itemMeta);
                return itemStack;
            });

            builder.clickHandler(click -> {
                ctx.getPlayer().closeInventory();
                ChatInput.waitInput(ctx.getPlayer(), (link) -> {
                        if (!PunishManager.checkLink(link)) {
                            StringUtils.send(audience, "<l-error>O link inserido é inválido.");
                            ctx.openMenu(ReportMenu.class, true);
                            return;
                        }
                        Tasks.runSync(() -> {
                            Map<String, Object> data = new HashMap<>(Map.of(EVIDENCE_KEY, link));
                            ctx.openMenu(ReportMenu.class, true, data);
                        });
                    },
                    "<l-info>Insira o link da evidência para o report. (30 segundos...)");
            });
        });
    }
}

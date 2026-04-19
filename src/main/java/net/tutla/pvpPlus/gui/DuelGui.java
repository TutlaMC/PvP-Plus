package net.tutla.pvpPlus.gui;

import net.tutla.pvpPlus.arena.Arena;
import net.tutla.pvpPlus.arena.ArenaManager;
import net.tutla.pvpPlus.duel.DuelManager;
import net.tutla.pvpPlus.kit.Kit;
import net.tutla.pvpPlus.kit.KitManager;
import net.tutla.pvpPlus.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class DuelGui {

    // State held per-player while the GUI is open
    public record DuelConfig(Kit kit, Arena arena, int rounds, Player target) {}

    public static void open(Player player, DuelManager duelManager,
                            KitManager kitManager, Player target) {
        // Grab default kit
        Kit defaultKit = kitManager.getServerKits().stream().findFirst().orElse(null);
        DuelConfig config = new DuelConfig(defaultKit, null,
                defaultKit != null ? defaultKit.getDefaultRounds() : 3, target);
        openWith(player, config, duelManager, kitManager);
    }

    private static void openWith(Player player, DuelConfig config,
                                 DuelManager duelManager, KitManager kitManager) {
        Inventory inv = Bukkit.createInventory(null, 27,
                TextUtil.parse("<gold>⚔ Configure Duel"));

        // Slot 11 — Kit selector
        inv.setItem(11, makeKitItem(config.kit()));

        // Slot 13 — Rounds
        inv.setItem(13, makeRoundsItem(config.rounds()));

        // Slot 15 — Arena selector
        inv.setItem(15, makeArenaItem(config.arena()));

        // Slot 22 — Send challenge
        inv.setItem(22, makeSendItem(config));

        player.openInventory(inv);
        GuiManager.register(player, new DuelGuiHandler(config, duelManager, kitManager));
    }

    private static ItemStack makeKitItem(Kit kit) {
        ItemStack item = kit != null && kit.hasIcon()
                ? kit.getIcon().clone() : new ItemStack(Material.IRON_SWORD);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(TextUtil.parse("<aqua>Kit: <white>" +
                (kit != null ? kit.getName() : "None")));
        meta.lore(List.of(TextUtil.parse("<gray>Click to change")));
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack makeRoundsItem(int rounds) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(TextUtil.parse("<aqua>Rounds: <white>" + rounds));
        meta.lore(List.of(
                TextUtil.parse("<gray>Left click: <white>+1"),
                TextUtil.parse("<gray>Right click: <white>-1")
        ));
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack makeArenaItem(Arena arena) {
        ItemStack item = new ItemStack(arena != null ? Material.GRASS_BLOCK : Material.COMPASS);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(TextUtil.parse("<aqua>Arena: <white>" +
                (arena != null ? arena.getName() : "Auto")));
        meta.lore(List.of(TextUtil.parse("<gray>Click to change")));
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack makeSendItem(DuelConfig config) {
        boolean ready = config.kit() != null && config.target() != null;
        ItemStack item = new ItemStack(ready ? Material.LIME_CONCRETE : Material.RED_CONCRETE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(TextUtil.parse(ready ? "<green>Send Challenge!" : "<red>No target selected"));
        if (config.target() != null)
            meta.lore(List.of(TextUtil.parse("<gray>To: <white>" + config.target().getName())));
        item.setItemMeta(meta);
        return item;
    }

    public record DuelGuiHandler(
            DuelConfig config, DuelManager duelManager, KitManager kitManager
    ) implements GuiHandler {
        @Override
        public void onClick(InventoryClickEvent event) {
            event.setCancelled(true);
            Player player = (Player) event.getWhoClicked();
            int slot = event.getRawSlot();

            switch (slot) {
                case 11 -> // Kit
                        KitSelectGui.open(player, kitManager, 0, kit -> {
                            DuelConfig updated = new DuelConfig(kit, config.arena(),
                                    kit.getDefaultRounds(), config.target());
                            openWith(player, updated, duelManager, kitManager);
                        });
                case 13 -> { // Rounds
                    int delta = event.getClick() == ClickType.RIGHT ? -1 : 1;
                    int newRounds = Math.max(1, Math.min(99, config.rounds() + delta));
                    DuelConfig updated = new DuelConfig(config.kit(), config.arena(),
                            newRounds, config.target());
                    openWith(player, updated, duelManager, kitManager);
                }
                case 15 -> { // Arena — needs ArenaManager, stored in GuiManager context
                    player.sendMessage(TextUtil.parse(
                            "<yellow>Arena selection coming — pass ArenaManager into DuelGui."));
                }
                case 22 -> { // Send
                    if (config.target() == null || config.kit() == null) return;
                    player.closeInventory();
                    var result = duelManager.sendDuel(
                            player, config.target(), config.kit(),
                            config.arena(), config.rounds());
                    if (result == DuelManager.SendResult.SENT) {
                        player.sendMessage(TextUtil.parse(
                                "<green>Duel request sent to <yellow>" +
                                        config.target().getName() + "</yellow>!"));
                    } else {
                        player.sendMessage(TextUtil.parse("<red>Could not send duel: " + result));
                    }
                }
            }
        }
    }
}
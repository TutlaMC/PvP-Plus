package net.tutla.pvpPlus.gui;

import net.tutla.pvpPlus.PvpPlus;
import net.tutla.pvpPlus.arena.Arena;
import net.tutla.pvpPlus.arena.ArenaManager;
import net.tutla.pvpPlus.duel.DuelManager;
import net.tutla.pvpPlus.fight.FightTeam;
import net.tutla.pvpPlus.fight.FightType;
import net.tutla.pvpPlus.kit.Kit;
import net.tutla.pvpPlus.kit.KitManager;
import net.tutla.pvpPlus.party.Party;
import net.tutla.pvpPlus.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class DuelGui {

    // State held per-player while the GUI is open
    public record DuelConfig(
            Kit kit, Arena arena, int rounds,
            Player target, boolean enemyIsTeam,
            List<FightTeam> prebuiltTeams, FightType prebuiltType  // null for normal duels
    ) {}

    public static void openForTeams(Player player, DuelManager duelManager,
                                    KitManager kitManager,
                                    List<FightTeam> teams, FightType type) {
        Kit defaultKit = kitManager.getServerKits().stream().findFirst().orElse(null);
        DuelConfig config = new DuelConfig(
                defaultKit, null,
                defaultKit != null ? defaultKit.getDefaultRounds() : 3,
                null, false,
                teams, type
        );
        openWith(player, config, duelManager, kitManager);
    }

    public static void open(Player player, DuelManager duelManager,
                            KitManager kitManager, Player target, boolean enemyIsTeam) {
        Kit defaultKit = kitManager.getServerKits().stream().findFirst().orElse(null);
        DuelConfig config = new DuelConfig(
                defaultKit, null,
                defaultKit != null ? defaultKit.getDefaultRounds() : 3,
                target, enemyIsTeam,
                null, null  // no prebuilt teams
        );
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
        boolean ready = config.kit() != null &&
                (config.target() != null || config.prebuiltTeams() != null);
        ItemStack item = new ItemStack(ready ? Material.LIME_CONCRETE : Material.RED_CONCRETE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(TextUtil.parse(ready ? "<green>Start Fight!" : "<red>No kit selected"));
        if (config.prebuiltTeams() != null) {
            meta.lore(List.of(
                    TextUtil.parse("<gray>Teams: <white>" + config.prebuiltTeams().size()),
                    TextUtil.parse("<gray>Type: <white>" + config.prebuiltType().name())
            ));
        } else if (config.target() != null) {
            meta.lore(List.of(TextUtil.parse("<gray>To: <white>" + config.target().getName())));
        }
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
                case 11 ->
                        KitSelectGui.open(player, kitManager, 0, kit -> {
                            DuelConfig updated = new DuelConfig(kit, config.arena(),
                                    kit.getDefaultRounds(), config.target(), config.enemyIsTeam(),
                                    config.prebuiltTeams(), config.prebuiltType());
                            openWith(player, updated, duelManager, kitManager);
                        });
                case 13 -> {
                    int delta = event.getClick() == ClickType.RIGHT ? -1 : 1;
                    int newRounds = Math.max(1, Math.min(99, config.rounds() + delta));
                    DuelConfig updated = new DuelConfig(config.kit(), config.arena(),
                            newRounds, config.target(), config.enemyIsTeam(),
                            config.prebuiltTeams(), config.prebuiltType());
                    openWith(player, updated, duelManager, kitManager);
                }
                case 15 -> {
                    ArenaSelectGui.open(player, PvpPlus.getInstance().getArenaManager(), 0, arena -> {
                        DuelConfig updated = new DuelConfig(config.kit(), arena,
                                config.rounds(), config.target(), config.enemyIsTeam(),
                                config.prebuiltTeams(), config.prebuiltType());
                        openWith(player, updated, duelManager, kitManager);
                    });
                }
                case 22 -> {
                    if (config.kit() == null) return;
                    player.closeInventory();

                    if (config.prebuiltTeams() != null) {
                        Arena arena = config.arena() != null
                                ? config.arena()
                                : PvpPlus.getInstance().getArenaManager().getAvailableArena().orElse(null);

                        if (arena == null) {
                            player.sendMessage(TextUtil.parse("<red>No arenas available."));
                            return;
                        }

                        boolean started = PvpPlus.getInstance().getFightManager().startFight(
                                config.prebuiltType(),
                                config.prebuiltTeams(),
                                arena, config.kit(), config.rounds());

                        if (started) {
                            player.sendMessage(TextUtil.parse("<green>Fight started!"));
                        } else {
                            player.sendMessage(TextUtil.parse(
                                    "<red>Could not start — someone may already be in a fight."));
                        }
                        return;
                    }

                    if (config.enemyIsTeam()) {
                        // Party vs party duel — build teams from the two parties
                        Party myParty = PvpPlus.getInstance().getPartyManager().getParty(player);
                        Party enemyParty = config.target() != null
                                ? PvpPlus.getInstance().getPartyManager().getParty(config.target())
                                : null;

                        if (myParty == null || enemyParty == null) {
                            player.sendMessage(TextUtil.parse("<red>Party no longer valid."));
                            return;
                        }

                        Arena arena = config.arena() != null
                                ? config.arena()
                                : PvpPlus.getInstance().getArenaManager().getAvailableArena().orElse(null);

                        if (arena == null) {
                            player.sendMessage(TextUtil.parse("<red>No arenas available."));
                            return;
                        }

                        List<FightTeam> teams = List.of(
                                new FightTeam(player.getName() + "'s Party",
                                        new ArrayList<>(myParty.getMembers())),
                                new FightTeam(config.target().getName() + "'s Party",
                                        new ArrayList<>(enemyParty.getMembers()))
                        );

                        boolean started = PvpPlus.getInstance().getFightManager()
                                .startFight(FightType.PARTY_DUEL, teams, arena, config.kit(), config.rounds());

                        if (!started) {
                            player.sendMessage(TextUtil.parse("<red>Could not start fight — someone may already be in one."));
                        }
                    } else {
                        Player target = config.target();
                        if (target == null) return;
                        var result = duelManager.sendDuel(player, target, config.kit(), config.arena(), config.rounds());
                        switch (result) {
                            case SENT -> {
                                player.sendMessage(TextUtil.parse(
                                        "<green>Duel request sent to <yellow>" + target.getName() + "</yellow>. " +
                                                "<gray>(30s to accept)"));
                                target.sendMessage(TextUtil.parse(
                                        "<yellow>" + player.getName() + " <green>challenged you to a duel!\n" +
                                                "<gray>Kit: <white>" + config.kit().getName() +
                                                " <gray>| Rounds: <white>" + config.rounds() + "\n" +
                                                "<gray>Use <white>/duel accept</white> or <white>/duel deny</white>."));
                            }
                            case SELF_DUEL ->
                                    player.sendMessage(TextUtil.parse("<red>You can't duel yourself."));
                            case TARGET_BUSY ->
                                    player.sendMessage(TextUtil.parse("<red>That player is already in a fight."));
                            case SENDER_BUSY ->
                                    player.sendMessage(TextUtil.parse("<red>You are already in a fight."));
                            case REQUESTS_DISABLED ->
                                    player.sendMessage(TextUtil.parse("<red>That player has duel requests disabled."));
                            case ALREADY_PENDING ->
                                    player.sendMessage(TextUtil.parse("<red>That player already has a pending duel request."));
                            case NO_ARENA ->
                                    player.sendMessage(TextUtil.parse("<red>No arenas are available right now."));
                        }
                    }
                }
            }
        }
    }
}
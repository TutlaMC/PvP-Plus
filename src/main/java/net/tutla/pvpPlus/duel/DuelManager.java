package net.tutla.pvpPlus.duel;

import net.tutla.pvpPlus.arena.Arena;
import net.tutla.pvpPlus.arena.ArenaManager;
import net.tutla.pvpPlus.fight.*;
import net.tutla.pvpPlus.kit.Kit;
import net.tutla.pvpPlus.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class DuelManager {

    public record DuelRequest(
            UUID challenger,
            UUID challenged,
            Kit kit,
            Arena arena,       // null = auto-assign
            int rounds,
            long expiry        // System.currentTimeMillis
    ) {}

    private final Map<UUID, DuelRequest> pendingRequests = new HashMap<>(); // challenged -> request
    private final Set<UUID> duelToggleOff = new HashSet<>(); // players who disabled duel requests

    private final FightManager fightManager;
    private final ArenaManager arenaManager;
    private final JavaPlugin plugin;

    public DuelManager(JavaPlugin plugin, FightManager fightManager, ArenaManager arenaManager) {
        this.plugin = plugin;
        this.fightManager = fightManager;
        this.arenaManager = arenaManager;
    }

    // -------------------------------------------------------------------------
    // Send duel
    // -------------------------------------------------------------------------

    public enum SendResult {
        SENT, SELF_DUEL, TARGET_BUSY, SENDER_BUSY,
        REQUESTS_DISABLED, ALREADY_PENDING, NO_ARENA
    }

    public SendResult sendDuel(Player challenger, Player challenged, Kit kit, Arena arena, int rounds) {
        if (challenger.equals(challenged)) return SendResult.SELF_DUEL;
        if (fightManager.isInFight(challenger)) return SendResult.SENDER_BUSY;
        if (fightManager.isInFight(challenged)) return SendResult.TARGET_BUSY;
        if (duelToggleOff.contains(challenged.getUniqueId())) return SendResult.REQUESTS_DISABLED;
        if (pendingRequests.containsKey(challenged.getUniqueId())) return SendResult.ALREADY_PENDING;

        Arena selectedArena = arena != null ? arena : arenaManager.getAvailableArena().orElse(null);
        if (selectedArena == null) return SendResult.NO_ARENA;

        DuelRequest request = new DuelRequest(
                challenger.getUniqueId(), challenged.getUniqueId(),
                kit, selectedArena, rounds,
                System.currentTimeMillis() + 30_000L
        );
        pendingRequests.put(challenged.getUniqueId(), request);

        // Auto-expire after 30s
        new BukkitRunnable() {
            @Override public void run() {
                DuelRequest current = pendingRequests.get(challenged.getUniqueId());
                if (current != null && current.challenger().equals(challenger.getUniqueId())) {
                    pendingRequests.remove(challenged.getUniqueId());
                    if (challenger.isOnline())
                        challenger.sendMessage(TextUtil.parse(
                                "<yellow>Your duel request to <white>" + challenged.getName() +
                                        "</white> expired."));
                    if (challenged.isOnline())
                        challenged.sendMessage(TextUtil.parse(
                                "<yellow>Duel request from <white>" + challenger.getName() +
                                        "</white> expired."));
                }
            }
        }.runTaskLater(plugin, 600L); // 30 seconds

        return SendResult.SENT;
    }

    // -------------------------------------------------------------------------
    // Accept / Deny
    // -------------------------------------------------------------------------

    public boolean acceptDuel(Player challenged) {
        DuelRequest req = pendingRequests.remove(challenged.getUniqueId());
        if (req == null || System.currentTimeMillis() > req.expiry()) return false;

        Player challenger = Bukkit.getPlayer(req.challenger());
        if (challenger == null) return false;

        List<FightTeam> teams = List.of(
                new FightTeam(challenger.getName(), List.of(challenger.getUniqueId())),
                new FightTeam(challenged.getName(), List.of(challenged.getUniqueId()))
        );

        return fightManager.startFight(FightType.DUEL, teams, req.arena(), req.kit(), req.rounds());
    }

    public boolean denyDuel(Player challenged) {
        DuelRequest req = pendingRequests.remove(challenged.getUniqueId());
        if (req == null) return false;
        Player challenger = Bukkit.getPlayer(req.challenger());
        if (challenger != null)
            challenger.sendMessage(TextUtil.parse(
                    "<red>" + challenged.getName() + " denied your duel request."));
        return true;
    }

    public boolean hasPendingRequest(Player player) {
        return pendingRequests.containsKey(player.getUniqueId());
    }

    // -------------------------------------------------------------------------
    // Toggle
    // -------------------------------------------------------------------------

    public boolean toggleDuelRequests(Player player) {
        UUID uuid = player.getUniqueId();
        if (duelToggleOff.remove(uuid)) return true;  // now enabled
        duelToggleOff.add(uuid); return false;         // now disabled
    }

    public boolean hasDuelsEnabled(Player player) {
        return !duelToggleOff.contains(player.getUniqueId());
    }

    public DuelRequest getPendingRequest(Player player) {
        return pendingRequests.get(player.getUniqueId());
    }
}
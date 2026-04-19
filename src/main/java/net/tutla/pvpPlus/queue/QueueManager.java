package net.tutla.pvpPlus.queue;

import net.tutla.pvpPlus.arena.ArenaManager;
import net.tutla.pvpPlus.fight.*;
import net.tutla.pvpPlus.kit.Kit;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;

public class QueueManager {

    // kit name -> ordered queue of waiting players
    private final Map<String, Queue<UUID>> queues = new HashMap<>();
    // player -> kit they're queued for
    private final Map<UUID, String> playerQueue = new HashMap<>();

    private final FightManager fightManager;
    private final ArenaManager arenaManager;

    public QueueManager(FightManager fightManager, ArenaManager arenaManager) {
        this.fightManager = fightManager;
        this.arenaManager = arenaManager;
    }

    public enum JoinResult { JOINED, ALREADY_IN_QUEUE, IN_FIGHT, NO_KIT }

    public JoinResult joinQueue(Player player, Kit kit) {
        if (fightManager.isInFight(player)) return JoinResult.IN_FIGHT;
        if (playerQueue.containsKey(player.getUniqueId())) return JoinResult.ALREADY_IN_QUEUE;
        if (kit == null) return JoinResult.NO_KIT;

        String kitName = kit.getName();
        queues.computeIfAbsent(kitName, k -> new LinkedList<>()).add(player.getUniqueId());
        playerQueue.put(player.getUniqueId(), kitName);

        tryMatch(kitName, kit);
        return JoinResult.JOINED;
    }

    public boolean leaveQueue(Player player) {
        String kitName = playerQueue.remove(player.getUniqueId());
        if (kitName == null) return false;
        Queue<UUID> queue = queues.get(kitName);
        if (queue != null) queue.remove(player.getUniqueId());
        return true;
    }

    private void tryMatch(String kitName, Kit kit) {
        Queue<UUID> queue = queues.get(kitName);
        if (queue == null || queue.size() < 2) return;

        // Check an arena is available
        var arenaOpt = arenaManager.getAvailableArena();
        if (arenaOpt.isEmpty()) return;

        UUID uuid1 = queue.poll();
        UUID uuid2 = queue.poll();
        playerQueue.remove(uuid1);
        playerQueue.remove(uuid2);

        Player p1 = Bukkit.getPlayer(uuid1);
        Player p2 = Bukkit.getPlayer(uuid2);

        // If either went offline, put back valid one and retry
        if (p1 == null || p2 == null) {
            if (p1 != null) { queue.add(uuid1); playerQueue.put(uuid1, kitName); }
            if (p2 != null) { queue.add(uuid2); playerQueue.put(uuid2, kitName); }
            return;
        }

        List<FightTeam> teams = List.of(
                new FightTeam(p1.getName(), List.of(uuid1)),
                new FightTeam(p2.getName(), List.of(uuid2))
        );

        fightManager.startFight(FightType.QUEUE, teams, arenaOpt.get(), kit, kit.getDefaultRounds());
    }

    public boolean isInQueue(Player player) {
        return playerQueue.containsKey(player.getUniqueId());
    }

    public String getQueuedKit(Player player) {
        return playerQueue.get(player.getUniqueId());
    }

    public int getQueueSize(String kitName) {
        Queue<UUID> q = queues.get(kitName);
        return q == null ? 0 : q.size();
    }

    public Map<String, Queue<UUID>> getAllQueues() { return queues; }
}
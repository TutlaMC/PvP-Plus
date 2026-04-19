package net.tutla.pvpPlus.fight;

import net.tutla.pvpPlus.arena.Arena;
import net.tutla.pvpPlus.kit.Kit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class Fight {

    private final UUID id = UUID.randomUUID();
    private final FightType type;
    private final List<FightTeam> teams;
    private final Arena arena;
    private final Kit kit;
    private final int totalRounds;
    private int currentRound = 0;
    private FightState state = FightState.COUNTDOWN;

    // Kill log: killer UUID -> list of killed UUIDs this fight
    private final Map<UUID, List<UUID>> killLog = new HashMap<>();

    // Spectators
    private final Set<UUID> spectators = new HashSet<>();

    // Bukkit task handle so we can cancel countdowns/timers
    private BukkitTask activeTask = null;

    public Fight(FightType type, List<FightTeam> teams, Arena arena, Kit kit, int totalRounds) {
        this.type = type;
        this.teams = teams;
        this.arena = arena;
        this.kit = kit;
        this.totalRounds = totalRounds;
    }

    // --- Getters ---
    public UUID getId() { return id; }
    public FightType getType() { return type; }
    public List<FightTeam> getTeams() { return teams; }
    public Arena getArena() { return arena; }
    public Kit getKit() { return kit; }
    public int getTotalRounds() { return totalRounds; }
    public int getCurrentRound() { return currentRound; }
    public FightState getState() { return state; }
    public Map<UUID, List<UUID>> getKillLog() { return killLog; }
    public Set<UUID> getSpectators() { return spectators; }

    public void setState(FightState state) { this.state = state; }
    public void incrementRound() { currentRound++; }

    public void setActiveTask(BukkitTask task) {
        if (activeTask != null) activeTask.cancel();
        activeTask = task;
    }

    public void cancelActiveTask() {
        if (activeTask != null) { activeTask.cancel(); activeTask = null; }
    }

    /** Returns which team this player belongs to, or null. */
    public FightTeam getTeamOf(UUID uuid) {
        return teams.stream().filter(t -> t.hasMember(uuid)).findFirst().orElse(null);
    }

    /** Returns all participant UUIDs across all teams. */
    public List<UUID> getAllParticipants() {
        List<UUID> all = new ArrayList<>();
        teams.forEach(t -> all.addAll(t.getMembers()));
        return all;
    }

    public void logKill(UUID killer, UUID killed) {
        killLog.computeIfAbsent(killer, k -> new ArrayList<>()).add(killed);
    }

    /** Find winning team — the one with most round wins when fight ends. */
    public FightTeam getWinner() {
        return teams.stream()
                .max(Comparator.comparingInt(FightTeam::getRoundWins))
                .orElse(null);
    }

    /** For FFA — returns the last non-eliminated team. */
    public FightTeam getLastSurvivingTeam() {
        List<FightTeam> alive = teams.stream()
                .filter(t -> !t.isTeamEliminated())
                .toList();
        return alive.size() == 1 ? alive.get(0) : null;
    }

    public void resetRound() {
        teams.forEach(FightTeam::resetEliminated);
    }
}
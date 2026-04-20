package net.tutla.pvpPlus.fight;

import net.tutla.pvpPlus.arena.Arena;
import net.tutla.pvpPlus.arena.ArenaManager;
import net.tutla.pvpPlus.kit.Kit;
import net.tutla.pvpPlus.kit.KitManager;
import net.tutla.pvpPlus.util.TextUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.*;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

public class FightManager {

    private final JavaPlugin plugin;
    private final ArenaManager arenaManager;
    private final KitManager kitManager;

    // All active fights
    private final Map<UUID, Fight> fights = new HashMap<>(); // fight id -> fight
    // Quick lookup: player UUID -> fight
    private final Map<UUID, Fight> playerFight = new HashMap<>();

    public FightManager(JavaPlugin plugin, ArenaManager arenaManager, KitManager kitManager) {
        this.plugin = plugin;
        this.arenaManager = arenaManager;
        this.kitManager = kitManager;
    }

    // -------------------------------------------------------------------------
    // Start a fight
    // -------------------------------------------------------------------------

    /**
     * Main entry point. Called by DuelManager, PartyCommand, QueueManager.
     * teams: list of FightTeam — for a 1v1 duel this is 2 teams of 1.
     * For FFA each player is their own team.
     */
    public boolean startFight(FightType type, List<FightTeam> teams, Arena arena, Kit kit, int rounds) {
        // Validate no one is already in a fight
        for (FightTeam team : teams) {
            for (UUID uuid : team.getMembers()) {
                if (playerFight.containsKey(uuid)) return false;
            }
        }

        arena.setInUse(true);
        Fight fight = new Fight(type, teams, arena, kit, rounds);
        fights.put(fight.getId(), fight);
        teams.forEach(team ->
                team.getMembers().forEach(uuid -> playerFight.put(uuid, fight)));

        startRound(fight);
        return true;
    }

    // -------------------------------------------------------------------------
    // Round lifecycle
    // -------------------------------------------------------------------------

    private void startRound(Fight fight) {
        fight.incrementRound();
        fight.resetRound();
        fight.setState(FightState.COUNTDOWN);

        fight.getArena().restoreSnapshot();

        List<FightTeam> teams = fight.getTeams();
        List<Location> allSpawns = new ArrayList<>(fight.getArena().getTeam1Spawns());
        allSpawns.addAll(fight.getArena().getTeam2Spawns());

        if (fight.getType() == FightType.FFA) {
            // Shuffle spawns and assign one per player
            Collections.shuffle(allSpawns);
            int spawnIndex = 0;
            for (FightTeam team : teams) {
                for (UUID uuid : team.getMembers()) {
                    Player p = Bukkit.getPlayer(uuid);
                    if (p == null) continue;
                    Location spawn = allSpawns.get(spawnIndex % allSpawns.size());
                    spawnIndex++;
                    setupPlayer(p, spawn, fight);
                }
            }
        } else {
            // Standard: team 0 -> spawns1, team 1 -> spawns2
            List<Location> spawns1 = fight.getArena().getTeam1Spawns();
            List<Location> spawns2 = fight.getArena().getTeam2Spawns();
            for (int t = 0; t < teams.size(); t++) {
                FightTeam team = teams.get(t);
                List<Location> spawns = (t == 0) ? spawns1 : spawns2;
                List<UUID> members = team.getMembers();
                for (int i = 0; i < members.size(); i++) {
                    Player p = Bukkit.getPlayer(members.get(i));
                    if (p == null) continue;
                    setupPlayer(p, spawns.get(i % spawns.size()), fight);
                }
            }
        }

        updateScoreboard(fight);
        runCountdown(fight);
    }

    private void setupPlayer(Player p, Location spawn, Fight fight) {
        p.teleport(spawn);
        p.setGameMode(GameMode.ADVENTURE);
        p.setHealth(20);
        p.setFoodLevel(20);
        p.setSaturation(20);p.getInventory().clear();
        kitManager.applyKit(p, fight.getKit());
        p.updateInventory();
    }

    private void runCountdown(Fight fight) {
        fight.setActiveTask(new BukkitRunnable() {
            int count = 3;
            @Override
            public void run() {
                if (count > 0) {
                    broadcastTitle(fight,
                            "<red>" + count,
                            "<gray>Round " + fight.getCurrentRound(),
                            0, 25, 5);
                    broadcastSound(fight, Sound.UI_BUTTON_CLICK);
                    count--;
                } else {
                    // Unfreeze
                    broadcastTitle(fight, "<green>FIGHT!", "", 0, 20, 10);
                    broadcastSound(fight, Sound.ENTITY_ENDER_DRAGON_GROWL);
                    for (UUID uuid : fight.getAllParticipants()) {
                        Player p = Bukkit.getPlayer(uuid);
                        if (p != null) unfreezePlayer(p);
                    }
                    fight.setState(FightState.ACTIVE);
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 20L));
    }

    // Called from the event listener when a player takes lethal damage
    public void handleDeath(Player killed, Player killer) {
        Fight fight = playerFight.get(killed.getUniqueId());
        if (fight == null || fight.getState() != FightState.ACTIVE) return;

        // Cancel death
        killed.setHealth(20);
        freezePlayer(killed);

        FightTeam deadTeam = fight.getTeamOf(killed.getUniqueId());
        if (deadTeam == null) return;
        deadTeam.eliminatePlayer(killed.getUniqueId());

        if (killer != null) {
            fight.logKill(killer.getUniqueId(), killed.getUniqueId());
            killer.sendMessage(TextUtil.parse("<red>You killed <white>" + killed.getName() + "</white>!"));
        }

        killed.sendMessage(TextUtil.parse("<red>You were eliminated this round."));
        killed.setGameMode(GameMode.SPECTATOR);
        updateScoreboard(fight);

        // Check if round is over
        checkRoundEnd(fight);
    }

    private void checkRoundEnd(Fight fight) {
        // FFA: last team standing
        if (fight.getType() == FightType.FFA) {
            FightTeam survivor = fight.getLastSurvivingTeam();
            if (survivor != null) roundWon(fight, survivor);
            return;
        }

        // Standard: check if any team is fully eliminated
        for (FightTeam team : fight.getTeams()) {
            if (team.isTeamEliminated()) {
                // The OTHER team won this round
                FightTeam winner = fight.getTeams().stream()
                        .filter(t -> !t.isTeamEliminated())
                        .findFirst().orElse(null);
                if (winner != null) roundWon(fight, winner);
                return;
            }
        }
    }

    private void roundWon(Fight fight, FightTeam winner) {
        fight.setState(FightState.BETWEEN_ROUNDS);
        fight.cancelActiveTask();

        winner.addRoundWin();
        updateScoreboard(fight);

        broadcastTitle(fight,
                "<gold>" + winner.getName() + " wins the round!",
                "<gray>Score: " + getScoreLine(fight),
                0, 40, 10);

        if (winner.getRoundWins() >= fight.getTotalRounds()) {
            fight.setActiveTask(new BukkitRunnable() {
                @Override public void run() { endFight(fight, winner); }
            }.runTaskLater(plugin, 60L));
        } else {
            fight.setActiveTask(new BukkitRunnable() {
                @Override public void run() { startRound(fight); }
            }.runTaskLater(plugin, 60L));
        }
    }

    // -------------------------------------------------------------------------
    // End fight
    // -------------------------------------------------------------------------

    public void leaveFight(Player player) {
        Fight fight = playerFight.get(player.getUniqueId()); // don't remove yet
        if (fight == null) return;

        // Immediately restore this player to survival
        player.setGameMode(GameMode.SURVIVAL);
        player.getInventory().clear();
        player.teleport(fight.getArena().getPos1().clone().add(0, 2, 0));
        removeScoreboard(player);
        playerFight.remove(player.getUniqueId());

        // Mark them eliminated and removed from their team
        FightTeam team = fight.getTeamOf(player.getUniqueId());
        if (team != null) {
            team.eliminatePlayer(player.getUniqueId());
            team.removeMember(player.getUniqueId());
        }

        // If fight is already ending/ended, don't interfere
        if (fight.getState() == FightState.ENDED) return;

        // Find the winner — works for both FFA and standard fights
        FightTeam winner = findWinnerAfterLeave(fight);
        if (winner != null) {
            endFight(fight, winner);
        } else {
            // Fight continues, just update scoreboard
            updateScoreboard(fight);
        }
    }

    private FightTeam findWinnerAfterLeave(Fight fight) {
        // Get teams that still have members
        List<FightTeam> teamsWithMembers = fight.getTeams().stream()
                .filter(t -> !t.getMembers().isEmpty())
                .toList();

        // If only one team has members left, they win
        if (teamsWithMembers.size() == 1) return teamsWithMembers.get(0);

        // If no teams have members (shouldn't happen but guard it)
        if (teamsWithMembers.isEmpty()) return null;

        // Multiple teams still active — fight continues
        return null;
    }

    public void endFight(Fight fight, FightTeam winner) {
        fight.setState(FightState.ENDED);
        fight.cancelActiveTask();

        broadcastTitle(fight,
                "<gold>⚔ " + winner.getName() + " wins!",
                "<gray>" + getScoreLine(fight),
                0, 60, 20);

        // Print fight summary to all participants
        sendFightSummary(fight, winner);

        // Restore arena, teleport everyone out, clean up
        fight.setActiveTask(new BukkitRunnable() {
            @Override public void run() {
                cleanupFight(fight);
            }
        }.runTaskLater(plugin, 80L));
    }

    private void cleanupFight(Fight fight) {
        // Teleport all participants to arena pos1 (outside) or a lobby
        // For now teleport to just above pos2 — you can change this to a lobby location
        Location exitLoc = fight.getArena().getPos1().clone().add(0, 2, 0);

        for (UUID uuid : fight.getAllParticipants()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                p.setGameMode(GameMode.SURVIVAL);
                p.teleport(exitLoc);
                p.getInventory().clear();
                removeScoreboard(p);
            }
            playerFight.remove(uuid);
        }

        for (UUID uuid : fight.getSpectators()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                p.setGameMode(GameMode.SURVIVAL);
                p.teleport(exitLoc);
                removeScoreboard(p);
            }
        }

        fight.getArena().restoreSnapshot();
        fight.getArena().setInUse(false);
        fights.remove(fight.getId());
    }

    // -------------------------------------------------------------------------
    // Spectator
    // -------------------------------------------------------------------------

    public boolean addSpectator(Player spectator, Player target) {
        Fight fight = playerFight.get(target.getUniqueId());
        if (fight == null) return false;
        if (playerFight.containsKey(spectator.getUniqueId())) return false;

        fight.getSpectators().add(spectator.getUniqueId());
        spectator.setGameMode(GameMode.SPECTATOR);
        spectator.teleport(target.getLocation());
        showScoreboard(fight, spectator);
        spectator.sendMessage(TextUtil.parse(
                "<gray>Spectating fight. Use <white>/leavefight</white> to exit."));
        return true;
    }

    public boolean removeSpectator(Player spectator) {
        for (Fight fight : fights.values()) {
            if (fight.getSpectators().remove(spectator.getUniqueId())) {
                spectator.setGameMode(GameMode.SURVIVAL);
                spectator.teleport(fight.getArena().getPos1().clone().add(0, 2, 0));
                removeScoreboard(spectator);
                return true;
            }
        }
        return false;
    }

    public boolean isSpectating(Player player) {
        return fights.values().stream()
                .anyMatch(f -> f.getSpectators().contains(player.getUniqueId()));
    }

    // -------------------------------------------------------------------------
    // Scoreboard
    // -------------------------------------------------------------------------

    private void updateScoreboard(Fight fight) {
        for (UUID uuid : fight.getAllParticipants()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) showScoreboard(fight, p);
        }
        for (UUID uuid : fight.getSpectators()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) showScoreboard(fight, p);
        }
    }

    private void showScoreboard(Fight fight, Player player) {
        ScoreboardManager sm = Bukkit.getScoreboardManager();
        Scoreboard sb = sm.getNewScoreboard();

        Objective obj = sb.registerNewObjective(
                "fight", Criteria.DUMMY,
                TextUtil.parse("<gold><bold>⚔ Fight</bold>"));
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        int line = fight.getTeams().size() + 2;
        setScore(obj, " ", line--);
        setScore(obj, "§eRound §f" + fight.getCurrentRound() + "§7 §(FT" + fight.getTotalRounds()+")", line--);
        setScore(obj, " §r", line--);

        for (FightTeam team : fight.getTeams()) {
            String wins = "§a" + team.getRoundWins();
            setScore(obj, "§f" + team.getName() + " §7- " + wins, line--);
        }

        player.setScoreboard(sb);
    }

    private void setScore(Objective obj, String text, int score) {
        Score s = obj.getScore(text);
        s.setScore(score);
    }

    private void removeScoreboard(Player player) {
        player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
    }

    // -------------------------------------------------------------------------
    // Fight summary
    // -------------------------------------------------------------------------

    private void sendFightSummary(Fight fight, FightTeam winner) {
        broadcastToFight(fight, "<gold>━━━━━━ Fight Summary ━━━━━━");
        broadcastToFight(fight, "<green>Winner: <white>" + winner.getName());
        broadcastToFight(fight, "<gray>Score: " + getScoreLine(fight));
        broadcastToFight(fight, " ");

        // Kill log
        broadcastToFight(fight, "<yellow>Kills:");
        fight.getKillLog().forEach((killerUuid, killed) -> {
            String killerName = getPlayerName(killerUuid);
            String kills = killed.stream()
                    .map(this::getPlayerName)
                    .collect(Collectors.joining(", "));
            broadcastToFight(fight, "  <white>" + killerName + " <gray>→ <red>" + kills);
        });
        broadcastToFight(fight, "<gold>━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    private String getScoreLine(Fight fight) {
        return fight.getTeams().stream()
                .map(t -> t.getName() + ": " + t.getRoundWins())
                .collect(Collectors.joining(" | "));
    }

    private String getPlayerName(UUID uuid) {
        Player p = Bukkit.getPlayer(uuid);
        return p != null ? p.getName() : uuid.toString().substring(0, 8);
    }

    // -------------------------------------------------------------------------
    // Freeze / Unfreeze
    // -------------------------------------------------------------------------

    private void freezePlayer(Player player) {
        // nun here rn
    }

    private void unfreezePlayer(Player player) {
        player.setGameMode(GameMode.SURVIVAL);
    }

    // -------------------------------------------------------------------------
    // Broadcast helpers
    // -------------------------------------------------------------------------

    private void broadcastTitle(Fight fight, String title, String subtitle, int in, int stay, int out) {
        Title t = Title.title(
                TextUtil.parse(title),
                TextUtil.parse(subtitle),
                Title.Times.times(
                        Duration.ofMillis(in * 50L),
                        Duration.ofMillis(stay * 50L),
                        Duration.ofMillis(out * 50L)
                )
        );
        for (UUID uuid : fight.getAllParticipants()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) p.showTitle(t);
        }
        for (UUID uuid : fight.getSpectators()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) p.showTitle(t);
        }
    }

    private void broadcastSound(Fight fight, Sound sound) {
        for (UUID uuid : fight.getAllParticipants()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) p.playSound(p.getLocation(), sound, 1f, 1f);
        }
    }

    private void broadcastToFight(Fight fight, String miniMessage) {
        Component msg = TextUtil.parse(miniMessage);
        for (UUID uuid : fight.getAllParticipants()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) p.sendMessage(msg);
        }
        for (UUID uuid : fight.getSpectators()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) p.sendMessage(msg);
        }
    }

    // -------------------------------------------------------------------------
    // Lookups
    // -------------------------------------------------------------------------

    public Fight getFight(Player player) { return playerFight.get(player.getUniqueId()); }
    public boolean isInFight(Player player) { return playerFight.containsKey(player.getUniqueId()); }
    public Collection<Fight> getAllFights() { return fights.values(); }
}
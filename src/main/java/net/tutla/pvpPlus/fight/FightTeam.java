package net.tutla.pvpPlus.fight;

import org.bukkit.entity.Player;
import java.util.*;

public class FightTeam {
    private final String name; // "Team 1", "Team 2", or player name for FFA
    private final List<UUID> members;
    private final Set<UUID> eliminated = new HashSet<>(); // eliminated this round
    private int roundWins = 0;

    public FightTeam(String name, List<UUID> members) {
        this.name = name;
        this.members = new ArrayList<>(members);
    }

    public String getName() { return name; }
    public List<UUID> getMembers() { return Collections.unmodifiableList(members); }
    public int getRoundWins() { return roundWins; }
    public void addRoundWin() { roundWins++; }

    public void eliminatePlayer(UUID uuid) { eliminated.add(uuid); }
    public boolean isEliminated(UUID uuid) { return eliminated.contains(uuid); }
    public void resetEliminated() { eliminated.clear(); }

    public void removeMember(UUID uuid) {
        members.remove(uuid);
        eliminated.remove(uuid);
    }

    public boolean isTeamEliminated() {
        return eliminated.containsAll(members) || members.isEmpty();
    }

    public boolean hasMember(UUID uuid) { return members.contains(uuid); }
}
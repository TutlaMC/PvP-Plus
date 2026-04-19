package net.tutla.pvpPlus.arena;

import org.bukkit.Location;

import java.util.*;

public class Arena {
    private final String name;

    private Location pos1, pos2;
    private final List<Location> team1Spawns = new ArrayList<>();
    private final List<Location> team2Spawns = new ArrayList<>();

    private boolean inUse = false;
    private ArenaSnapshot snapshot = null;

    public Arena(String name) { this.name = name; }

    public String getName() { return name; }

    public Location getPos1() { return pos1; }
    public Location getPos2() { return pos2; }
    public void setPos1(Location l) { this.pos1 = l; }
    public void setPos2(Location l) { this.pos2 = l; }

    public List<Location> getTeam1Spawns() { return team1Spawns; }
    public List<Location> getTeam2Spawns() { return team2Spawns; }
    public void addTeam1Spawn(Location l) { team1Spawns.add(l); }
    public void addTeam2Spawn(Location l) { team2Spawns.add(l); }

    public boolean isInUse() { return inUse; }
    public void setInUse(boolean inUse) { this.inUse = inUse; }

    public boolean isFullyConfigured() {
        return pos1 != null && pos2 != null
                && !team1Spawns.isEmpty()
                && !team2Spawns.isEmpty();
    }

    /**
     * Called once when /arena save is run.
     * Takes the initial snapshot that will be used as the "clean" reference.
     */
    public void captureSnapshot() {
        if (pos1 == null || pos2 == null) return;
        snapshot = new ArenaSnapshot(pos1.getWorld());
        snapshot.capture(pos1, pos2);
    }

    /**
     * Called by FightManager after a fight ends.
     */
    public void restoreSnapshot() {
        if (snapshot != null) {
            snapshot.restore();
        }
    }

    public boolean hasSnapshot() {
        return snapshot != null && !snapshot.isEmpty();
    }

    public int getSnapshotSize() {
        return snapshot == null ? 0 : snapshot.size();
    }
}

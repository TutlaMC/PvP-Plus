package net.tutla.pvpPlus.manager;

import net.tutla.pvpPlus.model.Arena;
import org.bukkit.entity.Player;
import java.util.*;

public class ArenaManager {

    // Arenas that have been saved/finalized
    private final Map<String, Arena> arenas = new HashMap<>();

    // Tracks which admin is currently setting up which arena
    private final Map<UUID, Arena> setupSessions = new HashMap<>();

    // --- Setup session management ---

    public boolean startSetup(Player admin, String arenaName) {
        if (arenas.containsKey(arenaName.toLowerCase())) return false; // already exists
        setupSessions.put(admin.getUniqueId(), new Arena(arenaName.toLowerCase()));
        return true;
    }

    public boolean isInSetup(Player admin) {
        return setupSessions.containsKey(admin.getUniqueId());
    }

    public Arena getSetupArena(Player admin) {
        return setupSessions.get(admin.getUniqueId());
    }

    public void cancelSetup(Player admin) {
        setupSessions.remove(admin.getUniqueId());
    }

    public boolean saveSetup(Player admin) {
        Arena arena = setupSessions.get(admin.getUniqueId());
        if (arena == null || !arena.isFullyConfigured()) return false;
        arenas.put(arena.getName(), arena);
        setupSessions.remove(admin.getUniqueId());
        return true;
    }

    // --- Arena access ---

    public Arena getArena(String name) {
        return arenas.get(name.toLowerCase());
    }

    public Collection<Arena> getAllArenas() {
        return arenas.values();
    }

    public Optional<Arena> getAvailableArena() {
        return arenas.values().stream().filter(a -> !a.isInUse()).findFirst();
    }

    public boolean deleteArena(String name) {
        return arenas.remove(name.toLowerCase()) != null;
    }
}

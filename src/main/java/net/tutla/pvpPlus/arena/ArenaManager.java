package net.tutla.pvpPlus.arena;

import org.bukkit.entity.Player;
import java.util.*;

public class ArenaManager {
    private final Map<String, Arena> arenas = new HashMap<>();
    private final Map<UUID, Arena> setupSessions = new HashMap<>();
    private final ArenaSerializer serializer;

    public ArenaManager(ArenaSerializer serializer) {
        this.serializer = serializer;
    }

    public void loadAll() {
        serializer.loadAll().forEach(a -> arenas.put(a.getName(), a));
    }

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
        arena.captureSnapshot();
        arenas.put(arena.getName(), arena);
        setupSessions.remove(admin.getUniqueId());
        serializer.save(arena); // ← persist to disk
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
        boolean removed = arenas.remove(name.toLowerCase()) != null;
        if (removed) serializer.delete(name); // ← remove file
        return removed;
    }
}

package net.tutla.pvpPlus.arena;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class ArenaSerializer {

    private final File arenasFolder;
    private final Logger log;

    public ArenaSerializer(File pluginDataFolder, Logger log) {
        this.arenasFolder = new File(pluginDataFolder, "arenas");
        this.log = log;
        if (!arenasFolder.exists()) arenasFolder.mkdirs();
    }

    // -------------------------------------------------------------------------
    // Save
    // -------------------------------------------------------------------------

    public void save(Arena arena) {
        File file = fileFor(arena.getName());
        YamlConfiguration cfg = new YamlConfiguration();

        writeLocation(cfg, "bounds.pos1", arena.getPos1());
        writeLocation(cfg, "bounds.pos2", arena.getPos2());

        writeLocationList(cfg, "spawns.team1", arena.getTeam1Spawns());
        writeLocationList(cfg, "spawns.team2", arena.getTeam2Spawns());

        if (arena.hasSnapshot()) {
            writeSnapshot(cfg, arena.getSnapshot());
        }

        try {
            cfg.save(file);
        } catch (IOException e) {
            log.severe("Failed to save arena " + arena.getName() + ": " + e.getMessage());
        }
    }

    public void delete(String arenaName) {
        File file = fileFor(arenaName);
        if (file.exists()) file.delete();
    }

    // -------------------------------------------------------------------------
    // Load all
    // -------------------------------------------------------------------------

    public List<Arena> loadAll() {
        List<Arena> result = new ArrayList<>();
        File[] files = arenasFolder.listFiles(f -> f.getName().endsWith(".yml"));
        if (files == null) return result;

        for (File file : files) {
            try {
                Arena arena = loadFrom(file);
                if (arena != null) result.add(arena);
            } catch (Exception e) {
                log.severe("Failed to load arena from " + file.getName() + ": " + e.getMessage());
            }
        }
        return result;
    }

    // -------------------------------------------------------------------------
    // Internal - load one file
    // -------------------------------------------------------------------------

    private Arena loadFrom(File file) {
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        String name = file.getName().replace(".yml", "");
        Arena arena = new Arena(name);

        Location pos1 = readLocation(cfg, "bounds.pos1");
        Location pos2 = readLocation(cfg, "bounds.pos2");
        if (pos1 == null || pos2 == null) {
            log.warning("Arena " + name + " is missing bounds, skipping.");
            return null;
        }
        arena.setPos1(pos1);
        arena.setPos2(pos2);

        readLocationList(cfg, "spawns.team1").forEach(arena::addTeam1Spawn);
        readLocationList(cfg, "spawns.team2").forEach(arena::addTeam2Spawn);

        if (cfg.contains("snapshot")) {
            ArenaSnapshot snapshot = readSnapshot(cfg, pos1.getWorld());
            arena.setSnapshot(snapshot);
        }

        return arena;
    }

    // -------------------------------------------------------------------------
    // Snapshot serialization
    // -------------------------------------------------------------------------

    private void writeSnapshot(YamlConfiguration cfg, ArenaSnapshot snapshot) {
        ConfigurationSection sec = cfg.createSection("snapshot");
        int i = 0;
        for (var entry : snapshot.getEntries().entrySet()) {
            Location loc = entry.getKey();
            ArenaSnapshot.BlockEntry block = entry.getValue();

            ConfigurationSection blockSec = sec.createSection(String.valueOf(i++));
            blockSec.set("x", loc.getBlockX());
            blockSec.set("y", loc.getBlockY());
            blockSec.set("z", loc.getBlockZ());
            blockSec.set("data", block.blockData().getAsString());
        }
        sec.set("count", i);
    }

    private ArenaSnapshot readSnapshot(YamlConfiguration cfg, World world) {
        ConfigurationSection sec = cfg.getConfigurationSection("snapshot");
        if (sec == null) return null;

        ArenaSnapshot snapshot = new ArenaSnapshot(world);

        for (String key : sec.getKeys(false)) {
            if (key.equals("count")) continue;
            ConfigurationSection blockSec = sec.getConfigurationSection(key);
            if (blockSec == null) continue;

            int x = blockSec.getInt("x");
            int y = blockSec.getInt("y");
            int z = blockSec.getInt("z");

            String dataStr = blockSec.getString("data");
            if (dataStr == null) continue;

            BlockData blockData = Bukkit.createBlockData(dataStr);
            snapshot.addEntry(new Location(world, x, y, z), blockData, null);
        }

        return snapshot;
    }

    // -------------------------------------------------------------------------
    // Location helpers
    // -------------------------------------------------------------------------

    private void writeLocation(YamlConfiguration cfg, String path, Location loc) {
        cfg.set(path + ".world", loc.getWorld().getName());
        cfg.set(path + ".x", loc.getX());
        cfg.set(path + ".y", loc.getY());
        cfg.set(path + ".z", loc.getZ());
        cfg.set(path + ".yaw", loc.getYaw());
        cfg.set(path + ".pitch", loc.getPitch());
    }

    private Location readLocation(YamlConfiguration cfg, String path) {
        String worldName = cfg.getString(path + ".world");
        if (worldName == null) return null;
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            log.warning("World '" + worldName + "' not found for path: " + path);
            return null;
        }
        double x = cfg.getDouble(path + ".x");
        double y = cfg.getDouble(path + ".y");
        double z = cfg.getDouble(path + ".z");
        float yaw   = (float) cfg.getDouble(path + ".yaw");
        float pitch = (float) cfg.getDouble(path + ".pitch");
        return new Location(world, x, y, z, yaw, pitch);
    }

    private void writeLocationList(YamlConfiguration cfg, String path, List<Location> locs) {
        for (int i = 0; i < locs.size(); i++) {
            writeLocation(cfg, path + "." + i, locs.get(i));
        }
        cfg.set(path + "._count", locs.size());
    }

    private List<Location> readLocationList(YamlConfiguration cfg, String path) {
        List<Location> result = new ArrayList<>();
        int count = cfg.getInt(path + "._count", 0);
        for (int i = 0; i < count; i++) {
            Location loc = readLocation(cfg, path + "." + i);
            if (loc != null) result.add(loc);
        }

        return result;
    }

    private File fileFor(String arenaName) {
        return new File(arenasFolder, arenaName.toLowerCase() + ".yml");
    }
}
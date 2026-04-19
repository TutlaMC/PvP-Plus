package net.tutla.pvpPlus.arena;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class ArenaSerializer {

    private static final int FORMAT_VERSION = 1;

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
        try (DataOutputStream out = new DataOutputStream(
                new GZIPOutputStream(new FileOutputStream(file)))) {

            out.writeInt(FORMAT_VERSION);

            // Bounds
            writeLocation(out, arena.getPos1());
            writeLocation(out, arena.getPos2());

            // Spawns
            writeLocationList(out, arena.getTeam1Spawns());
            writeLocationList(out, arena.getTeam2Spawns());

            // Snapshot
            if (arena.hasSnapshot()) {
                out.writeBoolean(true);
                writeSnapshot(out, arena.getSnapshot());
            } else {
                out.writeBoolean(false);
            }

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
        File[] files = arenasFolder.listFiles(f -> f.getName().endsWith(".dat"));
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
    // Internal load
    // -------------------------------------------------------------------------

    private Arena loadFrom(File file) throws IOException {
        try (DataInputStream in = new DataInputStream(
                new GZIPInputStream(new FileInputStream(file)))) {

            int version = in.readInt();
            if (version != FORMAT_VERSION) {
                log.warning("Unknown arena format version " + version + " in " + file.getName());
                return null;
            }

            String name = file.getName().replace(".dat", "");
            Arena arena = new Arena(name);

            arena.setPos1(readLocation(in));
            arena.setPos2(readLocation(in));

            readLocationList(in).forEach(arena::addTeam1Spawn);
            readLocationList(in).forEach(arena::addTeam2Spawn);

            boolean hasSnapshot = in.readBoolean();
            if (hasSnapshot) {
                arena.setSnapshot(readSnapshot(in, arena.getPos1().getWorld()));
            }

            return arena;
        }
    }

    // -------------------------------------------------------------------------
    // Snapshot
    // -------------------------------------------------------------------------

    private void writeSnapshot(DataOutputStream out, ArenaSnapshot snapshot) throws IOException {
        var entries = snapshot.getEntries().entrySet();
        out.writeInt(entries.size());
        for (var entry : entries) {
            Location loc = entry.getKey();
            out.writeInt(loc.getBlockX());
            out.writeInt(loc.getBlockY());
            out.writeInt(loc.getBlockZ());
            out.writeUTF(entry.getValue().blockData().getAsString());
        }
    }

    private ArenaSnapshot readSnapshot(DataInputStream in, World world) throws IOException {
        ArenaSnapshot snapshot = new ArenaSnapshot(world);
        int count = in.readInt();
        for (int i = 0; i < count; i++) {
            int x = in.readInt();
            int y = in.readInt();
            int z = in.readInt();
            BlockData data = Bukkit.createBlockData(in.readUTF());
            snapshot.addEntry(new Location(world, x, y, z), data, null);
        }
        return snapshot;
    }

    // -------------------------------------------------------------------------
    // Location helpers
    // -------------------------------------------------------------------------

    private void writeLocation(DataOutputStream out, Location loc) throws IOException {
        out.writeUTF(loc.getWorld().getName());
        out.writeDouble(loc.getX());
        out.writeDouble(loc.getY());
        out.writeDouble(loc.getZ());
        out.writeFloat(loc.getYaw());
        out.writeFloat(loc.getPitch());
    }

    private Location readLocation(DataInputStream in) throws IOException {
        String worldName = in.readUTF();
        World world = Bukkit.getWorld(worldName);
        if (world == null) throw new IOException("World not found: " + worldName);
        double x = in.readDouble();
        double y = in.readDouble();
        double z = in.readDouble();
        float yaw = in.readFloat();
        float pitch = in.readFloat();
        return new Location(world, x, y, z, yaw, pitch);
    }

    private void writeLocationList(DataOutputStream out, List<Location> locs) throws IOException {
        out.writeInt(locs.size());
        for (Location loc : locs) writeLocation(out, loc);
    }

    private List<Location> readLocationList(DataInputStream in) throws IOException {
        int count = in.readInt();
        List<Location> result = new ArrayList<>(count);
        for (int i = 0; i < count; i++) result.add(readLocation(in));
        return result;
    }

    private File fileFor(String arenaName) {
        return new File(arenasFolder, arenaName.toLowerCase() + ".dat");
    }
}
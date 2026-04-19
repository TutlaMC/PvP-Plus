package net.tutla.pvpPlus.arena;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ArenaSnapshot {
    // Stores BOTH BlockData (type + visual state) and BlockState (tile entity data
    // like chest contents, sign text, spawner settings etc.)
    public record BlockEntry(BlockData blockData) {}

    private final Map<Location, BlockEntry> snapshot = new HashMap<>();
    private final World world;

    public ArenaSnapshot(World world) {
        this.world = world;
    }

    /**
     * Iterates every block in the cuboid defined by pos1/pos2 and records its state.
     * Call this once when the arena is saved, then again before each fight starts
     * (in case a previous restore had any issues).
     */
    public void capture(Location pos1, Location pos2) {
        snapshot.clear();

        int minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
        int minY = Math.min(pos1.getBlockY(), pos2.getBlockY());
        int minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
        int maxX = Math.max(pos1.getBlockX(), pos2.getBlockX());
        int maxY = Math.max(pos1.getBlockY(), pos2.getBlockY());
        int maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Block block = world.getBlockAt(x, y, z);
                    BlockData data = block.getBlockData().clone();
                    // Pass null for blockState — BlockData covers all structural state
                    snapshot.put(new Location(world, x, y, z), new BlockEntry(data));
                }
            }
        }
    }

    /**
     * Restores every block to its captured state.
     * Call this after a fight ends.
     */
    public void restore() {
        if (snapshot.isEmpty()) return;
        for (Map.Entry<Location, BlockEntry> entry : snapshot.entrySet()) {
            Block block = world.getBlockAt(entry.getKey());
            block.setBlockData(entry.getValue().blockData().clone(), false);
        }
    }

    public boolean isEmpty() {
        return snapshot.isEmpty();
    }

    public int size() {
        return snapshot.size();
    }

    public Map<Location, BlockEntry> getEntries() {
        return Collections.unmodifiableMap(snapshot);
    }

    public void addEntry(Location loc, BlockData blockData, BlockState blockState) {
        snapshot.put(loc, new BlockEntry(blockData));
    }
}
package net.tutla.pvpPlus.model;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;

import java.util.HashMap;
import java.util.Map;

public class ArenaSnapshot {

    // Stores BOTH BlockData (type + visual state) and BlockState (tile entity data
    // like chest contents, sign text, spawner settings etc.)
    private record BlockEntry(BlockData blockData, BlockState blockState) {}

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

                    // getState() returns a snapshot of the tile entity (chest, sign, etc.)
                    // clone() is important — without it you hold a live reference
                    BlockState state = block.getState();
                    BlockData data = block.getBlockData().clone();

                    snapshot.put(new Location(world, x, y, z), new BlockEntry(data, state));
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
            Location loc = entry.getKey();
            BlockEntry saved = entry.getValue();
            Block block = world.getBlockAt(loc);

            // Step 1: restore the type and visual block state (orientation, waterlogged, etc.)
            block.setBlockData(saved.blockData().clone(), false);
            // false = don't apply physics updates during restore, avoids chain reactions

            // Step 2: restore tile entity data if present (chests, signs, banners, etc.)
            // We update the location on the saved state snapshot to match, then update
            BlockState liveState = block.getState();
            if (isTileEntity(liveState)) {
                // Copy the saved state back into the world by calling update()
                // We need to transfer data from the saved snapshot into the live block state
                restoreTileEntity(block, saved.blockState());
            }
        }
    }

    /**
     * Checks whether a BlockState represents a tile entity (has extra data beyond BlockData).
     */
    private boolean isTileEntity(BlockState state) {
        return state instanceof org.bukkit.block.Container       // chests, barrels, hoppers...
                || state instanceof org.bukkit.block.Sign
                || state instanceof org.bukkit.block.Banner
                || state instanceof org.bukkit.block.Skull
                || state instanceof org.bukkit.block.Lectern
                || state instanceof org.bukkit.block.CommandBlock;
    }

    /**
     * For tile entities, we can't just call setBlockData — we need to replay
     * the saved BlockState snapshot into the live block.
     *
     * The cleanest way in Paper 1.21 is to use the snapshot's own update() after
     * pointing it at the current block.
     */
    private void restoreTileEntity(Block block, BlockState savedState) {
        // Paper's BlockState is a snapshot. To reapply it, we just call update()
        // on the saved snapshot — this writes its data back into the world at its location.
        // Since we stored it from the same location, this works directly.
        savedState.update(true, false);
        // true  = force update even if block type changed
        // false = don't apply physics
    }

    public boolean isEmpty() {
        return snapshot.isEmpty();
    }

    public int size() {
        return snapshot.size();
    }
}
package net.tutla.pvpPlus.listener;

import net.tutla.pvpPlus.arena.Arena;
import net.tutla.pvpPlus.fight.Fight;
import net.tutla.pvpPlus.fight.FightManager;
import net.tutla.pvpPlus.fight.FightState;
import net.tutla.pvpPlus.util.TextUtil;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

public class FightListener implements Listener {

    private final FightManager fightManager;

    public FightListener(FightManager fightManager) {
        this.fightManager = fightManager;
    }

    // Cancel lethal damage and hand to FightManager instead of death screen
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player killed)) return;
        Fight fight = fightManager.getFight(killed);
        if (fight == null || fight.getState() != FightState.ACTIVE) return;

        double newHealth = killed.getHealth() - event.getFinalDamage();
        if (newHealth <= 0) {
            event.setCancelled(true);
            Player killer = null;
            if (event.getDamager() instanceof Player p) killer = p;
            fightManager.handleDeath(killed, killer);
        }
    }

    // Also catch non-player lethal damage (fall, fire, etc.)
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEnvironmentDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player killed)) return;
        if (event instanceof EntityDamageByEntityEvent) return; // handled above
        Fight fight = fightManager.getFight(killed);
        if (fight == null || fight.getState() != FightState.ACTIVE) return;

        double newHealth = killed.getHealth() - event.getFinalDamage();
        if (newHealth <= 0) {
            event.setCancelled(true);
            fightManager.handleDeath(killed, null);
        }
    }

    // Block movement during countdown
    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Fight fight = fightManager.getFight(event.getPlayer());
        if (fight == null || fight.getState() != FightState.COUNTDOWN) return;
        // Cancel any XZ movement, allow looking around
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) return;
        if (from.getBlockX() != to.getBlockX() || from.getBlockZ() != to.getBlockZ()
                || from.getBlockY() != to.getBlockY()) {
            event.setTo(from.clone().setDirection(to.getDirection()));
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Fight fight = fightManager.getFight(event.getPlayer());
        if (fight != null && fight.getState() == FightState.COUNTDOWN) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Fight fight = fightManager.getFight(event.getPlayer());
        if (fight != null && fight.getState() == FightState.COUNTDOWN) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onHunger(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player p)) return;
        Fight fight = fightManager.getFight(p);
        if (fight != null && fight.getState() == FightState.COUNTDOWN) {
            event.setCancelled(true);
        }
    }

    // Spectator arena boundary enforcement
    @EventHandler
    public void onSpectatorMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!fightManager.isSpectating(player)) return;

        Fight fight = null;
        for (var f : fightManager.getAllFights()) {
            if (f.getSpectators().contains(player.getUniqueId())) { fight = f; break; }
        }
        if (fight == null) return;

        Arena arena = fight.getArena();
        Location to = event.getTo();
        if (to == null) return;

        // Keep spectator inside the arena cuboid
        double minX = Math.min(arena.getPos1().getX(), arena.getPos2().getX()) - 1;
        double minY = Math.min(arena.getPos1().getY(), arena.getPos2().getY()) - 1;
        double minZ = Math.min(arena.getPos1().getZ(), arena.getPos2().getZ()) - 1;
        double maxX = Math.max(arena.getPos1().getX(), arena.getPos2().getX()) + 1;
        double maxY = Math.max(arena.getPos1().getY(), arena.getPos2().getY()) + 5;
        double maxZ = Math.max(arena.getPos1().getZ(), arena.getPos2().getZ()) + 1;

        if (to.getX() < minX || to.getX() > maxX ||
                to.getY() < minY || to.getY() > maxY ||
                to.getZ() < minZ || to.getZ() > maxZ) {
            event.setCancelled(true);
        }
    }

    // Handle player disconnecting mid-fight
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        Fight fight = fightManager.getFight(player);
        if (fight != null && fight.getState() == FightState.ACTIVE) {
            fightManager.handleDeath(player, null); // treat as death
        }
        if (fightManager.isSpectating(player)) {
            fightManager.removeSpectator(player);
        }
    }
}
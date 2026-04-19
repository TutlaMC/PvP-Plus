package net.tutla.pvpPlus;

import net.tutla.pvpPlus.arena.ArenaSerializer;
import net.tutla.pvpPlus.commandSystem.CommandContext;
import net.tutla.pvpPlus.commandSystem.CommandSystem;
import net.tutla.pvpPlus.kit.KitSerializer;
import net.tutla.pvpPlus.arena.ArenaManager;
import net.tutla.pvpPlus.kit.KitManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class PvpPlus extends JavaPlugin {
    private static PvpPlus instance;
    private final CommandSystem commandSystem = new CommandSystem();

    private final ArenaSerializer serializer = new ArenaSerializer(getDataFolder(), getLogger());
    private final ArenaManager arenaManager = new ArenaManager(serializer);

    KitSerializer kitSerializer = new KitSerializer(getDataFolder(), getLogger());
    KitManager kitManager = new KitManager(kitSerializer);

    // access functions
    public static PvpPlus getInstance() {
        return instance;
    }

    public ArenaManager getArenaManager(){
        return arenaManager;
    }
    public KitManager getKitManager() {
        return kitManager;
    }


    // actual shi
    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        kitManager.loadAll();
        arenaManager.loadAll();
        commandSystem.initialise();

        getServer().getPluginManager().registerEvents(new EventListeners(), this);
        getLogger().info("PvP Plugin Loaded!");

        new BukkitRunnable() {
            public void run() {
                // in case needed

            }
        }.runTaskTimer(this, 0L, 60*20); // every minute
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String @NotNull [] args) {
        if (!(sender instanceof Player player)) return false;
        CommandContext cmdCtx = new CommandContext(player, cmd, label, args);
        return commandSystem.execute(cmdCtx);
    }
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, Command cmd, @NotNull String label, String @NotNull [] args) {
        if (!(sender instanceof Player player)) return new ArrayList<>();
        return commandSystem.tabComplete(new CommandContext(player, cmd, label, args));
    }
}

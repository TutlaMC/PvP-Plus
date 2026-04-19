package net.tutla.pvpPlus.kit;

import org.bukkit.inventory.ItemStack;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class KitSerializer {

    private static final int FORMAT_VERSION = 1;

    private final File serverKitsFolder;
    private final File playerKitsFolder;
    private final Logger log;

    public KitSerializer(File pluginDataFolder, Logger log) {
        this.serverKitsFolder  = new File(pluginDataFolder, "kits/server");
        this.playerKitsFolder = new File(pluginDataFolder, "kits/player");
        this.log = log;
        serverKitsFolder.mkdirs();
        playerKitsFolder.mkdirs();
    }

    // -------------------------------------------------------------------------
    // Save / Delete
    // -------------------------------------------------------------------------

    public void save(Kit kit) {
        File file = fileFor(kit);
        try (DataOutputStream out = new DataOutputStream(
                new GZIPOutputStream(new FileOutputStream(file)))) {

            out.writeInt(FORMAT_VERSION);
            out.writeBoolean(kit.isPlayerMade());

            writeItemArray(out, kit.getContents());
            writeItemArray(out, kit.getArmor());
            writeItem(out, kit.getOffhand());

            if (kit.hasIcon()) {
                out.writeBoolean(true);
                writeItem(out, kit.getIcon());
            } else {
                out.writeBoolean(false);
            }

        } catch (IOException e) {
            log.severe("Failed to save kit " + kit.getName() + ": " + e.getMessage());
        }
    }

    public void delete(String name, boolean playerMade) {
        File file = fileFor(name, playerMade);
        if (file.exists()) file.delete();
    }

    // -------------------------------------------------------------------------
    // Load
    // -------------------------------------------------------------------------

    public List<Kit> loadAll() {
        List<Kit> result = new ArrayList<>();
        loadFolder(serverKitsFolder,  false, result);
        loadFolder(playerKitsFolder, true,  result);
        return result;
    }

    private void loadFolder(File folder, boolean playerMade, List<Kit> result) {
        File[] files = folder.listFiles(f -> f.getName().endsWith(".kit"));
        if (files == null) return;
        for (File file : files) {
            try {
                Kit kit = loadFrom(file);
                if (kit != null) result.add(kit);
            } catch (Exception e) {
                log.severe("Failed to load kit " + file.getName() + ": " + e.getMessage());
            }
        }
    }

    private Kit loadFrom(File file) throws IOException {
        try (DataInputStream in = new DataInputStream(
                new GZIPInputStream(new FileInputStream(file)))) {

            int version = in.readInt();
            if (version != FORMAT_VERSION) {
                log.warning("Unknown kit format version " + version + " in " + file.getName());
                return null;
            }

            boolean playerMade = in.readBoolean();
            String name = file.getName().replace(".kit", "");
            Kit kit = new Kit(name, playerMade);

            kit.setContents(readItemArray(in));
            kit.setArmor(readItemArray(in));
            kit.setOffhand(readItem(in));
            boolean hasIcon = in.readBoolean();
            if (hasIcon) kit.setIcon(readItem(in));

            return kit;
        }
    }

    // -------------------------------------------------------------------------
    // ItemStack serialization — Paper's serializeAsBytes() carries full NBT
    // including shulker/bundle contents, enchantments, custom data, etc.
    // -------------------------------------------------------------------------

    private void writeItem(DataOutputStream out, ItemStack item) throws IOException {
        if (item == null || item.getType().isAir()) {
            out.writeInt(0); // empty slot marker
        } else {
            byte[] bytes = item.serializeAsBytes();
            out.writeInt(bytes.length);
            out.write(bytes);
        }
    }

    private ItemStack readItem(DataInputStream in) throws IOException {
        int len = in.readInt();
        if (len == 0) return null;
        byte[] bytes = in.readNBytes(len);
        return ItemStack.deserializeBytes(bytes);
    }

    private void writeItemArray(DataOutputStream out, ItemStack[] items) throws IOException {
        if (items == null) {
            out.writeInt(0);
            return;
        }
        out.writeInt(items.length);
        for (ItemStack item : items) writeItem(out, item);
    }

    private ItemStack[] readItemArray(DataInputStream in) throws IOException {
        int len = in.readInt();
        ItemStack[] items = new ItemStack[len];
        for (int i = 0; i < len; i++) items[i] = readItem(in);
        return items;
    }

    // -------------------------------------------------------------------------
    // Paths
    // -------------------------------------------------------------------------

    private File fileFor(Kit kit) {
        return fileFor(kit.getName(), kit.isPlayerMade());
    }

    private File fileFor(String name, boolean playerMade) {
        File folder = playerMade ? playerKitsFolder : serverKitsFolder;
        return new File(folder, name.toLowerCase() + ".kit");
    }

    public boolean exists(String name, boolean playerMade) {
        return fileFor(name, playerMade).exists();
    }
}

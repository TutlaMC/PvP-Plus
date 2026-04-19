package net.tutla.pvpPlus.commandSystem;

public enum CommandSection {
    SETUP("Setup"),
    CONTROLS("Controls"),
    SETTINGS("Settings"),
    OTHER("Other"),
    NONE(null);

    public final String label;
    CommandSection(String label) { this.label = label; }
}

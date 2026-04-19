package net.tutla.pvpPlus.commandSystem;

import net.tutla.pvpPlus.util.TextUtil;
import org.bukkit.entity.Player;

public abstract class TutlaCommand {

    private final String name;
    private final String help;
    private final String description;
    private final CommandSection section;
    public final CommandTabAutoComplete autocomplete;

    public TutlaCommand(String name, String help, String description, CommandSection section, CommandTabAutoComplete autocomplete) { // TODO: make all these params a class
        this.name = name;
        this.help = help;
        this.description = description;
        this.section = section;
        this.autocomplete = autocomplete;
    }

    public String name() {
        return name;
    }

    public String getDescription(){
        return description;
    }

    public abstract boolean run(CommandContext params);

    public void help(Player player){
        player.sendMessage(TextUtil.parse("<red>Incorrect Usage</red>, Correct Usage is: ").append(TextUtil.parse(help)));
    }
    public String getHelpString(){
        return help;
    }

    public CommandSection getSection() {
        return section;
    }
}
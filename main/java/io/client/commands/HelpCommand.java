package io.client.commands;

public class HelpCommand implements Command {
    @Override
    public void execute(String[] args) throws Exception {
        CommandManager.INSTANCE.sendMessage("§9Available commands:");
        CommandManager.INSTANCE.sendMessage("§d|set <module> <setting> <value> §7- Changes a module setting");
        CommandManager.INSTANCE.sendMessage("§d|cordtarget <x> <y> <z> §7- Sets coordinates target | 'clear' to clear target");
        CommandManager.INSTANCE.sendMessage("§d|toggle <module> §7- Toggles a module");
        CommandManager.INSTANCE.sendMessage("§d|bind <module> <key> §7- Binds a key to a module | 'clear' to unbind");
        CommandManager.INSTANCE.sendMessage("§d|macro <new|delete|list> §7- Manage macros");
        CommandManager.INSTANCE.sendMessage("§d|friend <username> §7- Adds a player to your friend list");
        CommandManager.INSTANCE.sendMessage("§d|unfriend <username> §7- Removes a player from your friend list");
        CommandManager.INSTANCE.sendMessage("§d|friendlist §7- Shows your friend list");
        CommandManager.INSTANCE.sendMessage("§d|markchest §7- Marks the chest you are looking at for duping");
        CommandManager.INSTANCE.sendMessage("§d|bindlist §7- Shows your bind list");
        CommandManager.INSTANCE.sendMessage("§d|ioswag <suffix|preset|presets|greentext|clear> ... §7- Customize IoSwag chat style");
        CommandManager.INSTANCE.sendMessage("§d|help §7- Shows this message");
    }
}



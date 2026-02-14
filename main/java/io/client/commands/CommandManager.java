package io.client.commands;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;


public class CommandManager {
    public static final CommandManager INSTANCE = new CommandManager();
    private static final String PREFIX = "|";

    private final Map<String, Command> commands = new HashMap<>();
    private final Minecraft mc = Minecraft.getInstance();
    private int suggestionIndex = 0;

    private CommandManager() {
        registerCommands();
    }

    public String getNextSuggestion(String input) {
        String raw = input.substring(1).toLowerCase();
        String[] parts = raw.split("\\s+");

        if (parts.length == 1) {
            var matches = commands.keySet().stream()
                    .filter(c -> c.startsWith(parts[0]))
                    .sorted()
                    .toList();

            if (matches.isEmpty()) return null;

            String cmd = matches.get(suggestionIndex++ % matches.size());
            return "|" + cmd;
        }

        return null;
    }

    private void registerCommands() {
        commands.put("set", new SetKeyCommand());
        commands.put("cordtarget", new ChordTargetCommand());
        commands.put("toggle", new ToggleCommand());
        commands.put("help", new HelpCommand());
        commands.put("bind", new BindCommand());
        commands.put("friend", new FriendCommand());
        commands.put("unfriend", new UnfriendCommand());
        commands.put("friendlist", new FriendListCommand());
        commands.put("markchest", new MarkChestCommand());
        commands.put("bindlist", new BindListCommand());
        commands.put("macro", new MacroCommand());

    }

    public Set<String> getCommandNames() {
        return commands.keySet();
    }

    public boolean handleMessage(String message) {
        if (!message.startsWith(PREFIX)) return false;

        String commandLine = message.substring(PREFIX.length()).trim();
        if (commandLine.isEmpty()) return true;

        String[] parts = commandLine.split("\\s+");
        String commandName = parts[0].toLowerCase();

        Command command = commands.get(commandName);
        if (command == null) {
            sendMessage("§cUnknown command: " + commandName);
            return true;
        }

        String[] args = new String[parts.length - 1];
        System.arraycopy(parts, 1, args, 0, args.length);

        try {
            command.execute(args);
        } catch (Exception e) {
            sendMessage("§cError: " + e.getMessage());
        }

        return true;
    }

    public void sendMessage(String message) {
        if (mc.player != null) {
            mc.player.displayClientMessage(Component.literal(message), false);
        }
    }
}

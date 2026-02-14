package io.client.modules;

import io.client.Category;
import io.client.Module;
import io.client.commands.CommandManager;
import net.minecraft.client.Minecraft;

public class Macro extends Module {
    private final String command;

    public Macro(String name, String command, int key) {
        super(name, "Macro: " + command, key, Category.MISC);
        this.command = command;
    }

    @Override
    public void onEnable() {
        if (Minecraft.getInstance().player != null) {
            if (command.startsWith("|")) {
                CommandManager.INSTANCE.handleMessage(command);
            } else {
                Minecraft.getInstance().player.connection.sendChat(command);
            }
        }
        setEnabled(false);
    }

    public String getCommand() {
        return command;
    }
}

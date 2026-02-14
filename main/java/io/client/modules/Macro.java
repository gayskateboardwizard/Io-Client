package io.client.modules;

import io.client.Category;
import io.client.Module;
import io.client.commands.CommandManager;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

public class Macro extends Module {
    private final String command;
    private final int[] keyCodes;

    public Macro(String name, String command, int[] keyCodes) {
        super(name, "Macro: " + command, keyCodes[keyCodes.length - 1], Category.MACROS);
        this.command = command;
        this.keyCodes = keyCodes;
    }

    @Override
    public void toggle() {
        this.setEnabled(!this.isEnabled());
        if (this.isEnabled()) {
            onEnable();
        } else {
            onDisable();
        }
    }

    @Override
    public void onEnable() {
        if (Minecraft.getInstance().player == null) {
            setEnabled(false);
            return;
        }

        String[] commands = parseCommands(command);

        for (String cmd : commands) {
            cmd = cmd.trim();
            if (cmd.isEmpty()) continue;

            if (cmd.startsWith("|")) {
                CommandManager.INSTANCE.handleMessage(cmd);
            } else if (cmd.startsWith("/")) {
                Minecraft.getInstance().player.connection.sendCommand(cmd.substring(1));
            } else {
                Minecraft.getInstance().player.connection.sendChat(cmd);
            }
        }

        setEnabled(false);
    }

    private String[] parseCommands(String input) {
        if (!input.contains("}+{")) {
            return new String[]{input};
        }

        return input.split("\\}\\+\\{");
    }

    public String getCommand() {
        return command;
    }

    public int[] getKeyCodes() {
        return keyCodes;
    }

    public boolean matchesKeys(long window) {
        for (int keyCode : keyCodes) {
            if (GLFW.glfwGetKey(window, keyCode) != GLFW.GLFW_PRESS) {
                return false;
            }
        }
        return true;
    }
}
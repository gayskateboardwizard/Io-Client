package io.client.modules;

import io.client.Category;
import io.client.Module;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

public class Macro extends Module {
    private final String command;
    private final int[] keyCodes;

    public Macro(String name, String command, int[] keyCodes) {
        super(name, "Macro: " + command, keyCodes[keyCodes.length - 1], Category.MISC);
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
        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.connection.sendChat(command);
        }
        setEnabled(false);
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

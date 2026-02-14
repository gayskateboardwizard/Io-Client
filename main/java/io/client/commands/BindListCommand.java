package io.client.commands;

import io.client.ClickGuiScreen;
import io.client.Module;
import io.client.ModuleManager;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class BindListCommand implements Command {
    @Override
    public void execute(String[] args) throws Exception {
        List<String> binds = new ArrayList<>();

        binds.add("§7- §e" + getKeyName(ClickGuiScreen.clickGuiKey) + " §7> ClickGui");

        for (Module module : ModuleManager.INSTANCE.getModules()) {
            if (module.getKey() != -1) {
                binds.add("§7- §e" + getKeyName(module.getKey()) + " §7> " + module.getName());
            }
        }

        if (binds.isEmpty()) {
            CommandManager.INSTANCE.sendMessage("§7No keybinds set");
            return;
        }

        CommandManager.INSTANCE.sendMessage("§9Keybinds §7(" + binds.size() + "):");
        for (String bind : binds) {
            CommandManager.INSTANCE.sendMessage(bind);
        }
    }

    private String getKeyName(int keyCode) {
        if (keyCode == -1) return "NONE";

        return switch (keyCode) {
            case GLFW.GLFW_KEY_LEFT_SHIFT -> "LSHIFT";
            case GLFW.GLFW_KEY_RIGHT_SHIFT -> "RSHIFT";
            case GLFW.GLFW_KEY_LEFT_CONTROL -> "LCTRL";
            case GLFW.GLFW_KEY_RIGHT_CONTROL -> "RCTRL";
            case GLFW.GLFW_KEY_LEFT_ALT -> "LALT";
            case GLFW.GLFW_KEY_RIGHT_ALT -> "RALT";
            case GLFW.GLFW_KEY_SPACE -> "SPACE";
            case GLFW.GLFW_KEY_TAB -> "TAB";
            case GLFW.GLFW_KEY_ENTER -> "ENTER";
            case GLFW.GLFW_KEY_BACKSPACE -> "BACKSPACE";
            case GLFW.GLFW_KEY_BACKSLASH -> "BACKSLASH";
            default -> {
                if (keyCode >= GLFW.GLFW_KEY_A && keyCode <= GLFW.GLFW_KEY_Z) {
                    yield String.valueOf((char) ('A' + (keyCode - GLFW.GLFW_KEY_A)));
                }
                yield "KEY_" + keyCode;
            }
        };
    }
}
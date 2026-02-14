package io.client.commands;

import io.client.ClickGuiScreen;
import io.client.Module;
import io.client.ModuleManager;
import io.client.modules.Macro;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class BindListCommand implements Command {
    @Override
    public void execute(String[] args) throws Exception {
        List<String> binds = new ArrayList<>();

        binds.add("§7- §e" + getKeyName(ClickGuiScreen.clickGuiKey) + " §6> ClickGui");

        for (Module module : ModuleManager.INSTANCE.getModules()) {
            if (module instanceof Macro macro) {
                String keyBind = formatKeybind(macro.getKeyCodes());
                binds.add("§7- §e" + keyBind + " §d> " + macro.getName());
            } else if (module.getKey() != -1) {
                binds.add("§7- §e" + getKeyName(module.getKey()) + " §7> " + module.getName());
            }
        }

        if (binds.size() == 1) {
            CommandManager.INSTANCE.sendMessage("§7No keybinds, go make them");
            return;
        }

        CommandManager.INSTANCE.sendMessage("§9Keybinds §7(" + binds.size() + "):");
        for (String bind : binds) {
            CommandManager.INSTANCE.sendMessage(bind);
        }
    }

    private String formatKeybind(int[] keys) {
        if (keys.length == 1) {
            return getKeyName(keys[0]);
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < keys.length; i++) {
            if (i > 0) sb.append("+");
            sb.append(getKeyName(keys[i]));
        }
        return sb.toString();
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
                if (keyCode >= GLFW.GLFW_KEY_0 && keyCode <= GLFW.GLFW_KEY_9) {
                    yield String.valueOf((char) ('0' + (keyCode - GLFW.GLFW_KEY_0)));
                }
                yield "KEY_" + keyCode;
            }
        };
    }
}
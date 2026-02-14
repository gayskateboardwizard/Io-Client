package io.client.commands;

import io.client.MacroManager;
import io.client.modules.Macro;
import org.lwjgl.glfw.GLFW;

public class MacroCommand implements Command {
    @Override
    public void execute(String[] args) throws Exception {
        if (args.length < 1) {
            CommandManager.INSTANCE.sendMessage("§cUsage: |macro <new|delete|list>");
            return;
        }

        String action = args[0].toLowerCase();

        switch (action) {
            case "new", "add", "create" -> {
                if (args.length < 4) {
                    CommandManager.INSTANCE.sendMessage("§cUsage: |macro new <name> <command> <keybind>");
                    return;
                }

                String name = args[1];
                String command = args[2];
                String keyName = args[3].toUpperCase();

                if (MacroManager.INSTANCE.getMacro(name) != null) {
                    CommandManager.INSTANCE.sendMessage("§e" + name + " §7already exists");
                    return;
                }

                int keyCode = getKeyCode(keyName);
                if (keyCode == -1) {
                    CommandManager.INSTANCE.sendMessage("§cInvalid key: §e" + keyName);
                    return;
                }

                MacroManager.INSTANCE.addMacro(name, command, keyCode);
                CommandManager.INSTANCE.sendMessage("§aCreated macro §e" + name + " §7> §d" + command + " §7[§e" + keyName + "§7]");
            }

            case "delete", "remove", "del" -> {
                if (args.length < 2) {
                    CommandManager.INSTANCE.sendMessage("§cUsage: |macro delete <name>");
                    return;
                }

                String name = args[1];
                Macro macro = MacroManager.INSTANCE.getMacro(name);

                if (macro == null) {
                    CommandManager.INSTANCE.sendMessage("§cMacro not found: §e" + name);
                    return;
                }

                MacroManager.INSTANCE.removeMacro(name);
                CommandManager.INSTANCE.sendMessage("§aDeleted macro §e" + name);
            }

            case "list" -> {
                var macros = MacroManager.INSTANCE.getMacros();
                if (macros.isEmpty()) {
                    CommandManager.INSTANCE.sendMessage("§7You don't have any macros, loser");
                    return;
                }

                CommandManager.INSTANCE.sendMessage("§9Macros §7(" + macros.size() + "):");
                for (Macro macro : macros) {
                    String keyName = getKeyName(macro.getKey());
                    CommandManager.INSTANCE.sendMessage("§7- §e" + macro.getName() + " §7> §d" + macro.getCommand() + " §7[§e" + keyName + "§7]");
                }
            }

            default -> CommandManager.INSTANCE.sendMessage("§cUnknown action: §e" + action);
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

    private int getKeyCode(String name) {
        if (name.length() == 1) {
            return GLFW.GLFW_KEY_A + (name.charAt(0) - 'A');
        }

        return switch (name) {
            case "LSHIFT", "SHIFT" -> GLFW.GLFW_KEY_LEFT_SHIFT;
            case "RSHIFT" -> GLFW.GLFW_KEY_RIGHT_SHIFT;
            case "LCTRL", "CTRL" -> GLFW.GLFW_KEY_LEFT_CONTROL;
            case "RCTRL" -> GLFW.GLFW_KEY_RIGHT_CONTROL;
            case "LALT", "ALT" -> GLFW.GLFW_KEY_LEFT_ALT;
            case "RALT" -> GLFW.GLFW_KEY_RIGHT_ALT;
            case "SPACE" -> GLFW.GLFW_KEY_SPACE;
            case "TAB" -> GLFW.GLFW_KEY_TAB;
            case "ENTER" -> GLFW.GLFW_KEY_ENTER;
            case "BACKSPACE" -> GLFW.GLFW_KEY_BACKSPACE;
            case "BACKSLASH" -> GLFW.GLFW_KEY_BACKSLASH;
            default -> -1;
        };
    }
}

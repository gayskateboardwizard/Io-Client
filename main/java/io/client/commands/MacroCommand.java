package io.client.commands;

import io.client.managers.MacroManager;
import io.client.modules.macros.Macro;
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
                    CommandManager.INSTANCE.sendMessage("§cUsage: |macro new <name|{name}> <command|{command}> <keybind>");
                    return;
                }

                ParseResult nameResult = parseField(args, 1);
                if (nameResult == null) {
                    CommandManager.INSTANCE.sendMessage("§cInvalid name format");
                    return;
                }

                ParseResult commandResult = parseField(args, nameResult.nextIndex);
                if (commandResult == null) {
                    CommandManager.INSTANCE.sendMessage("§cInvalid command format");
                    return;
                }

                if (commandResult.nextIndex >= args.length) {
                    CommandManager.INSTANCE.sendMessage("§cMissing keybind");
                    return;
                }

                String name = nameResult.value;
                String command = commandResult.value;
                String keyBind = args[commandResult.nextIndex];

                if (MacroManager.INSTANCE.getMacro(name) != null) {
                    CommandManager.INSTANCE.sendMessage("§e" + name + " §7already exists");
                    return;
                }

                int[] keyCodes = parseKeybind(keyBind);
                if (keyCodes == null) {
                    CommandManager.INSTANCE.sendMessage("§cInvalid keybind: §e" + keyBind);
                    return;
                }

                MacroManager.INSTANCE.addMacro(name, command, keyCodes);
                CommandManager.INSTANCE.sendMessage("§aCreated macro §e" + name + " §7> §d" + command + " §7[§e" + keyBind.toUpperCase() + "§7]");
            }

            case "delete", "remove", "del" -> {
                if (args.length < 2) {
                    CommandManager.INSTANCE.sendMessage("§cUsage: |macro delete <name|{name}>");
                    return;
                }

                ParseResult nameResult = parseField(args, 1);
                if (nameResult == null) {
                    CommandManager.INSTANCE.sendMessage("§cInvalid name format");
                    return;
                }

                String name = nameResult.value;
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
                    String keyName = formatKeybind(macro.getKeyCodes());
                    CommandManager.INSTANCE.sendMessage("§7- §e" + macro.getName() + " §7> §d" + macro.getCommand() + " §7[§e" + keyName + "§7]");
                }
            }

            default -> CommandManager.INSTANCE.sendMessage("§cUnknown action: §e" + action);
        }
    }

    private static class ParseResult {
        String value;
        int nextIndex;

        ParseResult(String value, int nextIndex) {
            this.value = value;
            this.nextIndex = nextIndex;
        }
    }

    private ParseResult parseField(String[] args, int startIndex) {
        if (startIndex >= args.length) return null;

        String first = args[startIndex];

        if (first.startsWith("{")) {
            StringBuilder builder = new StringBuilder();
            int endIndex = startIndex;

            for (int i = startIndex; i < args.length; i++) {
                if (i > startIndex) builder.append(" ");
                builder.append(args[i]);

                if (args[i].endsWith("}")) {
                    endIndex = i + 1;
                    break;
                }
            }

            String result = builder.toString();
            if (!result.endsWith("}")) return null;

            result = result.substring(1, result.length() - 1);
            return new ParseResult(result, endIndex);
        } else {
            return new ParseResult(first, startIndex + 1);
        }
    }

    private int[] parseKeybind(String keyBind) {
        String[] parts = keyBind.toUpperCase().split("\\+");
        int[] keys = new int[parts.length];

        for (int i = 0; i < parts.length; i++) {
            int key = getKeyCode(parts[i].trim());
            if (key == -1) return null;
            keys[i] = key;
        }

        return keys;
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

    private int getKeyCode(String name) {
        if (name.length() == 1) {
            char c = name.charAt(0);
            if (c >= 'A' && c <= 'Z') {
                return GLFW.GLFW_KEY_A + (c - 'A');
            }
            if (c >= '0' && c <= '9') {
                return GLFW.GLFW_KEY_0 + (c - '0');
            }
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




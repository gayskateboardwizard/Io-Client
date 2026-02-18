package io.client.commands;

import io.client.managers.ModuleManager;
import io.client.modules.templates.Module;

import org.lwjgl.glfw.GLFW;

public class BindCommand implements Command {
    @Override
    public void execute(String[] args) throws Exception {
        if (args.length < 2) {
            CommandManager.INSTANCE.sendMessage("§cUsage: |bind <module> <key>");
            CommandManager.INSTANCE.sendMessage("§cUse 'clear' to unbind");
            return;
        }

        String moduleName = args[0];
        String keyName = args[1].toUpperCase();

        Module module = ModuleManager.INSTANCE.getModuleByName(moduleName);
        if (module == null) {
            CommandManager.INSTANCE.sendMessage("§cModule not found: " + moduleName);
            return;
        }

        int keyCode;
        if (keyName.equals("CLEAR")) {
            keyCode = -1;
        } else if (keyName.length() == 1) {
            keyCode = GLFW.GLFW_KEY_A + (keyName.charAt(0) - 'A');
        } else {
            keyCode = getKeyCode(keyName);
            if (keyCode == -1) {
                CommandManager.INSTANCE.sendMessage("§cInvalid key: " + keyName);
                return;
            }
        }

        module.setKey(keyCode);
        ModuleManager.INSTANCE.saveModules();

        if (keyCode == -1) {
            CommandManager.INSTANCE.sendMessage("§aUnbound " + moduleName);
        } else {
            CommandManager.INSTANCE.sendMessage("§aBound " + moduleName + " to " + keyName);
        }
    }

    private int getKeyCode(String name) {
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
            default -> -1;
        };
    }
}


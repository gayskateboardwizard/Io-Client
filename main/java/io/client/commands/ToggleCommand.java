package io.client.commands;

import io.client.Module;
import io.client.ModuleManager;

public class ToggleCommand implements Command {
    @Override
    public void execute(String[] args) throws Exception {
        if (args.length < 1) {
            CommandManager.INSTANCE.sendMessage("§cUsage: |toggles <module>");
            return;
        }

        String moduleName = args[0];
        Module module = ModuleManager.INSTANCE.getModuleByName(moduleName);

        if (module == null) {
            CommandManager.INSTANCE.sendMessage("§cModule not found: " + moduleName);
            return;
        }

        module.toggle();
        CommandManager.INSTANCE.sendMessage("§a" + moduleName + " is now " + (module.isEnabled() ? "§aenabled" : "§cdisabled"));
        ModuleManager.INSTANCE.saveModules();
    }
}
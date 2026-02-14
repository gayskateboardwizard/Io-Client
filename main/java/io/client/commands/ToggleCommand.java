package io.client.commands;

import io.client.MacroManager;
import io.client.Module;
import io.client.ModuleManager;
import io.client.modules.Macro;

public class ToggleCommand implements Command {
    @Override
    public void execute(String[] args) throws Exception {
        if (args.length < 1) {
            CommandManager.INSTANCE.sendMessage("§cUsage: |toggle <module|macro <name>>");
            return;
        }

        if (args[0].equalsIgnoreCase("macro")) {
            if (args.length < 2) {
                CommandManager.INSTANCE.sendMessage("§cUsage: |toggle macro <name>");
                return;
            }

            String macroName = args[1];
            Macro macro = MacroManager.INSTANCE.getMacro(macroName);

            if (macro == null) {
                CommandManager.INSTANCE.sendMessage("§cMacro not found: " + macroName);
                return;
            }

            macro.toggle();
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
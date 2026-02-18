package io.client.commands;

import io.client.managers.ModuleManager;
import io.client.modules.templates.Module;
import io.client.settings.BooleanSetting;
import io.client.settings.NumberSetting;
import io.client.settings.Setting;
import io.client.settings.StringSetting;

public class SetKeyCommand implements Command {
    @Override
    public void execute(String[] args) throws Exception {
        if (args.length < 3) {
            CommandManager.INSTANCE.sendMessage("§cUsage: |set <module> <setting> <value>");
            return;
        }

        String moduleName = args[0];
        String settingName = args[1];

        StringBuilder valueBuilder = new StringBuilder();
        for (int i = 2; i < args.length; i++) {
            if (i > 2) valueBuilder.append(" ");
            valueBuilder.append(args[i]);
        }
        String value = valueBuilder.toString();

        Module module = ModuleManager.INSTANCE.getModuleByName(moduleName);
        if (module == null) {
            CommandManager.INSTANCE.sendMessage("§cModule not found: " + moduleName);
            return;
        }

        Setting setting = null;
        for (Setting s : module.getSettings()) {
            if (s.getName().equalsIgnoreCase(settingName)) {
                setting = s;
                break;
            }
        }

        if (setting == null) {
            CommandManager.INSTANCE.sendMessage("§cSetting not found: " + settingName);
            return;
        }

        if (setting instanceof NumberSetting numSetting) {
            try {
                float val = Float.parseFloat(value);
                numSetting.setValue(val);
                CommandManager.INSTANCE.sendMessage("§aSet " + moduleName + "." + settingName + " to " + val);
                ModuleManager.INSTANCE.saveModules();
            } catch (NumberFormatException e) {
                CommandManager.INSTANCE.sendMessage("§cInvalid number: " + value);
            }
        } else if (setting instanceof BooleanSetting boolSetting) {
            boolean val = Boolean.parseBoolean(value);
            boolSetting.setEnabled(val);
            CommandManager.INSTANCE.sendMessage("§aSet " + moduleName + "." + settingName + " to " + val);
            ModuleManager.INSTANCE.saveModules();
        } else if (setting instanceof StringSetting strSetting) {
            strSetting.setValue(value);
            CommandManager.INSTANCE.sendMessage("§aSet " + moduleName + "." + settingName + " to " + value);
            ModuleManager.INSTANCE.saveModules();
        } else {
            CommandManager.INSTANCE.sendMessage("§cUnknown setting type");
        }
    }
}


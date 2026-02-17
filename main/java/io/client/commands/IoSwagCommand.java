package io.client.commands;

import io.client.ModuleManager;
import io.client.modules.misc.IoSwag;
import java.util.Locale;

public class IoSwagCommand implements Command {
    @Override
    public void execute(String[] args) throws Exception {
        IoSwag ioSwag = ModuleManager.INSTANCE.getModule(IoSwag.class);
        if (ioSwag == null) {
            CommandManager.INSTANCE.sendMessage("§cIoSwag module not found");
            return;
        }

        if (args.length == 0) {
            sendUsage();
            return;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "suffix" -> {
                if (args.length < 2) {
                    CommandManager.INSTANCE.sendMessage("§cUsage: |ioswag suffix <text>");
                    return;
                }
                String value = joinArgs(args, 1);
                ioSwag.setCustomSuffix(value);
                ModuleManager.INSTANCE.saveModules();
                CommandManager.INSTANCE.sendMessage("§aIoSwag custom suffix set to §f" + value);
            }
            case "preset" -> {
                if (args.length < 2) {
                    CommandManager.INSTANCE.sendMessage("§cUsage: |ioswag preset <option>");
                    return;
                }
                String option = joinArgs(args, 1);
                if (ioSwag.setPresetSuffix(option)) {
                    ModuleManager.INSTANCE.saveModules();
                    CommandManager.INSTANCE.sendMessage("§aIoSwag preset suffix set to §f" + ioSwag.getSuffix());
                } else {
                    CommandManager.INSTANCE.sendMessage("§cUnknown preset: §f" + option);
                    CommandManager.INSTANCE.sendMessage("§7Use |ioswag presets to view valid options");
                }
            }
            case "presets" -> CommandManager.INSTANCE.sendMessage(
                    "§9IoSwag presets: §7" + String.join(" §8| §7", ioSwag.getPresetSuffixes()));
            case "clear" -> {
                ioSwag.clearCustomSuffix();
                ModuleManager.INSTANCE.saveModules();
                CommandManager.INSTANCE.sendMessage("§aIoSwag custom suffix cleared");
            }
            default -> sendUsage();
        }
    }

    private static String joinArgs(String[] args, int start) {
        StringBuilder valueBuilder = new StringBuilder();
        for (int i = start; i < args.length; i++) {
            if (i > start) {
                valueBuilder.append(" ");
            }
            valueBuilder.append(args[i]);
        }
        return valueBuilder.toString();
    }

    private static void sendUsage() {
        CommandManager.INSTANCE.sendMessage("§d|ioswag suffix <text> §7- Set a custom suffix");
        CommandManager.INSTANCE.sendMessage("§d|ioswag preset <option> §7- Use a preset suffix");
        CommandManager.INSTANCE.sendMessage("§d|ioswag presets §7- Show all preset suffixes");
        CommandManager.INSTANCE.sendMessage("§d|ioswag greentext <true/false> §7- Toggle greentext");
        CommandManager.INSTANCE.sendMessage("§d|ioswag clear §7- Remove custom suffix and use preset");
    }
}


package io.client.commands;

import io.client.ModuleManager;
import io.client.modules.render.HUD;
import net.minecraft.client.MinecraftClient;

public class ChordTargetCommand implements Command {
    @Override
    public void execute(String[] args) throws Exception {
        HUD hud = ModuleManager.INSTANCE.getModule(HUD.class);
        if (hud == null) {
            CommandManager.INSTANCE.sendMessage("§cHUD module not found");
            return;
        }

        if (args.length == 0) {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player != null) {
                double x = mc.player.getX();
                double y = mc.player.getY();
                double z = mc.player.getZ();
                hud.setChordTarget(x, y, z);
                CommandManager.INSTANCE.sendMessage("§aSet chord target to current position: " +
                        String.format("%.1f, %.1f, %.1f", x, y, z));
            }
            return;
        }

        if (args[0].equalsIgnoreCase("clear")) {
            hud.clearChordTarget();
            CommandManager.INSTANCE.sendMessage("§aCleared chord target");
            return;
        }

        if (args.length < 3) {
            CommandManager.INSTANCE.sendMessage("§cUsage: |cordtarget <x> <y> <z>");
            CommandManager.INSTANCE.sendMessage("§cUsage: |cordtarget clear");
            CommandManager.INSTANCE.sendMessage("§cUsage: |cordtarget (no args for current pos)");
            return;
        }

        try {
            double x = Double.parseDouble(args[0]);
            double y = Double.parseDouble(args[1]);
            double z = Double.parseDouble(args[2]);

            hud.setChordTarget(x, y, z);
            CommandManager.INSTANCE.sendMessage("§aSet chord target to " + x + ", " + y + ", " + z);
        } catch (NumberFormatException e) {
            CommandManager.INSTANCE.sendMessage("§cInvalid coordinates");
        }
    }
}

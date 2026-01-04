package io.client.commands;

import io.client.ModuleManager;
import io.client.Module;
import io.client.modules.DonkeyBoatDupe;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public class MarkChestCommand implements Command {
    private final Minecraft mc = Minecraft.getInstance();

    @Override
    public void execute(String[] args) throws Exception {
        
        DonkeyBoatDupe dupeModule = ModuleManager.INSTANCE.getModule(DonkeyBoatDupe.class);

        if (dupeModule == null) {
            CommandManager.INSTANCE.sendMessage("§cDonkeyBoatDupe module not found!");
            return;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("clear")) {
            dupeModule.setMarkedChest(null);
            CommandManager.INSTANCE.sendMessage("§aCleared marked chest");
            return;
        }

        
        HitResult hit = mc.hitResult;

        if (hit == null || hit.getType() != HitResult.Type.BLOCK) {
            CommandManager.INSTANCE.sendMessage("§cYou must be looking at a chest!");
            return;
        }

        BlockHitResult blockHit = (BlockHitResult) hit;
        BlockPos pos = blockHit.getBlockPos();

        if (!(mc.level.getBlockEntity(pos) instanceof ChestBlockEntity)) {
            CommandManager.INSTANCE.sendMessage("§cThat's not a chest!");
            return;
        }

        dupeModule.setMarkedChest(pos);
        CommandManager.INSTANCE.sendMessage("§aMarked chest at " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ());
    }
}
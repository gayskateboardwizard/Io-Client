package io.client.modules;

import io.client.Category;
import io.client.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public class AutoMine extends Module {

    public AutoMine() {
        super("AutoMine", "Automatically mines blocks", -1, Category.WORLD);
    }

    @Override
    public void onUpdate() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.gameMode == null) return;
        HitResult hitResult = mc.hitResult;

        if (hitResult != null && hitResult.getType() == HitResult.Type.BLOCK) {
            BlockHitResult blockHit = (BlockHitResult) hitResult;

            mc.gameMode.startDestroyBlock(blockHit.getBlockPos(), blockHit.getDirection());

            mc.gameMode.continueDestroyBlock(blockHit.getBlockPos(), blockHit.getDirection());

        } else {
            mc.gameMode.stopDestroyBlock();
        }
    }

    @Override
    public void onDisable() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gameMode != null) {
            mc.gameMode.stopDestroyBlock();
        }
    }
}
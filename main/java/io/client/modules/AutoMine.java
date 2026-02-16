package io.client.modules;

import io.client.Category;
import io.client.Module;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;

public class AutoMine extends Module {

    public AutoMine() {
        super("AutoMine", "Automatically mines blocks", -1, Category.WORLD);
    }

    @Override
    public void onUpdate() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.interactionManager == null) return;
        HitResult hitResult = mc.crosshairTarget;

        if (hitResult != null && hitResult.getType() == HitResult.Type.BLOCK) {
            BlockHitResult blockHit = (BlockHitResult) hitResult;

            mc.interactionManager.attackBlock(blockHit.getBlockPos(), blockHit.getSide());

            mc.interactionManager.updateBlockBreakingProgress(blockHit.getBlockPos(), blockHit.getSide());

        } else {
            mc.interactionManager.cancelBlockBreaking();
        }
    }

    @Override
    public void onDisable() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.interactionManager != null) {
            mc.interactionManager.cancelBlockBreaking();
        }
    }
}
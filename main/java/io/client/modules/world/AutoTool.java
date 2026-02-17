package io.client.modules.world;

import io.client.Category;
import io.client.Module;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;

public class AutoTool extends Module {
    private int previousSlot = -1;
    private boolean hasSwitched = false;

    public AutoTool() {
        super("AutoTool", "Auto switch to tools", -1, Category.WORLD);
    }

    @Override
    public void onUpdate() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null || mc.crosshairTarget == null) return;

        if (mc.interactionManager.isBreakingBlock() && mc.crosshairTarget.getType() == HitResult.Type.BLOCK) {
            BlockPos pos = ((BlockHitResult) mc.crosshairTarget).getBlockPos();
            switchTool(mc, pos);
        } else {
            switchBack(mc);
        }
    }

    private void switchTool(MinecraftClient mc, BlockPos pos) {
        BlockState state = mc.world.getBlockState(pos);
        float bestSpeed = 1.0F;
        int bestSlot = -1;

        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = mc.player.getInventory().getStack(slot);
            if (stack.isEmpty()) continue;

            float speed = stack.getMiningSpeedMultiplier(state);
            if (speed > bestSpeed) {
                bestSpeed = speed;
                bestSlot = slot;
            }
        }

        int currentSlot = mc.player.getInventory().getSelectedSlot();

        if (bestSlot != -1 && bestSlot != currentSlot) {
            if (!hasSwitched) {
                previousSlot = currentSlot;
                hasSwitched = true;
            }
            mc.player.getInventory().setSelectedSlot(bestSlot);
        }
    }

    private void switchBack(MinecraftClient mc) {
        if (hasSwitched && previousSlot != -1) {
            mc.player.getInventory().setSelectedSlot(previousSlot);
            previousSlot = -1;
            hasSwitched = false;
        }
    }

    @Override
    public void onDisable() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) {
            switchBack(mc);
        }
    }
}

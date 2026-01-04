package io.client.modules;

import io.client.Category;
import io.client.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public class AutoTool extends Module {
    private int previousSlot = -1;
    private boolean hasSwitched = false;

    public AutoTool() {
        super("AutoTool", "Auto switch to tools", -1, Category.WORLD);
    }

    @Override
    public void onUpdate() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.hitResult == null) return;

        if (mc.gameMode.isDestroying() && mc.hitResult.getType() == HitResult.Type.BLOCK) {
            BlockPos pos = ((BlockHitResult) mc.hitResult).getBlockPos();
            switchTool(mc, pos);
        } else {
            switchBack(mc);
        }
    }

    private void switchTool(Minecraft mc, BlockPos pos) {
        BlockState state = mc.level.getBlockState(pos);
        float bestSpeed = 1.0F;
        int bestSlot = -1;

        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = mc.player.getInventory().getItem(slot);
            if (stack.isEmpty()) continue;

            float speed = stack.getDestroySpeed(state);
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

    private void switchBack(Minecraft mc) {
        if (hasSwitched && previousSlot != -1) {
            mc.player.getInventory().setSelectedSlot(previousSlot);
            previousSlot = -1;
            hasSwitched = false;
        }
    }

    @Override
    public void onDisable() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            switchBack(mc);
        }
    }
}
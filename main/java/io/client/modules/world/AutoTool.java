package io.client.modules.world;

import io.client.modules.templates.Category;
import io.client.modules.templates.Module;
import io.client.settings.BooleanSetting;
import io.client.settings.NumberSetting;
import io.client.settings.RadioSetting;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;

public class AutoTool extends Module {
    private boolean wasPressed = false;
    private boolean shouldSwitch = false;
    private int ticks = 0;
    private int bestSlot = -1;
    private int previousSlot = -1;
    private boolean hasSwitched = false;

    private final BooleanSetting antiBreak = new BooleanSetting("Anti Break", false);
    private final NumberSetting breakDurability = new NumberSetting("Break Durability %", 10.0f, 1.0f, 100.0f);
    private final BooleanSetting switchBack = new BooleanSetting("Switch Back", false);
    private final NumberSetting switchDelay = new NumberSetting("Switch Delay", 0f, 0f, 20f);

    public AutoTool() {
        super("AutoTool", "Switches to the best tool", -1, Category.WORLD);
        addSetting(antiBreak);
        addSetting(breakDurability);
        addSetting(switchBack);
        addSetting(switchDelay);
    }

    @Override
    public void onUpdate() {
        if (ticks <= 0 && shouldSwitch && bestSlot != -1) {
            swapTo(bestSlot);
            shouldSwitch = false;
        } else {
            ticks--;
        }

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null || mc.crosshairTarget == null) return;

        if (mc.interactionManager.isBreakingBlock() && mc.crosshairTarget.getType() == HitResult.Type.BLOCK) {
            BlockPos pos = ((BlockHitResult) mc.crosshairTarget).getBlockPos();
            evaluateBestTool(mc, pos);
        } else {
            revertIfNeeded(mc);
        }

        wasPressed = mc.options.attackKey.isPressed();
    }

    private void evaluateBestTool(MinecraftClient mc, BlockPos pos) {
        BlockState state = mc.world.getBlockState(pos);
        double bestScore = -1;
        bestSlot = -1;

        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.isEmpty()) continue;

            double score = stack.getMiningSpeedMultiplier(state);
            if (score > bestScore) {
                bestScore = score;
                bestSlot = i;
            }
        }

        int current = mc.player.getInventory().getSelectedSlot();
        if (bestSlot != -1 && (bestSlot != current)) {
            ticks = (int) switchDelay.getValue();
            if (ticks == 0) {
                swapTo(bestSlot);
            } else {
                shouldSwitch = true;
            }
        }
    }

    private void swapTo(int slot) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        int current = mc.player.getInventory().getSelectedSlot();
        if (!hasSwitched) {
            previousSlot = current;
            hasSwitched = true;
        }
        mc.player.getInventory().setSelectedSlot(slot);
    }

    private void revertIfNeeded(MinecraftClient mc) {
        if (switchBack.isEnabled() && hasSwitched && !mc.options.attackKey.isPressed() && previousSlot != -1) {
            mc.player.getInventory().setSelectedSlot(previousSlot);
            previousSlot = -1;
            hasSwitched = false;
        }
    }

    @Override
    public void onDisable() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null && previousSlot != -1) {
            mc.player.getInventory().setSelectedSlot(previousSlot);
        }
    }
}



package io.client.modules.world;

import io.client.modules.templates.Category;
import io.client.modules.templates.Module;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class Nuker extends Module {
    private static final int RANGE = 4;

    private BlockPos currentTarget = null;


    public Nuker() {
        super("Nuker", "Break blocks around you", -1, Category.WORLD);
    }

    @Override
    public void onUpdate() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null || mc.interactionManager == null) return;

        BlockPos playerPos = mc.player.getBlockPos();
        BlockPos newTarget = null;
        double shortestDistanceSq = Double.MAX_VALUE;

        for (int x = -RANGE; x <= RANGE; x++) {
            for (int y = -RANGE; y <= RANGE; y++) {
                for (int z = -RANGE; z <= RANGE; z++) {
                    BlockPos pos = playerPos.add(x, y, z);
                    BlockState state = mc.world.getBlockState(pos);

                    if (!state.isAir() && !state.isOf(Blocks.BEDROCK) && !pos.equals(playerPos)) {

                        double distanceSq = playerPos.getSquaredDistance(pos);

                        if (distanceSq < shortestDistanceSq) {
                            shortestDistanceSq = distanceSq;
                            newTarget = pos;
                        }
                    }
                }
            }
        }


        if (newTarget != null) {

            if (!newTarget.equals(currentTarget)) {
                mc.interactionManager.cancelBlockBreaking();

                mc.interactionManager.attackBlock(newTarget, Direction.UP);

                currentTarget = newTarget;
            } else {
                mc.interactionManager.updateBlockBreakingProgress(newTarget, Direction.UP);
            }
        } else if (currentTarget != null) {
            mc.interactionManager.cancelBlockBreaking();
            currentTarget = null;
        }
    }

    @Override
    public void onDisable() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null && mc.interactionManager != null && currentTarget != null) {
            mc.interactionManager.cancelBlockBreaking();
        }
        currentTarget = null;
    }
}   



package io.client.modules;

import io.client.Category;
import io.client.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class Nuker extends Module {
    private static final int RANGE = 4;

    private BlockPos currentTarget = null;


    public Nuker() {
        super("Nuker", "Break blocks around you", -1, Category.WORLD);
    }

    @Override
    public void onUpdate() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.gameMode == null) return;

        BlockPos playerPos = mc.player.blockPosition();
        BlockPos newTarget = null;
        double shortestDistanceSq = Double.MAX_VALUE;

        for (int x = -RANGE; x <= RANGE; x++) {
            for (int y = -RANGE; y <= RANGE; y++) {
                for (int z = -RANGE; z <= RANGE; z++) {
                    BlockPos pos = playerPos.offset(x, y, z);
                    BlockState state = mc.level.getBlockState(pos);

                    if (!state.isAir() && !state.is(Blocks.BEDROCK) && !pos.equals(playerPos)) {

                        double distanceSq = playerPos.distSqr(pos);

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
                mc.gameMode.stopDestroyBlock();

                mc.gameMode.startDestroyBlock(newTarget, Direction.UP);

                currentTarget = newTarget;
            } else {
                mc.gameMode.continueDestroyBlock(newTarget, Direction.UP);
            }
        } else if (currentTarget != null) {
            mc.gameMode.stopDestroyBlock();
            currentTarget = null;
        }
    }

    @Override
    public void onDisable() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.gameMode != null && currentTarget != null) {
            mc.gameMode.stopDestroyBlock();
        }
        currentTarget = null;
    }
}   
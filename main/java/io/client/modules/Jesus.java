package io.client.modules;

import io.client.Category;
import io.client.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class Jesus extends Module {

    public Jesus() {
        super("Jesus", "Walk on water", -1, Category.MOVEMENT);
    }

    @Override
    public void onUpdate() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        BlockPos playerPos = mc.player.blockPosition();
        BlockState blockBelowState = mc.level.getBlockState(playerPos.below());

        boolean isSinking = mc.player.getDeltaMovement().y < 0.0;

        if (mc.player.isInWater() && !mc.options.keyJump.isDown()) {
            Vec3 motion = mc.player.getDeltaMovement();
            mc.player.setDeltaMovement(motion.x, 0.1, motion.z);
        }

        if (isSinking &&
                !mc.player.isInWater() &&
                blockBelowState.is(Blocks.WATER)) {

            Vec3 motion = mc.player.getDeltaMovement();
            mc.player.setDeltaMovement(motion.x, 0, motion.z);

            double targetY = playerPos.below().getY() + 0.95;


            if (mc.player.getY() < targetY) {
                mc.player.setPos(mc.player.getX(), targetY, mc.player.getZ());
            }

            mc.player.setOnGround(true);
        }
    }
}
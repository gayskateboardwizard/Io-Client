package io.client.modules.movement;

import io.client.Category;
import io.client.Module;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class Jesus extends Module {

    public Jesus() {
        super("Jesus", "Walk on water", -1, Category.MOVEMENT);
    }

    @Override
    public void onUpdate() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;

        BlockPos playerPos = mc.player.getBlockPos();
        BlockState blockBelowState = mc.world.getBlockState(playerPos.down());

        boolean isSinking = mc.player.getVelocity().y < 0.0;

        if (mc.player.isTouchingWater() && !mc.options.jumpKey.isPressed()) {
            Vec3d motion = mc.player.getVelocity();
            mc.player.setVelocity(motion.x, 0.1, motion.z);
        }

        if (isSinking &&
                !mc.player.isTouchingWater() &&
                blockBelowState.isOf(Blocks.WATER)) {

            Vec3d motion = mc.player.getVelocity();
            mc.player.setVelocity(motion.x, 0, motion.z);

            double targetY = playerPos.down().getY() + 0.95;


            if (mc.player.getY() < targetY) {
                mc.player.setPosition(mc.player.getX(), targetY, mc.player.getZ());
            }

            mc.player.setOnGround(true);
        }
    }
}

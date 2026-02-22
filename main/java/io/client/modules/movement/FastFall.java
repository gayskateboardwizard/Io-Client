package io.client.modules.movement;

import io.client.modules.templates.Category;
import io.client.modules.templates.Module;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Vec3d;

public class FastFall extends Module {
    public FastFall() {
        super("FastFall", "Acceleration of player fall", -1, Category.MOVEMENT);
    }

    @Override
    public void onUpdate() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;

        if (mc.player.isOnGround() || mc.player.isGliding() || mc.player.isTouchingWater() || mc.player.isDead()) {
            return;
        }

        if (mc.player.fallDistance > 2.0f) {
            Vec3d velocity = mc.player.getVelocity();
            mc.player.setVelocity(velocity.x, -0.5, velocity.z);
        }
    }
}

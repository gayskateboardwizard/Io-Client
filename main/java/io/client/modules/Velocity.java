package io.client.modules;

import io.client.Category;
import io.client.Module;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Vec3d;

public class Velocity extends Module {

    public Velocity() {
        super("Velocity", "No knockback", -1, Category.COMBAT);
    }

    @Override
    public void onUpdate() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        if (mc.player.hurtTime == 1) {
            Vec3d motion = mc.player.getVelocity();

            mc.player.setVelocity(0.0, motion.y, 0.0);


        }
    }
}
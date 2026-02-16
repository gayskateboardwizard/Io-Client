package io.client.modules;

import io.client.Category;
import io.client.Module;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Vec3d;

public class Spider extends Module {

    public Spider() {
        super("Spider", "Climb walls like spider", -1, Category.MOVEMENT);
    }

    @Override
    public void onUpdate() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;


        if (mc.player.horizontalCollision) {


            if (mc.options.jumpKey.isPressed()) return;

            Vec3d motion = mc.player.getVelocity();


            mc.player.setVelocity(motion.x, 0.2, motion.z);
        }
    }
}
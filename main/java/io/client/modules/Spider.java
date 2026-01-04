package io.client.modules;

import io.client.Category;
import io.client.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

public class Spider extends Module {

    public Spider() {
        super("Spider", "Climb walls like spider", -1, Category.MOVEMENT);
    }

    @Override
    public void onUpdate() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

         
        if (mc.player.horizontalCollision) {

             
            if (mc.options.keyJump.isDown()) return;

            Vec3 motion = mc.player.getDeltaMovement();

             
             
            mc.player.setDeltaMovement(motion.x, 0.2, motion.z);
        }
    }
}
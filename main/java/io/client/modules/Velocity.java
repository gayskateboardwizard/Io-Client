package io.client.modules;

import io.client.Category;
import io.client.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

public class Velocity extends Module {

    public Velocity() {
        super("Velocity", "No knockback", -1, Category.COMBAT);
    }

    @Override
    public void onUpdate() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        if (mc.player.hurtTime == 1) {
            Vec3 motion = mc.player.getDeltaMovement();

            mc.player.setDeltaMovement(0.0, motion.y, 0.0);

             
        }
    }
}
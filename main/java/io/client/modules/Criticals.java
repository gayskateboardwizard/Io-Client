package io.client.modules;

import io.client.Category;
import io.client.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

public class Criticals extends Module {

    public Criticals() {
        super("Criticals", "Always crit", -1, Category.COMBAT);
    }

    @Override
    public void onUpdate() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.player.isSpectator() || mc.player.isCreative()) return;

        if (mc.player.onGround() && mc.options.keyAttack.isDown()) {

            Vec3 motion = mc.player.getDeltaMovement();

            mc.player.setDeltaMovement(motion.x, 0.1, motion.z);

            mc.player.setOnGround(false);

        }
    }

    @Override
    public void onDisable() {
    }
}

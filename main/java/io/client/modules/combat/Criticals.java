package io.client.modules.combat;

import io.client.Category;
import io.client.Module;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Vec3d;

public class Criticals extends Module {

    public Criticals() {
        super("Criticals", "Always crit", -1, Category.COMBAT);
    }

    @Override
    public void onUpdate() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.player.isSpectator() || mc.player.isCreative()) return;

        if (mc.player.isOnGround() && mc.options.attackKey.isPressed()) {

            Vec3d motion = mc.player.getVelocity();

            mc.player.setVelocity(motion.x, 0.1, motion.z);

            mc.player.setOnGround(false);

        }
    }

    @Override
    public void onDisable() {
    }
}


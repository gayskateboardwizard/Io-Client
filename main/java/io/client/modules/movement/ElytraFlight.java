package io.client.modules.movement;

import io.client.Category;
import io.client.Module;
import io.client.settings.BooleanSetting;
import io.client.settings.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

public class ElytraFlight extends Module {
    public final NumberSetting horizontalSpeed = new NumberSetting("Horizontal", 2.0F, 0.0F, 5.0F);
    public final NumberSetting verticalSpeed = new NumberSetting("Vertical", 0.1F, 0.0F, 2.0F);
    public final BooleanSetting drag = new BooleanSetting("Drag", false);
    public final BooleanSetting lockY = new BooleanSetting("Lock Y", false);

    public ElytraFlight() {
        super("ElytraFlight", "Infinite elytra flight", 0, Category.MOVEMENT);
        addSetting(horizontalSpeed);
        addSetting(verticalSpeed);
        addSetting(drag);
        addSetting(lockY);
    }

    @Override
    public void onUpdate() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        PlayerEntity player = mc.player;

        if (player.isGliding()) {
            Vec3d lookVec = player.getRotationVector();
            Vec3d motion = player.getVelocity();

            double motionY = 0;
            double motionX = motion.x;
            double motionZ = motion.z;

            if (mc.options.forwardKey.isPressed()) {
                motionX = lookVec.x * horizontalSpeed.getValue();
                motionZ = lookVec.z * horizontalSpeed.getValue();
            } else if (mc.options.backKey.isPressed()) {
                motionX = -lookVec.x * horizontalSpeed.getValue() * 0.5;
                motionZ = -lookVec.z * horizontalSpeed.getValue() * 0.5;
            } else {
                if (drag.isEnabled() != true) {
                    motionX *= 0;
                    motionZ *= 0;
                }
            }

            if (mc.options.jumpKey.isPressed()) {
                motionY = verticalSpeed.getValue();
            } else if (mc.options.sneakKey.isPressed()) {
                motionY = -verticalSpeed.getValue();
            } else if (lockY.isEnabled()) {
                motionY = 0.02;
            }

            player.setVelocity(motionX, motionY, motionZ);
        }
    }
}

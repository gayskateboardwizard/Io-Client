package io.client.modules;

import io.client.Category;
import io.client.Module;
import io.client.settings.BooleanSetting;
import io.client.settings.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

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
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        Player player = mc.player;

        if (player.isFallFlying()) {
            Vec3 lookVec = player.getLookAngle();
            Vec3 motion = player.getDeltaMovement();

            double motionY = 0;
            double motionX = motion.x;
            double motionZ = motion.z;

            if (mc.options.keyUp.isDown()) {
                motionX = lookVec.x * horizontalSpeed.getValue();
                motionZ = lookVec.z * horizontalSpeed.getValue();
            } else if (mc.options.keyDown.isDown()) {
                motionX = -lookVec.x * horizontalSpeed.getValue() * 0.5;
                motionZ = -lookVec.z * horizontalSpeed.getValue() * 0.5;
            } else {
                if (drag.isEnabled() != true) {
                    motionX *= 0;
                    motionZ *= 0;
                }
            }

            if (mc.options.keyJump.isDown()) {
                motionY = verticalSpeed.getValue();
            } else if (mc.options.keyShift.isDown()) {
                motionY = -verticalSpeed.getValue();
            } else if (lockY.isEnabled()) {
                motionY = 0.02;  
            }

            player.setDeltaMovement(motionX, motionY, motionZ);
        }
    }
}
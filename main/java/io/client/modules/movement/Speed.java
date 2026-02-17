package io.client.modules.movement;

import io.client.Category;
import io.client.Module;
import io.client.settings.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;

public class Speed extends Module {
    public final NumberSetting speed = new NumberSetting("Speed", 1.2F, 1F, 5.0F);

    public Speed() {
        super("Speed", "Move faster", -1, Category.MOVEMENT);
        addSetting(speed);
    }

    @Override
    public void onUpdate() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        PlayerEntity player = mc.player;


        if (speed.getValue() <= 1.0F) return;


        double forward = player.forwardSpeed;
        double strafe = player.sidewaysSpeed;

        if (forward == 0.0 && strafe == 0.0) return;

        float yaw = player.getYaw();
        double motionX = forward * speed.getValue() * -Math.sin(Math.toRadians(yaw))
                + strafe * speed.getValue() * Math.cos(Math.toRadians(yaw));
        double motionZ = forward * speed.getValue() * Math.cos(Math.toRadians(yaw))
                + strafe * speed.getValue() * Math.sin(Math.toRadians(yaw));


        player.setVelocity(motionX, player.getVelocity().y, motionZ);
    }
}


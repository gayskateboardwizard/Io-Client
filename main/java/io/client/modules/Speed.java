package io.client.modules;

import io.client.Category;
import io.client.Module;
import io.client.settings.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

public class Speed extends Module {
    public final NumberSetting speed = new NumberSetting("Speed", 1.2F, 1F, 5.0F);

    public Speed() {
        super("Speed", "Move faster", -1, Category.MOVEMENT);
        addSetting(speed);
    }

    @Override
    public void onUpdate() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        Player player = mc.player;


        if (speed.getValue() <= 1.0F) return;


        double forward = player.zza;
        double strafe = player.xxa;

        if (forward == 0.0 && strafe == 0.0) return;

        float yaw = player.getYRot();
        double motionX = forward * speed.getValue() * -Math.sin(Math.toRadians(yaw))
                + strafe * speed.getValue() * Math.cos(Math.toRadians(yaw));
        double motionZ = forward * speed.getValue() * Math.cos(Math.toRadians(yaw))
                + strafe * speed.getValue() * Math.sin(Math.toRadians(yaw));


        player.setDeltaMovement(motionX, player.getDeltaMovement().y, motionZ);
    }
}

package io.client.modules.misc;

import io.client.modules.templates.Category;
import io.client.modules.templates.Module;
import net.minecraft.client.MinecraftClient;

public class AntiAFK extends Module {

    private float yawSpeed = 2.0f;

    public AntiAFK() {
        super("AntiAFK", "Prevents AFK kick", -1, Category.MISC);
    }

    @Override
    public void onUpdate() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        mc.options.forwardKey.setPressed(true);

        mc.player.setYaw(mc.player.getYaw() + yawSpeed);

        mc.player.headYaw = mc.player.getYaw();
        mc.player.bodyYaw = mc.player.getYaw();
    }

    @Override
    public void onDisable() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        mc.options.forwardKey.setPressed(false);
    }
}




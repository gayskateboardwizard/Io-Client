package io.client.modules;

import io.client.Category;
import io.client.Module;
import net.minecraft.client.Minecraft;

public class AntiAFK extends Module {

    private float yawSpeed = 2.0f;

    public AntiAFK() {
        super("AntiAFK", "Prevents AFK kick", -1, Category.MISC);
    }

    @Override
    public void onUpdate() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        mc.options.keyUp.setDown(true);

        mc.player.setYRot(mc.player.getYRot() + yawSpeed);

        mc.player.yHeadRot = mc.player.getYRot();
        mc.player.yBodyRot = mc.player.getYRot();
    }

    @Override
    public void onDisable() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        mc.options.keyUp.setDown(false);
    }
}

package io.client.modules;

import io.client.Category;
import io.client.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public class AutoSprint extends Module {

    public AutoSprint() {
        super("AutoSprint", "Automatically sprints", -1, Category.MOVEMENT);
    }

    @Override
    public void onUpdate() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        LocalPlayer player = mc.player;

        boolean canSprint = mc.options.keyUp.isDown() &&
                !player.isCrouching() &&
                player.getFoodData().getFoodLevel() > 6.0f;


        if (canSprint) {
            mc.options.keySprint.setDown(true);
        } else {
            mc.options.keySprint.setDown(false);
        }
    }

    @Override
    public void onDisable() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            if (mc.options.keySprint.isDown()) {
                mc.options.keySprint.setDown(false);
            }
        }
        if (mc.player != null && mc.player.isSprinting()) {
            mc.player.setSprinting(false);
        }
    }
}
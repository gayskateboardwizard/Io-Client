package io.client.modules;

import io.client.Category;
import io.client.Module;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;

public class AutoSprint extends Module {

    public AutoSprint() {
        super("AutoSprint", "Automatically sprints", -1, Category.MOVEMENT);
    }

    @Override
    public void onUpdate() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        ClientPlayerEntity player = mc.player;

        boolean canSprint = mc.options.forwardKey.isPressed() &&
                !player.isInSneakingPose() &&
                player.getHungerManager().getFoodLevel() > 6.0f;


        if (canSprint) {
            mc.options.sprintKey.setPressed(true);
        } else {
            mc.options.sprintKey.setPressed(false);
        }
    }

    @Override
    public void onDisable() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) {
            if (mc.options.sprintKey.isPressed()) {
                mc.options.sprintKey.setPressed(false);
            }
        }
        if (mc.player != null && mc.player.isSprinting()) {
            mc.player.setSprinting(false);
        }
    }
}
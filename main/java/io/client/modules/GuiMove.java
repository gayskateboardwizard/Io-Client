package io.client.modules;

import io.client.Category;
import io.client.Module;
import io.client.settings.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

public class GuiMove extends Module {
    private final NumberSetting speed;

    public GuiMove() {
        super("GuiMove", "Move while in GUIs", -1, Category.MOVEMENT);
        speed = new NumberSetting("Speed", 0.3f, 0.1f, 1.0f);
        addSetting(speed);
    }

    @Override
    public void onUpdate() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;

        if (player == null) return;
        if (player.getVehicle() != null) return;
        if (mc.screen == null) return;
        if (mc.screen instanceof ChatScreen) return;

        float yaw = player.getYRot();
        Vec3 motion = new Vec3(0, player.getDeltaMovement().y, 0);

        if (isKeyDown(GLFW.GLFW_KEY_W)) motion = motion.add(forwardVec(yaw).scale(speed.getValue()));
        if (isKeyDown(GLFW.GLFW_KEY_S)) motion = motion.add(forwardVec(yaw).scale(-speed.getValue()));
        if (isKeyDown(GLFW.GLFW_KEY_A)) motion = motion.add(forwardVec(yaw - 90).scale(speed.getValue()));
        if (isKeyDown(GLFW.GLFW_KEY_D)) motion = motion.add(forwardVec(yaw + 90).scale(speed.getValue()));

        player.setDeltaMovement(motion);
        player.hurtMarked = true;
    }

    private boolean isKeyDown(int glfwKey) {
        return GLFW.glfwGetKey(Minecraft.getInstance().getWindow().getWindow(), glfwKey) == GLFW.GLFW_PRESS;
    }

    private Vec3 forwardVec(float yaw) {
        double rad = Math.toRadians(yaw);
        return new Vec3(-Math.sin(rad), 0, Math.cos(rad));
    }
}
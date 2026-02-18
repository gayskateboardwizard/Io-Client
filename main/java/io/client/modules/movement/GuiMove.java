package io.client.modules.movement;

import io.client.modules.templates.Category;
import io.client.modules.templates.Module;
import io.client.settings.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.Vec3d;
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
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity player = mc.player;

        if (player == null) return;
        if (player.getVehicle() != null) return;
        if (mc.currentScreen == null) return;
        if (mc.currentScreen instanceof ChatScreen) return;

        float yaw = player.getYaw();
        Vec3d motion = new Vec3d(0, player.getVelocity().y, 0);

        if (isKeyDown(GLFW.GLFW_KEY_W)) motion = motion.add(forwardVec(yaw).multiply(speed.getValue()));
        if (isKeyDown(GLFW.GLFW_KEY_S)) motion = motion.add(forwardVec(yaw).multiply(-speed.getValue()));
        if (isKeyDown(GLFW.GLFW_KEY_A)) motion = motion.add(forwardVec(yaw - 90).multiply(speed.getValue()));
        if (isKeyDown(GLFW.GLFW_KEY_D)) motion = motion.add(forwardVec(yaw + 90).multiply(speed.getValue()));

        player.setVelocity(motion);
        player.velocityModified = true;
    }

    private boolean isKeyDown(int glfwKey) {
        return GLFW.glfwGetKey(MinecraftClient.getInstance().getWindow().getHandle(), glfwKey) == GLFW.GLFW_PRESS;
    }

    private Vec3d forwardVec(float yaw) {
        double rad = Math.toRadians(yaw);
        return new Vec3d(-Math.sin(rad), 0, Math.cos(rad));
    }
}



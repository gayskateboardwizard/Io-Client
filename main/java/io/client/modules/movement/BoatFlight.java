package io.client.modules.movement;

import io.client.Category;
import io.client.Module;
import io.client.ModuleManager;
import io.client.settings.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

public class BoatFlight extends Module {
    public final NumberSetting horizontalSpeed = new NumberSetting("Horizontal", 0F, 0.1F, 2.0F);
    public final NumberSetting verticalSpeed = new NumberSetting("Vertical", 0.1F, 0.0F, 1.0F);

    public BoatFlight() {
        super("BoatFlight", "Fly in boats", -1, Category.MOVEMENT);
        addSetting(horizontalSpeed);
        addSetting(verticalSpeed);
    }

    @Override
    public void onUpdate() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        Entity vehicle = mc.player.getVehicle();
        if (!(vehicle instanceof BoatEntity boat)) return;

        boat.setNoGravity(true);
        boat.setYaw(mc.player.getYaw());
        boat.setPitch(mc.player.getPitch());

        boolean guiMoveActive = false;
        Module guiMoveModule = ModuleManager.INSTANCE.getModule(io.client.modules.movement.GuiMove.class);
        if (guiMoveModule != null && guiMoveModule.isEnabled()) guiMoveActive = true;

        float forwardMove = 0;
        float strafe = 0;
        double motionY = 0;

        boolean shiftPressed = isKeyDown(GLFW.GLFW_KEY_LEFT_SHIFT);

        if (mc.currentScreen == null || guiMoveActive) {
            if (isKeyDown(GLFW.GLFW_KEY_W)) forwardMove += 1.0F;
            if (isKeyDown(GLFW.GLFW_KEY_S)) forwardMove -= 1.0F;
            if (isKeyDown(GLFW.GLFW_KEY_A)) strafe += 1.0F;
            if (isKeyDown(GLFW.GLFW_KEY_D)) strafe -= 1.0F;

            if (isKeyDown(GLFW.GLFW_KEY_SPACE)) motionY = verticalSpeed.getValue();
            if (shiftPressed) {
                motionY = -verticalSpeed.getValue();
            }
        }

        Vec3d moveVec = new Vec3d(strafe, 0, forwardMove);
        if (moveVec.length() > 0) {
            moveVec = moveVec.normalize().rotateY(-mc.player.getYaw() * MathHelper.RADIANS_PER_DEGREE);
        }

        double motionX = moveVec.x * horizontalSpeed.getValue();
        double motionZ = moveVec.z * horizontalSpeed.getValue();

        boat.setVelocity(motionX, motionY, motionZ);
    }

    @Override
    public void onDisable() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        Entity vehicle = mc.player.getVehicle();
        if (!(vehicle instanceof BoatEntity boat)) return;

        boat.setNoGravity(false);
    }

    private boolean isKeyDown(int key) {
        return GLFW.glfwGetKey(MinecraftClient.getInstance().getWindow().getHandle(), key) == GLFW.GLFW_PRESS;
    }
}


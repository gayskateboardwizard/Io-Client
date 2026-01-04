package io.client.modules;

import io.client.Category;
import io.client.Module;
import io.client.ModuleManager;
import io.client.settings.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.phys.Vec3;
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
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        Entity vehicle = mc.player.getVehicle();
        if (!(vehicle instanceof Boat boat)) return;

        boat.setNoGravity(true);
        boat.setYRot(mc.player.getYRot());
        boat.setXRot(mc.player.getXRot());

        boolean guiMoveActive = false;
        Module guiMoveModule = ModuleManager.INSTANCE.getModule(io.client.modules.GuiMove.class);
        if (guiMoveModule != null && guiMoveModule.isEnabled()) guiMoveActive = true;

        float forwardMove = 0;
        float strafe = 0;
        double motionY = 0;

        boolean shiftPressed = isKeyDown(GLFW.GLFW_KEY_LEFT_SHIFT);

        if (mc.screen == null || guiMoveActive) {
            if (isKeyDown(GLFW.GLFW_KEY_W)) forwardMove += 1.0F;
            if (isKeyDown(GLFW.GLFW_KEY_S)) forwardMove -= 1.0F;
            if (isKeyDown(GLFW.GLFW_KEY_A)) strafe += 1.0F;
            if (isKeyDown(GLFW.GLFW_KEY_D)) strafe -= 1.0F;

            if (isKeyDown(GLFW.GLFW_KEY_SPACE)) motionY = verticalSpeed.getValue();
            if (shiftPressed) {
                motionY = -verticalSpeed.getValue();
            }
        }

        Vec3 moveVec = new Vec3(strafe, 0, forwardMove);
        if (moveVec.length() > 0) {
            moveVec = moveVec.normalize().yRot(-mc.player.getYRot() * Mth.DEG_TO_RAD);
        }

        double motionX = moveVec.x * horizontalSpeed.getValue();
        double motionZ = moveVec.z * horizontalSpeed.getValue();

        boat.setDeltaMovement(motionX, motionY, motionZ);
    }

    @Override
    public void onDisable() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        Entity vehicle = mc.player.getVehicle();
        if (!(vehicle instanceof Boat boat)) return;

        boat.setNoGravity(false);
    }

    private boolean isKeyDown(int key) {
        return GLFW.glfwGetKey(Minecraft.getInstance().getWindow().getWindow(), key) == GLFW.GLFW_PRESS;
    }
}
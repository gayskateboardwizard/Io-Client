package io.client.modules.movement;

import io.client.Category;
import io.client.Module;
import io.client.settings.BooleanSetting;
import io.client.settings.NumberSetting;
import io.client.settings.RadioSetting;
import java.util.stream.StreamSupport;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.Box;

public class Step extends Module {
    public final NumberSetting stepHeight = new NumberSetting("Height", 2.1F, 0.1F, 7.0F);
    public final RadioSetting mode = new RadioSetting("Mode", "Vanilla");
    public final BooleanSetting extra = new BooleanSetting("Extra", false);
    public final BooleanSetting alternate = new BooleanSetting("Alternate", false);

    private double previousStepHeight = 0.6;
    private long stepTimer = 0;

    public Step() {
        super("Step", "Step up blocks", -1, Category.MOVEMENT);
        mode.addOption("Vanilla");
        mode.addOption("Normal");
        addSetting(stepHeight);
        addSetting(mode);
        addSetting(extra);
        addSetting(alternate);
    }

    @Override
    public void onEnable() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) {
            EntityAttributeInstance stepAttribute = mc.player.getAttributeInstance(EntityAttributes.STEP_HEIGHT);
            if (stepAttribute != null) {
                previousStepHeight = stepAttribute.getBaseValue();
            }
        }
        stepTimer = System.currentTimeMillis();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) {
            setStepHeight(mc, (float) previousStepHeight);
        }
        stepTimer = System.currentTimeMillis();
    }

    @Override
    public void onUpdate() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;


        if (mode.getSelectedOption().equals("Vanilla")) {
            handleVanillaMode(mc);
            return;
        }


        handleNormalMode(mc);
    }

    private void handleVanillaMode(MinecraftClient mc) {
        if (mc.player.isTouchingWater() || mc.player.isInLava() || mc.player.isGliding()) {
            setStepHeight(mc, 0.6f);
            return;
        }

        if (mc.player.isOnGround() && System.currentTimeMillis() - stepTimer > 50) {
            setStepHeight(mc, stepHeight.getValue());
        } else {
            setStepHeight(mc, 0.6f);
        }
    }

    private void handleNormalMode(MinecraftClient mc) {

        if (mc.player.isTouchingWater() || mc.player.isInLava() || mc.player.isGliding()) {
            setStepHeight(mc, 0.6f);
            return;
        }

        if (mc.player.isOnGround() && System.currentTimeMillis() - stepTimer > 50) {
            setStepHeight(mc, stepHeight.getValue());
        } else {
            setStepHeight(mc, 0.6f);
        }


        double height = mc.player.getY() - mc.player.lastY;
        if (height <= 0.5 || height > stepHeight.getValue()) {
            return;
        }

        if (alternate.isEnabled() && height <= 1.5f) {
            return;
        }


        double[] offsets = getStepOffsets(height);
        for (double off : offsets) {
            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                    mc.player.lastX,
                    mc.player.lastY + off,
                    mc.player.lastZ,
                    false,
                    false
            ));
        }

        stepTimer = System.currentTimeMillis();
    }

    public void doNCPStep(MinecraftClient mc, double[] dir, boolean forceStep) {
        if (mc.player == null || mc.world == null) return;

        boolean two = false;
        boolean onefive = false;
        boolean one = false;

        Box playerBox = mc.player.getBoundingBox();


        if (isCollisionFree(mc, playerBox.offset(dir[0], 2.1, dir[1])) &&
                !isCollisionFree(mc, playerBox.offset(dir[0], 1.9, dir[1]))) {
            two = true;
        }

        if (isCollisionFree(mc, playerBox.offset(dir[0], 1.6, dir[1])) &&
                !isCollisionFree(mc, playerBox.offset(dir[0], 1.4, dir[1]))) {
            onefive = true;
        }

        if (isCollisionFree(mc, playerBox.offset(dir[0], 1.0, dir[1])) &&
                !isCollisionFree(mc, playerBox.offset(dir[0], 0.6, dir[1]))) {
            one = true;
        }

        if (mc.player.horizontalCollision &&
                ((mc.player.forwardSpeed != 0 || mc.player.sidewaysSpeed != 0) || forceStep) &&
                mc.player.isOnGround()) {

            if (one && stepHeight.getValue() >= 1.0) {
                sendStepPackets(mc, 1.0);
            } else if (onefive && stepHeight.getValue() >= 1.5) {
                sendStepPackets(mc, 1.5);
            } else if (two && stepHeight.getValue() >= 2.0) {
                sendStepPackets(mc, 2.0);
            }
        }
    }

    private void sendStepPackets(MinecraftClient mc, double height) {
        double[] offsets = getStepOffsets(height);
        for (double v : offsets) {
            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                    mc.player.lastX,
                    mc.player.lastY + v,
                    mc.player.lastZ,
                    false,
                    false
            ));
        }
        mc.player.setPosition(mc.player.getX(), mc.player.getY() + height, mc.player.getZ());
    }

    private boolean isCollisionFree(MinecraftClient mc, Box box) {
        return StreamSupport.stream(
                mc.world.getCollisions(mc.player, box).spliterator(),
                false
        ).findAny().isEmpty();
    }

    private double[] getStepOffsets(double stepHeight) {
        double[] offsets;

        if (extra.isEnabled()) {
            if (stepHeight > 1.1661) {
                offsets = new double[]{0.42, 0.7532, 1.001, 1.1661, stepHeight};
            } else if (stepHeight > 1.015) {
                offsets = new double[]{0.42, 0.7532, 1.001, stepHeight};
            } else if (stepHeight > 0.6) {
                offsets = new double[]{0.42 * stepHeight, 0.7532 * stepHeight, stepHeight};
            } else {
                offsets = new double[0];
            }
            return offsets;
        }

        if (stepHeight > 2.019) {
            offsets = new double[]{0.425, 0.821, 0.699, 0.599, 1.022, 1.372, 1.652, 1.869, 2.019, 1.919};
        } else if (stepHeight > 1.5) {
            offsets = new double[]{0.42, 0.78, 0.63, 0.51, 0.9, 1.21, 1.45, 1.43};
        } else if (stepHeight > 1.015) {
            offsets = new double[]{0.42, 0.7532, 1.01, 1.093, 1.015};
        } else if (stepHeight > 0.6) {
            offsets = new double[]{0.42 * stepHeight, 0.7532 * stepHeight};
        } else {
            offsets = new double[0];
        }

        return offsets;
    }

    private void setStepHeight(MinecraftClient mc, float height) {
        EntityAttributeInstance stepAttribute = mc.player.getAttributeInstance(EntityAttributes.STEP_HEIGHT);
        if (stepAttribute != null) {
            stepAttribute.setBaseValue(height);
        }
    }
}

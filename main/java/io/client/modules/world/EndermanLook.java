package io.client.modules.world;

import io.client.modules.templates.Category;
import io.client.modules.templates.Module;
import io.client.settings.BooleanSetting;
import io.client.settings.RadioSetting;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.mob.EndermanEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

public class EndermanLook extends Module {
    private final RadioSetting lookMode = new RadioSetting("LookMode", "Away");
    private final BooleanSetting stunHostiles = new BooleanSetting("StunHostiles", true);
    private float lastSafeYaw;
    private float lastSafePitch;

    public EndermanLook() {
        super("EndermanLook", "Either looks at endermen or prevents looking at them", -1, Category.WORLD);
        lookMode.addOption("At");
        lookMode.addOption("Away");
        addSetting(lookMode);
        addSetting(stunHostiles);
    }

    public boolean shouldStrictlyBlockAggro(PlayerEntity player) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!isEnabled() || player == null || mc.player == null) {
            return false;
        }

        if (!"Away".equals(lookMode.getSelectedOption())) {
            return false;
        }

        if (player != mc.player) {
            return false;
        }

        return !player.getEquippedStack(EquipmentSlot.HEAD).isOf(Blocks.CARVED_PUMPKIN.asItem())
                && !player.getAbilities().creativeMode;
    }

    @Override
    public void onUpdate() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) {
            return;
        }

        // Same behavior as Meteor: carved pumpkin / creative mode short-circuits.
        if (mc.player.getEquippedStack(EquipmentSlot.HEAD).isOf(Blocks.CARVED_PUMPKIN.asItem())
                || mc.player.getAbilities().creativeMode) {
            return;
        }

        if ("Away".equals(lookMode.getSelectedOption())) {
            boolean shouldRevertToSafe = false;
            for (Entity entity : mc.world.getEntities()) {
                if (!(entity instanceof EndermanEntity enderman) || !enderman.isAlive() || !mc.player.canSee(enderman)) {
                    continue;
                }

                if (enderman.isAngry() && stunHostiles.isEnabled()) {
                    lookAt(mc, enderman);
                    continue;
                }

                if (angleCheck(mc, enderman)) {
                    shouldRevertToSafe = true;
                    break;
                }
            }

            if (shouldRevertToSafe) {
                // Keep view at the most recent safe orientation instead of forcing pitch down.
                mc.player.setYaw(lastSafeYaw);
                mc.player.setPitch(lastSafePitch);
            } else {
                lastSafeYaw = mc.player.getYaw();
                lastSafePitch = mc.player.getPitch();
            }
            return;
        }

        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof EndermanEntity enderman) || !enderman.isAlive() || !mc.player.canSee(enderman)) {
                continue;
            }

            switch (lookMode.getSelectedOption()) {
                case "At":
                    if (!enderman.isAngry()) {
                        lookAt(mc, enderman);
                    }
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public void onEnable() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) {
            lastSafeYaw = mc.player.getYaw();
            lastSafePitch = mc.player.getPitch();
        }
    }

    private void lookAt(MinecraftClient mc, EndermanEntity enderman) {
        Vec3d from = mc.player.getEyePos();
        Vec3d to = new Vec3d(enderman.getX(), enderman.getEyeY(), enderman.getZ());
        double dx = to.x - from.x;
        double dy = to.y - from.y;
        double dz = to.z - from.z;
        double horizontal = Math.sqrt(dx * dx + dz * dz);

        float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0f;
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, horizontal));
        mc.player.setYaw(yaw);
        mc.player.setPitch(pitch);
    }

    // Stricter-than-vanilla check to reduce edge-case aggro.
    private boolean angleCheck(MinecraftClient mc, EndermanEntity enderman) {
        Vec3d lookVec = mc.player.getRotationVec(1.0f).normalize();
        Vec3d toEnderman = new Vec3d(
                enderman.getX() - mc.player.getX(),
                enderman.getEyeY() - mc.player.getEyeY(),
                enderman.getZ() - mc.player.getZ());

        double distance = toEnderman.length();
        toEnderman = toEnderman.normalize();
        double dot = lookVec.dotProduct(toEnderman);
        double strictness = 0.04D;
        return dot > 1.0D - strictness / distance;
    }
}


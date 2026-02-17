package io.client.modules.combat;

import io.client.Category;
import io.client.Module;
import io.client.settings.BooleanSetting;
import io.client.settings.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.MaceItem;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Vec3d;

public class Criticals extends Module {
    private final BooleanSetting maceSmash = new BooleanSetting("MaceSmash", true);
    private final NumberSetting additionalHeight = new NumberSetting("MaceHeight", 0.0f, 0.0f, 10.0f);
    private boolean lastAttackPressed = false;

    public Criticals() {
        super("Criticals", "Always crit", -1, Category.COMBAT);
        addSetting(maceSmash);
        addSetting(additionalHeight);
    }

    @Override
    public void onUpdate() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.player.isSpectator() || mc.player.isCreative() || mc.world == null)
            return;

        boolean attackPressed = mc.options.attackKey.isPressed();
        boolean attackClicked = attackPressed && !lastAttackPressed;
        lastAttackPressed = attackPressed;

        if (!attackClicked || !isTargetingLiving(mc))
            return;

        if (maceSmash.isEnabled()
                && mc.player.getMainHandStack().getItem() instanceof MaceItem
                && !mc.player.isGliding()) {
            sendMaceSpoofPackets(mc, additionalHeight.getValue());
            return;
        }

        if (mc.player.isOnGround()) {
            Vec3d motion = mc.player.getVelocity();
            mc.player.setVelocity(motion.x, 0.1, motion.z);
            mc.player.setOnGround(false);
        }
    }

    @Override
    public void onDisable() {
        lastAttackPressed = false;
    }

    private boolean isTargetingLiving(MinecraftClient mc) {
        return mc.crosshairTarget instanceof EntityHitResult hit && hit.getEntity() instanceof LivingEntity;
    }

    private void sendMaceSpoofPackets(MinecraftClient mc, float extraHeight) {
        if (mc.player == null || mc.player.networkHandler == null)
            return;

        double x = mc.player.getX();
        double y = mc.player.getY();
        double z = mc.player.getZ();

        mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                x, y, z, false, mc.player.horizontalCollision));
        mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                x, y + 1.501 + extraHeight, z, false, mc.player.horizontalCollision));
        mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                x, y, z, false, mc.player.horizontalCollision));
    }
}

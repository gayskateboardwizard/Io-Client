package io.client.modules.movement;

import io.client.modules.templates.Category;
import io.client.modules.templates.Module;
import io.client.settings.RadioSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.fluid.Fluids;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;

public class NoFall extends Module {
    public final RadioSetting mode = new RadioSetting("Mode", "Packet");

    private double predictY = 0;

    public NoFall() {
        super("NoFall", "Prevents fall damage", -1, Category.MOVEMENT);
        mode.addOption("Packet");
        mode.addOption("Grim");
        mode.addOption("Latency");
        mode.addOption("Spoof");
        addSetting(mode);
    }

    @Override
    public void onUpdate() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;

        if (!isFalling(mc)) return;

        switch (mode.getSelectedOption()) {
            case "Packet":

                mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(true, true));
                mc.player.fallDistance = 0.0f;
                break;

            case "Grim":

                mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.Full(
                        mc.player.getX(),
                        mc.player.getY() + 1.0e-9,
                        mc.player.getZ(),
                        mc.player.getYaw(),
                        mc.player.getPitch(),
                        true,
                        true
                ));
                mc.player.onLanding();
                break;

            case "Latency":

                if (mc.world.getRegistryKey() == World.NETHER) {
                    mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                            mc.player.getX(), 0, mc.player.getZ(), true, true
                    ));
                } else {
                    mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                            0, 64, 0, true, true
                    ));
                }
                mc.player.fallDistance = 0.0f;
                break;

            case "Spoof":

                if (predict(mc) && mc.player.fallDistance >= 3) {
                    mc.player.setVelocity(
                            mc.player.getVelocity().x,
                            0,
                            mc.player.getVelocity().z
                    );
                    mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                            mc.player.getX(),
                            predictY,
                            mc.player.getZ(),
                            true,
                            true
                    ));
                    mc.player.fallDistance = 0.0f;
                }
                break;
        }
    }

    private boolean isFalling(MinecraftClient mc) {
        if (mc.player.fallDistance <= 3.0f) return false;
        if (isAboveWater(mc) || isInWater(mc)) return false;
        return true;
    }

    private boolean predict(MinecraftClient mc) {
        predictY = getGroundLevel(mc) - 0.1;
        return mc.player.getY() - predictY < 3.0;
    }

    private boolean isAboveWater(MinecraftClient mc) {
        return mc.world.getFluidState(mc.player.getBlockPos().down()).isOf(Fluids.WATER);
    }

    private boolean isInWater(MinecraftClient mc) {
        return mc.player.isTouchingWater();
    }

    private double getGroundLevel(MinecraftClient mc) {
        double y = mc.player.getY();
        while (y > -64) {
            Box box = mc.player.getBoundingBox().offset(0, y - mc.player.getY(), 0);
            if (!mc.world.isSpaceEmpty(mc.player, box)) {
                return y + 1;
            }
            y -= 0.5;
        }
        return -64;
    }
}



package io.client.modules;

import io.client.Category;
import io.client.Module;
import io.client.settings.RadioSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;

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
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        if (!isFalling(mc)) return;

        switch (mode.getSelectedOption()) {
            case "Packet":

                mc.player.connection.send(new ServerboundMovePlayerPacket.StatusOnly(true, true));
                mc.player.fallDistance = 0.0f;
                break;

            case "Grim":

                mc.player.connection.send(new ServerboundMovePlayerPacket.PosRot(
                        mc.player.getX(),
                        mc.player.getY() + 1.0e-9,
                        mc.player.getZ(),
                        mc.player.getYRot(),
                        mc.player.getXRot(),
                        true,
                        true
                ));
                mc.player.resetFallDistance();
                break;

            case "Latency":

                if (mc.level.dimension() == Level.NETHER) {
                    mc.player.connection.send(new ServerboundMovePlayerPacket.Pos(
                            mc.player.getX(), 0, mc.player.getZ(), true, true
                    ));
                } else {
                    mc.player.connection.send(new ServerboundMovePlayerPacket.Pos(
                            0, 64, 0, true, true
                    ));
                }
                mc.player.fallDistance = 0.0f;
                break;

            case "Spoof":

                if (predict(mc) && mc.player.fallDistance >= 3) {
                    mc.player.setDeltaMovement(
                            mc.player.getDeltaMovement().x,
                            0,
                            mc.player.getDeltaMovement().z
                    );
                    mc.player.connection.send(new ServerboundMovePlayerPacket.Pos(
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

    private boolean isFalling(Minecraft mc) {
        if (mc.player.fallDistance <= 3.0f) return false;
        if (isAboveWater(mc) || isInWater(mc)) return false;
        return true;
    }

    private boolean predict(Minecraft mc) {
        predictY = getGroundLevel(mc) - 0.1;
        return mc.player.getY() - predictY < 3.0;
    }

    private boolean isAboveWater(Minecraft mc) {
        return mc.level.getFluidState(mc.player.blockPosition().below()).is(Fluids.WATER);
    }

    private boolean isInWater(Minecraft mc) {
        return mc.player.isInWater();
    }

    private double getGroundLevel(Minecraft mc) {
        double y = mc.player.getY();
        while (y > -64) {
            AABB box = mc.player.getBoundingBox().move(0, y - mc.player.getY(), 0);
            if (!mc.level.noCollision(mc.player, box)) {
                return y + 1;
            }
            y -= 0.5;
        }
        return -64;
    }
}
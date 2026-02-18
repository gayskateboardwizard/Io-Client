package io.client.modules.world;

import io.client.modules.templates.Category;
import io.client.modules.templates.Module;
import io.client.settings.BooleanSetting;
import io.client.settings.NumberSetting;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.BlockItem;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class Scaffold extends Module {
    public final BooleanSetting rotate = new BooleanSetting("Rotate", true);
    public final BooleanSetting lockY = new BooleanSetting("LockY", false);
    public final BooleanSetting tower = new BooleanSetting("Tower", true);
    public final NumberSetting towerSpeed = new NumberSetting("TowerSpeed", 0.42F, 0.3F, 0.5F);

    private int lockYLevel = -999;
    private long lastTowerTime = 0;

    public Scaffold() {
        super("Scaffold", "Auto place blocks under you", -1, Category.WORLD);
        addSetting(rotate);
        addSetting(lockY);
        addSetting(tower);
        addSetting(towerSpeed);
    }

    @Override
    public void onEnable() {
        lockYLevel = -999;
        lastTowerTime = 0;
    }

    @Override
    public void onUpdate() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null || mc.interactionManager == null) return;


        if (!(mc.player.getMainHandStack().getItem() instanceof BlockItem)) return;


        BlockPos targetPos;
        if (lockY.isEnabled() && lockYLevel != -999) {
            targetPos = new BlockPos(
                    (int) Math.floor(mc.player.getX()),
                    lockYLevel,
                    (int) Math.floor(mc.player.getZ())
            );
        } else {
            targetPos = new BlockPos(
                    (int) Math.floor(mc.player.getX()),
                    (int) Math.floor(mc.player.getY() - 1),
                    (int) Math.floor(mc.player.getZ())
            );
        }


        if (mc.options.jumpKey.isPressed() && !isMoving()) {
            lockYLevel = (int) Math.floor(mc.player.getY() - 1);
        }


        BlockState targetState = mc.world.getBlockState(targetPos);
        if (!targetState.isAir() && !targetState.isReplaceable()) return;


        BlockPosWithFacing placeInfo = findPlaceablePosition(mc, targetPos);
        if (placeInfo == null) return;


        if (rotate.isEnabled()) {
            Vec3d hitVec = Vec3d.ofCenter(placeInfo.pos).add(Vec3d.of(placeInfo.facing.getVector()).multiply(0.5));
            float[] angles = calculateAngles(mc.player.getEyePos(), hitVec);
            mc.player.setYaw(angles[0]);
            mc.player.setPitch(angles[1]);
        }


        if (tower.isEnabled() && mc.options.jumpKey.isPressed() && !isMoving() && mc.player.isOnGround()) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastTowerTime > 1500) {
                mc.player.setVelocity(mc.player.getVelocity().x, towerSpeed.getValue(), mc.player.getVelocity().z);
                lastTowerTime = currentTime;
            }
        }


        Vec3d hitVec = Vec3d.ofCenter(placeInfo.pos).add(Vec3d.of(placeInfo.facing.getVector()).multiply(0.5));
        BlockHitResult hitResult = new BlockHitResult(hitVec, placeInfo.facing, placeInfo.pos, false);

        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hitResult);
        mc.player.swingHand(Hand.MAIN_HAND);


        lockYLevel = targetPos.getY();
    }

    private BlockPosWithFacing findPlaceablePosition(MinecraftClient mc, BlockPos targetPos) {

        Direction[] directions = {Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.UP};

        for (Direction dir : directions) {
            BlockPos neighborPos = targetPos.offset(dir);
            BlockState neighborState = mc.world.getBlockState(neighborPos);


            if (!neighborState.isAir() && !neighborState.isReplaceable()) {
                return new BlockPosWithFacing(neighborPos, dir.getOpposite());
            }
        }


        for (int x = -2; x <= 2; x++) {
            for (int y = -1; y <= 0; y++) {
                for (int z = -2; z <= 2; z++) {
                    if (x == 0 && y == 0 && z == 0) continue;

                    BlockPos extendedPos = targetPos.add(x, y, z);
                    BlockState extendedState = mc.world.getBlockState(extendedPos);

                    if (!extendedState.isAir() && !extendedState.isReplaceable()) {

                        for (Direction dir : directions) {
                            BlockPos checkPos = extendedPos.offset(dir);
                            BlockState checkState = mc.world.getBlockState(checkPos);
                            if (checkState.isAir() || checkState.isReplaceable()) {
                                return new BlockPosWithFacing(extendedPos, dir.getOpposite());
                            }
                        }
                    }
                }
            }
        }

        return null;
    }

    private float[] calculateAngles(Vec3d from, Vec3d to) {
        double diffX = to.x - from.x;
        double diffY = to.y - from.y;
        double diffZ = to.z - from.z;

        double diffXZ = Math.sqrt(diffX * diffX + diffZ * diffZ);

        float yaw = (float) Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0F;
        float pitch = (float) -Math.toDegrees(Math.atan2(diffY, diffXZ));

        return new float[]{yaw, pitch};
    }

    private boolean isMoving() {
        MinecraftClient mc = MinecraftClient.getInstance();
        return mc.options.forwardKey.isPressed() || mc.options.backKey.isPressed() || mc.options.leftKey.isPressed() || mc.options.rightKey.isPressed();
    }

    private static class BlockPosWithFacing {
        public final BlockPos pos;
        public final Direction facing;

        public BlockPosWithFacing(BlockPos pos, Direction facing) {
            this.pos = pos;
            this.facing = facing;
        }
    }
}



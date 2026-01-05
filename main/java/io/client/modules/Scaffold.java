package io.client.modules;

import io.client.Category;
import io.client.Module;
import io.client.settings.BooleanSetting;
import io.client.settings.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

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
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.gameMode == null) return;


        if (!(mc.player.getMainHandItem().getItem() instanceof BlockItem)) return;


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


        if (mc.options.keyJump.isDown() && !isMoving()) {
            lockYLevel = (int) Math.floor(mc.player.getY() - 1);
        }


        BlockState targetState = mc.level.getBlockState(targetPos);
        if (!targetState.isAir() && !targetState.canBeReplaced()) return;


        BlockPosWithFacing placeInfo = findPlaceablePosition(mc, targetPos);
        if (placeInfo == null) return;


        if (rotate.isEnabled()) {
            Vec3 hitVec = Vec3.atCenterOf(placeInfo.pos).add(Vec3.atLowerCornerOf(placeInfo.facing.getUnitVec3i()).scale(0.5));
            float[] angles = calculateAngles(mc.player.getEyePosition(), hitVec);
            mc.player.setYRot(angles[0]);
            mc.player.setXRot(angles[1]);
        }


        if (tower.isEnabled() && mc.options.keyJump.isDown() && !isMoving() && mc.player.onGround()) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastTowerTime > 1500) {
                mc.player.setDeltaMovement(mc.player.getDeltaMovement().x, towerSpeed.getValue(), mc.player.getDeltaMovement().z);
                lastTowerTime = currentTime;
            }
        }


        Vec3 hitVec = Vec3.atCenterOf(placeInfo.pos).add(Vec3.atLowerCornerOf(placeInfo.facing.getUnitVec3i()).scale(0.5));
        BlockHitResult hitResult = new BlockHitResult(hitVec, placeInfo.facing, placeInfo.pos, false);

        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hitResult);
        mc.player.swing(InteractionHand.MAIN_HAND);


        lockYLevel = targetPos.getY();
    }

    private BlockPosWithFacing findPlaceablePosition(Minecraft mc, BlockPos targetPos) {

        Direction[] directions = {Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.UP};

        for (Direction dir : directions) {
            BlockPos neighborPos = targetPos.relative(dir);
            BlockState neighborState = mc.level.getBlockState(neighborPos);


            if (!neighborState.isAir() && !neighborState.canBeReplaced()) {
                return new BlockPosWithFacing(neighborPos, dir.getOpposite());
            }
        }


        for (int x = -2; x <= 2; x++) {
            for (int y = -1; y <= 0; y++) {
                for (int z = -2; z <= 2; z++) {
                    if (x == 0 && y == 0 && z == 0) continue;

                    BlockPos extendedPos = targetPos.offset(x, y, z);
                    BlockState extendedState = mc.level.getBlockState(extendedPos);

                    if (!extendedState.isAir() && !extendedState.canBeReplaced()) {

                        for (Direction dir : directions) {
                            BlockPos checkPos = extendedPos.relative(dir);
                            BlockState checkState = mc.level.getBlockState(checkPos);
                            if (checkState.isAir() || checkState.canBeReplaced()) {
                                return new BlockPosWithFacing(extendedPos, dir.getOpposite());
                            }
                        }
                    }
                }
            }
        }

        return null;
    }

    private float[] calculateAngles(Vec3 from, Vec3 to) {
        double diffX = to.x - from.x;
        double diffY = to.y - from.y;
        double diffZ = to.z - from.z;

        double diffXZ = Math.sqrt(diffX * diffX + diffZ * diffZ);

        float yaw = (float) Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0F;
        float pitch = (float) -Math.toDegrees(Math.atan2(diffY, diffXZ));

        return new float[]{yaw, pitch};
    }

    private boolean isMoving() {
        Minecraft mc = Minecraft.getInstance();
        return mc.options.keyUp.isDown() || mc.options.keyDown.isDown() || mc.options.keyLeft.isDown() || mc.options.keyRight.isDown();
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
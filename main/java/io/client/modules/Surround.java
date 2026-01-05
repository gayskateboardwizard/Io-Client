package io.client.modules;

import io.client.Category;
import io.client.Module;
import io.client.settings.BooleanSetting;
import io.client.settings.NumberSetting;
import io.client.settings.RadioSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public class Surround extends Module {
    private static final Vec3i[] SURROUND_PATTERN = {
            new Vec3i(1, 0, 0),
            new Vec3i(-1, 0, 0),
            new Vec3i(0, 0, 1),
            new Vec3i(0, 0, -1)
    };
    public final NumberSetting blocksPerTick = new NumberSetting("PerTick", 8.0F, 1.0F, 12.0F);
    public final NumberSetting placeDelay = new NumberSetting("PlaceDelay", 3.0F, 0.0F, 10.0F);
    public final RadioSetting centerMode = new RadioSetting("Center", "Disabled");
    public final BooleanSetting onYChange = new BooleanSetting("OnYChange", true);
    public final BooleanSetting onDeath = new BooleanSetting("OnDeath", false);
    public final BooleanSetting rotate = new BooleanSetting("Rotate", true);
    private int delay = 0;
    private double prevY = 0;
    private int previousSlot = -1;
    private boolean hasSwitched = false;

    public Surround() {
        super("Surround", "Surrounds you with blocks", 0, Category.COMBAT);
        centerMode.addOption("Disabled");
        centerMode.addOption("Teleport");
        centerMode.addOption("Motion");
        addSetting(blocksPerTick);
        addSetting(placeDelay);
        addSetting(centerMode);
        addSetting(onYChange);
        addSetting(onDeath);
        addSetting(rotate);
    }

    @Override
    public void onEnable() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        delay = 0;
        prevY = mc.player.getY();


        if (centerMode.getSelectedOption().equals("Teleport")) {
            double centerX = Math.floor(mc.player.getX()) + 0.5;
            double centerZ = Math.floor(mc.player.getZ()) + 0.5;
            mc.player.setPos(centerX, mc.player.getY(), centerZ);
        }
    }

    @Override
    public void onUpdate() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.gameMode == null) return;


        if (onDeath.isEnabled() && (mc.player.isDeadOrDying() || mc.player.getHealth() <= 0)) {
            toggle();
            return;
        }


        if (onYChange.isEnabled() && prevY != mc.player.getY()) {
            toggle();
            return;
        }

        prevY = mc.player.getY();


        if (centerMode.getSelectedOption().equals("Motion")) {
            Vec3 centerVec = new Vec3(
                    Math.floor(mc.player.getX()) + 0.5,
                    mc.player.getY(),
                    Math.floor(mc.player.getZ()) + 0.5
            );

            AABB centerBox = new AABB(
                    centerVec.x - 0.2, centerVec.y - 0.1, centerVec.z - 0.2,
                    centerVec.x + 0.2, centerVec.y + 0.1, centerVec.z + 0.2
            );

            if (!centerBox.contains(mc.player.position())) {
                Vec3 motion = new Vec3(
                        (centerVec.x - mc.player.getX()) / 2,
                        0,
                        (centerVec.z - mc.player.getZ()) / 2
                );
                mc.player.setDeltaMovement(mc.player.getDeltaMovement().add(motion));
                return;
            }
        }


        int obsidianSlot = findObsidian(mc);
        if (obsidianSlot == -1) {
            switchBack(mc);
            return;
        }


        int currentSlot = mc.player.getInventory().getSelectedSlot();
        if (obsidianSlot != currentSlot) {
            if (!hasSwitched) {
                previousSlot = currentSlot;
                hasSwitched = true;
            }
            mc.player.getInventory().setSelectedSlot(obsidianSlot);
        }


        List<BlockPos> blocks = getBlocks(mc);
        if (blocks.isEmpty()) {
            switchBack(mc);
            return;
        }


        if (delay > 0) {
            delay--;
            return;
        }


        int placed = 0;
        while (placed < blocksPerTick.getValue() && !blocks.isEmpty()) {
            BlockPos targetBlock = getNextPosition(mc, blocks);
            if (targetBlock == null) break;

            if (placeBlock(mc, targetBlock)) {
                placed++;
                delay = (int) placeDelay.getValue();
            } else {
                break;
            }
        }
    }

    @Override
    public void onDisable() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            switchBack(mc);
        }
    }

    private int findObsidian(Minecraft mc) {
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = mc.player.getInventory().getItem(slot);
            if (stack.getItem() == Items.OBSIDIAN) {
                return slot;
            }
        }
        return -1;
    }

    private void switchBack(Minecraft mc) {
        if (hasSwitched && previousSlot != -1) {
            mc.player.getInventory().setSelectedSlot(previousSlot);
            previousSlot = -1;
            hasSwitched = false;
        }
    }

    private List<BlockPos> getBlocks(Minecraft mc) {
        BlockPos playerPos = getPlayerPos(mc);
        List<BlockPos> offsets = new ArrayList<>();

        if (centerMode.getSelectedOption().equals("Disabled")) {

            double decimalX = Math.abs(mc.player.getX()) - Math.floor(Math.abs(mc.player.getX()));
            double decimalZ = Math.abs(mc.player.getZ()) - Math.floor(Math.abs(mc.player.getZ()));

            int lengthXPos = calcLength(decimalX, false);
            int lengthXNeg = calcLength(decimalX, true);
            int lengthZPos = calcLength(decimalZ, false);
            int lengthZNeg = calcLength(decimalZ, true);

            List<BlockPos> tempOffsets = new ArrayList<>();
            offsets.addAll(getOverlapPos(mc));


            for (int x = 1; x < lengthXPos + 1; x++) {
                tempOffsets.add(playerPos.offset(x, 0, 1 + lengthZPos));
                tempOffsets.add(playerPos.offset(x, 0, -(1 + lengthZNeg)));
            }
            for (int x = 0; x <= lengthXNeg; x++) {
                tempOffsets.add(playerPos.offset(-x, 0, 1 + lengthZPos));
                tempOffsets.add(playerPos.offset(-x, 0, -(1 + lengthZNeg)));
            }
            for (int z = 1; z < lengthZPos + 1; z++) {
                tempOffsets.add(playerPos.offset(1 + lengthXPos, 0, z));
                tempOffsets.add(playerPos.offset(-(1 + lengthXNeg), 0, z));
            }
            for (int z = 0; z <= lengthZNeg; z++) {
                tempOffsets.add(playerPos.offset(1 + lengthXPos, 0, -z));
                tempOffsets.add(playerPos.offset(-(1 + lengthXNeg), 0, -z));
            }

            for (BlockPos pos : tempOffsets) {
                if (needsDown(mc, pos)) {
                    offsets.add(pos.below());
                }
                offsets.add(pos);
            }
        } else {

            offsets.add(playerPos.below());

            for (Vec3i pattern : SURROUND_PATTERN) {
                BlockPos surroundPos = playerPos.offset(pattern);
                if (needsDown(mc, surroundPos)) {
                    offsets.add(surroundPos.below());
                }
                offsets.add(surroundPos);
            }
        }

        return offsets;
    }

    private BlockPos getNextPosition(Minecraft mc, List<BlockPos> blocks) {
        for (BlockPos pos : blocks) {
            AABB box = new AABB(pos);
            if (box.intersects(mc.player.getBoundingBox())) continue;

            BlockState state = mc.level.getBlockState(pos);
            if (state.isAir() || state.canBeReplaced()) {
                return pos;
            }
        }
        return null;
    }

    private boolean placeBlock(Minecraft mc, BlockPos pos) {

        for (Direction dir : Direction.values()) {
            BlockPos neighbor = pos.relative(dir);
            BlockState neighborState = mc.level.getBlockState(neighbor);

            if (!neighborState.isAir() && !neighborState.canBeReplaced()) {
                Vec3 hitVec = Vec3.atCenterOf(neighbor).add(Vec3.atLowerCornerOf(dir.getOpposite().getUnitVec3i()).scale(0.5));
                BlockHitResult hitResult = new BlockHitResult(hitVec, dir.getOpposite(), neighbor, false);


                if (rotate.isEnabled()) {
                    float[] angles = calculateAngles(mc.player.getEyePosition(), hitVec);
                    mc.player.setYRot(angles[0]);
                    mc.player.setXRot(angles[1]);
                }

                mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hitResult);
                mc.player.swing(InteractionHand.MAIN_HAND);
                return true;
            }
        }
        return false;
    }

    private boolean needsDown(Minecraft mc, BlockPos pos) {
        for (Direction dir : Direction.values()) {
            if (!mc.level.getBlockState(pos.relative(dir)).canBeReplaced()) {
                return false;
            }
        }
        return mc.level.getBlockState(pos).canBeReplaced();
    }

    private List<BlockPos> getOverlapPos(Minecraft mc) {
        List<BlockPos> positions = new ArrayList<>();

        double decimalX = mc.player.getX() - Math.floor(mc.player.getX());
        double decimalZ = mc.player.getZ() - Math.floor(mc.player.getZ());
        int offX = calcOffset(decimalX);
        int offZ = calcOffset(decimalZ);

        BlockPos playerPos = getPlayerPos(mc);
        positions.add(playerPos);

        for (int x = 0; x <= Math.abs(offX); x++) {
            for (int z = 0; z <= Math.abs(offZ); z++) {
                int properX = x * offX;
                int properZ = z * offZ;
                positions.add(playerPos.offset(properX, -1, properZ));
            }
        }

        return positions;
    }

    private BlockPos getPlayerPos(Minecraft mc) {
        double y = mc.player.getY() - Math.floor(mc.player.getY()) > 0.8
                ? Math.floor(mc.player.getY()) + 1.0
                : Math.floor(mc.player.getY());
        return BlockPos.containing(mc.player.getX(), y, mc.player.getZ());
    }

    private int calcLength(double decimal, boolean negative) {
        if (negative) {
            return decimal >= 0.3 ? 1 : 0;
        }
        return decimal <= 0.7 ? 0 : 1;
    }

    private int calcOffset(double decimal) {
        if (decimal >= 0.7) return 1;
        if (decimal <= 0.3) return -1;
        return 0;
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
}
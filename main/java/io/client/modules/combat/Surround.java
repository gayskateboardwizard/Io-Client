package io.client.modules.combat;

import io.client.modules.templates.Category;
import io.client.modules.templates.Module;
import io.client.settings.BooleanSetting;
import io.client.settings.NumberSetting;
import io.client.settings.RadioSetting;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;

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
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        delay = 0;
        prevY = mc.player.getY();


        if (centerMode.getSelectedOption().equals("Teleport")) {
            double centerX = Math.floor(mc.player.getX()) + 0.5;
            double centerZ = Math.floor(mc.player.getZ()) + 0.5;
            mc.player.setPosition(centerX, mc.player.getY(), centerZ);
        }
    }

    @Override
    public void onUpdate() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null || mc.interactionManager == null) return;


        if (onDeath.isEnabled() && (mc.player.isDead() || mc.player.getHealth() <= 0)) {
            toggle();
            return;
        }


        if (onYChange.isEnabled() && prevY != mc.player.getY()) {
            toggle();
            return;
        }

        prevY = mc.player.getY();


        if (centerMode.getSelectedOption().equals("Motion")) {
            Vec3d centerVec = new Vec3d(
                    Math.floor(mc.player.getX()) + 0.5,
                    mc.player.getY(),
                    Math.floor(mc.player.getZ()) + 0.5
            );

            Box centerBox = new Box(
                    centerVec.x - 0.2, centerVec.y - 0.1, centerVec.z - 0.2,
                    centerVec.x + 0.2, centerVec.y + 0.1, centerVec.z + 0.2
            );

            if (!centerBox.contains(mc.player.getPos())) {
                Vec3d motion = new Vec3d(
                        (centerVec.x - mc.player.getX()) / 2,
                        0,
                        (centerVec.z - mc.player.getZ()) / 2
                );
                mc.player.setVelocity(mc.player.getVelocity().add(motion));
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
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) {
            switchBack(mc);
        }
    }

    private int findObsidian(MinecraftClient mc) {
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = mc.player.getInventory().getStack(slot);
            if (stack.getItem() == Items.OBSIDIAN) {
                return slot;
            }
        }
        return -1;
    }

    private void switchBack(MinecraftClient mc) {
        if (hasSwitched && previousSlot != -1) {
            mc.player.getInventory().setSelectedSlot(previousSlot);
            previousSlot = -1;
            hasSwitched = false;
        }
    }

    private List<BlockPos> getBlocks(MinecraftClient mc) {
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
                tempOffsets.add(playerPos.add(x, 0, 1 + lengthZPos));
                tempOffsets.add(playerPos.add(x, 0, -(1 + lengthZNeg)));
            }
            for (int x = 0; x <= lengthXNeg; x++) {
                tempOffsets.add(playerPos.add(-x, 0, 1 + lengthZPos));
                tempOffsets.add(playerPos.add(-x, 0, -(1 + lengthZNeg)));
            }
            for (int z = 1; z < lengthZPos + 1; z++) {
                tempOffsets.add(playerPos.add(1 + lengthXPos, 0, z));
                tempOffsets.add(playerPos.add(-(1 + lengthXNeg), 0, z));
            }
            for (int z = 0; z <= lengthZNeg; z++) {
                tempOffsets.add(playerPos.add(1 + lengthXPos, 0, -z));
                tempOffsets.add(playerPos.add(-(1 + lengthXNeg), 0, -z));
            }

            for (BlockPos pos : tempOffsets) {
                if (needsDown(mc, pos)) {
                    offsets.add(pos.down());
                }
                offsets.add(pos);
            }
        } else {

            offsets.add(playerPos.down());

            for (Vec3i pattern : SURROUND_PATTERN) {
                BlockPos surroundPos = playerPos.add(pattern);
                if (needsDown(mc, surroundPos)) {
                    offsets.add(surroundPos.down());
                }
                offsets.add(surroundPos);
            }
        }

        return offsets;
    }

    private BlockPos getNextPosition(MinecraftClient mc, List<BlockPos> blocks) {
        for (BlockPos pos : blocks) {
            Box box = new Box(pos);
            if (box.intersects(mc.player.getBoundingBox())) continue;

            BlockState state = mc.world.getBlockState(pos);
            if (state.isAir() || state.isReplaceable()) {
                return pos;
            }
        }
        return null;
    }

    private boolean placeBlock(MinecraftClient mc, BlockPos pos) {

        for (Direction dir : Direction.values()) {
            BlockPos neighbor = pos.offset(dir);
            BlockState neighborState = mc.world.getBlockState(neighbor);

            if (!neighborState.isAir() && !neighborState.isReplaceable()) {
                Vec3d hitVec = Vec3d.ofCenter(neighbor).add(Vec3d.of(dir.getOpposite().getVector()).multiply(0.5));
                BlockHitResult hitResult = new BlockHitResult(hitVec, dir.getOpposite(), neighbor, false);


                if (rotate.isEnabled()) {
                    float[] angles = calculateAngles(mc.player.getEyePos(), hitVec);
                    mc.player.setYaw(angles[0]);
                    mc.player.setPitch(angles[1]);
                }

                mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hitResult);
                mc.player.swingHand(Hand.MAIN_HAND);
                return true;
            }
        }
        return false;
    }

    private boolean needsDown(MinecraftClient mc, BlockPos pos) {
        for (Direction dir : Direction.values()) {
            if (!mc.world.getBlockState(pos.offset(dir)).isReplaceable()) {
                return false;
            }
        }
        return mc.world.getBlockState(pos).isReplaceable();
    }

    private List<BlockPos> getOverlapPos(MinecraftClient mc) {
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
                positions.add(playerPos.add(properX, -1, properZ));
            }
        }

        return positions;
    }

    private BlockPos getPlayerPos(MinecraftClient mc) {
        double y = mc.player.getY() - Math.floor(mc.player.getY()) > 0.8
                ? Math.floor(mc.player.getY()) + 1.0
                : Math.floor(mc.player.getY());
        return BlockPos.ofFloored(mc.player.getX(), y, mc.player.getZ());
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

    private float[] calculateAngles(Vec3d from, Vec3d to) {
        double diffX = to.x - from.x;
        double diffY = to.y - from.y;
        double diffZ = to.z - from.z;

        double diffXZ = Math.sqrt(diffX * diffX + diffZ * diffZ);

        float yaw = (float) Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0F;
        float pitch = (float) -Math.toDegrees(Math.atan2(diffY, diffXZ));

        return new float[]{yaw, pitch};
    }
}



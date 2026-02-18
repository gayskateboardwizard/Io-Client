package io.client.modules.combat;

import io.client.managers.TargetManager;
import io.client.modules.templates.Category;
import io.client.modules.templates.Module;
import io.client.settings.BooleanSetting;
import io.client.settings.NumberSetting;
import io.client.settings.RadioSetting;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.hit.BlockHitResult;

public class HoleFiller extends Module {
    private final NumberSetting range = new NumberSetting("Range", 4.5f, 0.0f, 8.0f);
    private final NumberSetting delay = new NumberSetting("Delay", 2.0f, 0.0f, 20.0f);
    private final BooleanSetting rotate = new BooleanSetting("Rotate", true);
    private final RadioSetting switchMode = new RadioSetting("Switch", "Normal");
    private final BooleanSetting noGapSwitch = new BooleanSetting("NoGapSwitch", true);
    private final BooleanSetting noBowSwitch = new BooleanSetting("NoBowSwitch", true);

    private int timer = 0;
    private int previousSlot = -1;
    private boolean hasSwitched = false;

    public HoleFiller() {
        super("HoleFiller", "Fills nearby valid holes with obsidian", -1, Category.COMBAT);
        switchMode.addOption("None");
        switchMode.addOption("Normal");

        addSetting(range);
        addSetting(delay);
        addSetting(rotate);
        addSetting(switchMode);
        addSetting(noGapSwitch);
        addSetting(noBowSwitch);
    }

    @Override
    public void onEnable() {
        timer = 0;
    }

    @Override
    public void onDisable() {
        switchBack(MinecraftClient.getInstance());
    }

    @Override
    public void onUpdate() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null || mc.interactionManager == null) return;

        timer = Math.max(0, timer - 1);
        if (timer > 0) return;

        LivingEntity target = findTarget(mc);
        if (target == null) {
            switchBack(mc);
            return;
        }

        BlockPos hole = findBestHolePos(mc, target);
        if (hole == null) return;

        if (placeHole(mc, hole)) {
            timer = (int) delay.getValue();
        }
    }

    private LivingEntity findTarget(MinecraftClient mc) {
        LivingEntity best = null;
        double bestDistSq = Double.MAX_VALUE;
        double maxRangeSq = sq(range.getValue() + 4.0f);

        for (Entity e : mc.world.getEntities()) {
            if (e == mc.player) continue;
            if (!(e instanceof LivingEntity living) || !living.isAlive()) continue;
            if (!TargetManager.INSTANCE.isValidTarget(e)) continue;

            double distSq = mc.player.squaredDistanceTo(e);
            if (distSq > maxRangeSq) continue;
            if (distSq < bestDistSq) {
                bestDistSq = distSq;
                best = living;
            }
        }
        return best;
    }

    private BlockPos findBestHolePos(MinecraftClient mc, LivingEntity target) {
        BlockPos best = null;
        double bestDistSq = Double.MAX_VALUE;
        BlockPos center = target.getBlockPos();
        int horizontal = (int) Math.ceil(range.getValue());
        int vertical = 2;
        double maxRangeSq = sq(range.getValue());

        for (int x = -horizontal; x <= horizontal; x++) {
            for (int y = -vertical; y <= vertical; y++) {
                for (int z = -horizontal; z <= horizontal; z++) {
                    BlockPos pos = center.add(x, y, z);
                    if (mc.player.getBlockPos().getSquaredDistance(pos) > maxRangeSq) continue;
                    if (!isValidHole(mc, pos)) continue;

                    double distSq = target.getBlockPos().getSquaredDistance(pos);
                    if (distSq < bestDistSq) {
                        bestDistSq = distSq;
                        best = pos;
                    }
                }
            }
        }
        return best;
    }

    private boolean placeHole(MinecraftClient mc, BlockPos pos) {
        boolean offhand = mc.player.getOffHandStack().isOf(Items.OBSIDIAN);
        if (!offhand && !mc.player.getMainHandStack().isOf(Items.OBSIDIAN)) {
            if ("None".equals(switchMode.getSelectedOption())) return false;
            if (shouldPauseForItem(mc)) return false;

            int slot = findObsidianSlot(mc);
            if (slot == -1) return false;
            if (!hasSwitched) {
                previousSlot = mc.player.getInventory().getSelectedSlot();
                hasSwitched = true;
            }
            mc.player.getInventory().setSelectedSlot(slot);
        }

        if (rotate.isEnabled()) applyRotation(mc, Vec3d.ofCenter(pos));

        Hand hand = offhand ? Hand.OFF_HAND : Hand.MAIN_HAND;
        Vec3d hitVec = Vec3d.ofCenter(pos.down()).add(0, 1, 0);
        BlockHitResult hit = new BlockHitResult(hitVec, Direction.UP, pos.down(), false);
        mc.interactionManager.interactBlock(mc.player, hand, hit);
        mc.player.swingHand(hand);
        return true;
    }

    private boolean isValidHole(MinecraftClient mc, BlockPos pos) {
        if (!mc.world.getBlockState(pos).isReplaceable()) return false;
        if (!mc.world.getBlockState(pos.up()).isAir()) return false;
        if (!mc.world.getBlockState(pos.up().up()).isAir()) return false;

        Box box = new Box(pos);
        for (Entity e : mc.world.getOtherEntities(null, box)) {
            if (e instanceof ItemEntity) continue;
            return false;
        }

        return isBlastResistantBase(mc, pos.down())
                && isBlastResistantBase(mc, pos.north())
                && isBlastResistantBase(mc, pos.south())
                && isBlastResistantBase(mc, pos.east())
                && isBlastResistantBase(mc, pos.west());
    }

    private boolean isBlastResistantBase(MinecraftClient mc, BlockPos pos) {
        return mc.world.getBlockState(pos).isOf(Blocks.OBSIDIAN)
                || mc.world.getBlockState(pos).isOf(Blocks.BEDROCK);
    }

    private int findObsidianSlot(MinecraftClient mc) {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).isOf(Items.OBSIDIAN)) return i;
        }
        return -1;
    }

    private void switchBack(MinecraftClient mc) {
        if (mc.player == null) return;
        if (hasSwitched && previousSlot != -1) {
            mc.player.getInventory().setSelectedSlot(previousSlot);
            previousSlot = -1;
            hasSwitched = false;
        }
    }

    private boolean shouldPauseForItem(MinecraftClient mc) {
        if (!mc.player.isUsingItem()) return false;
        Item item = mc.player.getActiveItem().getItem();
        if (noGapSwitch.isEnabled() && (item == Items.GOLDEN_APPLE || item == Items.ENCHANTED_GOLDEN_APPLE)) {
            return true;
        }
        return noBowSwitch.isEnabled() && item instanceof BowItem;
    }

    private void applyRotation(MinecraftClient mc, Vec3d to) {
        Vec3d from = mc.player.getEyePos();
        double dx = to.x - from.x;
        double dy = to.y - from.y;
        double dz = to.z - from.z;
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0f;
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, horizontal));
        mc.player.setYaw(yaw);
        mc.player.setPitch(pitch);
    }

    private static double sq(double v) {
        return v * v;
    }
}




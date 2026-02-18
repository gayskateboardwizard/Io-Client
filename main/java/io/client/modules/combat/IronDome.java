package io.client.modules.combat;

import io.client.managers.TargetManager;
import io.client.modules.templates.Category;
import io.client.modules.templates.Module;
import io.client.managers.PacketManager;
import io.client.settings.BooleanSetting;
import io.client.settings.NumberSetting;
import io.client.settings.RadioSetting;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class IronDome extends Module {
    private final NumberSetting range;
    private final NumberSetting heightCheck;
    private final NumberSetting delay;
    private final NumberSetting prediction;
    private final BooleanSetting onlyPlayers;
    private final BooleanSetting renderSwing;
    private final BooleanSetting slowFallingArrows;
    private final BooleanSetting autoSwitch;
    private final NumberSetting arrowDelay;
    private final RadioSetting placeBlock;
    private final BooleanSetting smartPlacement;
    private final NumberSetting maxBlocksPerCycle;
    private final NumberSetting keepBlocksInInventory;
    private final BooleanSetting includeDiagonals;

    private int tickCounter;
    private int arrowTickCounter;
    private int previousSlot = -1;
    private boolean hasSwitched = false;

    public IronDome() {
        super("IronDome", "Uses Isreali Defense Forces Against Maces", -1, Category.COMBAT);

        range = new NumberSetting("Range", 10f, 5f, 20f);
        heightCheck = new NumberSetting("HeightCheck", 5f, 2f, 10f);
        delay = new NumberSetting("Delay", 5f, 0f, 20f);
        prediction = new NumberSetting("Prediction", 3f, 0f, 10f);
        onlyPlayers = new BooleanSetting("OnlyPlayers", true);
        renderSwing = new BooleanSetting("RenderSwing", true);
        slowFallingArrows = new BooleanSetting("SlowFalling", false);
        autoSwitch = new BooleanSetting("AutoSwitch", true);
        arrowDelay = new NumberSetting("ArrowDelay", 10f, 0f, 40f);
        placeBlock = new RadioSetting("PlaceBlock", "Webs");
        placeBlock.addOption("Webs");
        placeBlock.addOption("Obsidian");
        smartPlacement = new BooleanSetting("SmartPlacement", true);
        maxBlocksPerCycle = new NumberSetting("MaxBlocksPerCycle", 3f, 1f, 9f);
        keepBlocksInInventory = new NumberSetting("KeepBlocks", 8f, 0f, 64f);
        includeDiagonals = new BooleanSetting("IncludeDiagonals", false);

        addSetting(range);
        addSetting(heightCheck);
        addSetting(delay);
        addSetting(prediction);
        addSetting(onlyPlayers);
        addSetting(renderSwing);
        addSetting(slowFallingArrows);
        addSetting(autoSwitch);
        addSetting(arrowDelay);
        addSetting(placeBlock);
        addSetting(smartPlacement);
        addSetting(maxBlocksPerCycle);
        addSetting(keepBlocksInInventory);
        addSetting(includeDiagonals);

        tickCounter = 0;
        arrowTickCounter = 0;
    }

    @Override
    public void onEnable() {
        tickCounter = 0;
        arrowTickCounter = 0;
        previousSlot = -1;
        hasSwitched = false;
    }

    @Override
    public void onDisable() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null && hasSwitched && previousSlot != -1) {
            mc.player.getInventory().setSelectedSlot(previousSlot);
            previousSlot = -1;
            hasSwitched = false;
        }
    }

    @Override
    public void onUpdate() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null)
            return;

        Entity target = findTargetAbove();
        if (target == null) {
            revertSlot(mc);
            return;
        }

        tickCounter++;
        arrowTickCounter++;

        if (tickCounter >= delay.getValue()) {
            if (slowFallingArrows.isEnabled() && arrowTickCounter >= arrowDelay.getValue()) {
                shootSlowFallingArrow(target);
                arrowTickCounter = 0;
            }

            placeBlocks(target);
            tickCounter = 0;
        }
    }

    private Entity findTargetAbove() {
        MinecraftClient mc = MinecraftClient.getInstance();
        Vec3d playerPos = mc.player.getPos();

        Box searchBox = new Box(
                playerPos.x - range.getValue(),
                playerPos.y,
                playerPos.z - range.getValue(),
                playerPos.x + range.getValue(),
                playerPos.y + heightCheck.getValue(),
                playerPos.z + range.getValue());

        for (Entity e : mc.world.getOtherEntities(mc.player, searchBox)) {
            if (!(e instanceof LivingEntity living))
                continue;
            if (!living.isAlive() || living.isDead())
                continue;

            if (onlyPlayers.isEnabled() && !(e instanceof PlayerEntity))
                continue;

            if (e instanceof PlayerEntity player) {
                if (TargetManager.INSTANCE.isFriend(player.getName().getString()))
                    continue;
            }

            if (!TargetManager.INSTANCE.isValidTarget(e))
                continue;

            if (e.getY() > mc.player.getY() + 2) {
                return e;
            }
        }

        return null;
    }

    private void placeBlocks(Entity target) {
        MinecraftClient mc = MinecraftClient.getInstance();

        net.minecraft.item.Item placeItem = placeBlock.isSelected("Obsidian") ? Items.OBSIDIAN : Items.COBWEB;
        int blockCount = countItem(placeItem);
        int keepCount = (int) keepBlocksInInventory.getValue();
        if (blockCount <= keepCount)
            return;

        int placeSlot = findItemSlot(placeItem);
        if (placeSlot == -1)
            return;

        Vec3d predictedPos = predictPosition(target);
        BlockPos targetPos = BlockPos.ofFloored(predictedPos).up(3);

        if (!switchToSlot(placeSlot))
            return;

        int budget = Math.min((int) maxBlocksPerCycle.getValue(), blockCount - keepCount);
        int placed = 0;
        for (BlockPos pos : getPlacementTargets(targetPos, target.getVelocity())) {
            if (placed >= budget)
                break;
            if (placeBlockAt(pos, placeItem))
                placed++;
        }

        revertSlot(mc);
    }

    private void shootSlowFallingArrow(Entity target) {
        MinecraftClient mc = MinecraftClient.getInstance();

        int bowSlot = findBowSlot();
        if (bowSlot == -1)
            return;

        int arrowSlot = findSlowFallingArrowSlot();
        if (arrowSlot == -1)
            return;

        if (!switchToSlot(bowSlot))
            return;

        Vec3d predictedPos = predictPosition(target);
        float[] angles = calculateAngles(mc.player.getEyePos(), predictedPos);

        mc.player.setPitch(angles[1]);
        mc.player.setYaw(angles[0]);

        PacketManager.INSTANCE.send(new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, 0, angles[0], angles[1]));

        for (int i = 0; i < 3; i++) {
            mc.player.getInventory().setSelectedSlot(bowSlot);
        }

        revertSlot(mc);
    }

    private Vec3d predictPosition(Entity target) {
        if (prediction.getValue() == 0)
            return target.getPos();

        Vec3d velocity = target.getVelocity();
        Vec3d pos = target.getPos();

        return pos.add(velocity.multiply(prediction.getValue()));
    }

    private float[] calculateAngles(Vec3d from, Vec3d to) {
        Vec3d diff = to.subtract(from);
        double dist = Math.sqrt(diff.x * diff.x + diff.z * diff.z);

        float yaw = (float) Math.toDegrees(Math.atan2(diff.z, diff.x)) - 90f;
        float pitch = (float) -Math.toDegrees(Math.atan2(diff.y, dist));

        return new float[] { yaw, pitch };
    }

    private boolean placeBlockAt(BlockPos pos, net.minecraft.item.Item placeItem) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!canPlaceAt(pos, placeItem)) {
            return false;
        }

        if (smartPlacement.isEnabled() && !hasSolidSupport(pos)) {
            return false;
        }

        double dist = mc.player.squaredDistanceTo(Vec3d.ofCenter(pos));
        if (dist > range.getValue() * range.getValue())
            return false;

        BlockPos belowPos = pos.down();
        Vec3d hitVec = new Vec3d(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        BlockHitResult hitResult = new BlockHitResult(hitVec, Direction.UP, belowPos, false);

        PacketManager.INSTANCE.send(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, hitResult, 0));

        if (renderSwing.isEnabled()) {
            mc.player.swingHand(Hand.MAIN_HAND);
        } else {
            PacketManager.INSTANCE.send(new HandSwingC2SPacket(Hand.MAIN_HAND));
        }

        return true;
    }

    private boolean canPlaceAt(BlockPos pos, net.minecraft.item.Item placeItem) {
        MinecraftClient mc = MinecraftClient.getInstance();
        BlockState state = mc.world.getBlockState(pos);

        if ((placeItem == Items.COBWEB && state.getBlock() == Blocks.COBWEB)
                || (placeItem == Items.OBSIDIAN && state.getBlock() == Blocks.OBSIDIAN))
            return false;
        if (!state.isReplaceable())
            return false;

        return true;
    }

    private boolean hasSolidSupport(BlockPos pos) {
        MinecraftClient mc = MinecraftClient.getInstance();
        return !mc.world.getBlockState(pos.down()).isReplaceable();
    }

    private List<BlockPos> getPlacementTargets(BlockPos center, Vec3d velocity) {
        if (!smartPlacement.isEnabled()) {
            List<BlockPos> full = new ArrayList<>();
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    full.add(center.add(x, 0, z));
                }
            }
            return full;
        }

        LinkedHashSet<BlockPos> targets = new LinkedHashSet<>();
        targets.add(center);
        targets.add(center.north());
        targets.add(center.east());
        targets.add(center.south());
        targets.add(center.west());

        int leadX = (int) Math.signum(velocity.x);
        int leadZ = (int) Math.signum(velocity.z);
        if (leadX != 0 || leadZ != 0) {
            targets.add(center.add(leadX, 0, leadZ));
        }

        if (includeDiagonals.isEnabled()) {
            targets.add(center.north().east());
            targets.add(center.north().west());
            targets.add(center.south().east());
            targets.add(center.south().west());
        }

        return new ArrayList<>(targets);
    }

    private int countItem(net.minecraft.item.Item item) {
        MinecraftClient mc = MinecraftClient.getInstance();
        int count = 0;
        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() == item) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private boolean switchToSlot(int slot) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!autoSwitch.isEnabled())
            return false;
        if (slot == -1)
            return false;

        int current = mc.player.getInventory().getSelectedSlot();
        if (slot == current)
            return true;

        if (!hasSwitched) {
            previousSlot = current;
            hasSwitched = true;
        }

        mc.player.getInventory().setSelectedSlot(slot);
        return true;
    }

    private void revertSlot(MinecraftClient mc) {
        if (!autoSwitch.isEnabled())
            return;
        if (!hasSwitched || previousSlot == -1)
            return;

        mc.player.getInventory().setSelectedSlot(previousSlot);
        previousSlot = -1;
        hasSwitched = false;
    }

    private int findItemSlot(net.minecraft.item.Item item) {
        MinecraftClient mc = MinecraftClient.getInstance();
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).getItem() == item) {
                return i;
            }
        }
        return -1;
    }

    private int findBowSlot() {
        MinecraftClient mc = MinecraftClient.getInstance();
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() instanceof BowItem) {
                return i;
            }
        }
        return -1;
    }

    private int findSlowFallingArrowSlot() {
        MinecraftClient mc = MinecraftClient.getInstance();
        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() == Items.TIPPED_ARROW) {
                return i;
            }
        }
        return -1;
    }
}

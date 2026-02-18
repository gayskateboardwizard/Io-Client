package io.client.modules.combat;

import io.client.managers.TargetManager;
import io.client.modules.templates.Category;
import io.client.modules.templates.Module;
import io.client.settings.BooleanSetting;
import io.client.settings.CategorySetting;
import io.client.settings.NumberSetting;
import io.client.settings.RadioSetting;
import io.client.utils.DamageUtils;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

public class CrystalAuraV2 extends Module {
    private final CategorySetting targetCategory = new CategorySetting("Target");
    private final NumberSetting targetRange = new NumberSetting("TargetRange", 10.0f, 1.0f, 20.0f);
    private final RadioSetting targetLogic = new RadioSetting("TargetLogic", "Distance");
    private final BooleanSetting zombieTesting = new BooleanSetting("ZombieTesting", true);

    private final CategorySetting damageCategory = new CategorySetting("Damage");
    private final NumberSetting minDamage = new NumberSetting("MinDamage", 6.0f, 0.0f, 36.0f);
    private final NumberSetting maxSelfDamage = new NumberSetting("MaxSelfDamage", 6.0f, 0.0f, 36.0f);
    private final NumberSetting facePlaceHP = new NumberSetting("FacePlaceHP", 8.0f, 0.0f, 20.0f);
    private final BooleanSetting antiSuicide = new BooleanSetting("AntiSuicide", true);

    private final CategorySetting placeCategory = new CategorySetting("Place");
    private final BooleanSetting doPlace = new BooleanSetting("Place", true);
    private final NumberSetting placeDelay = new NumberSetting("PlaceDelay", 0.0f, 0.0f, 20.0f);
    private final NumberSetting placeRange = new NumberSetting("PlaceRange", 4.5f, 0.0f, 6.0f);
    private final NumberSetting placeWallsRange = new NumberSetting("WallPlaceRange", 4.5f, 0.0f, 6.0f);
    private final BooleanSetting place112 = new BooleanSetting("1.12Place", false);

    private final CategorySetting breakCategory = new CategorySetting("Break");
    private final BooleanSetting doBreak = new BooleanSetting("Break", true);
    private final NumberSetting breakDelay = new NumberSetting("BreakDelay", 0.0f, 0.0f, 20.0f);
    private final NumberSetting breakRange = new NumberSetting("BreakRange", 4.5f, 0.0f, 6.0f);
    private final NumberSetting breakWallsRange = new NumberSetting("WallBreakRange", 4.5f, 0.0f, 6.0f);
    private final NumberSetting minCrystalAge = new NumberSetting("CrystalAge", 0.0f, 0.0f, 20.0f);

    private final CategorySetting autoObbyCategory = new CategorySetting("AutoObsidian");
    private final BooleanSetting autoObsidian = new BooleanSetting("AutoObsidian", true);
    private final NumberSetting obbyRange = new NumberSetting("ObbyRange", 4.5f, 0.0f, 6.0f);
    private final NumberSetting obbyDelay = new NumberSetting("ObbyDelay", 2.0f, 0.0f, 20.0f);
    private final BooleanSetting obbyOnlyGround = new BooleanSetting("ObbyOnlyGround", true);
    private final BooleanSetting obbySmart = new BooleanSetting("Smart", true);

    private final CategorySetting autoProtectCategory = new CategorySetting("AutoProtect");
    private final BooleanSetting autoProtect = new BooleanSetting("AutoProtect", false);
    private final NumberSetting protectDelay = new NumberSetting("ProtectDelay", 4.0f, 0.0f, 20.0f);
    private final BooleanSetting protectOnlyGround = new BooleanSetting("ProtectOnlyGround", true);

    private final CategorySetting switchCategory = new CategorySetting("Switch");
    private final RadioSetting switchMode = new RadioSetting("Switch", "Normal");
    private final BooleanSetting noGapSwitch = new BooleanSetting("NoGapSwitch", true);
    private final BooleanSetting noBowSwitch = new BooleanSetting("NoBowSwitch", true);

    private final CategorySetting pauseCategory = new CategorySetting("Pause");
    private final BooleanSetting pauseMining = new BooleanSetting("PauseMining", true);
    private final BooleanSetting pauseEating = new BooleanSetting("PauseEating", true);
    private final NumberSetting pauseHP = new NumberSetting("PauseHP", 8.0f, 0.0f, 20.0f);

    private final BooleanSetting rotate = new BooleanSetting("Rotate", true);

    private LivingEntity target;
    private EndCrystalEntity bestCrystal;
    private BlockPos bestPlacePos;
    private BlockPos bestObbyPos;
    private int placeTimer;
    private int breakTimer;
    private int obbyTimer;
    private int protectTimer;
    private int previousSlot = -1;
    private boolean hasSwitched;

    public CrystalAuraV2() {
        super("CrystalAuraV2", "Meteor-style crystal aura flow", -1, Category.COMBAT);

        targetLogic.addOption("Distance");
        targetLogic.addOption("Health");
        switchMode.addOption("None");
        switchMode.addOption("Normal");

        addSetting(targetCategory);
        targetCategory.addSetting(targetRange);
        targetCategory.addSetting(targetLogic);
        targetCategory.addSetting(zombieTesting);

        addSetting(damageCategory);
        damageCategory.addSetting(minDamage);
        damageCategory.addSetting(maxSelfDamage);
        damageCategory.addSetting(facePlaceHP);
        damageCategory.addSetting(antiSuicide);

        addSetting(placeCategory);
        placeCategory.addSetting(doPlace);
        placeCategory.addSetting(placeDelay);
        placeCategory.addSetting(placeRange);
        placeCategory.addSetting(placeWallsRange);
        placeCategory.addSetting(place112);

        addSetting(breakCategory);
        breakCategory.addSetting(doBreak);
        breakCategory.addSetting(breakDelay);
        breakCategory.addSetting(breakRange);
        breakCategory.addSetting(breakWallsRange);
        breakCategory.addSetting(minCrystalAge);

        addSetting(autoObbyCategory);
        autoObbyCategory.addSetting(autoObsidian);
        autoObbyCategory.addSetting(obbyRange);
        autoObbyCategory.addSetting(obbyDelay);
        autoObbyCategory.addSetting(obbyOnlyGround);
        autoObbyCategory.addSetting(obbySmart);

        addSetting(autoProtectCategory);
        autoProtectCategory.addSetting(autoProtect);
        autoProtectCategory.addSetting(protectDelay);
        autoProtectCategory.addSetting(protectOnlyGround);

        addSetting(switchCategory);
        switchCategory.addSetting(switchMode);
        switchCategory.addSetting(noGapSwitch);
        switchCategory.addSetting(noBowSwitch);

        addSetting(pauseCategory);
        pauseCategory.addSetting(pauseMining);
        pauseCategory.addSetting(pauseEating);
        pauseCategory.addSetting(pauseHP);

        addSetting(rotate);
    }

    @Override
    public void onEnable() {
        placeTimer = 0;
        breakTimer = 0;
        obbyTimer = 0;
        protectTimer = 0;
        target = null;
        bestCrystal = null;
        bestPlacePos = null;
        bestObbyPos = null;
    }

    @Override
    public void onUpdate() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null || mc.interactionManager == null)
            return;

        placeTimer = Math.max(0, placeTimer - 1);
        breakTimer = Math.max(0, breakTimer - 1);
        obbyTimer = Math.max(0, obbyTimer - 1);
        protectTimer = Math.max(0, protectTimer - 1);

        if (shouldPause(mc)) {
            switchBack(mc);
            return;
        }

        target = findTarget(mc);
        if (target == null) {
            switchBack(mc);
            return;
        }

        findBestCrystal(mc);
        findBestPlacePos(mc);

        if (autoProtect.isEnabled() && protectTimer <= 0) {
            if (placeAutoProtect(mc)) {
                protectTimer = (int) protectDelay.getValue();
                return;
            }
        }

        if (autoObsidian.isEnabled() && bestPlacePos == null && obbyTimer <= 0) {
            findBestObsidianPos(mc);
            if (bestObbyPos != null) {
                placeObsidian(mc);
                return;
            }
        }

        if (bestCrystal != null && doBreak.isEnabled() && breakTimer <= 0) {
            breakCrystal(mc);
        } else if (bestPlacePos != null && doPlace.isEnabled() && placeTimer <= 0) {
            placeCrystal(mc);
        }
    }

    @Override
    public void onDisable() {
        switchBack(MinecraftClient.getInstance());
    }

    private LivingEntity findTarget(MinecraftClient mc) {
        LivingEntity best = null;
        double bestValue = Double.MAX_VALUE;
        double maxRangeSq = sq(targetRange.getValue());

        for (Entity e : mc.world.getEntities()) {
            if (e == mc.player || e instanceof EndCrystalEntity || e instanceof ItemEntity)
                continue;
            if (!(e instanceof LivingEntity living) || !living.isAlive())
                continue;

            boolean configured = TargetManager.INSTANCE.isValidTarget(e);
            boolean zombie = zombieTesting.isEnabled() && e instanceof ZombieEntity;
            if (!configured && !zombie)
                continue;

            double distSq = mc.player.squaredDistanceTo(e);
            if (distSq > maxRangeSq)
                continue;

            double value = "Health".equals(targetLogic.getSelectedOption()) ? living.getHealth() : distSq;
            if (value < bestValue) {
                bestValue = value;
                best = living;
            }
        }
        return best;
    }

    private void findBestCrystal(MinecraftClient mc) {
        bestCrystal = null;
        double bestDamage = 0.0;

        if (!doBreak.isEnabled() || target == null)
            return;

        double maxBreak = breakRange.getValue();
        double maxWallBreakSq = sq(breakWallsRange.getValue());

        for (Entity e : mc.world.getEntities()) {
            if (!(e instanceof EndCrystalEntity crystal))
                continue;
            if (minCrystalAge.getValue() > 0 && crystal.age < minCrystalAge.getValue())
                continue;

            double dist = mc.player.getEyePos().distanceTo(crystal.getPos());
            if (dist > maxBreak)
                continue;

            boolean canSee = canSee(mc, crystal.getPos().add(0, crystal.getHeight() * 0.5, 0));
            if (!canSee && sq(dist) > maxWallBreakSq)
                continue;

            double targetDmg = DamageUtils.crystalDamage(target, crystal.getPos());
            double selfDmg = DamageUtils.crystalDamage(mc.player, crystal.getPos());
            if (!isDamageSafe(targetDmg, selfDmg, mc.player))
                continue;

            if (targetDmg > bestDamage) {
                bestDamage = targetDmg;
                bestCrystal = crystal;
            }
        }
    }

    private void findBestPlacePos(MinecraftClient mc) {
        bestPlacePos = null;
        double bestDamage = 0.0;

        if (!doPlace.isEnabled() || target == null)
            return;

        BlockPos playerPos = mc.player.getBlockPos();
        int radius = (int) Math.ceil(placeRange.getValue());
        int vertical = 2;

        for (int x = -radius; x <= radius; x++) {
            for (int y = -vertical; y <= vertical; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos pos = playerPos.add(x, y, z);
                    if (!canPlaceCrystal(mc, pos))
                        continue;

                    Vec3d crystalPos = Vec3d.ofCenter(pos).add(0, 1, 0);
                    double dist = mc.player.getEyePos().distanceTo(crystalPos);
                    if (dist > placeRange.getValue())
                        continue;

                    boolean canSee = canSee(mc, crystalPos);
                    if (!canSee && dist > placeWallsRange.getValue())
                        continue;

                    double targetDmg = DamageUtils.crystalDamage(target, crystalPos);
                    double selfDmg = DamageUtils.crystalDamage(mc.player, crystalPos);
                    if (!isDamageSafe(targetDmg, selfDmg, mc.player))
                        continue;

                    if (targetDmg > bestDamage) {
                        bestDamage = targetDmg;
                        bestPlacePos = pos;
                    }
                }
            }
        }
    }

    private void breakCrystal(MinecraftClient mc) {
        if (bestCrystal == null)
            return;
        if (rotate.isEnabled())
            applyRotation(mc, bestCrystal.getPos().add(0, bestCrystal.getHeight() * 0.5, 0));

        mc.interactionManager.attackEntity(mc.player, bestCrystal);
        mc.player.swingHand(Hand.MAIN_HAND);
        breakTimer = (int) breakDelay.getValue();
    }

    private void placeCrystal(MinecraftClient mc) {
        if (bestPlacePos == null)
            return;

        boolean offhand = mc.player.getOffHandStack().isOf(Items.END_CRYSTAL);
        if (!offhand && !mc.player.getMainHandStack().isOf(Items.END_CRYSTAL)) {
            if ("None".equals(switchMode.getSelectedOption()))
                return;
            if (shouldPauseForItem(mc))
                return;

            int crystalSlot = findCrystalSlot(mc);
            if (crystalSlot == -1)
                return;

            if (!hasSwitched) {
                previousSlot = mc.player.getInventory().getSelectedSlot();
                hasSwitched = true;
            }
            mc.player.getInventory().setSelectedSlot(crystalSlot);
        }

        Vec3d hitVec = Vec3d.ofCenter(bestPlacePos).add(0, 1, 0);
        if (rotate.isEnabled())
            applyRotation(mc, hitVec);

        Hand hand = offhand ? Hand.OFF_HAND : Hand.MAIN_HAND;
        BlockHitResult hit = new BlockHitResult(hitVec, Direction.UP, bestPlacePos, false);
        mc.interactionManager.interactBlock(mc.player, hand, hit);
        mc.player.swingHand(hand);
        placeTimer = (int) placeDelay.getValue();
    }

    private void findBestObsidianPos(MinecraftClient mc) {
        bestObbyPos = null;
        if (target == null)
            return;

        BlockPos center = target.getBlockPos();
        int horizontal = (int) Math.ceil(obbyRange.getValue());
        int vertical = 2;
        double maxRangeSq = sq(obbyRange.getValue());
        double bestScore = 0.0;

        for (int x = -horizontal; x <= horizontal; x++) {
            for (int y = -vertical; y <= vertical; y++) {
                for (int z = -horizontal; z <= horizontal; z++) {
                    BlockPos pos = center.add(x, y, z);
                    if (mc.player.getBlockPos().getSquaredDistance(pos) > maxRangeSq)
                        continue;
                    if (!canPlaceObsidian(mc, pos))
                        continue;
                    if (obbySmart.isEnabled() && hasObsidianNearby(mc, pos))
                        continue;

                    Vec3d crystalPos = Vec3d.ofCenter(pos).add(0, 1, 0);
                    if (mc.player.getEyePos().distanceTo(crystalPos) > placeRange.getValue())
                        continue;

                    double targetDmg = DamageUtils.crystalDamage(target, crystalPos);
                    double selfDmg = DamageUtils.crystalDamage(mc.player, crystalPos);
                    if (!isDamageSafe(targetDmg, selfDmg, mc.player))
                        continue;

                    double score = targetDmg - Math.sqrt(target.getBlockPos().getSquaredDistance(pos)) * 0.25;
                    if (score > bestScore) {
                        bestScore = score;
                        bestObbyPos = pos;
                    }
                }
            }
        }
    }

    private boolean canPlaceObsidian(MinecraftClient mc, BlockPos pos) {
        if (!mc.world.getBlockState(pos).isReplaceable())
            return false;
        if (obbyOnlyGround.isEnabled() && !mc.world.getBlockState(pos.down()).isSolid())
            return false;
        if (!mc.world.getBlockState(pos.down()).isSolid())
            return false;

        if (!mc.world.getBlockState(pos.up()).isAir())
            return false;
        if (!place112.isEnabled() && !mc.world.getBlockState(pos.up().up()).isAir())
            return false;

        Box box = new Box(pos);
        for (Entity e : mc.world.getOtherEntities(null, box)) {
            if (e instanceof ItemEntity)
                continue;
            return false;
        }
        return true;
    }

    private boolean hasObsidianNearby(MinecraftClient mc, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            if (direction == Direction.UP || direction == Direction.DOWN)
                continue;
            BlockPos offset = pos.offset(direction);
            if (mc.world.getBlockState(offset).isOf(Blocks.OBSIDIAN)
                    || mc.world.getBlockState(offset).isOf(Blocks.BEDROCK)) {
                return true;
            }
        }
        return false;
    }

    private void placeObsidian(MinecraftClient mc) {
        if (bestObbyPos == null)
            return;

        boolean offhand = mc.player.getOffHandStack().isOf(Items.OBSIDIAN);
        if (!offhand && !mc.player.getMainHandStack().isOf(Items.OBSIDIAN)) {
            if ("None".equals(switchMode.getSelectedOption()))
                return;
            if (shouldPauseForItem(mc))
                return;

            int slot = findObsidianSlot(mc);
            if (slot == -1)
                return;

            if (!hasSwitched) {
                previousSlot = mc.player.getInventory().getSelectedSlot();
                hasSwitched = true;
            }
            mc.player.getInventory().setSelectedSlot(slot);
        }

        Vec3d hitVec = Vec3d.ofCenter(bestObbyPos.down()).add(0, 1, 0);
        if (rotate.isEnabled())
            applyRotation(mc, Vec3d.ofCenter(bestObbyPos));

        Hand hand = offhand ? Hand.OFF_HAND : Hand.MAIN_HAND;
        BlockHitResult hit = new BlockHitResult(hitVec, Direction.UP, bestObbyPos.down(), false);
        mc.interactionManager.interactBlock(mc.player, hand, hit);
        mc.player.swingHand(hand);
        obbyTimer = (int) obbyDelay.getValue();
    }

    private boolean placeAutoProtect(MinecraftClient mc) {
        if (protectOnlyGround.isEnabled() && !mc.player.isOnGround()) {
            return false;
        }

        Direction facing = yawToHorizontalDirection(mc.player.getYaw());
        BlockPos lower = mc.player.getBlockPos().offset(facing);
        BlockPos upper = lower.up();

        boolean placedAny = false;
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

        Hand hand = offhand ? Hand.OFF_HAND : Hand.MAIN_HAND;

        if (!mc.world.getBlockState(lower).isOf(Blocks.OBSIDIAN) && canPlaceProtectObsidian(mc, lower)) {
            Vec3d hitVec = Vec3d.ofCenter(lower.down()).add(0, 1, 0);
            if (rotate.isEnabled()) applyRotation(mc, Vec3d.ofCenter(lower));
            BlockHitResult hit = new BlockHitResult(hitVec, Direction.UP, lower.down(), false);
            mc.interactionManager.interactBlock(mc.player, hand, hit);
            mc.player.swingHand(hand);
            placedAny = true;
        }

        if (!mc.world.getBlockState(upper).isOf(Blocks.OBSIDIAN) && canPlaceProtectObsidian(mc, upper)) {
            Vec3d hitVec = Vec3d.ofCenter(upper.down()).add(0, 1, 0);
            if (rotate.isEnabled()) applyRotation(mc, Vec3d.ofCenter(upper));
            BlockHitResult hit = new BlockHitResult(hitVec, Direction.UP, upper.down(), false);
            mc.interactionManager.interactBlock(mc.player, hand, hit);
            mc.player.swingHand(hand);
            placedAny = true;
        }

        return placedAny;
    }

    private boolean canPlaceCrystal(MinecraftClient mc, BlockPos pos) {
        if (!mc.world.getBlockState(pos).isOf(Blocks.OBSIDIAN) && !mc.world.getBlockState(pos).isOf(Blocks.BEDROCK)) {
            return false;
        }

        BlockPos above = pos.up();
        if (!mc.world.getBlockState(above).isAir())
            return false;
        if (!place112.isEnabled() && !mc.world.getBlockState(above.up()).isAir())
            return false;

        Box box = new Box(above);
        if (!place112.isEnabled())
            box = box.stretch(0, 1, 0);

        for (Entity e : mc.world.getOtherEntities(null, box)) {
            if (e instanceof ItemEntity)
                continue;
            return false;
        }
        return true;
    }

    private boolean canPlaceProtectObsidian(MinecraftClient mc, BlockPos pos) {
        if (!mc.world.getBlockState(pos).isReplaceable()) return false;

        Box box = new Box(pos);
        for (Entity e : mc.world.getOtherEntities(null, box)) {
            if (e instanceof ItemEntity) continue;
            return false;
        }

        if (mc.world.getBlockState(pos.down()).isSolid()) return true;
        for (Direction dir : Direction.values()) {
            if (dir == Direction.UP || dir == Direction.DOWN) continue;
            if (mc.world.getBlockState(pos.offset(dir)).isSolid()) return true;
        }
        return false;
    }

    private boolean canSee(MinecraftClient mc, Vec3d to) {
        RaycastContext context = new RaycastContext(mc.player.getEyePos(), to, RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE, mc.player);
        BlockHitResult hit = mc.world.raycast(context);
        return hit.getType() == HitResult.Type.MISS;
    }

    private boolean isDamageSafe(double targetDmg, double selfDmg, PlayerEntity self) {
        if (target == null)
            return false;

        if (target.getHealth() + target.getAbsorptionAmount() <= facePlaceHP.getValue()) {
            if (targetDmg < 1.0)
                return false;
        } else if (targetDmg < minDamage.getValue()) {
            return false;
        }

        if (selfDmg > maxSelfDamage.getValue())
            return false;
        if (antiSuicide.isEnabled() && selfDmg >= self.getHealth() + self.getAbsorptionAmount())
            return false;
        return true;
    }

    private boolean shouldPause(MinecraftClient mc) {
        if (pauseMining.isEnabled() && mc.interactionManager.isBreakingBlock())
            return true;
        if (pauseEating.isEnabled() && mc.player.isUsingItem())
            return true;
        return mc.player.getHealth() + mc.player.getAbsorptionAmount() < pauseHP.getValue();
    }

    private boolean shouldPauseForItem(MinecraftClient mc) {
        if (!mc.player.isUsingItem())
            return false;
        Item item = mc.player.getActiveItem().getItem();
        if (noGapSwitch.isEnabled() && (item == Items.GOLDEN_APPLE || item == Items.ENCHANTED_GOLDEN_APPLE))
            return true;
        return noBowSwitch.isEnabled() && item instanceof BowItem;
    }

    private int findCrystalSlot(MinecraftClient mc) {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).isOf(Items.END_CRYSTAL))
                return i;
        }
        return -1;
    }

    private int findObsidianSlot(MinecraftClient mc) {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).isOf(Items.OBSIDIAN))
                return i;
        }
        return -1;
    }

    private void switchBack(MinecraftClient mc) {
        if (mc.player == null)
            return;
        if (hasSwitched && previousSlot != -1) {
            mc.player.getInventory().setSelectedSlot(previousSlot);
            previousSlot = -1;
            hasSwitched = false;
        }
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

    private Direction yawToHorizontalDirection(float yaw) {
        int i = MathHelper.floor((yaw * 4.0f / 360.0f) + 0.5f) & 3;
        return switch (i) {
            case 0 -> Direction.SOUTH;
            case 1 -> Direction.WEST;
            case 2 -> Direction.NORTH;
            default -> Direction.EAST;
        };
    }
}



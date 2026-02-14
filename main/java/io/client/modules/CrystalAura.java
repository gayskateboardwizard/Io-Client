package io.client.modules;

import io.client.Category;
import io.client.Module;
import io.client.TargetManager;
import io.client.settings.BooleanSetting;
import io.client.settings.CategorySetting;
import io.client.settings.NumberSetting;
import io.client.settings.RadioSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CrystalAura extends Module {

    private static final double MAX_CRYSTAL_DAMAGE_DISTANCE = 12.0;
    private static double sq(double v) { return v * v; }

    private final CategorySetting targetCategory = new CategorySetting("Target");
    private final RadioSetting targetLogic = new RadioSetting("TargetLogic", "Distance");
    private final NumberSetting targetRange = new NumberSetting("TargetRange", 10.0F, 1.0F, 20.0F);

    private final CategorySetting placeCategory = new CategorySetting("Place");
    private final BooleanSetting doPlace = new BooleanSetting("Place", true);
    private final NumberSetting placeDelay = new NumberSetting("PlaceDelay", 0.0F, 0.0F, 20.0F);
    private final NumberSetting placeRange = new NumberSetting("PlaceRange", 4.5F, 0.0F, 6.0F);
    private final NumberSetting placeWallsRange = new NumberSetting("WallplaceRange", 4.5F, 0.0F, 6.0F);
    private final BooleanSetting oldPlace = new BooleanSetting("1.12Place", false);
    private final BooleanSetting raycast = new BooleanSetting("Raycast", false);
    private final BooleanSetting blacklist = new BooleanSetting("Blacklist", true);

    private final CategorySetting breakCategory = new CategorySetting("Break");
    private final BooleanSetting doBreak = new BooleanSetting("Break", true);
    private final NumberSetting breakDelay = new NumberSetting("BreakDelay", 0.0F, 0.0F, 20.0F);
    private final NumberSetting breakRange = new NumberSetting("BreakRange", 4.5F, 0.0F, 6.0F);
    private final NumberSetting breakWallsRange = new NumberSetting("WallBreakRange", 4.5F, 0.0F, 6.0F);
    private final NumberSetting crystalAge = new NumberSetting("CrystalAge", 0.0F, 0.0F, 20.0F);
    private final BooleanSetting inhibit = new BooleanSetting("Inhibit", true);

    private final CategorySetting damageCategory = new CategorySetting("Damage");
    private final NumberSetting minDamage = new NumberSetting("MinDamage", 6.0F, 0.0F, 50.0F);
    private final NumberSetting maxSelfDamage = new NumberSetting("MaxSelfDamage", 6.0F, 0.0F, 36.0F);
    private final BooleanSetting antiSuicide = new BooleanSetting("AntiSuicide", true);
    private final BooleanSetting efficiency = new BooleanSetting("Efficiency", false);
    private final NumberSetting efficiencyRatio = new NumberSetting("EfficiencyRatio", 2.0F, 1.0F, 5.0F);
    private final NumberSetting armorScale = new NumberSetting("ArmorScale", 5.0F, 0.0F, 40.0F);
    private final NumberSetting facePlaceHP = new NumberSetting("FacePlaceHP", 8.0F, 0.0F, 20.0F);

    private final CategorySetting switchCategory = new CategorySetting("Switch");
    private final RadioSetting autoSwitch = new RadioSetting("Switch", "Normal");
    private final BooleanSetting noGapSwitch = new BooleanSetting("NoGapSwitch", true);
    private final BooleanSetting noBowSwitch = new BooleanSetting("NoBowSwitch", true);
    private final RadioSetting antiWeakness = new RadioSetting("AntiWeakness", "Silent");

    private final CategorySetting timingCategory = new CategorySetting("Timing");
    private final RadioSetting timing = new RadioSetting("Timing", "Normal");
    private final RadioSetting sequential = new RadioSetting("Sequential", "Strong");

    private final CategorySetting pauseCategory = new CategorySetting("Pause");
    private final BooleanSetting pauseMining = new BooleanSetting("PauseMining", true);
    private final BooleanSetting pauseEating = new BooleanSetting("PauseEating", true);
    private final NumberSetting pauseHP = new NumberSetting("PauseHP", 8.0F, 0.0F, 20.0F);

    private final CategorySetting rotateCategory = new CategorySetting("Rotate");
    private final BooleanSetting rotate = new BooleanSetting("Rotate", true);
    private final RadioSetting rotateType = new RadioSetting("RotateType", "Simple");

    private final CategorySetting autoObbyCat = new CategorySetting("AutoObby");
    private final BooleanSetting autoObby = new BooleanSetting("AutoObby", true);
    private final NumberSetting obbyRange = new NumberSetting("ObbyRange", 4.5F, 0.0F, 6.0F);
    private final NumberSetting obbyDelay = new NumberSetting("ObbyDelay", 2.0F, 0.0F, 20.0F);
    private final BooleanSetting obbyOnlyGround = new BooleanSetting("OnlyOnGround", true);
    private final BooleanSetting obbySmartPlace = new BooleanSetting("SmartPlace", true); // wont place if obsidian is placed nearby already
    private final NumberSetting obbyMinDamage = new NumberSetting("MinimumEstDMG", 2.0F, 0.0F, 20.0F); // minimum estimated damage from the placed obsidian for the crystal
    private final NumberSetting obbyDistanceRatio = new NumberSetting("PTTratio", 1.0F, 0.1F, 5.0F); // player to target distance ratio for placement

    private final Map<BlockPos, Long> blacklistedPos = new ConcurrentHashMap<>();
    private final Map<Integer, Long> attackedCrystals = new ConcurrentHashMap<>();

    private LivingEntity target;
    private BlockPos bestPlacePos;
    private EndCrystal bestCrystal;
    private int placeTimer = 0;
    private int breakTimer = 0;
    private BlockPos bestObbyPos;
    private int obbyTimer = 0;
    private int previousSlot = -1;
    private boolean hasSwitched = false;

    public CrystalAura() {
        super("CrystalAura", "Auto crystal pvp", -1, Category.COMBAT);

        targetLogic.addOption("Distance");
        targetLogic.addOption("Health");

        autoSwitch.addOption("None");
        autoSwitch.addOption("Normal");
        autoSwitch.addOption("Silent");

        antiWeakness.addOption("None");
        antiWeakness.addOption("Normal");
        antiWeakness.addOption("Silent");

        timing.addOption("Normal");
        timing.addOption("Sequential");

        sequential.addOption("None");
        sequential.addOption("Soft");
        sequential.addOption("Strong");

        rotateType.addOption("Simple");
        rotateType.addOption("Center");

        addSetting(targetCategory);
        targetCategory.addSetting(targetLogic);
        targetCategory.addSetting(targetRange);

        addSetting(placeCategory);
        placeCategory.addSetting(doPlace);
        placeCategory.addSetting(placeDelay);
        placeCategory.addSetting(placeRange);
        placeCategory.addSetting(placeWallsRange);
        placeCategory.addSetting(oldPlace);
        placeCategory.addSetting(raycast);
        placeCategory.addSetting(blacklist);

        addSetting(breakCategory);
        breakCategory.addSetting(doBreak);
        breakCategory.addSetting(breakDelay);
        breakCategory.addSetting(breakRange);
        breakCategory.addSetting(breakWallsRange);
        breakCategory.addSetting(crystalAge);
        breakCategory.addSetting(inhibit);

        addSetting(damageCategory);
        damageCategory.addSetting(minDamage);
        damageCategory.addSetting(maxSelfDamage);
        damageCategory.addSetting(antiSuicide);
        damageCategory.addSetting(efficiency);
        damageCategory.addSetting(efficiencyRatio);
        damageCategory.addSetting(armorScale);
        damageCategory.addSetting(facePlaceHP);

        addSetting(switchCategory);
        switchCategory.addSetting(autoSwitch);
        switchCategory.addSetting(noGapSwitch);
        switchCategory.addSetting(noBowSwitch);
        switchCategory.addSetting(antiWeakness);

        addSetting(timingCategory);
        timingCategory.addSetting(timing);
        timingCategory.addSetting(sequential);

        addSetting(pauseCategory);
        pauseCategory.addSetting(pauseMining);
        pauseCategory.addSetting(pauseEating);
        pauseCategory.addSetting(pauseHP);

        addSetting(rotateCategory);
        rotateCategory.addSetting(rotate);
        rotateCategory.addSetting(rotateType);

        addSetting(autoObbyCat);
        autoObbyCat.addSetting(autoObby);
        autoObbyCat.addSetting(obbyRange);
        autoObbyCat.addSetting(obbyDelay);
        autoObbyCat.addSetting(obbyOnlyGround);
        autoObbyCat.addSetting(obbySmartPlace);
        autoObbyCat.addSetting(obbyMinDamage);
        autoObbyCat.addSetting(obbyDistanceRatio);
    }

    @Override
    public void onUpdate() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        placeTimer = Math.max(0, placeTimer - 1);
        breakTimer = Math.max(0, breakTimer - 1);
        obbyTimer = Math.max(0, obbyTimer - 1);

        long worldTick = mc.level.getGameTime();

        if (!blacklistedPos.isEmpty()) {
            blacklistedPos.entrySet().removeIf(e -> e.getValue() <= worldTick);
        }

        if (!attackedCrystals.isEmpty()) {
            attackedCrystals.entrySet().removeIf(e -> (worldTick - e.getValue()) > 20L);
        }

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

        if (autoObby.isEnabled() && bestPlacePos == null && obbyTimer <= 0) {
            findBestObbyPos(mc);
            if (bestObbyPos != null) {
                placeObsidian(mc);
                return;
            }
        }

        if (timing.getSelectedOption().equals("Sequential")) {
            doSequential(mc);
        } else {
            if (bestCrystal != null && doBreak.isEnabled() && breakTimer <= 0) {
                breakCrystal(mc);
            } else if (bestPlacePos != null && doPlace.isEnabled() && placeTimer <= 0) {
                placeCrystal(mc);
            }
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
        switchBack(Minecraft.getInstance());
        target = null;
        bestPlacePos = null;
        bestCrystal = null;
    }

    private void doSequential(Minecraft mc) {
        if (sequential.getSelectedOption().equals("None")) {
            if (bestCrystal != null && breakTimer <= 0) {
                breakCrystal(mc);
            } else if (bestPlacePos != null && placeTimer <= 0) {
                placeCrystal(mc);
            }
        } else {
            if (bestCrystal != null && breakTimer <= 0) {
                breakCrystal(mc);
            }
            if (bestPlacePos != null && placeTimer <= 0) {
                placeCrystal(mc);
            }
        }
    }

    private LivingEntity findTarget(Minecraft mc) {
        LivingEntity best = null;
        double bestValue = Double.MAX_VALUE;
        double maxRangeSq = sq(targetRange.getValue());

        for (Entity e : mc.level.entitiesForRendering()) {
            if (!(e instanceof LivingEntity living)) continue;
            if (e == mc.player) continue;
            if (living.getHealth() <= 0) continue;
            if (e instanceof EndCrystal || e instanceof ItemEntity) continue;
            if (!TargetManager.INSTANCE.isValidTarget(e)) continue;

            double distSq = mc.player.distanceToSqr(e);
            if (distSq > maxRangeSq) continue;

            double value = targetLogic.getSelectedOption().equals("Distance") ? distSq : living.getHealth();

            if (value < bestValue) {
                bestValue = value;
                best = living;
            }
        }

        return best;
    }

    private void findBestCrystal(Minecraft mc) {
        bestCrystal = null;
        double bestDamage = 0;

        if (!doBreak.isEnabled()) return;
        if (target == null) return;

        double maxBreakRange = breakRange.getValue();
        double maxWallRangeSq = sq(breakWallsRange.getValue());
        long worldTick = mc.level.getGameTime();

        for (Entity e : mc.level.entitiesForRendering()) {
            if (!(e instanceof EndCrystal crystal)) continue;

            if (crystalAge.getValue() > 0 && crystal.tickCount < crystalAge.getValue()) continue;

            if (inhibit.isEnabled() && attackedCrystals.containsKey(crystal.getId())) {
                long timeSinceAttackTicks = worldTick - attackedCrystals.get(crystal.getId());
                if (timeSinceAttackTicks < 10L) continue;
            }

            double dist = mc.player.getEyePosition().distanceTo(crystal.position());
            if (dist > maxBreakRange) continue;

            boolean canSee = canSeeCrystal(mc, crystal);
            if (!canSee && sq(dist) > maxWallRangeSq) continue;

            double targetDmg = calculateDamage(crystal.position(), target);
            double selfDmg = calculateDamage(crystal.position(), mc.player);

            if (!isDamageSafe(targetDmg, selfDmg, mc.player)) continue;

            if (targetDmg > bestDamage) {
                bestDamage = targetDmg;
                bestCrystal = crystal;
            }
        }
    }

    private void findBestObbyPos(Minecraft mc) {
        bestObbyPos = null;
        double bestScore = 0;

        if (target == null) return;

        BlockPos targetPos = target.blockPosition();
        int range = (int) Math.ceil(obbyRange.getValue());
        double maxRangeSq = sq(obbyRange.getValue());
        int verticalRange = 2;

        for (int x = -range; x <= range; x++) {
            for (int y = -verticalRange; y <= verticalRange; y++) {
                for (int z = -range; z <= range; z++) {
                    BlockPos pos = targetPos.offset(x, y, z);

                    double distSq = mc.player.blockPosition().distSqr(pos);
                    if (distSq > maxRangeSq) continue;

                    if (!canPlaceObby(mc, pos)) continue;

                    if (obbyOnlyGround.isEnabled()) {
                        if (!mc.level.getBlockState(pos.below()).isSolid()) continue;
                    }

                    if (obbySmartPlace.isEnabled() && hasObbyNearby(mc, pos)) continue;

                    double score = scoreObbyPosition(mc, pos);

                    if (score > bestScore) {
                        bestScore = score;
                        bestObbyPos = pos;
                    }
                }
            }
        }
    }

    private boolean hasObbyNearby(Minecraft mc, BlockPos pos) {
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                if (x == 0 && z == 0) continue;
                BlockPos checkPos = pos.offset(x, 0, z);
                if (mc.level.getBlockState(checkPos).is(Blocks.OBSIDIAN) ||
                        mc.level.getBlockState(checkPos).is(Blocks.BEDROCK)) {
                    return true;
                }
            }
        }
        return false;
    }

    private double scoreObbyPosition(Minecraft mc, BlockPos obbyPos) {
        if (target == null) return 0;

        BlockPos crystalPos = obbyPos.above();
        Vec3 crystalVec = Vec3.atCenterOf(crystalPos).add(0, 1, 0);

        double distToPlayer = mc.player.getEyePosition().distanceTo(crystalVec);
        if (distToPlayer > placeRange.getValue()) return 0;

        if (!mc.level.getBlockState(crystalPos).isAir()) return 0;
        if (!mc.level.getBlockState(crystalPos.above()).isAir() && !oldPlace.isEnabled()) return 0;

        double targetDmg = calculateDamage(crystalVec, target);
        double selfDmg = calculateDamage(crystalVec, mc.player);

        if (obbySmartPlace.isEnabled()) {
            if (targetDmg < obbyMinDamage.getValue()) return 0;
        }

        if (!isDamageSafe(targetDmg, selfDmg, mc.player)) return 0;

        double score = targetDmg;

        double targetDist = Math.sqrt(target.blockPosition().distSqr(obbyPos));
        double playerDist = Math.sqrt(mc.player.blockPosition().distSqr(obbyPos));

        double ratio = obbyDistanceRatio.getValue();
        score -= (targetDist * 0.5);
        score -= (playerDist * 0.5 * ratio);

        return score;
    }

    private boolean canPlaceObby(Minecraft mc, BlockPos pos) {
        if (!mc.level.getBlockState(pos).canBeReplaced()) return false;

        if (mc.level.getBlockState(pos.below()).is(Blocks.OBSIDIAN)) return false;
        if (mc.level.getBlockState(pos.below()).is(Blocks.BEDROCK)) return false;
        if (!mc.level.getBlockState(pos.below()).isSolid()) return false;

        AABB box = new AABB(pos);
        for (Entity e : mc.level.getEntities(null, box)) {
            if (!(e instanceof ItemEntity)) return false;
        }

        return true;
    }

    private void placeObsidian(Minecraft mc) {
        if (bestObbyPos == null) return;

        int obbySlot = findObsidianSlot(mc);
        if (obbySlot == -1) return;

        boolean offhand = mc.player.getOffhandItem().is(Items.OBSIDIAN);
        int oldSlot = mc.player.getInventory().getSelectedSlot();
        boolean needSwitch = !offhand && !mc.player.getMainHandItem().is(Items.OBSIDIAN);

        if (needSwitch) {
            if (autoSwitch.getSelectedOption().equals("None")) return;

            if (!autoSwitch.getSelectedOption().equals("Silent")) {
                if (!hasSwitched) {
                    previousSlot = oldSlot;
                    hasSwitched = true;
                }
            }

            switchToSlot(mc, obbySlot);
        }

        if (rotate.isEnabled()) {
            Vec3 vec = Vec3.atCenterOf(bestObbyPos);
            applyRotation(mc, vec);
        }

        InteractionHand hand = offhand ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        Vec3 hitVec = Vec3.atCenterOf(bestObbyPos.below()).add(0, 1, 0);
        BlockHitResult result = new BlockHitResult(hitVec, Direction.UP, bestObbyPos.below(), false);

        mc.gameMode.useItemOn(mc.player, hand, result);
        mc.player.swing(hand);

        obbyTimer = (int) obbyDelay.getValue();

        if (needSwitch && autoSwitch.getSelectedOption().equals("Silent")) {
            switchToSlot(mc, oldSlot);
        }
    }

    private int findObsidianSlot(Minecraft mc) {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getItem(i).is(Items.OBSIDIAN)) {
                return i;
            }
        }
        return -1;
    }

    private void findBestPlacePos(Minecraft mc) {
        bestPlacePos = null;
        double bestDamage = 0;

        if (!doPlace.isEnabled()) return;
        if (target == null) return;

        BlockPos playerPos = mc.player.blockPosition();
        int horizontalRange = (int) Math.ceil(placeRange.getValue());
        int verticalRange = 2;

        double placeRangeVal = placeRange.getValue();
        double placeWallsRangeVal = placeWallsRange.getValue();

        for (int x = -horizontalRange; x <= horizontalRange; x++) {
            for (int y = -verticalRange; y <= verticalRange; y++) {
                for (int z = -horizontalRange; z <= horizontalRange; z++) {
                    BlockPos pos = playerPos.offset(x, y, z);

                    if (blacklist.isEnabled() && blacklistedPos.containsKey(pos)) continue;

                    if (!canPlaceCrystal(mc, pos)) continue;

                    Vec3 crystalVec = Vec3.atCenterOf(pos).add(0, 1, 0);
                    double dist = mc.player.getEyePosition().distanceTo(crystalVec);

                    if (dist > placeRangeVal) continue;

                    boolean canSee = !isLineBlocked(mc, mc.player.getEyePosition(), crystalVec);
                    if (!canSee && dist > placeWallsRangeVal) continue;

                    if (raycast.isEnabled() && !canSee) continue;

                    double targetDmg = calculateDamage(crystalVec, target);
                    double selfDmg = calculateDamage(crystalVec, mc.player);

                    if (!isDamageSafe(targetDmg, selfDmg, mc.player)) continue;

                    if (targetDmg > bestDamage) {
                        bestDamage = targetDmg;
                        bestPlacePos = pos;
                    }
                }
            }
        }
    }

    private boolean isDamageSafe(double targetDmg, double selfDmg, Player player) {
        if (target == null) return false;

        if (shouldFacePlace(target)) {
            if (targetDmg < 1.0) return false;
        } else {
            if (targetDmg < minDamage.getValue()) return false;
        }

        if (selfDmg > maxSelfDamage.getValue()) return false;

        if (antiSuicide.isEnabled()) {
            if (selfDmg >= player.getHealth() + player.getAbsorptionAmount()) return false;
        }

        if (efficiency.isEnabled()) {
            if (selfDmg == 0) return true;
            double ratio = targetDmg / selfDmg;
            if (ratio < efficiencyRatio.getValue()) return false;
        }

        return true;
    }

    private boolean shouldFacePlace(LivingEntity target) {
        if (target == null) return false;
        return target.getHealth() + target.getAbsorptionAmount() <= facePlaceHP.getValue();
    }

    private void breakCrystal(Minecraft mc) {
        if (bestCrystal == null) return;

        int oldSlot = mc.player.getInventory().getSelectedSlot();
        boolean switched = switchForWeakness(mc);

        if (rotate.isEnabled()) {
            Vec3 vec = bestCrystal.position().add(0, bestCrystal.getBbHeight() / 2, 0);
            applyRotation(mc, vec);
        }

        mc.gameMode.attack(mc.player, bestCrystal);
        mc.player.swing(InteractionHand.MAIN_HAND);

        attackedCrystals.put(bestCrystal.getId(), mc.level.getGameTime());
        breakTimer = (int) breakDelay.getValue();

        if (switched) {
            switchToSlot(mc, oldSlot);
        }
    }

    private void placeCrystal(Minecraft mc) {
        if (bestPlacePos == null) return;

        boolean offhand = mc.player.getOffhandItem().is(Items.END_CRYSTAL);

        if (!offhand && !mc.player.getMainHandItem().is(Items.END_CRYSTAL)) {
            if (autoSwitch.getSelectedOption().equals("None")) {
                return;
            }

            if (shouldPauseForItem(mc)) {
                return;
            }

            int crystalSlot = findCrystalSlot(mc);
            if (crystalSlot == -1) return;

            if (!autoSwitch.getSelectedOption().equals("Silent")) {
                if (!hasSwitched) {
                    previousSlot = mc.player.getInventory().getSelectedSlot();
                    hasSwitched = true;
                }
            }

            switchToSlot(mc, crystalSlot);
        }

        if (rotate.isEnabled()) {
            Vec3 vec = getPlaceVec(bestPlacePos);
            applyRotation(mc, vec);
        }

        InteractionHand hand = offhand ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        Vec3 hitVec = Vec3.atCenterOf(bestPlacePos).add(0, 1, 0);
        BlockHitResult result = new BlockHitResult(hitVec, Direction.UP, bestPlacePos, false);

        mc.gameMode.useItemOn(mc.player, hand, result);
        mc.player.swing(hand);

        placeTimer = (int) placeDelay.getValue();

        if (blacklist.isEnabled()) {
            long expiration = mc.level.getGameTime() + 4L;
            blacklistedPos.put(bestPlacePos, expiration);
        }
    }

    private Vec3 getPlaceVec(BlockPos pos) {
        if (rotateType.getSelectedOption().equals("Center")) {
            return Vec3.atCenterOf(pos).add(0, 1, 0);
        }
        return Vec3.atCenterOf(pos).add(0, 0.5, 0);
    }

    private boolean switchForWeakness(Minecraft mc) {
        if (antiWeakness.getSelectedOption().equals("None")) return false;
        if (!mc.player.hasEffect(MobEffects.WEAKNESS)) return false;

        int weaponSlot = findWeaponSlot(mc);
        if (weaponSlot == -1) return false;
        if (mc.player.getInventory().getSelectedSlot() == weaponSlot) return false;

        switchToSlot(mc, weaponSlot);
        return antiWeakness.getSelectedOption().equals("Silent");
    }

    private boolean shouldPause(Minecraft mc) {
        if (pauseMining.isEnabled() && mc.gameMode.isDestroying()) {
            return true;
        }

        if (pauseEating.isEnabled() && mc.player.isUsingItem()) {
            return true;
        }

        if (mc.player.getHealth() + mc.player.getAbsorptionAmount() < pauseHP.getValue()) {
            return true;
        }

        return false;
    }

    private boolean shouldPauseForItem(Minecraft mc) {
        if (noGapSwitch.isEnabled() && mc.player.isUsingItem()) {
            Item item = mc.player.getUseItem().getItem();
            if (item == Items.GOLDEN_APPLE || item == Items.ENCHANTED_GOLDEN_APPLE) {
                return true;
            }
        }

        if (noBowSwitch.isEnabled() && mc.player.isUsingItem()) {
            if (mc.player.getUseItem().getItem() instanceof BowItem) {
                return true;
            }
        }

        return false;
    }

    private boolean canPlaceCrystal(Minecraft mc, BlockPos pos) {
        if (!mc.level.getBlockState(pos).is(Blocks.OBSIDIAN) &&
                !mc.level.getBlockState(pos).is(Blocks.BEDROCK)) {
            return false;
        }

        BlockPos above = pos.above();
        if (!mc.level.getBlockState(above).isAir()) return false;

        if (oldPlace.isEnabled()) {
            if (!mc.level.getBlockState(above.above()).isAir()) return false;
        }

        AABB box = new AABB(above);
        if (oldPlace.isEnabled()) {
            box = box.expandTowards(0, 1, 0);
        }

        for (Entity e : mc.level.getEntities(null, box)) {
            if (e instanceof ItemEntity) continue;
            return false;
        }

        return true;
    }

    private boolean canSeeCrystal(Minecraft mc, EndCrystal crystal) {
        Vec3 start = mc.player.getEyePosition();
        Vec3 end = crystal.position().add(0, crystal.getBbHeight() / 2, 0);
        return !isLineBlocked(mc, start, end);
    }

    private boolean isLineBlocked(Minecraft mc, Vec3 start, Vec3 end) {
        ClipContext context = new ClipContext(start, end, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, mc.player);
        BlockHitResult result = mc.level.clip(context);
        return result.getType() == HitResult.Type.BLOCK;
    }

    private double calculateDamage(Vec3 crystalPos, LivingEntity target) {
        if (target == null) return 0.0;

        double distance = target.position().distanceTo(crystalPos);
        if (distance > MAX_CRYSTAL_DAMAGE_DISTANCE) return 0.0;

        double exposure = 1.0 - (distance / MAX_CRYSTAL_DAMAGE_DISTANCE);
        double impact = exposure * 2.0;
        double rawDamage = Math.floor(impact * 49.0 + 1.0);

        float armor = Math.min(20.0f, target.getArmorValue() * 0.04f);
        float finalDamage = (float) (rawDamage * (1.0f - armor));

        return Math.max(0.5, finalDamage);
    }

    private void applyRotation(Minecraft mc, Vec3 to) {
        Vec3 from = mc.player.getEyePosition();
        float[] rots = calculateRotation(from, to);
        mc.player.setYRot(rots[0]);
        mc.player.setXRot(rots[1]);
    }

    private float[] calculateRotation(Vec3 from, Vec3 to) {
        double diffX = to.x - from.x;
        double diffY = to.y - from.y;
        double diffZ = to.z - from.z;
        double diffXZ = Math.sqrt(diffX * diffX + diffZ * diffZ);

        float yaw = (float) Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0F;
        float pitch = (float) -Math.toDegrees(Math.atan2(diffY, diffXZ));

        return new float[]{yaw, pitch};
    }

    private int findCrystalSlot(Minecraft mc) {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getItem(i).is(Items.END_CRYSTAL)) {
                return i;
            }
        }
        return -1;
    }

    private int findWeaponSlot(Minecraft mc) {
        for (int i = 0; i < 9; i++) {
            Item item = mc.player.getInventory().getItem(i).getItem();
            String itemName = item.toString().toLowerCase();
            if (itemName.contains("sword") || itemName.contains("axe")) {
                return i;
            }
        }
        return -1;
    }

    private void switchToSlot(Minecraft mc, int slot) {
        if (slot == -1) return;
        if (autoSwitch.getSelectedOption().equals("Silent")) {
            mc.player.connection.send(new ServerboundSetCarriedItemPacket(slot));
        } else {
            mc.player.getInventory().setSelectedSlot(slot);
        }
    }

    private void switchBack(Minecraft mc) {
        if (hasSwitched && previousSlot != -1) {
            if (autoSwitch.getSelectedOption().equals("Silent")) {
                mc.player.connection.send(new ServerboundSetCarriedItemPacket(previousSlot));
            } else {
                mc.player.getInventory().setSelectedSlot(previousSlot);
            }
            previousSlot = -1;
            hasSwitched = false;
        }
    }
}

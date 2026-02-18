package io.client.modules.combat;

import io.client.managers.TargetManager;
import io.client.modules.templates.Category;
import io.client.modules.templates.Module;
import io.client.settings.BooleanSetting;
import io.client.settings.CategorySetting;
import io.client.settings.NumberSetting;
import io.client.settings.RadioSetting;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

public class CrystalAura extends Module {

    private static final double MAX_CRYSTAL_DAMAGE_DISTANCE = 12.0;
    private static double sq(double v) { return v * v; }

    private final CategorySetting targetCategory = new CategorySetting("Target");
    private final RadioSetting targetLogic = new RadioSetting("TargetLogic", "Distance");
    private final NumberSetting targetRange = new NumberSetting("TargetRange", 10.0F, 1.0F, 20.0F);
    private final BooleanSetting allowZombieTesting = new BooleanSetting("ZombieTesting", true);

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
    private EndCrystalEntity bestCrystal;
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
        targetCategory.addSetting(allowZombieTesting);

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
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;

        placeTimer = Math.max(0, placeTimer - 1);
        breakTimer = Math.max(0, breakTimer - 1);
        obbyTimer = Math.max(0, obbyTimer - 1);

        long worldTick = mc.world.getTime();

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
        switchBack(MinecraftClient.getInstance());
        target = null;
        bestPlacePos = null;
        bestCrystal = null;
    }

    private void doSequential(MinecraftClient mc) {
	if (bestCrystal != null && breakTimer <= 0) {
  	    breakCrystal(mc);
    	    if (sequential.getSelectedOption().equals("None")) return; 
	}
    	if (bestPlacePos != null && placeTimer <= 0) {
      	    placeCrystal(mc);
        }
    }

    private LivingEntity findTarget(MinecraftClient mc) {
        LivingEntity best = null;
        double bestValue = Double.MAX_VALUE;
        double maxRangeSq = sq(targetRange.getValue());

	for (Entity e : mc.world.getEntities()) {
    		if (e == mc.player || e instanceof EndCrystalEntity || e instanceof ItemEntity) continue;
    		if (!(e instanceof LivingEntity living)) continue;
    		if (living.getHealth() <= 0) continue;
            boolean isConfiguredTarget = TargetManager.INSTANCE.isValidTarget(e);
            boolean isZombieTestTarget = allowZombieTesting.isEnabled() && e instanceof ZombieEntity;
    		if (!isConfiguredTarget && !isZombieTestTarget) continue;

            double distSq = mc.player.squaredDistanceTo(e);
            if (distSq > maxRangeSq) continue;

            double value = targetLogic.getSelectedOption().equals("Distance") ? distSq : living.getHealth();

            if (value < bestValue) {
                bestValue = value;
                best = living;
            }
        }

        return best;
    }

    private void findBestCrystal(MinecraftClient mc) {
        bestCrystal = null;
        double bestDamage = 0;

        if (!doBreak.isEnabled()) return;
        if (target == null) return;

        double maxBreakRange = breakRange.getValue();
        double maxWallRangeSq = sq(breakWallsRange.getValue());
        long worldTick = mc.world.getTime();

        for (Entity e : mc.world.getEntities()) {
            if (!(e instanceof EndCrystalEntity crystal)) continue;

            if (crystalAge.getValue() > 0 && crystal.age < crystalAge.getValue()) continue;

            if (inhibit.isEnabled() && attackedCrystals.containsKey(crystal.getId())) {
                long timeSinceAttackTicks = worldTick - attackedCrystals.get(crystal.getId());
                if (timeSinceAttackTicks < 10L) continue;
            }

            double dist = mc.player.getEyePos().distanceTo(crystal.getPos());
            if (dist > maxBreakRange) continue;

            boolean canSee = canSeeCrystal(mc, crystal);
            if (!canSee && sq(dist) > maxWallRangeSq) continue;

            double targetDmg = calculateDamage(crystal.getPos(), target);
            double selfDmg = calculateDamage(crystal.getPos(), mc.player);

            if (!isDamageSafe(targetDmg, selfDmg, mc.player)) continue;

            if (targetDmg > bestDamage) {
                bestDamage = targetDmg;
                bestCrystal = crystal;
            }
        }
    }

    private void findBestObbyPos(MinecraftClient mc) {
        bestObbyPos = null;
        double bestScore = 0;

        if (target == null) return;

        BlockPos targetPos = target.getBlockPos();
        int range = (int) Math.ceil(obbyRange.getValue());
        double maxRangeSq = sq(obbyRange.getValue());
        int verticalRange = 2;

        for (int x = -range; x <= range; x++) {
            for (int y = -verticalRange; y <= verticalRange; y++) {
                for (int z = -range; z <= range; z++) {
                    BlockPos pos = targetPos.add(x, y, z);

                    double distSq = mc.player.getBlockPos().getSquaredDistance(pos);
                    if (distSq > maxRangeSq) continue;

                    if (!canPlaceObby(mc, pos)) continue;

                    if (obbyOnlyGround.isEnabled()) {
                        if (!mc.world.getBlockState(pos.down()).isSolid()) continue;
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

   private boolean hasObbyNearby(MinecraftClient mc, BlockPos pos) {
      for (Direction dir : Direction.values()) {
          if (dir == Direction.UP || dir == Direction.DOWN) continue;
       	 	BlockState state = mc.world.getBlockState(pos.offset(dir));
        	if (state.isOf(Blocks.OBSIDIAN) || state.isOf(Blocks.BEDROCK)) {
            		return true;
        	}
    	}
    	return false;
   }

    private double scoreObbyPosition(MinecraftClient mc, BlockPos obbyPos) {
        if (target == null) return 0;

        BlockPos crystalPos = obbyPos.up();
        Vec3d crystalVec = Vec3d.ofCenter(crystalPos).add(0, 1, 0);

        double distToPlayer = mc.player.getEyePos().distanceTo(crystalVec);
        if (distToPlayer > placeRange.getValue()) return 0;

        if (!mc.world.getBlockState(crystalPos).isAir()) return 0;
        if (!mc.world.getBlockState(crystalPos.up()).isAir() && !oldPlace.isEnabled()) return 0;

        double targetDmg = calculateDamage(crystalVec, target);
        double selfDmg = calculateDamage(crystalVec, mc.player);

        if (obbySmartPlace.isEnabled()) {
            if (targetDmg < obbyMinDamage.getValue()) return 0;
        }

        if (!isDamageSafe(targetDmg, selfDmg, mc.player)) return 0;

        double score = targetDmg;

        double targetDist = Math.sqrt(target.getBlockPos().getSquaredDistance(obbyPos));
        double playerDist = Math.sqrt(mc.player.getBlockPos().getSquaredDistance(obbyPos));

        double ratio = obbyDistanceRatio.getValue();
        score -= (targetDist * 0.5);
        score -= (playerDist * 0.5 * ratio);

        return score;
    }

    private boolean canPlaceObby(MinecraftClient mc, BlockPos pos) {
        if (!mc.world.getBlockState(pos).isReplaceable()) return false;

        if (mc.world.getBlockState(pos.down()).isOf(Blocks.OBSIDIAN)) return false;
        if (mc.world.getBlockState(pos.down()).isOf(Blocks.BEDROCK)) return false;
        if (!mc.world.getBlockState(pos.down()).isSolid()) return false;

        Box box = new Box(pos);
        for (Entity e : mc.world.getOtherEntities(null, box)) {
            if (!(e instanceof ItemEntity)) return false;
        }

        return true;
    }

    private void placeObsidian(MinecraftClient mc) {
        if (bestObbyPos == null) return;

        int obbySlot = findObsidianSlot(mc);
        if (obbySlot == -1) return;

        boolean offhand = mc.player.getOffHandStack().isOf(Items.OBSIDIAN);
        int oldSlot = mc.player.getInventory().getSelectedSlot();
        boolean needSwitch = !offhand && !mc.player.getMainHandStack().isOf(Items.OBSIDIAN);

        if (needSwitch) {
            if (autoSwitch.getSelectedOption().equals("None")) return;

            if (!autoSwitch.getSelectedOption().equals("Silent") && !hasSwitched) {
                previousSlot = oldSlot;
                hasSwitched = true;
            }

            switchToSlot(mc, obbySlot);
        }

        if (rotate.isEnabled()) {
            Vec3d vec = Vec3d.ofCenter(bestObbyPos);
            applyRotation(mc, vec);
        }

        Hand hand = offhand ? Hand.OFF_HAND : Hand.MAIN_HAND;
        Vec3d hitVec = Vec3d.ofCenter(bestObbyPos.down()).add(0, 1, 0);
        BlockHitResult result = new BlockHitResult(hitVec, Direction.UP, bestObbyPos.down(), false);

        mc.interactionManager.interactBlock(mc.player, hand, result);
        mc.player.swingHand(hand);

        obbyTimer = (int) obbyDelay.getValue();

        if (needSwitch && autoSwitch.getSelectedOption().equals("Silent")) {
            switchToSlot(mc, oldSlot);
        }
    }

    private int findObsidianSlot(MinecraftClient mc) {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).isOf(Items.OBSIDIAN)) {
                return i;
            }
        }
        return -1;
    }

    private void findBestPlacePos(MinecraftClient mc) {
        bestPlacePos = null;
        double bestDamage = 0;

        if (!doPlace.isEnabled()) return;
        if (target == null) return;

        BlockPos playerPos = mc.player.getBlockPos();
        int horizontalRange = (int) Math.ceil(placeRange.getValue());
        int verticalRange = 2;

        double placeRangeVal = placeRange.getValue();
        double placeWallsRangeVal = placeWallsRange.getValue();

        for (int x = -horizontalRange; x <= horizontalRange; x++) {
            for (int y = -verticalRange; y <= verticalRange; y++) {
                for (int z = -horizontalRange; z <= horizontalRange; z++) {
                    BlockPos pos = playerPos.add(x, y, z);

                    if (blacklist.isEnabled() && blacklistedPos.containsKey(pos)) continue;

                    if (!canPlaceCrystal(mc, pos)) continue;

                    Vec3d crystalVec = Vec3d.ofCenter(pos).add(0, 1, 0);
                    double dist = mc.player.getEyePos().distanceTo(crystalVec);

                    if (dist > placeRangeVal) continue;

                    boolean canSee = !isLineBlocked(mc, mc.player.getEyePos(), crystalVec);
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

    private boolean isDamageSafe(double targetDmg, double selfDmg, PlayerEntity player) {
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

    private void breakCrystal(MinecraftClient mc) {
        if (bestCrystal == null) return;

        int oldSlot = mc.player.getInventory().getSelectedSlot();
        boolean switched = switchForWeakness(mc);

        if (rotate.isEnabled()) {
            Vec3d vec = bestCrystal.getPos().add(0, bestCrystal.getHeight() / 2, 0);
            applyRotation(mc, vec);
        }

        mc.interactionManager.attackEntity(mc.player, bestCrystal);
        mc.player.swingHand(Hand.MAIN_HAND);

        attackedCrystals.put(bestCrystal.getId(), mc.world.getTime());
        breakTimer = (int) breakDelay.getValue();

        if (switched) {
            switchToSlot(mc, oldSlot);
        }
    }

    private void placeCrystal(MinecraftClient mc) {
        if (bestPlacePos == null) return;

        boolean offhand = mc.player.getOffHandStack().isOf(Items.END_CRYSTAL);

        if (!offhand && !mc.player.getMainHandStack().isOf(Items.END_CRYSTAL)) {
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
            Vec3d vec = getPlaceVec(bestPlacePos);
            applyRotation(mc, vec);
        }

        Hand hand = offhand ? Hand.OFF_HAND : Hand.MAIN_HAND;
        Vec3d hitVec = Vec3d.ofCenter(bestPlacePos).add(0, 1, 0);
        BlockHitResult result = new BlockHitResult(hitVec, Direction.UP, bestPlacePos, false);

        mc.interactionManager.interactBlock(mc.player, hand, result);
        mc.player.swingHand(hand);

        placeTimer = (int) placeDelay.getValue();

        if (blacklist.isEnabled()) {
            long expiration = mc.world.getTime() + 4L;
            blacklistedPos.put(bestPlacePos, expiration);
        }
    }

    private Vec3d getPlaceVec(BlockPos pos) {
        if (rotateType.getSelectedOption().equals("Center")) {
            return Vec3d.ofCenter(pos).add(0, 1, 0);
        }
        return Vec3d.ofCenter(pos).add(0, 0.5, 0);
    }

    private boolean switchForWeakness(MinecraftClient mc) {
        if (antiWeakness.getSelectedOption().equals("None")) return false;
        if (!mc.player.hasStatusEffect(StatusEffects.WEAKNESS)) return false;

        int weaponSlot = findWeaponSlot(mc);
        if (weaponSlot == -1) return false;
        if (mc.player.getInventory().getSelectedSlot() == weaponSlot) return false;

        switchToSlot(mc, weaponSlot);
        return antiWeakness.getSelectedOption().equals("Silent");
    }

    private boolean shouldPause(MinecraftClient mc) {
        if (pauseMining.isEnabled() && mc.interactionManager.isBreakingBlock()) {
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

    private boolean shouldPauseForItem(MinecraftClient mc) {
        if (noGapSwitch.isEnabled() && mc.player.isUsingItem()) {
            Item item = mc.player.getActiveItem().getItem();
            if (item == Items.GOLDEN_APPLE || item == Items.ENCHANTED_GOLDEN_APPLE) {
                return true;
            }
        }

        if (noBowSwitch.isEnabled() && mc.player.isUsingItem()) {
            if (mc.player.getActiveItem().getItem() instanceof BowItem) {
                return true;
            }
        }

        return false;
    }

    private boolean canPlaceCrystal(MinecraftClient mc, BlockPos pos) {
        if (!mc.world.getBlockState(pos).isOf(Blocks.OBSIDIAN) && !mc.world.getBlockState(pos).isOf(Blocks.BEDROCK)) {
            return false;
        }

        BlockPos above = pos.up();
        if (!mc.world.getBlockState(above).isAir()) return false;

        // 1.13+ requires two air blocks; 1.12 mode allows the older single-block rule.
        if (!oldPlace.isEnabled() && !mc.world.getBlockState(above.up()).isAir()) {
            return false;
        }

        Box box = new Box(above);
        if (!oldPlace.isEnabled()) {
            box = box.stretch(0, 1, 0);
        }

        for (Entity e : mc.world.getOtherEntities(null, box)) {
            if (e instanceof ItemEntity) continue;
            return false;
        }

        return true;
    }

    private boolean canSeeCrystal(MinecraftClient mc, EndCrystalEntity crystal) {
        Vec3d start = mc.player.getEyePos();
        Vec3d end = crystal.getPos().add(0, crystal.getHeight() / 2, 0);
        return !isLineBlocked(mc, start, end);
    }

    private boolean isLineBlocked(MinecraftClient mc, Vec3d start, Vec3d end) {
        RaycastContext context = new RaycastContext(start, end, RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE, mc.player);
        BlockHitResult result = mc.world.raycast(context);
        return result.getType() == HitResult.Type.BLOCK;
    }

    private double calculateDamage(Vec3d crystalPos, LivingEntity target) {
        if (target == null) return 0.0;

        double distance = target.getPos().distanceTo(crystalPos);
        if (distance > MAX_CRYSTAL_DAMAGE_DISTANCE) return 0.0;

        double exposure = 1.0 - (distance / MAX_CRYSTAL_DAMAGE_DISTANCE);
        double impact = exposure * 2.0;
        double rawDamage = Math.floor(impact * 49.0 + 1.0);

        float armor = Math.min(20.0f, target.getArmor() * 0.04f);
        float finalDamage = (float) (rawDamage * (1.0f - armor));

        return Math.max(0.5, finalDamage);
    }

    private void applyRotation(MinecraftClient mc, Vec3d to) {
        Vec3d from = mc.player.getEyePos();
        float[] rots = calculateRotation(from, to);
        mc.player.setYaw(rots[0]);
        mc.player.setPitch(rots[1]);
    }

    private float[] calculateRotation(Vec3d from, Vec3d to) {
        double diffX = to.x - from.x;
        double diffY = to.y - from.y;
        double diffZ = to.z - from.z;
        double diffXZ = Math.sqrt(diffX * diffX + diffZ * diffZ);

        float yaw = (float) Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0F;
        float pitch = (float) -Math.toDegrees(Math.atan2(diffY, diffXZ));

        return new float[]{yaw, pitch};
    }

    private int findCrystalSlot(MinecraftClient mc) {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).isOf(Items.END_CRYSTAL)) {
                return i;
            }
        }
        return -1;
    }

    private int findWeaponSlot(MinecraftClient mc) {
        for (int i = 0; i < 9; i++) {
            Item item = mc.player.getInventory().getStack(i).getItem();
            String itemName = item.toString().toLowerCase();
            if (itemName.contains("sword") || itemName.contains("axe")) {
                return i;
            }
        }
        return -1;
    }

    private void switchToSlot(MinecraftClient mc, int slot) {
        if (slot == -1) return;
        if (autoSwitch.getSelectedOption().equals("Silent")) {
            mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(slot));
        } else {
            mc.player.getInventory().setSelectedSlot(slot);
        }
    }

    private void switchBack(MinecraftClient mc) {
        if (!hasSwitched || previousSlot == -1) return; 
 	if (autoSwitch.getSelectedOption().equals("Silent")) {
                mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(previousSlot));
    	} else {
             mc.player.getInventory().setSelectedSlot(previousSlot);
	    }        
            previousSlot = -1;
            hasSwitched = false;
    }
}




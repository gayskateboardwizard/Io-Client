package io.client.modules.combat;

import io.client.Category;
import io.client.Module;
import io.client.TargetManager;
import io.client.settings.BooleanSetting;
import io.client.settings.CategorySetting;
import io.client.settings.NumberSetting;
import io.client.settings.RadioSetting;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.stream.Stream;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

public class Killaura extends Module {
    public final CategorySetting general = new CategorySetting("General");
    public final NumberSetting range = new NumberSetting("Range", 6.0F, 3.0F, 6.0F);
    public final NumberSetting wallsRange = new NumberSetting("WallsRange", 3.0F, 0.0F, 6.0F);
    public final RadioSetting sorting = new RadioSetting("Sorting", "Distance");
    public final RadioSetting multiTask = new RadioSetting("MultiTask", "None");
    public final RadioSetting delayMode = new RadioSetting("Delay", "Cooldown");
    public final NumberSetting attackSpeed = new NumberSetting("AttackSpeed", 20.0F, 0.1F, 20.0F);
    public final BooleanSetting autoSwitch = new BooleanSetting("AutoSwitch", false);
    public final BooleanSetting onlySword = new BooleanSetting("OnlyWeapon", true);
    public final BooleanSetting rotate = new BooleanSetting("Rotate", true);
    public final RadioSetting rotateType = new RadioSetting("RotateType", "Auto");
    public final NumberSetting sensitivity = new NumberSetting("Sensitivity", 15.0F, 1.0F, 50.0F);

    public final CategorySetting humanize = new CategorySetting("Humanize");
    public final BooleanSetting enabled = new BooleanSetting("Enabled", true);
    public final NumberSetting reactionDelay = new NumberSetting("ReactionDelay", 120.0F, 0.0F, 1000.0F);
    public final NumberSetting focusDelay = new NumberSetting("FocusDelay", 80.0F, 0.0F, 500.0F);
    public final BooleanSetting smoothMovement = new BooleanSetting("SmoothMovement", true);
    public final NumberSetting smoothSpeed = new NumberSetting("SmoothSpeed", 6.0F, 1.0F, 20.0F);
    public final NumberSetting smoothVariance = new NumberSetting("SmoothVariance", 15.0F, 0.0F, 50.0F);
    public final NumberSetting minMovement = new NumberSetting("MinMovement", 0.8F, 0.0F, 5.0F);
    public final NumberSetting movementDelay = new NumberSetting("MovementDelay", 35.0F, 0.0F, 200.0F);
    public final BooleanSetting accelerationCurve = new BooleanSetting("AccelerationCurve", true);
    public final NumberSetting acceleration = new NumberSetting("Acceleration", 1.5F, 0.5F, 3.0F);
    public final BooleanSetting microDrift = new BooleanSetting("MicroDrift", true);
    public final NumberSetting driftIntensity = new NumberSetting("DriftIntensity", 0.4F, 0.0F, 2.0F);
    public final NumberSetting driftSpeed = new NumberSetting("DriftSpeed", 0.88F, 0.5F, 0.99F);
    public final BooleanSetting movementDrift = new BooleanSetting("MovementDrift", true);
    public final NumberSetting driftAmount = new NumberSetting("DriftAmount", 1.2F, 0.0F, 5.0F);
    public final BooleanSetting overshoot = new BooleanSetting("Overshoot", true);
    public final NumberSetting overshootChance = new NumberSetting("OvershootChance", 12.0F, 0.0F, 100.0F);
    public final NumberSetting overshootAmount = new NumberSetting("OvershootAmount", 1.18F, 1.0F, 2.0F);
    public final BooleanSetting skipAttacks = new BooleanSetting("SkipAttacks", true);
    public final NumberSetting skipRatio = new NumberSetting("SkipRatio", 4.0F, 1.0F, 10.0F);
    public final BooleanSetting variableTiming = new BooleanSetting("VariableTiming", true);
    public final NumberSetting timingVariation = new NumberSetting("TimingVariation", 12.0F, 0.0F, 50.0F);
    public final NumberSetting alignmentThreshold = new NumberSetting("AlignmentThreshold", 4.5F, 0.5F, 15.0F);
    public final BooleanSetting targetPrediction = new BooleanSetting("TargetPrediction", false);
    public final NumberSetting predictionStrength = new NumberSetting("PredictionStrength", 0.3F, 0.0F, 1.0F);

    private Entity target = null;
    private long lastHitTime = 0;
    private int previousSlot = -1;
    private boolean hasSwitched = false;
    private long reactionStartTime = 0;
    private boolean reactionDelayActive = false;
    private int attackCounter = 0;
    private float targetYaw = 0;
    private float targetPitch = 0;
    private float currentYaw = 0;
    private float currentPitch = 0;
    private final Random random = new Random();
    private Entity lastTarget = null;
    private float microOffsetYaw = 0f;
    private float microOffsetPitch = 0f;
    private float rotationRamp = 0f;
    private long lastDriftUpdate = 0;
    private long nextAttackDelay = 0;
    private long lastMovementTime = 0;
    private long focusStartTime = 0;
    private boolean focusDelayActive = false;
    private float[] lastTargetVelocity = new float[]{0f, 0f, 0f};

    public Killaura() {
        super("KillAura", "Automates PVP", -1, Category.COMBAT);

        sorting.addOption("Distance");
        sorting.addOption("FOV");
        sorting.addOption("Health");

        multiTask.addOption("None");
        multiTask.addOption("Soft");
        multiTask.addOption("Strong");

        delayMode.addOption("Cooldown");
        delayMode.addOption("Delay");

        rotateType.addOption("Auto");
        rotateType.addOption("Head");
        rotateType.addOption("Torso");
        rotateType.addOption("Feet");

        general.addSetting(range);
        general.addSetting(wallsRange);
        general.addSetting(sorting);
        general.addSetting(multiTask);
        general.addSetting(delayMode);
        general.addSetting(attackSpeed);
        general.addSetting(autoSwitch);
        general.addSetting(onlySword);
        general.addSetting(rotate);
        general.addSetting(rotateType);
        general.addSetting(sensitivity);
        addSetting(general);

        humanize.addSetting(enabled);
        humanize.addSetting(reactionDelay);
        humanize.addSetting(focusDelay);
        humanize.addSetting(smoothMovement);
        humanize.addSetting(smoothSpeed);
        humanize.addSetting(smoothVariance);
        humanize.addSetting(minMovement);
        humanize.addSetting(movementDelay);
        humanize.addSetting(accelerationCurve);
        humanize.addSetting(acceleration);
        humanize.addSetting(microDrift);
        humanize.addSetting(driftIntensity);
        humanize.addSetting(driftSpeed);
        humanize.addSetting(movementDrift);
        humanize.addSetting(driftAmount);
        humanize.addSetting(overshoot);
        humanize.addSetting(overshootChance);
        humanize.addSetting(overshootAmount);
        humanize.addSetting(skipAttacks);
        humanize.addSetting(skipRatio);
        humanize.addSetting(variableTiming);
        humanize.addSetting(timingVariation);
        humanize.addSetting(alignmentThreshold);
        humanize.addSetting(targetPrediction);
        humanize.addSetting(predictionStrength);
        addSetting(humanize);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) {
            currentYaw = mc.player.getYaw();
            currentPitch = mc.player.getPitch();
        }
        rotationRamp = 1.0f;
        microOffsetYaw = 0f;
        microOffsetPitch = 0f;
        lastMovementTime = System.currentTimeMillis();
    }

    @Override
    public void onUpdate() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;

        if (mc.player.isUsingItem()) {
            switch (multiTask.getSelectedOption()) {
                case "Soft":
                    if (!mc.player.getActiveHand().equals(Hand.OFF_HAND)) {
                        target = null;
                        switchBack(mc);
                        reactionDelayActive = false;
                        focusDelayActive = false;
                        return;
                    }
                    break;
                case "Strong":
                    target = null;
                    switchBack(mc);
                    reactionDelayActive = false;
                    focusDelayActive = false;
                    return;
            }
        }

        Entity newTarget = findTarget(mc);
        if (newTarget == null) {
            target = null;
            switchBack(mc);
            reactionDelayActive = false;
            focusDelayActive = false;
            rotationRamp = 1.0f;
            return;
        }

        if (target != newTarget) {
            if (enabled.isEnabled() && reactionDelay.getValue() > 0) {
                reactionDelayActive = true;
                reactionStartTime = System.currentTimeMillis();
            }
            if (enabled.isEnabled() && focusDelay.getValue() > 0) {
                focusDelayActive = true;
                focusStartTime = System.currentTimeMillis();
            }
            target = newTarget;
            rotationRamp = 0f;
            lastTargetVelocity = new float[]{0f, 0f, 0f};
        } else if (target == null) {
            target = newTarget;
            if (enabled.isEnabled() && reactionDelay.getValue() > 0) {
                reactionDelayActive = true;
                reactionStartTime = System.currentTimeMillis();
            }
            if (enabled.isEnabled() && focusDelay.getValue() > 0) {
                focusDelayActive = true;
                focusStartTime = System.currentTimeMillis();
            }
            rotationRamp = 0f;
            lastTargetVelocity = new float[]{0f, 0f, 0f};
        }

        if (reactionDelayActive) {
            long elapsed = System.currentTimeMillis() - reactionStartTime;
            if (elapsed < reactionDelay.getValue()) {
                return;
            }
            reactionDelayActive = false;
        }

        if (onlySword.isEnabled() && !isHoldingWeapon(mc)) {
            if (!autoSwitch.isEnabled()) {
                target = null;
                switchBack(mc);
                return;
            }
        }

        if (autoSwitch.isEnabled()) {
            switchWeapon(mc);
        }

        boolean canAttack = false;
        if (delayMode.getSelectedOption().equals("Cooldown")) {
            canAttack = mc.player.getAttackCooldownProgress(0.0F) >= 1.0F;
        } else {
            long currentTime = System.currentTimeMillis();

            if (nextAttackDelay == 0 || currentTime - lastHitTime >= nextAttackDelay) {
                canAttack = true;

                if (enabled.isEnabled() && variableTiming.isEnabled()) {
                    double baseDelay = 1000.0 / attackSpeed.getValue();
                    double variation = baseDelay * (timingVariation.getValue() / 100.0);
                    nextAttackDelay = (long)(baseDelay + (random.nextDouble() - 0.5) * variation);
                } else {
                    nextAttackDelay = (long)(1000.0 / attackSpeed.getValue());
                }
            }
        }

        if (enabled.isEnabled() && skipAttacks.isEnabled() && canAttack) {
            attackCounter++;
            int ratio = (int) skipRatio.getValue();
            if (attackCounter % (ratio + 1) == 0) {
                canAttack = false;
            }
        }

        Vec3d targetVec = getAttackRotateVec(target, mc);

        if (enabled.isEnabled() && targetPrediction.isEnabled()) {
            Vec3d velocity = target.getVelocity();
            float predStr = predictionStrength.getValue();

            lastTargetVelocity[0] = lastTargetVelocity[0] * 0.7f + (float)velocity.x * 0.3f;
            lastTargetVelocity[1] = lastTargetVelocity[1] * 0.7f + (float)velocity.y * 0.3f;
            lastTargetVelocity[2] = lastTargetVelocity[2] * 0.7f + (float)velocity.z * 0.3f;

            targetVec = targetVec.add(
                    lastTargetVelocity[0] * predStr * 3,
                    lastTargetVelocity[1] * predStr * 3,
                    lastTargetVelocity[2] * predStr * 3
            );
        }

        if (rotate.isEnabled()) {
            if (focusDelayActive) {
                long elapsed = System.currentTimeMillis() - focusStartTime;
                if (elapsed < focusDelay.getValue()) {
                    return;
                }
                focusDelayActive = false;
            }

            float[] rotation = calculateRotation(mc.player.getEyePos(), targetVec);

            if (target != lastTarget) {
                targetYaw = rotation[0];
                targetPitch = rotation[1];
                lastTarget = target;
                currentYaw = mc.player.getYaw();
                currentPitch = mc.player.getPitch();
            } else {
                targetYaw = rotation[0];
                targetPitch = rotation[1];
            }

            long currentTime = System.currentTimeMillis();
            if (enabled.isEnabled() && microDrift.isEnabled() && currentTime - lastDriftUpdate > 50) {
                float intensity = driftIntensity.getValue();
                microOffsetYaw += (random.nextFloat() - 0.5f) * intensity * 0.15f;
                microOffsetPitch += (random.nextFloat() - 0.5f) * intensity * 0.12f;

                float decay = driftSpeed.getValue();
                microOffsetYaw *= decay;
                microOffsetPitch *= decay;

                lastDriftUpdate = currentTime;
            }

            if (enabled.isEnabled() && movementDrift.isEnabled() && mc.player.getVelocity().horizontalLengthSquared() > 0.01) {
                float driftOffset = (random.nextFloat() - 0.5F) * driftAmount.getValue();
                targetYaw += driftOffset;
                targetPitch += driftOffset * 0.4F;
            }

            if (enabled.isEnabled() && smoothMovement.isEnabled()) {
                float yawDiff = normalizeAngle(targetYaw - currentYaw);
                float pitchDiff = targetPitch - currentPitch;

                if (microDrift.isEnabled()) {
                    yawDiff += microOffsetYaw;
                    pitchDiff += microOffsetPitch;
                }

                float totalDiff = Math.abs(yawDiff) + Math.abs(pitchDiff);
                if (totalDiff < minMovement.getValue()) {
                    return;
                }

                if (movementDelay.getValue() > 0) {
                    if (currentTime - lastMovementTime < movementDelay.getValue()) {
                        return;
                    }
                    lastMovementTime = currentTime;
                }

                float yawStep, pitchStep;

                if (accelerationCurve.isEnabled()) {
                    float yawDistanceFactor = Math.min(Math.abs(yawDiff) / 90f, 1f);
                    float pitchDistanceFactor = Math.min(Math.abs(pitchDiff) / 45f, 1f);

                    float variance = 1.0f;
                    if (smoothVariance.getValue() > 0) {
                        variance = 1.0f + ((random.nextFloat() - 0.5f) * 2f * (smoothVariance.getValue() / 100f));
                    }

                    float accel = acceleration.getValue();
                    float dynamicYawSpeed = (smoothSpeed.getValue() * variance) * (0.3f + 0.7f * (float)Math.pow(yawDistanceFactor, accel));
                    float dynamicPitchSpeed = (smoothSpeed.getValue() * variance) * (0.3f + 0.7f * (float)Math.pow(pitchDistanceFactor, accel));

                    yawStep = yawDiff / dynamicYawSpeed;
                    pitchStep = pitchDiff / dynamicPitchSpeed;

                    if (Math.abs(yawDiff) < 10f) {
                        yawStep *= 0.55f;
                    }
                    if (Math.abs(pitchDiff) < 8f) {
                        pitchStep *= 0.55f;
                    }
                } else {
                    yawStep = yawDiff / smoothSpeed.getValue();
                    pitchStep = pitchDiff / smoothSpeed.getValue();
                }

                if (rotationRamp < 1.0f) {
                    rotationRamp = Math.min(rotationRamp + 0.12f, 1.0f);
                    yawStep *= rotationRamp;
                    pitchStep *= rotationRamp;
                }

                float maxStep = sensitivity.getValue();
                yawStep = Math.max(-maxStep, Math.min(maxStep, yawStep));
                pitchStep = Math.max(-maxStep, Math.min(maxStep, pitchStep));

                if (overshoot.isEnabled()) {
                    if (Math.abs(yawDiff) < 3f && Math.abs(yawDiff) > 0.5f) {
                        if (random.nextFloat() * 100f < overshootChance.getValue()) {
                            yawStep *= overshootAmount.getValue();
                        }
                    }
                    if (Math.abs(pitchDiff) < 3f && Math.abs(pitchDiff) > 0.5f) {
                        if (random.nextFloat() * 100f < overshootChance.getValue()) {
                            pitchStep *= overshootAmount.getValue();
                        }
                    }
                }

                currentYaw += yawStep;
                currentPitch += pitchStep;

                currentYaw = normalizeAngle(currentYaw);
                currentPitch = Math.max(-90.0F, Math.min(90.0F, currentPitch));

                mc.player.setYaw(currentYaw);
                mc.player.setPitch(currentPitch);
            } else {
                mc.player.setYaw(normalizeAngle(targetYaw));
                mc.player.setPitch(Math.max(-90.0F, Math.min(90.0F, targetPitch)));
                currentYaw = normalizeAngle(targetYaw);
                currentPitch = Math.max(-90.0F, Math.min(90.0F, targetPitch));
            }
        }

        if (canAttack && isCrosshairAligned(mc, targetVec)) {
            mc.interactionManager.attackEntity(mc.player, target);
            mc.player.swingHand(Hand.MAIN_HAND);
            lastHitTime = System.currentTimeMillis();
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) {
            switchBack(mc);
        }
        target = null;
        lastTarget = null;
        reactionDelayActive = false;
        focusDelayActive = false;
        attackCounter = 0;
        rotationRamp = 1.0f;
        microOffsetYaw = 0f;
        microOffsetPitch = 0f;
    }

    private boolean isCrosshairAligned(MinecraftClient mc, Vec3d targetVec) {
        if (!enabled.isEnabled() || !smoothMovement.isEnabled()) {
            return true;
        }

        Vec3d eyePos = mc.player.getEyePos();
        float[] neededRot = calculateRotation(eyePos, targetVec);

        float yawDiff = Math.abs(normalizeAngle(neededRot[0] - mc.player.getYaw()));
        float pitchDiff = Math.abs(neededRot[1] - mc.player.getPitch());

        return (yawDiff + pitchDiff) <= alignmentThreshold.getValue();
    }

    private float normalizeAngle(float angle) {
        while (angle > 180.0F) angle -= 360.0F;
        while (angle < -180.0F) angle += 360.0F;
        return angle;
    }

    private Entity findTarget(MinecraftClient mc) {
        Vec3d playerPos = mc.player.getEyePos();
        Box searchBox = new Box(
                playerPos.x - range.getValue(),
                playerPos.y - range.getValue(),
                playerPos.z - range.getValue(),
                playerPos.x + range.getValue(),
                playerPos.y + range.getValue(),
                playerPos.z + range.getValue()
        );

        List<Entity> entities = mc.world.getOtherEntities(mc.player, searchBox);

        Stream<Entity> entityStream = entities.stream()
                .filter(e -> e instanceof LivingEntity)
                .filter(e -> e != mc.player)
                .filter(e -> ((LivingEntity) e).isAlive())
                .filter(e -> TargetManager.INSTANCE.isValidTarget(e))
                .filter(e -> isInAttackRange(playerPos, e, mc));

        switch (sorting.getSelectedOption()) {
            case "Distance":
                return entityStream
                        .min(Comparator.comparingDouble(e -> mc.player.distanceTo(e)))
                        .orElse(null);
            case "Health":
                return entityStream
                        .min(Comparator.comparingDouble(e -> ((LivingEntity) e).getHealth()))
                        .orElse(null);
            default:
                return entityStream
                        .min(Comparator.comparingDouble(e -> {
                            Vec3d targetVec = getAttackRotateVec(e, mc);
                            float[] rot = calculateRotation(playerPos, targetVec);
                            float yawDiff = Math.abs(normalizeAngle(rot[0] - mc.player.getYaw()));
                            float pitchDiff = Math.abs(rot[1] - mc.player.getPitch());
                            return yawDiff + pitchDiff;
                        }))
                        .orElse(null);
        }
    }

    private boolean isInAttackRange(Vec3d pos, Entity entity, MinecraftClient mc) {
        Vec3d targetVec = getAttackRotateVec(entity, mc);
        double dist = pos.distanceTo(targetVec);

        if (dist > range.getValue()) {
            return false;
        }

        BlockHitResult result = mc.world.raycast(new RaycastContext(
                pos,
                targetVec,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                mc.player
        ));

        return result.getType() == HitResult.Type.MISS || dist <= wallsRange.getValue();
    }

    private Vec3d getAttackRotateVec(Entity entity, MinecraftClient mc) {
        Vec3d feetPos = entity.getPos();

        return switch (rotateType.getSelectedOption()) {
            case "Feet" -> feetPos;
            case "Torso" -> feetPos.add(0.0, entity.getHeight() / 2.0, 0.0);
            case "Head" -> entity.getEyePos();
            case "Auto" -> {
                Vec3d torsoPos = feetPos.add(0.0, entity.getHeight() / 2.0, 0.0);
                Vec3d eyesPos = entity.getEyePos();
                Vec3d playerEye = mc.player.getEyePos();

                yield Stream.of(feetPos, torsoPos, eyesPos)
                        .min(Comparator.comparing(v -> playerEye.squaredDistanceTo(v)))
                        .orElse(eyesPos);
            }
            default -> entity.getEyePos();
        };
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

    private boolean isHoldingWeapon(MinecraftClient mc) {
        ItemStack stack = mc.player.getMainHandStack();
        return stack.getItem().toString().toLowerCase().contains("sword") ||
                stack.getItem().toString().toLowerCase().contains("axe") ||
                stack.getItem() == Items.TRIDENT;
    }

    private void switchWeapon(MinecraftClient mc) {
        int weaponSlot = findBestWeaponSlot(mc);
        int currentSlot = mc.player.getInventory().getSelectedSlot();

        if (weaponSlot != -1 && weaponSlot != currentSlot) {
            if (!hasSwitched) {
                previousSlot = currentSlot;
                hasSwitched = true;
            }
            mc.player.getInventory().setSelectedSlot(weaponSlot);
        }
    }

    private int findBestWeaponSlot(MinecraftClient mc) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.isEmpty()) continue;

            String itemName = stack.getItem().toString().toLowerCase();
            if (itemName.contains("sword") || itemName.contains("axe") ||
                    stack.getItem() == Items.TRIDENT) {
                return i;
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
}


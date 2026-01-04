package io.client.modules;

import io.client.Category;
import io.client.Module;
import io.client.TargetManager;
import io.client.settings.BooleanSetting;
import io.client.settings.NumberSetting;
import io.client.settings.RadioSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public class Killaura extends Module {
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

    private Entity target = null;
    private long lastHitTime = 0;
    private int previousSlot = -1;
    private boolean hasSwitched = false;

    public Killaura() {
        super("KillAura", "Automates PVP", -1, Category.COMBAT);

        sorting.addOption("Distance");
        sorting.addOption("FOV");

        multiTask.addOption("None");
        multiTask.addOption("Soft");
        multiTask.addOption("Strong");

        delayMode.addOption("Cooldown");
        delayMode.addOption("Delay");

        rotateType.addOption("Auto");
        rotateType.addOption("Head");
        rotateType.addOption("Torso");
        rotateType.addOption("Feet");

        addSetting(range);
        addSetting(wallsRange);
        addSetting(sorting);
        addSetting(multiTask);
        addSetting(delayMode);
        addSetting(attackSpeed);
        addSetting(autoSwitch);
        addSetting(onlySword);
        addSetting(rotate);
        addSetting(rotateType);
    }

    @Override
    public void onUpdate() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        if (mc.player.isUsingItem()) {
            switch (multiTask.getSelectedOption()) {
                case "Soft":
                    if (!mc.player.getUsedItemHand().equals(InteractionHand.OFF_HAND)) {
                        target = null;
                        switchBack(mc);
                        return;
                    }
                    break;
                case "Strong":
                    target = null;
                    switchBack(mc);
                    return;
            }
        }

        Entity newTarget = findTarget(mc);
        if (newTarget == null) {
            target = null;
            switchBack(mc);
            return;
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

        target = newTarget;

        boolean canAttack = false;
        if (delayMode.getSelectedOption().equals("Cooldown")) {
            canAttack = mc.player.getAttackStrengthScale(0.0F) >= 1.0F;
        } else {
            long currentTime = System.currentTimeMillis();
            long delay = (long) (1000.0 / attackSpeed.getValue());
            if (currentTime - lastHitTime >= delay) {
                canAttack = true;
                lastHitTime = currentTime;
            }
        }

        if (rotate.isEnabled() && canAttack) {
            Vec3 targetVec = getAttackRotateVec(target, mc);
            float[] rotation = calculateRotation(mc.player.getEyePosition(), targetVec);
            mc.player.setYRot(rotation[0]);
            mc.player.setXRot(rotation[1]);
        }

        if (canAttack) {
            mc.gameMode.attack(mc.player, target);
            mc.player.swing(InteractionHand.MAIN_HAND);
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            switchBack(mc);
        }
        target = null;
    }

    private Entity findTarget(Minecraft mc) {
        Vec3 playerPos = mc.player.getEyePosition();
        AABB searchBox = new AABB(
                playerPos.x - range.getValue(),
                playerPos.y - range.getValue(),
                playerPos.z - range.getValue(),
                playerPos.x + range.getValue(),
                playerPos.y + range.getValue(),
                playerPos.z + range.getValue()
        );

        List<Entity> entities = mc.level.getEntities(mc.player, searchBox);

        Stream<Entity> entityStream = entities.stream()
                .filter(e -> e instanceof LivingEntity)
                .filter(e -> e != mc.player)
                .filter(e -> ((LivingEntity) e).isAlive())
                .filter(e -> TargetManager.INSTANCE.isValidTarget(e))
                .filter(e -> isInAttackRange(playerPos, e, mc));

        if (sorting.getSelectedOption().equals("Distance")) {
            return entityStream
                    .min(Comparator.comparingDouble(e -> mc.player.distanceTo(e)))
                    .orElse(null);
        } else {
            return entityStream
                    .min(Comparator.comparingDouble(e -> {
                        Vec3 targetVec = getAttackRotateVec(e, mc);
                        float[] rot = calculateRotation(playerPos, targetVec);
                        float yawDiff = Math.abs(rot[0] - mc.player.getYRot());
                        float pitchDiff = Math.abs(rot[1] - mc.player.getXRot());
                        return yawDiff + pitchDiff;
                    }))
                    .orElse(null);
        }
    }

    private boolean isInAttackRange(Vec3 pos, Entity entity, Minecraft mc) {
        Vec3 targetVec = getAttackRotateVec(entity, mc);
        double dist = pos.distanceTo(targetVec);

        if (dist > range.getValue()) {
            return false;
        }

        BlockHitResult result = mc.level.clip(new ClipContext(
                pos,
                targetVec,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                mc.player
        ));

        return result.getType() == HitResult.Type.MISS || dist <= wallsRange.getValue();
    }

    private Vec3 getAttackRotateVec(Entity entity, Minecraft mc) {
        Vec3 feetPos = entity.position();

        return switch (rotateType.getSelectedOption()) {
            case "Feet" -> feetPos;
            case "Torso" -> feetPos.add(0.0, entity.getBbHeight() / 2.0, 0.0);
            case "Head" -> entity.getEyePosition();
            case "Auto" -> {
                Vec3 torsoPos = feetPos.add(0.0, entity.getBbHeight() / 2.0, 0.0);
                Vec3 eyesPos = entity.getEyePosition();
                Vec3 playerEye = mc.player.getEyePosition();

                if (playerEye.distanceToSqr(eyesPos) < 0.2) {
                    yield feetPos;
                }

                yield Stream.of(feetPos, torsoPos, eyesPos)
                        .min(Comparator.comparing(v -> playerEye.distanceToSqr(v)))
                        .orElse(eyesPos);
            }
            default -> entity.getEyePosition();
        };
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

    private boolean isHoldingWeapon(Minecraft mc) {
        ItemStack stack = mc.player.getMainHandItem();
        return stack.getItem().toString().toLowerCase().contains("sword") ||
                stack.getItem().toString().toLowerCase().contains("axe") ||
                stack.getItem() == Items.TRIDENT;
    }

    private void switchWeapon(Minecraft mc) {
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

    private int findBestWeaponSlot(Minecraft mc) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (stack.isEmpty()) continue;

            String itemName = stack.getItem().toString().toLowerCase();
            if (itemName.contains("sword") || itemName.contains("axe") ||
                    stack.getItem() == Items.TRIDENT) {
                return i;
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
}
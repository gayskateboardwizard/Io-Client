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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public class WebAura extends Module {
    private final CategorySetting general;
    private final NumberSetting range;
    private final NumberSetting wallsRange;
    private final NumberSetting delay;
    private final BooleanSetting rotate;
    private final RadioSetting mode;
    private final RadioSetting sorting;
    private final BooleanSetting onlyPlayers;

    private final CategorySetting placement;
    private final BooleanSetting feet;
    private final BooleanSetting surround;
    private final BooleanSetting doubles;

    private final CategorySetting prediction;
    private final BooleanSetting predictMovement;
    private final NumberSetting predictTicks;

    private int tickCounter;
    private LivingEntity target;

    public WebAura() {
        super("WebAura", "Places webs on targets", -1, Category.COMBAT);

        general = new CategorySetting("General");
        addSetting(general);

        mode = new RadioSetting("Mode", "Target");
        mode.addOption("Target");
        mode.addOption("Self");
        general.addSetting(mode);

        range = new NumberSetting("Range", 5f, 1f, 7f);
        general.addSetting(range);

        wallsRange = new NumberSetting("WallsRange", 3f, 0f, 7f);
        general.addSetting(wallsRange);

        delay = new NumberSetting("Delay", 10f, 0f, 20f);
        general.addSetting(delay);

        rotate = new BooleanSetting("Rotate", true);
        general.addSetting(rotate);

        sorting = new RadioSetting("Sorting", "Distance");
        sorting.addOption("Distance");
        sorting.addOption("FOV");
        general.addSetting(sorting);

        onlyPlayers = new BooleanSetting("OnlyPlayers", false);
        general.addSetting(onlyPlayers);

        placement = new CategorySetting("Placement");
        addSetting(placement);

        feet = new BooleanSetting("Feet", true);
        placement.addSetting(feet);

        surround = new BooleanSetting("Surround", true);
        placement.addSetting(surround);

        doubles = new BooleanSetting("Double", true);
        placement.addSetting(doubles);

        prediction = new CategorySetting("Prediction");
        addSetting(prediction);

        predictMovement = new BooleanSetting("Enable", true);
        prediction.addSetting(predictMovement);

        predictTicks = new NumberSetting("Ticks", 5f, 1f, 20f);
        prediction.addSetting(predictTicks);

        tickCounter = 0;
    }

    @Override
    public void onEnable() {
        target = null;
        tickCounter = 0;
    }

    @Override
    public void onDisable() {
        target = null;
    }

    @Override
    public void onUpdate() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        int webSlot = getWebSlot();
        if (webSlot == -1) return;

        tickCounter++;
        if (tickCounter < delay.getValue()) return;
        tickCounter = 0;

        if (mode.getSelectedOption().equals("Self")) {
            handleSelfMode(webSlot);
        } else {
            handleTargetMode(webSlot);
        }
    }

    private void handleSelfMode(int webSlot) {
        Minecraft mc = Minecraft.getInstance();
        BlockPos playerPos = mc.player.blockPosition();

        int oldSlot = mc.player.getInventory().getSelectedSlot();
        mc.player.getInventory().setSelectedSlot(webSlot);

        if (feet.isEnabled()) {
            placeWeb(playerPos);
            if (doubles.isEnabled()) {
                placeWeb(playerPos.above());
            }
        }

        if (surround.isEnabled()) {
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                BlockPos surroundPos = playerPos.relative(dir);
                placeWeb(surroundPos);
                if (doubles.isEnabled()) {
                    placeWeb(surroundPos.above());
                }
            }
        }

        mc.player.getInventory().setSelectedSlot(oldSlot);
    }

    private void handleTargetMode(int webSlot) {
        updateTarget();
        if (target == null) return;

        Vec3 targetPos = target.position();

        if (predictMovement.isEnabled()) {
            Vec3 vel = target.getDeltaMovement();
            double tickMultiplier = predictTicks.getValue();
            targetPos = targetPos.add(
                    vel.x * tickMultiplier,
                    vel.y * tickMultiplier,
                    vel.z * tickMultiplier
            );
        }

        BlockPos basePos = BlockPos.containing(targetPos);

        int oldSlot = Minecraft.getInstance().player.getInventory().getSelectedSlot();
        Minecraft.getInstance().player.getInventory().setSelectedSlot(webSlot);

        if (feet.isEnabled()) {
            placeWeb(basePos);
            if (doubles.isEnabled()) {
                placeWeb(basePos.above());
            }
        }

        if (surround.isEnabled()) {
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                BlockPos surroundPos = basePos.relative(dir);
                placeWeb(surroundPos);
                if (doubles.isEnabled()) {
                    placeWeb(surroundPos.above());
                }
            }
        }

        Minecraft.getInstance().player.getInventory().setSelectedSlot(oldSlot);
    }

    private void updateTarget() {
        Minecraft mc = Minecraft.getInstance();
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
                .filter(e -> !((LivingEntity) e).isDeadOrDying())
                .filter(e -> {
                    if (onlyPlayers.isEnabled()) {
                        return e instanceof Player;
                    }
                    return true;
                })
                .filter(e -> {
                    if (e instanceof Player player) {
                        return !TargetManager.INSTANCE.isFriend(player.getName().getString());
                    }
                    return TargetManager.INSTANCE.isValidTarget(e);
                })
                .filter(e -> isInRange(playerPos, e, mc));

        if (sorting.getSelectedOption().equals("Distance")) {
            target = (LivingEntity) entityStream
                    .min(Comparator.comparingDouble(e -> mc.player.distanceTo(e)))
                    .orElse(null);
        } else {
            target = (LivingEntity) entityStream
                    .min(Comparator.comparingDouble(e -> {
                        Vec3 targetVec = e.getEyePosition();
                        float[] rot = calculateRotation(playerPos, targetVec);
                        float yawDiff = Math.abs(rot[0] - mc.player.getYRot());
                        float pitchDiff = Math.abs(rot[1] - mc.player.getXRot());
                        return yawDiff + pitchDiff;
                    }))
                    .orElse(null);
        }
    }

    private boolean isInRange(Vec3 pos, Entity entity, Minecraft mc) {
        Vec3 targetVec = entity.getEyePosition();
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

    private float[] calculateRotation(Vec3 from, Vec3 to) {
        double diffX = to.x - from.x;
        double diffY = to.y - from.y;
        double diffZ = to.z - from.z;

        double diffXZ = Math.sqrt(diffX * diffX + diffZ * diffZ);

        float yaw = (float) Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0F;
        float pitch = (float) -Math.toDegrees(Math.atan2(diffY, diffXZ));

        return new float[]{yaw, pitch};
    }

    private void placeWeb(BlockPos pos) {
        Minecraft mc = Minecraft.getInstance();
        BlockState state = mc.level.getBlockState(pos);

        if (state.getBlock() == Blocks.COBWEB) return;
        if (!state.canBeReplaced()) return;

        double dist = mc.player.distanceToSqr(Vec3.atCenterOf(pos));
        if (dist > range.getValue() * range.getValue()) return;

        if (rotate.isEnabled()) {
            lookAt(pos);
        }

        BlockPos belowPos = pos.below();
        Vec3 hitVec = new Vec3(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        BlockHitResult hitResult = new BlockHitResult(hitVec, Direction.UP, belowPos, false);

        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hitResult);
        mc.player.swing(InteractionHand.MAIN_HAND);
    }

    private void lookAt(BlockPos pos) {
        Minecraft mc = Minecraft.getInstance();
        Vec3 eyePos = mc.player.getEyePosition();
        Vec3 target = Vec3.atCenterOf(pos);

        float[] rotation = calculateRotation(eyePos, target);
        mc.player.setYRot(rotation[0]);
        mc.player.setXRot(rotation[1]);
    }

    private int getWebSlot() {
        Minecraft mc = Minecraft.getInstance();

        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getItem(i).getItem() == Items.COBWEB) {
                return i;
            }
        }
        return -1;
    }
}

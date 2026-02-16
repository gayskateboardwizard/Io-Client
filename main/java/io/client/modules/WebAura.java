package io.client.modules;

import io.client.Category;
import io.client.Module;
import io.client.TargetManager;
import io.client.settings.BooleanSetting;
import io.client.settings.CategorySetting;
import io.client.settings.NumberSetting;
import io.client.settings.RadioSetting;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

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
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;

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
        MinecraftClient mc = MinecraftClient.getInstance();
        BlockPos playerPos = mc.player.getBlockPos();

        int oldSlot = mc.player.getInventory().getSelectedSlot();
        mc.player.getInventory().setSelectedSlot(webSlot);

        if (feet.isEnabled()) {
            placeWeb(playerPos);
            if (doubles.isEnabled()) {
                placeWeb(playerPos.up());
            }
        }

        if (surround.isEnabled()) {
            for (Direction dir : Direction.Type.HORIZONTAL) {
                BlockPos surroundPos = playerPos.offset(dir);
                placeWeb(surroundPos);
                if (doubles.isEnabled()) {
                    placeWeb(surroundPos.up());
                }
            }
        }

        mc.player.getInventory().setSelectedSlot(oldSlot);
    }

    private void handleTargetMode(int webSlot) {
        updateTarget();
        if (target == null) return;

        Vec3d targetPos = target.getPos();

        if (predictMovement.isEnabled()) {
            Vec3d vel = target.getVelocity();
            double tickMultiplier = predictTicks.getValue();
            targetPos = targetPos.add(
                    vel.x * tickMultiplier,
                    vel.y * tickMultiplier,
                    vel.z * tickMultiplier
            );
        }

        BlockPos basePos = BlockPos.ofFloored(targetPos);

        int oldSlot = MinecraftClient.getInstance().player.getInventory().getSelectedSlot();
        MinecraftClient.getInstance().player.getInventory().setSelectedSlot(webSlot);

        if (feet.isEnabled()) {
            placeWeb(basePos);
            if (doubles.isEnabled()) {
                placeWeb(basePos.up());
            }
        }

        if (surround.isEnabled()) {
            for (Direction dir : Direction.Type.HORIZONTAL) {
                BlockPos surroundPos = basePos.offset(dir);
                placeWeb(surroundPos);
                if (doubles.isEnabled()) {
                    placeWeb(surroundPos.up());
                }
            }
        }

        MinecraftClient.getInstance().player.getInventory().setSelectedSlot(oldSlot);
    }

    private void updateTarget() {
        MinecraftClient mc = MinecraftClient.getInstance();
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
                .filter(e -> !((LivingEntity) e).isDead())
                .filter(e -> {
                    if (onlyPlayers.isEnabled()) {
                        return e instanceof PlayerEntity;
                    }
                    return true;
                })
                .filter(e -> {
                    if (e instanceof PlayerEntity player) {
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
                        Vec3d targetVec = e.getEyePos();
                        float[] rot = calculateRotation(playerPos, targetVec);
                        float yawDiff = Math.abs(rot[0] - mc.player.getYaw());
                        float pitchDiff = Math.abs(rot[1] - mc.player.getPitch());
                        return yawDiff + pitchDiff;
                    }))
                    .orElse(null);
        }
    }

    private boolean isInRange(Vec3d pos, Entity entity, MinecraftClient mc) {
        Vec3d targetVec = entity.getEyePos();
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

    private float[] calculateRotation(Vec3d from, Vec3d to) {
        double diffX = to.x - from.x;
        double diffY = to.y - from.y;
        double diffZ = to.z - from.z;

        double diffXZ = Math.sqrt(diffX * diffX + diffZ * diffZ);

        float yaw = (float) Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0F;
        float pitch = (float) -Math.toDegrees(Math.atan2(diffY, diffXZ));

        return new float[]{yaw, pitch};
    }

    private void placeWeb(BlockPos pos) {
        MinecraftClient mc = MinecraftClient.getInstance();
        BlockState state = mc.world.getBlockState(pos);

        if (state.getBlock() == Blocks.COBWEB) return;
        if (!state.isReplaceable()) return;

        double dist = mc.player.squaredDistanceTo(Vec3d.ofCenter(pos));
        if (dist > range.getValue() * range.getValue()) return;

        if (rotate.isEnabled()) {
            lookAt(pos);
        }

        BlockPos belowPos = pos.down();
        Vec3d hitVec = new Vec3d(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        BlockHitResult hitResult = new BlockHitResult(hitVec, Direction.UP, belowPos, false);

        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hitResult);
        mc.player.swingHand(Hand.MAIN_HAND);
    }

    private void lookAt(BlockPos pos) {
        MinecraftClient mc = MinecraftClient.getInstance();
        Vec3d eyePos = mc.player.getEyePos();
        Vec3d target = Vec3d.ofCenter(pos);

        float[] rotation = calculateRotation(eyePos, target);
        mc.player.setYaw(rotation[0]);
        mc.player.setPitch(rotation[1]);
    }

    private int getWebSlot() {
        MinecraftClient mc = MinecraftClient.getInstance();

        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).getItem() == Items.COBWEB) {
                return i;
            }
        }
        return -1;
    }
}

package io.client.modules;

import io.client.Category;
import io.client.Module;
import io.client.settings.BooleanSetting;
import io.client.settings.CategorySetting;
import io.client.settings.NumberSetting;
import io.client.settings.RadioSetting;
import java.util.List;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class Burrow extends Module {

    public final RadioSetting mode = new RadioSetting("Mode", "Default");


    public final CategorySetting offsetCategory = new CategorySetting("Offset");
    public final RadioSetting offsetMode = new RadioSetting("OffsetMode", "Smart");
    public final NumberSetting vClip = new NumberSetting("VClip", -9.0F, -256.0F, 256.0F);
    public final BooleanSetting evade = new BooleanSetting("Evade", false);
    public final BooleanSetting discrete = new BooleanSetting("Discrete", true);
    public final BooleanSetting noVoid = new BooleanSetting("NoVoid", false);
    public final BooleanSetting air = new BooleanSetting("Air", false);
    public final BooleanSetting fallback = new BooleanSetting("Fallback", true);
    public final BooleanSetting skipZero = new BooleanSetting("SkipZero", true);


    public final CategorySetting behaviorCategory = new CategorySetting("Behavior");
    public final BooleanSetting scaleDown = new BooleanSetting("ScaleDown", false);
    public final BooleanSetting attack = new BooleanSetting("Attack", true);
    public final BooleanSetting wait = new BooleanSetting("Wait", true);
    public final BooleanSetting placeDisable = new BooleanSetting("PlaceDisable", false);
    public final BooleanSetting allowUp = new BooleanSetting("IgnoreHeadBlock", false);
    public final BooleanSetting rotate = new BooleanSetting("Rotate", true);
    public final BooleanSetting onGround = new BooleanSetting("OnGround", true);

    private BlockPos startPos;
    private long lastPlaceTime = 0;

    public Burrow() {
        super("Burrow", "Burrows you into a block", 0, Category.COMBAT);
        mode.addOption("Default");
        mode.addOption("Web");
        offsetMode.addOption("Smart");
        offsetMode.addOption("Constant");


        addSetting(mode);
        addSetting(offsetMode);
        addSetting(scaleDown);


        addSetting(offsetCategory);
        offsetCategory.addSetting(vClip);
        offsetCategory.addSetting(evade);
        offsetCategory.addSetting(discrete);
        offsetCategory.addSetting(noVoid);
        offsetCategory.addSetting(air);
        offsetCategory.addSetting(fallback);
        offsetCategory.addSetting(skipZero);


        addSetting(behaviorCategory);
        behaviorCategory.addSetting(attack);
        behaviorCategory.addSetting(wait);
        behaviorCategory.addSetting(placeDisable);
        behaviorCategory.addSetting(allowUp);
        behaviorCategory.addSetting(rotate);
        behaviorCategory.addSetting(onGround);
    }

    private static BlockPos getPosition(Entity entity) {
        double y = entity.getY();
        if (entity.getY() - Math.floor(entity.getY()) > 0.5) {
            y = Math.ceil(entity.getY());
        }
        return BlockPos.ofFloored(entity.getX(), y, entity.getZ());
    }

    private static BlockPos getPlayerPos(MinecraftClient mc) {
        return Math.abs(mc.player.getVelocity().y) > 0.1
                ? mc.player.getBlockPos()
                : getPosition(mc.player);
    }

    @Override
    public void onEnable() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        startPos = getPlayerPos(mc);
        lastPlaceTime = System.currentTimeMillis();
    }

    @Override
    public void onUpdate() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null || mc.getNetworkHandler() == null) return;


        if (wait.isEnabled()) {
            BlockPos currentPos = getPlayerPos(mc);
            if (!currentPos.equals(startPos)) {
                toggle();
                return;
            }
        }

        BlockPos pos = getPosition(mc.player);
        BlockState state = mc.world.getBlockState(pos);

        if (!state.isAir() && !state.isReplaceable()) {
            if (!wait.isEnabled()) {
                toggle();
            }
            return;
        }


        Box box = new Box(pos);
        List<Entity> entities = mc.world.getOtherEntities(mc.player, box);

        for (Entity entity : entities) {
            if (entity instanceof EndCrystalEntity && attack.isEnabled()) {

                mc.interactionManager.attackEntity(mc.player, entity);
                mc.player.swingHand(Hand.MAIN_HAND);
                continue;
            }
            if (!wait.isEnabled()) {
                toggle();
            }
            return;
        }


        if (mode.getSelectedOption().equals("Web")) {
            handleWeb(mc, pos);
        } else {
            handleDefault(mc, pos);
        }
    }

    private void handleWeb(MinecraftClient mc, BlockPos pos) {

        int webSlot = findBlockInHotbar(mc, Blocks.COBWEB);
        if (webSlot == -1) {
            toggle();
            return;
        }

        if (System.currentTimeMillis() - lastPlaceTime < 250) return;


        if (rotate.isEnabled()) {
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(
                    mc.player.getYaw(), 90.0F, onGround.isEnabled(), false
            ));
        }


        int prevSlot = mc.player.getInventory().getSelectedSlot();
        mc.player.getInventory().setSelectedSlot(webSlot);

        placeBlock(mc, pos);

        mc.player.getInventory().setSelectedSlot(prevSlot);
        lastPlaceTime = System.currentTimeMillis();

        if (!wait.isEnabled() || placeDisable.isEnabled()) {
            toggle();
        }
    }

    private void handleDefault(MinecraftClient mc, BlockPos pos) {

        if (!mc.player.verticalCollision) {
            return;
        }


        if (!allowUp.isEnabled()) {
            BlockPos headPos = pos.up(2);
            BlockState headState = mc.world.getBlockState(headPos);
            if (!headState.isAir() && !headState.isReplaceable()) {
                if (!wait.isEnabled()) {
                    toggle();
                }
                return;
            }
        }


        int obbySlot = findBlockInHotbar(mc, Blocks.OBSIDIAN);
        int echestSlot = findBlockInHotbar(mc, Blocks.ENDER_CHEST);
        int slot = obbySlot != -1 ? obbySlot : echestSlot;

        if (slot == -1) {
            toggle();
            return;
        }

        if (System.currentTimeMillis() - lastPlaceTime < 1000) return;


        double y = getY(mc.player, offsetMode.getSelectedOption().equals("Smart"));

        if (Double.isNaN(y)) {
            return;
        }


        if (rotate.isEnabled()) {
            float[] angles = calculateAngles(mc.player.getEyePos(), Vec3d.ofCenter(pos));
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(
                    angles[0], angles[1], onGround.isEnabled(), false
            ));
        }


        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                mc.player.getX(), mc.player.getY() + 0.42, mc.player.getZ(), onGround.isEnabled(), false
        ));
        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                mc.player.getX(), mc.player.getY() + 0.75, mc.player.getZ(), onGround.isEnabled(), false
        ));
        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                mc.player.getX(), mc.player.getY() + 1.01, mc.player.getZ(), onGround.isEnabled(), false
        ));
        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                mc.player.getX(), mc.player.getY() + 1.16, mc.player.getZ(), onGround.isEnabled(), false
        ));


        int prevSlot = mc.player.getInventory().getSelectedSlot();
        mc.player.getInventory().setSelectedSlot(slot);

        placeBlock(mc, pos);

        mc.player.getInventory().setSelectedSlot(prevSlot);


        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                mc.player.getX(), y, mc.player.getZ(), false, false
        ));

        lastPlaceTime = System.currentTimeMillis();

        if (!wait.isEnabled() || placeDisable.isEnabled()) {
            toggle();
        }
    }

    private double getY(Entity entity, boolean smart) {
        if (!smart) {

            double y = entity.getY() + vClip.getValue();
            if (evade.isEnabled() && Math.abs(y) < 1) {
                y = -1;
            }
            return y;
        }


        double d = getYOffset(entity, 3, 10, true);
        if (Double.isNaN(d)) {
            d = getYOffset(entity, -3, -10, false);
            if (Double.isNaN(d) && fallback.isEnabled()) {
                return getY(entity, false);
            }
        }
        return d;
    }

    private double getYOffset(Entity entity, double min, double max, boolean add) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (min > max && add || max > min && !add) {
            return Double.NaN;
        }

        double x = entity.getX();
        double y = entity.getY();
        double z = entity.getZ();

        boolean airFound = false;
        double lastOff = 0.0;
        BlockPos last = null;

        for (double off = min; add ? off < max : off > max; off = (add ? ++off : --off)) {
            BlockPos pos = BlockPos.ofFloored(x, y - off, z);

            if (noVoid.isEnabled() && pos.getY() < 0) {
                continue;
            }

            if (skipZero.isEnabled() && Math.abs(y) < 1) {
                airFound = false;
                last = pos;
                lastOff = y - off;
                continue;
            }

            BlockState state = mc.world.getBlockState(pos);
            boolean isAir = state.isAir() || (!this.air.isEnabled() && !state.blocksMovement());

            if (isAir) {
                if (airFound) {
                    if (add) return discrete.isEnabled() ? pos.getY() : y - off;
                    else return discrete.isEnabled() ? last.getY() : lastOff;
                }
                airFound = true;
            } else {
                airFound = false;
            }

            last = pos;
            lastOff = y - off;
        }

        return Double.NaN;
    }

    private void placeBlock(MinecraftClient mc, BlockPos pos) {

        for (Direction dir : Direction.values()) {
            BlockPos neighbor = pos.offset(dir);
            BlockState neighborState = mc.world.getBlockState(neighbor);

            if (!neighborState.isAir() && !neighborState.isReplaceable()) {
                Vec3d hitVec = Vec3d.ofCenter(neighbor).add(Vec3d.of(dir.getOpposite().getVector()).multiply(0.5));
                BlockHitResult hitResult = new BlockHitResult(hitVec, dir.getOpposite(), neighbor, false);

                mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hitResult);
                mc.player.swingHand(Hand.MAIN_HAND);
                return;
            }
        }
    }

    private int findBlockInHotbar(MinecraftClient mc, net.minecraft.block.Block block) {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).getItem() instanceof BlockItem blockItem) {
                if (blockItem.getBlock() == block) {
                    return i;
                }
            }
        }
        return -1;
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
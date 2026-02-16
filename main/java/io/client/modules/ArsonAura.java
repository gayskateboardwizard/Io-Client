package io.client.modules;

import io.client.Category;
import io.client.Module;
import io.client.TargetManager;
import io.client.settings.BooleanSetting;
import io.client.settings.NumberSetting;
import java.util.List;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class ArsonAura extends Module {
    public final NumberSetting range = new NumberSetting("Range", 3.5F, 1.0F, 6.0F);
    public final NumberSetting delay = new NumberSetting("Delay", 10.0F, 0.0F, 20.0F);
    public final BooleanSetting autoSwitch = new BooleanSetting("AutoSwitch", false);
    public final BooleanSetting relight = new BooleanSetting("Relight", true);

    private int tickCounter = 0;
    private int previousSlot = -1;
    private boolean hasSwitched = false;

    public ArsonAura() {
        super("ArsonAura", "Sets fire under targets", -1, Category.COMBAT);
        addSetting(range);
        addSetting(delay);
        addSetting(autoSwitch);
        addSetting(relight);
    }

    @Override
    public void onDisable() {
        super.onDisable();
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) {
            switchBack(mc);
        }
    }

    @Override
    public void onUpdate() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;

        Entity validTarget = findValidTarget(mc);

        if (validTarget == null) {
            switchBack(mc);
            return;
        }

        Hand hand = null;
        if (mc.player.getMainHandStack().getItem() == Items.FLINT_AND_STEEL) {
            hand = Hand.MAIN_HAND;
        } else if (mc.player.getOffHandStack().getItem() == Items.FLINT_AND_STEEL) {
            hand = Hand.OFF_HAND;
        }

        if (hand == null) {
            if (!autoSwitch.isEnabled()) {
                return;
            }

            if (!switchToFlintAndSteel(mc)) {
                return;
            }

            hand = Hand.MAIN_HAND;
        }

        tickCounter++;
        if (tickCounter < delay.getValue()) return;
        tickCounter = 0;

        BlockPos entityPos = validTarget.getBlockPos();
        BlockPos belowPos = entityPos.down();

        BlockState belowState = mc.world.getBlockState(belowPos);
        BlockState entityState = mc.world.getBlockState(entityPos);

        if (!belowState.isAir() && entityState.isAir()) {
            Vec3d hitVec = new Vec3d(entityPos.getX() + 0.5, entityPos.getY(), entityPos.getZ() + 0.5);
            BlockHitResult hitResult = new BlockHitResult(hitVec, Direction.UP, belowPos, false);

            mc.interactionManager.interactBlock(mc.player, hand, hitResult);
        }
    }

    private Entity findValidTarget(MinecraftClient mc) {
        double rangeValue = range.getValue();
        Box searchBox = mc.player.getBoundingBox().expand(rangeValue);
        List<Entity> entities = mc.world.getOtherEntities(mc.player, searchBox);

        for (Entity entity : entities) {
            if (!TargetManager.INSTANCE.isValidTarget(entity)) continue;
            if (!(entity instanceof LivingEntity)) continue;

            if (!relight.isEnabled() && entity.isOnFire()) continue;

            BlockPos entityPos = entity.getBlockPos();
            BlockPos belowPos = entityPos.down();

            BlockState belowState = mc.world.getBlockState(belowPos);
            BlockState entityState = mc.world.getBlockState(entityPos);

            if (!belowState.isAir() && entityState.isAir()) {
                return entity;
            }
        }

        return null;
    }

    private boolean switchToFlintAndSteel(MinecraftClient mc) {
        int flintSlot = findFlintAndSteelSlot(mc);
        int currentSlot = mc.player.getInventory().getSelectedSlot();

        if (flintSlot != -1 && flintSlot != currentSlot) {
            if (!hasSwitched) {
                previousSlot = currentSlot;
                hasSwitched = true;
            }
            mc.player.getInventory().setSelectedSlot(flintSlot);
            return true;
        }

        return flintSlot != -1;
    }

    private int findFlintAndSteelSlot(MinecraftClient mc) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() == Items.FLINT_AND_STEEL) {
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

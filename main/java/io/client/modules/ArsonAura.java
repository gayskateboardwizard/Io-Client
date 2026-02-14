package io.client.modules;

import io.client.Category;
import io.client.Module;
import io.client.TargetManager;
import io.client.settings.BooleanSetting;
import io.client.settings.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

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
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            switchBack(mc);
        }
    }

    @Override
    public void onUpdate() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        Entity validTarget = findValidTarget(mc);

        if (validTarget == null) {
            switchBack(mc);
            return;
        }

        InteractionHand hand = null;
        if (mc.player.getMainHandItem().getItem() == Items.FLINT_AND_STEEL) {
            hand = InteractionHand.MAIN_HAND;
        } else if (mc.player.getOffhandItem().getItem() == Items.FLINT_AND_STEEL) {
            hand = InteractionHand.OFF_HAND;
        }

        if (hand == null) {
            if (!autoSwitch.isEnabled()) {
                return;
            }

            if (!switchToFlintAndSteel(mc)) {
                return;
            }

            hand = InteractionHand.MAIN_HAND;
        }

        tickCounter++;
        if (tickCounter < delay.getValue()) return;
        tickCounter = 0;

        BlockPos entityPos = validTarget.blockPosition();
        BlockPos belowPos = entityPos.below();

        BlockState belowState = mc.level.getBlockState(belowPos);
        BlockState entityState = mc.level.getBlockState(entityPos);

        if (!belowState.isAir() && entityState.isAir()) {
            Vec3 hitVec = new Vec3(entityPos.getX() + 0.5, entityPos.getY(), entityPos.getZ() + 0.5);
            BlockHitResult hitResult = new BlockHitResult(hitVec, Direction.UP, belowPos, false);

            mc.gameMode.useItemOn(mc.player, hand, hitResult);
        }
    }

    private Entity findValidTarget(Minecraft mc) {
        double rangeValue = range.getValue();
        AABB searchBox = mc.player.getBoundingBox().inflate(rangeValue);
        List<Entity> entities = mc.level.getEntities(mc.player, searchBox);

        for (Entity entity : entities) {
            if (!TargetManager.INSTANCE.isValidTarget(entity)) continue;
            if (!(entity instanceof LivingEntity)) continue;

            if (!relight.isEnabled() && entity.isOnFire()) continue;

            BlockPos entityPos = entity.blockPosition();
            BlockPos belowPos = entityPos.below();

            BlockState belowState = mc.level.getBlockState(belowPos);
            BlockState entityState = mc.level.getBlockState(entityPos);

            if (!belowState.isAir() && entityState.isAir()) {
                return entity;
            }
        }

        return null;
    }

    private boolean switchToFlintAndSteel(Minecraft mc) {
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

    private int findFlintAndSteelSlot(Minecraft mc) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (stack.getItem() == Items.FLINT_AND_STEEL) {
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

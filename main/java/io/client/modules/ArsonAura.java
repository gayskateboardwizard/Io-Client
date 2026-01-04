package io.client.modules;

import io.client.Category;
import io.client.Module;
import io.client.TargetManager;
import io.client.settings.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class ArsonAura extends Module {
    public final NumberSetting range = new NumberSetting("Range", 3.5F, 1.0F, 6.0F);
    public final NumberSetting delay = new NumberSetting("Delay", 10.0F, 0.0F, 20.0F);

    private int tickCounter = 0;

    public ArsonAura() {
        super("ArsonAura", "Sets fire under targets", 0, Category.COMBAT);
        addSetting(range);
        addSetting(delay);
    }

    @Override
    public void onUpdate() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        InteractionHand hand = null;
        if (mc.player.getMainHandItem().getItem() == Items.FLINT_AND_STEEL) {
            hand = InteractionHand.MAIN_HAND;
        } else if (mc.player.getOffhandItem().getItem() == Items.FLINT_AND_STEEL) {
            hand = InteractionHand.OFF_HAND;
        }

        if (hand == null) return;

        tickCounter++;
        if (tickCounter < delay.getValue()) return;
        tickCounter = 0;

        double rangeValue = range.getValue();
        AABB searchBox = mc.player.getBoundingBox().inflate(rangeValue);
        List<Entity> entities = mc.level.getEntities(mc.player, searchBox);

        for (Entity entity : entities) {
            if (!TargetManager.INSTANCE.isValidTarget(entity)) continue;
            if (!(entity instanceof LivingEntity)) continue;

            BlockPos entityPos = entity.blockPosition();
            BlockPos belowPos = entityPos.below();

            BlockState belowState = mc.level.getBlockState(belowPos);
            BlockState entityState = mc.level.getBlockState(entityPos);

            if (!belowState.isAir() && entityState.isAir()) {
                Vec3 hitVec = new Vec3(entityPos.getX() + 0.5, entityPos.getY(), entityPos.getZ() + 0.5);
                BlockHitResult hitResult = new BlockHitResult(hitVec, Direction.UP, belowPos, false);

                mc.gameMode.useItemOn(mc.player, hand, hitResult);
                break;
            }
        }
    }
}
package io.client.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;

public class ItemEvents {
    public static final Event<BonemealUsed> BONEMEAL_USED = EventFactory.createArrayBacked(BonemealUsed.class, (callbacks) -> (position, entity, itemstack, blockstate) -> {
        for (BonemealUsed event : callbacks) {
            boolean result = event.onBonemealUsed(position, entity, itemstack, blockstate);
            if (!result) {
                return false;
            }
        }
        return true;
    });

    @FunctionalInterface
    public interface BonemealUsed {
        boolean onBonemealUsed(BlockPos position, Entity entity, ItemStack itemstack, BlockState blockstate);
    }
}
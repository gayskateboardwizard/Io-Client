package io.client.mixin;

import io.client.event.BlockEvents;
import net.minecraft.entity.Entity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.util.ActionResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockItem.class)
public abstract class BlockItemMixin {
    @Inject(method = "useOnBlock", at = @At("HEAD"), cancellable = true)
    public void useOn(ItemUsageContext context, CallbackInfoReturnable<ActionResult> cir) {
        ItemPlacementContext placeContext = new ItemPlacementContext(context);
        boolean result = BlockEvents.BLOCK_PLACE.invoker().onBlockPlaced(context.getBlockPos(), (Entity) placeContext.getPlayer(), ((BlockItem) placeContext.getStack().getItem()).getBlock().getDefaultState(),
                placeContext.getPlayer().getWorld().getBlockState(context.getBlockPos()));
        if (!result)
            cir.setReturnValue(ActionResult.FAIL);
    }
}


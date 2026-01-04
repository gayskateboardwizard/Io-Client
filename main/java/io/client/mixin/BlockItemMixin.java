package io.client.mixin;

import io.client.event.BlockEvents;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockItem.class)
public abstract class BlockItemMixin {
    @Inject(method = "useOn(Lnet/minecraft/world/item/context/UseOnContext;)Lnet/minecraft/world/InteractionResult;", at = @At("HEAD"), cancellable = true)
    public void useOn(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        BlockPlaceContext placeContext = new BlockPlaceContext(context);
        boolean result = BlockEvents.BLOCK_PLACE.invoker().onBlockPlaced(context.getClickedPos(), (Entity) placeContext.getPlayer(), ((BlockItem) placeContext.getItemInHand().getItem()).getBlock().defaultBlockState(),
                placeContext.getPlayer().level().getBlockState(context.getClickedPos()));
        if (!result)
            cir.setReturnValue(InteractionResult.FAIL);
    }
}
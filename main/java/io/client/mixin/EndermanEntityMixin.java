package io.client.mixin;

import io.client.managers.ModuleManager;
import io.client.modules.world.EndermanLook;
import net.minecraft.entity.mob.EndermanEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EndermanEntity.class)
public class EndermanEntityMixin {
    @Inject(method = "isPlayerStaring", at = @At("HEAD"), cancellable = true)
    private void io_client$strictEndermanLook(PlayerEntity player, CallbackInfoReturnable<Boolean> cir) {
        EndermanLook endermanLook = ModuleManager.INSTANCE.getModule(EndermanLook.class);
        if (endermanLook != null && endermanLook.shouldStrictlyBlockAggro(player)) {
            cir.setReturnValue(false);
        }
    }
}

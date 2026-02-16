package io.client.mixin;

import io.client.event.PlayerEvents;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ExperienceOrbEntity.class)
public abstract class ExperienceOrbMixin {
    @Inject(method = "onPlayerCollision", at = @At("HEAD"), cancellable = true)
    public void playerTouch(PlayerEntity player, CallbackInfo ci) {
        if (player instanceof ServerPlayerEntity serverPlayer)
            if (serverPlayer.experiencePickUpDelay == 0)
                if (!PlayerEvents.PICKUP_XP.invoker().onPickupXp(serverPlayer))
                    ci.cancel();
    }
}
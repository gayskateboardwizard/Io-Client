package io.client.mixin;

import io.client.event.PlayerEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ExperienceOrb.class)
public abstract class ExperienceOrbMixin {
    @Inject(method = "playerTouch(Lnet/minecraft/world/entity/player/Player;)V", at = @At("HEAD"), cancellable = true)
    public void playerTouch(Player player, CallbackInfo ci) {
        if (player instanceof ServerPlayer serverPlayer)
            if (serverPlayer.takeXpDelay == 0)
                if (!PlayerEvents.PICKUP_XP.invoker().onPickupXp(serverPlayer))
                    ci.cancel();
    }
}
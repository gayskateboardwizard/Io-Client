package io.client.mixin;

import io.client.event.PlayerEvents;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerMixin {
    @Inject(method = "tick()V", at = @At("TAIL"))
    public void tick(CallbackInfo ci) {
        PlayerEvents.END_PLAYER_TICK.invoker().onEndTick((Player) (Object) this);
    }

    @Inject(method = "giveExperiencePoints(I)V", at = @At("HEAD"))
    public void giveExperiencePoints(int amount, CallbackInfo ci) {
        PlayerEvents.XP_CHANGE.invoker().onXpChange((Player) (Object) this, amount);
    }

    @Inject(method = "giveExperienceLevels(I)V", at = @At("HEAD"))
    public void giveExperienceLevels(int amount, CallbackInfo ci) {
        PlayerEvents.LEVEL_CHANGE.invoker().onLevelChange((Player) (Object) this, amount);
    }
}
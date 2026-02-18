package io.client.mixin;

import io.client.event.PlayerEvents;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public abstract class PlayerMixin {
    @Inject(method = "tick()V", at = @At("TAIL"))
    public void tick(CallbackInfo ci) {
        PlayerEvents.END_PLAYER_TICK.invoker().onEndTick((PlayerEntity) (Object) this);
    }

    @Inject(method = "addExperience", at = @At("HEAD"))
    public void giveExperiencePoints(int amount, CallbackInfo ci) {
        PlayerEvents.XP_CHANGE.invoker().onXpChange((PlayerEntity) (Object) this, amount);
    }

    @Inject(method = "addExperienceLevels", at = @At("HEAD"))
    public void giveExperienceLevels(int amount, CallbackInfo ci) {
        PlayerEvents.LEVEL_CHANGE.invoker().onLevelChange((PlayerEntity) (Object) this, amount);
    }
}


package io.client.mixin;

import io.client.TargetManager;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderer.class)
public class EntityRenderMixin {

    @Inject(method = "hasLabel", at = @At("HEAD"), cancellable = true)
    private void onShouldShowName(Entity entity, double distance, CallbackInfoReturnable<Boolean> cir) {
        if (!TargetManager.INSTANCE.isValidTarget(entity)) {
            cir.setReturnValue(false);
        }
    }
}
package io.client.mixin;

import io.client.modules.render.ESP;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public class EntityRendererMixin {
    @Inject(
            method = "renderLabelIfPresent",
            at = @At("HEAD"),
            cancellable = true)
    private void hidePlayerNametags(
            EntityRenderState renderState,
            Text text,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light,
            CallbackInfo ci
    ) {
        if (renderState instanceof PlayerEntityRenderState && ESP.shouldHideVanillaPlayerNametags()) {
            ci.cancel();
        }
    }
}


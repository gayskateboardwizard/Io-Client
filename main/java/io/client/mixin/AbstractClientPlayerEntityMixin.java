package io.client.mixin;

import com.mojang.authlib.GameProfile;
import io.client.network.IoUserCapeService;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.util.SkinTextures;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractClientPlayerEntity.class)
public abstract class AbstractClientPlayerEntityMixin {
    @Inject(method = "getSkinTextures()Lnet/minecraft/client/util/SkinTextures;", at = @At("RETURN"), cancellable = true)
    private void ioClient$injectSelfCape(CallbackInfoReturnable<SkinTextures> cir) {
        AbstractClientPlayerEntity self = (AbstractClientPlayerEntity) (Object) this;
        GameProfile profile = self.getGameProfile();
        if (profile == null)
            return;
        cir.setReturnValue(IoUserCapeService.withIoCapeIfEligible(profile.getName(), cir.getReturnValue()));
    }
}

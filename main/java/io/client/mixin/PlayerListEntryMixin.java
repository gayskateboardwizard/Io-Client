package io.client.mixin;

import com.mojang.authlib.GameProfile;
import io.client.network.IoUserCapeService;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.SkinTextures;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerListEntry.class)
public abstract class PlayerListEntryMixin {
    @Shadow
    public abstract GameProfile getProfile();

    @Inject(method = "getSkinTextures()Lnet/minecraft/client/util/SkinTextures;", at = @At("RETURN"), cancellable = true)
    private void ioClient$injectIoCape(CallbackInfoReturnable<SkinTextures> cir) {
        GameProfile profile = getProfile();
        if (profile == null)
            return;
        SkinTextures original = cir.getReturnValue();
        if (original == null)
            return;
        cir.setReturnValue(IoUserCapeService.withIoCapeIfEligible(profile.getName(), original));
    }
}



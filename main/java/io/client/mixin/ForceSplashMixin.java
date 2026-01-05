package io.client.mixin;

import net.minecraft.client.gui.components.SplashRenderer;
import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public class ForceSplashMixin {

    @Shadow
    private SplashRenderer splash;

    @Inject(method = "init()V", at = @At("TAIL"))
    private void forceSplash(CallbackInfo ci) {
        this.splash = new SplashRenderer("§d我们正在看着你");
    }
}
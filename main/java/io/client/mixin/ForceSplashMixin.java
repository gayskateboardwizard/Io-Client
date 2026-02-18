package io.client.mixin;

import io.client.SplashTexts;
import net.minecraft.client.gui.screen.SplashTextRenderer;
import net.minecraft.client.gui.screen.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public class ForceSplashMixin {

    @Shadow
    private SplashTextRenderer splashText;

    @Inject(method = "init()V", at = @At("TAIL"))
    private void forceSplash(CallbackInfo ci) {
        this.splashText = new SplashTextRenderer(SplashTexts.getRandomSplash());
    }
}


package io.client.mixin;

import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class MinecraftMixin {
    @Inject(method = "updateWindowTitle", at=@At("HEAD"), cancellable = true)
    private void onUpdateTitle(CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        mc.getWindow().setTitle("IO Client");//not cool, must be IO.CLIENT_NAME
        ci.cancel();
    }
}

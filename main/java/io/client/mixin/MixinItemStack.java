package io.client.mixin;

import io.client.modules.NBTThrottle;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(ItemStack.class)
public abstract class MixinItemStack {

    @Inject(
            method = "getTooltipLines",
            at = @At("RETURN")
    )
    private void onGetTooltipLines(CallbackInfoReturnable<List<Component>> cir) {
        NBTThrottle module = NBTThrottle.getInstance();

        if (module != null && module.isEnabled()) {
            ItemStack stack = (ItemStack) (Object) this;
            if (!stack.isEmpty()) {
                int size = module.getCachedSize(stack);
                ChatFormatting color = module.isOversized(size) ? ChatFormatting.RED : ChatFormatting.GRAY;
                String sizeText = formatSize(size);

                cir.getReturnValue().add(Component.literal(sizeText).withStyle(color));
            }
        }
    }

    private static String formatSize(int bytes) {
        if (bytes < 1024) return "Size: " +  bytes + " Bytes";
        if (bytes < 1024 * 1024) return "Size: " + String.format("%.1fkB", bytes / 1024f);
        if (bytes < 1024 * 1024 * 1024) return "Size: " + String.format("%.1fMB", bytes / (1024f * 1024f));
        return "Size: " + String.format("%.1fGB", bytes / (1024f * 1024f * 1024f));
    }
}
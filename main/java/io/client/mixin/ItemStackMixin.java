package io.client.mixin;

import io.client.modules.ExtraItemInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Item;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin {

    @Inject(
            method = "getTooltip",
            at = @At("RETURN")
    )
    private void onGetTooltip(Item.TooltipContext context, PlayerEntity player, TooltipType type, CallbackInfoReturnable<List<Text>> cir) {
        ExtraItemInfo module = ExtraItemInfo.getInstance();

        if (module != null && module.isEnabled()) {
            ItemStack stack = (ItemStack) (Object) this;
            if (!stack.isEmpty()) {
                int size = module.getCachedSize(stack);
                Formatting color = module.isOversized(size) ? Formatting.RED : Formatting.GRAY;
                String sizeText = formatSize(size);

                cir.getReturnValue().add(Text.literal(sizeText).formatted(color));
            }
        }
    }

    private static String formatSize(int bytes) {
        if (bytes < 1024) return "Size: " + bytes + " Bytes";
        if (bytes < 1024 * 1024) return "Size: " + String.format("%.1fkB", bytes / 1024f);
        if (bytes < 1024 * 1024 * 1024) return "Size: " + String.format("%.1fMB", bytes / (1024f * 1024f));
        return "Size: " + String.format("%.1fGB", bytes / (1024f * 1024f * 1024f));
    }
}
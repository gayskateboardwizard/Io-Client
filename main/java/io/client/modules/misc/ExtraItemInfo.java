package io.client.modules.misc;

import io.client.Category;
import io.client.Module;
import io.client.settings.NumberSetting;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.util.Map;
import java.util.WeakHashMap;

public class ExtraItemInfo extends Module {
    private static ExtraItemInfo INSTANCE;
    private final NumberSetting sizeThreshold;
    private final Map<ItemStack, Integer> sizeCache = new WeakHashMap<>();

    public ExtraItemInfo() {
        super("ExtraItemInfo", "Displays item size", 0, Category.MISC);
        INSTANCE = this;
        sizeThreshold = new NumberSetting("Threshold (KB)", 64.0f, 0.1f, 2048.0f);
        addSetting(sizeThreshold);
    }

    public static ExtraItemInfo getInstance() {
        return INSTANCE;
    }

    public boolean isOversized(int size) {
        return size >= (int) (sizeThreshold.getValue() * 1024);
    }

    public int getCachedSize(ItemStack stack) {
        return sizeCache.computeIfAbsent(stack, ExtraItemInfo::getItemSize);
    }

    public static int getItemSize(ItemStack stack) {
        try {
            var result = ItemStack.CODEC.encodeStart(NbtOps.INSTANCE, stack).result();
            if (result.isPresent() && result.get() instanceof NbtCompound compound) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                NbtIo.writeCompound(compound, new DataOutputStream(baos));
                return baos.size();
            }
        } catch (Exception ignored) {
        }
        return 0;
    }
}

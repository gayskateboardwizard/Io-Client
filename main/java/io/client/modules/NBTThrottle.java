package io.client.modules;

import io.client.Category;
import io.client.Module;
import io.client.settings.NumberSetting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.item.ItemStack;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.util.Map;
import java.util.WeakHashMap;

public class NBTThrottle extends Module {
    private static NBTThrottle INSTANCE;
    private final NumberSetting sizeThreshold;
    private final Map<ItemStack, Integer> sizeCache = new WeakHashMap<>();

    public NBTThrottle() {
        super("ExtraItemInfo", "Displays item size", 0, Category.MISC);
        INSTANCE = this;
        sizeThreshold = new NumberSetting("Threshold (KB)", 64.0f, 0.1f, 2048.0f);
        addSetting(sizeThreshold);
    }

    public static NBTThrottle getInstance() {
        return INSTANCE;
    }

    public boolean isOversized(int size) {
        return size >= (int) (sizeThreshold.getValue() * 1024);
    }

    public int getCachedSize(ItemStack stack) {
        return sizeCache.computeIfAbsent(stack, NBTThrottle::getItemSize);
    }

    public static int getItemSize(ItemStack stack) {
        try {
            var result = ItemStack.CODEC.encodeStart(NbtOps.INSTANCE, stack).result();
            if (result.isPresent() && result.get() instanceof CompoundTag compound) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                NbtIo.write(compound, new DataOutputStream(baos));
                return baos.size();
            }
        } catch (Exception ignored) {}
        return 0;
    }
}
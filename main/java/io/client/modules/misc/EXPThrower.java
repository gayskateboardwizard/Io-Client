package io.client.modules.misc;

import io.client.managers.ItemSwapManager;
import io.client.managers.SwapPriority;
import io.client.modules.templates.Category;
import io.client.modules.templates.Module;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;

public class EXPThrower extends Module {
    private static final String SWAP_OWNER = "EXPThrower";

    public EXPThrower() {
        super("EXPThrower", "Automatically throws XP bottles from your hotbar", -1, Category.MISC);
    }

    @Override
    public void onUpdate() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.player.getInventory() == null || mc.player.networkHandler == null) return;

        int expSlot = findExpInHotbar(mc);
        if (expSlot == -1) return;
        if (!ItemSwapManager.INSTANCE.acquire(SWAP_OWNER, SwapPriority.LOW, 100L)) return;

        int previous = mc.player.getInventory().getSelectedSlot();
        try {
            mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(expSlot));
            mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
        } finally {
            mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(previous));
            ItemSwapManager.INSTANCE.release(SWAP_OWNER);
        }
    }

    private int findExpInHotbar(MinecraftClient mc) {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).isOf(Items.EXPERIENCE_BOTTLE)) return i;
        }
        return -1;
    }
}




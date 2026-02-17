package io.client.modules.combat;

import io.client.Category;
import io.client.Module;
import io.client.settings.RadioSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;

public class OffHand extends Module {

    private static final int OFFHAND_CONTAINER_SLOT = 45;
    private RadioSetting itemSetting;
    private boolean isSwapping = false;

    public OffHand() {
        super("OffHand", "Auto equip items in offhand", -1, Category.COMBAT);

        itemSetting = new RadioSetting("Item", "Totem");
        itemSetting.addOption("Totem");
        itemSetting.addOption("Crystal");
        itemSetting.addOption("Gapple");
        itemSetting.addOption("Obsidian");
        itemSetting.addOption("Empty");
        addSetting(itemSetting);
    }

    @Override
    public void onUpdate() {
        MinecraftClient mc = MinecraftClient.getInstance();

        if (mc.player == null || mc.getNetworkHandler() == null || mc.player.currentScreenHandler == null) return;
        if (isSwapping) return;

        ItemStack offhand = mc.player.getOffHandStack();
        String selectedItem = itemSetting.getSelectedOption();


        if (!isCorrectItem(offhand, selectedItem) && mc.player.currentScreenHandler.getCursorStack().isEmpty()) {


            if (selectedItem.equals("Empty")) {
                if (!offhand.isEmpty()) {
                    swapItem(mc, -1);
                }
                return;
            }


            int itemSlot = findItemSlot(mc.player.getInventory(), selectedItem);

            if (itemSlot != -1) {
                swapItem(mc, itemSlot);
            }
        }
    }

    private boolean isCorrectItem(ItemStack stack, String itemType) {
        switch (itemType) {
            case "Totem":
                return stack.isOf(Items.TOTEM_OF_UNDYING);
            case "Crystal":
                return stack.isOf(Items.END_CRYSTAL);
            case "Gapple":
                return stack.isOf(Items.ENCHANTED_GOLDEN_APPLE);
            case "Obsidian":
                return stack.isOf(Items.OBSIDIAN);
            case "Empty":
                return stack.isEmpty();
            default:
                return false;
        }
    }

    private int findItemSlot(PlayerInventory inventory, String itemType) {
        net.minecraft.item.Item targetItem = null;

        switch (itemType) {
            case "Totem":
                targetItem = Items.TOTEM_OF_UNDYING;
                break;
            case "Crystal":
                targetItem = Items.END_CRYSTAL;
                break;
            case "Gapple":
                targetItem = Items.ENCHANTED_GOLDEN_APPLE;
                break;
            case "Obsidian":
                targetItem = Items.OBSIDIAN;
                break;
            default:
                return -1;
        }


        for (int i = 0; i < 36; i++) {
            if (inventory.getStack(i).isOf(targetItem)) {
                return i;
            }
        }

        return -1;
    }

    private void swapItem(MinecraftClient mc, int inventorySlot) {
        isSwapping = true;

        if (inventorySlot == -1) {

            mc.interactionManager.clickSlot(
                    mc.player.currentScreenHandler.syncId,
                    OFFHAND_CONTAINER_SLOT,
                    0,
                    SlotActionType.PICKUP,
                    mc.player
            );
        } else {

            int containerSlot;

            if (inventorySlot >= 0 && inventorySlot <= 8) {

                containerSlot = inventorySlot + 36;
            } else if (inventorySlot >= 9 && inventorySlot <= 35) {

                containerSlot = inventorySlot;
            } else {
                isSwapping = false;
                return;
            }


            mc.interactionManager.clickSlot(
                    mc.player.currentScreenHandler.syncId,
                    containerSlot,
                    0,
                    SlotActionType.PICKUP,
                    mc.player
            );


            mc.interactionManager.clickSlot(
                    mc.player.currentScreenHandler.syncId,
                    OFFHAND_CONTAINER_SLOT,
                    0,
                    SlotActionType.PICKUP,
                    mc.player
            );


            if (!mc.player.currentScreenHandler.getCursorStack().isEmpty()) {
                mc.interactionManager.clickSlot(
                        mc.player.currentScreenHandler.syncId,
                        containerSlot,
                        0,
                        SlotActionType.PICKUP,
                        mc.player
                );
            }
        }


        new Thread(() -> {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            isSwapping = false;
        }).start();
    }
}

package io.client.modules;

import io.client.Module;
import io.client.Category;
import io.client.settings.RadioSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.ClickType;

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
        Minecraft mc = Minecraft.getInstance();

        if (mc.player == null || mc.getConnection() == null || mc.player.containerMenu == null) return;
        if (isSwapping) return;  

        ItemStack offhand = mc.player.getOffhandItem();
        String selectedItem = itemSetting.getSelectedOption();

         
        if (!isCorrectItem(offhand, selectedItem) && mc.player.containerMenu.getCarried().isEmpty()) {

             
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
                return stack.is(Items.TOTEM_OF_UNDYING);
            case "Crystal":
                return stack.is(Items.END_CRYSTAL);
            case "Gapple":
                return stack.is(Items.ENCHANTED_GOLDEN_APPLE);
            case "Obsidian":
                return stack.is(Items.OBSIDIAN);
            case "Empty":
                return stack.isEmpty();
            default:
                return false;
        }
    }

    private int findItemSlot(Inventory inventory, String itemType) {
        net.minecraft.world.item.Item targetItem = null;

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
            if (inventory.getItem(i).is(targetItem)) {
                return i;
            }
        }

        return -1;
    }

    private void swapItem(Minecraft mc, int inventorySlot) {
        isSwapping = true;

        if (inventorySlot == -1) {
             
            mc.gameMode.handleInventoryMouseClick(
                    mc.player.containerMenu.containerId,
                    OFFHAND_CONTAINER_SLOT,
                    0,
                    ClickType.PICKUP,
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

             
            mc.gameMode.handleInventoryMouseClick(
                    mc.player.containerMenu.containerId,
                    containerSlot,
                    0,
                    ClickType.PICKUP,
                    mc.player
            );

             
            mc.gameMode.handleInventoryMouseClick(
                    mc.player.containerMenu.containerId,
                    OFFHAND_CONTAINER_SLOT,
                    0,
                    ClickType.PICKUP,
                    mc.player
            );

             
            if (!mc.player.containerMenu.getCarried().isEmpty()) {
                mc.gameMode.handleInventoryMouseClick(
                        mc.player.containerMenu.containerId,
                        containerSlot,
                        0,
                        ClickType.PICKUP,
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
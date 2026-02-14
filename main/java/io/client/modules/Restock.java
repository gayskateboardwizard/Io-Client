package io.client.modules;

import io.client.Category;
import io.client.Module;
import io.client.settings.BooleanSetting;
import io.client.settings.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class Restock extends Module {
    private final NumberSetting threshold;
    private final NumberSetting delay;
    private final BooleanSetting weapons;
    private final BooleanSetting armor;
    private final BooleanSetting stackables;

    private int tickCounter = 0;

    public Restock() {
        super("Restock", "Automatically restocks items from inventory", -1, Category.MISC);

        threshold = new NumberSetting("Threshold", 10.0F, 1.0F, 50.0F);
        addSetting(threshold);

        delay = new NumberSetting("Delay", 2.0F, 0.0F, 20.0F);
        addSetting(delay);

        weapons = new BooleanSetting("Weapons", true);
        addSetting(weapons);

        armor = new BooleanSetting("Armor", true);
        addSetting(armor);

        stackables = new BooleanSetting("Stackables", true);
        addSetting(stackables);
    }

    @Override
    public void onUpdate() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        tickCounter++;
        if (tickCounter < delay.getValue()) return;
        tickCounter = 0;

        for (int hotbarSlot = 0; hotbarSlot < 9; hotbarSlot++) {
            ItemStack hotbarStack = mc.player.getInventory().getItem(hotbarSlot);

            if (hotbarStack.isEmpty()) continue;

            if (shouldRestock(hotbarStack)) {
                restockItem(mc, hotbarSlot, hotbarStack);
                return;
            }
        }

        if (armor.isEnabled()) {
            checkAndRestockArmor(mc);
        }
    }

    private boolean shouldRestock(ItemStack stack) {
        if (stack.isEmpty()) return false;

        if (stackables.isEnabled() && stack.isStackable()) {
            int maxStackSize = stack.getMaxStackSize();
            int currentCount = stack.getCount();
            float percentage = (currentCount / (float) maxStackSize) * 100.0F;
            return percentage <= threshold.getValue();
        }

        if (weapons.isEnabled() && stack.isDamageableItem()) {
            int maxDamage = stack.getMaxDamage();
            int currentDamage = stack.getDamageValue();
            int durabilityLeft = maxDamage - currentDamage;
            float percentage = (durabilityLeft / (float) maxDamage) * 100.0F;
            return percentage <= threshold.getValue();
        }

        return false;
    }

    private void restockItem(Minecraft mc, int hotbarSlot, ItemStack hotbarStack) {
        for (int invSlot = 9; invSlot < 36; invSlot++) {
            ItemStack invStack = mc.player.getInventory().getItem(invSlot);

            if (invStack.isEmpty()) continue;

            if (stackables.isEnabled() && hotbarStack.isStackable()) {
                if (ItemStack.isSameItemSameComponents(hotbarStack, invStack)) {
                    mc.gameMode.handleInventoryMouseClick(
                            mc.player.inventoryMenu.containerId,
                            invSlot,
                            0,
                            ClickType.QUICK_MOVE,
                            mc.player
                    );
                    return;
                }
            }

            if (weapons.isEnabled() && hotbarStack.isDamageableItem()) {
                if (ItemStack.isSameItemSameComponents(invStack, hotbarStack)) {
                    int hotbarDurability = hotbarStack.getMaxDamage() - hotbarStack.getDamageValue();
                    int invDurability = invStack.getMaxDamage() - invStack.getDamageValue();

                    if (invDurability > hotbarDurability) {
                        mc.gameMode.handleInventoryMouseClick(
                                mc.player.inventoryMenu.containerId,
                                invSlot,
                                hotbarSlot,
                                ClickType.SWAP,
                                mc.player
                        );
                        return;
                    }
                }
            }
        }
    }

    private void checkAndRestockArmor(Minecraft mc) {
        EquipmentSlot[] armorSlots = {
                EquipmentSlot.FEET,
                EquipmentSlot.LEGS,
                EquipmentSlot.CHEST,
                EquipmentSlot.HEAD
        };

        for (EquipmentSlot slot : armorSlots) {
            ItemStack currentArmor = mc.player.getItemBySlot(slot);

            if (currentArmor.isEmpty()) continue;
            if (!currentArmor.isDamageableItem()) continue;

            int maxDamage = currentArmor.getMaxDamage();
            int currentDamage = currentArmor.getDamageValue();
            int durabilityLeft = maxDamage - currentDamage;
            float percentage = (durabilityLeft / (float) maxDamage) * 100.0F;

            if (percentage <= threshold.getValue()) {
                for (int invSlot = 9; invSlot < 36; invSlot++) {
                    ItemStack invStack = mc.player.getInventory().getItem(invSlot);

                    if (invStack.isEmpty()) continue;
                    if (!invStack.isDamageableItem()) continue;

                    if (ItemStack.isSameItemSameComponents(invStack, currentArmor)) {
                        int invDurability = invStack.getMaxDamage() - invStack.getDamageValue();
                        int currentDurabilityLeft = currentArmor.getMaxDamage() - currentArmor.getDamageValue();

                        if (invDurability > currentDurabilityLeft) {
                            int armorSlotId = 8 - slot.getIndex();
                            mc.gameMode.handleInventoryMouseClick(
                                    mc.player.inventoryMenu.containerId,
                                    invSlot,
                                    armorSlotId - 36,
                                    ClickType.SWAP,
                                    mc.player
                            );
                            return;
                        }
                    }
                }
            }
        }
    }
}

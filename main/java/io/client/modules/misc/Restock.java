package io.client.modules.misc;

import io.client.Category;
import io.client.Module;
import io.client.settings.BooleanSetting;
import io.client.settings.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;

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
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;

        tickCounter++;
        if (tickCounter < delay.getValue()) return;
        tickCounter = 0;

        for (int hotbarSlot = 0; hotbarSlot < 9; hotbarSlot++) {
            ItemStack hotbarStack = mc.player.getInventory().getStack(hotbarSlot);

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
            int maxStackSize = stack.getMaxCount();
            int currentCount = stack.getCount();
            float percentage = (currentCount / (float) maxStackSize) * 100.0F;
            return percentage <= threshold.getValue();
        }

        if (weapons.isEnabled() && stack.isDamageable()) {
            int maxDamage = stack.getMaxDamage();
            int currentDamage = stack.getDamage();
            int durabilityLeft = maxDamage - currentDamage;
            float percentage = (durabilityLeft / (float) maxDamage) * 100.0F;
            return percentage <= threshold.getValue();
        }

        return false;
    }

    private void restockItem(MinecraftClient mc, int hotbarSlot, ItemStack hotbarStack) {
        for (int invSlot = 9; invSlot < 36; invSlot++) {
            ItemStack invStack = mc.player.getInventory().getStack(invSlot);

            if (invStack.isEmpty()) continue;

            if (stackables.isEnabled() && hotbarStack.isStackable()) {
                if (ItemStack.areItemsAndComponentsEqual(hotbarStack, invStack)) {
                    mc.interactionManager.clickSlot(
                            mc.player.playerScreenHandler.syncId,
                            invSlot,
                            0,
                            SlotActionType.QUICK_MOVE,
                            mc.player
                    );
                    return;
                }
            }

            if (weapons.isEnabled() && hotbarStack.isDamageable()) {
                if (ItemStack.areItemsAndComponentsEqual(invStack, hotbarStack)) {
                    int hotbarDurability = hotbarStack.getMaxDamage() - hotbarStack.getDamage();
                    int invDurability = invStack.getMaxDamage() - invStack.getDamage();

                    if (invDurability > hotbarDurability) {
                        mc.interactionManager.clickSlot(
                                mc.player.playerScreenHandler.syncId,
                                invSlot,
                                hotbarSlot,
                                SlotActionType.SWAP,
                                mc.player
                        );
                        return;
                    }
                }
            }
        }
    }

    private void checkAndRestockArmor(MinecraftClient mc) {
        EquipmentSlot[] armorSlots = {
                EquipmentSlot.FEET,
                EquipmentSlot.LEGS,
                EquipmentSlot.CHEST,
                EquipmentSlot.HEAD
        };

        for (EquipmentSlot slot : armorSlots) {
            ItemStack currentArmor = mc.player.getEquippedStack(slot);

            if (currentArmor.isEmpty()) continue;
            if (!currentArmor.isDamageable()) continue;

            int maxDamage = currentArmor.getMaxDamage();
            int currentDamage = currentArmor.getDamage();
            int durabilityLeft = maxDamage - currentDamage;
            float percentage = (durabilityLeft / (float) maxDamage) * 100.0F;

            if (percentage <= threshold.getValue()) {
                for (int invSlot = 9; invSlot < 36; invSlot++) {
                    ItemStack invStack = mc.player.getInventory().getStack(invSlot);

                    if (invStack.isEmpty()) continue;
                    if (!invStack.isDamageable()) continue;

                    if (ItemStack.areItemsAndComponentsEqual(invStack, currentArmor)) {
                        int invDurability = invStack.getMaxDamage() - invStack.getDamage();
                        int currentDurabilityLeft = currentArmor.getMaxDamage() - currentArmor.getDamage();

                        if (invDurability > currentDurabilityLeft) {
                            int armorSlotId = 8 - slot.getEntitySlotId();
                            mc.interactionManager.clickSlot(
                                    mc.player.playerScreenHandler.syncId,
                                    invSlot,
                                    armorSlotId - 36,
                                    SlotActionType.SWAP,
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


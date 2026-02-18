package io.client.modules.combat;

import io.client.managers.ModuleManager;
import io.client.modules.templates.Category;
import io.client.modules.templates.Module;
import io.client.settings.BooleanSetting;
import io.client.settings.NumberSetting;
import io.client.settings.RadioSetting;
import io.client.utils.DamageUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.screen.slot.SlotActionType;

public class OffHand extends Module {

    private static final int OFFHAND_CONTAINER_SLOT = 45;
    
    // Settings
    private final NumberSetting delayTicks = new NumberSetting("Item Switch Delay", 0, 0, 20);
    private final RadioSetting preferredItem = new RadioSetting("Item", "Crystal");
    private final BooleanSetting hotbar = new BooleanSetting("Hotbar", false);
    private final BooleanSetting rightGapple = new BooleanSetting("Right Gapple", false);
    private final BooleanSetting swordGap = new BooleanSetting("Sword Gapple", false);
    private final BooleanSetting alwaysSwordGap = new BooleanSetting("Always Gap on Sword", false);
    private final BooleanSetting alwaysPot = new BooleanSetting("Always Pot on Sword", false);
    private final BooleanSetting potionClick = new BooleanSetting("Sword Pot", false);
    
    // Totem settings
    private final NumberSetting minHealth = new NumberSetting("Min Health", 10, 0, 36);
    private final BooleanSetting elytra = new BooleanSetting("Elytra", false);
    private final BooleanSetting falling = new BooleanSetting("Falling", false);
    private final BooleanSetting explosion = new BooleanSetting("Explosion", true);
    
    private boolean isClicking = false;
    private boolean sentMessage = false;
    private boolean isSwapping = false;
    public boolean locked = false;
    
    private int ticks = 0;
    private ItemType currentItem;

    public OffHand() {
        super("OffHand", "Auto equip items in offhand", -1, Category.COMBAT);

        preferredItem.addOption("Totem");
        preferredItem.addOption("Crystal");
        preferredItem.addOption("EGap");
        preferredItem.addOption("Gap");
        preferredItem.addOption("Shield");
        preferredItem.addOption("Potion");
        
        addSetting(delayTicks);
        addSetting(preferredItem);
        addSetting(hotbar);
        addSetting(rightGapple);
        addSetting(swordGap);
        addSetting(alwaysSwordGap);
        addSetting(alwaysPot);
        addSetting(potionClick);
        addSetting(minHealth);
        addSetting(elytra);
        addSetting(falling);
        addSetting(explosion);
    }

    @Override
    public void onEnable() {
        ticks = 0;
        sentMessage = false;
        isClicking = false;
        currentItem = ItemType.fromString(preferredItem.getSelectedOption());
    }

    @Override
    public void onUpdate() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.getNetworkHandler() == null || mc.player.currentScreenHandler == null) return;
        if (isSwapping) return;

        int totems = countItem(mc.player.getInventory(), Items.TOTEM_OF_UNDYING);
        
        if (totems <= 0) {
            locked = false;
        } else if (ticks >= delayTicks.getValue()) {
            float health = mc.player.getHealth() + mc.player.getAbsorptionAmount();
            float possibleDamage = calculatePossibleDamage(mc);
            boolean lowHealth = health - possibleDamage <= minHealth.getValue();
            boolean usingElytra = elytra.isEnabled() && 
                                 mc.player.getEquippedStack(EquipmentSlot.CHEST).getItem() == Items.ELYTRA && 
                                 mc.player.isGliding();
            
            locked = lowHealth || usingElytra;
            
            if (locked && !mc.player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING)) {
                int totemSlot = findItemSlot(mc.player.getInventory(), Items.TOTEM_OF_UNDYING, false);
                if (totemSlot != -1) {
                    swapItem(mc, totemSlot);
                }
                ticks = 0;
                return;
            }
        }
        
        ticks++;
        
        // Determine current item based on context
        currentItem = ItemType.fromString(preferredItem.getSelectedOption());
        
        // Right-click gapple logic
        if (rightGapple.isEnabled() && !locked) {
            if (swordGap.isEnabled() && mc.player.getMainHandStack().isIn(ItemTags.SWORDS)) {
                if (isClicking) {
                    currentItem = ItemType.EGAP;
                }
            } else if (!swordGap.isEnabled() && isClicking) {
                currentItem = ItemType.EGAP;
            }
        }
        // Always gapple on sword
        else if ((mc.player.getMainHandStack().isIn(ItemTags.SWORDS) || 
                  mc.player.getMainHandStack().getItem() instanceof AxeItem) && 
                 alwaysSwordGap.isEnabled()) {
            currentItem = ItemType.EGAP;
        }
        // Potion click
        else if (potionClick.isEnabled() && !locked) {
            if (mc.player.getMainHandStack().isIn(ItemTags.SWORDS) && isClicking) {
                currentItem = ItemType.POTION;
            }
        }
        // Always pot on sword
        else if ((mc.player.getMainHandStack().isIn(ItemTags.SWORDS) || 
                  mc.player.getMainHandStack().getItem() instanceof AxeItem) && 
                 alwaysPot.isEnabled()) {
            currentItem = ItemType.POTION;
        }
        
        // Check and swap offhand item
        ItemStack offhand = mc.player.getOffHandStack();
        if (!offhand.isOf(currentItem.item) && ticks >= delayTicks.getValue() && !locked) {
            int itemSlot = findItemSlot(mc.player.getInventory(), currentItem.item, hotbar.isEnabled());
            
            if (itemSlot == -1) {
                if (!sentMessage) {
                    sentMessage = true;
                }
            } else {
                swapItem(mc, itemSlot);
                sentMessage = false;
            }
            ticks = 0;
        }
    }

    private float calculatePossibleDamage(MinecraftClient mc) {
        return DamageUtils.possibleHealthReductions(mc.player, explosion.isEnabled(), falling.isEnabled());
    }

    private int countItem(PlayerInventory inventory, net.minecraft.item.Item item) {
        int count = 0;
        for (int i = 0; i < 36; i++) {
            if (inventory.getStack(i).isOf(item)) {
                count += inventory.getStack(i).getCount();
            }
        }
        return count;
    }

    private int findItemSlot(PlayerInventory inventory, net.minecraft.item.Item item, boolean includeHotbar) {
        int start = includeHotbar ? 0 : 9;
        
        for (int i = start; i < 36; i++) {
            if (inventory.getStack(i).isOf(item)) {
                return i;
            }
        }
        return -1;
    }

    private void swapItem(MinecraftClient mc, int inventorySlot) {
        if (inventorySlot == -1) return;
        
        isSwapping = true;
        
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

        new Thread(() -> {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            isSwapping = false;
        }).start();
    }
    
    public void setClicking(boolean clicking) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        
        // Check if holding usable item
        boolean usableItem = mc.player.getMainHandStack().getItem() == Items.BOW ||
                            mc.player.getMainHandStack().getItem() == Items.TRIDENT ||
                            mc.player.getMainHandStack().getItem() == Items.CROSSBOW ||
                            mc.player.getMainHandStack().getItem().getComponents().contains(DataComponentTypes.FOOD);
        
        if (!usableItem && !mc.player.isUsingItem()) {
            this.isClicking = clicking;
        }
    }
    
    public boolean isLocked() {
        return locked;
    }

    private enum ItemType {
        TOTEM(Items.TOTEM_OF_UNDYING),
        CRYSTAL(Items.END_CRYSTAL),
        EGAP(Items.ENCHANTED_GOLDEN_APPLE),
        GAP(Items.GOLDEN_APPLE),
        SHIELD(Items.SHIELD),
        POTION(Items.POTION);
        
        final net.minecraft.item.Item item;
        
        ItemType(net.minecraft.item.Item item) {
            this.item = item;
        }
        
        static ItemType fromString(String name) {
            return switch (name) {
                case "Totem" -> TOTEM;
                case "Crystal" -> CRYSTAL;
                case "EGap" -> EGAP;
                case "Gap" -> GAP;
                case "Shield" -> SHIELD;
                case "Potion" -> POTION;
                default -> CRYSTAL;
            };
        }
    }
}



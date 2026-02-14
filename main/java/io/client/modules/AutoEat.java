//this implementation fucking sucks btw

package io.client.modules;

import io.client.Category;
import io.client.Module;
import io.client.settings.BooleanSetting;
import io.client.settings.NumberSetting;
import io.client.settings.RadioSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Set;

public class AutoEat extends Module {
    private static final long SWAP_DELAY_MS = 100;
    private static final Set<Item> FOOD_BLACKLIST = Set.of(
            Items.ROTTEN_FLESH,
            Items.SPIDER_EYE,
            Items.POISONOUS_POTATO,
            Items.PUFFERFISH,
            Items.CHICKEN,
            Items.SUSPICIOUS_STEW
    );

    public final NumberSetting minHunger = new NumberSetting("Min Hunger", 16.0F, 0.0F, 20.0F);
    public final NumberSetting minHealth = new NumberSetting("Min Health", 14.0F, 0.0F, 20.0F);
    public final RadioSetting handMode = new RadioSetting("Hand", "Auto");
    public final BooleanSetting prioritizeGapples = new BooleanSetting("Prioritize Gapples", true);
    public final BooleanSetting eatWhileMoving = new BooleanSetting("Eat While Moving", true);

    private int previousSlot = -1;
    private boolean hasSwitchedSlot = false;
    private boolean isEating = false;
    private InteractionHand eatingHand = null;
    private long lastSwapTime = 0;

    public AutoEat() {
        super("AutoEat", "Auto eat food when low", -1, Category.COMBAT);

        handMode.addOption("Auto");
        handMode.addOption("Mainhand");
        handMode.addOption("Offhand");

        addSetting(minHunger);
        addSetting(minHealth);
        addSetting(handMode);
        addSetting(prioritizeGapples);
        addSetting(eatWhileMoving);
    }

    @Override
    public void onUpdate() {
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.player instanceof LocalPlayer player) || mc.gameMode == null) return;

        int hunger = player.getFoodData().getFoodLevel();
        float health = player.getHealth();
        long now = System.currentTimeMillis();

        if (hunger >= minHunger.getValue() && health >= minHealth.getValue()) {
            stopEating(mc, player);
            return;
        }

        if (player.isUsingItem()) {
            isEating = true;
            if (!eatWhileMoving.isEnabled()) {
                mc.options.keyUp.setDown(false);
                mc.options.keyDown.setDown(false);
                mc.options.keyLeft.setDown(false);
                mc.options.keyRight.setDown(false);
            }
            return;
        }

        if (isEating && !player.isUsingItem()) {
            isEating = false;
            eatingHand = null;
        }

        if (!isEating && (hunger < minHunger.getValue() || health < minHealth.getValue())) {
            if (now - lastSwapTime < SWAP_DELAY_MS) return;

            FoodSlot foodSlot = findBestFood(player);
            if (foodSlot == null) {
                return;
            }

            startEating(mc, player, foodSlot, now);
        }
    }

    private FoodSlot findBestFood(LocalPlayer player) {
        String mode = handMode.getSelectedOption();
        Inventory inv = player.getInventory();

        FoodSlot bestMainhand = null;
        FoodSlot bestOffhand = null;

        ItemStack offhandStack = player.getOffhandItem();
        if (isValidFood(offhandStack)) {
            int priority = getFoodPriority(offhandStack);
            bestOffhand = new FoodSlot(-1, InteractionHand.OFF_HAND, priority);
        }

        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = inv.getItem(slot);
            if (isValidFood(stack)) {
                int priority = getFoodPriority(stack);
                if (bestMainhand == null || priority > bestMainhand.priority) {
                    bestMainhand = new FoodSlot(slot, InteractionHand.MAIN_HAND, priority);
                }
            }
        }

        switch (mode) {
            case "Mainhand":
                return bestMainhand;
            case "Offhand":
                return bestOffhand;
            case "Auto":
            default:
                if (bestOffhand != null && bestMainhand != null) {
                    return bestOffhand.priority >= bestMainhand.priority ? bestOffhand : bestMainhand;
                }
                return bestOffhand != null ? bestOffhand : bestMainhand;
        }
    }

    private boolean isValidFood(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (FOOD_BLACKLIST.contains(stack.getItem())) return false;

        FoodProperties food = stack.getItem().components().get(net.minecraft.core.component.DataComponents.FOOD);
        return food != null;
    }

    private int getFoodPriority(ItemStack stack) {
        if (!prioritizeGapples.isEnabled()) return 1;

        if (stack.is(Items.ENCHANTED_GOLDEN_APPLE)) return 3;
        if (stack.is(Items.GOLDEN_APPLE)) return 2;
        return 1;
    }

    private void startEating(Minecraft mc, LocalPlayer player, FoodSlot foodSlot, long now) {
        Inventory inv = player.getInventory();

        if (foodSlot.hand == InteractionHand.MAIN_HAND) {
            int currSlot = getCurrentSlot(inv);
            if (currSlot != foodSlot.slot) {
                previousSlot = currSlot;
                hasSwitchedSlot = true;
                setSelectedSlot(mc, inv, foodSlot.slot);
                lastSwapTime = now;
                return;
            }
        }

        mc.options.keyUse.setDown(true);
        eatingHand = foodSlot.hand;
        isEating = true;
    }

    private void stopEating(Minecraft mc, LocalPlayer player) {
        mc.options.keyUse.setDown(false);

        if (hasSwitchedSlot && previousSlot != -1) {
            setSelectedSlot(mc, player.getInventory(), previousSlot);
            previousSlot = -1;
            hasSwitchedSlot = false;
        }

        isEating = false;
        eatingHand = null;
    }

    private int getCurrentSlot(Inventory inventory) {
        try {
            var field = Inventory.class.getDeclaredField("selected");
            field.setAccessible(true);
            return field.getInt(inventory);
        } catch (Exception e) {
            return -1;
        }
    }

    private void setSelectedSlot(Minecraft mc, Inventory inventory, int slot) {
        try {
            var field = Inventory.class.getDeclaredField("selected");
            field.setAccessible(true);
            field.setInt(inventory, slot);
        } catch (Exception ignored) {
        }

        if (mc.player != null && mc.player.connection != null) {
            mc.player.connection.send(new ServerboundSetCarriedItemPacket(slot));
        }
    }

    @Override
    public void onDisable() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player instanceof LocalPlayer player) {
            stopEating(mc, player);

            previousSlot = -1;
            hasSwitchedSlot = false;
        }
    }

    private static class FoodSlot {
        final int slot;
        final InteractionHand hand;
        final int priority;

        FoodSlot(int slot, InteractionHand hand, int priority) {
            this.slot = slot;
            this.hand = hand;
            this.priority = priority;
        }
    }
}

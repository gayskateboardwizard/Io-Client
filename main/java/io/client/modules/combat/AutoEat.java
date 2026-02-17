

package io.client.modules.combat;

import io.client.Category;
import io.client.Module;
import io.client.settings.BooleanSetting;
import io.client.settings.NumberSetting;
import io.client.settings.RadioSetting;
import java.util.Set;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;

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
    public final BooleanSetting prioritizeGapples = new BooleanSetting("PrioritizeGapps", true);
    public final BooleanSetting eatWhileMoving = new BooleanSetting("EatWhileMoving", true);

    private int previousSlot = -1;
    private boolean hasSwitchedSlot = false;
    private boolean isEating = false;
    private Hand eatingHand = null;
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
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!(mc.player instanceof ClientPlayerEntity player) || mc.interactionManager == null) return;

        int hunger = player.getHungerManager().getFoodLevel();
        float health = player.getHealth();
        long now = System.currentTimeMillis();

        if (hunger >= minHunger.getValue() && health >= minHealth.getValue()) {
            stopEating(mc, player);
            return;
        }

        if (player.isUsingItem()) {
            isEating = true;
            if (!eatWhileMoving.isEnabled()) {
                mc.options.forwardKey.setPressed(false);
                mc.options.backKey.setPressed(false);
                mc.options.leftKey.setPressed(false);
                mc.options.rightKey.setPressed(false);
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

    private FoodSlot findBestFood(ClientPlayerEntity player) {
        String mode = handMode.getSelectedOption();
        PlayerInventory inv = player.getInventory();

        FoodSlot bestMainhand = null;
        FoodSlot bestOffhand = null;

        ItemStack offhandStack = player.getOffHandStack();
        if (isValidFood(offhandStack)) {
            int priority = getFoodPriority(offhandStack);
            bestOffhand = new FoodSlot(-1, Hand.OFF_HAND, priority);
        }

        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = inv.getStack(slot);
            if (isValidFood(stack)) {
                int priority = getFoodPriority(stack);
                if (bestMainhand == null || priority > bestMainhand.priority) {
                    bestMainhand = new FoodSlot(slot, Hand.MAIN_HAND, priority);
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

        FoodComponent food = stack.getItem().getComponents().get(net.minecraft.component.DataComponentTypes.FOOD);
        return food != null;
    }

    private int getFoodPriority(ItemStack stack) {
        if (!prioritizeGapples.isEnabled()) return 1;

        if (stack.isOf(Items.ENCHANTED_GOLDEN_APPLE)) return 3;
        if (stack.isOf(Items.GOLDEN_APPLE)) return 2;
        return 1;
    }

    private void startEating(MinecraftClient mc, ClientPlayerEntity player, FoodSlot foodSlot, long now) {
        PlayerInventory inv = player.getInventory();

        if (foodSlot.hand == Hand.MAIN_HAND) {
            int currSlot = player.getInventory().getSelectedSlot();
            if (currSlot != foodSlot.slot) {
                previousSlot = currSlot;
                hasSwitchedSlot = true;
                player.getInventory().setSelectedSlot(foodSlot.slot);
                mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(foodSlot.slot));
                lastSwapTime = now;
                return;
            }
        }

        mc.options.useKey.setPressed(true);
        eatingHand = foodSlot.hand;
        isEating = true;
    }

    private void stopEating(MinecraftClient mc, ClientPlayerEntity player) {
        mc.options.useKey.setPressed(false);

        if (hasSwitchedSlot && previousSlot != -1) {
            player.getInventory().setSelectedSlot(previousSlot);
            mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(previousSlot));
            previousSlot = -1;
            hasSwitchedSlot = false;
        }

        isEating = false;
        eatingHand = null;
    }



    @Override
    public void onDisable() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player instanceof ClientPlayerEntity player) {
            stopEating(mc, player);

            previousSlot = -1;
            hasSwitchedSlot = false;
        }
    }

    private static class FoodSlot {
        final int slot;
        final Hand hand;
        final int priority;

        FoodSlot(int slot, Hand hand, int priority) {
            this.slot = slot;
            this.hand = hand;
            this.priority = priority;
        }
    }
}


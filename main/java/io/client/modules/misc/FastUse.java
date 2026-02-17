package io.client.modules.misc;

import io.client.Category;
import io.client.Module;
import io.client.settings.BooleanSetting;
import io.client.settings.NumberSetting;
import io.client.mixin.IMinecraftAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.BlockItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.lwjgl.glfw.GLFW;

public class FastUse extends Module {
    private final NumberSetting delay;
    private final BooleanSetting blocks;
    private final BooleanSetting xp;
    private final BooleanSetting crystals;
    private final BooleanSetting all;
    private final BooleanSetting ghostFix;
    private final BooleanSetting bows;
    private final NumberSetting bowDelay;
    private final BooleanSetting crossbow;

    public FastUse() {
        super("FastUse", "Use items faster", -1, Category.MISC);

        delay = new NumberSetting("Delay", 0, 0, 10);
        blocks = new BooleanSetting("Blocks", false);
        xp = new BooleanSetting("XP", false);
        crystals = new BooleanSetting("Crystals", false);
        all = new BooleanSetting("All", false);
        ghostFix = new BooleanSetting("GhostFix", false);
        bows = new BooleanSetting("FastBow", false);
        bowDelay = new NumberSetting("BowDrawTime", 3, 3, 25);
        crossbow = new BooleanSetting("FastCrossbow", false);

        addSetting(delay);
        addSetting(blocks);
        addSetting(xp);
        addSetting(crystals);
        addSetting(all);
        addSetting(ghostFix);
        addSetting(bows);
        addSetting(bowDelay);
        addSetting(crossbow);
    }

    @Override
    public void onUpdate() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        handleBow(mc);
        handleItems(mc);
    }

    private void handleBow(MinecraftClient mc) {
        ItemStack mainhand = mc.player.getMainHandStack();

        if (mainhand.getItem() == Items.BOW && bows.isEnabled()) {
            if (mc.player.getItemUseTime() >= (int) bowDelay.getValue()) {
                mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(
                        PlayerActionC2SPacket.Action.RELEASE_USE_ITEM,
                        BlockPos.ORIGIN,
                        Direction.DOWN
                ));
                mc.player.clearActiveItem();
            }
        } else if (crossbow.isEnabled() && mainhand.getItem() == Items.CROSSBOW) {
            if (mc.player.getItemUseTime() / (float) CrossbowItem.getPullTime(mainhand, mc.player) > 1.0f) {
                mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(
                        PlayerActionC2SPacket.Action.RELEASE_USE_ITEM,
                        BlockPos.ORIGIN,
                        Direction.DOWN
                ));
                mc.player.clearActiveItem();
                mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
            }
        }
    }

    private void handleItems(MinecraftClient mc) {
        if (getItemUseCooldown(mc) > delay.getValue() && check(mc.player.getMainHandStack().getItem())) {
            if (ghostFix.isEnabled()) {
                mc.getNetworkHandler().sendPacket(new PlayerInteractItemC2SPacket(
                        mc.player.getActiveHand(),
                        0,
                        mc.player.getYaw(),
                        mc.player.getPitch()
                ));
            }
            setItemUseCooldown(mc, (int) delay.getValue());
        }
    }

    private boolean check(Item item) {
        return (item instanceof BlockItem && blocks.isEnabled())
                || (item == Items.END_CRYSTAL && crystals.isEnabled())
                || (item == Items.EXPERIENCE_BOTTLE && xp.isEnabled())
                || all.isEnabled();
    }

    private int getItemUseCooldown(MinecraftClient mc) {
        return ((IMinecraftAccessor) mc).getItemUseCooldown();
    }

    private void setItemUseCooldown(MinecraftClient mc, int value) {
        ((IMinecraftAccessor) mc).setItemUseCooldown(value);
    }
}


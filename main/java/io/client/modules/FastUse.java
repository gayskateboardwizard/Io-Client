package io.client.modules;

import io.client.Category;
import io.client.Module;
import io.client.settings.BooleanSetting;
import io.client.settings.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import io.client.mixin.IMinecraftAccessor;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
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
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        handleBow(mc);
        handleItems(mc);
    }

    private void handleBow(Minecraft mc) {
        ItemStack mainhand = mc.player.getMainHandItem();

        if (mainhand.getItem() == Items.BOW && bows.isEnabled()) {
            if (mc.player.getTicksUsingItem() >= (int) bowDelay.getValue()) {
                mc.getConnection().send(new ServerboundPlayerActionPacket(
                        ServerboundPlayerActionPacket.Action.RELEASE_USE_ITEM,
                        BlockPos.ZERO,
                        Direction.DOWN
                ));
                mc.player.stopUsingItem();
            }
        } else if (crossbow.isEnabled() && mainhand.getItem() == Items.CROSSBOW) {
            if (mc.player.getTicksUsingItem() / (float) CrossbowItem.getChargeDuration(mainhand, mc.player) > 1.0f) {
                mc.getConnection().send(new ServerboundPlayerActionPacket(
                        ServerboundPlayerActionPacket.Action.RELEASE_USE_ITEM,
                        BlockPos.ZERO,
                        Direction.DOWN
                ));
                mc.player.stopUsingItem();
                mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
            }
        }
    }

    private void handleItems(Minecraft mc) {
        if (getItemUseCooldown(mc) > delay.getValue() && check(mc.player.getMainHandItem().getItem())) {
            if (ghostFix.isEnabled()) {
                mc.getConnection().send(new ServerboundUseItemPacket(
                        mc.player.getUsedItemHand(),
                        0,
                        mc.player.getYRot(),
                        mc.player.getXRot()
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

    private int getItemUseCooldown(Minecraft mc) {
        return ((IMinecraftAccessor) mc).getItemUseCooldown();
    }

    private void setItemUseCooldown(Minecraft mc, int value) {
        ((IMinecraftAccessor) mc).setItemUseCooldown(value);
    }
}

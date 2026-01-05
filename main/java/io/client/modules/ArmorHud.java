package io.client.modules;

import io.client.Category;
import io.client.Module;
import io.client.settings.BooleanSetting;
import io.client.settings.CategorySetting;
import io.client.settings.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

import java.awt.*;

public class ArmorHud extends Module {
    private final BooleanSetting percentIcon;
    private final BooleanSetting small;
    private final BooleanSetting triColor;

    private final NumberSetting xPos;
    private final NumberSetting yPos;

    private final CategorySetting highColorCategory;
    private final NumberSetting highR;
    private final NumberSetting highG;
    private final NumberSetting highB;

    private final CategorySetting midColorCategory;
    private final NumberSetting midR;
    private final NumberSetting midG;
    private final NumberSetting midB;

    private final CategorySetting lowColorCategory;
    private final NumberSetting lowR;
    private final NumberSetting lowG;
    private final NumberSetting lowB;

    public ArmorHud() {
        super("ArmorHud", "Shows armor durability on HUD", -1, Category.RENDER);

        percentIcon = new BooleanSetting("Percent Icon", false);
        small = new BooleanSetting("Small", false);
        triColor = new BooleanSetting("Tri Color", false);

        xPos = new NumberSetting("X Position", 0, 0, 1920);
        yPos = new NumberSetting("Y Position", 0, 0, 1080);

        highColorCategory = new CategorySetting("High Color");
        highR = new NumberSetting("R", 0, 0, 255);
        highG = new NumberSetting("G", 255, 0, 255);
        highB = new NumberSetting("B", 255, 0, 255);
        highColorCategory.addSetting(highR);
        highColorCategory.addSetting(highG);
        highColorCategory.addSetting(highB);

        midColorCategory = new CategorySetting("Mid Color");
        midR = new NumberSetting("R", 0, 0, 255);
        midG = new NumberSetting("G", 0, 0, 255);
        midB = new NumberSetting("B", 255, 0, 255);
        midColorCategory.addSetting(midR);
        midColorCategory.addSetting(midG);
        midColorCategory.addSetting(midB);

        lowColorCategory = new CategorySetting("Low Color");
        lowR = new NumberSetting("R", 87, 0, 255);
        lowG = new NumberSetting("G", 8, 0, 255);
        lowB = new NumberSetting("B", 97, 0, 255);
        lowColorCategory.addSetting(lowR);
        lowColorCategory.addSetting(lowG);
        lowColorCategory.addSetting(lowB);

        addSetting(percentIcon);
        addSetting(small);
        addSetting(triColor);
        addSetting(xPos);
        addSetting(yPos);
        addSetting(highColorCategory);
        addSetting(midColorCategory);
        addSetting(lowColorCategory);
    }

    public void render(GuiGraphics context, float tickDelta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        int width = mc.getWindow().getGuiScaledWidth();
        int height = mc.getWindow().getGuiScaledHeight();

        int centerX = width / 2;
        int iteration = 0;
        int baseY = height - 55 - (mc.player.isEyeInFluid(FluidTags.WATER) ? 10 : 0);
        int y = (int) yPos.getValue();
        if (y == 0) y = baseY;

        for (int i = 0; i < 4; i++) {
            ItemStack armor = mc.player.getInventory().getItem(36 + i);
            iteration++;
            if (armor.isEmpty()) continue;

            int baseX = centerX - 90 + (9 - iteration) * 20 + 2;
            int x = (int) xPos.getValue();
            if (x == 0) x = baseX;
            else x = x + ((9 - iteration) * 20);

            context.renderItem(armor, x, y);
            context.renderItemDecorations(mc.font, armor, x, y);

            if (armor.getCount() > 1) {
                String countStr = armor.getCount() + "";
                context.drawString(mc.font, countStr,
                        x + 19 - 2 - mc.font.width(countStr),
                        y + 9, 0xFFFFFF);
            }

            if (armor.isDamageableItem()) {
                float durabilityPercent = ((float) armor.getMaxDamage() - (float) armor.getDamageValue()) / (float) armor.getMaxDamage();
                int dmg = Math.round(durabilityPercent * 100);

                String dmgStr = dmg + (percentIcon.isEnabled() ? "%" : "");
                Color color = getArmorColor(dmg);

                if (small.isEnabled()) {
                    context.pose().pushMatrix();
                    context.pose().scale(0.625f, 0.625f);
                    context.drawString(mc.font, dmgStr,
                            (int) (((x + 6) * 1.6f) - (mc.font.width(dmgStr) / 2.0f) * 0.6f),
                            (int) ((y * 1.6f) - 11),
                            color.getRGB());
                    context.pose().popMatrix();
                } else {
                    context.drawString(mc.font, dmgStr,
                            (int) (x + 8 - mc.font.width(dmgStr) / 2.0f),
                            y - 9,
                            color.getRGB());
                }
            }
        }
    }

    private Color getArmorColor(int dmg) {
        if (!triColor.isEnabled()) {
            return Color.WHITE;
        }

        Color highColor = new Color((int) highR.getValue(), (int) highG.getValue(), (int) highB.getValue());
        Color midColor = new Color((int) midR.getValue(), (int) midG.getValue(), (int) midB.getValue());
        Color lowColor = new Color((int) lowR.getValue(), (int) lowG.getValue(), (int) lowB.getValue());

        if (dmg > 66) {
            float t = Mth.clamp(normalize(dmg, 66, 100), 0, 1);
            return interpolate(t, highColor, midColor);
        } else if (dmg > 33) {
            float t = Mth.clamp(normalize(dmg, 33, 66), 0, 1);
            return interpolate(t, midColor, lowColor);
        } else {
            return lowColor;
        }
    }

    private float normalize(float value, float min, float max) {
        return (value - min) / (max - min);
    }

    private Color interpolate(float t, Color a, Color b) {
        int r = (int) (a.getRed() + t * (b.getRed() - a.getRed()));
        int g = (int) (a.getGreen() + t * (b.getGreen() - a.getGreen()));
        int blue = (int) (a.getBlue() + t * (b.getBlue() - a.getBlue()));
        return new Color(r, g, blue);
    }
}
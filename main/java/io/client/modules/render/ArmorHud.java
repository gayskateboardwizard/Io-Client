package io.client.modules.render;

import io.client.clickgui.screens.ClickGuiScreen;
import io.client.modules.templates.Category;
import io.client.modules.templates.Module;
import io.client.settings.BooleanSetting;
import io.client.settings.CategorySetting;
import io.client.settings.NumberSetting;
import io.client.settings.RadioSetting;
import java.awt.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.math.MathHelper;

public class ArmorHud extends Module {
    private final BooleanSetting percentIcon;
    private final BooleanSetting small;
    private final RadioSetting colorMode;

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

        percentIcon = new BooleanSetting("PercentIcon", false);
        small = new BooleanSetting("Small", false);
        colorMode = new RadioSetting("ColorMode", "TriColor");
        colorMode.addOption("TriColor");
        colorMode.addOption("Theme");
        colorMode.addOption("None");

        highColorCategory = new CategorySetting("High Color");
        highR = new NumberSetting("HighR", 10, 0, 255);
        highG = new NumberSetting("HighG", 255, 0, 255);
        highB = new NumberSetting("HighB", 10, 0, 255);
        highColorCategory.addSetting(highR);
        highColorCategory.addSetting(highG);
        highColorCategory.addSetting(highB);

        midColorCategory = new CategorySetting("Mid Color");
        midR = new NumberSetting("MidR", 255, 0, 255);
        midG = new NumberSetting("MidG", 125, 0, 255);
        midB = new NumberSetting("MidB", 10, 0, 255);
        midColorCategory.addSetting(midR);
        midColorCategory.addSetting(midG);
        midColorCategory.addSetting(midB);

        lowColorCategory = new CategorySetting("Low Color");
        lowR = new NumberSetting("LowR", 255, 0, 255);
        lowG = new NumberSetting("LowG", 10, 0, 255);
        lowB = new NumberSetting("LowB", 10, 0, 255);
        lowColorCategory.addSetting(lowR);
        lowColorCategory.addSetting(lowG);
        lowColorCategory.addSetting(lowB);

        addSetting(percentIcon);
        addSetting(small);
        addSetting(colorMode);
        addSetting(highColorCategory);
        addSetting(midColorCategory);
        addSetting(lowColorCategory);
    }

    public void render(DrawContext context, float tickDelta) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        int width = mc.getWindow().getScaledWidth();
        int height = mc.getWindow().getScaledHeight();

        int centerX = width / 2;
        int iteration = 0;
        int y = height - 55 - (mc.player.isSubmergedIn(FluidTags.WATER) ? 10 : 0);

        for (int i = 0; i < 4; i++) {
            ItemStack armor = mc.player.getInventory().getStack(36 + i);
            iteration++;
            if (armor.isEmpty()) continue;

            int x = centerX - 90 + (9 - iteration) * 20 + 2;

            context.drawItem(armor, x, y);
            context.drawStackOverlay(mc.textRenderer, armor, x, y);

            if (armor.getCount() > 1) {
                String countStr = armor.getCount() + "";
                context.drawTextWithShadow(mc.textRenderer, countStr,
                        x + 19 - 2 - mc.textRenderer.getWidth(countStr),
                        y + 9, 0xFFFFFF);
            }

            if (armor.isDamageable() && !colorMode.isSelected("None")) {
                float durabilityPercent = ((float) armor.getMaxDamage() - (float) armor.getDamage()) / (float) armor.getMaxDamage();
                int dmg = Math.round(durabilityPercent * 100);

                String dmgStr = dmg + (percentIcon.isEnabled() ? "%" : "");
                Color color = getArmorColor(dmg);

                if (small.isEnabled()) {
                    context.getMatrices().pushMatrix();
                    context.getMatrices().scale(0.625f, 0.625f);
                    context.drawTextWithShadow(mc.textRenderer, dmgStr,
                            (int) (((x + 6) * 1.6f) - (mc.textRenderer.getWidth(dmgStr) / 2.0f) * 0.6f),
                            (int) ((y * 1.6f) - 11),
                            color.getRGB());
                    context.getMatrices().popMatrix();
                } else {
                    context.drawTextWithShadow(mc.textRenderer, dmgStr,
                            (int) (x + 8 - mc.textRenderer.getWidth(dmgStr) / 2.0f),
                            y - 9,
                            color.getRGB());
                }
            }
        }
    }

    private Color getArmorColor(int dmg) {
        if (colorMode.isSelected("Theme")) {
            return new Color(ClickGuiScreen.currentTheme.moduleEnabled);
        }

        if (colorMode.isSelected("None")) {
            return Color.WHITE;
        }

        Color highColor = new Color((int) highR.getValue(), (int) highG.getValue(), (int) highB.getValue());
        Color midColor = new Color((int) midR.getValue(), (int) midG.getValue(), (int) midB.getValue());
        Color lowColor = new Color((int) lowR.getValue(), (int) lowG.getValue(), (int) lowB.getValue());

        if (dmg > 75) {
            return highColor;
        } else if (dmg > 66) {
            float t = MathHelper.clamp(normalize(dmg, 66, 75), 0, 1);
            return interpolate(t, highColor, midColor);
        } else if (dmg > 50) {
            return midColor;
        } else if (dmg > 33) {
            float t = MathHelper.clamp(normalize(dmg, 33, 50), 0, 1);
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




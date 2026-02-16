package io.client.clickgui;

import io.client.Category;
import io.client.Module;
import io.client.ModuleManager;
import io.client.settings.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PanelRenderer {
    private static int PANEL_WIDTH = 90;
    private static int MODULE_HEIGHT = 10;
    private static int SETTING_HEIGHT = 12;
    private static int TITLE_BAR_HEIGHT = 13;
    private static float SCALE = 1.0f;

    private final Map<String, Float> textScrollOffsets = new HashMap<>();
    private Theme theme;

    public PanelRenderer(Theme theme) {
        this.theme = theme;
    }

    public static void setScale(float scale) {
        SCALE = scale;
        PANEL_WIDTH = (int) (90 * scale);
        MODULE_HEIGHT = (int) (10 * scale);
        SETTING_HEIGHT = (int) (12 * scale);
        TITLE_BAR_HEIGHT = (int) (13 * scale);
    }

    public static int getPanelWidth() {
        return PANEL_WIDTH;
    }

    public static int getTitleBarHeight() {
        return TITLE_BAR_HEIGHT;
    }

    public static int getModuleHeight() {
        return MODULE_HEIGHT;
    }

    public static int getSettingHeight() {
        return SETTING_HEIGHT;
    }

    public void setTheme(Theme theme) {
        this.theme = theme;
    }

    public String renderPanel(GuiGraphics graphics, Font font, CategoryPanel panel, int mouseX, int mouseY) {
        Minecraft mc = Minecraft.getInstance();
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        panel.x = Math.max(0, Math.min(panel.x, screenWidth - PANEL_WIDTH));
        panel.y = Math.max(0, Math.min(panel.y, screenHeight - TITLE_BAR_HEIGHT));

        List<Module> modules = ModuleManager.INSTANCE.getModulesByCategory(panel.category);
        int contentHeight = calculateContentHeight(modules, panel.collapsed);
        int actualHeight = panel.collapsed ? TITLE_BAR_HEIGHT : TITLE_BAR_HEIGHT + 2 + contentHeight;

        renderTitleBar(graphics, font, panel, mouseX, mouseY);

        String hoveredDescription = null;
        if (!panel.collapsed) {
            renderPanelBackground(graphics, panel, actualHeight);
            hoveredDescription = renderModules(graphics, font, panel, modules, mouseX, mouseY);
        }

        return hoveredDescription;
    }

    private int calculateContentHeight(List<Module> modules, boolean collapsed) {
        if (collapsed) return 0;

        int height = 0;
        for (Module module : modules) {
            height += MODULE_HEIGHT;
            if (module.isExtended()) {
                for (Setting setting : module.getSettings()) {
                    height += SETTING_HEIGHT;
                    if (setting instanceof CategorySetting catSetting && catSetting.isExpanded()) {
                        height += catSetting.getSettings().size() * SETTING_HEIGHT;
                    }
                }
            }
        }
        return height;
    }

    private void renderTitleBar(GuiGraphics graphics, Font font, CategoryPanel panel, int mouseX, int mouseY) {
        graphics.fill(panel.x, panel.y, panel.x + PANEL_WIDTH, panel.y + TITLE_BAR_HEIGHT, theme.titleBar);
        graphics.fill(panel.x, panel.y, panel.x + PANEL_WIDTH, panel.y + 1, 0x44FFFFFF);

        graphics.pose().pushMatrix();
        graphics.pose().translate(panel.x + 3 * SCALE, panel.y + 2 * SCALE);
        graphics.pose().scale(SCALE, SCALE);
        graphics.drawString(font, panel.category.name(), 0, 0, 0xFFFFFFFF, false);
        graphics.pose().popMatrix();

        int collapseX = panel.x + PANEL_WIDTH - (int)(12 * SCALE);
        graphics.pose().pushMatrix();
        graphics.pose().translate(collapseX, panel.y + 2 * SCALE);
        graphics.pose().scale(SCALE, SCALE);
        graphics.drawString(font, panel.collapsed ? "+" : "-", 0, 0, 0xFFAAAAAA, false);
        graphics.pose().popMatrix();

        if (panel.category == Category.COMBAT) {
            int targetsX = panel.x + PANEL_WIDTH - (int)(28 * SCALE);
            graphics.pose().pushMatrix();
            graphics.pose().translate(targetsX, panel.y + 2 * SCALE);
            graphics.pose().scale(SCALE, SCALE);
            graphics.drawString(font, "T", 0, 0, theme.moduleEnabled, false);
            graphics.pose().popMatrix();
        }
    }

    private void renderPanelBackground(GuiGraphics graphics, CategoryPanel panel, int actualHeight) {
        graphics.fill(panel.x, panel.y + TITLE_BAR_HEIGHT, panel.x + PANEL_WIDTH, panel.y + actualHeight, theme.panelBackground);
        graphics.fill(panel.x, panel.y + TITLE_BAR_HEIGHT, panel.x + PANEL_WIDTH, panel.y + TITLE_BAR_HEIGHT + 1, 0x22FFFFFF);
    }

    private String renderModules(GuiGraphics graphics, Font font, CategoryPanel panel, List<Module> modules, int mouseX, int mouseY) {
        String hoveredDescription = null;
        int yOffset = panel.y + TITLE_BAR_HEIGHT + 2;

        for (Module module : modules) {
            String desc = renderModule(graphics, font, panel, module, yOffset, mouseX, mouseY);
            if (desc != null) hoveredDescription = desc;

            yOffset += MODULE_HEIGHT;
            if (module.isExtended()) {
                yOffset = renderSettings(graphics, font, panel, module.getSettings(), yOffset, mouseX, mouseY);
            }
        }

        return hoveredDescription;
    }

    private String renderModule(GuiGraphics graphics, Font font, CategoryPanel panel, Module module, int yOffset, int mouseX, int mouseY) {
        int moduleColor = module.isEnabled() ? theme.moduleEnabled : theme.moduleDisabled;
        int x1 = panel.x, y1 = yOffset, x2 = panel.x + PANEL_WIDTH, y2 = yOffset + MODULE_HEIGHT;

        boolean isModuleHovered = mouseX >= x1 && mouseX <= x2 && mouseY >= y1 && mouseY <= y2;
        if (isModuleHovered) {
            graphics.fill(x1, y1, x2, y2, theme.hoverHighlight);
        }

        if (!module.getSettings().isEmpty()) {
            String indicator = module.isExtended() ? "^" : ">";
            graphics.pose().pushMatrix();
            graphics.pose().translate(panel.x + PANEL_WIDTH - 8 * SCALE, yOffset + 1 * SCALE);
            graphics.pose().scale(SCALE, SCALE);
            graphics.drawString(font, indicator, 0, 0, 0xFF888888, false);
            graphics.pose().popMatrix();
        }

        graphics.pose().pushMatrix();
        graphics.pose().translate(panel.x + 3 * SCALE, yOffset + 1 * SCALE);
        graphics.pose().scale(SCALE, SCALE);
        graphics.drawString(font, module.getName(), 0, 0, moduleColor, false);
        graphics.pose().popMatrix();

        return isModuleHovered ? module.getDescription() : null;
    }

    private int renderSettings(GuiGraphics graphics, Font font, CategoryPanel panel, List<Setting> settings, int yOffset, int mouseX, int mouseY) {
        for (Setting setting : settings) {
            if (setting instanceof CategorySetting catSetting) {
                yOffset = renderCategorySetting(graphics, font, panel, catSetting, yOffset, mouseX, mouseY);
            } else if (setting instanceof BooleanSetting boolSetting) {
                yOffset = renderBooleanSetting(graphics, font, panel, boolSetting, yOffset, mouseX, mouseY, 3);
            } else if (setting instanceof NumberSetting numSetting) {
                yOffset = renderNumberSetting(graphics, font, panel, numSetting, yOffset, mouseX, mouseY, 3);
            } else if (setting instanceof StringSetting strSetting) {
                yOffset = renderStringSetting(graphics, font, panel, strSetting, yOffset, mouseX, mouseY);
            } else if (setting instanceof RadioSetting radioSetting) {
                yOffset = renderRadioSetting(graphics, font, panel, radioSetting, yOffset, mouseX, mouseY, 3);
            }
        }
        return yOffset;
    }

    private int renderCategorySetting(GuiGraphics graphics, Font font, CategoryPanel panel, CategorySetting catSetting, int yOffset, int mouseX, int mouseY) {
        int settingX1 = panel.x, settingY1 = yOffset, settingX2 = panel.x + PANEL_WIDTH, settingY2 = yOffset + SETTING_HEIGHT;

        boolean isSettingHovered = mouseX >= settingX1 && mouseX <= settingX2 && mouseY >= settingY1 && mouseY <= settingY2;
        if (isSettingHovered) {
            graphics.fill(settingX1, settingY1, settingX2, settingY2, theme.hoverHighlight);
        }

        String catIndicator = catSetting.isExpanded() ? "^" : ">";
        graphics.pose().pushMatrix();
        graphics.pose().translate(panel.x + PANEL_WIDTH - 8 * SCALE, yOffset + 1 * SCALE);
        graphics.pose().scale(SCALE, SCALE);
        graphics.drawString(font, catIndicator, 0, 0, theme.moduleDisabled, false);
        graphics.pose().popMatrix();

        graphics.pose().pushMatrix();
        graphics.pose().translate(panel.x + 3 * SCALE, yOffset + 1 * SCALE);
        graphics.pose().scale(SCALE, SCALE);
        graphics.drawString(font, catSetting.getName(), 0, 0, theme.moduleEnabled, false);
        graphics.pose().popMatrix();

        yOffset += SETTING_HEIGHT;

        if (catSetting.isExpanded()) {
            for (Object catItem : catSetting.getSettings()) {
                int catItemY1 = yOffset, catItemY2 = yOffset + SETTING_HEIGHT;

                boolean isCatItemHovered = mouseX >= panel.x && mouseX <= panel.x + PANEL_WIDTH && mouseY >= catItemY1 && mouseY <= catItemY2;
                if (isCatItemHovered) {
                    graphics.fill(panel.x, catItemY1, panel.x + PANEL_WIDTH, catItemY2, theme.hoverHighlight);
                }

                if (catItem instanceof BooleanSetting boolSetting) {
                    renderBooleanSetting(graphics, font, panel, boolSetting, yOffset, mouseX, mouseY, 8);
                    yOffset += SETTING_HEIGHT;
                } else if (catItem instanceof NumberSetting numSetting) {
                    renderNumberSetting(graphics, font, panel, numSetting, yOffset, mouseX, mouseY, 8);
                    yOffset += SETTING_HEIGHT;
                } else if (catItem instanceof StringSetting strSetting) {
                    String displayValue = strSetting.getValue();
                    String label = strSetting.getName() + ": \"" + displayValue + "\"";
                    graphics.pose().pushMatrix();
                    graphics.pose().translate(panel.x + 8 * SCALE, yOffset + 2 * SCALE);
                    graphics.pose().scale(SCALE, SCALE);
                    graphics.drawString(font, label, 0, 0, 0xFFFFFFFF, false);
                    graphics.pose().popMatrix();
                    yOffset += SETTING_HEIGHT;
                } else if (catItem instanceof RadioSetting radioSetting) {
                    renderRadioSetting(graphics, font, panel, radioSetting, yOffset, mouseX, mouseY, 8);
                    yOffset += SETTING_HEIGHT;
                }
            }
        }

        return yOffset;
    }

    private int renderBooleanSetting(GuiGraphics graphics, Font font, CategoryPanel panel, BooleanSetting boolSetting, int yOffset, int mouseX, int mouseY, int indent) {
        int settingX1 = panel.x, settingY1 = yOffset, settingX2 = panel.x + PANEL_WIDTH, settingY2 = yOffset + SETTING_HEIGHT;

        boolean isSettingHovered = mouseX >= settingX1 && mouseX <= settingX2 && mouseY >= settingY1 && mouseY <= settingY2;
        if (isSettingHovered && indent == 3) {
            graphics.fill(settingX1, settingY1, settingX2, settingY2, theme.hoverHighlight);
        }

        int indicatorColor = boolSetting.isEnabled() ? theme.moduleEnabled : theme.moduleDisabled;
        String indicatorText = boolSetting.isEnabled() ? "[x]" : "[ ]";
        float indicatorX = panel.x + indent * SCALE;

        graphics.pose().pushMatrix();
        graphics.pose().translate(indicatorX, yOffset + 2 * SCALE);
        graphics.pose().scale(SCALE, SCALE);
        graphics.drawString(font, indicatorText, 0, 0, indicatorColor, false);
        graphics.pose().popMatrix();

        float indicatorWidth = font.width(indicatorText) * SCALE;

        String settingName = boolSetting.getName();
        int maxWidth = (int)((PANEL_WIDTH - indicatorWidth - indent * SCALE - 6 * SCALE) / SCALE);

        if (font.width(settingName) > maxWidth) {
            String key = "bool_" + indent + "_" + boolSetting.getName();
            if (isSettingHovered) {
                float offset = textScrollOffsets.getOrDefault(key, 0f);
                offset += 0.5f;
                float textWidth = font.width(settingName + "  " + settingName + "  ");
                if (offset > textWidth) offset = 0;
                textScrollOffsets.put(key, offset);
            } else {
                textScrollOffsets.put(key, 0f);
            }

            float offset = textScrollOffsets.get(key);
            String scrollText = settingName + "  " + settingName + "  " + settingName;

            int scissorX1 = (int)(indicatorX + indicatorWidth + 4 * SCALE);
            int scissorX2 = (int)(panel.x + PANEL_WIDTH - 3 * SCALE);
            graphics.enableScissor(scissorX1, yOffset, scissorX2, yOffset + SETTING_HEIGHT);
            graphics.pose().pushMatrix();
            graphics.pose().translate(indicatorX + indicatorWidth + 4 * SCALE - offset * SCALE, yOffset + 2 * SCALE);
            graphics.pose().scale(SCALE, SCALE);
            graphics.drawString(font, scrollText, 0, 0, 0xFFFFFFFF, false);
            graphics.pose().popMatrix();
            graphics.disableScissor();
        } else {
            graphics.pose().pushMatrix();
            graphics.pose().translate(indicatorX + indicatorWidth + 4 * SCALE, yOffset + 2 * SCALE);
            graphics.pose().scale(SCALE, SCALE);
            graphics.drawString(font, settingName, 0, 0, 0xFFFFFFFF, false);
            graphics.pose().popMatrix();
        }

        return yOffset + SETTING_HEIGHT;
    }

    private int renderNumberSetting(GuiGraphics graphics, Font font, CategoryPanel panel, NumberSetting numSetting, int yOffset, int mouseX, int mouseY, int indent) {
        int settingX1 = panel.x, settingY1 = yOffset, settingX2 = panel.x + PANEL_WIDTH, settingY2 = yOffset + SETTING_HEIGHT;

        boolean isSettingHovered = mouseX >= settingX1 && mouseX <= settingX2 && mouseY >= settingY1 && mouseY <= settingY2;
        if (isSettingHovered && indent == 3) {
            graphics.fill(settingX1, settingY1, settingX2, settingY2, theme.hoverHighlight);
        }

        float range = numSetting.getMax() - numSetting.getMin();
        int barWidth = (int) ((numSetting.getValue() - numSetting.getMin()) / range * (PANEL_WIDTH - indent * SCALE - 7 * SCALE));

        int sliderY = yOffset + (int)(10 * SCALE);
        graphics.fill(panel.x + (int)(indent * SCALE), sliderY, panel.x + PANEL_WIDTH - (int)(3 * SCALE), sliderY + Math.max(1, (int)SCALE), theme.sliderBackground);
        graphics.fill(panel.x + (int)(indent * SCALE), sliderY, panel.x + (int)(indent * SCALE) + barWidth, sliderY + Math.max(1, (int)SCALE), theme.sliderForeground);

        String label = numSetting.getName() + ": " + String.format("%.1f", numSetting.getValue());
        int maxWidth = (int)((PANEL_WIDTH - indent * SCALE - 7 * SCALE) / SCALE);

        if (font.width(label) > maxWidth) {
            String key = "num_" + indent + "_" + numSetting.getName();
            if (isSettingHovered) {
                float offset = textScrollOffsets.getOrDefault(key, 0f);
                offset += 0.5f;
                float textWidth = font.width(label + "  " + label + "  ");
                if (offset > textWidth) offset = 0;
                textScrollOffsets.put(key, offset);
            } else {
                textScrollOffsets.put(key, 0f);
            }

            float offset = textScrollOffsets.get(key);
            String scrollText = label + "  " + label + "  " + label;

            int scissorX1 = panel.x + (int)(indent * SCALE);
            int scissorX2 = panel.x + PANEL_WIDTH - (int)(3 * SCALE);
            graphics.enableScissor(scissorX1, yOffset, scissorX2, yOffset + SETTING_HEIGHT);
            graphics.pose().pushMatrix();
            graphics.pose().translate(panel.x + indent * SCALE - offset * SCALE, yOffset + 1 * SCALE);
            graphics.pose().scale(SCALE, SCALE);
            graphics.drawString(font, scrollText, 0, 0, 0xFFFFFFFF, false);
            graphics.pose().popMatrix();
            graphics.disableScissor();
        } else {
            graphics.pose().pushMatrix();
            graphics.pose().translate(panel.x + indent * SCALE, yOffset + 1 * SCALE);
            graphics.pose().scale(SCALE, SCALE);
            graphics.drawString(font, label, 0, 0, 0xFFFFFFFF, false);
            graphics.pose().popMatrix();
        }

        return yOffset + SETTING_HEIGHT;
    }

    private int renderStringSetting(GuiGraphics graphics, Font font, CategoryPanel panel, StringSetting strSetting, int yOffset, int mouseX, int mouseY) {
        int settingX1 = panel.x, settingY1 = yOffset, settingX2 = panel.x + PANEL_WIDTH, settingY2 = yOffset + SETTING_HEIGHT;

        boolean isSettingHovered = mouseX >= settingX1 && mouseX <= settingX2 && mouseY >= settingY1 && mouseY <= settingY2;
        if (isSettingHovered) {
            graphics.fill(settingX1, settingY1, settingX2, settingY2, theme.hoverHighlight);
        }

        String displayValue = strSetting.getValue();
        String label = strSetting.getName() + ": \"" + displayValue + "\"";

        int maxWidth = (int)((PANEL_WIDTH - 6 * SCALE) / SCALE);
        if (font.width(label) > maxWidth) {
            String key = "str_" + strSetting.getName();
            if (isSettingHovered) {
                float offset = textScrollOffsets.getOrDefault(key, 0f);
                offset += 0.3f;
                float textWidth = font.width(label + "  " + label + "  ");
                if (offset > textWidth) offset = 0;
                textScrollOffsets.put(key, offset);
            } else {
                textScrollOffsets.put(key, 0f);
            }

            float offset = textScrollOffsets.get(key);
            String scrollText = label + "  " + label + "  " + label;

            int scissorX1 = panel.x + (int)(3 * SCALE);
            int scissorX2 = panel.x + PANEL_WIDTH - (int)(3 * SCALE);
            graphics.enableScissor(scissorX1, yOffset, scissorX2, yOffset + SETTING_HEIGHT);
            graphics.pose().pushMatrix();
            graphics.pose().translate(panel.x + 3 * SCALE - offset * SCALE, yOffset + 2 * SCALE);
            graphics.pose().scale(SCALE, SCALE);
            graphics.drawString(font, scrollText, 0, 0, 0xFFFFFFFF, false);
            graphics.pose().popMatrix();
            graphics.disableScissor();
        } else {
            graphics.pose().pushMatrix();
            graphics.pose().translate(panel.x + 3 * SCALE, yOffset + 2 * SCALE);
            graphics.pose().scale(SCALE, SCALE);
            graphics.drawString(font, label, 0, 0, 0xFFFFFFFF, false);
            graphics.pose().popMatrix();
        }

        return yOffset + SETTING_HEIGHT;
    }

    private int renderRadioSetting(GuiGraphics graphics, Font font, CategoryPanel panel, RadioSetting radioSetting, int yOffset, int mouseX, int mouseY, int indent) {
        int settingX1 = panel.x, settingY1 = yOffset, settingX2 = panel.x + PANEL_WIDTH, settingY2 = yOffset + SETTING_HEIGHT;

        boolean isSettingHovered = mouseX >= settingX1 && mouseX <= settingX2 && mouseY >= settingY1 && mouseY <= settingY2;
        if (isSettingHovered && indent == 3) {
            graphics.fill(settingX1, settingY1, settingX2, settingY2, theme.hoverHighlight);
        }

        String selected = radioSetting.getSelectedOption();
        int indicatorColor = theme.moduleEnabled;
        String indicatorText = ">";
        float indicatorX = panel.x + indent * SCALE;

        graphics.pose().pushMatrix();
        graphics.pose().translate(indicatorX, yOffset + 2 * SCALE);
        graphics.pose().scale(SCALE, SCALE);
        graphics.drawString(font, indicatorText, 0, 0, indicatorColor, false);
        graphics.pose().popMatrix();

        String label = radioSetting.getName() + ": " + selected;
        int maxWidth = (int)((PANEL_WIDTH - indent * SCALE - 12 * SCALE) / SCALE);

        if (font.width(label) > maxWidth) {
            String key = "radio_" + indent + "_" + radioSetting.getName();
            if (isSettingHovered) {
                float offset = textScrollOffsets.getOrDefault(key, 0f);
                offset += 0.5f;
                float textWidth = font.width(label + "  " + label + "  ");
                if (offset > textWidth) offset = 0;
                textScrollOffsets.put(key, offset);
            } else {
                textScrollOffsets.put(key, 0f);
            }

            float offset = textScrollOffsets.get(key);
            String scrollText = label + "  " + label + "  " + label;

            int scissorX1 = (int)(indicatorX + 12 * SCALE);
            int scissorX2 = panel.x + PANEL_WIDTH - (int)(3 * SCALE);
            graphics.enableScissor(scissorX1, yOffset, scissorX2, yOffset + SETTING_HEIGHT);
            graphics.pose().pushMatrix();
            graphics.pose().translate(indicatorX + 12 * SCALE - offset * SCALE, yOffset + 2 * SCALE);
            graphics.pose().scale(SCALE, SCALE);
            graphics.drawString(font, scrollText, 0, 0, 0xFFFFFFFF, false);
            graphics.pose().popMatrix();
            graphics.disableScissor();
        } else {
            graphics.pose().pushMatrix();
            graphics.pose().translate(indicatorX + 12 * SCALE, yOffset + 2 * SCALE);
            graphics.pose().scale(SCALE, SCALE);
            graphics.drawString(font, label, 0, 0, 0xFFFFFFFF, false);
            graphics.pose().popMatrix();
        }

        return yOffset + SETTING_HEIGHT;
    }
}
package io.client;

import io.client.clickgui.SavedPanelConfig;
import io.client.clickgui.Theme;
import io.client.modules.settings.GUIScale;
import io.client.settings.BooleanSetting;
import io.client.settings.CategorySetting;
import io.client.settings.NumberSetting;
import io.client.settings.RadioSetting;
import io.client.settings.Setting;
import io.client.settings.StringSetting;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;

public class ModernClickGuiScreen extends Screen {
    private final List<ModernPanel> panels = new ArrayList<>();
    private final Map<String, Float> textScrollOffsets = new HashMap<>();
    private final Map<String, Float> animationProgress = new HashMap<>();
    private int tickCounter = 0;
    private float guiScale = 1.0f;

    public ModernClickGuiScreen() {
        super(Text.literal("Modern GUI"));

        GUIScale guiScaleModule = ModuleManager.INSTANCE.getModule(GUIScale.class);
        if (guiScaleModule != null) {
            guiScale = guiScaleModule.getScale();
        }

        initializePanels();
    }

    private void initializePanels() {
        panels.clear();
        Map<Category, SavedPanelConfig> loadedConfig = ModuleManager.INSTANCE.loadUiConfig();
        int defaultX = 12;

        for (Category category : Category.values()) {
            SavedPanelConfig config = loadedConfig.get(category);
            ModernPanel panel;

            if (config != null) {
                panel = new ModernPanel(category.name(), category, config.x, config.y);
                panel.open = !config.collapsed;
            } else {
                panel = new ModernPanel(category.name(), category, defaultX, 12);
                defaultX += (int)(148 * guiScale);
            }

            panels.add(panel);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        tickCounter++;

        GUIScale guiScaleModule = ModuleManager.INSTANCE.getModule(GUIScale.class);
        if (guiScaleModule != null) {
            guiScale = guiScaleModule.getScale();
        }

        Theme theme = ClickGuiScreen.currentTheme;
        ModernPalette palette = ModernPalette.fromTheme(theme);

        int width = this.client != null ? this.client.getWindow().getScaledWidth() : 0;
        int height = this.client != null ? this.client.getWindow().getScaledHeight() : 0;

        renderBackdrop(context, width, height, palette);

        context.getMatrices().pushMatrix();
        context.getMatrices().scale(guiScale, guiScale);

        float invScale = 1.0f / guiScale;
        int scaledMouseX = (int)(mouseX * invScale);
        int scaledMouseY = (int)(mouseY * invScale);

        for (ModernPanel panel : panels) {
            panel.render(context, scaledMouseX, scaledMouseY, delta, palette);
        }

        context.getMatrices().popMatrix();

        super.render(context, mouseX, mouseY, delta);
    }

    private void renderBackdrop(DrawContext context, int width, int height, ModernPalette palette) {
        context.fillGradient(0, 0, width, height, palette.bgTop, palette.bgBottom);

        int gridSize = 60;
        int gridColor = withAlpha(palette.accent, 8);
        for (int x = 0; x < width; x += gridSize) {
            context.fill(x, 0, x + 1, height, gridColor);
        }
        for (int y = 0; y < height; y += gridSize) {
            context.fill(0, y, width, y + 1, gridColor);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        float invScale = 1.0f / guiScale;
        int scaledMouseX = (int)(mouseX * invScale);
        int scaledMouseY = (int)(mouseY * invScale);

        for (ModernPanel panel : panels) {
            panel.mouseClicked(scaledMouseX, scaledMouseY, button);
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        float invScale = 1.0f / guiScale;
        int scaledMouseX = (int)(mouseX * invScale);
        int scaledMouseY = (int)(mouseY * invScale);

        for (ModernPanel panel : panels) {
            panel.mouseReleased(scaledMouseX, scaledMouseY, button);
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (button == 0) {
            float invScale = 1.0f / guiScale;
            int scaledMouseX = (int)(mouseX * invScale);
            int scaledMouseY = (int)(mouseY * invScale);

            for (ModernPanel panel : panels) {
                panel.drag(scaledMouseX, scaledMouseY);
            }
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void close() {
        Map<Category, io.client.clickgui.CategoryPanel> configMap = new HashMap<>();
        for (ModernPanel panel : panels) {
            io.client.clickgui.CategoryPanel categoryPanel = new io.client.clickgui.CategoryPanel(
                    panel.category, panel.x, panel.y, !panel.open
            );
            configMap.put(panel.category, categoryPanel);
        }
        ModuleManager.INSTANCE.saveUiConfig(configMap);
        ModuleManager.INSTANCE.saveModules();
        ModuleManager.INSTANCE.saveTheme(ClickGuiScreen.currentTheme);
        if (this.client != null) {
            this.client.setScreen(null);
        }
    }

    private final class ModernPanel {
        private static final int HEADER_HEIGHT = 28;
        private static final int CORNER_RADIUS = 8;
        private final String name;
        private final Category category;
        private final int width = 140;
        private final List<ModuleCard> cards = new ArrayList<>();
        private int x;
        private int y;
        private int dragOffsetX;
        private int dragOffsetY;
        private boolean dragging;
        private boolean open = true;
        private float expandProgress = 1.0f;

        private ModernPanel(String name, Category category, int x, int y) {
            this.name = name;
            this.category = category;
            this.x = x;
            this.y = y;
            ModuleManager.INSTANCE.getModulesByCategory(category).stream()
                    .sorted(Comparator.comparing(Module::getName))
                    .forEach(module -> cards.add(new ModuleCard(module)));
        }

        private void render(DrawContext context, int mouseX, int mouseY, float delta, ModernPalette palette) {
            float targetProgress = open ? 1.0f : 0.0f;
            expandProgress += (targetProgress - expandProgress) * 0.2f;

            int totalHeight = HEADER_HEIGHT + (int)(getBodyHeight() * expandProgress);

            renderGlass(context, x, y, width, totalHeight, palette);
            renderHeader(context, mouseX, mouseY, palette);

            if (expandProgress > 0.01f) {
                context.enableScissor(x, y + HEADER_HEIGHT, x + width, y + totalHeight);
                renderBody(context, mouseX, mouseY, palette);
                context.disableScissor();
            }
        }

        private void renderGlass(DrawContext context, int x, int y, int width, int height, ModernPalette palette) {
            context.fill(x, y, x + width, y + height, palette.glass);

            context.fill(x, y, x + width, y + 1, palette.borderBright);
            context.fill(x, y, x + 1, y + height, palette.borderBright);
            context.fill(x + width - 1, y, x + width, y + height, palette.borderDim);
            context.fill(x, y + height - 1, x + width, y + height, palette.borderDim);

            int innerGlow = withAlpha(palette.accent, 15);
            context.fill(x + 1, y + 1, x + width - 1, y + 2, innerGlow);
        }

        private void renderHeader(DrawContext context, int mouseX, int mouseY, ModernPalette palette) {
            boolean headerHover = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + HEADER_HEIGHT;

            if (headerHover) {
                int hoverOverlay = withAlpha(palette.accent, 20);
                context.fill(x, y, x + width, y + HEADER_HEIGHT, hoverOverlay);
            }

            drawGuiTextWithShadow(context, name.toUpperCase(), x + 10, y + 10, palette.textPrimary);

            String icon = open ? "−" : "+";
            int iconX = x + width - 20;
            drawGuiTextWithShadow(context, icon, iconX, y + 10, palette.textSecondary);

            context.fill(x + 8, y + HEADER_HEIGHT - 1, x + width - 8, y + HEADER_HEIGHT, palette.borderDim);
        }

        private void renderBody(DrawContext context, int mouseX, int mouseY, ModernPalette palette) {
            int cardY = y + HEADER_HEIGHT + 4;

            for (ModuleCard card : cards) {
                card.setBounds(x + 4, cardY, width - 8);
                card.render(context, mouseX, mouseY, palette);
                cardY += (int)(card.getHeight() * expandProgress) + 3;
            }
        }

        private int getBodyHeight() {
            int total = 4;
            for (ModuleCard card : cards) {
                total += card.getHeight() + 3;
            }
            return total + 4;
        }

        private void mouseClicked(int mouseX, int mouseY, int button) {
            if (hoveringHeader(mouseX, mouseY)) {
                if (button == 0) {
                    dragging = true;
                    dragOffsetX = x - mouseX;
                    dragOffsetY = y - mouseY;
                    return;
                }
                if (button == 1) {
                    open = !open;
                    playClick();
                    return;
                }
            }

            if (!open || expandProgress < 0.5f) return;

            for (ModuleCard card : cards) {
                card.mouseClicked(mouseX, mouseY, button);
            }
        }

        private void mouseReleased(int mouseX, int mouseY, int button) {
            if (button == 0) {
                dragging = false;
            }
            if (!open) return;
            for (ModuleCard card : cards) {
                card.mouseReleased(mouseX, mouseY, button);
            }
        }

        private void drag(int mouseX, int mouseY) {
            if (!dragging) return;
            this.x = dragOffsetX + mouseX;
            this.y = dragOffsetY + mouseY;
        }

        private boolean hoveringHeader(int mouseX, int mouseY) {
            return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + HEADER_HEIGHT;
        }
    }

    private final class ModuleCard {
        private static final int CARD_HEIGHT = 20;
        private final Module module;
        private final List<SettingRow> settingRows = new ArrayList<>();
        private boolean open;
        private float expandProgress = 0.0f;
        private int x;
        private int y;
        private int width;

        private ModuleCard(Module module) {
            this.module = module;
            appendSettingRows(settingRows, module.getSettings(), 0);
        }

        private void setBounds(int x, int y, int width) {
            this.x = x;
            this.y = y;
            this.width = width;
        }

        private int getHeight() {
            float settingsHeight = 0;
            if (open && !settingRows.isEmpty()) {
                for (SettingRow row : settingRows) {
                    settingsHeight += row.getHeight() + 2;
                }
                settingsHeight += 4;
            }
            return CARD_HEIGHT + (int)(settingsHeight * expandProgress);
        }

        private void render(DrawContext context, int mouseX, int mouseY, ModernPalette palette) {
            float targetProgress = (open && !settingRows.isEmpty()) ? 1.0f : 0.0f;
            expandProgress += (targetProgress - expandProgress) * 0.15f;

            boolean hovered = hovering(mouseX, mouseY);
            int cardBg = module.isEnabled() ? palette.cardEnabled : palette.cardDisabled;

            if (hovered) {
                cardBg = blendColors(cardBg, palette.accent, 0.15f);
            }

            context.fill(x, y, x + width, y + getHeight(), cardBg);

            if (module.isEnabled()) {
                context.fill(x, y, x + 2, y + CARD_HEIGHT, palette.accent);
            }

            int textColor = module.isEnabled() ? palette.textPrimary : palette.textSecondary;
            drawGuiTextWithShadow(context, module.getName(), x + 8, y + 6, textColor);

            if (!settingRows.isEmpty()) {
                String gear = "⚙";
                int gearColor = open ? palette.accent : palette.textSecondary;
                drawGuiTextWithShadow(context, gear, x + width - 14, y + 6, gearColor);
            }

            if (expandProgress > 0.01f && open) {
                int settingY = y + CARD_HEIGHT + 4;
                context.enableScissor(x, y + CARD_HEIGHT, x + width, y + getHeight());
                for (SettingRow row : settingRows) {
                    row.setBounds(x + 6, settingY, width - 12);
                    row.render(context, mouseX, mouseY, palette);
                    settingY += row.getHeight() + 2;
                }
                context.disableScissor();
            }
        }

        private void mouseClicked(int mouseX, int mouseY, int button) {
            if (hovering(mouseX, mouseY)) {
                if (button == 0) {
                    module.toggle();
                    playClick();
                } else if (button == 1 && !settingRows.isEmpty()) {
                    open = !open;
                    playClick();
                }
                return;
            }

            if (open && expandProgress > 0.5f) {
                for (SettingRow row : settingRows) {
                    row.mouseClicked(mouseX, mouseY, button);
                }
            }
        }

        private void mouseReleased(int mouseX, int mouseY, int button) {
            if (!open) return;
            for (SettingRow row : settingRows) {
                row.mouseReleased(mouseX, mouseY, button);
            }
        }

        private boolean hovering(int mouseX, int mouseY) {
            return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + CARD_HEIGHT;
        }
    }

    private abstract static class SettingRow {
        protected int x;
        protected int y;
        protected int width;
        protected final int indent;

        protected SettingRow(int indent) {
            this.indent = indent;
        }

        void setBounds(int x, int y, int width) {
            this.x = x;
            this.y = y;
            this.width = width;
        }

        abstract void render(DrawContext context, int mouseX, int mouseY, ModernPalette palette);

        void mouseClicked(int mouseX, int mouseY, int button) {}
        void mouseReleased(int mouseX, int mouseY, int button) {}

        int getHeight() {
            return 16;
        }

        boolean hovering(int mouseX, int mouseY) {
            return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + getHeight();
        }
    }

    private final class BooleanSettingRow extends SettingRow {
        private final BooleanSetting setting;

        private BooleanSettingRow(BooleanSetting setting, int indent) {
            super(indent);
            this.setting = setting;
        }

        @Override
        void render(DrawContext context, int mouseX, int mouseY, ModernPalette palette) {
            boolean hovered = hovering(mouseX, mouseY);

            if (hovered) {
                int hoverBg = withAlpha(palette.accent, 15);
                context.fill(x, y, x + width, y + getHeight(), hoverBg);
            }

            int checkX = x + indent;
            int checkSize = 10;
            int checkY = y + 3;

            int checkBg = setting.isEnabled() ? palette.accent : palette.borderDim;
            context.fill(checkX, checkY, checkX + checkSize, checkY + checkSize, checkBg);

            if (setting.isEnabled()) {
                context.fill(checkX + 2, checkY + 2, checkX + checkSize - 2, checkY + checkSize - 2, palette.textPrimary);
            }

            drawGuiTextWithShadow(context, setting.getName(), checkX + checkSize + 6, y + 4, palette.textPrimary);
        }

        @Override
        void mouseClicked(int mouseX, int mouseY, int button) {
            if (button == 0 && hovering(mouseX, mouseY)) {
                setting.toggle();
                playClick();
            }
        }
    }

    private final class RadioSettingRow extends SettingRow {
        private final RadioSetting setting;

        private RadioSettingRow(RadioSetting setting, int indent) {
            super(indent);
            this.setting = setting;
        }

        @Override
        void render(DrawContext context, int mouseX, int mouseY, ModernPalette palette) {
            boolean hovered = hovering(mouseX, mouseY);

            if (hovered) {
                int hoverBg = withAlpha(palette.accent, 15);
                context.fill(x, y, x + width, y + getHeight(), hoverBg);
            }

            String label = setting.getName() + ": " + setting.getSelectedOption();
            drawGuiTextWithShadow(context, label, x + indent, y + 4, palette.textPrimary);

            drawGuiTextWithShadow(context, "◀ ▶", x + width - 28, y + 4, palette.textSecondary);
        }

        @Override
        void mouseClicked(int mouseX, int mouseY, int button) {
            if (hovering(mouseX, mouseY)) {
                if (button == 0) {
                    setting.cycleNext();
                } else if (button == 1) {
                    List<String> options = setting.getOptions();
                    if (!options.isEmpty()) {
                        int current = options.indexOf(setting.getSelectedOption());
                        int prev = (current - 1 + options.size()) % options.size();
                        setting.setSelectedOption(options.get(prev));
                    }
                }
                playClick();
            }
        }
    }

    private final class NumberSettingRow extends SettingRow {
        private final NumberSetting setting;
        private boolean dragging;

        private NumberSettingRow(NumberSetting setting, int indent) {
            super(indent);
            this.setting = setting;
        }

        @Override
        void render(DrawContext context, int mouseX, int mouseY, ModernPalette palette) {
            boolean hovered = hovering(mouseX, mouseY);

            if (hovered) {
                int hoverBg = withAlpha(palette.accent, 15);
                context.fill(x, y, x + width, y + getHeight(), hoverBg);
            }

            String label = setting.getName() + ": " + format(setting.getValue());
            drawGuiTextWithShadow(context, label, x + indent, y + 2, palette.textPrimary);

            float range = setting.getMax() - setting.getMin();
            float normalized = range <= 0.0f ? 0.0f : (setting.getValue() - setting.getMin()) / range;

            int sliderY = y + 12;
            int sliderHeight = 2;

            context.fill(x + indent, sliderY, x + width - 2, sliderY + sliderHeight, palette.borderDim);

            int fillWidth = (int)(normalized * (width - indent - 2));
            context.fill(x + indent, sliderY, x + indent + fillWidth, sliderY + sliderHeight, palette.accent);

            int thumbX = x + indent + fillWidth - 2;
            context.fill(thumbX, sliderY - 1, thumbX + 4, sliderY + sliderHeight + 1, palette.accent);

            if (dragging) {
                setFromMouse(mouseX);
            }
        }

        @Override
        void mouseClicked(int mouseX, int mouseY, int button) {
            if (button == 0 && hovering(mouseX, mouseY)) {
                dragging = true;
                setFromMouse(mouseX);
                playClick();
            }
        }

        @Override
        void mouseReleased(int mouseX, int mouseY, int button) {
            if (button == 0) {
                dragging = false;
            }
        }

        private void setFromMouse(int mouseX) {
            int sliderWidth = width - indent - 2;
            float t = (mouseX - (x + indent)) / (float) sliderWidth;
            t = Math.max(0.0f, Math.min(1.0f, t));
            float value = setting.getMin() + (setting.getMax() - setting.getMin()) * t;
            setting.setValue(value);
        }
    }

    private final class StringSettingRow extends SettingRow {
        private final StringSetting setting;

        private StringSettingRow(StringSetting setting, int indent) {
            super(indent);
            this.setting = setting;
        }

        @Override
        void render(DrawContext context, int mouseX, int mouseY, ModernPalette palette) {
            boolean hovered = hovering(mouseX, mouseY);

            if (hovered) {
                int hoverBg = withAlpha(palette.accent, 15);
                context.fill(x, y, x + width, y + getHeight(), hoverBg);
            }

            String label = setting.getName() + ": \"" + setting.getValue() + "\"";
            drawGuiTextWithShadow(context, label, x + indent, y + 4, palette.textPrimary);
        }
    }

    private final class CategorySettingRow extends SettingRow {
        private final CategorySetting setting;
        private final List<SettingRow> children = new ArrayList<>();
        private boolean open;
        private float expandProgress = 0.0f;

        private CategorySettingRow(CategorySetting setting, int indent) {
            super(indent);
            this.setting = setting;
            this.open = setting.isExpanded();
        }

        @Override
        void render(DrawContext context, int mouseX, int mouseY, ModernPalette palette) {
            boolean hovered = hovering(mouseX, mouseY);

            if (hovered) {
                int hoverBg = withAlpha(palette.accent, 15);
                context.fill(x, y, x + width, y + getHeight(), hoverBg);
            }

            String arrow = open ? "▼" : "▶";
            drawGuiTextWithShadow(context, arrow, x + indent, y + 4, palette.accent);
            drawGuiTextWithShadow(context, setting.getName(), x + indent + 12, y + 4, palette.accent);

            float targetProgress = open ? 1.0f : 0.0f;
            expandProgress += (targetProgress - expandProgress) * 0.15f;

            if (expandProgress > 0.01f && open) {
                int sy = y + 16;
                for (SettingRow child : children) {
                    child.setBounds(x, sy, width);
                    child.render(context, mouseX, mouseY, palette);
                    sy += child.getHeight() + 2;
                }
            }
        }

        @Override
        int getHeight() {
            if (!open || expandProgress < 0.01f) return 16;
            int total = 16;
            for (SettingRow child : children) {
                total += (int)((child.getHeight() + 2) * expandProgress);
            }
            return total;
        }

        @Override
        void mouseClicked(int mouseX, int mouseY, int button) {
            if (hovering(mouseX, mouseY) && button == 1) {
                open = !open;
                setting.setExpanded(open);
                playClick();
                return;
            }
            if (!open || expandProgress < 0.5f) return;
            for (SettingRow child : children) {
                child.mouseClicked(mouseX, mouseY, button);
            }
        }

        @Override
        void mouseReleased(int mouseX, int mouseY, int button) {
            if (!open) return;
            for (SettingRow child : children) {
                child.mouseReleased(mouseX, mouseY, button);
            }
        }

        @Override
        boolean hovering(int mouseX, int mouseY) {
            return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + 16;
        }
    }

    private void appendSettingRows(List<SettingRow> targetRows, List<?> settings, int indent) {
        for (Object obj : settings) {
            if (!(obj instanceof Setting setting)) continue;

            if (setting instanceof CategorySetting categorySetting) {
                CategorySettingRow categoryRow = new CategorySettingRow(categorySetting, indent);
                appendSettingRows(categoryRow.children, categorySetting.getSettings(), indent + 8);
                targetRows.add(categoryRow);
            } else if (setting instanceof BooleanSetting boolSetting) {
                targetRows.add(new BooleanSettingRow(boolSetting, indent));
            } else if (setting instanceof RadioSetting radioSetting) {
                targetRows.add(new RadioSettingRow(radioSetting, indent));
            } else if (setting instanceof NumberSetting numberSetting) {
                targetRows.add(new NumberSettingRow(numberSetting, indent));
            } else if (setting instanceof StringSetting stringSetting) {
                targetRows.add(new StringSettingRow(stringSetting, indent));
            }
        }
    }

    private static String format(float value) {
        if (Math.abs(value - Math.round(value)) < 0.001f)
            return Integer.toString(Math.round(value));
        return String.format("%.2f", value);
    }

    private int getGuiTextWidth(String text) {
        if (textRenderer == null) return 0;
        if (!ClickGuiScreen.useJetBrainsMonoFont())
            return textRenderer.getWidth(text);
        return textRenderer.getWidth(ClickGuiScreen.styledGuiText(text));
    }

    private void drawGuiTextWithShadow(DrawContext context, String text, int x, int y, int color) {
        if (textRenderer == null) return;
        if (!ClickGuiScreen.useJetBrainsMonoFont()) {
            context.drawTextWithShadow(textRenderer, text, x, y, color);
            return;
        }
        context.drawTextWithShadow(textRenderer, ClickGuiScreen.styledGuiText(text), x, y, color);
    }

    private static int withAlpha(int color, int alpha) {
        return (alpha << 24) | (color & 0x00FFFFFF);
    }

    private static int blendColors(int color1, int color2, float ratio) {
        int r1 = (color1 >> 16) & 0xFF;
        int g1 = (color1 >> 8) & 0xFF;
        int b1 = color1 & 0xFF;

        int r2 = (color2 >> 16) & 0xFF;
        int g2 = (color2 >> 8) & 0xFF;
        int b2 = color2 & 0xFF;

        int r = (int)(r1 + (r2 - r1) * ratio);
        int g = (int)(g1 + (g2 - g1) * ratio);
        int b = (int)(b1 + (b2 - b1) * ratio);

        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private record ModernPalette(
            int bgTop,
            int bgBottom,
            int glass,
            int cardEnabled,
            int cardDisabled,
            int accent,
            int borderBright,
            int borderDim,
            int textPrimary,
            int textSecondary) {

        static ModernPalette fromTheme(Theme theme) {
            int baseAccent = theme.moduleEnabled & 0x00FFFFFF;
            int baseBg = theme.panelBackground & 0x00FFFFFF;

            return new ModernPalette(
                    withAlpha(baseBg, 40),
                    withAlpha(baseBg, 90),
                    withAlpha(baseBg, 160),
                    withAlpha(baseBg, 200),
                    withAlpha(baseBg, 140),
                    withAlpha(baseAccent, 255),
                    withAlpha(0xFFFFFF, 25),
                    withAlpha(0x000000, 40),
                    0xFFFFFFFF,
                    withAlpha(0xFFFFFF, 180)
            );
        }
    }

    private static void playClick() {
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null && client.getSoundManager() != null) {
                client.getSoundManager().play(
                        PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK.value(), 1.2f)
                );
            }
        } catch (Exception ignored) {}
    }
}
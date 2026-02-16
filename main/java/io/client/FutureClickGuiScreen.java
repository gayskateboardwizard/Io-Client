package io.client;

import io.client.clickgui.Theme;
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
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class FutureClickGuiScreen extends Screen {
    private static final Identifier ARROW_TEXTURE = Identifier.of("io_client", "textures/future/arrow.png");
    private static final Identifier GEAR_TEXTURE = Identifier.of("io_client", "textures/future/gear.png");
    private final List<Panel> panels = new ArrayList<>();
    private final Map<String, Float> textScrollOffsets = new HashMap<>();

    public FutureClickGuiScreen() {
        super(Text.literal("Future GUI"));
        initializePanels();
    }

    private void initializePanels() {
        panels.clear();
        int x = 6;
        for (Category category : Category.values()) {
            Panel panel = new Panel(category.name(), category, x, 8);
            panels.add(panel);
            x += panel.width + 4;
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        Theme theme = ClickGuiScreen.currentTheme;
        FuturePalette palette = FuturePalette.fromTheme(theme);
        int width = this.client != null ? this.client.getWindow().getScaledWidth() : 0;
        int height = this.client != null ? this.client.getWindow().getScaledHeight() : 0;
        context.fillGradient(0, 0, width, height, palette.backgroundTop, palette.backgroundBottom);
        for (Panel panel : panels) {
            panel.render(context, mouseX, mouseY, delta, theme, palette);
        }
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (Panel panel : panels) {
            panel.mouseClicked((int) mouseX, (int) mouseY, button);
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        for (Panel panel : panels) {
            panel.mouseReleased((int) mouseX, (int) mouseY, button);
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (button == 0) {
            for (Panel panel : panels) {
                panel.drag((int) mouseX, (int) mouseY);
            }
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private final class Panel {
        private static final int HEADER_HEIGHT = 15;
        private final String name;
        private final Category category;
        private final int width = 92;
        private final List<ModuleRow> rows = new ArrayList<>();
        private int x;
        private int y;
        private int dragOffsetX;
        private int dragOffsetY;
        private boolean dragging;
        private boolean open = true;
        private int angle = 180;

        private Panel(String name, Category category, int x, int y) {
            this.name = name;
            this.category = category;
            this.x = x;
            this.y = y;
            ModuleManager.INSTANCE.getModulesByCategory(category).stream()
                    .sorted(Comparator.comparing(Module::getName))
                    .forEach(module -> rows.add(new ModuleRow(module)));
        }

        private void render(DrawContext context, int mouseX, int mouseY, float delta, Theme theme, FuturePalette palette) {
            context.fill(x, y, x + width, y + HEADER_HEIGHT, palette.panelHeader);
            context.fill(x, y + HEADER_HEIGHT - 1, x + width, y + HEADER_HEIGHT, palette.accentSoft);
            context.drawTextWithShadow(textRenderer, name, x + 4, y + 4, palette.textMain);
            if (!open) {
                if (angle > 0) angle -= 8;
                if (category == Category.COMBAT) {
                    context.drawTextWithShadow(textRenderer, "T", x + width - 17, y + 4, palette.accentStrong);
                }
                drawArrow(context, x + width - 8, y + 7, angle);
                return;
            }

            if (angle < 180) angle += 8;
            if (category == Category.COMBAT) {
                context.drawTextWithShadow(textRenderer, "T", x + width - 17, y + 4, palette.accentStrong);
            }
            drawArrow(context, x + width - 8, y + 7, angle);
            context.fill(x, y + HEADER_HEIGHT, x + width, y + HEADER_HEIGHT + getBodyHeight(), palette.panelBody);

            int rowY = y + HEADER_HEIGHT;
            for (ModuleRow row : rows) {
                row.setBounds(x, rowY, width);
                row.render(context, mouseX, mouseY, palette);
                rowY += row.getHeight();
            }
        }

        private int getBodyHeight() {
            int body = 0;
            for (ModuleRow row : rows) {
                body += row.getHeight();
            }
            return body;
        }

        private void mouseClicked(int mouseX, int mouseY, int button) {
            if (hoveringHeader(mouseX, mouseY)) {
                if (category == Category.COMBAT && mouseX >= x + width - 22 && mouseX <= x + width - 12) {
                    if (FutureClickGuiScreen.this.client != null) {
                        FutureClickGuiScreen.this.client.setScreen(new TargetsScreen(FutureClickGuiScreen.this));
                    }
                    return;
                }
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
            if (!open) return;
            for (ModuleRow row : rows) {
                row.mouseClicked(mouseX, mouseY, button);
            }
        }

        private void mouseReleased(int mouseX, int mouseY, int button) {
            if (button == 0) {
                dragging = false;
            }
            if (!open) return;
            for (ModuleRow row : rows) {
                row.mouseReleased(mouseX, mouseY, button);
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

    private final class ModuleRow {
        private static final int ROW_HEIGHT = 14;
        private final Module module;
        private final List<SettingRow> settingRows = new ArrayList<>();
        private boolean open;
        private int gearAngle;
        private int x;
        private int y;
        private int width;

        private ModuleRow(Module module) {
            this.module = module;
            appendSettingRows(settingRows, module.getSettings(), 0);
        }

        private void setBounds(int x, int y, int width) {
            this.x = x;
            this.y = y;
            this.width = width;
        }

        private int getHeight() {
            if (!open || settingRows.isEmpty()) return ROW_HEIGHT;
            int extra = 0;
            for (SettingRow row : settingRows) {
                extra += row.getHeight();
            }
            return ROW_HEIGHT + extra;
        }

        private void render(DrawContext context, int mouseX, int mouseY, FuturePalette palette) {
            int bg = module.isEnabled() ? palette.moduleEnabled : palette.moduleDisabled;
            context.fill(x, y, x + width, y + ROW_HEIGHT, bg);
            boolean hovered = hovering(mouseX, mouseY);
            if (hovered) {
                context.fill(x, y, x + width, y + ROW_HEIGHT, palette.rowHover);
            }
            int labelStart = x + 3;
            int labelEnd = settingRows.isEmpty() ? (x + width - 3) : (x + width - 14);
            drawScrollableText(
                    context,
                    "module:" + module.getName(),
                    module.getName(),
                    labelStart,
                    y + 3,
                    palette.textMain,
                    labelStart,
                    labelEnd,
                    hovered,
                    0.5f
            );
            if (!settingRows.isEmpty()) {
                if (open) {
                    gearAngle = Math.min(gearAngle + 10, 180);
                } else {
                    gearAngle = Math.max(gearAngle - 10, 0);
                }
                drawGear(context, x + width - 7, y + 7, gearAngle);
            }

            if (!open) return;
            int sy = y + ROW_HEIGHT;
            for (SettingRow row : settingRows) {
                row.setBounds(x + 1, sy, width - 2);
                row.render(context, mouseX, mouseY, palette);
                sy += row.getHeight();
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
            if (open) {
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
            return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + ROW_HEIGHT;
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

        abstract void render(DrawContext context, int mouseX, int mouseY, FuturePalette palette);

        void mouseClicked(int mouseX, int mouseY, int button) {
        }

        void mouseReleased(int mouseX, int mouseY, int button) {
        }

        int getHeight() {
            return 14;
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
        void render(DrawContext context, int mouseX, int mouseY, FuturePalette palette) {
            int color = setting.isEnabled() ? palette.settingEnabled : palette.settingDisabled;
            context.fill(x, y, x + width, y + getHeight(), color);
            int labelStart = x + 3 + indent;
            int labelEnd = x + width - 3;
            drawScrollableText(
                    context,
                    "bool:" + System.identityHashCode(this),
                    setting.getName(),
                    labelStart,
                    y + 3,
                    palette.textMain,
                    labelStart,
                    labelEnd,
                    hovering(mouseX, mouseY),
                    0.5f
            );
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
        void render(DrawContext context, int mouseX, int mouseY, FuturePalette palette) {
            context.fill(x, y, x + width, y + getHeight(), palette.settingDisabled);
            int labelStart = x + 3 + indent;
            int labelEnd = x + width - 3;
            int baseY = y + 3;
            String namePart = setting.getName() + ": ";
            String optionPart = setting.getSelectedOption();
            int nameWidth = textRenderer.getWidth(namePart);
            int optionWidth = textRenderer.getWidth(optionPart);
            int available = Math.max(0, labelEnd - labelStart);

            if ((nameWidth + optionWidth) <= available) {
                context.drawTextWithShadow(textRenderer, namePart, labelStart, baseY, palette.textMuted);
                context.drawTextWithShadow(textRenderer, optionPart, labelStart + nameWidth, baseY, palette.accentStrong);
            } else {
                String combined = namePart + optionPart;
                drawScrollableText(
                        context,
                        "radio:" + System.identityHashCode(this),
                        combined,
                        labelStart,
                        baseY,
                        palette.textMain,
                        labelStart,
                        labelEnd,
                        hovering(mouseX, mouseY),
                        0.5f
                );
            }
        }

        @Override
        void mouseClicked(int mouseX, int mouseY, int button) {
            if (hovering(mouseX, mouseY) && (button == 0 || button == 1)) {
                if (button == 0) {
                    setting.cycleNext();
                } else {
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
        void render(DrawContext context, int mouseX, int mouseY, FuturePalette palette) {
            context.fill(x, y, x + width, y + getHeight(), palette.settingDisabled);
            float range = setting.getMax() - setting.getMin();
            float normalized = range <= 0.0f ? 0.0f : (setting.getValue() - setting.getMin()) / range;
            int sliderStart = x + 2 + indent;
            int sliderWidth = Math.max(10, width - 4 - indent);
            int fill = (int) (normalized * sliderWidth);
            context.fill(sliderStart, y + getHeight() - 3, sliderStart + sliderWidth, y + getHeight() - 1, palette.sliderTrack);
            context.fill(sliderStart, y + getHeight() - 3, sliderStart + fill, y + getHeight() - 1, palette.sliderFill);
            String label = setting.getName() + ": " + format(setting.getValue());
            int labelStart = x + 3 + indent;
            int labelEnd = x + width - 3;
            drawScrollableText(
                    context,
                    "number:" + System.identityHashCode(this),
                    label,
                    labelStart,
                    y + 3,
                    palette.textMain,
                    labelStart,
                    labelEnd,
                    hovering(mouseX, mouseY),
                    0.5f
            );
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
            int sliderStart = x + 2 + indent;
            int sliderWidth = Math.max(10, width - 4 - indent);
            float t = (mouseX - sliderStart) / (float) sliderWidth;
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
        void render(DrawContext context, int mouseX, int mouseY, FuturePalette palette) {
            context.fill(x, y, x + width, y + getHeight(), palette.settingDisabled);
            String label = setting.getName() + ": \"" + setting.getValue() + "\"";
            int labelStart = x + 3 + indent;
            int labelEnd = x + width - 3;
            drawScrollableText(
                    context,
                    "string:" + System.identityHashCode(this),
                    label,
                    labelStart,
                    y + 3,
                    palette.textMain,
                    labelStart,
                    labelEnd,
                    hovering(mouseX, mouseY),
                    0.5f
            );
        }
    }

    private final class CategorySettingRow extends SettingRow {
        private final CategorySetting setting;
        private final List<SettingRow> children = new ArrayList<>();
        private boolean open;

        private CategorySettingRow(CategorySetting setting, int indent) {
            super(indent);
            this.setting = setting;
            this.open = setting.isExpanded();
        }

        @Override
        void render(DrawContext context, int mouseX, int mouseY, FuturePalette palette) {
            context.fill(x, y, x + width, y + getHeight(), palette.categoryRow);
            context.drawTextWithShadow(textRenderer, open ? "-" : "+", x + 3 + indent, y + 3, palette.accentStrong);
            int labelStart = x + 12 + indent;
            int labelEnd = x + width - 3;
            drawScrollableText(
                    context,
                    "category:" + System.identityHashCode(this),
                    setting.getName(),
                    labelStart,
                    y + 3,
                    palette.textMain,
                    labelStart,
                    labelEnd,
                    hovering(mouseX, mouseY),
                    0.5f
            );

            if (!open) return;
            int sy = y + 14;
            for (SettingRow child : children) {
                child.setBounds(x, sy, width);
                child.render(context, mouseX, mouseY, palette);
                sy += child.getHeight();
            }
        }

        @Override
        int getHeight() {
            if (!open) return 14;
            int total = 14;
            for (SettingRow child : children) {
                total += child.getHeight();
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
            if (!open) return;
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
        if (Math.abs(value - Math.round(value)) < 0.001f) return Integer.toString(Math.round(value));
        return String.format("%.2f", value);
    }

    private void drawScrollableText(
            DrawContext context,
            String key,
            String text,
            int x,
            int y,
            int color,
            int clipX1,
            int clipX2,
            boolean hovered,
            float speed
    ) {
        if (textRenderer == null) return;
        int maxWidth = Math.max(0, clipX2 - clipX1);
        int textWidth = textRenderer.getWidth(text);

        if (textWidth <= maxWidth) {
            textScrollOffsets.put(key, 0.0f);
            context.drawTextWithShadow(textRenderer, text, x, y, color);
            return;
        }

        float offset = textScrollOffsets.getOrDefault(key, 0.0f);
        if (hovered) {
            offset += speed;
            float cycleWidth = textWidth + 30.0f;
            if (offset > cycleWidth) offset = 0.0f;
            textScrollOffsets.put(key, offset);
        } else {
            offset = 0.0f;
            textScrollOffsets.put(key, 0.0f);
        }

        String scrollText = text + "  " + text + "  " + text;
        context.enableScissor(clipX1, y, clipX2, y + 10);
        context.getMatrices().pushMatrix();
        context.getMatrices().translate(x - offset, y);
        context.drawTextWithShadow(textRenderer, scrollText, 0, 0, color);
        context.getMatrices().popMatrix();
        context.disableScissor();
    }

    private static int withAlpha(int color, int alpha) {
        return (alpha << 24) | (color & 0x00FFFFFF);
    }

    private static int blendRgb(int c1, int c2, float t) {
        t = Math.max(0.0f, Math.min(1.0f, t));
        int r1 = (c1 >> 16) & 0xFF, g1 = (c1 >> 8) & 0xFF, b1 = c1 & 0xFF;
        int r2 = (c2 >> 16) & 0xFF, g2 = (c2 >> 8) & 0xFF, b2 = c2 & 0xFF;
        int r = (int) (r1 + (r2 - r1) * t);
        int g = (int) (g1 + (g2 - g1) * t);
        int b = (int) (b1 + (b2 - b1) * t);
        return (r << 16) | (g << 8) | b;
    }

    private record FuturePalette(
            int backgroundTop,
            int backgroundBottom,
            int panelHeader,
            int panelBody,
            int accentSoft,
            int accentStrong,
            int moduleEnabled,
            int moduleDisabled,
            int rowHover,
            int categoryRow,
            int settingEnabled,
            int settingDisabled,
            int sliderTrack,
            int sliderFill,
            int textMain,
            int textMuted
    ) {
        static FuturePalette fromTheme(Theme theme) {
            int accent = theme.moduleEnabled & 0x00FFFFFF;
            int text = 0xFFFFFFFF;
            int bgDark = blendRgb(theme.panelBackground & 0x00FFFFFF, 0x000000, 0.55f);
            int bgDeeper = blendRgb(theme.titleBar & 0x00FFFFFF, 0x000000, 0.65f);
            int offBase = blendRgb(theme.moduleDisabled & 0x00FFFFFF, bgDark, 0.35f);
            int onBase = blendRgb(accent, bgDark, 0.20f);
            int settingOff = blendRgb(theme.sliderBackground & 0x00FFFFFF, bgDark, 0.20f);
            int settingOn = blendRgb(theme.sliderForeground & 0x00FFFFFF, bgDark, 0.15f);

            return new FuturePalette(
                    0x13000000 | bgDeeper,
                    0xB8000000 | bgDark,
                    withAlpha(bgDeeper, 235),
                    withAlpha(bgDark, 195),
                    withAlpha(accent, 140),
                    withAlpha(accent, 235),
                    withAlpha(onBase, 225),
                    withAlpha(offBase, 205),
                    withAlpha(accent, 70),
                    withAlpha(blendRgb(settingOff, accent, 0.12f), 215),
                    withAlpha(settingOn, 220),
                    withAlpha(settingOff, 220),
                    withAlpha(blendRgb(settingOff, 0xFFFFFF, 0.08f), 255),
                    withAlpha(accent, 255),
                    text,
                    withAlpha(blendRgb(theme.moduleDisabled & 0x00FFFFFF, 0xB0B0B0, 0.45f), 255)
            );
        }
    }

    private static void drawArrow(DrawContext context, float x, float y, float angle) {
        context.getMatrices().pushMatrix();
        context.getMatrices().translate(x, y);
        context.getMatrices().rotate((float) Math.toRadians(calculateRotation(angle)));
        context.drawTexture(RenderPipelines.GUI_TEXTURED, ARROW_TEXTURE, -5, -5, 0.0f, 0.0f, 10, 10, 10, 10);
        context.getMatrices().popMatrix();
    }

    private static void drawGear(DrawContext context, float x, float y, float angle) {
        context.getMatrices().pushMatrix();
        context.getMatrices().translate(x, y);
        context.getMatrices().rotate((float) Math.toRadians(calculateRotation(angle)));
        context.drawTexture(RenderPipelines.GUI_TEXTURED, GEAR_TEXTURE, -5, -5, 0.0f, 0.0f, 10, 10, 10, 10);
        context.getMatrices().popMatrix();
    }

    private static float calculateRotation(float angle) {
        angle %= 360.0f;
        if (angle >= 180.0f) angle -= 360.0f;
        if (angle < -180.0f) angle += 360.0f;
        return angle;
    }

    private static void playClick() {
        if (MinecraftClientHolder.CLIENT == null || MinecraftClientHolder.CLIENT.getSoundManager() == null) return;
        MinecraftClientHolder.CLIENT.getSoundManager()
                .play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK.value(), 1.0f));
    }

    private static final class MinecraftClientHolder {
        private static final net.minecraft.client.MinecraftClient CLIENT = net.minecraft.client.MinecraftClient.getInstance();
    }
}


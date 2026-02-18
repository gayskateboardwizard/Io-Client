package io.client.clickgui.screens;

import io.client.modules.templates.Module;
import io.client.clickgui.SavedPanelConfig;
import io.client.clickgui.Theme;
import io.client.managers.ModuleManager;
import io.client.modules.settings.GUIScale;
import io.client.modules.templates.Category;
import io.client.settings.BooleanSetting;
import io.client.settings.CategorySetting;
import io.client.settings.NumberSetting;
import io.client.settings.RadioSetting;
import io.client.settings.Setting;
import io.client.settings.StringSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BasicClickGuiScreen extends Screen {

    private static final int PANEL_WIDTH   = 90;
    private static final int HEADER_HEIGHT = 14;
    private static final int ROW_HEIGHT    = 13;
    private static final float SCROLL_SPEED = 0.4f;
    private static final int SCROLL_GAP    = 20;

    private String searchQuery = "";
    private boolean typingSearch = false;
    private boolean draggingScale = false;

    private final Map<String, Float> scrollOffsets = new HashMap<>();
    private final List<Panel> panels = new ArrayList<>();

    public BasicClickGuiScreen() {
        super(Text.literal("Basic GUI"));
        initPanels();
    }

    private void initPanels() {
        Map<Category, SavedPanelConfig> loaded = ModuleManager.INSTANCE.loadUiConfig();
        int defaultX = 5;
        for (Category category : Category.values()) {
            SavedPanelConfig cfg = loaded.get(category);
            Panel p;
            if (cfg != null) {
                p = new Panel(category, cfg.x, cfg.y);
                p.open = !cfg.collapsed;
            } else {
                p = new Panel(category, defaultX, 5);
                defaultX += PANEL_WIDTH + 4;
            }
            panels.add(p);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        Theme theme = ClickGuiScreen.currentTheme;

        renderGradientFill(context, 0, 0, width, height,
                (theme.panelBackground & 0x00FFFFFF) | 0x55000000,
                (theme.panelBackground & 0x00FFFFFF) | 0x55000000);

        GUIScale guiScale = ModuleManager.INSTANCE.getModule(GUIScale.class);
        float scale = guiScale != null ? guiScale.getScale() : 1.0f;

        MinecraftClient mc = MinecraftClient.getInstance();
        int sw = mc.getWindow().getScaledWidth();
        int sh = mc.getWindow().getScaledHeight();

        float invScale = 1.0f / scale;
        int scaledMX = (int) (mouseX * invScale);
        int scaledMY = (int) (mouseY * invScale);

        context.getMatrices().pushMatrix();
        context.getMatrices().scale(scale, scale);

        for (Panel p : panels) {
            p.clamp(sw, sh);
            p.render(context, scaledMX, scaledMY, theme);
        }

        context.getMatrices().popMatrix();

        renderScaleSlider(context, (int) mouseX, (int) mouseY, theme, guiScale, scale);
        renderSearchBar(context, (int) mouseX, (int) mouseY, theme);

        super.render(context, mouseX, mouseY, delta);
    }

    private void renderScaleSlider(DrawContext ctx, int mx, int my, Theme theme, GUIScale guiScale, float scale) {
        int barW = 120;
        int barH = 10;
        int bx = width / 2 - barW / 2;
        int by = height - 32;

        ctx.fill(bx, by, bx + barW, by + barH, (theme.panelBackground & 0x00FFFFFF) | 0xAA000000);
        ctx.fill(bx + 1, by, bx + barW - 1, by + 1, 0x22FFFFFF);
        ctx.fill(bx + 1, by + barH - 1, bx + barW - 1, by + barH, 0x22000000);
        ctx.fill(bx, by, bx + 1, by + barH, 0x22FFFFFF);
        ctx.fill(bx + barW - 1, by, bx + barW, by + barH, 0x22FFFFFF);
        renderOutline(ctx, bx, by, barW, barH, (theme.moduleDisabled & 0x00FFFFFF) | 0x66000000);

        float min = 0.5f, max = 2.0f;
        float norm = (scale - min) / (max - min);
        int fillW = (int) (norm * (barW - 4));
        ctx.fill(bx + 2, by + 3, bx + 2 + fillW, by + barH - 3, (theme.moduleEnabled & 0x00FFFFFF) | 0xBB000000);

        int knobX = bx + 2 + fillW;
        ctx.fill(knobX - 1, by + 1, knobX + 1, by + barH - 1, 0xCCFFFFFF);

        String label = "scale: " + fmt(scale);
        int lx = bx + barW / 2 - textRenderer.getWidth(label) / 2;
        ctx.drawTextWithShadow(textRenderer, label, lx, by + 1, (theme.moduleDisabled & 0x00FFFFFF) | 0xCC000000);

        if (draggingScale && guiScale != null) {
            float t = Math.max(0f, Math.min(1f, (mx - bx - 2f) / (barW - 4f)));
            guiScale.getSettings().stream()
                    .filter(s -> s instanceof NumberSetting && s.getName().equals("Scale"))
                    .findFirst()
                    .ifPresent(s -> ((NumberSetting) s).setValue(min + (max - min) * t));
        }
    }

    private void renderSearchBar(DrawContext ctx, int mx, int my, Theme theme) {
        int barW = 120;
        int barH = 12;
        int bx = width / 2 - barW / 2;
        int by = height - 18;

        ctx.fill(bx, by, bx + barW, by + barH, (theme.panelBackground & 0x00FFFFFF) | 0xAA000000);
        ctx.fill(bx + 1, by, bx + barW - 1, by + 1, 0x22FFFFFF);
        ctx.fill(bx + 1, by + barH - 1, bx + barW - 1, by + barH, 0x22000000);
        ctx.fill(bx, by, bx + 1, by + barH, 0x22FFFFFF);
        ctx.fill(bx + barW - 1, by, bx + barW, by + barH, 0x22FFFFFF);
        renderOutline(ctx, bx, by, barW, barH, (theme.moduleDisabled & 0x00FFFFFF) | 0x66000000);

        String display = typingSearch ? searchQuery + "|" : (searchQuery.isEmpty() ? "search..." : searchQuery);
        int textColor = searchQuery.isEmpty() && !typingSearch ? theme.moduleDisabled : theme.sliderForeground;
        ctx.drawTextWithShadow(textRenderer, display, bx + 4, by + 2, textColor);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        int bx = width / 2 - 60;

        int sby = height - 18;
        if (mx >= bx && mx <= bx + 120 && my >= sby && my <= sby + 12) {
            typingSearch = true;
            return true;
        } else {
            typingSearch = false;
        }

        int slby = height - 32;
        if (mx >= bx && mx <= bx + 120 && my >= slby && my <= slby + 10) {
            draggingScale = true;
            return true;
        }

        GUIScale guiScale = ModuleManager.INSTANCE.getModule(GUIScale.class);
        float invScale = 1.0f / (guiScale != null ? guiScale.getScale() : 1.0f);
        int smx = (int) (mx * invScale);
        int smy = (int) (my * invScale);

        for (Panel p : panels) p.mouseClicked(smx, smy, btn);
        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int btn) {
        if (btn == 0) draggingScale = false;

        GUIScale guiScale = ModuleManager.INSTANCE.getModule(GUIScale.class);
        float invScale = 1.0f / (guiScale != null ? guiScale.getScale() : 1.0f);
        int smx = (int) (mx * invScale);
        int smy = (int) (my * invScale);

        for (Panel p : panels) p.mouseReleased(smx, smy, btn);
        return super.mouseReleased(mx, my, btn);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int btn, double dx, double dy) {
        if (btn == 0) {
            GUIScale guiScale = ModuleManager.INSTANCE.getModule(GUIScale.class);
            float invScale = 1.0f / (guiScale != null ? guiScale.getScale() : 1.0f);
            int smx = (int) (mx * invScale);
            int smy = (int) (my * invScale);
            for (Panel p : panels) p.drag(smx, smy);
        }
        return super.mouseDragged(mx, my, btn, dx, dy);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (typingSearch) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                typingSearch = false;
                searchQuery = "";
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !searchQuery.isEmpty()) {
                searchQuery = searchQuery.substring(0, searchQuery.length() - 1);
                return true;
            }
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (typingSearch) {
            searchQuery += chr;
            return true;
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void close() {
        Map<Category, io.client.clickgui.CategoryPanel> cfg = new HashMap<>();
        for (Panel p : panels) {
            cfg.put(p.category, new io.client.clickgui.CategoryPanel(p.category, p.x, p.y, !p.open));
        }
        ModuleManager.INSTANCE.saveUiConfig(cfg);
        ModuleManager.INSTANCE.saveModules();
        ModuleManager.INSTANCE.saveTheme(ClickGuiScreen.currentTheme);
        if (client != null) client.setScreen(null);
    }

    private void renderGradientFill(DrawContext ctx, int x, int y, int w, int h, int colorTop, int colorBottom) {
        ctx.fill(x, y, x + w, y + h, colorTop);
    }

    private void renderGradientFillH(DrawContext ctx, int x, int y, int w, int h, int colorLeft, int colorRight) {
        ctx.fill(x, y, x + w, y + h, colorLeft);
    }

    private static int darken(int color, float factor) {
        int a = (color >> 24) & 0xFF;
        int r = (int) (((color >> 16) & 0xFF) * (1f - factor));
        int g = (int) (((color >> 8) & 0xFF) * (1f - factor));
        int b = (int) ((color & 0xFF) * (1f - factor));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private void renderOutline(DrawContext ctx, int x, int y, int w, int h, int color) {
        ctx.fill(x, y, x + w, y + 1, color);
        ctx.fill(x, y + h - 1, x + w, y + h, color);
        ctx.fill(x, y, x + 1, y + h, color);
        ctx.fill(x + w - 1, y, x + w, y + h, color);
    }

    private void drawScrollable(DrawContext ctx, String key, String text, int x, int y, int clipX1, int clipX2, int color, boolean hovered) {
        if (textRenderer == null) return;
        int maxW = Math.max(0, clipX2 - clipX1);
        int textW = textRenderer.getWidth(text);

        if (textW <= maxW) {
            scrollOffsets.put(key, 0f);
            ctx.enableScissor(clipX1, y, clipX2, y + ROW_HEIGHT);
            ctx.drawTextWithShadow(textRenderer, text, x, y, color);
            ctx.disableScissor();
            return;
        }

        float offset = scrollOffsets.getOrDefault(key, 0f);
        if (hovered) {
            offset += SCROLL_SPEED;
            float cycle = textW + SCROLL_GAP;
            if (offset > cycle) offset = 0f;
            scrollOffsets.put(key, offset);
        } else {
            scrollOffsets.put(key, 0f);
            offset = 0f;
        }

        String repeated = text + "   " + text;
        ctx.enableScissor(clipX1, y, clipX2, y + ROW_HEIGHT);
        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().translate(x - offset, y);
        ctx.drawTextWithShadow(textRenderer, repeated, 0, 0, color);
        ctx.getMatrices().popMatrix();
        ctx.disableScissor();
    }

    private final class Panel {
        final Category category;
        final List<ModuleRow> rows = new ArrayList<>();
        int x, y;
        int dragOffX, dragOffY;
        boolean dragging;
        boolean open = true;

        Panel(Category category, int x, int y) {
            this.category = category;
            this.x = x;
            this.y = y;
            ModuleManager.INSTANCE.getModulesByCategory(category).stream()
                    .sorted(Comparator.comparing(Module::getName))
                    .forEach(m -> rows.add(new ModuleRow(m)));
        }

        int bodyHeight() {
            if (!open) return 0;
            int h = 0;
            for (ModuleRow r : rows) h += r.totalHeight();
            return h;
        }

        void clamp(int sw, int sh) {
            GUIScale guiScale = ModuleManager.INSTANCE.getModule(GUIScale.class);
            float scale = guiScale != null ? guiScale.getScale() : 1.0f;
            x = Math.max(0, Math.min(x, (int)(sw / scale) - PANEL_WIDTH));
            y = Math.max(0, Math.min(y, (int)(sh / scale) - HEADER_HEIGHT));
        }

        void render(DrawContext ctx, int mx, int my, Theme theme) {
            int totalH = HEADER_HEIGHT + bodyHeight();

            ctx.fill(x, y, x + PANEL_WIDTH, y + totalH, (theme.panelBackground & 0x00FFFFFF) | 0x66000000);

            ctx.fill(x + 1, y, x + PANEL_WIDTH - 1, y + 1, 0x33FFFFFF);
            ctx.fill(x + 1, y + totalH - 1, x + PANEL_WIDTH - 1, y + totalH, 0x33FFFFFF);
            ctx.fill(x, y, x + 1, y + totalH, 0x33FFFFFF);
            ctx.fill(x + PANEL_WIDTH - 1, y, x + PANEL_WIDTH, y + totalH, 0x33FFFFFF);

            ctx.fill(x, y, x + PANEL_WIDTH, y + HEADER_HEIGHT, theme.moduleEnabled);
            ctx.fill(x + 1, y + 1, x + PANEL_WIDTH - 1, y + 2, 0x33FFFFFF);
            ctx.fill(x + 1, y + HEADER_HEIGHT - 1, x + PANEL_WIDTH - 1, y + HEADER_HEIGHT, 0x22000000);

            boolean hov = overHeader(mx, my);
            if (hov) ctx.fill(x, y, x + PANEL_WIDTH, y + HEADER_HEIGHT, theme.hoverHighlight);

            int centerX = x + (PANEL_WIDTH - textRenderer.getWidth(category.name())) / 2;
            int centerY = y + (HEADER_HEIGHT - 8) / 2;
            ctx.drawTextWithShadow(textRenderer, category.name(), centerX, centerY, 0xFFFFFFFF);

            String openStr = open ? "\u2212" : "\u002B";
            ctx.drawTextWithShadow(textRenderer, openStr, x + PANEL_WIDTH - textRenderer.getWidth(openStr) - 3, centerY, 0xFFFFFFFF);

            if (!open) return;

            int ry = y + HEADER_HEIGHT;
            for (ModuleRow r : rows) {
                r.render(ctx, mx, my, x, ry, PANEL_WIDTH, theme);
                ry += r.totalHeight();
            }
        }

        void mouseClicked(int mx, int my, int btn) {
            if (overHeader(mx, my)) {
                if (btn == 0) { dragging = true; dragOffX = x - mx; dragOffY = y - my; }
                else if (btn == 1) { open = !open; click(); }
                return;
            }
            if (!open) return;
            int ry = y + HEADER_HEIGHT;
            for (ModuleRow r : rows) {
                r.mouseClicked(mx, my, btn, x, ry, PANEL_WIDTH);
                ry += r.totalHeight();
            }
        }

        void mouseReleased(int mx, int my, int btn) {
            if (btn == 0) dragging = false;
            if (!open) return;
            int ry = y + HEADER_HEIGHT;
            for (ModuleRow r : rows) {
                r.mouseReleased(mx, my, btn, x, ry, PANEL_WIDTH);
                ry += r.totalHeight();
            }
        }

        void drag(int mx, int my) {
            if (!dragging) return;
            MinecraftClient mc = MinecraftClient.getInstance();
            GUIScale guiScale = ModuleManager.INSTANCE.getModule(GUIScale.class);
            float scale = guiScale != null ? guiScale.getScale() : 1.0f;
            int maxX = (int) (mc.getWindow().getScaledWidth() / scale) - PANEL_WIDTH;
            int maxY = (int) (mc.getWindow().getScaledHeight() / scale) - HEADER_HEIGHT;
            x = Math.max(0, Math.min(dragOffX + mx, maxX));
            y = Math.max(0, Math.min(dragOffY + my, maxY));
        }

        boolean overHeader(int mx, int my) {
            return mx >= x && mx <= x + PANEL_WIDTH && my >= y && my <= y + HEADER_HEIGHT;
        }
    }

    private final class ModuleRow {
        final Module module;
        final List<SettingRow> settingRows = new ArrayList<>();
        boolean open;

        ModuleRow(Module module) {
            this.module = module;
            buildRows(settingRows, module.getSettings(), 0);
        }

        int totalHeight() {
            if (!open || settingRows.isEmpty()) return ROW_HEIGHT;
            int h = ROW_HEIGHT;
            for (SettingRow r : settingRows) h += r.height();
            return h;
        }

        void render(DrawContext ctx, int mx, int my, int px, int py, int pw, Theme theme) {
            boolean hov = mx >= px && mx <= px + pw && my >= py && my <= py + ROW_HEIGHT;

            String name = module.getName();
            boolean searchMatch = !searchQuery.isEmpty() && name.toLowerCase().contains(searchQuery.toLowerCase());
            boolean hasSettings = !settingRows.isEmpty();

            int bg;
            if (searchMatch) {
                bg = (theme.moduleEnabled & 0x00FFFFFF) | 0xAA000000;
            } else if (module.isEnabled()) {
                bg = theme.moduleEnabled;
            } else {
                bg = (theme.panelBackground & 0x00FFFFFF) | 0xAA000000;
            }

            ctx.fill(px, py, px + pw, py + ROW_HEIGHT, bg);
            if (hov) ctx.fill(px, py, px + pw, py + ROW_HEIGHT, theme.hoverHighlight);

            if (module.isEnabled()) {
                ctx.fill(px + 1, py, px + pw - 1, py + 1, theme.sliderForeground);
                ctx.fill(px + 1, py + ROW_HEIGHT - 1, px + pw - 1, py + ROW_HEIGHT, theme.sliderForeground);
                ctx.fill(px, py, px + 1, py + ROW_HEIGHT, theme.sliderForeground);
                ctx.fill(px + pw - 1, py, px + pw, py + ROW_HEIGHT, theme.sliderForeground);
            }

            ctx.fill(px, py + ROW_HEIGHT - 1, px + pw, py + ROW_HEIGHT, (theme.panelBackground & 0x00FFFFFF) | 0x18000000);

            int textColor = searchMatch ? theme.sliderForeground : (module.isEnabled() ? 0xFFFFFFFF : theme.moduleDisabled);

            int textRight = px + pw - 3;
            if (hasSettings) {
                String indicator = open ? "\u2212" : "\u002B";
                int indColor = (theme.moduleDisabled & 0x00FFFFFF) | 0xBB000000;
                ctx.drawTextWithShadow(textRenderer, indicator, px + pw - textRenderer.getWidth(indicator) - 3, py + 2, indColor);
                textRight = px + pw - textRenderer.getWidth(indicator) - 6;
            }

            drawScrollable(ctx, "mod:" + module.getName(), name, px + 3, py + 2, px + 3, textRight, textColor, hov);

            if (!open) return;
            int sy = py + ROW_HEIGHT;
            for (SettingRow r : settingRows) {
                r.render(ctx, mx, my, px + 1, sy, pw - 2, theme);
                sy += r.height();
            }
        }

        void mouseClicked(int mx, int my, int btn, int px, int py, int pw) {
            if (mx >= px && mx <= px + pw && my >= py && my <= py + ROW_HEIGHT) {
                if (btn == 0) { module.toggle(); click(); }
                else if (btn == 1 && !settingRows.isEmpty()) { open = !open; click(); }
                return;
            }
            if (!open) return;
            int sy = py + ROW_HEIGHT;
            for (SettingRow r : settingRows) {
                r.mouseClicked(mx, my, btn, px + 1, sy, pw - 2);
                sy += r.height();
            }
        }

        void mouseReleased(int mx, int my, int btn, int px, int py, int pw) {
            if (!open) return;
            int sy = py + ROW_HEIGHT;
            for (SettingRow r : settingRows) {
                r.mouseReleased(mx, my, btn, px + 1, sy, pw - 2);
                sy += r.height();
            }
        }
    }

    private abstract class SettingRow {
        final int indent;
        SettingRow(int indent) { this.indent = indent; }
        abstract void render(DrawContext ctx, int mx, int my, int x, int y, int w, Theme theme);
        void mouseClicked(int mx, int my, int btn, int x, int y, int w) {}
        void mouseReleased(int mx, int my, int btn, int x, int y, int w) {}
        int height() { return ROW_HEIGHT; }
        boolean over(int mx, int my, int x, int y, int w) {
            return mx >= x && mx <= x + w && my >= y && my <= y + height();
        }
        void scroll(DrawContext ctx, String key, String text, int x, int y, int x1, int x2, int color, boolean hov) {
            drawScrollable(ctx, key, text, x, y, x1, x2, color, hov);
        }
    }

    private final class BoolRow extends SettingRow {
        final BooleanSetting setting;
        BoolRow(BooleanSetting s, int indent) { super(indent); setting = s; }

        @Override
        void render(DrawContext ctx, int mx, int my, int x, int y, int w, Theme theme) {
            boolean hov = over(mx, my, x, y, w);
            int bg = setting.isEnabled() ? theme.moduleEnabled : theme.panelBackground;
            ctx.fill(x, y, x + w, y + ROW_HEIGHT, bg);
            if (hov) ctx.fill(x, y, x + w, y + ROW_HEIGHT, theme.hoverHighlight);
            int textColor = setting.isEnabled() ? 0xFFFFFFFF : theme.moduleDisabled;
            scroll(ctx, "bool:" + System.identityHashCode(this), setting.getName(), x + 3 + indent, y + 2, x + 3 + indent, x + w - 3, textColor, hov);
        }

        @Override
        void mouseClicked(int mx, int my, int btn, int x, int y, int w) {
            if (btn == 0 && over(mx, my, x, y, w)) { setting.toggle(); click(); }
        }
    }

    private final class RadioRow extends SettingRow {
        final RadioSetting setting;
        RadioRow(RadioSetting s, int indent) { super(indent); setting = s; }

        @Override
        void render(DrawContext ctx, int mx, int my, int x, int y, int w, Theme theme) {
            boolean hov = over(mx, my, x, y, w);
            ctx.fill(x, y, x + w, y + ROW_HEIGHT, theme.panelBackground);
            if (hov) ctx.fill(x, y, x + w, y + ROW_HEIGHT, theme.hoverHighlight);
            String label = setting.getName() + ": " + setting.getSelectedOption();
            scroll(ctx, "radio:" + System.identityHashCode(this), label, x + 3 + indent, y + 2, x + 3 + indent, x + w - 3, theme.moduleDisabled, hov);
        }

        @Override
        void mouseClicked(int mx, int my, int btn, int x, int y, int w) {
            if (!over(mx, my, x, y, w)) return;
            if (btn == 0) {
                setting.cycleNext();
            } else if (btn == 1) {
                List<String> opts = setting.getOptions();
                if (!opts.isEmpty()) {
                    int cur = opts.indexOf(setting.getSelectedOption());
                    setting.setSelectedOption(opts.get((cur - 1 + opts.size()) % opts.size()));
                }
            }
            click();
        }
    }

    private final class NumberRow extends SettingRow {
        final NumberSetting setting;
        boolean dragging;
        NumberRow(NumberSetting s, int indent) { super(indent); setting = s; }

        @Override
        void render(DrawContext ctx, int mx, int my, int x, int y, int w, Theme theme) {
            boolean hov = over(mx, my, x, y, w);
            ctx.fill(x, y, x + w, y + ROW_HEIGHT, theme.panelBackground);
            if (hov) ctx.fill(x, y, x + w, y + ROW_HEIGHT, theme.hoverHighlight);

            int trackX = x + 2 + indent;
            int trackW = w - 4 - indent;
            float norm = (setting.getValue() - setting.getMin()) / (setting.getMax() - setting.getMin());
            int fillW = (int)(norm * trackW);

            ctx.fill(trackX, y + ROW_HEIGHT - 2, trackX + trackW, y + ROW_HEIGHT - 1, theme.sliderBackground);
            ctx.fill(trackX, y + ROW_HEIGHT - 2, trackX + fillW, y + ROW_HEIGHT - 1, theme.sliderForeground);

            String label = setting.getName() + ": " + fmt(setting.getValue());
            scroll(ctx, "num:" + System.identityHashCode(this), label, x + 3 + indent, y + 2, x + 3 + indent, x + w - 3, theme.moduleDisabled, hov);

            if (dragging) applyMouse(mx, x, w);
        }

        @Override
        void mouseClicked(int mx, int my, int btn, int x, int y, int w) {
            if (btn == 0 && over(mx, my, x, y, w)) { dragging = true; applyMouse(mx, x, w); }
        }

        @Override
        void mouseReleased(int mx, int my, int btn, int x, int y, int w) {
            if (btn == 0) dragging = false;
        }

        void applyMouse(int mx, int x, int w) {
            int trackX = x + 2 + indent;
            int trackW = w - 4 - indent;
            float t = Math.max(0f, Math.min(1f, (mx - trackX) / (float) trackW));
            setting.setValue(setting.getMin() + (setting.getMax() - setting.getMin()) * t);
        }
    }

    private final class StringRow extends SettingRow {
        final StringSetting setting;
        StringRow(StringSetting s, int indent) { super(indent); setting = s; }

        @Override
        void render(DrawContext ctx, int mx, int my, int x, int y, int w, Theme theme) {
            boolean hov = over(mx, my, x, y, w);
            ctx.fill(x, y, x + w, y + ROW_HEIGHT, theme.panelBackground);
            if (hov) ctx.fill(x, y, x + w, y + ROW_HEIGHT, theme.hoverHighlight);
            String label = setting.getName() + ": \"" + setting.getValue() + "\"";
            scroll(ctx, "str:" + System.identityHashCode(this), label, x + 3 + indent, y + 2, x + 3 + indent, x + w - 3, theme.moduleDisabled, hov);
        }
    }

    private final class CategoryRow extends SettingRow {
        final CategorySetting setting;
        final List<SettingRow> children = new ArrayList<>();
        boolean open;

        CategoryRow(CategorySetting s, int indent) {
            super(indent);
            setting = s;
            open = s.isExpanded();
        }

        @Override
        int height() {
            if (!open) return ROW_HEIGHT;
            int h = ROW_HEIGHT;
            for (SettingRow r : children) h += r.height();
            return h;
        }

        @Override
        void render(DrawContext ctx, int mx, int my, int x, int y, int w, Theme theme) {
            boolean hov = mx >= x && mx <= x + w && my >= y && my <= y + ROW_HEIGHT;
            ctx.fill(x, y, x + w, y + ROW_HEIGHT, theme.panelBackground);
            if (hov) ctx.fill(x, y, x + w, y + ROW_HEIGHT, theme.hoverHighlight);

            ctx.fill(x, y, x + 1, y + ROW_HEIGHT, theme.moduleEnabled);

            String label = (open ? "- " : "+ ") + setting.getName();
            scroll(ctx, "cat:" + System.identityHashCode(this), label, x + 5 + indent, y + 2, x + 5 + indent, x + w - 3, theme.sliderForeground, hov);

            if (!open) return;
            int sy = y + ROW_HEIGHT;
            for (SettingRow r : children) {
                r.render(ctx, mx, my, x, sy, w, theme);
                sy += r.height();
            }
        }

        @Override
        void mouseClicked(int mx, int my, int btn, int x, int y, int w) {
            if (mx >= x && mx <= x + w && my >= y && my <= y + ROW_HEIGHT && btn == 1) {
                open = !open; setting.setExpanded(open); click(); return;
            }
            if (!open) return;
            int sy = y + ROW_HEIGHT;
            for (SettingRow r : children) {
                r.mouseClicked(mx, my, btn, x, sy, w);
                sy += r.height();
            }
        }

        @Override
        void mouseReleased(int mx, int my, int btn, int x, int y, int w) {
            if (!open) return;
            int sy = y + ROW_HEIGHT;
            for (SettingRow r : children) {
                r.mouseReleased(mx, my, btn, x, sy, w);
                sy += r.height();
            }
        }
    }

    private void buildRows(List<SettingRow> target, List<?> settings, int indent) {
        for (Object obj : settings) {
            if (!(obj instanceof Setting s)) continue;
            if (s instanceof CategorySetting cs) {
                CategoryRow cr = new CategoryRow(cs, indent);
                buildRows(cr.children, cs.getSettings(), indent + 6);
                target.add(cr);
            } else if (s instanceof BooleanSetting bs) {
                target.add(new BoolRow(bs, indent));
            } else if (s instanceof RadioSetting rs) {
                target.add(new RadioRow(rs, indent));
            } else if (s instanceof NumberSetting ns) {
                target.add(new NumberRow(ns, indent));
            } else if (s instanceof StringSetting ss) {
                target.add(new StringRow(ss, indent));
            }
        }
    }

    private static String fmt(float v) {
        return Math.abs(v - Math.round(v)) < 0.001f ? Integer.toString(Math.round(v)) : String.format("%.2f", v);
    }

    private static void click() {
        try {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc != null && mc.getSoundManager() != null)
                mc.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK.value(), 1.0f));
        } catch (Exception ignored) {}
    }
}



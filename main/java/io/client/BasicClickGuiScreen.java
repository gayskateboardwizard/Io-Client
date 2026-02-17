package io.client;

import io.client.clickgui.SavedPanelConfig;
import io.client.clickgui.Theme;
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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BasicClickGuiScreen extends Screen {

    private static final int PANEL_WIDTH   = 90;
    private static final int HEADER_HEIGHT = 12;
    private static final int ROW_HEIGHT    = 12;
    private static final float SCROLL_SPEED = 0.4f;
    private static final int SCROLL_GAP    = 20;

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
        context.fill(0, 0, width, height, (theme.panelBackground & 0x00FFFFFF) | 0xAA000000);

        MinecraftClient mc = MinecraftClient.getInstance();
        int sw = mc.getWindow().getScaledWidth();
        int sh = mc.getWindow().getScaledHeight();
        for (Panel p : panels) {
            p.clamp(sw, sh);
            p.render(context, mouseX, mouseY, theme);
        }
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        for (Panel p : panels) p.mouseClicked((int) mx, (int) my, btn);
        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int btn) {
        for (Panel p : panels) p.mouseReleased((int) mx, (int) my, btn);
        return super.mouseReleased(mx, my, btn);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int btn, double dx, double dy) {
        if (btn == 0) for (Panel p : panels) p.drag((int) mx, (int) my);
        return super.mouseDragged(mx, my, btn, dx, dy);
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
            x = Math.max(0, Math.min(x, sw - PANEL_WIDTH));
            y = Math.max(0, Math.min(y, sh - HEADER_HEIGHT));
        }

        void render(DrawContext ctx, int mx, int my, Theme theme) {
            ctx.fill(x, y, x + PANEL_WIDTH, y + HEADER_HEIGHT + bodyHeight(), theme.panelBackground);
            ctx.fill(x, y, x + PANEL_WIDTH, y + HEADER_HEIGHT, theme.titleBar);
            int headerText = open ? theme.moduleEnabled : theme.moduleDisabled;
            drawScrollable(ctx, "panel:" + category.name(), category.name(), x + 3, y + 2, x + 3, x + PANEL_WIDTH - 3, headerText, overHeader(mx, my));

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
            int sw = mc.getWindow().getScaledWidth();
            int sh = mc.getWindow().getScaledHeight();
            x = Math.max(0, Math.min(dragOffX + mx, sw - PANEL_WIDTH));
            y = Math.max(0, Math.min(dragOffY + my, sh - HEADER_HEIGHT));
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
            int bg = module.isEnabled() ? theme.moduleEnabled : theme.panelBackground;
            ctx.fill(px, py, px + pw, py + ROW_HEIGHT, bg);
            if (hov) ctx.fill(px, py, px + pw, py + ROW_HEIGHT, theme.hoverHighlight);
            int textColor = module.isEnabled() ? 0xFFFFFFFF : theme.moduleDisabled;
            drawScrollable(ctx, "mod:" + module.getName(), module.getName(), px + 3, py + 2, px + 3, px + pw - 3, textColor, hov);

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
            ctx.fill(trackX, y + ROW_HEIGHT - 2, trackX + trackW, y + ROW_HEIGHT - 1, theme.sliderBackground);
            ctx.fill(trackX, y + ROW_HEIGHT - 2, trackX + (int)(norm * trackW), y + ROW_HEIGHT - 1, theme.sliderForeground);

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
            String label = (open ? "- " : "+ ") + setting.getName();
            scroll(ctx, "cat:" + System.identityHashCode(this), label, x + 3 + indent, y + 2, x + 3 + indent, x + w - 3, theme.moduleEnabled, hov);

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
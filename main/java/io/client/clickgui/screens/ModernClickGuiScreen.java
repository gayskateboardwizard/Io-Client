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

import java.util.*;

public class ModernClickGuiScreen extends Screen {

    private static final int SIDEBAR_W  = 74;
    private static final int TOP_H      = 28;
    private static final int CARD_H     = 22;
    private static final int CARD_GAP   = 3;
    private static final int CARD_COLS  = 2;
    private static final int CARD_PAD   = 6;
    private static final int SETTINGS_W = 160;
    private static final int ROW_H      = 14;

    private Category selectedCategory;
    private Module   settingsTarget;
    private boolean  typingSearch;
    private String   searchQuery = "";
    private float    moduleScroll;
    private float    settingsScroll;
    private boolean  draggingScale;

    private final List<SettingRow> settingRows = new ArrayList<>();
    private NumberRow draggingNumber;

    public ModernClickGuiScreen() {
        super(Text.literal("GUI"));
        Category[] cats = Category.values();
        selectedCategory = cats.length > 0 ? cats[0] : null;
    }

    private float getScale() {
        GUIScale g = ModuleManager.INSTANCE.getModule(GUIScale.class);
        return g != null ? g.getScale() : 1.0f;
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        Theme theme = ClickGuiScreen.currentTheme;
        float scale = getScale();

        ctx.fill(0, 0, width, height, 0xF2000000 | (theme.panelBackground & 0x00FFFFFF));

        renderTopBar(ctx, mouseX, mouseY, width, height, theme, scale);

        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().scale(scale, scale);

        int sw       = (int) (width  / scale);
        int sh       = (int) (height / scale);
        int smx      = (int) (mouseX / scale);
        int smy      = (int) (mouseY / scale);
        int topOff   = (int) Math.ceil(TOP_H / scale);

        renderSidebar(ctx, smx, smy, sw, sh, topOff, theme);
        renderModuleGrid(ctx, smx, smy, sw, sh, topOff, theme);
        if (settingsTarget != null) renderSettingsPanel(ctx, smx, smy, sw, sh, topOff, theme);

        ctx.getMatrices().popMatrix();

        super.render(ctx, mouseX, mouseY, delta);
    }

    private void renderTopBar(DrawContext ctx, int mx, int my, int sw, int sh,
                              Theme theme, float scale) {
        ctx.fill(0, 0, sw, TOP_H, 0xFF000000 | (theme.titleBar & 0x00FFFFFF));
        ctx.fill(0, TOP_H - 1, sw, TOP_H, 0x44000000 | (theme.moduleEnabled & 0x00FFFFFF));

        ctx.drawTextWithShadow(textRenderer, "IO CLIENT",
                SIDEBAR_W + 10, TOP_H / 2 - 4, theme.moduleEnabled);

        int sbW = 140, sbH = 14;
        int sbX = sw - 10 - 110 - 10 - sbW, sbY = TOP_H / 2 - sbH / 2;
        ctx.fill(sbX, sbY, sbX + sbW, sbY + sbH, theme.panelBackground);
        drawOutline(ctx, sbX, sbY, sbW, sbH, theme.sliderBackground);
        if (typingSearch) drawOutline(ctx, sbX, sbY, sbW, sbH, theme.moduleEnabled);

        String searchDisplay = typingSearch
                ? (searchQuery.isEmpty() ? "|" : searchQuery + "|")
                : (searchQuery.isEmpty() ? "search..." : searchQuery);
        int sCl = searchQuery.isEmpty() && !typingSearch ? theme.moduleDisabled : theme.sliderForeground;
        ctx.enableScissor(sbX + 3, sbY, sbX + sbW - 3, sbY + sbH);
        ctx.drawTextWithShadow(textRenderer, searchDisplay, sbX + 5, sbY + 3, sCl);
        ctx.disableScissor();

        int barW = 110, barH = 14;
        int bx = sw - barW - 10, by = TOP_H / 2 - barH / 2;
        ctx.fill(bx, by, bx + barW, by + barH, theme.panelBackground);
        drawOutline(ctx, bx, by, barW, barH, theme.sliderBackground);

        float min = 0.5f, max = 2.0f;
        float norm = (scale - min) / (max - min);
        int fillW = Math.max(0, (int) (norm * (barW - 4)));
        ctx.fill(bx + 2, by + 4, bx + 2 + fillW, by + barH - 4, theme.sliderBackground);
        ctx.fill(bx + 2 + fillW - 1, by + 2, bx + 2 + fillW + 2, by + barH - 2, theme.sliderForeground);

        ctx.drawTextWithShadow(textRenderer, "scale " + fmt(scale), bx + 5, by + 3, theme.moduleDisabled);

        if (draggingScale) {
            GUIScale guiScale = ModuleManager.INSTANCE.getModule(GUIScale.class);
            if (guiScale != null) {
                float t = Math.max(0f, Math.min(1f, (mx - bx - 2f) / (barW - 4f)));
                guiScale.getSettings().stream()
                        .filter(s -> s instanceof NumberSetting && s.getName().equals("Scale"))
                        .findFirst()
                        .ifPresent(s -> ((NumberSetting) s).setValue(min + (max - min) * t));
            }
        }
    }

    private void renderSidebar(DrawContext ctx, int mx, int my, int sw, int sh,
                               int topOff, Theme theme) {
        ctx.fill(0, topOff, SIDEBAR_W, sh, 0xFF000000 | (theme.titleBar & 0x00FFFFFF));

        int itemH = 26;
        int sy = topOff + 6;

        ctx.enableScissor(0, topOff, SIDEBAR_W, sh);
        for (Category cat : Category.values()) {
            boolean sel = cat == selectedCategory;
            boolean hov = mx >= 0 && mx < SIDEBAR_W && my >= sy && my < sy + itemH;

            if (sel || hov) ctx.fill(0, sy, SIDEBAR_W - 1, sy + itemH, theme.hoverHighlight);
            if (sel) ctx.fill(0, sy + 2, 3, sy + itemH - 2, theme.moduleEnabled);

            int count = (int) ModuleManager.INSTANCE.getModulesByCategory(cat)
                    .stream().filter(Module::isEnabled).count();
            int total = ModuleManager.INSTANCE.getModulesByCategory(cat).size();

            int textColor = sel ? theme.sliderForeground : theme.moduleDisabled;
            ctx.drawTextWithShadow(textRenderer, cat.name(), 9, sy + 6, textColor);
            int badgeColor = count > 0 ? theme.moduleEnabled : theme.moduleDisabled;
            ctx.drawTextWithShadow(textRenderer, count + "/" + total, 9, sy + 15, badgeColor);

            sy += itemH + 2;
        }
        ctx.disableScissor();

        ctx.fill(SIDEBAR_W - 1, topOff, SIDEBAR_W, sh, 0x22000000 | (theme.moduleEnabled & 0x00FFFFFF));
    }

    private List<Module> getDisplayModules() {
        if (!searchQuery.isEmpty()) {
            List<Module> all = new ArrayList<>();
            for (Category cat : Category.values()) {
                for (Module m : ModuleManager.INSTANCE.getModulesByCategory(cat)) {
                    if (m.getName().toLowerCase().contains(searchQuery.toLowerCase())) {
                        all.add(m);
                    }
                }
            }
            all.sort(Comparator.comparing(Module::getName));
            return all;
        }
        if (selectedCategory == null) return Collections.emptyList();
        List<Module> list = new ArrayList<>(ModuleManager.INSTANCE.getModulesByCategory(selectedCategory));
        list.sort(Comparator.comparing(Module::getName));
        return list;
    }

    private void renderModuleGrid(DrawContext ctx, int mx, int my, int sw, int sh,
                                  int topOff, Theme theme) {
        int settingsW = settingsTarget != null ? SETTINGS_W : 0;
        int mainX = SIDEBAR_W + CARD_PAD;
        int mainW = sw - SIDEBAR_W - settingsW - CARD_PAD * 2;
        int mainY = topOff + 6;
        int gridY  = mainY + 16;
        int gridH  = sh - gridY - 4;

        List<Module> modules = getDisplayModules();

        String header = !searchQuery.isEmpty() ? "results" : (selectedCategory != null ? selectedCategory.name() : "");
        ctx.drawTextWithShadow(textRenderer, header, mainX, mainY, theme.moduleEnabled);
        ctx.fill(mainX, mainY + 10, mainX + textRenderer.getWidth(header) + 4, mainY + 11,
                0x66000000 | (theme.moduleEnabled & 0x00FFFFFF));

        int colW = (mainW - CARD_GAP) / 2;
        int rows = (modules.size() + 1) / 2;
        int contentH = rows * (CARD_H + CARD_GAP);
        moduleScroll = Math.max(0, Math.min(moduleScroll, Math.max(0, contentH - gridH)));

        ctx.enableScissor(mainX, gridY, mainX + mainW, gridY + gridH);

        for (int i = 0; i < modules.size(); i++) {
            Module m = modules.get(i);
            int col = i % CARD_COLS;
            int row = i / CARD_COLS;
            int cx = mainX + col * (colW + CARD_GAP);
            int cy = gridY + row * (CARD_H + CARD_GAP) - (int) moduleScroll;
            if (cy + CARD_H < gridY || cy > gridY + gridH) continue;
            renderModuleCard(ctx, mx, my, m, cx, cy, colW, theme);
        }

        ctx.disableScissor();
    }

    private void renderModuleCard(DrawContext ctx, int mx, int my, Module m,
                                  int cx, int cy, int cw, Theme theme) {
        boolean hov     = mx >= cx && mx < cx + cw && my >= cy && my < cy + CARD_H;
        boolean enabled = m.isEnabled();
        boolean isSel   = m == settingsTarget;

        int cardBg = isSel
                ? theme.hoverHighlight
                : enabled
                ? (0xFF000000 | (theme.panelBackground & 0x00FFFFFF))
                : (0xFF000000 | (theme.titleBar & 0x00FFFFFF));

        int borderCol = isSel
                ? theme.moduleEnabled
                : enabled
                ? (0x55000000 | (theme.moduleEnabled & 0x00FFFFFF))
                : hov ? theme.sliderBackground : (0x44000000 | (theme.sliderBackground & 0x00FFFFFF));

        ctx.fill(cx, cy, cx + cw, cy + CARD_H, cardBg);
        drawOutline(ctx, cx, cy, cw, CARD_H, borderCol);

        if (hov && !isSel) ctx.fill(cx + 1, cy + 1, cx + cw - 1, cy + CARD_H - 1, theme.hoverHighlight);

        if (enabled) {
            ctx.fill(cx + 1, cy + 2, cx + 3, cy + CARD_H - 2, theme.moduleEnabled);
            ctx.fill(cx + 2, cy + 1, cx + cw - 2, cy + 2, 0x66000000 | (theme.moduleEnabled & 0x00FFFFFF));
        }

        int nameX = cx + (enabled ? 6 : 4);
        int nameY = cy + CARD_H / 2 - 4;
        int nameColor = enabled ? theme.sliderForeground : theme.moduleDisabled;
        ctx.enableScissor(nameX, cy + 1, cx + cw - 6, cy + CARD_H - 1);
        ctx.drawTextWithShadow(textRenderer, m.getName(), nameX, nameY, nameColor);
        ctx.disableScissor();

        if (!m.getSettings().isEmpty()) {
            int dotColor = isSel ? theme.moduleEnabled : theme.sliderBackground;
            ctx.fill(cx + cw - 6, cy + 4, cx + cw - 4, cy + 6, dotColor);
        }
    }

    private void renderSettingsPanel(DrawContext ctx, int mx, int my, int sw, int sh,
                                     int topOff, Theme theme) {
        int px = sw - SETTINGS_W;

        ctx.fill(px + 1, topOff + 1, sw, sh, 0xFF000000 | (theme.titleBar & 0x00FFFFFF));
        ctx.fill(px, topOff, px + 1, sh, 0x33000000 | (theme.moduleEnabled & 0x00FFFFFF));
        ctx.fill(px, topOff, sw, topOff + 1, 0x33000000 | (theme.moduleEnabled & 0x00FFFFFF));

        ctx.fill(px + 1, topOff + 1, sw, topOff + 22, 0xFF000000 | (theme.panelBackground & 0x00FFFFFF));
        ctx.fill(px, topOff + 21, sw, topOff + 22, 0x44000000 | (theme.moduleEnabled & 0x00FFFFFF));

        int nameColor = settingsTarget.isEnabled() ? theme.sliderForeground : theme.moduleDisabled;
        ctx.drawTextWithShadow(textRenderer, settingsTarget.getName(), px + 8, topOff + 7, nameColor);
        ctx.drawTextWithShadow(textRenderer, "\u00D7", sw - 12, topOff + 6, theme.moduleDisabled);

        int tooltipY = topOff + 24;
        String desc = settingsTarget.getDescription();
        if (desc != null && !desc.isEmpty()) {
            List<String> wrapped = wrapText(desc, SETTINGS_W - 12);
            int tooltipH = wrapped.size() * 9 + 6;

            ctx.fill(px + 5, tooltipY + 1, sw - 5, tooltipY + tooltipH - 1, 0xFF000000 | (theme.titleBar & 0x00FFFFFF));
            drawOutline(ctx, px + 4, tooltipY, SETTINGS_W - 8, tooltipH, 0x44000000 | (theme.moduleEnabled & 0x00FFFFFF));

            int ty = tooltipY + 3;
            for (String line : wrapped) {
                ctx.drawTextWithShadow(textRenderer, line, px + 7, ty, theme.moduleDisabled);
                ty += 9;
            }
            tooltipY += tooltipH + 4;
        }

        int rowY0 = tooltipY;
        int rowsH = sh - rowY0;
        int contentH = settingRows.stream().mapToInt(SettingRow::height).sum();
        settingsScroll = Math.max(0, Math.min(settingsScroll, Math.max(0, contentH - rowsH)));

        ctx.enableScissor(px + 4, rowY0, sw - 4, sh - 4);
        int ry = rowY0 - (int) settingsScroll;
        for (SettingRow r : settingRows) {
            r.render(ctx, mx, my, px + 4, ry, SETTINGS_W - 8, theme);
            ry += r.height();
        }
        ctx.disableScissor();
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        float scale  = getScale();
        float inv    = 1f / scale;
        int rawMX    = (int) mx,       rawMY    = (int) my;
        int smx      = (int) (mx * inv), smy    = (int) (my * inv);
        int sw       = width,           rawSW    = width;
        int scaledSW = (int) (width  * inv);
        int topOff   = (int) Math.ceil(TOP_H / scale);

        int barW = 110;
        int bx = rawSW - barW - 10, by = TOP_H / 2 - 7;
        if (rawMX >= bx && rawMX < bx + barW && rawMY >= by && rawMY < by + 14) {
            draggingScale = true;
            return true;
        }

        int sbW = 140;
        int sbX = rawSW - 10 - 110 - 10 - sbW, sbY = TOP_H / 2 - 7;
        if (rawMX >= sbX && rawMX < sbX + sbW && rawMY >= sbY && rawMY < sbY + 14) {
            typingSearch = true;
            return true;
        } else {
            typingSearch = false;
        }

        int itemH = 26;
        int sy = topOff + 6;
        for (Category cat : Category.values()) {
            if (smx >= 0 && smx < SIDEBAR_W && smy >= sy && smy < sy + itemH) {
                if (selectedCategory != cat) { selectedCategory = cat; moduleScroll = 0; }
                if (settingsTarget != null) closeSettings();
                click();
                return true;
            }
            sy += itemH + 2;
        }

        if (settingsTarget != null) {
            int px = scaledSW - SETTINGS_W;
            if (smx >= scaledSW - 16 && smx < scaledSW - 4 && smy >= topOff + 4 && smy < topOff + 18) {
                closeSettings(); click(); return true;
            }

            int tooltipY = topOff + 24;
            String desc = settingsTarget.getDescription();
            if (desc != null && !desc.isEmpty()) {
                List<String> wrapped = wrapText(desc, SETTINGS_W - 12);
                int tooltipH = wrapped.size() * 9 + 6;
                tooltipY += tooltipH + 4;
            }

            int ry = tooltipY - (int) settingsScroll;
            for (SettingRow r : settingRows) {
                r.mouseClicked(smx, smy, btn, px + 4, ry, SETTINGS_W - 8);
                ry += r.height();
            }
        }

        if (smx > SIDEBAR_W) {
            int settingsW = settingsTarget != null ? SETTINGS_W : 0;
            int mainX = SIDEBAR_W + CARD_PAD;
            int mainW = scaledSW - SIDEBAR_W - settingsW - CARD_PAD * 2;
            int colW  = (mainW - CARD_GAP) / 2;
            int gridY = topOff + 22;

            List<Module> modules = getDisplayModules();

            for (int i = 0; i < modules.size(); i++) {
                int col = i % 2;
                int row = i / 2;
                int cx = mainX + col * (colW + CARD_GAP);
                int cy = gridY + row * (CARD_H + CARD_GAP) - (int) moduleScroll;
                if (smx >= cx && smx < cx + colW && smy >= cy && smy < cy + CARD_H) {
                    Module m = modules.get(i);
                    if (btn == 0) { m.toggle(); click(); }
                    else if (btn == 1) {
                        if (settingsTarget == m) closeSettings();
                        else openSettings(m);
                        click();
                    }
                    return true;
                }
            }
        }

        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int btn) {
        if (btn == 0) {
            draggingScale = false;
            if (draggingNumber != null) { draggingNumber.dragging = false; draggingNumber = null; }
        }
        float inv = 1f / getScale();
        int smx = (int) (mx * inv), smy = (int) (my * inv), sw = (int) (width * inv);
        int topOff = (int) Math.ceil(TOP_H / getScale());
        if (settingsTarget != null) {
            int px = sw - SETTINGS_W;

            int tooltipY = topOff + 24;
            String desc = settingsTarget.getDescription();
            if (desc != null && !desc.isEmpty()) {
                List<String> wrapped = wrapText(desc, SETTINGS_W - 12);
                int tooltipH = wrapped.size() * 9 + 6;
                tooltipY += tooltipH + 4;
            }

            int ry = tooltipY - (int) settingsScroll;
            for (SettingRow r : settingRows) {
                r.mouseReleased(smx, smy, btn, px + 4, ry, SETTINGS_W - 8);
                ry += r.height();
            }
        }
        return super.mouseReleased(mx, my, btn);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int btn, double dx, double dy) {
        float inv = 1f / getScale();
        int smx = (int) (mx * inv), sw = (int) (width * inv);
        if (draggingNumber != null && settingsTarget != null) {
            draggingNumber.applyMouse(smx, sw - SETTINGS_W + 4, SETTINGS_W - 8);
        }
        return super.mouseDragged(mx, my, btn, dx, dy);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double hDelta, double vDelta) {
        float inv = 1f / getScale();
        int smx = (int) (mx * inv), sw = (int) (width * inv);
        if (settingsTarget != null && smx >= sw - SETTINGS_W) {
            settingsScroll = Math.max(0, settingsScroll - (float) (vDelta * 20));
        } else {
            moduleScroll = Math.max(0, moduleScroll - (float) (vDelta * 20));
        }
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (typingSearch) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) { typingSearch = false; searchQuery = ""; return true; }
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !searchQuery.isEmpty()) {
                searchQuery = searchQuery.substring(0, searchQuery.length() - 1);
                return true;
            }
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE && settingsTarget != null) { closeSettings(); return true; }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (typingSearch) { searchQuery += chr; return true; }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public boolean shouldPause() { return false; }

    @Override
    public void close() {
        Map<Category, io.client.clickgui.CategoryPanel> cfg = new HashMap<>();
        int i = 0;
        for (Category cat : Category.values()) {
            cfg.put(cat, new io.client.clickgui.CategoryPanel(cat, i * 100, 5, false));
            i++;
        }
        ModuleManager.INSTANCE.saveUiConfig(cfg);
        ModuleManager.INSTANCE.saveModules();
        ModuleManager.INSTANCE.saveTheme(ClickGuiScreen.currentTheme);
        if (client != null) client.setScreen(null);
    }

    private void openSettings(Module m) {
        settingsTarget = m;
        settingsScroll = 0;
        settingRows.clear();
        buildRows(settingRows, m.getSettings(), 0);
    }

    private void closeSettings() {
        settingsTarget = null;
        settingRows.clear();
    }

    private void drawOutline(DrawContext ctx, int x, int y, int w, int h, int color) {
        ctx.fill(x, y, x + w, y + 1, color);
        ctx.fill(x, y + h - 1, x + w, y + h, color);
        ctx.fill(x, y, x + 1, y + h, color);
        ctx.fill(x + w - 1, y, x + w, y + h, color);
    }

    private List<String> wrapText(String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder current = new StringBuilder();

        for (String word : words) {
            String test = current.length() == 0 ? word : current + " " + word;
            if (textRenderer.getWidth(test) <= maxWidth) {
                current = new StringBuilder(test);
            } else {
                if (current.length() > 0) {
                    lines.add(current.toString());
                    current = new StringBuilder(word);
                } else {
                    lines.add(word);
                }
            }
        }
        if (current.length() > 0) {
            lines.add(current.toString());
        }
        return lines;
    }

    private static String fmt(float v) {
        return Math.abs(v - Math.round(v)) < 0.001f
                ? Integer.toString(Math.round(v))
                : String.format("%.2f", v);
    }

    private static void click() {
        try {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc != null && mc.getSoundManager() != null)
                mc.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK.value(), 1.0f));
        } catch (Exception ignored) {}
    }

    private abstract class SettingRow {
        final int indent;
        SettingRow(int indent) { this.indent = indent; }
        abstract void render(DrawContext ctx, int mx, int my, int x, int y, int w, Theme theme);
        void mouseClicked(int mx, int my, int btn, int x, int y, int w) {}
        void mouseReleased(int mx, int my, int btn, int x, int y, int w) {}
        int height() { return ROW_H; }
        boolean over(int mx, int my, int x, int y, int w) {
            return mx >= x && mx <= x + w && my >= y && my <= y + height();
        }
    }

    private final class BoolRow extends SettingRow {
        final BooleanSetting setting;
        BoolRow(BooleanSetting s, int indent) { super(indent); setting = s; }

        @Override
        void render(DrawContext ctx, int mx, int my, int x, int y, int w, Theme theme) {
            boolean hov = over(mx, my, x, y, w);
            boolean on  = setting.isEnabled();

            if (hov) {
                ctx.fill(x, y, x + w, y + ROW_H, theme.hoverHighlight);
            }

            int tW = 20, tH = 7;
            int tx = x + w - tW - 3, ty = y + ROW_H / 2 - tH / 2;
            ctx.fill(tx, ty, tx + tW, ty + tH,
                    on ? (0x55000000 | (theme.moduleEnabled & 0x00FFFFFF)) : theme.sliderBackground);
            drawOutline(ctx, tx, ty, tW, tH, on ? theme.moduleEnabled : theme.sliderBackground);
            int knobX = on ? tx + tW - tH + 1 : tx + 1;
            ctx.fill(knobX, ty + 1, knobX + tH - 2, ty + tH - 1,
                    on ? theme.sliderForeground : theme.moduleDisabled);

            ctx.drawTextWithShadow(textRenderer, setting.getName(), x + 3 + indent, y + 3,
                    on ? theme.sliderForeground : theme.moduleDisabled);
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

            if (hov) {
                ctx.fill(x, y, x + w, y + ROW_H, theme.hoverHighlight);
            }

            String val = setting.getSelectedOption();
            int valW = textRenderer.getWidth(val);
            ctx.drawTextWithShadow(textRenderer, val, x + w - valW - 3, y + 3, theme.moduleEnabled);
            ctx.drawTextWithShadow(textRenderer, setting.getName(), x + 3 + indent, y + 3, theme.moduleDisabled);
        }

        @Override
        void mouseClicked(int mx, int my, int btn, int x, int y, int w) {
            if (!over(mx, my, x, y, w)) return;
            if (btn == 0) setting.cycleNext();
            else if (btn == 1) {
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
        int height() { return ROW_H + 6; }

        @Override
        void render(DrawContext ctx, int mx, int my, int x, int y, int w, Theme theme) {
            boolean hov = over(mx, my, x, y, w);

            if (hov) {
                ctx.fill(x, y, x + w, y + height(), theme.hoverHighlight);
            }

            ctx.drawTextWithShadow(textRenderer, setting.getName(), x + 3 + indent, y + 3, theme.moduleDisabled);
            String val = fmt(setting.getValue());
            ctx.drawTextWithShadow(textRenderer, val, x + w - textRenderer.getWidth(val) - 3, y + 3, theme.moduleEnabled);

            int trackX = x + 3 + indent, trackW = w - 6 - indent;
            int trackY = y + ROW_H;
            float norm  = (setting.getValue() - setting.getMin()) / (setting.getMax() - setting.getMin());
            int fillW  = (int) (norm * trackW);

            ctx.fill(trackX, trackY, trackX + trackW, trackY + 3, theme.sliderBackground);
            ctx.fill(trackX, trackY, trackX + fillW, trackY + 3, theme.sliderForeground);
            ctx.fill(trackX + fillW - 1, trackY - 1, trackX + fillW + 2, trackY + 4, theme.sliderForeground);

            if (dragging) applyMouse(mx, x, w);
        }

        @Override
        void mouseClicked(int mx, int my, int btn, int x, int y, int w) {
            if (btn == 0 && over(mx, my, x, y, w)) {
                dragging = true;
                draggingNumber = this;
                applyMouse(mx, x, w);
            }
        }

        @Override
        void mouseReleased(int mx, int my, int btn, int x, int y, int w) {
            if (btn == 0) dragging = false;
        }

        void applyMouse(int mx, int x, int w) {
            int trackX = x + 3 + indent, trackW = w - 6 - indent;
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

            if (hov) {
                ctx.fill(x, y, x + w, y + ROW_H, theme.hoverHighlight);
            }

            String val = "\"" + setting.getValue() + "\"";
            int valW = textRenderer.getWidth(val);
            ctx.drawTextWithShadow(textRenderer, val, x + w - valW - 3, y + 3, theme.moduleEnabled);
            ctx.drawTextWithShadow(textRenderer, setting.getName(), x + 3 + indent, y + 3, theme.moduleDisabled);
        }
    }

    private final class CategoryRow extends SettingRow {
        final CategorySetting setting;
        final List<SettingRow> children = new ArrayList<>();
        boolean open;

        CategoryRow(CategorySetting s, int indent) { super(indent); setting = s; open = s.isExpanded(); }

        @Override
        int height() {
            if (!open) return ROW_H;
            return ROW_H + children.stream().mapToInt(SettingRow::height).sum();
        }

        @Override
        void render(DrawContext ctx, int mx, int my, int x, int y, int w, Theme theme) {
            boolean hov = mx >= x && mx <= x + w && my >= y && my <= y + ROW_H;
            ctx.fill(x, y, x + w, y + ROW_H, 0xFF000000 | (theme.panelBackground & 0x00FFFFFF));

            if (hov) {
                ctx.fill(x, y, x + w, y + ROW_H, theme.hoverHighlight);
            }

            ctx.fill(x, y, x + 2, y + ROW_H, theme.moduleEnabled);
            ctx.drawTextWithShadow(textRenderer, (open ? "\u2212 " : "\u002B ") + setting.getName(),
                    x + 5 + indent, y + 3, theme.sliderForeground);
            if (!open) return;
            int sy = y + ROW_H;
            for (SettingRow r : children) {
                r.render(ctx, mx, my, x, sy, w, theme);
                sy += r.height();
            }
        }

        @Override
        void mouseClicked(int mx, int my, int btn, int x, int y, int w) {
            if (mx >= x && mx <= x + w && my >= y && my <= y + ROW_H && btn == 1) {
                open = !open; setting.setExpanded(open); click(); return;
            }
            if (!open) return;
            int sy = y + ROW_H;
            for (SettingRow r : children) { r.mouseClicked(mx, my, btn, x, sy, w); sy += r.height(); }
        }

        @Override
        void mouseReleased(int mx, int my, int btn, int x, int y, int w) {
            if (!open) return;
            int sy = y + ROW_H;
            for (SettingRow r : children) { r.mouseReleased(mx, my, btn, x, sy, w); sy += r.height(); }
        }
    }

    private void buildRows(List<SettingRow> target, List<?> settings, int indent) {
        for (Object obj : settings) {
            if (!(obj instanceof Setting s)) continue;
            if (s instanceof CategorySetting cs) {
                CategoryRow cr = new CategoryRow(cs, indent);
                buildRows(cr.children, cs.getSettings(), indent + 5);
                target.add(cr);
            } else if (s instanceof BooleanSetting bs) target.add(new BoolRow(bs, indent));
            else if (s instanceof RadioSetting rs)     target.add(new RadioRow(rs, indent));
            else if (s instanceof NumberSetting ns)    target.add(new NumberRow(ns, indent));
            else if (s instanceof StringSetting ss)    target.add(new StringRow(ss, indent));
        }
    }
}
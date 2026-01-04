package io.client;

import io.client.clickgui.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class ClickGuiScreen extends Screen {
    private static final int PANEL_WIDTH = 90;
    private static final int TITLE_BAR_HEIGHT = 13;
    private static final int PANEL_GAP = 10;

    public static boolean opened = false;
    public static Theme currentTheme = Theme.IO;

    private final Map<Category, CategoryPanel> panels = new HashMap<>();
    private final PanelRenderer renderer;
    private final InputHandler inputHandler;

    public ClickGuiScreen() {
        super(Component.literal("IO Client"));

        Theme savedTheme = ModuleManager.INSTANCE.loadTheme();
        if (savedTheme != null) currentTheme = savedTheme;

        this.renderer = new PanelRenderer(currentTheme);
        this.inputHandler = new InputHandler(this);

        initializePanels();
    }

    private void initializePanels() {
        Map<Category, SavedPanelConfig> loadedConfig = ModuleManager.INSTANCE.loadUiConfig();
        AtomicInteger index = new AtomicInteger(0);
        int initialY = 20;

        for (Category category : Category.values()) {
            SavedPanelConfig config = loadedConfig.get(category);
            if (config != null) {
                panels.put(category, new CategoryPanel(category, config.x, config.y, config.collapsed));
            } else {
                int defaultX = 20 + (index.get() * (PANEL_WIDTH + PANEL_GAP));
                panels.put(category, new CategoryPanel(category, defaultX, initialY, false));
            }
            index.incrementAndGet();
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        opened = true;
        renderer.setTheme(currentTheme);

        String hoveredDescription = null;

        for (CategoryPanel panel : panels.values()) {
            String desc = renderer.renderPanel(graphics, font, panel, mouseX, mouseY);
            if (desc != null) hoveredDescription = desc;
        }

        if (hoveredDescription != null) {
            renderTooltip(graphics, hoveredDescription, mouseX, mouseY);
        }

        super.render(graphics, mouseX, mouseY, partialTicks);
    }

    private void renderTooltip(GuiGraphics graphics, String description, int mouseX, int mouseY) {
        Minecraft mc = Minecraft.getInstance();
        int tipWidth = mc.font.width(description) + 6;
        int tipHeight = mc.font.lineHeight + 6;
        int tipX = mouseX + 10;
        int tipY = mouseY - 5;
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        if (tipX + tipWidth > screenWidth) tipX = mouseX - tipWidth - 5;
        if (tipY + tipHeight > screenHeight) tipY = screenHeight - tipHeight - 5;
        if (tipY < 0) tipY = 0;

        graphics.fill(tipX, tipY, tipX + tipWidth, tipY + tipHeight, 0xE0000000);
        graphics.fill(tipX, tipY, tipX + tipWidth, tipY + 1, 0x44FFFFFF);
        graphics.drawString(mc.font, description, tipX + 3, tipY + 3, 0xFFFFFFFF, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (inputHandler.handleMouseClick(panels, mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (inputHandler.handleMouseRelease(panels, mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (inputHandler.handleMouseDrag(panels, mouseX, mouseY, button, dragX, dragY)) {
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        ModuleManager.INSTANCE.saveUiConfig(panels);
        ModuleManager.INSTANCE.saveModules();
        ModuleManager.INSTANCE.saveTheme(currentTheme);
        if (this.minecraft != null) {
            this.minecraft.setScreen(null);
        }
    }
}
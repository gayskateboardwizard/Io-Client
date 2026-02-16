package io.client;

import io.client.clickgui.*;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class ClickGuiScreen extends Screen {
    private static final int PANEL_GAP = 10;

    public static int clickGuiKey = GLFW.GLFW_KEY_BACKSLASH;

    public static boolean opened = false;
    public static Theme currentTheme = Theme.IO;

    private final Map<Category, CategoryPanel> panels = new HashMap<>();
    private final PanelRenderer renderer;
    private final InputHandler inputHandler;

    public ClickGuiScreen() {
        super(Text.literal("IO Client"));

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
                int panelWidth = PanelRenderer.getPanelWidth();
                int defaultX = 20 + (index.get() * (panelWidth + PANEL_GAP));
                panels.put(category, new CategoryPanel(category, defaultX, initialY, false));
            }
            index.incrementAndGet();
        }
    }

    @Override
    public void render(DrawContext graphics, int mouseX, int mouseY, float partialTicks) {
        opened = true;
        renderer.setTheme(currentTheme);

        String hoveredDescription = null;

        for (CategoryPanel panel : panels.values()) {
            String desc = renderer.renderPanel(graphics, textRenderer, panel, mouseX, mouseY);
            if (desc != null) hoveredDescription = desc;
        }

        if (hoveredDescription != null) {
            renderTooltip(graphics, hoveredDescription, mouseX, mouseY);
        }

        super.render(graphics, mouseX, mouseY, partialTicks);
    }

    private void renderTooltip(DrawContext graphics, String description, int mouseX, int mouseY) {
        MinecraftClient mc = MinecraftClient.getInstance();

        graphics.getMatrices().pushMatrix();
        graphics.getMatrices().translate(mouseX + 10, mouseY - 5);

        float scale = PanelRenderer.getPanelWidth() / 90.0f;
        graphics.getMatrices().scale(scale, scale);

        int tipWidth = mc.textRenderer.getWidth(description) + 6;
        int tipHeight = mc.textRenderer.fontHeight + 6;
        int screenWidth = mc.getWindow().getScaledWidth();
        int screenHeight = mc.getWindow().getScaledHeight();

        int tipX = 0;
        int tipY = 0;

        if ((mouseX + 10 + tipWidth * scale) > screenWidth) tipX = (int)(-(tipWidth + 15) * scale);
        if ((mouseY - 5 + tipHeight * scale) > screenHeight) tipY = (int)((screenHeight - mouseY - tipHeight - 5) / scale);
        if ((mouseY - 5) < 0) tipY = (int)(-mouseY / scale);

        graphics.fill(tipX, tipY, tipX + tipWidth, tipY + tipHeight, 0xE0000000);
        graphics.fill(tipX, tipY, tipX + tipWidth, tipY + 1, 0x44FFFFFF);
        graphics.drawText(mc.textRenderer, description, tipX + 3, tipY + 3, 0xFFFFFFFF, false);

        graphics.getMatrices().popMatrix();
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
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void close() {
        ModuleManager.INSTANCE.saveUiConfig(panels);
        ModuleManager.INSTANCE.saveModules();
        ModuleManager.INSTANCE.saveTheme(currentTheme);
        if (this.client != null) {
            this.client.setScreen(null);
        }
    }
}
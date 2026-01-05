package io.client;

import io.client.clickgui.Theme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class TargetsScreen extends Screen {
    private static final int PANEL_WIDTH = 150;
    private static final int ITEM_HEIGHT = 20;
    private static final int TITLE_BAR_HEIGHT = 25;
    private final Screen parentScreen;
    private int panelX;
    private int panelY;
    private boolean dragging = false;
    private int dragOffsetX;
    private int dragOffsetY;

    public TargetsScreen(Screen parentScreen) {
        super(Component.literal("Target Selection"));
        this.parentScreen = parentScreen;
    }

    @Override
    protected void init() {
        int panelHeight = TITLE_BAR_HEIGHT + (TargetManager.TargetType.values().length * ITEM_HEIGHT) + 10;

        this.panelX = (this.width / 2) - (PANEL_WIDTH / 2);
        this.panelY = (this.height / 2) - (panelHeight / 2);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {

        if (this.parentScreen != null) {
            this.parentScreen.render(graphics, 0, 0, partialTicks);
        }

        graphics.fill(0, 0, this.width, this.height, 0x42000000);


        Theme theme = ClickGuiScreen.currentTheme;

        int panelHeight = TITLE_BAR_HEIGHT + (TargetManager.TargetType.values().length * ITEM_HEIGHT) + 10;

        int titleBarColor = (theme.titleBar & 0x00FFFFFF) | 0xFF000000;
        graphics.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + TITLE_BAR_HEIGHT, titleBarColor);
        graphics.drawString(this.font, "Target Selection", panelX + 5, panelY + 8, 0xFFFFFFFF, false);

        graphics.drawString(this.font, "X", panelX + PANEL_WIDTH - 15, panelY + 8, theme.moduleEnabled, false);

        int panelBackgroundColor = (theme.panelBackground & 0x00FFFFFF) | 0xFF000000;
        graphics.fill(panelX, panelY + TITLE_BAR_HEIGHT, panelX + PANEL_WIDTH, panelY + panelHeight, panelBackgroundColor);

        int yOffset = panelY + TITLE_BAR_HEIGHT + 5;
        for (TargetManager.TargetType type : TargetManager.TargetType.values()) {
            boolean enabled = TargetManager.INSTANCE.isTargetEnabled(type);

            int color = enabled ? theme.moduleEnabled : theme.moduleDisabled;

            if (mouseX >= panelX && mouseX <= panelX + PANEL_WIDTH &&
                    mouseY >= yOffset && mouseY <= yOffset + ITEM_HEIGHT) {
                graphics.fill(panelX, yOffset, panelX + PANEL_WIDTH, yOffset + ITEM_HEIGHT, theme.hoverHighlight);
            }

            String checkbox = enabled ? "[X]" : "[ ]";
            graphics.drawString(this.font, checkbox, panelX + 5, yOffset + 6, color, false);
            graphics.drawString(this.font, type.getName(), panelX + 30, yOffset + 6, 0xFFFFFFFF, false);

            yOffset += ITEM_HEIGHT;
        }

        super.render(graphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (mouseX >= panelX + PANEL_WIDTH - 20 && mouseX <= panelX + PANEL_WIDTH &&
                mouseY >= panelY && mouseY <= panelY + TITLE_BAR_HEIGHT) {
            this.onClose();
            return true;
        }

        if (mouseX >= panelX && mouseX <= panelX + PANEL_WIDTH - 20 &&
                mouseY >= panelY && mouseY <= panelY + TITLE_BAR_HEIGHT) {
            dragging = true;
            dragOffsetX = (int) (mouseX - panelX);
            dragOffsetY = (int) (mouseY - panelY);
            return true;
        }

        int yOffset = panelY + TITLE_BAR_HEIGHT + 5;
        TargetManager.TargetType[] types = TargetManager.TargetType.values();
        for (TargetManager.TargetType type : types) {
            if (mouseX >= panelX && mouseX <= panelX + PANEL_WIDTH &&
                    mouseY >= yOffset && mouseY <= yOffset + ITEM_HEIGHT) {
                TargetManager.INSTANCE.toggleTarget(type);
                return true;
            }
            yOffset += ITEM_HEIGHT;
        }

        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (dragging) {
            dragging = false;
            return true;
        }

        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (dragging) {
            panelX = (int) (mouseX - dragOffsetX);
            panelY = (int) (mouseY - dragOffsetY);
            return true;
        }

        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parentScreen);
        }
    }
}
package io.client.modules;

import io.client.Category;
import io.client.ClickGuiScreen;
import io.client.Module;
import io.client.ModuleManager;
import io.client.settings.RadioSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ModuleHUD extends Module {
    private final RadioSetting horizontal;
    private final RadioSetting vertical;
    private final RadioSetting sorting;

    public ModuleHUD() {
        super("ModuleHUD", "Shows enabled modules", 0, Category.RENDER);

        horizontal = new RadioSetting("Horizontal", "Left");
        horizontal.addOption("Left");
        horizontal.addOption("Right");

        vertical = new RadioSetting("Vertical", "Top");
        vertical.addOption("Top");
        vertical.addOption("Bottom");

        sorting = new RadioSetting("Sorting", "Length");
        sorting.addOption("Length");
        sorting.addOption("Alphabetical");
        sorting.addOption("None");

        addSetting(horizontal);
        addSetting(vertical);
        addSetting(sorting);
    }

    public void render(GuiGraphics graphics) {
        if (!isEnabled()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        float scale = 1.0f;
        GUIScale guiScale = ModuleManager.INSTANCE.getModule(GUIScale.class);
        if (guiScale != null) {
            scale = guiScale.getScale();
        }

        List<Module> activeModules = new ArrayList<>();
        for (Module module : ModuleManager.INSTANCE.getModules()) {
            if (module.isEnabled() && module != this && module.getCategory() != Category.SETTINGS) {
                activeModules.add(module);
            }
        }

        if (sorting.isSelected("Length")) {
            activeModules.sort(Comparator.comparingInt(m -> -mc.font.width(m.getName())));
        } else if (sorting.isSelected("Alphabetical")) {
            activeModules.sort(Comparator.comparing(Module::getName));
        }

        boolean isTop = vertical.isSelected("Top");
        boolean isLeft = horizontal.isSelected("Left");

        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        int lineHeight = (int)(10 * scale);
        int startY = isTop ? 2 : screenHeight - (activeModules.size() * lineHeight + 2);

        graphics.pose().pushMatrix();
        graphics.pose().scale(scale, scale);

        for (int i = 0; i < activeModules.size(); i++) {
            Module module = activeModules.get(i);
            String name = module.getName();
            int textWidth = mc.font.width(name);

            int y = isTop ? (startY + i * lineHeight) : (startY + i * lineHeight);
            int x = isLeft ? 2 : (screenWidth - (int)(textWidth * scale) - 2);

            int scaledX = (int) (x / scale);
            int scaledY = (int) (y / scale);

            graphics.drawString(mc.font, name, scaledX, scaledY, ClickGuiScreen.currentTheme.moduleEnabled, true);
        }

        graphics.pose().popMatrix();
    }
}
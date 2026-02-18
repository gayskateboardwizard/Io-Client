package io.client.modules.settings;

import io.client.Category;
import io.client.ClickGuiScreen;
import io.client.Module;
import io.client.ModuleManager;
import io.client.clickgui.ClickGuiModeRegistry;
import io.client.clickgui.Theme;
import io.client.settings.RadioSetting;
import net.minecraft.client.MinecraftClient;

public class ThemeChanger extends Module {
    public final RadioSetting themeSelect = new RadioSetting("Theme", "Io");
    public final RadioSetting clickGuiMode = new RadioSetting("ClickGui", ClickGuiModeRegistry.getDefaultModeName());
    public final RadioSetting guiFont = new RadioSetting("GuiFont", "Minecraft");
    private String lastSelectedTheme = "Io";
    private String lastSelectedClickGuiMode = ClickGuiModeRegistry.getDefaultModeName();

    public ThemeChanger() {
        super("Themes", "Change the UI looks", -1, Category.SETTINGS);
        themeSelect.addOption("Io");
        themeSelect.addOption("Ganymede");
        themeSelect.addOption("Callisto");
        themeSelect.addOption("Europa");
        themeSelect.addOption("Amalthea");
        themeSelect.addOption("Thebe");
        themeSelect.addOption("Metis");
        themeSelect.addOption("Adrastea");
        themeSelect.addOption("Future");
        themeSelect.addOption("RedFuture");
        themeSelect.addOption("Titan");
        themeSelect.addOption("Phobos");
        themeSelect.addOption("Deimos");
        themeSelect.addOption("Triton");

        for (String modeName : ClickGuiModeRegistry.getModeNames()) {
            clickGuiMode.addOption(modeName);
        }

        guiFont.addOption("Minecraft");
        guiFont.addOption("JetBrains Mono");
        guiFont.addOption("Verdana");

        addSetting(themeSelect);
        addSetting(clickGuiMode);
        addSetting(guiFont);

        Theme savedTheme = ModuleManager.INSTANCE.loadTheme();
        if (savedTheme != null) {
            ClickGuiScreen.currentTheme = savedTheme;
            lastSelectedTheme = savedTheme.getName();
            themeSelect.setSelectedOption(savedTheme.getName());
        } else if (ClickGuiScreen.opened && ClickGuiScreen.currentTheme != null) {
            lastSelectedTheme = ClickGuiScreen.currentTheme.getName();
            themeSelect.setSelectedOption(ClickGuiScreen.currentTheme.getName());
        }
    }

    @Override
    public void onUpdate() {
        String currentSelection = themeSelect.getSelectedOption();
        if (!currentSelection.equals(lastSelectedTheme)) {
            selectTheme(currentSelection);
            lastSelectedTheme = currentSelection;
        }

        String selectedGuiMode = clickGuiMode.getSelectedOption();
        if (!selectedGuiMode.equals(lastSelectedClickGuiMode)) {
            applyClickGuiMode(selectedGuiMode);
            lastSelectedClickGuiMode = selectedGuiMode;
        }
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public void toggle() {
    }

    public void selectTheme(String name) {
        switch (name) {
            case "Io" -> ClickGuiScreen.currentTheme = Theme.IO;
            case "Ganymede" -> ClickGuiScreen.currentTheme = Theme.GANYMEDE;
            case "Callisto" -> ClickGuiScreen.currentTheme = Theme.CALLISTO;
            case "Europa" -> ClickGuiScreen.currentTheme = Theme.EUROPA;
            case "Amalthea" -> ClickGuiScreen.currentTheme = Theme.AMALTHEA;
            case "Thebe" -> ClickGuiScreen.currentTheme = Theme.THEBE;
            case "Metis" -> ClickGuiScreen.currentTheme = Theme.METIS;
            case "Adrastea" -> ClickGuiScreen.currentTheme = Theme.ADRASTEA;
            case "Future" -> ClickGuiScreen.currentTheme = Theme.FUTURE;
            case "RedFuture" -> ClickGuiScreen.currentTheme = Theme.FUTURE_RED;
            case "Titan" -> ClickGuiScreen.currentTheme = Theme.TITAN;
            case "Phobos" -> ClickGuiScreen.currentTheme = Theme.PHOBOS;
            case "Deimos" -> ClickGuiScreen.currentTheme = Theme.DEIMOS;
            case "Triton" -> ClickGuiScreen.currentTheme = Theme.TRITON;
        }
        ModuleManager.INSTANCE.saveTheme(ClickGuiScreen.currentTheme);
    }

    public String getSelectedTheme() {
        return themeSelect.getSelectedOption();
    }

    public String getSelectedClickGuiMode() {
        return clickGuiMode.getSelectedOption();
    }

    public String getSelectedFontMode() {
        return guiFont.getSelectedOption();
    }

    private void applyClickGuiMode(String modeName) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.currentScreen != null && ClickGuiModeRegistry.isClickGuiScreen(mc.currentScreen)) {
            mc.setScreen(ClickGuiModeRegistry.createScreen(modeName));
        }
        ModuleManager.INSTANCE.saveModules();
    }
}
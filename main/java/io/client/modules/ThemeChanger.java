package io.client.modules;

import io.client.Category;
import io.client.ClickGuiScreen;
import io.client.Module;
import io.client.ModuleManager;
import io.client.clickgui.Theme;
import io.client.settings.RadioSetting;

public class ThemeChanger extends Module {
    public final RadioSetting themeSelect = new RadioSetting("Theme", "Io");
    private String lastSelectedTheme = "Io";

    public ThemeChanger() {
        super("Themes", "Change the UI colors", -1, Category.SETTINGS);
        themeSelect.addOption("Io");
        themeSelect.addOption("Ganymede");
        themeSelect.addOption("Callisto");
        themeSelect.addOption("Europa");
        themeSelect.addOption("Amalthea");
        themeSelect.addOption("Thebe");
        themeSelect.addOption("Metis");
        themeSelect.addOption("Adrastea");

        addSetting(themeSelect);

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
        }
        ModuleManager.INSTANCE.saveTheme(ClickGuiScreen.currentTheme);
    }

    public String getSelectedTheme() {
        return themeSelect.getSelectedOption();
    }
}

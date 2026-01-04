package io.client.clickgui;

import io.client.Category;
import io.client.Module;
import io.client.ModuleManager;
import io.client.TargetsScreen;
import io.client.settings.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

import java.util.List;
import java.util.Map;

public class InputHandler {
    private static final int PANEL_WIDTH = 90;
    private static final int MODULE_HEIGHT = 10;
    private static final int SETTING_HEIGHT = 12;
    private static final int TITLE_BAR_HEIGHT = 13;
    private final Screen parentScreen;
    private NumberSetting draggingSetting = null;

    public InputHandler(Screen parentScreen) {
        this.parentScreen = parentScreen;
    }

    public boolean handleMouseClick(Map<Category, CategoryPanel> panels, double mouseX, double mouseY, int button) {
        for (CategoryPanel panel : panels.values()) {
            if (mouseX >= panel.x && mouseX <= panel.x + PANEL_WIDTH && mouseY >= panel.y && mouseY <= panel.y + TITLE_BAR_HEIGHT) {
                if (mouseX >= panel.x + PANEL_WIDTH - 15) {
                    panel.collapsed = !panel.collapsed;
                    ModuleManager.INSTANCE.saveUiConfig(panels);
                    return true;
                }

                if (panel.category == Category.COMBAT && mouseX >= panel.x + PANEL_WIDTH - 32 && mouseX <= panel.x + PANEL_WIDTH - 20) {
                    Minecraft.getInstance().setScreen(new TargetsScreen(parentScreen));
                    return true;
                }

                panel.dragging = true;
                panel.dragOffsetX = (int) (mouseX - panel.x);
                panel.dragOffsetY = (int) (mouseY - panel.y);
                return true;
            }

            if (!panel.collapsed) {
                if (handlePanelContentClick(panel, mouseX, mouseY, button)) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean handlePanelContentClick(CategoryPanel panel, double mouseX, double mouseY, int button) {
        int yOffset = panel.y + TITLE_BAR_HEIGHT + 2;
        List<Module> modules = ModuleManager.INSTANCE.getModulesByCategory(panel.category);

        for (Module module : modules) {
            int x1 = panel.x, y1 = yOffset, x2 = panel.x + PANEL_WIDTH, y2 = yOffset + MODULE_HEIGHT;
            boolean isThemeChanger = panel.category == Category.SETTINGS && module.getName().equals("Themes");

            if (mouseX >= x1 && mouseX <= x2 && mouseY >= y1 && mouseY <= y2) {
                if (button == 0 && !isThemeChanger) {
                    module.toggle();
                    ModuleManager.INSTANCE.saveModules();
                } else if (button == 1) {
                    module.setExtended(!module.isExtended());
                    ModuleManager.INSTANCE.saveModules();
                }
                return true;
            }

            yOffset += MODULE_HEIGHT;
            if (module.isExtended()) {
                int newYOffset = handleSettingsClick(panel, module.getSettings(), yOffset, mouseX, mouseY, button);
                if (newYOffset == -1) return true;
                yOffset = newYOffset;
            }
        }

        return false;
    }

    private int handleSettingsClick(CategoryPanel panel, List<Setting> settings, int yOffset, double mouseX, double mouseY, int button) {
        for (Setting setting : settings) {
            if (setting instanceof CategorySetting catSetting) {
                int catY1 = yOffset, catY2 = yOffset + SETTING_HEIGHT;

                if (mouseX >= panel.x && mouseX <= panel.x + PANEL_WIDTH && mouseY >= catY1 && mouseY <= catY2) {
                    if (button == 1) {
                        catSetting.toggleExpanded();
                        ModuleManager.INSTANCE.saveModules();
                        return -1;
                    }
                }

                yOffset += SETTING_HEIGHT;
                if (catSetting.isExpanded()) {
                    for (Object catItem : catSetting.getSettings()) {
                        int catItemY1 = yOffset, catItemY2 = yOffset + SETTING_HEIGHT;

                        if (mouseX >= panel.x && mouseX <= panel.x + PANEL_WIDTH && mouseY >= catItemY1 && mouseY <= catItemY2) {
                            if (catItem instanceof NumberSetting numSetting) {
                                if (button == 0) {
                                    draggingSetting = numSetting;
                                    updateSliderValue(numSetting, mouseX, panel.x);
                                    ModuleManager.INSTANCE.saveModules();
                                }
                            } else if (catItem instanceof BooleanSetting boolSetting) {
                                if (button == 0) {
                                    boolSetting.toggle();
                                    ModuleManager.INSTANCE.saveModules();
                                }
                            } else if (catItem instanceof RadioSetting radioSetting) {
                                if (button == 0) {
                                    radioSetting.cycleNext();
                                    ModuleManager.INSTANCE.saveModules();
                                }
                            }
                            return -1;
                        }
                        yOffset += SETTING_HEIGHT;
                    }
                }
            } else if (setting instanceof NumberSetting numSetting) {
                int settingY1 = yOffset, settingY2 = yOffset + SETTING_HEIGHT;

                if (mouseX >= panel.x && mouseX <= panel.x + PANEL_WIDTH && mouseY >= settingY1 && mouseY <= settingY2) {
                    if (button == 0) {
                        draggingSetting = numSetting;
                        updateSliderValue(numSetting, mouseX, panel.x);
                        ModuleManager.INSTANCE.saveModules();
                    }
                    return -1;
                }
                yOffset += SETTING_HEIGHT;
            } else if (setting instanceof BooleanSetting boolSetting) {
                int settingY1 = yOffset, settingY2 = yOffset + SETTING_HEIGHT;

                if (mouseX >= panel.x && mouseX <= panel.x + PANEL_WIDTH && mouseY >= settingY1 && mouseY <= settingY2) {
                    boolSetting.toggle();
                    ModuleManager.INSTANCE.saveModules();
                    return -1;
                }
                yOffset += SETTING_HEIGHT;
            } else if (setting instanceof StringSetting) {
                yOffset += SETTING_HEIGHT;
            } else if (setting instanceof RadioSetting radioSetting) {
                int settingY1 = yOffset, settingY2 = yOffset + SETTING_HEIGHT;

                if (mouseX >= panel.x && mouseX <= panel.x + PANEL_WIDTH && mouseY >= settingY1 && mouseY <= settingY2) {
                    if (button == 0) {
                        radioSetting.cycleNext();
                        ModuleManager.INSTANCE.saveModules();
                    }
                    return -1;
                }
                yOffset += SETTING_HEIGHT;
            }
        }

        return yOffset;
    }

    public boolean handleMouseRelease(Map<Category, CategoryPanel> panels, double mouseX, double mouseY, int button) {
        if (draggingSetting != null) {
            draggingSetting = null;
            ModuleManager.INSTANCE.saveModules();
            return true;
        }

        for (CategoryPanel panel : panels.values()) {
            if (panel.dragging) {
                ModuleManager.INSTANCE.saveUiConfig(panels);
            }
            panel.dragging = false;
        }

        return false;
    }

    public boolean handleMouseDrag(Map<Category, CategoryPanel> panels, double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (draggingSetting != null) {
            CategoryPanel panel = findPanelForSetting(panels, draggingSetting);
            if (panel != null) {
                updateSliderValue(draggingSetting, mouseX, panel.x);
            }
            return true;
        }

        for (CategoryPanel panel : panels.values()) {
            if (panel.dragging) {
                Minecraft mc = Minecraft.getInstance();
                int screenWidth = mc.getWindow().getGuiScaledWidth();
                int screenHeight = mc.getWindow().getGuiScaledHeight();

                int newX = (int) mouseX - panel.dragOffsetX;
                int newY = (int) mouseY - panel.dragOffsetY;

                panel.x = Math.max(0, Math.min(newX, screenWidth - PANEL_WIDTH));
                panel.y = Math.max(0, Math.min(newY, screenHeight - TITLE_BAR_HEIGHT));
                return true;
            }
        }

        return false;
    }

    private void updateSliderValue(NumberSetting setting, double mouseX, int panelX) {
        double percent = Math.max(0, Math.min(1, (mouseX - (panelX + 5)) / (PANEL_WIDTH - 10)));
        float newValue = setting.getMin() + (float) (percent * (setting.getMax() - setting.getMin()));
        setting.setValue(Math.round(newValue * 10f) / 10f);
    }

    private CategoryPanel findPanelForSetting(Map<Category, CategoryPanel> panels, NumberSetting setting) {
        for (CategoryPanel panel : panels.values()) {
            List<Module> modules = ModuleManager.INSTANCE.getModulesByCategory(panel.category);
            for (Module module : modules) {
                for (Setting s : module.getSettings()) {
                    if (s == setting) {
                        return panel;
                    }

                    if (s instanceof CategorySetting catSetting) {
                        for (Object catItem : catSetting.getSettings()) {
                            if (catItem instanceof NumberSetting && catItem == setting) {
                                return panel;
                            }
                        }
                    }
                }
            }
        }
        return null;
    }
}
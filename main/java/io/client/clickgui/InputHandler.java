package io.client.clickgui;

import io.client.managers.ModuleManager;
import io.client.modules.templates.Category;
import io.client.modules.templates.Module;
import io.client.settings.*;
import java.util.List;
import java.util.Map;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;

public class InputHandler {
    private final Screen parentScreen;
    private NumberSetting draggingSetting = null;

    public InputHandler(Screen parentScreen) {
        this.parentScreen = parentScreen;
    }

    public boolean handleMouseClick(Map<Category, CategoryPanel> panels, double mouseX, double mouseY, int button) {
        for (CategoryPanel panel : panels.values()) {
            int panelWidth = PanelRenderer.getPanelWidth();
            int titleBarHeight = PanelRenderer.getTitleBarHeight();

            if (mouseX >= panel.x && mouseX <= panel.x + panelWidth && mouseY >= panel.y && mouseY <= panel.y + titleBarHeight) {
                if (mouseX >= panel.x + panelWidth - 15) {
                    panel.collapsed = !panel.collapsed;
                    ModuleManager.INSTANCE.saveUiConfig(panels);
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
        int titleBarHeight = PanelRenderer.getTitleBarHeight();
        int moduleHeight = PanelRenderer.getModuleHeight();
        int panelWidth = PanelRenderer.getPanelWidth();

        int yOffset = panel.y + titleBarHeight + 2;
        List<Module> modules = ModuleManager.INSTANCE.getModulesByCategory(panel.category);

        for (Module module : modules) {
            int x1 = panel.x, y1 = yOffset, x2 = panel.x + panelWidth, y2 = yOffset + moduleHeight;
            boolean isThemeChanger = panel.category == Category.SETTINGS && module.getName().equals("Themes");
            boolean isGuiScale = panel.category == Category.SETTINGS && module.getName().equals("GuiScale");

            if (mouseX >= x1 && mouseX <= x2 && mouseY >= y1 && mouseY <= y2) {
                if (button == 0 && !isThemeChanger && !isGuiScale) {
                    module.toggle();
                    ModuleManager.INSTANCE.saveModules();
                } else if (button == 1) {
                    module.setExtended(!module.isExtended());
                    ModuleManager.INSTANCE.saveModules();
                }
                return true;
            }

            yOffset += moduleHeight;
            if (module.isExtended()) {
                int newYOffset = handleSettingsClick(panel, module.getSettings(), yOffset, mouseX, mouseY, button);
                if (newYOffset == -1) return true;
                yOffset = newYOffset;
            }
        }

        return false;
    }

    private int handleSettingsClick(CategoryPanel panel, List<Setting> settings, int yOffset, double mouseX, double mouseY, int button) {
        int settingHeight = PanelRenderer.getSettingHeight();
        int panelWidth = PanelRenderer.getPanelWidth();

        for (Setting setting : settings) {
            if (setting instanceof CategorySetting catSetting) {
                int catY1 = yOffset, catY2 = yOffset + settingHeight;

                if (mouseX >= panel.x && mouseX <= panel.x + panelWidth && mouseY >= catY1 && mouseY <= catY2) {
                    if (button == 1) {
                        catSetting.toggleExpanded();
                        ModuleManager.INSTANCE.saveModules();
                        return -1;
                    }
                }

                yOffset += settingHeight;
                if (catSetting.isExpanded()) {
                    for (Object catItem : catSetting.getSettings()) {
                        int catItemY1 = yOffset, catItemY2 = yOffset + settingHeight;

                        if (mouseX >= panel.x && mouseX <= panel.x + panelWidth && mouseY >= catItemY1 && mouseY <= catItemY2) {
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
                        yOffset += settingHeight;
                    }
                }
            } else if (setting instanceof NumberSetting numSetting) {
                int settingY1 = yOffset, settingY2 = yOffset + settingHeight;

                if (mouseX >= panel.x && mouseX <= panel.x + panelWidth && mouseY >= settingY1 && mouseY <= settingY2) {
                    if (button == 0) {
                        draggingSetting = numSetting;
                        updateSliderValue(numSetting, mouseX, panel.x);
                        ModuleManager.INSTANCE.saveModules();
                    }
                    return -1;
                }
                yOffset += settingHeight;
            } else if (setting instanceof BooleanSetting boolSetting) {
                int settingY1 = yOffset, settingY2 = yOffset + settingHeight;

                if (mouseX >= panel.x && mouseX <= panel.x + panelWidth && mouseY >= settingY1 && mouseY <= settingY2) {
                    boolSetting.toggle();
                    ModuleManager.INSTANCE.saveModules();
                    return -1;
                }
                yOffset += settingHeight;
            } else if (setting instanceof StringSetting) {
                yOffset += settingHeight;
            } else if (setting instanceof RadioSetting radioSetting) {
                int settingY1 = yOffset, settingY2 = yOffset + settingHeight;

                if (mouseX >= panel.x && mouseX <= panel.x + panelWidth && mouseY >= settingY1 && mouseY <= settingY2) {
                    if (button == 0) {
                        radioSetting.cycleNext();
                        ModuleManager.INSTANCE.saveModules();
                    }
                    return -1;
                }
                yOffset += settingHeight;
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
                MinecraftClient mc = MinecraftClient.getInstance();
                int screenWidth = mc.getWindow().getScaledWidth();
                int screenHeight = mc.getWindow().getScaledHeight();
                int panelWidth = PanelRenderer.getPanelWidth();
                int titleBarHeight = PanelRenderer.getTitleBarHeight();

                int newX = (int) mouseX - panel.dragOffsetX;
                int newY = (int) mouseY - panel.dragOffsetY;

                panel.x = Math.max(0, Math.min(newX, screenWidth - panelWidth));
                panel.y = Math.max(0, Math.min(newY, screenHeight - titleBarHeight));
                return true;
            }
        }

        return false;
    }

    private void updateSliderValue(NumberSetting setting, double mouseX, int panelX) {
        int panelWidth = PanelRenderer.getPanelWidth();
        double percent = Math.max(0, Math.min(1, (mouseX - (panelX + 5)) / (panelWidth - 10)));
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


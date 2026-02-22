package io.client.managers;

import io.client.clickgui.screens.ClickGuiScreen;
import io.client.clickgui.CategoryPanel;
import io.client.clickgui.SavedPanelConfig;
import io.client.clickgui.Theme;
import io.client.commands.CommandManager;
import io.client.modules.combat.*;
import io.client.modules.macros.Macro;
import io.client.modules.misc.*;
import io.client.modules.movement.*;
import io.client.modules.render.*;
import io.client.modules.render.Fullbright;
import io.client.modules.settings.*;
import io.client.modules.templates.Category;
import io.client.modules.templates.Module;
import io.client.modules.world.*;
import io.client.settings.*;
import org.lwjgl.glfw.GLFW;
import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import net.minecraft.client.MinecraftClient;

public class ModuleManager {
    public static final ModuleManager INSTANCE = new ModuleManager();
    private static final String CLIENT_FOLDER_NAME = "io";
    private static final String MODULE_CONFIG_FILE = "modules.cfg";
    private static final String UI_CONFIG_FILE = "ui.cfg";
    private static final String KEYBIND_CONFIG_FILE = "keybinds.cfg";
    private static final String THEME_CONFIG_FILE = "theme.cfg";
    private static final String FRIENDS_CONFIG_FILE = "friends.cfg";
    private static final String TARGETS_CONFIG_FILE = "targets.cfg";
    private final List<Module> modules = new ArrayList<>();
    private boolean initialized = false;

    private ModuleManager() {
    }

    public void initTheme() {
        Theme saved = loadTheme();
        ClickGuiScreen.currentTheme = saved != null ? saved : Theme.IO;
        ClickGuiScreen.opened = true;
    }

    public void init() {
        if (initialized)
            return;
        initialized = true;

        modules.add(new ElytraFlight());
        modules.add(new BoatFlight());
        modules.add(new Speed());
        modules.add(new NoFall());
        modules.add(new Step());
        modules.add(new Jesus());
        modules.add(new Spider());
        modules.add(new AutoSprint());
        modules.add(new Killaura());
        modules.add(new Velocity());
        modules.add(new Criticals());
        modules.add(new OffHand());
        modules.add(new Fullbright());
        modules.add(new Nuker());
        modules.add(new Scaffold());
        modules.add(new AutoMine());
        modules.add(new AutoTool());
        modules.add(new EXPThrower());
        modules.add(new FakePlayerModule());
        modules.add(new DiscordRPCModule());
        modules.add(new IoSwag());
        modules.add(new CrystalAura());
        modules.add(new CrystalAuraV2());
        modules.add(new HoleFiller());
        modules.add(new AutoEat());
        modules.add(new AntiAFK());
        modules.add(new MessageSpammer());
        modules.add(new GuiMove());
        modules.add(new Safety());
        modules.add(new ThemeChanger());
        modules.add(new CapeSettings());
        modules.add(new HUD());
        modules.add(new ESP());
        modules.add(new Tracers());
        modules.add(new Chams());
        modules.add(new ArsonAura());
        modules.add(new FastFall());
        modules.add(new Surround());
        modules.add(new Burrow());
        modules.add(new DonkeyBoatDupe());
        modules.add(new ExtraItemInfo());
        modules.add(new ArmorHud());
        modules.add(new WebAura());
        modules.add(new Restock());
        modules.add(new FastUse());
        modules.add(new ModuleHUD());
        modules.add(new GUIScale());
        modules.add(new Targets());
        modules.add(new IronDome());
        // modules.add(new );
        // modules.add(new );

        AddonManager.INSTANCE.loadAddons(this);

        System.out.println("Loaded " + modules.size() + " modules");

        loadModules();
        loadKeybinds();
        TargetManager.INSTANCE.loadTargets();
        TargetManager.INSTANCE.loadFriends();
        MacroManager.INSTANCE.loadMacros();
        initTheme();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            saveModules();
            saveKeybinds();
            TargetManager.INSTANCE.saveFriends();
            TargetManager.INSTANCE.saveTargets();
            MacroManager.INSTANCE.saveMacros();
        }));
    }

    private File getFile(String fileName) {
        Path gameDir = MinecraftClient.getInstance().runDirectory.toPath();
        File clientFolder = gameDir.resolve(CLIENT_FOLDER_NAME).toFile();
        if (!clientFolder.exists())
            clientFolder.mkdirs();
        return clientFolder.toPath().resolve(fileName).toFile();
    }

    public void saveKeybinds() {
        File configFile = getFile(KEYBIND_CONFIG_FILE);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(configFile))) {
            for (Module module : modules) {
                if (module.getKey() != -1) {
                    writer.write(module.getName() + ":" + module.getKey());
                    writer.newLine();
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to save keybinds: " + e.getMessage());
        }
    }

    public void loadKeybinds() {
        File configFile = getFile(KEYBIND_CONFIG_FILE);
        if (!configFile.exists())
            return;

        try (BufferedReader reader = new BufferedReader(new FileReader(configFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(":");
                if (parts.length != 2)
                    continue;

                Module module = getModuleByName(parts[0]);
                if (module == null)
                    continue;

                try {
                    int key = Integer.parseInt(parts[1]);
                    module.setKey(key);
                } catch (NumberFormatException ignored) {
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to load keybinds: " + e.getMessage());
        }
    }

    public void saveModules() {
        File configFile = getFile(MODULE_CONFIG_FILE);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(configFile))) {
            for (Module module : modules) {
                writer.write(module.getName() + ":enabled:" + module.isEnabled());
                writer.newLine();
                writer.write(module.getName() + ":extended:" + module.isExtended());
                writer.newLine();
                for (Setting setting : module.getSettings()) {
                    saveSetting(writer, module.getName(), setting);
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to save module configs: " + e.getMessage());
        }
    }

    private void saveSetting(BufferedWriter writer, String moduleName, Setting setting) throws IOException {
        if (setting instanceof NumberSetting) {
            writer.write(moduleName + ":setting:" + setting.getName() + ":" + ((NumberSetting) setting).getValue());
            writer.newLine();
        } else if (setting instanceof BooleanSetting) {
            writer.write(moduleName + ":setting:" + setting.getName() + ":" + ((BooleanSetting) setting).isEnabled());
            writer.newLine();
        } else if (setting instanceof StringSetting) {
            writer.write(moduleName + ":setting:" + setting.getName() + ":" + ((StringSetting) setting).getValue());
            writer.newLine();
        } else if (setting instanceof RadioSetting) {
            writer.write(
                    moduleName + ":setting:" + setting.getName() + ":" + ((RadioSetting) setting).getSelectedOption());
            writer.newLine();
        } else if (setting instanceof CategorySetting) {
            CategorySetting catSetting = (CategorySetting) setting;
            for (Object catItem : catSetting.getSettings()) {
                if (catItem instanceof Setting catItemSetting) {
                    saveSetting(writer, moduleName, catItemSetting);
                }
            }
        }
    }

    public void loadModules() {
        File configFile = getFile(MODULE_CONFIG_FILE);
        if (!configFile.exists())
            return;

        try (BufferedReader reader = new BufferedReader(new FileReader(configFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(":");
                if (parts.length < 3)
                    continue;

                Module module = getModuleByName(parts[0]);
                if (module == null)
                    continue;

                switch (parts[1]) {
                    case "enabled" -> {
                        boolean enabled = Boolean.parseBoolean(parts[2]);
                        if (enabled && !module.isEnabled())
                            module.toggle();
                    }
                    case "extended" -> module.setExtended(Boolean.parseBoolean(parts[2]));
                    case "setting" -> {
                        if (parts.length < 4)
                            continue;
                        Setting setting = findSettingByName(module, parts[2]);
                        if (setting != null) {
                            loadSetting(setting, parts);
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to load module configs: " + e.getMessage());
        }
    }

    private void loadSetting(Setting setting, String[] parts) {
        if (parts.length < 4)
            return;

        if (setting instanceof NumberSetting) {
            try {
                ((NumberSetting) setting).setValue(Float.parseFloat(parts[3]));
            } catch (NumberFormatException ignored) {
            }
        } else if (setting instanceof BooleanSetting) {
            ((BooleanSetting) setting).setEnabled(Boolean.parseBoolean(parts[3]));
        } else if (setting instanceof StringSetting) {
            String value = parts[3];
            for (int i = 4; i < parts.length; i++) {
                value += ":" + parts[i];
            }
            ((StringSetting) setting).setValue(value);
        } else if (setting instanceof RadioSetting) {
            ((RadioSetting) setting).setSelectedOption(parts[3]);
        }
    }

    private Setting findSettingByName(Module module, String name) {
        for (Setting setting : module.getSettings()) {
            if (setting.getName().equals(name)) {
                return setting;
            }
            if (setting instanceof CategorySetting) {
                Setting found = findSettingInCategory((CategorySetting) setting, name);
                if (found != null)
                    return found;
            }
        }
        return null;
    }

    private Setting findSettingInCategory(CategorySetting catSetting, String name) {
        for (Object catItem : catSetting.getSettings()) {
            if (catItem instanceof Setting itemSetting) {
                if (itemSetting.getName().equals(name)) {
                    return itemSetting;
                }
            }
        }
        return null;
    }

    public void saveUiConfig(Map<Category, CategoryPanel> panels) {
        File configFile = getFile(UI_CONFIG_FILE);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(configFile))) {
            for (CategoryPanel panel : panels.values()) {
                writer.write(panel.category.name() + ":" + panel.x + ":" + panel.y + ":" + panel.collapsed);
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("Failed to save UI config: " + e.getMessage());
        }
    }

    public Map<Category, SavedPanelConfig> loadUiConfig() {
        File configFile = getFile(UI_CONFIG_FILE);
        Map<Category, SavedPanelConfig> configMap = new HashMap<>();
        if (!configFile.exists())
            return configMap;

        try (BufferedReader reader = new BufferedReader(new FileReader(configFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(":");
                if (parts.length == 4) {
                    try {
                        Category category = Category.valueOf(parts[0]);
                        int x = Integer.parseInt(parts[1]);
                        int y = Integer.parseInt(parts[2]);
                        boolean collapsed = Boolean.parseBoolean(parts[3]);
                        configMap.put(category, new SavedPanelConfig(x, y, collapsed));
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to load UI config: " + e.getMessage());
        }
        return configMap;
    }

    public void saveTheme(Theme theme) {
        File configFile = getFile(THEME_CONFIG_FILE);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(configFile))) {
            writer.write(theme.name());
            writer.newLine();
        } catch (IOException e) {
            System.err.println("Failed to save theme: " + e.getMessage());
        }
    }

    public Theme loadTheme() {
        File configFile = getFile(THEME_CONFIG_FILE);
        if (!configFile.exists())
            return Theme.IO;

        try (BufferedReader reader = new BufferedReader(new FileReader(configFile))) {
            String line = reader.readLine();
            if (line != null) {
                try {
                    return Theme.valueOf(line);
                } catch (IllegalArgumentException ignored) {
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to load theme: " + e.getMessage());
        }
        return Theme.IO;
    }

    public File getFriendsFile() {
        return getFile(FRIENDS_CONFIG_FILE);
    }

    public File getTargetsFile() {
        return getFile(TARGETS_CONFIG_FILE);
    }

    public List<Module> getModules() {
        return modules;
    }

    public List<Module> getModulesByCategory(Category category) {
        return modules.stream().filter(m -> m.getCategory() == category).collect(Collectors.toList());
    }

    public Module getModuleByName(String name) {
        for (Module module : modules)
            if (module.getName().equalsIgnoreCase(name))
                return module;
        return null;
    }

    public void onUpdate() {
        for (Module module : modules)
            if (module.isEnabled())
                module.onUpdate();
    }

    public <T extends Module> T getModule(Class<T> clazz) {
        for (Module m : modules)
            if (clazz.isInstance(m))
                return clazz.cast(m);
        return null;
    }

    public void addModule(Module module) {
        modules.add(module);
    }

    public void removeModule(Module module) {
        modules.remove(module);
    }

    public File getConfigDir() {
        Path gameDir = MinecraftClient.getInstance().runDirectory.toPath();
        File clientFolder = gameDir.resolve(CLIENT_FOLDER_NAME).toFile();
        if (!clientFolder.exists())
            clientFolder.mkdirs();
        return clientFolder;
    }

    public void onKeyPress(int key) {
        long window = MinecraftClient.getInstance().getWindow().getHandle();

        for (Module module : modules) {
            if (module instanceof Macro macro) {
                if (macro.getKeyCodes()[macro.getKeyCodes().length - 1] == key && macro.matchesKeys(window)) {
                    if (isExactKeyMatch(window, macro.getKeyCodes())) {
                        macro.toggle();
                        saveModules();
                        return;
                    }
                }
            } else if (module.getKey() == key) {
                boolean isPartOfMacro = false;
                for (Module m : modules) {
                    if (m instanceof Macro macro2 && macro2.matchesKeys(window)) {
                        isPartOfMacro = true;
                        break;
                    }
                }

                if (!isPartOfMacro) {
                    module.toggle();
                    saveModules();

                    CommandManager.INSTANCE.sendMessage(
                            "§a" + module.getName() + " is now " +
                                    (module.isEnabled() ? "enabled" : "§cdisabled"));
                }
            }
        }
    }

    private boolean isExactKeyMatch(long window, int[] requiredKeys) {
        int[] allModifiers = {
                GLFW.GLFW_KEY_LEFT_SHIFT, GLFW.GLFW_KEY_RIGHT_SHIFT,
                GLFW.GLFW_KEY_LEFT_CONTROL, GLFW.GLFW_KEY_RIGHT_CONTROL,
                GLFW.GLFW_KEY_LEFT_ALT, GLFW.GLFW_KEY_RIGHT_ALT
        };

        for (int modifier : allModifiers) {
            boolean isRequired = false;
            for (int required : requiredKeys) {
                if (required == modifier) {
                    isRequired = true;
                    break;
                }
            }

            boolean isPressed = GLFW.glfwGetKey(window, modifier) == GLFW.GLFW_PRESS;

            if (isPressed && !isRequired) {
                return false;
            }
        }

        return true;
    }
}

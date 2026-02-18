package io.client.clickgui;

import io.client.clickgui.screens.BasicClickGuiScreen;
import io.client.clickgui.screens.ClickGuiScreen;
import io.client.clickgui.screens.FutureClickGuiScreen;
import io.client.clickgui.screens.ModernClickGuiScreen;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.client.gui.screen.Screen;

public final class ClickGuiModeRegistry {
    private static final String DEFAULT_MODE = "IO";
    private static final Map<String, ModeEntry> MODES = new LinkedHashMap<>();

    static {
        register("IO", ClickGuiScreen.class, ClickGuiScreen::new);
        register("Future", FutureClickGuiScreen.class, FutureClickGuiScreen::new);
        register("Modern", ModernClickGuiScreen.class, ModernClickGuiScreen::new);
        register("Basic", BasicClickGuiScreen.class, BasicClickGuiScreen::new);
    }

    private ClickGuiModeRegistry() {
    }

    public static void register(String displayName, Class<? extends Screen> screenType, Supplier<Screen> factory) {
        String key = normalize(displayName);
        MODES.put(key, new ModeEntry(displayName, screenType, factory));
    }

    public static List<String> getModeNames() {
        List<String> names = new ArrayList<>();
        for (ModeEntry entry : MODES.values()) {
            names.add(entry.displayName);
        }
        return names;
    }

    public static String getDefaultModeName() {
        return DEFAULT_MODE;
    }

    public static Screen createScreen(String selectedMode) {
        ModeEntry entry = MODES.get(normalize(selectedMode));
        if (entry == null) {
            entry = MODES.get(normalize(DEFAULT_MODE));
        }
        return entry != null ? entry.factory.get() : new ClickGuiScreen();
    }

    public static boolean isClickGuiScreen(Screen screen) {
        if (screen == null) {
            return false;
        }
        for (ModeEntry entry : MODES.values()) {
            if (entry.screenType.isInstance(screen)) {
                return true;
            }
        }
        return false;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static final class ModeEntry {
        private final String displayName;
        private final Class<? extends Screen> screenType;
        private final Supplier<Screen> factory;

        private ModeEntry(String displayName, Class<? extends Screen> screenType, Supplier<Screen> factory) {
            this.displayName = displayName;
            this.screenType = screenType;
            this.factory = factory;
        }
    }
}



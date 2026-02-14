package io.client;

import io.client.modules.Macro; // Import the correct Macro class
import java.util.HashMap;
import java.util.Map;
import java.util.Collection; // Import Collection for getMacros

public class MacroManager {
    public static final MacroManager INSTANCE = new MacroManager();

    private final Map<String, Macro> macros = new HashMap<>();

    private MacroManager() {
        // Private constructor for singleton
    }

    public void addMacro(String name, String command, int keyCode) {
        macros.put(name, new Macro(name, command, keyCode));
    }

    public Macro getMacro(String name) {
        return macros.get(name);
    }

    public Collection<Macro> getMacros() { // Return Collection instead of Map
        return macros.values();
    }

    public void removeMacro(String name) {
        macros.remove(name);
    }
}
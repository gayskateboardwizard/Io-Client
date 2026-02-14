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
import io.client.modules.Macro;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class MacroManager {
    public static final MacroManager INSTANCE = new MacroManager();
    private final List<Macro> macros = new ArrayList<>();

    private MacroManager() {}

    public void addMacro(String name, String command, int key) {
        Macro macro = new Macro(name, command, key);
        macros.add(macro);
        ModuleManager.INSTANCE.addModule(macro);
        saveMacros();
    }

    public void removeMacro(String name) {
        macros.removeIf(macro -> {
            if (macro.getName().equalsIgnoreCase(name)) {
                ModuleManager.INSTANCE.removeModule(macro);
                return true;
            }
            return false;
        });
        saveMacros();
    }

    public Macro getMacro(String name) {
        return macros.stream()
                .filter(m -> m.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    public List<Macro> getMacros() {
        return new ArrayList<>(macros);
    }

    public void saveMacros() {
        File configFile = new File(ModuleManager.INSTANCE.getConfigDir(), "macros.txt");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(configFile))) {
            for (Macro macro : macros) {
                writer.write(macro.getName() + ":" + macro.getCommand() + ":" + macro.getKey());
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("Failed to save macros: " + e.getMessage());
        }
    }

    public void loadMacros() {
        File configFile = new File(ModuleManager.INSTANCE.getConfigDir(), "macros.txt");
        if (!configFile.exists()) {
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(configFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(":", 3);
                if (parts.length == 3) {
                    String name = parts[0];
                    String command = parts[1];
                    int key = Integer.parseInt(parts[2]);
                    addMacro(name, command, key);
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to load macros: " + e.getMessage());
        }
    }
}

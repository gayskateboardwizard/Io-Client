package io.client.managers;

import io.client.modules.macros.Macro;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class MacroManager {
    public static final MacroManager INSTANCE = new MacroManager();
    private final List<Macro> macros = new ArrayList<>();

    private MacroManager() {}

    public void addMacro(String name, String command, int[] keys) {
        Macro macro = new Macro(name, command, keys);
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
                StringBuilder keysStr = new StringBuilder();
                int[] keys = macro.getKeyCodes();
                for (int i = 0; i < keys.length; i++) {
                    if (i > 0) keysStr.append(",");
                    keysStr.append(keys[i]);
                }
                writer.write(macro.getName() + ":" + macro.getCommand() + ":" + keysStr);
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

        macros.clear();

        try (BufferedReader reader = new BufferedReader(new FileReader(configFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(":", 3);
                if (parts.length == 3) {
                    String name = parts[0];
                    String command = parts[1];
                    String[] keyStrs = parts[2].split(",");
                    int[] keys = new int[keyStrs.length];
                    for (int i = 0; i < keyStrs.length; i++) {
                        keys[i] = Integer.parseInt(keyStrs[i]);
                    }

                    Macro macro = new Macro(name, command, keys);
                    macros.add(macro);
                    ModuleManager.INSTANCE.addModule(macro);
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to load macros: " + e.getMessage());
        }
    }
}





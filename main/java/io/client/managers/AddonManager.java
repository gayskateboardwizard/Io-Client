package io.client.managers;

import io.client.addons.AddonContext;
import io.client.addons.IoAddon;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.stream.Stream;

public class AddonManager {
    public static final AddonManager INSTANCE = new AddonManager();

    private final List<URLClassLoader> classLoaders = new ArrayList<>();
    private boolean loaded;

    private AddonManager() {
    }

    public void loadAddons(ModuleManager moduleManager) {
        if (loaded) return;
        loaded = true;

        Path addonsDir = moduleManager.getConfigDir().toPath().resolve("addons");
        try {
            Files.createDirectories(addonsDir);
        } catch (IOException e) {
            System.err.println("Failed to create addons directory: " + e.getMessage());
            return;
        }

        try (Stream<Path> jarStream = Files.list(addonsDir)) {
            jarStream
                    .filter(path -> path.getFileName().toString().toLowerCase().endsWith(".jar"))
                    .forEach(path -> loadAddonJar(path, moduleManager));
        } catch (IOException e) {
            System.err.println("Failed to scan addons: " + e.getMessage());
        }
    }

    private void loadAddonJar(Path jarPath, ModuleManager moduleManager) {
        try {
            URLClassLoader classLoader = new URLClassLoader(
                    new URL[]{jarPath.toUri().toURL()},
                    AddonManager.class.getClassLoader());
            classLoaders.add(classLoader);

            ServiceLoader<IoAddon> loader = ServiceLoader.load(IoAddon.class, classLoader);
            AddonContext context = new AddonContext(moduleManager);

            for (IoAddon addon : loader) {
                try {
                    addon.onInitialize(context);
                    System.out.println("Loaded addon: " + addon.getName());
                } catch (Throwable t) {
                    System.err.println("Addon failed to initialize (" + jarPath.getFileName() + "): " + t.getMessage());
                }
            }
        } catch (Throwable t) {
            System.err.println("Failed to load addon jar " + jarPath.getFileName() + ": " + t.getMessage());
        }
    }
}

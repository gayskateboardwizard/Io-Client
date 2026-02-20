package io.client.addons;

import io.client.managers.ModuleManager;
import io.client.modules.templates.Module;

public class AddonContext {
    private final ModuleManager moduleManager;

    public AddonContext(ModuleManager moduleManager) {
        this.moduleManager = moduleManager;
    }

    public void registerModule(Module module) {
        moduleManager.addModule(module);
    }
}

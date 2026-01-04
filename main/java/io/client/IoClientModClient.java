package io.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class IoClientModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ModuleManager.INSTANCE.init();
        IoClientEventHandler.getInstance().registerEvents();
    }

}
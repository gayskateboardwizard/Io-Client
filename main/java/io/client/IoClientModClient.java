package io.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import io.client.discord.DiscordRpcManager;
import io.client.managers.ModuleManager;
import io.client.managers.PacketManager;
import io.client.utils.render.postprocess.PostProcessShaders;

@Environment(EnvType.CLIENT)
public class IoClientModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        PacketManager.INSTANCE.init();
        ModuleManager.INSTANCE.init();
        IoClientEventHandler.getInstance().registerEvents();
        PostProcessShaders.init();
        DiscordRpcManager.init();
        Runtime.getRuntime().addShutdownHook(new Thread(DiscordRpcManager::shutdown));
    }
}


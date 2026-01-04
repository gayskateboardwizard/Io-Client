package io.client;

import net.fabricmc.api.ModInitializer;

public class IoClientMod implements ModInitializer {

    @Override
    public void onInitialize() {
        // This is the common entrypoint - runs on both client and server
        // Since this is a client-only mod, we don't need to do anything here
        // All client-specific initialization happens in IoClientModClient

        System.out.println("Io Client Mod - Main entrypoint loaded");
    }
}
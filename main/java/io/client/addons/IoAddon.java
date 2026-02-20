package io.client.addons;

public interface IoAddon {
    String getName();

    default void onInitialize(AddonContext context) {
    }
}

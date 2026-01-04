package io.client;

import java.util.HashSet;
import java.util.Set;

public class KeyManager {
    public static final KeyManager INSTANCE = new KeyManager();

    private final Set<Integer> pressedKeys = new HashSet<>();

    private KeyManager() {
    }

    public boolean isKeyPressed(int key) {
        return pressedKeys.contains(key);
    }

    public void addKey(int key) {
        pressedKeys.add(key);
    }

    public void removeKey(int key) {
        pressedKeys.remove(key);
    }

    public void clear() {
        pressedKeys.clear();
    }
}
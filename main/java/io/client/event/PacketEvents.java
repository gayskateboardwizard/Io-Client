package io.client.event;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public final class PacketEvents {
    private static final Set<ReceiveListener> RECEIVE_LISTENERS = new CopyOnWriteArraySet<>();

    private PacketEvents() {
    }

    public static void registerReceive(ReceiveListener listener) {
        RECEIVE_LISTENERS.add(listener);
    }

    public static void unregisterReceive(ReceiveListener listener) {
        RECEIVE_LISTENERS.remove(listener);
    }

    public static void fireReceive(Object packet) {
        for (ReceiveListener listener : RECEIVE_LISTENERS) {
            listener.onReceive(packet);
        }
    }

    public interface ReceiveListener {
        void onReceive(Object packet);
    }
}


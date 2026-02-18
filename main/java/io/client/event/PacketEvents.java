package io.client.event;

import io.client.managers.PacketManager;
import net.minecraft.network.packet.Packet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

@Deprecated(forRemoval = false)
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
        if (packet instanceof Packet<?> typedPacket) {
            PacketManager.INSTANCE.fireReceive(typedPacket);
        }

        for (ReceiveListener listener : RECEIVE_LISTENERS) {
            listener.onReceive(packet);
        }
    }

    public interface ReceiveListener {
        void onReceive(Object packet);
    }
}




package io.client.managers;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.Packet;

public final class PacketManager {
    public static final PacketManager INSTANCE = new PacketManager();

    private final Set<ReceiveListener> receiveListeners = new CopyOnWriteArraySet<>();
    private final Set<SendListener> sendListeners = new CopyOnWriteArraySet<>();

    private PacketManager() {
    }

    public void init() {
        // Reserved for future packet bootstrap logic.
    }

    public boolean send(Packet<?> packet) {
        if (packet == null) {
            return false;
        }

        ClientPlayNetworkHandler networkHandler = MinecraftClient.getInstance().getNetworkHandler();
        if (networkHandler == null) {
            return false;
        }

        fireSend(packet);
        networkHandler.sendPacket(packet);
        return true;
    }

    public void registerReceive(ReceiveListener listener) {
        if (listener != null) {
            receiveListeners.add(listener);
        }
    }

    public void unregisterReceive(ReceiveListener listener) {
        if (listener != null) {
            receiveListeners.remove(listener);
        }
    }

    public void registerSend(SendListener listener) {
        if (listener != null) {
            sendListeners.add(listener);
        }
    }

    public void unregisterSend(SendListener listener) {
        if (listener != null) {
            sendListeners.remove(listener);
        }
    }

    public void fireReceive(Packet<?> packet) {
        if (packet == null) {
            return;
        }

        for (ReceiveListener listener : receiveListeners) {
            listener.onReceive(packet);
        }
    }

    public void fireSend(Packet<?> packet) {
        if (packet == null) {
            return;
        }

        for (SendListener listener : sendListeners) {
            listener.onSend(packet);
        }
    }

    public interface ReceiveListener {
        void onReceive(Packet<?> packet);
    }

    public interface SendListener {
        void onSend(Packet<?> packet);
    }
}




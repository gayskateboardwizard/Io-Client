package io.client.mixin;

import io.client.managers.PacketManager;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDeltaUpdateS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {
    @Inject(method = "onChunkData", at = @At("TAIL"))
    private void io_client$onChunkData(ChunkDataS2CPacket packet, CallbackInfo ci) {
        PacketManager.INSTANCE.fireReceive(packet);
    }

    @Inject(method = "onChunkDeltaUpdate", at = @At("TAIL"))
    private void io_client$onChunkDeltaUpdate(ChunkDeltaUpdateS2CPacket packet, CallbackInfo ci) {
        PacketManager.INSTANCE.fireReceive(packet);
    }

    @Inject(method = "onBlockUpdate", at = @At("TAIL"))
    private void io_client$onBlockUpdate(BlockUpdateS2CPacket packet, CallbackInfo ci) {
        PacketManager.INSTANCE.fireReceive(packet);
    }
}




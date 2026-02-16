package io.client.mixininterface;

import net.minecraft.client.gl.Framebuffer;

public interface IWorldRenderer {
    void io_client$pushEntityOutlineFramebuffer(Framebuffer framebuffer);

    void io_client$popEntityOutlineFramebuffer();
}




package io.client.utils.render.postprocess;

import com.mojang.blaze3d.systems.RenderSystem;
import io.client.mixininterface.IWorldRenderer;

import java.util.OptionalInt;

import static net.minecraft.client.MinecraftClient.getInstance;

public abstract class EntityShader extends PostProcessShader {
    @Override
    public boolean beginRender() {
        if (framebuffer == null) return false;
        if (super.beginRender()) {
            RenderSystem.getDevice()
                    .createCommandEncoder()
                    .createRenderPass(() -> "IO Client EntityShader", framebuffer.getColorAttachmentView(), OptionalInt.of(0))
                    .close();
            return true;
        }
        return false;
    }

    @Override
    protected void preDraw() {
        ((IWorldRenderer) getInstance().worldRenderer).io_client$pushEntityOutlineFramebuffer(framebuffer);
    }

    @Override
    protected void postDraw() {
        ((IWorldRenderer) getInstance().worldRenderer).io_client$popEntityOutlineFramebuffer();
    }

    public void endRender() {
        endRender(() -> vertexConsumerProvider.draw());
    }
}




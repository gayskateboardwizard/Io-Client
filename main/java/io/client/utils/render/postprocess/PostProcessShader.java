package io.client.utils.render.postprocess;

import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import io.client.renderer.FullScreenRenderer;
import io.client.renderer.MeshRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.DynamicUniformStorage;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.render.OutlineVertexConsumerProvider;
import net.minecraft.entity.Entity;

import java.nio.ByteBuffer;

import static org.lwjgl.glfw.GLFW.glfwGetTime;

public abstract class PostProcessShader {
    protected final MinecraftClient mc = MinecraftClient.getInstance();
    public OutlineVertexConsumerProvider vertexConsumerProvider;
    public Framebuffer framebuffer;
    protected RenderPipeline pipeline;

    public void init(RenderPipeline pipeline) {
        if (mc == null) return;
        vertexConsumerProvider = new OutlineVertexConsumerProvider(mc.getBufferBuilders().getEntityVertexConsumers());
        framebuffer = new SimpleFramebuffer(
                "io_client-post-process",
                mc.getWindow().getFramebufferWidth(),
                mc.getWindow().getFramebufferHeight(),
                true
        );
        this.pipeline = pipeline;
    }

    protected abstract boolean shouldDraw();

    public abstract boolean shouldDraw(Entity entity);

    protected void preDraw() {
    }

    protected void postDraw() {
    }

    protected abstract void setupPass(MeshRenderer renderer);

    public boolean beginRender() {
        return shouldDraw();
    }

    public void endRender(Runnable draw) {
        if (!shouldDraw() || framebuffer == null || mc == null) return;

        preDraw();
        draw.run();
        postDraw();

        MeshRenderer renderer = MeshRenderer.begin()
                .attachments(mc.getFramebuffer())
                .pipeline(pipeline)
                .mesh(FullScreenRenderer.mesh)
                .uniform("PostData", getUniformStorage().write(new UniformData(
                        mc.getWindow().getFramebufferWidth(),
                        mc.getWindow().getFramebufferHeight(),
                        (float) glfwGetTime()
                )))
                .sampler("u_Texture", framebuffer.getColorAttachmentView());

        setupPass(renderer);
        renderer.end();
    }

    public void onResized(int width, int height) {
        if (framebuffer != null) {
            framebuffer.resize(width, height);
        }
    }

    private static final int UNIFORM_SIZE = new Std140SizeCalculator()
            .putVec2()
            .putFloat()
            .get();

    private static DynamicUniformStorage<UniformData> UNIFORM_STORAGE;

    private static DynamicUniformStorage<UniformData> getUniformStorage() {
        if (UNIFORM_STORAGE == null) {
            UNIFORM_STORAGE = new DynamicUniformStorage<>("IO Client - Post UBO", UNIFORM_SIZE, 16);
        }
        return UNIFORM_STORAGE;
    }

    public static void flipFrame() {
        if (UNIFORM_STORAGE != null) {
            UNIFORM_STORAGE.clear();
        }
    }

    private record UniformData(float sizeX, float sizeY, float time) implements DynamicUniformStorage.Uploadable {
        @Override
        public void write(ByteBuffer buffer) {
            Std140Builder.intoBuffer(buffer)
                    .putVec2(sizeX, sizeY)
                    .putFloat(time);
        }
    }
}






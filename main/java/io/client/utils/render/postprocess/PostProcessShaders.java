package io.client.utils.render.postprocess;

import io.client.renderer.FullScreenRenderer;
import io.client.renderer.IoRenderPipelines;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;

public class PostProcessShaders {
    public static EntityShader CHAMS;
    public static boolean rendering;
    private static boolean initialized;
    private static boolean pipelinesPrecompiled;

    private PostProcessShaders() {
    }

    public static void init() {
        if (initialized)
            return;
        initialized = true;

        FullScreenRenderer.init();
        CHAMS = new ChamsShader();
    }

    public static void beginRender() {
        if (CHAMS == null)
            return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!pipelinesPrecompiled && mc != null && mc.world != null) {
            // Precompile only after client resources are fully available.
            try {
                IoRenderPipelines.precompile();
                pipelinesPrecompiled = true;
            } catch (Throwable t) {
                System.err.println("Failed to precompile IO Client render pipelines: " + t.getMessage());
            }
        }
        if (CHAMS.framebuffer == null && mc != null && mc.getWindow() != null) {
            CHAMS.init(IoRenderPipelines.POST_OUTLINE);
        }
        CHAMS.beginRender();
    }

    public static void endRender() {
        if (CHAMS == null)
            return;
        CHAMS.endRender();
        PostProcessShader.flipFrame();
        ChamsShader.flipFrame();
    }

    public static void onResized(int width, int height) {
        if (CHAMS != null)
            CHAMS.onResized(width, height);
    }

    public static boolean isCustom(VertexConsumerProvider vcp) {
        return CHAMS != null && vcp == CHAMS.vertexConsumerProvider;
    }
}

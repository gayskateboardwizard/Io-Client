package io.client.renderer;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.UniformType;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public abstract class IoRenderPipelines {
    private static final List<RenderPipeline> PIPELINES = new ArrayList<>();

    public static final RenderPipeline POST_OUTLINE = add(RenderPipeline.builder()
            .withLocation(identifier("pipeline/post/outline"))
            .withVertexFormat(MeteorVertexFormats.POS2, VertexFormat.DrawMode.TRIANGLES)
            .withVertexShader(identifier("shaders/post-process/base.vert"))
            .withFragmentShader(identifier("shaders/post-process/outline.frag"))
            .withSampler("u_Texture")
            .withUniform("PostData", UniformType.UNIFORM_BUFFER)
            .withUniform("OutlineData", UniformType.UNIFORM_BUFFER)
            .withUniform("MeshData", UniformType.UNIFORM_BUFFER)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withCull(false)
            .build());

    private static RenderPipeline add(RenderPipeline pipeline) {
        PIPELINES.add(pipeline);
        return pipeline;
    }

    public static void precompile() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return;
        ResourceManager resources = client.getResourceManager();
        GpuDevice device = RenderSystem.getDevice();

        for (RenderPipeline pipeline : PIPELINES) {
            device.precompilePipeline(pipeline, (id, shaderType) -> {
                var resource = resources.getResource(id).orElseThrow();
                try (var in = resource.getInputStream()) {
                    return IOUtils.toString(in, StandardCharsets.UTF_8);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    private static Identifier identifier(String path) {
        return Identifier.of("io_client", path);
    }

    private IoRenderPipelines() {
    }
}






package io.client.renderer;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.ColorHelper;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.OptionalDouble;
import java.util.OptionalInt;

public class MeshRenderer {
    private static final MeshRenderer INSTANCE = new MeshRenderer();
    private static boolean taken;

    private GpuTextureView colorAttachment;
    private GpuTextureView depthAttachment;
    private Integer clearColor;
    private RenderPipeline pipeline;
    private MeshBuilder mesh;
    private Matrix4f matrix;
    private final HashMap<String, GpuBufferSlice> uniforms = new HashMap<>();
    private final HashMap<String, GpuTextureView> samplers = new HashMap<>();

    private MeshRenderer() {
    }

    public static MeshRenderer begin() {
        if (taken) throw new IllegalStateException("Previous MeshRenderer was not ended.");
        taken = true;
        return INSTANCE;
    }

    public MeshRenderer attachments(GpuTextureView color, GpuTextureView depth) {
        colorAttachment = color;
        depthAttachment = depth;
        return this;
    }

    public MeshRenderer attachments(Framebuffer framebuffer) {
        colorAttachment = framebuffer.getColorAttachmentView();
        depthAttachment = framebuffer.getDepthAttachmentView();
        return this;
    }

    public MeshRenderer clearColor(int argb) {
        clearColor = argb;
        return this;
    }

    public MeshRenderer pipeline(RenderPipeline pipeline) {
        this.pipeline = pipeline;
        return this;
    }

    public MeshRenderer mesh(MeshBuilder mesh) {
        this.mesh = mesh;
        return this;
    }

    public MeshRenderer mesh(MeshBuilder mesh, Matrix4f matrix) {
        this.mesh = mesh;
        this.matrix = matrix;
        return this;
    }

    public MeshRenderer mesh(MeshBuilder mesh, MatrixStack matrices) {
        this.mesh = mesh;
        this.matrix = matrices.peek().getPositionMatrix();
        return this;
    }

    public MeshRenderer uniform(String name, GpuBufferSlice slice) {
        uniforms.put(name, slice);
        return this;
    }

    public MeshRenderer sampler(String name, GpuTextureView view) {
        if (name != null && view != null) {
            samplers.put(name, view);
        }
        return this;
    }

    public void end() {
        if (mesh != null && mesh.isBuilding()) mesh.end();

        if (mesh != null && mesh.getIndicesCount() > 0) {
            if (matrix != null) {
                RenderSystem.getModelViewStack().pushMatrix();
                RenderSystem.getModelViewStack().mul(matrix);
            }

            GpuBuffer vertexBuffer = mesh.getVertexBuffer();
            GpuBuffer indexBuffer = mesh.getIndexBuffer();

            OptionalInt clear = clearColor != null ? OptionalInt.of(clearColor) : OptionalInt.empty();
            // Full-screen post pass in clip-space; identity projection is sufficient.
            GpuBufferSlice meshData = MeshUniforms.write(new Matrix4f().identity(), RenderSystem.getModelViewStack());

            RenderPass pass = (depthAttachment != null && pipeline.wantsDepthTexture())
                    ? RenderSystem.getDevice().createCommandEncoder().createRenderPass(
                    () -> "IO Client MeshRenderer",
                    colorAttachment,
                    clear,
                    depthAttachment,
                    OptionalDouble.empty()
            )
                    : RenderSystem.getDevice().createCommandEncoder().createRenderPass(
                    () -> "IO Client MeshRenderer",
                    colorAttachment,
                    clear
            );

            pass.setPipeline(pipeline);
            pass.setUniform("MeshData", meshData);

            for (var entry : uniforms.entrySet()) {
                pass.setUniform(entry.getKey(), entry.getValue());
            }
            for (var entry : samplers.entrySet()) {
                pass.bindSampler(entry.getKey(), entry.getValue());
            }

            pass.setVertexBuffer(0, vertexBuffer);
            pass.setIndexBuffer(indexBuffer, VertexFormat.IndexType.INT);
            pass.drawIndexed(0, 0, mesh.getIndicesCount(), 1);
            pass.close();

            if (matrix != null) {
                RenderSystem.getModelViewStack().popMatrix();
            }
        }

        colorAttachment = null;
        depthAttachment = null;
        clearColor = null;
        pipeline = null;
        mesh = null;
        matrix = null;
        uniforms.clear();
        samplers.clear();
        taken = false;
    }

    public static int argb(int r, int g, int b, int a) {
        return ColorHelper.getArgb(a, r, g, b);
    }
}





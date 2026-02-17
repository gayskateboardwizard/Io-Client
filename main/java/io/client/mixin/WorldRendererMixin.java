package io.client.mixin;

import io.client.ModuleManager;
import io.client.mixininterface.IWorldRenderer;
import io.client.modules.render.Chams;
import io.client.utils.render.postprocess.EntityShader;
import io.client.utils.render.postprocess.PostProcessShaders;
import it.unimi.dsi.fastutil.Stack;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.render.DefaultFramebufferSet;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.Handle;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public abstract class WorldRendererMixin implements IWorldRenderer {
    @Shadow
    protected abstract void renderEntity(Entity entity, double cameraX, double cameraY, double cameraZ, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers);

    @Shadow
    private Framebuffer entityOutlineFramebuffer;

    @Shadow
    @Final
    private DefaultFramebufferSet framebufferSet;

    @Unique
    private Stack<Framebuffer> io_client$framebufferStack;

    @Unique
    private Stack<Handle<Framebuffer>> io_client$framebufferHandleStack;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void io_client$init(CallbackInfo ci) {
        io_client$framebufferStack = new ObjectArrayList<>();
        io_client$framebufferHandleStack = new ObjectArrayList<>();
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void io_client$onRenderHead(CallbackInfo ci) {
        PostProcessShaders.beginRender();
    }

    @Inject(method = "method_62214", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/OutlineVertexConsumerProvider;draw()V"))
    private void io_client$onRender(CallbackInfo ci) {
        PostProcessShaders.endRender();
    }

    @Inject(method = "renderEntity", at = @At("HEAD"))
    private void io_client$renderChams(
            Entity entity,
            double cameraX,
            double cameraY,
            double cameraZ,
            float tickDelta,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            CallbackInfo ci
    ) {
        if (entity == null || PostProcessShaders.CHAMS == null) return;
        if (PostProcessShaders.isCustom(vertexConsumers)) return;

        Chams chams = ModuleManager.INSTANCE.getModule(Chams.class);
        if (chams == null || !chams.shouldRender(entity)) return;
        if (!PostProcessShaders.CHAMS.shouldDraw(entity)) return;

        int[] color = chams.getColor(entity);
        draw(entity, cameraX, cameraY, cameraZ, tickDelta, vertexConsumers, matrices, PostProcessShaders.CHAMS, color);
    }

    @Unique
    private void draw(
            Entity entity,
            double cameraX,
            double cameraY,
            double cameraZ,
            float tickDelta,
            VertexConsumerProvider vertexConsumers,
            MatrixStack matrices,
            EntityShader shader,
            int[] rgba
    ) {
        if (shader == null || shader.framebuffer == null || rgba == null || rgba.length < 4) return;
        if (!shader.shouldDraw(entity) || PostProcessShaders.isCustom(vertexConsumers)) return;

        io_client$pushEntityOutlineFramebuffer(shader.framebuffer);
        PostProcessShaders.rendering = true;
        try {
            shader.vertexConsumerProvider.setColor(rgba[0], rgba[1], rgba[2], rgba[3]);
            renderEntity(entity, cameraX, cameraY, cameraZ, tickDelta, matrices, shader.vertexConsumerProvider);
        } finally {
            PostProcessShaders.rendering = false;
            io_client$popEntityOutlineFramebuffer();
        }
    }

    @Override
    public void io_client$pushEntityOutlineFramebuffer(Framebuffer framebuffer) {
        io_client$framebufferStack.push(this.entityOutlineFramebuffer);
        this.entityOutlineFramebuffer = framebuffer;

        io_client$framebufferHandleStack.push(this.framebufferSet.entityOutlineFramebuffer);
        this.framebufferSet.entityOutlineFramebuffer = () -> framebuffer;
        }

    @Override
    public void io_client$popEntityOutlineFramebuffer() {
        this.entityOutlineFramebuffer = io_client$framebufferStack.pop();
        this.framebufferSet.entityOutlineFramebuffer = io_client$framebufferHandleStack.pop();
    }
}



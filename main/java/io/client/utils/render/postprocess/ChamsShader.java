package io.client.utils.render.postprocess;

import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import io.client.ModuleManager;
import io.client.modules.Chams;
import io.client.renderer.MeshRenderer;
import net.minecraft.client.gl.DynamicUniformStorage;
import net.minecraft.entity.Entity;

import java.nio.ByteBuffer;

public class ChamsShader extends EntityShader {
    private static Chams chams;

    @Override
    protected void setupPass(MeshRenderer renderer) {
        if (chams == null) chams = ModuleManager.INSTANCE.getModule(Chams.class);
        if (chams == null) return;

        renderer.uniform("OutlineData", getUniformStorage().write(new OutlineUniformData(
                2,
                0.0f,
                0,
                1.0f
        )));
    }

    @Override
    protected boolean shouldDraw() {
        if (chams == null) chams = ModuleManager.INSTANCE.getModule(Chams.class);
        return chams != null && chams.isEnabled();
    }

    @Override
    public boolean shouldDraw(Entity entity) {
        if (!shouldDraw()) return false;
        return chams.shouldRender(entity);
    }

    private static final int UNIFORM_SIZE = new Std140SizeCalculator()
            .putInt()
            .putFloat()
            .putInt()
            .putFloat()
            .get();

    private static DynamicUniformStorage<OutlineUniformData> UNIFORM_STORAGE;

    private static DynamicUniformStorage<OutlineUniformData> getUniformStorage() {
        if (UNIFORM_STORAGE == null) {
            UNIFORM_STORAGE = new DynamicUniformStorage<>("IO Client - Outline UBO", UNIFORM_SIZE, 16);
        }
        return UNIFORM_STORAGE;
    }

    public static void flipFrame() {
        if (UNIFORM_STORAGE != null) {
            UNIFORM_STORAGE.clear();
        }
    }

    private record OutlineUniformData(int width, float fillOpacity, int shapeMode,
                                      float glowMultiplier) implements DynamicUniformStorage.Uploadable {
        @Override
        public void write(ByteBuffer buffer) {
            Std140Builder.intoBuffer(buffer)
                    .putInt(width)
                    .putFloat(fillOpacity)
                    .putInt(shapeMode)
                    .putFloat(glowMultiplier);
        }
    }
}




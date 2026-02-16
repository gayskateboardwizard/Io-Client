package io.client.renderer;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import net.minecraft.client.gl.DynamicUniformStorage;
import org.joml.Matrix4f;

import java.nio.ByteBuffer;

public class MeshUniforms {
    public static final int SIZE = new Std140SizeCalculator()
            .putMat4f()
            .putMat4f()
            .get();

    private static final Data DATA = new Data();
    private static DynamicUniformStorage<Data> STORAGE;

    private static DynamicUniformStorage<Data> getStorage() {
        if (STORAGE == null) {
            STORAGE = new DynamicUniformStorage<>("IO Client - Mesh UBO", SIZE, 16);
        }
        return STORAGE;
    }

    public static void flipFrame() {
        if (STORAGE != null) {
            STORAGE.clear();
        }
    }

    public static GpuBufferSlice write(Matrix4f projection, Matrix4f modelView) {
        DATA.projection = projection;
        DATA.modelView = modelView;
        return getStorage().write(DATA);
    }

    private static final class Data implements DynamicUniformStorage.Uploadable {
        private Matrix4f projection;
        private Matrix4f modelView;

        @Override
        public void write(ByteBuffer buffer) {
            Std140Builder.intoBuffer(buffer)
                    .putMat4f(projection)
                    .putMat4f(modelView);
        }

        @Override
        public boolean equals(Object o) {
            return false;
        }
    }
}




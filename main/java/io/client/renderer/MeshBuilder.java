package io.client.renderer;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.fabricmc.loader.api.FabricLoader;
import org.lwjgl.BufferUtils;

import java.nio.ByteBuffer;

import static org.lwjgl.system.MemoryUtil.memAddress0;
import static org.lwjgl.system.MemoryUtil.memCopy;
import static org.lwjgl.system.MemoryUtil.memPutFloat;
import static org.lwjgl.system.MemoryUtil.memPutInt;

public class MeshBuilder {
    private static final boolean DEBUG = FabricLoader.getInstance().isDevelopmentEnvironment() || Boolean.getBoolean("io_client.render.debug");

    private final VertexFormat format;
    private final int primitiveVerticesSize;
    private final int primitiveIndicesCount;

    private ByteBuffer vertices;
    private long verticesPointerStart;
    private long verticesPointer;

    private ByteBuffer indices;
    private long indicesPointer;

    private int vertexI;
    private int indicesCount;
    private boolean building;

    public MeshBuilder(RenderPipeline pipeline) {
        this(pipeline.getVertexFormat(), pipeline.getVertexFormatMode());
    }

    public MeshBuilder(VertexFormat format, VertexFormat.DrawMode drawMode) {
        this.format = format;
        this.primitiveVerticesSize = format.getVertexSize();
        this.primitiveIndicesCount = drawMode.firstVertexCount;
    }

    public MeshBuilder(VertexFormat format, VertexFormat.DrawMode drawMode, int vertexCount, int indexCount) {
        this(format, drawMode);
        allocateBuffers(vertexCount, indexCount);
    }

    public void begin() {
        if (building) throw new IllegalStateException("Mesh.begin() called while already building.");
        verticesPointer = verticesPointerStart;
        vertexI = 0;
        indicesCount = 0;
        building = true;
    }

    public MeshBuilder vec2(double x, double y) {
        debugVertexBufferCapacity();
        long p = verticesPointer;
        memPutFloat(p, (float) x);
        memPutFloat(p + 4, (float) y);
        verticesPointer += 8;
        return this;
    }

    public int next() {
        return vertexI++;
    }

    public void quad(int i1, int i2, int i3, int i4) {
        debugIndexBufferCapacity();
        long p = indicesPointer + indicesCount * 4L;
        memPutInt(p, i1);
        memPutInt(p + 4, i2);
        memPutInt(p + 8, i3);
        memPutInt(p + 12, i3);
        memPutInt(p + 16, i4);
        memPutInt(p + 20, i1);
        indicesCount += 6;
    }

    public void ensureQuadCapacity() {
        ensureCapacity(4, 6);
    }

    public void ensureCapacity(int vertexCount, int indexCount) {
        if (DEBUG && (indexCount % primitiveIndicesCount != 0)) {
            throw new IllegalArgumentException("Unexpected amount of indices written to MeshBuilder.");
        }

        if (vertices == null || indices == null) {
            allocateBuffers(256 * 4, 512 * 4);
            return;
        }

        if ((vertexI + vertexCount) * primitiveVerticesSize >= vertices.capacity()) {
            int offset = getVerticesOffset();
            int newSize = Math.max(vertices.capacity() * 2, vertices.capacity() + vertexCount * primitiveVerticesSize);
            ByteBuffer newVertices = BufferUtils.createByteBuffer(newSize);
            memCopy(memAddress0(vertices), memAddress0(newVertices), offset);
            vertices = newVertices;
            verticesPointerStart = memAddress0(vertices);
            verticesPointer = verticesPointerStart + offset;
        }

        if ((indicesCount + indexCount) * Integer.BYTES >= indices.capacity()) {
            int newSize = Math.max(indices.capacity() * 2, indices.capacity() + indexCount * Integer.BYTES);
            ByteBuffer newIndices = BufferUtils.createByteBuffer(newSize);
            memCopy(memAddress0(indices), memAddress0(newIndices), indicesCount * 4L);
            indices = newIndices;
            indicesPointer = memAddress0(indices);
        }
    }

    private void allocateBuffers(int vertexCount, int indexCount) {
        vertices = BufferUtils.createByteBuffer(primitiveVerticesSize * vertexCount);
        verticesPointer = verticesPointerStart = memAddress0(vertices);
        indices = BufferUtils.createByteBuffer(indexCount * Integer.BYTES);
        indicesPointer = memAddress0(indices);
    }

    public void end() {
        if (!building) throw new IllegalStateException("Mesh.end() called while not building.");
        building = false;
    }

    public boolean isBuilding() {
        return building;
    }

    public GpuBuffer getVertexBuffer() {
        vertices.limit(getVerticesOffset());
        return format.uploadImmediateVertexBuffer(vertices);
    }

    public GpuBuffer getIndexBuffer() {
        indices.limit(indicesCount * Integer.BYTES);
        return format.uploadImmediateIndexBuffer(indices);
    }

    public int getIndicesCount() {
        return indicesCount;
    }

    private int getVerticesOffset() {
        return (int) (verticesPointer - verticesPointerStart);
    }

    private void debugVertexBufferCapacity() {
        if (DEBUG && (vertices == null || vertexI * primitiveVerticesSize >= vertices.capacity())) {
            throw new IndexOutOfBoundsException("Vertices written to MeshBuilder without calling ensureCapacity() first.");
        }
    }

    private void debugIndexBufferCapacity() {
        if (DEBUG && (indices == null || indicesCount * Integer.BYTES >= indices.capacity())) {
            throw new IndexOutOfBoundsException("Indices written to MeshBuilder without calling ensureCapacity() first.");
        }
    }
}




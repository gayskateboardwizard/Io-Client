package io.client.renderer;

import com.mojang.blaze3d.vertex.VertexFormat;

public class FullScreenRenderer {
    public static MeshBuilder mesh;

    private FullScreenRenderer() {
    }

    public static void init() {
        if (mesh != null) return;
        mesh = new MeshBuilder(MeteorVertexFormats.POS2, VertexFormat.DrawMode.TRIANGLES, 4, 6);
        mesh.begin();
        mesh.ensureQuadCapacity();
        mesh.quad(
                mesh.vec2(-1, -1).next(),
                mesh.vec2(-1, 1).next(),
                mesh.vec2(1, 1).next(),
                mesh.vec2(1, -1).next()
        );
        mesh.end();
    }
}






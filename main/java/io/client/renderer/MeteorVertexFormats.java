package io.client.renderer;

import com.mojang.blaze3d.vertex.VertexFormat;

public abstract class MeteorVertexFormats {
    public static final VertexFormat POS2 = VertexFormat.builder()
            .add("Position", MeteorVertexFormatElements.POS2)
            .build();

    private MeteorVertexFormats() {
    }
}




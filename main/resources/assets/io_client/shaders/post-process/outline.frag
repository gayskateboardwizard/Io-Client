#version 330 core

in vec2 v_TexCoord;
in vec2 v_OneTexel;

uniform sampler2D u_Texture;

layout (std140) uniform OutlineData {
    int width;
    float fillOpacity;
    int shapeMode;
    float glowMultiplier;
} u_Outline;

out vec4 color;

// Smooth antialiased edge detection with improved falloff
float smoothEdge(float dist, float minDist, float maxDist) {
    // Use smoothstep for smooth antialiased transitions
    float t = (maxDist - dist) / (maxDist - minDist);
    return smoothstep(0.0, 1.0, clamp(t, 0.0, 1.0));
}

void main() {
    vec4 center = texture(u_Texture, v_TexCoord);

    if (center.a != 0.0) {
        if (u_Outline.shapeMode == 0) discard;
        center = vec4(center.rgb, center.a * u_Outline.fillOpacity);
    }
    else {
        if (u_Outline.shapeMode == 1) discard;

        float outlineWidth = float(u_Outline.width);
        float maxDist = outlineWidth * outlineWidth * 4.0;
        float minDist = outlineWidth * outlineWidth;
        float closestDist = maxDist;
        vec4 closestColor = vec4(0.0);
        float totalWeight = 0.0;
        vec4 weightedColor = vec4(0.0);

        // Improved sampling with weighted average for smoother antialiasing
        for (int x = -u_Outline.width; x <= u_Outline.width; x++) {
            for (int y = -u_Outline.width; y <= u_Outline.width; y++) {
                vec2 offset = v_OneTexel * vec2(float(x), float(y));
                vec4 sample = texture(u_Texture, v_TexCoord + offset);

                if (sample.a > 0.0) {
                    float dist = float(x) * float(x) + float(y) * float(y);
                    
                    // Find closest for color
                    if (dist < closestDist) {
                        closestDist = dist;
                        closestColor = sample;
                    }
                    
                    // Weighted average for smoother edges (antialiasing)
                    float weight = 1.0 / (1.0 + dist * 0.5);
                    weightedColor += sample * weight * sample.a;
                    totalWeight += weight * sample.a;
                }
            }
        }

        if (closestDist > minDist) {
            center.a = 0.0;
        } else {
            // Use weighted color for smoother antialiasing
            if (totalWeight > 0.0) {
                center = weightedColor / totalWeight;
            } else {
                center = closestColor;
            }
            
            // Smooth antialiased falloff with improved curve
            float edgeFactor = smoothEdge(closestDist, minDist * 0.3, minDist);
            // Apply additional smoothing for even smoother edges
            edgeFactor = smoothstep(0.0, 1.0, edgeFactor);
            center.a = clamp(edgeFactor * u_Outline.glowMultiplier, 0.0, 1.0);
        }
    }

    color = center;
}


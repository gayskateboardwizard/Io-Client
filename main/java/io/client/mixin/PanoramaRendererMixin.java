package io.client.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.RotatingCubeMapRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RotatingCubeMapRenderer.class)
public class PanoramaRendererMixin {

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void renderStaticPanorama(DrawContext graphics, int width, int height, boolean spin, CallbackInfo ci) {
        graphics.fill(0, 0, width, height, 0xFF242424);

        var mc = MinecraftClient.getInstance();
        var font = mc.textRenderer;

        String text = "IO client: ";
        String hoverText = "§kIO client: ";
        int color = 0xFF292929;

        int textWidth = font.getWidth(text);
        int lineHeight = font.fontHeight + 2;
        int slide = 2;

        double mouseX = mc.mouse.getX() * mc.getWindow().getScaledWidth() / mc.getWindow().getWidth();
        double mouseY = mc.mouse.getY() * mc.getWindow().getScaledHeight() / mc.getWindow().getHeight();

        int row = 0;
        for (int y = 0; y < height; y += lineHeight) {
            int rowOffset = (row * slide) % textWidth;

            for (int x = -textWidth; x < width + textWidth; x += textWidth) {
                int drawX = x + rowOffset;
                int drawY = y;

                boolean hovering =
                        mouseX >= drawX &&
                                mouseX <= drawX + textWidth &&
                                mouseY >= drawY &&
                                mouseY <= drawY + font.fontHeight;

                graphics.drawText(
                        font,
                        hovering ? hoverText : text,
                        drawX,
                        drawY,
                        color,
                        false
                );
            }

            row++;
        }

        ci.cancel();
    }
}



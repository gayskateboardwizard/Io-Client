package io.client.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.PanoramaRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PanoramaRenderer.class)
public class PanoramaRendererMixin {

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void renderStaticPanorama(GuiGraphics graphics, int width, int height, boolean spin, CallbackInfo ci) {
        graphics.fill(0, 0, width, height, 0xFF242424);

        var mc = Minecraft.getInstance();
        var font = mc.font;

        String text = "IO client: ";
        String hoverText = "§kIO client: ";
        int color = 0xFF292929;

        int textWidth = font.width(text);
        int lineHeight = font.lineHeight + 2;
        int slide = 2;

        double mouseX = mc.mouseHandler.xpos() * mc.getWindow().getGuiScaledWidth() / mc.getWindow().getScreenWidth();
        double mouseY = mc.mouseHandler.ypos() * mc.getWindow().getGuiScaledHeight() / mc.getWindow().getScreenHeight();

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
                                mouseY <= drawY + font.lineHeight;

                graphics.drawString(
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

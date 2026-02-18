package io.client.modules.render;

import io.client.clickgui.screens.ClickGuiScreen;
import io.client.managers.TargetManager;
import io.client.modules.templates.Category;
import io.client.modules.templates.Module;
import io.client.settings.BooleanSetting;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;

public class Tracers extends Module {
    private final BooleanSetting playersOnly = new BooleanSetting("PlayersOnly", false);
    private int tracerColor = 0xFFc71e00;

    public Tracers() {
        super("Tracers", "Draw lines to entities", -1, Category.RENDER);
        addSetting(playersOnly);
    }

    @Override
    public void onUpdate() {
        tracerColor = ClickGuiScreen.currentTheme.moduleEnabled;
    }

    public void render(DrawContext graphics, float partialTicks) {
        if (!isEnabled())
            return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null)
            return;

        Camera cam = mc.gameRenderer.getCamera();
        Vec3d camPos = cam.getPos();

        int sw = mc.getWindow().getScaledWidth();
        int sh = mc.getWindow().getScaledHeight();

        double baseFov = mc.options.getFov().getValue();
        float fovModifier = 1.0f;
        if (mc.player.isSprinting())
            fovModifier += 0.15f;
        if (mc.player.getAbilities().flying)
            fovModifier += 0.1f;

        float currentFov = (float) (baseFov * fovModifier);
        Matrix4f projection = mc.gameRenderer.getBasicProjectionMatrix(currentFov);
        Matrix4f modelView = new Matrix4f();
        org.joml.Quaternionf rotation = new org.joml.Quaternionf(cam.getRotation());
        rotation.conjugate();
        modelView.rotation(rotation);

        Matrix4f mvp = new Matrix4f(projection);
        mvp.mul(modelView);

        int renderDist = mc.options.getViewDistance().getValue();
        double maxRange = renderDist * 16;

        for (Entity e : mc.world.getEntities()) {
            if (!(e instanceof LivingEntity))
                continue;
            if (e == mc.player)
                continue;

            if (playersOnly.isEnabled()) {
                if (!(e instanceof net.minecraft.entity.player.PlayerEntity))
                    continue;
            } else {
                if (!TargetManager.INSTANCE.isValidTarget(e))
                    continue;
            }

            double dist = mc.player.distanceTo(e);
            if (dist > maxRange)
                continue;

            double x = e.lastX + (e.getX() - e.lastX) * partialTicks - camPos.x;
            double y = e.lastY + (e.getY() - e.lastY) * partialTicks + e.getHeight() * 0.5 - camPos.y;
            double z = e.lastZ + (e.getZ() - e.lastZ) * partialTicks - camPos.z;

            Vector4f pos = new Vector4f((float) x, (float) y, (float) z, 1.0f);
            pos.mul(mvp);

            if (pos.w <= 0.0f || pos.z <= 0.0f)
                continue;

            float nx = pos.x / pos.w;
            float ny = pos.y / pos.w;

            if (nx < -1.2f || nx > 1.2f || ny < -1.2f || ny > 1.2f)
                continue;

            int sx = (int) ((nx * 0.5f + 0.5f) * sw);
            int sy = (int) ((1.0f - (ny * 0.5f + 0.5f)) * sh);
            drawLine(graphics, sw / 2, sh - 1, sx, sy, tracerColor);
        }
    }

    private void drawLine(DrawContext graphics, int x0, int y0, int x1, int y1, int color) {
        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;
        int err = dx - dy;

        int x = x0;
        int y = y0;
        while (true) {
            graphics.fill(x, y, x + 1, y + 1, color);
            if (x == x1 && y == y1)
                break;
            int e2 = err * 2;
            if (e2 > -dy) {
                err -= dy;
                x += sx;
            }
            if (e2 < dx) {
                err += dx;
                y += sy;
            }
        }
    }
}




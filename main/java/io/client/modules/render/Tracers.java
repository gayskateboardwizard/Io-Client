package io.client.modules.render;

import io.client.clickgui.screens.ClickGuiScreen;
import io.client.managers.TargetManager;
import io.client.modules.templates.Category;
import io.client.modules.templates.Module;
import io.client.settings.BooleanSetting;
import io.client.settings.NumberSetting;
import io.client.settings.RadioSetting;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

public class Tracers extends Module {
    private final RadioSetting style = new RadioSetting("Style", "Lines");
    private final RadioSetting target = new RadioSetting("Target", "Body");
    private final BooleanSetting stem = new BooleanSetting("Stem", true);
    private final NumberSetting maxDistance = new NumberSetting("MaxDistance", 256.0f, 8.0f, 512.0f);

    private final BooleanSetting playersOnly = new BooleanSetting("PlayersOnly", false);
    private final BooleanSetting ignoreSelf = new BooleanSetting("IgnoreSelf", true);
    private final BooleanSetting ignoreFriends = new BooleanSetting("IgnoreFriends", false);
    private final BooleanSetting showInvisible = new BooleanSetting("ShowInvisible", true);

    private final BooleanSetting distanceFade = new BooleanSetting("DistanceFade", true);
    private final BooleanSetting fastMode = new BooleanSetting("FastMode", true);
    private final NumberSetting maxEntities = new NumberSetting("MaxEntities", 64.0f, 8.0f, 256.0f);
    private final NumberSetting thickness = new NumberSetting("Thickness", 1.0f, 1.0f, 3.0f);
    private final RadioSetting startVertical = new RadioSetting("StartVertical", "Bottom");
    private final RadioSetting startHorizontal = new RadioSetting("StartHorizontal", "Center");
    private int tracerColor = 0xFFc71e00;

    public Tracers() {
        super("Tracers", "Draw lines to entities", -1, Category.RENDER);
        style.addOption("Lines");
        style.addOption("Offscreen");
        target.addOption("Head");
        target.addOption("Body");
        target.addOption("Feet");
        addSetting(style);
        addSetting(target);
        addSetting(stem);
        addSetting(maxDistance);
        addSetting(playersOnly);
        addSetting(ignoreSelf);
        addSetting(ignoreFriends);
        addSetting(showInvisible);
        startVertical.addOption("Top");
        startVertical.addOption("Center");
        startVertical.addOption("Bottom");
        startHorizontal.addOption("Left");
        startHorizontal.addOption("Center");
        startHorizontal.addOption("Right");
        addSetting(startVertical);
        addSetting(startHorizontal);
        addSetting(distanceFade);
        addSetting(fastMode);
        addSetting(maxEntities);
        addSetting(thickness);
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

        double maxRange = maxDistance.getValue();
        double maxRangeSq = maxRange * maxRange;

        int drawn = 0;
        int startX = getStartX(sw);
        int startY = getStartY(sh);

        for (Entity e : mc.world.getEntities()) {
            if (drawn >= (int) maxEntities.getValue()) break;
            if (!(e instanceof LivingEntity living))
                continue;
            if (ignoreSelf.isEnabled() && e == mc.player)
                continue;
            if (!e.isAlive())
                continue;
            if (!showInvisible.isEnabled() && e.isInvisible())
                continue;
            if (ignoreFriends.isEnabled() && e instanceof PlayerEntity player && TargetManager.INSTANCE.isFriend(player.getName().getString()))
                continue;

            if (playersOnly.isEnabled()) {
                if (!(e instanceof net.minecraft.entity.player.PlayerEntity))
                    continue;
            } else {
                if (!TargetManager.INSTANCE.isValidTarget(e))
                    continue;
            }

            double distSq = mc.player.squaredDistanceTo(e);
            if (distSq > maxRangeSq)
                continue;
            double dist = Math.sqrt(distSq);

            double x = e.lastX + (e.getX() - e.lastX) * partialTicks - camPos.x;
            double yOffset = switch (target.getSelectedOption()) {
                case "Head" -> e.getHeight();
                case "Feet" -> 0.05;
                default -> e.getHeight() * 0.5;
            };
            double y = e.lastY + (e.getY() - e.lastY) * partialTicks + yOffset - camPos.y;
            double z = e.lastZ + (e.getZ() - e.lastZ) * partialTicks - camPos.z;

            Vector4f pos = new Vector4f((float) x, (float) y, (float) z, 1.0f);
            pos.mul(mvp);

            if (pos.w <= 0.0f || pos.z <= 0.0f)
                continue;

            float nx = pos.x / pos.w;
            float ny = pos.y / pos.w;

            int sx = (int) ((nx * 0.5f + 0.5f) * sw);
            int sy = (int) ((1.0f - (ny * 0.5f + 0.5f)) * sh);
            int color = applyDistanceFade(tracerColor, dist, maxRange);

            boolean onScreen = nx >= -1.0f && nx <= 1.0f && ny >= -1.0f && ny <= 1.0f && pos.w > 0.0f && pos.z > 0.0f;

            if ("Offscreen".equals(style.getSelectedOption())) {
                if (!onScreen) {
                    drawOffscreenArrow(graphics, sw, sh, sx, sy, color);
                    drawn++;
                }
                continue;
            }

            if (!onScreen) continue;

            drawLineThick(graphics, startX, startY, sx, sy, color, Math.max(1, (int) thickness.getValue()));

            if (stem.isEnabled()) {
                ScreenPoint head = projectToScreen(mvp, sw, sh, x, e.lastY + (e.getY() - e.lastY) * partialTicks + e.getHeight() - camPos.y, z);
                ScreenPoint feet = projectToScreen(mvp, sw, sh, x, e.lastY + (e.getY() - e.lastY) * partialTicks - camPos.y, z);
                if (head.visible && feet.visible) {
                    drawLineThick(graphics, head.x, head.y, feet.x, feet.y, color, 1);
                }
            }
            drawn++;
        }
    }

    private ScreenPoint projectToScreen(Matrix4f mvp, int sw, int sh, double x, double y, double z) {
        Vector4f p = new Vector4f((float) x, (float) y, (float) z, 1.0f);
        p.mul(mvp);
        if (p.w <= 0.0f || p.z <= 0.0f) return new ScreenPoint(0, 0, false);
        float nx = p.x / p.w;
        float ny = p.y / p.w;
        boolean visible = nx >= -1.0f && nx <= 1.0f && ny >= -1.0f && ny <= 1.0f;
        int sx = (int) ((nx * 0.5f + 0.5f) * sw);
        int sy = (int) ((1.0f - (ny * 0.5f + 0.5f)) * sh);
        return new ScreenPoint(sx, sy, visible);
    }

    private void drawOffscreenArrow(DrawContext graphics, int sw, int sh, int targetX, int targetY, int color) {
        float cx = sw * 0.5f;
        float cy = sh * 0.5f;
        float dx = targetX - cx;
        float dy = targetY - cy;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 0.001f) return;
        dx /= len;
        dy /= len;

        float radius = Math.min(sw, sh) * 0.42f;
        float px = cx + dx * radius;
        float py = cy + dy * radius;

        float size = 7.0f;
        float lx = -dy;
        float ly = dx;

        int x1 = (int) px;
        int y1 = (int) py;
        int x2 = (int) (px - dx * size + lx * size);
        int y2 = (int) (py - dy * size + ly * size);
        int x3 = (int) (px - dx * size - lx * size);
        int y3 = (int) (py - dy * size - ly * size);

        drawLine(graphics, x1, y1, x2, y2, color);
        drawLine(graphics, x2, y2, x3, y3, color);
        drawLine(graphics, x3, y3, x1, y1, color);
    }

    private int getStartX(int screenWidth) {
        return switch (startHorizontal.getSelectedOption()) {
            case "Left" -> 0;
            case "Right" -> screenWidth - 1;
            default -> screenWidth / 2;
        };
    }

    private int getStartY(int screenHeight) {
        return switch (startVertical.getSelectedOption()) {
            case "Top" -> 0;
            case "Center" -> screenHeight / 2;
            default -> screenHeight - 1;
        };
    }

    private int applyDistanceFade(int baseColor, double distance, double maxRange) {
        if (!distanceFade.isEnabled()) return (0xFF << 24) | (baseColor & 0x00FFFFFF);
        float t = (float) Math.max(0.0, Math.min(1.0, distance / maxRange));
        int alpha = (int) (230 - (t * 160));
        return (alpha << 24) | (baseColor & 0x00FFFFFF);
    }

    private void drawLineThick(DrawContext graphics, int x0, int y0, int x1, int y1, int color, int width) {
        int effectiveWidth = fastMode.isEnabled() ? 1 : Math.max(1, width);
        drawLine(graphics, x0, y0, x1, y1, color, effectiveWidth);
    }

    private void drawLine(DrawContext graphics, int x0, int y0, int x1, int y1, int color, int width) {
        float dx = x1 - x0;
        float dy = y1 - y0;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        if (length < 1.0f) return;

        float angle = (float) Math.atan2(dy, dx);
        int half = Math.max(1, width) / 2;

        var matrices = graphics.getMatrices();
        matrices.pushMatrix();
        matrices.translate(x0, y0);
        matrices.rotate(angle);
        graphics.fill(0, -half, Math.max(1, (int) length), half + 1, color);
        matrices.popMatrix();
    }

    private void drawLine(DrawContext graphics, int x0, int y0, int x1, int y1, int color) {
        drawLine(graphics, x0, y0, x1, y1, color, 1);
    }

    private record ScreenPoint(int x, int y, boolean visible) {
    }
}




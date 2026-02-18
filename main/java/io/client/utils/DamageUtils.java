package io.client.utils;

import net.minecraft.block.BedBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

public class DamageUtils {
    private static final int BED_SCAN_RADIUS = 6;

    private DamageUtils() {
    }

    public static float crystalDamage(LivingEntity target, Vec3d crystalPos) {
        if (crystalPos == null)
            return 0.0f;
        return explosionDamage(target, crystalPos, 12.0f);
    }

    public static float bedDamage(LivingEntity target, Vec3d bedPos) {
        if (bedPos == null)
            return 0.0f;
        return explosionDamage(target, bedPos, 10.0f);
    }

    public static float anchorDamage(LivingEntity target, Vec3d anchorPos) {
        if (anchorPos == null)
            return 0.0f;
        return explosionDamage(target, anchorPos, 10.0f);
    }

    private static float explosionDamage(LivingEntity target, Vec3d explosionPos, float power) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (target == null || explosionPos == null || mc.world == null)
            return 0.0f;

        Vec3d targetPos = target.getPos();
        Box targetBox = target.getBoundingBox();

        double distance = Math.sqrt(
                Math.pow(targetPos.x - explosionPos.x, 2) +
                        Math.pow(targetPos.y - explosionPos.y, 2) +
                        Math.pow(targetPos.z - explosionPos.z, 2));

        if (distance > power)
            return 0.0f;

        double exposure = getExposure(explosionPos, targetBox);
        double impact = (1.0 - (distance / power)) * exposure;
        float damage = (float) ((impact * impact + impact) / 2.0 * 7.0 * power + 1.0);

        // Apply armor and protection reductions
        return applyDamageReductions(target, damage);
    }

    private static double getExposure(Vec3d source, Box box) {
        if (source == null || box == null)
            return 0.0;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null)
            return 0.0;

        double xDiff = box.maxX - box.minX;
        double yDiff = box.maxY - box.minY;
        double zDiff = box.maxZ - box.minZ;

        double xStep = 1.0 / (xDiff * 2.0 + 1.0);
        double yStep = 1.0 / (yDiff * 2.0 + 1.0);
        double zStep = 1.0 / (zDiff * 2.0 + 1.0);

        if (xStep > 0 && yStep > 0 && zStep > 0) {
            int misses = 0;
            int hits = 0;

            double xOffset = (1.0 - Math.floor(1.0 / xStep) * xStep) * 0.5;
            double zOffset = (1.0 - Math.floor(1.0 / zStep) * zStep) * 0.5;

            xStep = xStep * xDiff;
            yStep = yStep * yDiff;
            zStep = zStep * zDiff;

            double startX = box.minX + xOffset;
            double startY = box.minY;
            double startZ = box.minZ + zOffset;
            double endX = box.maxX + xOffset;
            double endY = box.maxY;
            double endZ = box.maxZ + zOffset;

            for (double x = startX; x <= endX; x += xStep) {
                for (double y = startY; y <= endY; y += yStep) {
                    for (double z = startZ; z <= endZ; z += zStep) {
                        Vec3d position = new Vec3d(x, y, z);

                        var result = mc.world.raycast(new RaycastContext(
                                position,
                                source,
                                RaycastContext.ShapeType.COLLIDER,
                                RaycastContext.FluidHandling.NONE,
                                mc.player));

                        if (result.getType() == net.minecraft.util.hit.HitResult.Type.MISS) {
                            misses++;
                        }

                        hits++;
                    }
                }
            }

            return (double) misses / hits;
        }

        return 0.0;
    }

    private static float applyDamageReductions(LivingEntity entity, float damage) {
        if (entity == null)
            return 0.0f;

        // Get armor value
        float armor = (float) entity.getArmor();
        float toughness = (float) entity
                .getAttributeValue(net.minecraft.entity.attribute.EntityAttributes.ARMOR_TOUGHNESS);

        // Apply armor reduction (simplified)
        damage = damage
                * (1.0f - Math.min(20.0f, Math.max(armor / 5.0f, armor - damage / (2.0f + toughness / 4.0f))) / 25.0f);

        // Apply difficulty scaling
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world != null) {
            switch (mc.world.getDifficulty()) {
                case EASY:
                    damage = Math.min(damage / 2.0f + 1.0f, damage);
                    break;
                case HARD:
                    damage *= 1.5f;
                    break;
                case NORMAL:
                case PEACEFUL:
                default:
                    break;
            }
        }

        return Math.max(damage, 0.0f);
    }

    public static float fallDamage(PlayerEntity player) {
        if (player.getAbilities().flying)
            return 0.0f;
        if (player.fallDistance <= 3.0f)
            return 0.0f;

        float damage = (float) (player.fallDistance - 3.0);
        return applyDamageReductions(player, damage);
    }

    public static float possibleHealthReductions(PlayerEntity player, boolean checkExplosions, boolean checkFall) {
        if (player == null)
            return 0.0f;

        float maxDamage = 0.0f;
        MinecraftClient mc = MinecraftClient.getInstance();

        if (checkExplosions && mc.world != null) {
            // Check for nearby end crystals
            for (Entity entity : mc.world.getEntities()) {
                if (entity instanceof EndCrystalEntity) {
                    double distance = player.squaredDistanceTo(entity);
                    if (distance < 144) { // 12 block range
                        float damage = crystalDamage(player, entity.getPos());
                        if (damage > maxDamage)
                            maxDamage = damage;
                    }
                }
            }

            // Check for beds in nether/end by scanning nearby blocks.
            if (!mc.world.getDimension().bedWorks()) {
                BlockPos center = player.getBlockPos();
                for (int x = -BED_SCAN_RADIUS; x <= BED_SCAN_RADIUS; x++) {
                    for (int y = -BED_SCAN_RADIUS; y <= BED_SCAN_RADIUS; y++) {
                        for (int z = -BED_SCAN_RADIUS; z <= BED_SCAN_RADIUS; z++) {
                            BlockPos pos = center.add(x, y, z);
                            if (!(mc.world.getBlockState(pos).getBlock() instanceof BedBlock))
                                continue;

                            double distance = player.squaredDistanceTo(pos.getX(), pos.getY(), pos.getZ());
                            if (distance < 100) {
                                float damage = bedDamage(player, Vec3d.ofCenter(pos));
                                if (damage > maxDamage)
                                    maxDamage = damage;
                            }
                        }
                    }
                }
            }
        }

        if (checkFall) {
            float fallDmg = fallDamage(player);
            if (fallDmg > maxDamage)
                maxDamage = fallDmg;
        }

        return maxDamage;
    }
}



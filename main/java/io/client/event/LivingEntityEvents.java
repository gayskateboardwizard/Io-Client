package io.client.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.ItemStack;
import java.util.Arrays;

public class LivingEntityEvents {
    public static final Event<StartUseItem> START_USE_ITEM = EventFactory.createArrayBacked(StartUseItem.class, (callbacks) -> (entity, itemstack) -> Arrays.stream(callbacks).forEach(callback -> callback.onStartUseItem(entity, itemstack)));
    public static final Event<EntityHeal> ENTITY_HEAL = EventFactory.createArrayBacked(EntityHeal.class, (callbacks) -> (entity, amount) -> {
        for (EntityHeal event : callbacks) {
            boolean result = event.onEntityHeal(entity, amount);
            if (!result) {
                return false;
            }
        }
        return true;
    });

    public static final Event<EntityBlock> ENTITY_BLOCK = EventFactory.createArrayBacked(EntityBlock.class, (callbacks) -> (entity, damagesource, amount) -> {
        for (EntityBlock event : callbacks) {
            boolean result = event.onEntityBlock(entity, damagesource, amount);
            if (!result) {
                return false;
            }
        }
        return true;
    });
    public static final Event<EntityDropXp> ENTITY_DROP_XP = EventFactory.createArrayBacked(EntityDropXp.class, (callbacks) -> (entity, sourceentity, amount) -> {
        for (EntityDropXp event : callbacks) {
            boolean result = event.onEntityDropXp(entity, sourceentity, amount);
            if (!result) {
                return false;
            }
        }
        return true;
    });
    public static final Event<EntityFall> ENTITY_FALL = EventFactory.createArrayBacked(EntityFall.class, (callbacks) -> (entity, falldistance, damagemultiplier) -> {
        for (EntityFall event : callbacks) {
            boolean result = event.onEntityFall(entity, falldistance, damagemultiplier);
            if (!result) {
                return false;
            }
        }
        return true;
    });
    public static final Event<EntityPickupItem> ENTITY_PICKUP_ITEM = EventFactory.createArrayBacked(EntityPickupItem.class,
            (callbacks) -> (entity, itemstack) -> Arrays.stream(callbacks).forEach(callback -> callback.onEntityPickupItem(entity, itemstack)));
    public static final Event<EntityJump> ENTITY_JUMP = EventFactory.createArrayBacked(EntityJump.class, (callbacks) -> (entity) -> Arrays.stream(callbacks).forEach(callback -> callback.onEntityJump(entity)));

    @FunctionalInterface
    public interface StartUseItem {
        void onStartUseItem(Entity entity, ItemStack itemstack);
    }

    @FunctionalInterface
    public interface EntityHeal {
        boolean onEntityHeal(Entity entity, float amount);
    }

    @FunctionalInterface
    public interface EntityBlock {
        boolean onEntityBlock(Entity entity, DamageSource damagesource, double amount);
    }

    @FunctionalInterface
    public interface EntityDropXp {
        boolean onEntityDropXp(Entity entity, Entity sourceentity, double amount);
    }

    @FunctionalInterface
    public interface EntityFall {
        boolean onEntityFall(Entity entity, double falldistance, double damagemultiplier);
    }

    @FunctionalInterface
    public interface EntityPickupItem {
        void onEntityPickupItem(Entity entity, ItemStack itemstack);
    }

    @FunctionalInterface
    public interface EntityJump {
        void onEntityJump(Entity entity);
    }

}


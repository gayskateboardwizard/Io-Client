package io.client.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import java.util.Arrays;

public class PlayerEvents {
    public static final Event<TickEnd> END_PLAYER_TICK = EventFactory.createArrayBacked(TickEnd.class, (callbacks) -> (entity) -> Arrays.stream(callbacks).forEach(callback -> callback.onEndTick(entity)));
    public static final Event<XPChange> XP_CHANGE = EventFactory.createArrayBacked(XPChange.class, (callbacks) -> (entity, amount) -> Arrays.stream(callbacks).forEach(callback -> callback.onXpChange(entity, amount)));
    public static final Event<LevelChange> LEVEL_CHANGE = EventFactory.createArrayBacked(LevelChange.class, (callbacks) -> (entity, amount) -> Arrays.stream(callbacks).forEach(callback -> callback.onLevelChange(entity, amount)));
    public static final Event<PickupXp> PICKUP_XP = EventFactory.createArrayBacked(PickupXp.class, (callbacks) -> (entity) -> {
        for (PickupXp event : callbacks) {
            boolean result = event.onPickupXp(entity);
            if (!result) {
                return false;
            }
        }
        return true;
    });

    @FunctionalInterface
    public interface TickEnd {
        void onEndTick(PlayerEntity player);
    }

    @FunctionalInterface
    public interface XPChange {
        void onXpChange(PlayerEntity player, int amount);
    }

    @FunctionalInterface
    public interface LevelChange {
        void onLevelChange(PlayerEntity player, int amount);
    }

    @FunctionalInterface
    public interface PickupXp {
        boolean onPickupXp(Entity entity);
    }
}


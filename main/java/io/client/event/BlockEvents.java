package io.client.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;

public class BlockEvents {
    public static final Event<BlockMultiplace> BLOCK_MULTIPLACE = EventFactory.createArrayBacked(BlockMultiplace.class, (callbacks) -> (position, entity, placed, placedAgainst) -> {
        for (BlockMultiplace event : callbacks) {
            boolean result = event.onMultiplaced(position, entity, placed, placedAgainst);
            if (!result) {
                return false;
            }
        }
        return true;
    });
    public static final Event<BlockPlace> BLOCK_PLACE = EventFactory.createArrayBacked(BlockPlace.class, (callbacks) -> (position, entity, placed, placedAgainst) -> {
        for (BlockPlace event : callbacks) {
            boolean result = event.onBlockPlaced(position, entity, placed, placedAgainst);
            if (!result) {
                return false;
            }
        }
        return true;
    });

    @FunctionalInterface
    public interface BlockMultiplace {
        boolean onMultiplaced(BlockPos position, Entity entity, BlockState placed, BlockState placedAgainst);
    }

    @FunctionalInterface
    public interface BlockPlace {
        boolean onBlockPlaced(BlockPos position, Entity entity, BlockState placed, BlockState placedAgainst);
    }
}


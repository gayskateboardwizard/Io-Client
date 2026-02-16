package io.client;

import java.io.*;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.boss.dragon.EnderDragonPart;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.mob.AmbientEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.WaterCreatureEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.entity.vehicle.BoatEntity;

public class TargetManager {
    public static final TargetManager INSTANCE = new TargetManager();
    private final Map<TargetType, Boolean> targetStates = new EnumMap<>(TargetType.class);
    private final Set<String> friends = new HashSet<>();

    private TargetManager() {
        for (TargetType type : TargetType.values()) {
            targetStates.put(type, true);
        }
    }

    public boolean isTargetEnabled(TargetType type) {
        return targetStates.getOrDefault(type, false);
    }

    public void toggleTarget(TargetType type) {
        targetStates.put(type, !isTargetEnabled(type));
        saveTargets();
    }

    public void addFriend(String username) {
        friends.add(username.toLowerCase());
        saveFriends();
    }

    public void removeFriend(String username) {
        friends.remove(username.toLowerCase());
        saveFriends();
    }

    public boolean isFriend(String username) {
        return friends.contains(username.toLowerCase());
    }

    public Set<String> getFriends() {
        return new HashSet<>(friends);
    }

    public void clearFriends() {
        friends.clear();
        saveFriends();
    }

    public boolean isValidTarget(Entity entity) {
        if (entity == null) {
            return false;
        }

        if (entity == MinecraftClient.getInstance().player) {
            return false;
        }

        if (entity instanceof PlayerEntity player) {
            if (isFriend(player.getName().getString())) {
                return false;
            }
        }

        if (entity instanceof ArmorStandEntity) {
            return isTargetEnabled(TargetType.ARMOR_STANDS);
        }

        if (entity instanceof AbstractMinecartEntity || entity instanceof BoatEntity) {
            return isTargetEnabled(TargetType.VEHICLES);
        }

        if (!(entity instanceof LivingEntity livingEntity)) {
            return false;
        }

        if (livingEntity.isDead()) {
            return false;
        }

        if (entity instanceof PlayerEntity) {
            return isTargetEnabled(TargetType.PLAYERS);
        }

        if (entity instanceof EnderDragonEntity || entity instanceof WitherEntity || entity instanceof EnderDragonPart) {
            return isTargetEnabled(TargetType.BOSSES);
        }

        if (entity instanceof MerchantEntity) {
            return isTargetEnabled(TargetType.VILLAGERS);
        }

        if (entity instanceof AmbientEntity) {
            return isTargetEnabled(TargetType.AMBIENT);
        }

        if (entity instanceof WaterCreatureEntity) {
            return isTargetEnabled(TargetType.WATER_ANIMALS);
        }

        if (entity instanceof AnimalEntity) {
            return isTargetEnabled(TargetType.ANIMALS);
        }

        if (entity instanceof HostileEntity) {
            return isTargetEnabled(TargetType.MOBS);
        }

        return isTargetEnabled(TargetType.MOBS);
    }

    public void saveTargets() {
        File configFile = ModuleManager.INSTANCE.getTargetsFile();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(configFile))) {
            for (Map.Entry<TargetType, Boolean> entry : targetStates.entrySet()) {
                writer.write(entry.getKey().name() + ":" + entry.getValue());
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("Failed to save target settings: " + e.getMessage());
        }
    }

    public void loadTargets() {
        File configFile = ModuleManager.INSTANCE.getTargetsFile();
        if (!configFile.exists()) {
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(configFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(":");
                if (parts.length == 2) {
                    try {
                        TargetType type = TargetType.valueOf(parts[0]);
                        boolean enabled = Boolean.parseBoolean(parts[1]);
                        targetStates.put(type, enabled);
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to load target settings: " + e.getMessage());
        }
    }

    public void saveFriends() {
        File configFile = ModuleManager.INSTANCE.getFriendsFile();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(configFile))) {
            for (String friend : friends) {
                writer.write(friend);
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("Failed to save friends: " + e.getMessage());
        }
    }

    public void loadFriends() {
        File configFile = ModuleManager.INSTANCE.getFriendsFile();
        if (!configFile.exists()) {
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(configFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    friends.add(line.trim().toLowerCase());
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to load friends: " + e.getMessage());
        }
    }

    public enum TargetType {
        PLAYERS("Players"),
        MOBS("Mobs"),
        ANIMALS("Animals"),
        VILLAGERS("Villagers"),
        AMBIENT("Ambient"),
        WATER_ANIMALS("Water Animals"),
        BOSSES("Bosses"),
        ARMOR_STANDS("Armor Stands"),
        VEHICLES("Vehicles");

        private final String name;

        TargetType(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }
}
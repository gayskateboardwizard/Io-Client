package io.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.boss.EnderDragonPart;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.Boat;

import java.io.*;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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

        if (entity == Minecraft.getInstance().player) {
            return false;
        }

        if (entity instanceof Player player) {
            if (isFriend(player.getName().getString())) {
                return false;
            }
        }

        if (entity instanceof ArmorStand) {
            return isTargetEnabled(TargetType.ARMOR_STANDS);
        }

        if (entity instanceof AbstractMinecart || entity instanceof Boat) {
            return isTargetEnabled(TargetType.VEHICLES);
        }

        if (!(entity instanceof LivingEntity livingEntity)) {
            return false;
        }

        if (livingEntity.isDeadOrDying()) {
            return false;
        }

        if (entity instanceof Player) {
            return isTargetEnabled(TargetType.PLAYERS);
        }

        if (entity instanceof EnderDragon || entity instanceof WitherBoss || entity instanceof EnderDragonPart) {
            return isTargetEnabled(TargetType.BOSSES);
        }

        if (entity instanceof AbstractVillager) {
            return isTargetEnabled(TargetType.VILLAGERS);
        }

        if (entity instanceof AmbientCreature) {
            return isTargetEnabled(TargetType.AMBIENT);
        }

        if (entity instanceof WaterAnimal) {
            return isTargetEnabled(TargetType.WATER_ANIMALS);
        }

        if (entity instanceof Animal) {
            return isTargetEnabled(TargetType.ANIMALS);
        }

        if (entity instanceof Monster) {
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
package io.client.modules;

import io.client.Category;
import io.client.Module;
import io.client.settings.*;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.AbstractDonkeyEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public class DonkeyBoatDupe extends Module {
    private final MinecraftClient mc = MinecraftClient.getInstance();
    private final Random random = new Random();

    private final RadioSetting modeSetting = new RadioSetting("Mode", "Boat");

    private final CategorySetting boatCategory = new CategorySetting("Boat Settings");
    private final NumberSetting distanceSetting = new NumberSetting("Distance", 128.0f, 1.0f, 256.0f);
    private final BooleanSetting useForwardDistance = new BooleanSetting("UseForwardDistanceForReturn", true);
    private final NumberSetting returnDistance = new NumberSetting("ReturnDistance", 128.0f, 1.0f, 256.0f);
    private final NumberSetting boatSpeed = new NumberSetting("BoatSpeed", 0.5f, 0.1f, 2.0f);
    private final NumberSetting exitGracePeriod = new NumberSetting("ExitGraceTicks", 5.0f, 1.0f, 20.0f);

    private final CategorySetting homeCategory = new CategorySetting("Home Settings");
    private final StringSetting home1Setting = new StringSetting("Home1", "home1");
    private final StringSetting home2Setting = new StringSetting("Home2", "home2");
    private final RadioSetting chestLocationSetting = new RadioSetting("ChestLocation", "Home1");

    private final NumberSetting invDelay = new NumberSetting("Delay", 2.0f, 1.0f, 10.0f);
    private final NumberSetting delayRandom = new NumberSetting("AddRandomTickDelay", 2.0f, 0.0f, 5.0f);

    private enum State {
        FIND_DONKEY, APPROACH_DONKEY, OPEN_DONKEY, WAIT_DONKEY_OPEN,
        FIND_BOAT, APPROACH_BOAT, WAIT_BOAT_MOUNT,
        MOVE_FORWARD, WAIT_FORWARD_STOP,
        WAIT_INV_OPEN, TAKE_ITEMS,
        MOVE_BACKWARD, WAIT_BACKWARD_STOP,
        EXIT_BOAT, WAIT_BOAT_EXIT,
        APPROACH_CHEST, OPEN_CHEST, WAIT_CHEST_OPEN, DUMP_ITEMS,
        CHECK_CHEST,

        HOME_TELEPORT_1, HOME_WAIT_TP1, HOME_WAIT_DONKEY_OPEN,
        HOME_TAKE_ITEMS, HOME_TELEPORT_2, HOME_WAIT_TP2,
        HOME_APPROACH_CHEST, HOME_OPEN_CHEST, HOME_WAIT_CHEST_OPEN, HOME_DUMP_ITEMS,
        HOME_CHECK_CHEST
    }

    private State state = State.FIND_DONKEY;
    private AbstractDonkeyEntity targetDonkey;
    private BoatEntity targetBoat;
    private BlockPos markedChest;
    private int ticks = 0;
    private int stopTicks = 0;
    private int invTickCounter = 0;
    private int currentRandomDelay = 0;
    private Vec3d boatEntryPos = null;
    private double maxDistReached = 0;

    public DonkeyBoatDupe() {
        super("AutoDupe", "Automates dupe for 8b8t.me", -1, Category.MISC);

        modeSetting.addOption("Boat");
        modeSetting.addOption("Home");
        addSetting(modeSetting);

        boatCategory.addSetting(distanceSetting);
        boatCategory.addSetting(useForwardDistance);
        boatCategory.addSetting(returnDistance);
        boatCategory.addSetting(boatSpeed);
        boatCategory.addSetting(exitGracePeriod);
        addSetting(boatCategory);

        homeCategory.addSetting(home1Setting);
        homeCategory.addSetting(home2Setting);
        chestLocationSetting.addOption("Home1");
        chestLocationSetting.addOption("Home2");
        homeCategory.addSetting(chestLocationSetting);
        addSetting(homeCategory);

        addSetting(invDelay);
        addSetting(delayRandom);
    }

    @Override
    public void onEnable() {
        resetState();
    }

    @Override
    public void onDisable() {
        cleanup();
    }

    private void resetState() {
        if (modeSetting.isSelected("Boat")) {
            state = State.FIND_DONKEY;
        } else {
            state = State.FIND_DONKEY;
        }
        ticks = 0;
        stopTicks = 0;
        invTickCounter = 0;
        updateRandomDelay();
        targetDonkey = null;
        targetBoat = null;
        boatEntryPos = null;
        maxDistReached = 0;
    }

    private void cleanup() {
        releaseAllKeys();
        if (mc.player != null) {
            mc.player.setVelocity(0, 0, 0);
            if (mc.player.getVehicle() != null) {
                mc.player.getVehicle().setVelocity(0, 0, 0);
            }
            if (mc.currentScreen != null) {
                mc.player.closeHandledScreen();
            }
        }
    }

    @Override
    public void onUpdate() {
        if (mc.player == null || mc.world == null) return;
        ticks++;

        if (modeSetting.isSelected("Boat")) {
            updateBoatMode();
        } else {
            updateHomeMode();
        }
    }

    private void updateBoatMode() {
        switch (state) {
            case FIND_DONKEY:
                findDonkey();
                break;
            case APPROACH_DONKEY:
                approachDonkey();
                break;
            case OPEN_DONKEY:
                openDonkey();
                break;
            case WAIT_DONKEY_OPEN:
                waitDonkeyOpen();
                break;
            case FIND_BOAT:
                findBoat();
                break;
            case APPROACH_BOAT:
                approachBoat();
                break;
            case WAIT_BOAT_MOUNT:
                waitBoatMount();
                break;
            case MOVE_FORWARD:
                moveForward();
                break;
            case WAIT_FORWARD_STOP:
                waitForwardStop();
                break;
            case WAIT_INV_OPEN:
                waitInventoryOpen();
                break;
            case TAKE_ITEMS:
                takeItems();
                break;
            case MOVE_BACKWARD:
                moveBackward();
                break;
            case WAIT_BACKWARD_STOP:
                waitBackwardStop();
                break;
            case EXIT_BOAT:
                exitBoat();
                break;
            case WAIT_BOAT_EXIT:
                waitBoatExit();
                break;
            case APPROACH_CHEST:
                approachChest();
                break;
            case OPEN_CHEST:
                openChest();
                break;
            case WAIT_CHEST_OPEN:
                waitChestOpen();
                break;
            case DUMP_ITEMS:
                dumpItems();
                break;
            case CHECK_CHEST:
                checkChest();
                break;
        }
    }

    private void updateHomeMode() {
        switch (state) {
            case FIND_DONKEY:
                findDonkeyHome();
                break;
            case APPROACH_DONKEY:
                approachDonkeyHome();
                break;
            case OPEN_DONKEY:
                openDonkeyHome();
                break;
            case HOME_WAIT_DONKEY_OPEN:
                homeWaitDonkeyOpen();
                break;
            case HOME_TELEPORT_1:
                homeTeleport1();
                break;
            case HOME_WAIT_TP1:
                homeWaitTp1();
                break;
            case HOME_TAKE_ITEMS:
                homeTakeItems();
                break;
            case HOME_TELEPORT_2:
                homeTeleport2();
                break;
            case HOME_WAIT_TP2:
                homeWaitTp2();
                break;
            case HOME_APPROACH_CHEST:
                homeApproachChest();
                break;
            case HOME_OPEN_CHEST:
                homeOpenChest();
                break;
            case HOME_WAIT_CHEST_OPEN:
                homeWaitChestOpen();
                break;
            case HOME_DUMP_ITEMS:
                homeDumpItems();
                break;
            case HOME_CHECK_CHEST:
                homeCheckChest();
                break;
        }
    }

    private void findDonkeyHome() {
        List<AbstractDonkeyEntity> horses = mc.world.getEntitiesByClass(AbstractDonkeyEntity.class,
                new Box(mc.player.getBlockPos()).expand(32), h -> h.hasChest() && h.isAlive());
        if (horses.isEmpty()) {
            this.toggle();
            return;
        }
        targetDonkey = horses.stream()
                .min(Comparator.comparingDouble(h -> h.squaredDistanceTo(mc.player)))
                .orElse(null);
        if (targetDonkey != null) {
            state = State.APPROACH_DONKEY;
            ticks = 0;
        }
    }

    private void approachDonkeyHome() {
        if (targetDonkey == null || !targetDonkey.isAlive()) {
            state = State.FIND_DONKEY;
            return;
        }

        double distance = mc.player.distanceTo(targetDonkey);
        if (distance < 3.0) {
            releaseAllKeys();
            state = State.OPEN_DONKEY;
            ticks = 0;
        } else {
            lookAt(targetDonkey.getPos());
            pressForward();
        }
    }

    private void openDonkeyHome() {
        if (targetDonkey == null || !targetDonkey.isAlive()) {
            state = State.FIND_DONKEY;
            return;
        }

        lookAt(targetDonkey.getPos());
        mc.options.sneakKey.setPressed(true);

        if (ticks > 5) {
            mc.interactionManager.interactEntity(mc.player, targetDonkey, mc.player.getActiveHand());
            mc.options.sneakKey.setPressed(false);
            state = State.HOME_WAIT_DONKEY_OPEN;
            ticks = 0;
        }
    }

    private void homeWaitDonkeyOpen() {
        if (mc.currentScreen != null) {
            state = State.HOME_TELEPORT_1;
            ticks = 0;
        } else if (ticks > 20) {
            state = State.OPEN_DONKEY;
            ticks = 0;
        }
    }

    private void homeTeleport1() {
        if (ticks == 60) {
            mc.player.networkHandler.sendChatCommand("home " + home1Setting.getValue());
        }
        if (ticks > 80) {
            state = State.HOME_WAIT_TP1;
            ticks = 0;
        }
    }

    private void homeWaitTp1() {
        if (ticks > 40) {
            state = State.HOME_TAKE_ITEMS;
            ticks = 0;
            invTickCounter = 0;
            updateRandomDelay();
        }
    }

    private void homeTakeItems() {
        if (mc.currentScreen == null) {
            state = State.HOME_TELEPORT_2;
            ticks = 0;
            return;
        }

        invTickCounter++;
        if (invTickCounter < currentRandomDelay) return;

        invTickCounter = 0;
        updateRandomDelay();

        int playerInvStart = mc.player.currentScreenHandler.slots.size() - 36;
        boolean movedAny = false;

        for (int i = 0; i < playerInvStart; i++) {
            if (!mc.player.currentScreenHandler.getSlot(i).getStack().isEmpty()) {
                mc.interactionManager.clickSlot(
                        mc.player.currentScreenHandler.syncId, i, 0,
                        net.minecraft.screen.slot.SlotActionType.QUICK_MOVE, mc.player);
                movedAny = true;
                break;
            }
        }

        if (!movedAny) {
            mc.player.closeHandledScreen();
            state = State.HOME_TELEPORT_2;
            ticks = 0;
        }
    }

    private void homeTeleport2() {
        if (ticks == 60) {
            mc.player.networkHandler.sendChatCommand("home " + home2Setting.getValue());
        }
        if (ticks > 80) {
            state = State.HOME_WAIT_TP2;
            ticks = 0;
        }
    }

    private void homeWaitTp2() {
        if (ticks > 40) {
            if (markedChest != null) {
                state = State.HOME_APPROACH_CHEST;
                ticks = 0;
            } else {
                this.toggle();
            }
        }
    }

    private void homeApproachChest() {
        if (markedChest == null) {
            this.toggle();
            return;
        }

        double distToChest = mc.player.getPos().distanceTo(Vec3d.ofCenter(markedChest));

        if (distToChest < 4.5) {
            releaseAllKeys();
            state = State.HOME_OPEN_CHEST;
            ticks = 0;
        } else {
            lookAt(Vec3d.ofCenter(markedChest), false);
            pressForward();
        }
    }

    private void homeOpenChest() {
        if (markedChest == null) {
            this.toggle();
            return;
        }

        lookAt(Vec3d.ofCenter(markedChest));

        if (ticks > 5) {
            mc.interactionManager.interactBlock(mc.player, mc.player.getActiveHand(),
                    new net.minecraft.util.hit.BlockHitResult(
                            Vec3d.ofCenter(markedChest),
                            net.minecraft.util.math.Direction.UP,
                            markedChest, false));
            state = State.HOME_WAIT_CHEST_OPEN;
            ticks = 0;
        }
    }

    private void homeWaitChestOpen() {
        if (mc.currentScreen != null) {
            state = State.HOME_DUMP_ITEMS;
            ticks = 0;
            invTickCounter = 0;
            updateRandomDelay();
        } else if (ticks > 40) {
            state = State.HOME_APPROACH_CHEST;
            ticks = 0;
        }
    }

    private void homeDumpItems() {
        if (mc.currentScreen == null) {
            state = State.HOME_CHECK_CHEST;
            ticks = 0;
            return;
        }

        invTickCounter++;
        if (invTickCounter < currentRandomDelay) return;

        invTickCounter = 0;
        updateRandomDelay();

        int playerInvStart = mc.player.currentScreenHandler.slots.size() - 36;
        boolean movedAny = false;

        for (int i = playerInvStart; i < mc.player.currentScreenHandler.slots.size(); i++) {
            if (!mc.player.currentScreenHandler.getSlot(i).getStack().isEmpty()) {
                mc.interactionManager.clickSlot(
                        mc.player.currentScreenHandler.syncId, i, 0,
                        net.minecraft.screen.slot.SlotActionType.QUICK_MOVE, mc.player);
                movedAny = true;
                break;
            }
        }

        if (!movedAny) {
            mc.player.closeHandledScreen();
            state = State.HOME_CHECK_CHEST;
            ticks = 0;
        }
    }

    private void homeCheckChest() {
        if (markedChest == null) {
            this.toggle();
            return;
        }

        if (mc.world.getBlockEntity(markedChest) instanceof ChestBlockEntity chest) {
            boolean hasEmptySlot = false;
            for (int i = 0; i < chest.size(); i++) {
                if (chest.getStack(i).isEmpty()) {
                    hasEmptySlot = true;
                    break;
                }
            }

            if (hasEmptySlot) {
                state = State.FIND_DONKEY;
                ticks = 0;
                invTickCounter = 0;
                updateRandomDelay();
                targetDonkey = null;
            } else {
                this.toggle();
            }
        } else {
            this.toggle();
        }
    }

    private void findDonkey() {
        List<AbstractDonkeyEntity> horses = mc.world.getEntitiesByClass(AbstractDonkeyEntity.class,
                new Box(mc.player.getBlockPos()).expand(32), h -> h.hasChest() && h.isAlive());
        if (horses.isEmpty()) {
            this.toggle();
            return;
        }
        targetDonkey = horses.stream()
                .min(Comparator.comparingDouble(h -> h.squaredDistanceTo(mc.player)))
                .orElse(null);
        if (targetDonkey != null) {
            state = State.APPROACH_DONKEY;
            ticks = 0;
        }
    }

    private void approachDonkey() {
        if (targetDonkey == null || !targetDonkey.isAlive()) {
            state = State.FIND_DONKEY;
            return;
        }

        double distance = mc.player.distanceTo(targetDonkey);
        if (distance < 3.0) {
            releaseAllKeys();
            state = State.OPEN_DONKEY;
            ticks = 0;
        } else {
            lookAt(targetDonkey.getPos());
            pressForward();
        }
    }

    private void openDonkey() {
        if (targetDonkey == null || !targetDonkey.isAlive()) {
            state = State.FIND_DONKEY;
            return;
        }

        lookAt(targetDonkey.getPos());
        mc.options.sneakKey.setPressed(true);

        if (ticks > 5) {
            mc.interactionManager.interactEntity(mc.player, targetDonkey, mc.player.getActiveHand());
            mc.options.sneakKey.setPressed(false);
            state = State.WAIT_DONKEY_OPEN;
            ticks = 0;
        }
    }

    private void waitDonkeyOpen() {
        if (mc.currentScreen != null || ticks > 20) {
            if (mc.currentScreen != null) {
                mc.player.closeHandledScreen();
            }
            state = State.FIND_BOAT;
            ticks = 0;
        }
    }

    private void findBoat() {
        List<BoatEntity> boats = mc.world.getEntitiesByClass(BoatEntity.class,
                new Box(mc.player.getBlockPos()).expand(32), Entity::isAlive);
        if (boats.isEmpty()) {
            this.toggle();
            return;
        }
        targetBoat = boats.stream()
                .min(Comparator.comparingDouble(b -> b.squaredDistanceTo(mc.player)))
                .orElse(null);
        if (targetBoat != null) {
            state = State.APPROACH_BOAT;
            ticks = 0;
        }
    }

    private void approachBoat() {
        if (targetBoat == null || !targetBoat.isAlive()) {
            state = State.FIND_BOAT;
            return;
        }

        double distance = mc.player.distanceTo(targetBoat);
        if (distance < 3.5) {
            releaseAllKeys();
            lookAt(targetBoat.getPos());
            mc.interactionManager.interactEntity(mc.player, targetBoat, mc.player.getActiveHand());
            state = State.WAIT_BOAT_MOUNT;
            ticks = 0;
        } else {
            lookAt(targetBoat.getPos());
            pressForward();
        }
    }

    private void waitBoatMount() {
        if (mc.player.getVehicle() instanceof BoatEntity) {
            if (boatEntryPos == null) {
                boatEntryPos = mc.player.getVehicle().getPos();
            }
            state = State.MOVE_FORWARD;
            ticks = 0;
            stopTicks = 0;
        } else if (ticks > 40) {
            state = State.APPROACH_BOAT;
            ticks = 0;
        }
    }

    private void moveForward() {
        Entity boat = mc.player.getVehicle();
        if (!(boat instanceof BoatEntity)) {
            state = State.FIND_BOAT;
            return;
        }

        applyVelocity(boat, true, boatSpeed.getValue());
        state = State.WAIT_FORWARD_STOP;
        ticks = 0;
        stopTicks = 0;
    }

    private void waitForwardStop() {
        Entity boat = mc.player.getVehicle();
        if (!(boat instanceof BoatEntity) || boatEntryPos == null) {
            state = State.FIND_BOAT;
            return;
        }

        double distFromHome = boat.getPos().distanceTo(boatEntryPos);
        double speed = boat.getVelocity().horizontalLength();
        double targetDist = distanceSetting.getValue();

        if (distFromHome > maxDistReached) {
            maxDistReached = distFromHome;
        }

        boolean reachedTarget = distFromHome >= targetDist;
        boolean stoppedEarly = (speed < 0.05) && (distFromHome >= targetDist * 0.9);

        if (reachedTarget || stoppedEarly) {
            boat.setVelocity(0, 0, 0);
            stopTicks++;
            if (stopTicks > 10) {
                state = State.WAIT_INV_OPEN;
                ticks = 0;
                invTickCounter = 0;
            }
        } else {
            applyVelocity(boat, true, boatSpeed.getValue());
            stopTicks = 0;
        }
    }

    private void waitInventoryOpen() {
        if (mc.currentScreen != null) {
            state = State.TAKE_ITEMS;
            ticks = 0;
            invTickCounter = 0;
            updateRandomDelay();
        } else if (ticks > 60) {
            state = State.MOVE_BACKWARD;
            ticks = 0;
        }
    }

    private void takeItems() {
        if (mc.currentScreen == null) {
            state = State.MOVE_BACKWARD;
            ticks = 0;
            return;
        }

        invTickCounter++;
        if (invTickCounter < currentRandomDelay) return;

        invTickCounter = 0;
        updateRandomDelay();

        int playerInvStart = mc.player.currentScreenHandler.slots.size() - 36;
        boolean movedAny = false;

        for (int i = 0; i < playerInvStart; i++) {
            if (!mc.player.currentScreenHandler.getSlot(i).getStack().isEmpty()) {
                mc.interactionManager.clickSlot(
                        mc.player.currentScreenHandler.syncId, i, 0,
                        net.minecraft.screen.slot.SlotActionType.QUICK_MOVE, mc.player);
                movedAny = true;
                break;
            }
        }

        if (!movedAny) {
            mc.player.closeHandledScreen();
            state = State.MOVE_BACKWARD;
            ticks = 0;
        }
    }

    private void moveBackward() {
        Entity boat = mc.player.getVehicle();
        if (!(boat instanceof BoatEntity)) {
            state = State.EXIT_BOAT;
            return;
        }

        applyVelocity(boat, false, boatSpeed.getValue());
        state = State.WAIT_BACKWARD_STOP;
        ticks = 0;
        stopTicks = 0;
    }

    private void waitBackwardStop() {
        Entity boat = mc.player.getVehicle();
        if (!(boat instanceof BoatEntity) || boatEntryPos == null) {
            state = State.EXIT_BOAT;
            return;
        }

        double distFromHome = boat.getPos().distanceTo(boatEntryPos);
        double speed = boat.getVelocity().horizontalLength();

        boolean nearHome = distFromHome <= 3.0;
        boolean stopped = speed < 0.05;

        if (nearHome && stopped) {
            boat.setVelocity(0, 0, 0);
            stopTicks++;
            if (stopTicks > exitGracePeriod.getValue()) {
                state = State.EXIT_BOAT;
                ticks = 0;
            }
        } else if (distFromHome > 3.0) {
            applyVelocity(boat, false, boatSpeed.getValue());
            stopTicks = 0;
        } else {
            boat.setVelocity(boat.getVelocity().multiply(0.5));
            stopTicks = 0;
        }
    }

    private void exitBoat() {
        mc.options.sneakKey.setPressed(true);
        if (ticks > 5) {
            mc.options.sneakKey.setPressed(false);
            state = State.WAIT_BOAT_EXIT;
            ticks = 0;
        }
    }

    private void waitBoatExit() {
        if (mc.player.getVehicle() == null) {
            if (markedChest != null) {
                state = State.APPROACH_CHEST;
                ticks = 0;
            } else {
                this.toggle();
            }
        } else if (ticks > 40) {
            state = State.EXIT_BOAT;
            ticks = 0;
        }
    }

    private void approachChest() {
        if (markedChest == null) {
            this.toggle();
            return;
        }

        double distToChest = mc.player.getPos().distanceTo(Vec3d.ofCenter(markedChest));

        if (distToChest < 4.5) {
            releaseAllKeys();
            state = State.OPEN_CHEST;
            ticks = 0;
        } else {
            lookAt(Vec3d.ofCenter(markedChest), false);
            pressForward();
        }
    }

    private void openChest() {
        if (markedChest == null) {
            this.toggle();
            return;
        }

        lookAt(Vec3d.ofCenter(markedChest));

        if (ticks == 5) {
            mc.options.useKey.setPressed(true);
        }

        if (ticks > 7) {
            mc.options.useKey.setPressed(false);
            state = State.WAIT_CHEST_OPEN;
            ticks = 0;
        }
    }

    private void waitChestOpen() {
        if (mc.currentScreen != null) {
            state = State.DUMP_ITEMS;
            ticks = 0;
            invTickCounter = 0;
            updateRandomDelay();
        } else if (ticks > 40) {
            state = State.APPROACH_CHEST;
            ticks = 0;
        }
    }

    private void dumpItems() {
        if (mc.currentScreen == null) {
            state = State.CHECK_CHEST;
            ticks = 0;
            return;
        }

        invTickCounter++;
        if (invTickCounter < currentRandomDelay) return;

        invTickCounter = 0;
        updateRandomDelay();

        int playerInvStart = mc.player.currentScreenHandler.slots.size() - 36;
        boolean movedAny = false;

        for (int i = playerInvStart; i < mc.player.currentScreenHandler.slots.size(); i++) {
            if (!mc.player.currentScreenHandler.getSlot(i).getStack().isEmpty()) {
                mc.interactionManager.clickSlot(
                        mc.player.currentScreenHandler.syncId, i, 0,
                        net.minecraft.screen.slot.SlotActionType.QUICK_MOVE, mc.player);
                movedAny = true;
                break;
            }
        }

        if (!movedAny) {
            mc.player.closeHandledScreen();
            state = State.CHECK_CHEST;
            ticks = 0;
        }
    }

    private void checkChest() {
        if (markedChest == null) {
            this.toggle();
            return;
        }

        if (mc.world.getBlockEntity(markedChest) instanceof ChestBlockEntity chest) {
            boolean hasEmptySlot = false;
            for (int i = 0; i < chest.size(); i++) {
                if (chest.getStack(i).isEmpty()) {
                    hasEmptySlot = true;
                    break;
                }
            }

            if (hasEmptySlot) {
                state = State.FIND_DONKEY;
                ticks = 0;
                stopTicks = 0;
                invTickCounter = 0;
                updateRandomDelay();
                targetDonkey = null;
                targetBoat = null;
                maxDistReached = 0;
            } else {
                this.toggle();
            }
        } else {
            this.toggle();
        }
    }

    private void applyVelocity(Entity entity, boolean forward, float speed) {
        float yaw = entity.getYaw();
        float targetYaw = forward ? yaw : yaw + 180;
        double rad = Math.toRadians(targetYaw);
        Vec3d moveVec = new Vec3d(-Math.sin(rad), 0, Math.cos(rad)).multiply(speed);
        entity.setVelocity(moveVec.x, entity.getVelocity().y, moveVec.z);
        if (entity instanceof net.minecraft.client.network.ClientPlayerEntity) {
            ((net.minecraft.client.network.ClientPlayerEntity) entity).velocityModified = true;
        }
    }

    private void lookAt(Vec3d target) {
        lookAt(target, true);
    }

    private void lookAt(Vec3d target, boolean includePitch) {
        Vec3d dir = target.subtract(mc.player.getEyePos()).normalize();
        mc.player.setYaw((float) Math.toDegrees(Math.atan2(-dir.x, dir.z)));
        if (includePitch) {
            mc.player.setPitch((float) Math.toDegrees(Math.asin(-dir.y)));
        }
    }

    private void updateRandomDelay() {
        int maxRandom = (int) delayRandom.getValue();
        this.currentRandomDelay = (int) invDelay.getValue() + (maxRandom > 0 ? random.nextInt(maxRandom + 1) : 0);
    }

    private void pressForward() {
        mc.options.forwardKey.setPressed(true);
    }

    private void releaseAllKeys() {
        mc.options.forwardKey.setPressed(false);
        mc.options.backKey.setPressed(false);
        mc.options.leftKey.setPressed(false);
        mc.options.rightKey.setPressed(false);
        mc.options.sneakKey.setPressed(false);
    }

    public void setMarkedChest(BlockPos pos) {
        this.markedChest = pos;
    }
}

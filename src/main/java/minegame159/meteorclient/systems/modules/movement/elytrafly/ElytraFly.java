/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.systems.modules.movement.elytrafly;

import meteordevelopment.orbit.EventHandler;
import minegame159.meteorclient.MeteorClient;
import minegame159.meteorclient.events.entity.player.PlayerMoveEvent;
import minegame159.meteorclient.events.packets.PacketEvent;
import minegame159.meteorclient.events.world.TickEvent;
import minegame159.meteorclient.mixininterface.IVec3d;
import minegame159.meteorclient.settings.*;
import minegame159.meteorclient.systems.modules.Categories;
import minegame159.meteorclient.systems.modules.Module;
import minegame159.meteorclient.systems.modules.Modules;
import minegame159.meteorclient.systems.modules.movement.elytrafly.modes.Packet;
import minegame159.meteorclient.systems.modules.movement.elytrafly.modes.Vanilla;
import minegame159.meteorclient.systems.modules.player.ChestSwap;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ElytraItem;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

public class ElytraFly extends Module {
    public enum ChestSwapMode {
        Always,
        Never,
        WaitForGround
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgAutopilot = settings.createGroup("Autopilot");

    // General

    public final Setting<ElytraFlightModes> flightMode = sgGeneral.add(new EnumSetting.Builder<ElytraFlightModes>()
            .name("mode")
            .description("The mode of flying.")
            .defaultValue(ElytraFlightModes.Vanilla)
            .onModuleActivated(flightModesSetting -> onModeChanged(flightModesSetting.get()))
            .onChanged(this::onModeChanged)
            .build()
    );

    public final Setting<Boolean> autoTakeOff = sgGeneral.add(new BoolSetting.Builder()
            .name("auto-take-off")
            .description("Automatically takes off when you hold jump without needing to double jump.")
            .defaultValue(false)
            .build()
    );

    public final Setting<Boolean> replace = sgGeneral.add(new BoolSetting.Builder()
            .name("elytra-replace")
            .description("Replaces broken elytra with a new elytra.")
            .defaultValue(false)
            .build()
    );

    public final Setting<Integer> replaceDurability = sgGeneral.add(new IntSetting.Builder()
            .name("replace-durability")
            .description("The durability threshold your elytra will be replaced at.")
            .defaultValue(2)
            .min(1)
            .max(Items.ELYTRA.getMaxDamage() - 1)
            .sliderMax(20)
            .build()
    );

    public final Setting<Double> fallMultiplier = sgGeneral.add(new DoubleSetting.Builder()
            .name("fall-multiplier")
            .description("Controls how fast will you go down naturally.")
            .defaultValue(0.01)
            .min(0)
            .build()
    );

    public final Setting<Double> horizontalSpeed = sgGeneral.add(new DoubleSetting.Builder()
            .name("horizontal-speed")
            .description("How fast you go forward and backward.")
            .defaultValue(1)
            .min(0)
            .build()
    );

    public final Setting<Double> verticalSpeed = sgGeneral.add(new DoubleSetting.Builder()
            .name("vertical-speed")
            .description("How fast you go up and down.")
            .defaultValue(1)
            .min(0)
            .build()
    );

    public final Setting<Boolean> stopInWater = sgGeneral.add(new BoolSetting.Builder()
            .name("stop-in-water")
            .description("Stops flying in water.")
            .defaultValue(true)
            .build()
    );

    public final Setting<Boolean> dontGoIntoUnloadedChunks = sgGeneral.add(new BoolSetting.Builder()
            .name("no-unloaded-chunks")
            .description("Stops you from going into unloaded chunks.")
            .defaultValue(true)
            .build()
    );

    public final Setting<Boolean> noCrash = sgGeneral.add(new BoolSetting.Builder()
            .name("no-crash")
            .description("Stops you from going into walls.")
            .defaultValue(true)
            .build()
    );

    public final Setting<Integer> crashLookAhead = sgGeneral.add(new IntSetting.Builder()
            .name("crash-look-ahead")
            .description("Distance to look ahead when flying.")
            .defaultValue(5)
            .min(1)
            .max(15)
            .sliderMin(1)
            .sliderMax(10)
            .build()
    );

    public final Setting<ChestSwapMode> chestSwap = sgGeneral.add(new EnumSetting.Builder<ChestSwapMode>()
            .name("chest-swap")
            .description("Enables ChestSwap when toggling this module.")
            .defaultValue(ChestSwapMode.Never)
            .build()
    );

    private final Setting<Boolean> instaDrop = sgGeneral.add(new BoolSetting.Builder()
            .name("insta-drop")
            .description("Makes you drop out of flight instantly.")
            .defaultValue(false)
            .build()
    );


    // Autopilot

    private final Setting<Boolean> enableAutopilot = sgAutopilot.add(new BoolSetting.Builder()
            .name("enable-autopilot")
            .description("Use autopilot.")
            .defaultValue(false)
            .build()
    );


    public final Setting<Boolean> useFireworks = sgAutopilot.add(new BoolSetting.Builder()
            .name("use-fireworks")
            .description("Uses firework rockets every second of your choice.")
            .defaultValue(false)
            .build()
    );

    public final Setting<Double> autoPilotFireworkDelay = sgAutopilot.add(new DoubleSetting.Builder()
            .name("firework-delay")
            .description("The delay in seconds in between using fireworks if \"Use Fireworks\" is enabled.")
            .min(1)
            .defaultValue(10)
            .sliderMax(20)
            .build()
    );

    public final Setting<Boolean> moveForward = sgAutopilot.add(new BoolSetting.Builder()
            .name("move-forward")
            .description("Moves forward while elytra flying.")
            .defaultValue(false)
            .build()
    );

    public final Setting<Double> autoPilotMinimumHeight = sgAutopilot.add(new DoubleSetting.Builder()
            .name("minimum-height")
            .description("The minimum height for moving forward.")
            .defaultValue(120)
            .min(0)
            .sliderMax(260)
            .build()
    );

    private ElytraFlightMode currentMode;

    public ElytraFly() {
        super(Categories.Movement, "elytra-fly", "Gives you more control over your elytra.");
    }

    @Override
    public void onActivate() {
        currentMode.onActivate();
        if ((chestSwap.get() == ChestSwapMode.Always || chestSwap.get() == ChestSwapMode.WaitForGround)
                && mc.player.getEquippedStack(EquipmentSlot.CHEST).getItem() != Items.ELYTRA) {
            Modules.get().get(ChestSwap.class).swap();
        }
    }

    @Override
    public void onDeactivate() {
        if (moveForward.get()) mc.options.keyForward.setPressed(false);

        if (chestSwap.get() == ChestSwapMode.Always && mc.player.getEquippedStack(EquipmentSlot.CHEST).getItem() == Items.ELYTRA) {
            Modules.get().get(ChestSwap.class).swap();
        } else if (chestSwap.get() == ChestSwapMode.WaitForGround) {
            enableGroundListener();
        }

        if (mc.player.isFallFlying() && instaDrop.get()) {
            enableInstaDropListener();
        }

        currentMode.onDeactivate();
    }

    @EventHandler
    private void onPlayerMove(PlayerMoveEvent event) {
        if (!(mc.player.getEquippedStack(EquipmentSlot.CHEST).getItem() instanceof ElytraItem)) return;

        currentMode.autoTakeoff();

        if (mc.player.isFallFlying()) {
            currentMode.velX = 0;
            currentMode.velY = event.movement.y;
            currentMode.velZ = 0;
            currentMode.forward = Vec3d.fromPolar(0, mc.player.yaw).multiply(0.1);
            currentMode.right = Vec3d.fromPolar(0, mc.player.yaw + 90).multiply(0.1);

            // Handle stopInWater
            if (mc.player.isTouchingWater() && stopInWater.get()) {
                mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
                return;
            }

            currentMode.handleFallMultiplier();
            if (enableAutopilot.get()) {
                currentMode.handleAutopilot();
            }
            currentMode.handleHorizontalSpeed();
            currentMode.handleVerticalSpeed();

            int chunkX = (int) ((mc.player.getX() + currentMode.velX) / 16);
            int chunkZ = (int) ((mc.player.getZ() + currentMode.velZ) / 16);
            if (dontGoIntoUnloadedChunks.get()) {
                if (mc.world.getChunkManager().isChunkLoaded(chunkX, chunkZ)) {
                    ((IVec3d) event.movement).set(currentMode.velX, currentMode.velY, currentMode.velZ);
                } else {
                    ((IVec3d) event.movement).set(0, currentMode.velY, 0);
                }
            } else ((IVec3d) event.movement).set(currentMode.velX, currentMode.velY, currentMode.velZ);

            currentMode.onPlayerMove();
        } else {
            if (currentMode.lastForwardPressed) {
                mc.options.keyForward.setPressed(false);
                currentMode.lastForwardPressed = false;
            }
        }

        if (noCrash.get() && mc.player.isFallFlying()) {
            Vec3d lookAheadPos = mc.player.getPos().add(mc.player.getVelocity().normalize().multiply(crashLookAhead.get()));
            RaycastContext raycastContext = new RaycastContext(mc.player.getPos(), new Vec3d(lookAheadPos.getX(), mc.player.getY(), lookAheadPos.getZ()), RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player);
            BlockHitResult hitResult = mc.world.raycast(raycastContext);
            if (hitResult != null && hitResult.getType() == HitResult.Type.BLOCK) {
                ((IVec3d) event.movement).set(0, currentMode.velY, 0);
            }
        }
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        currentMode.onTick();
    }

    @EventHandler
    private void onPacketSend(PacketEvent.Send event) {
        currentMode.onPacketSend(event);
    }

    private void onModeChanged(ElytraFlightModes mode) {
        switch (mode) {
            case Vanilla:   currentMode = new Vanilla(); break;
            case Packet:    currentMode = new Packet(); break;
        }
    }

    //Ground
    private class StaticGroundListener {
        @EventHandler
        private void chestSwapGroundListener(PlayerMoveEvent event) {
            if (mc.player != null && mc.player.isOnGround()) {
                if (mc.player.getEquippedStack(EquipmentSlot.CHEST).getItem() == Items.ELYTRA) {
                    Modules.get().get(ChestSwap.class).swap();
                    disableGroundListener();
                }
            }
        }
    }

    private final StaticGroundListener staticGroundListener = new StaticGroundListener();

    protected void enableGroundListener() {
        MeteorClient.EVENT_BUS.subscribe(staticGroundListener);
    }

    protected void disableGroundListener() {
        MeteorClient.EVENT_BUS.unsubscribe(staticGroundListener);
    }

    //Drop
    private class StaticInstaDropListener {
        @EventHandler
        private void onInstadropTick(TickEvent.Post event) {
            if (mc.player != null && mc.player.isFallFlying()) {
                mc.player.setVelocity(0, 0, 0);
                mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket(true));
            } else {
                disableInstaDropListener();
            }
        }
    }

    private final StaticInstaDropListener staticInstadropListener = new StaticInstaDropListener();

    protected void enableInstaDropListener() {
        MeteorClient.EVENT_BUS.subscribe(staticInstadropListener);
    }

    protected void disableInstaDropListener() {
        MeteorClient.EVENT_BUS.unsubscribe(staticInstadropListener);
    }

    @Override
    public String getInfoString() {
        return currentMode.getHudString();
    }
}
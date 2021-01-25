/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.modules.movement;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.MeteorClient;
import minegame159.meteorclient.events.entity.player.PlayerMoveEvent;
import minegame159.meteorclient.events.world.TickEvent;
import minegame159.meteorclient.mixininterface.IKeyBinding;
import minegame159.meteorclient.mixininterface.IVec3d;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.modules.player.ChestSwap;
import minegame159.meteorclient.settings.*;
import minegame159.meteorclient.utils.player.ChatUtils;
import minegame159.meteorclient.utils.player.InvUtils;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ElytraItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;

public class ElytraPlus extends Module {
    public enum Mode {
        Normal,
        Packet
    }

    public enum ChestSwapMode {
        Always,
        Never,
        WaitForGround
    }

    public enum AutoPilotMode {
        None,
        Forward,
        Firework
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgAutopilot = settings.createGroup("Autopilot");

    // General

    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
            .name("mode")
            .description("The mode used to make you fly.")
            .defaultValue(Mode.Normal)
            .build()
    );

    private final Setting<Boolean> autoTakeOff = sgGeneral.add(new BoolSetting.Builder()
            .name("auto-take-off")
            .description("Automatically takes off when you hold jump without needing to double jump.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> replace = sgGeneral.add(new BoolSetting.Builder()
            .name("elytra-replace")
            .description("Replaces broken elytra with a new elytra.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Integer> replaceDurability = sgGeneral.add(new IntSetting.Builder()
            .name("replace-durability")
            .description("The durability threshold your elytra will be replaced at.")
            .defaultValue(2)
            .min(1)
            .max(Items.ELYTRA.getMaxDamage() - 1)
            .sliderMax(20)
            .build()
    );

    private final Setting<Double> fallMultiplier = sgGeneral.add(new DoubleSetting.Builder()
            .name("fall-multiplier")
            .description("Controls how fast will you go down naturally.")
            .defaultValue(0.01)
            .min(0)
            .build()
    );

    private final Setting<Double> horizontalSpeed = sgGeneral.add(new DoubleSetting.Builder()
            .name("horizontal-speed")
            .description("How fast you go forward and backward.")
            .defaultValue(1)
            .min(0)
            .build()
    );

    private final Setting<Double> verticalSpeed = sgGeneral.add(new DoubleSetting.Builder()
            .name("vertical-speed")
            .description("How fast you go up and down.")
            .defaultValue(1)
            .min(0)
            .build()
    );

    private final Setting<Boolean> stopInWater = sgGeneral.add(new BoolSetting.Builder()
            .name("stop-in-water")
            .description("Stops flying in water.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> dontGoIntoUnloadedChunks = sgGeneral.add(new BoolSetting.Builder()
            .name("no-unloaded-chunks")
            .description("Stops you from going into unloaded chunks.")
            .defaultValue(true)
            .build()
    );

    private final Setting<ChestSwapMode> chestSwap = sgGeneral.add(new EnumSetting.Builder<ChestSwapMode>()
            .name("chest-swap")
            .description("Enables ChestSwap when toggling this module.")
            .defaultValue(ChestSwapMode.Never)
            .build()
    );

    // Autopilot

    private final Setting<AutoPilotMode> autoPilotMode = sgAutopilot.add(new EnumSetting.Builder<AutoPilotMode>()
            .name("autopilot-mode")
            .description("Automates whatever mode you choose.")
            .defaultValue(AutoPilotMode.None)
            .build()
    );

    private final Setting<Double> autoPilotMinimumHeight = sgAutopilot.add(new DoubleSetting.Builder()
            .name("minimum-height")
            .description("The minimum height for autopilot.")
            .defaultValue(160)
            .min(0)
            .sliderMax(260)
            .build()
    );

    private final Setting<Integer> autoPilotFireworkDelay = sgAutopilot.add(new IntSetting.Builder()
            .name("firework-delay")
            .description("The delay in seconds in between shooting fireworks for Firework mode.")
            .min(1)
            .defaultValue(10)
            .sliderMax(20)
            .build()
    );

    private final Setting<Boolean> autoPilotFireworkGhosthand = sgAutopilot.add(new BoolSetting.Builder()
            .name("firework-ghost-hand")
            .description("Doesn't switch to your firework slot client-side.")
            .defaultValue(false)
            .build()
    );

    private boolean lastJumpPressed;
    private boolean incrementJumpTimer;
    private int jumpTimer;

    private double velX, velY, velZ;
    private Vec3d forward, right;

    private boolean decrementFireworkTimer;
    private int fireworkTimer;

    private boolean lastForwardPressed;
    int ticksLeft = autoPilotFireworkDelay.get() * 20;

    public ElytraPlus() {
        super(Category.Movement, "Elytra+", "Gives you more control over your elytra.");
    }

    @Override
    public void onActivate() {
        lastJumpPressed = false;
        jumpTimer = 0;
        ticksLeft = 0;
        if ((chestSwap.get() == ChestSwapMode.Always || chestSwap.get() == ChestSwapMode.WaitForGround) && mc.player.getEquippedStack(EquipmentSlot.CHEST).getItem() != Items.ELYTRA) {
            ModuleManager.INSTANCE.get(ChestSwap.class).swap();
        }
    }

    @Override
    public void onDeactivate() {
        if (autoPilotMode.get() == AutoPilotMode.Forward) ((IKeyBinding) mc.options.keyForward).setPressed(false);

        if (chestSwap.get() == ChestSwapMode.Always && mc.player.getEquippedStack(EquipmentSlot.CHEST).getItem() == Items.ELYTRA) {
            ModuleManager.INSTANCE.get(ChestSwap.class).swap();
        } else if (chestSwap.get() == ChestSwapMode.WaitForGround)
            enableGroundListener();
    }

    @EventHandler
    private final Listener<PlayerMoveEvent> onPlayerMove = new Listener<>(event -> {
        if (!(mc.player.getEquippedStack(EquipmentSlot.CHEST).getItem() instanceof ElytraItem)) return;

        handleAutoTakeOff();

        if (mc.player.isFallFlying()) {
            velX = 0;
            velY = event.movement.y;
            velZ = 0;
            forward = Vec3d.fromPolar(0, mc.player.yaw).multiply(0.1);
            right = Vec3d.fromPolar(0, mc.player.yaw + 90).multiply(0.1);

            // Handle stopInWater
            if (mc.player.isTouchingWater() && stopInWater.get()) {
                mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
                return;
            }

            handleFallMultiplier();
            handleAutopilot();

            handleHorizontalSpeed();
            handleVerticalSpeed();

            int chunkX = (int) ((mc.player.getX() + velX) / 16);
            int chunkZ = (int) ((mc.player.getZ() + velZ) / 16);
            if (dontGoIntoUnloadedChunks.get()) {
                if (mc.world.getChunkManager().isChunkLoaded(chunkX, chunkZ)) {
                    ((IVec3d) event.movement).set(velX, velY, velZ);
                } else {
                    ((IVec3d) event.movement).set(0, velY, 0);
                }
            } else ((IVec3d) event.movement).set(velX, velY, velZ);
        } else {
            if (lastForwardPressed) {
                ((IKeyBinding) mc.options.keyForward).setPressed(false);
                lastForwardPressed = false;
            }
        }
    });

    @EventHandler
    private final Listener<TickEvent.Post> onTick = new Listener<>(event -> {
        if (decrementFireworkTimer) {
            if (fireworkTimer <= 0) decrementFireworkTimer = false;

            fireworkTimer--;
        }
        if (replace.get()) {
            if (mc.player.inventory.getArmorStack(2).getItem() == Items.ELYTRA) {
                if (mc.player.inventory.getArmorStack(2).getMaxDamage() - mc.player.inventory.getArmorStack(2).getDamage() <= replaceDurability.get()) {
                    int slot = -1;
                    for (int i = 9; i < 45; i++) {
                        ItemStack stack = mc.player.inventory.getStack(i);
                        if (stack.getItem() == Items.ELYTRA && stack.getMaxDamage() - stack.getDamage() > replaceDurability.get()) {
                            slot = i;
                        }
                    }
                    if (slot != -1) {
                        InvUtils.clickSlot(slot, 0, SlotActionType.PICKUP);
                        InvUtils.clickSlot(6, 0, SlotActionType.PICKUP);
                        InvUtils.clickSlot(slot, 0, SlotActionType.PICKUP);
                    }
                }
            }
        }
        if (mode.get() == Mode.Packet && mc.player.inventory.getArmorStack(2).getItem() == Items.ELYTRA && mc.player.fallDistance > 0.2 && !mc.options.keySneak.isPressed()) {
            Vec3d vec3d = new Vec3d(0, 0, 0);

            if (mc.options.keyForward.isPressed()) {
                vec3d.add(0, 0, horizontalSpeed.get());
                vec3d.rotateY(-(float) Math.toRadians(mc.player.yaw));
            } else if (mc.options.keyBack.isPressed()) {
                vec3d.add(0, 0, horizontalSpeed.get());
                vec3d.rotateY((float) Math.toRadians(mc.player.yaw));
            }

            if (mc.options.keyJump.isPressed()) {
                vec3d.add(0, verticalSpeed.get(), 0);
            } else if (!mc.options.keyJump.isPressed()) {
                vec3d.add(0, -verticalSpeed.get(), 0);
            }

            mc.player.setVelocity(vec3d);
            mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket(true));
        }
    });

    private void handleAutopilot() {
        int slot = InvUtils.findItemInHotbar(Items.FIREWORK_ROCKET, itemStack -> true);
        int prevSlot = mc.player.inventory.selectedSlot;
        if (autoPilotMode.get() == AutoPilotMode.Forward) {
            ((IKeyBinding) mc.options.keyForward).setPressed(true);

            if (mc.player.getY() < autoPilotMinimumHeight.get() && !decrementFireworkTimer) {
                if (slot == -1 && mc.player.getOffHandStack().getItem() != Items.FIREWORK_ROCKET) {
                    if (mc.player.getOffHandStack().getItem() != Items.FIREWORK_ROCKET) mc.player.inventory.selectedSlot = slot;
                    mc.interactionManager.interactItem(mc.player, mc.world, getFireworkHand());
                    mc.player.swingHand(getFireworkHand());
                    if (autoPilotFireworkGhosthand.get()) mc.player.inventory.selectedSlot = prevSlot;

                    decrementFireworkTimer = true;
                    fireworkTimer = 20;
                } else {
                    ChatUtils.moduleWarning(this, "No fireworks found in hotbar... Changing AutoPilot mode to NONE.");
                    autoPilotMode.set(AutoPilotMode.None);
                }
            }

            if (fireworkTimer > 0) {
                velY = 2;
            }
            lastForwardPressed = true;
        } else if (autoPilotMode.get() == AutoPilotMode.Firework) {
            ticksLeft--;
            if (!mc.player.isFallFlying()) return;
            if (slot == -1 && mc.player.getOffHandStack().getItem() != Items.FIREWORK_ROCKET) {
                ChatUtils.moduleWarning(this, "No fireworks found in hotbar... Changing AutoPilot mode to NONE.");
                autoPilotMode.set(AutoPilotMode.None);
            }
            if (ticksLeft <= 0) {
                ticksLeft = autoPilotFireworkDelay.get() * 20;
                if (mc.player.getOffHandStack().getItem() != Items.FIREWORK_ROCKET) mc.player.inventory.selectedSlot = slot;

                mc.interactionManager.interactItem(mc.player, mc.world, getFireworkHand());
                mc.player.swingHand(getFireworkHand());
                if (autoPilotFireworkGhosthand.get()) mc.player.inventory.selectedSlot = prevSlot;
            }
        }
    }

    private void handleHorizontalSpeed() {
        boolean a = false;
        boolean b = false;

        if (mc.options.keyForward.isPressed()) {
            velX += forward.x * horizontalSpeed.get() * 10;
            velZ += forward.z * horizontalSpeed.get() * 10;
            a = true;
        } else if (mc.options.keyBack.isPressed()) {
            velX -= forward.x * horizontalSpeed.get() * 10;
            velZ -= forward.z * horizontalSpeed.get() * 10;
            a = true;
        }

        if (mc.options.keyRight.isPressed()) {
            velX += right.x * horizontalSpeed.get() * 10;
            velZ += right.z * horizontalSpeed.get() * 10;
            b = true;
        } else if (mc.options.keyLeft.isPressed()) {
            velX -= right.x * horizontalSpeed.get() * 10;
            velZ -= right.z * horizontalSpeed.get() * 10;
            b = true;
        }

        if (a && b) {
            double diagonal = 1 / Math.sqrt(2);
            velX *= diagonal;
            velZ *= diagonal;
        }
    }

    private void handleVerticalSpeed() {
        if (mc.options.keyJump.isPressed()) {
            velY += 0.5 * verticalSpeed.get();
        } else if (mc.options.keySneak.isPressed()) {
            velY -= 0.5 * verticalSpeed.get();
        }
    }

    private void handleFallMultiplier() {
        if (velY < 0) velY *= fallMultiplier.get();
        else if (velY > 0) velY = 0;
    }

    private void handleAutoTakeOff() {
        if (incrementJumpTimer) jumpTimer++;

        boolean jumpPressed = mc.options.keyJump.isPressed();

        if (autoTakeOff.get() && jumpPressed) {
            if (!lastJumpPressed && !mc.player.isFallFlying()) {
                jumpTimer = 0;
                incrementJumpTimer = true;
            }

            if (jumpTimer >= 8) {
                jumpTimer = 0;
                incrementJumpTimer = false;
                mc.player.setJumping(false);
                mc.player.setSprinting(true);
                mc.player.jump();
                mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
            }
        }

        lastJumpPressed = jumpPressed;
    }

    private final Listener<PlayerMoveEvent> chestSwapGroundListener = new Listener<>(event -> {
        if (mc.player != null && mc.player.isOnGround()) {
            if (mc.player.getEquippedStack(EquipmentSlot.CHEST).getItem() == Items.ELYTRA) {
                ModuleManager.INSTANCE.get(ChestSwap.class).swap();
                disableGroundListener();
            }
        }
    });

    protected void enableGroundListener() {
        MeteorClient.EVENT_BUS.subscribe(chestSwapGroundListener);
    }

    protected void disableGroundListener() {
        MeteorClient.EVENT_BUS.unsubscribe(chestSwapGroundListener);
    }

    private Hand getFireworkHand() {
        assert mc.player != null;
        Hand hand = Hand.MAIN_HAND;
        if (mc.player.getMainHandStack().getItem() != Items.FIREWORK_ROCKET && mc.player.getOffHandStack().getItem() == Items.FIREWORK_ROCKET) {
            hand = Hand.OFF_HAND;
        }
        return hand;
    }
}
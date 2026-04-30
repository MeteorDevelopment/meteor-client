/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.movement.elytrafly;

import meteordevelopment.meteorclient.events.entity.player.PlayerMoveEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

public class ElytraFlightMode {
    protected final Minecraft mc;
    protected final ElytraFly elytraFly;
    private final ElytraFlightModes type;

    protected boolean lastJumpPressed;
    protected boolean incrementJumpTimer;
    protected boolean lastForwardPressed;
    protected int jumpTimer;
    protected double velX, velY, velZ;
    protected double ticksLeft;
    protected Vec3 forward, right;
    protected double acceleration;

    public ElytraFlightMode(ElytraFlightModes type) {
        this.elytraFly = Modules.get().get(ElytraFly.class);
        this.mc = Minecraft.getInstance();
        this.type = type;
    }

    public void onTick() {
        if (elytraFly.autoReplenish.get()) {
            FindItemResult fireworks = InvUtils.find(Items.FIREWORK_ROCKET);

            if (fireworks.found() && !fireworks.isHotbar()) {
                InvUtils.move().from(fireworks.slot()).toHotbar(elytraFly.replenishSlot.get() - 1);
            }
        }

        if (elytraFly.replace.get()) {
            ItemStack chestStack = mc.player.getItemBySlot(EquipmentSlot.CHEST);

            if (chestStack.getItem() == Items.ELYTRA) {
                if (chestStack.getMaxDamage() - chestStack.getDamageValue() <= elytraFly.replaceDurability.get()) {
                    FindItemResult elytra = InvUtils.find(stack -> stack.getMaxDamage() - stack.getDamageValue() > elytraFly.replaceDurability.get() && stack.getItem() == Items.ELYTRA);

                    InvUtils.move().from(elytra.slot()).toArmor(2);
                }
            }
        }
    }

    public void onPreTick() {
    }

    public void onPacketSend(PacketEvent.Send event) {
    }

    public void onPacketReceive(PacketEvent.Receive event) {
    }

    public void onPlayerMove() {
    }

    public void onActivate() {
        lastJumpPressed = false;
        jumpTimer = 0;
        ticksLeft = 0;
        acceleration = 0;
    }

    public void onDeactivate() {
    }

    public void autoTakeoff() {
        if (incrementJumpTimer) jumpTimer++;

        boolean jumpPressed = mc.options.keyJump.isDown();

        if ((elytraFly.autoTakeOff.get() && elytraFly.flightMode.get() != ElytraFlightModes.Pitch40 && elytraFly.flightMode.get() != ElytraFlightModes.Bounce) ||
            (!elytraFly.manualTakeoff.get() && elytraFly.flightMode.get() == ElytraFlightModes.Bounce) && jumpPressed) {
            if (!lastJumpPressed && !mc.player.isFallFlying()) {
                jumpTimer = 0;
                incrementJumpTimer = true;
            }

            if (jumpTimer >= 8) {
                jumpTimer = 0;
                incrementJumpTimer = false;
                mc.player.setJumping(false);
                mc.player.setSprinting(true);
                mc.player.jumpFromGround();
                mc.getConnection().send(new ServerboundPlayerCommandPacket(mc.player, ServerboundPlayerCommandPacket.Action.START_FALL_FLYING));
            }
        }

        lastJumpPressed = jumpPressed;
    }

    public void handleAutopilot() {
        if (!mc.player.isFallFlying()) return;

        if (elytraFly.autoPilot.get() && mc.player.getY() > elytraFly.autoPilotMinimumHeight.get() && elytraFly.flightMode.get() != ElytraFlightModes.Bounce) {
            mc.options.keyUp.setDown(true);
            lastForwardPressed = true;
        }

        if (elytraFly.useFireworks.get()) {
            if (ticksLeft <= 0) {
                ticksLeft = elytraFly.autoPilotFireworkDelay.get() * 20;

                FindItemResult itemResult = InvUtils.findInHotbar(Items.FIREWORK_ROCKET);
                if (!itemResult.found()) return;

                if (itemResult.isOffhand()) {
                    mc.gameMode.useItem(mc.player, InteractionHand.OFF_HAND);
                    mc.player.swing(InteractionHand.OFF_HAND);
                } else {
                    InvUtils.swap(itemResult.slot(), true);

                    mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
                    mc.player.swing(InteractionHand.MAIN_HAND);

                    InvUtils.swapBack();
                }
            }
            ticksLeft--;
        }
    }

    public void handleHorizontalSpeed(PlayerMoveEvent event) {
        boolean a = false;
        boolean b = false;

        if (mc.options.keyUp.isDown()) {
            velX += forward.x * getSpeed() * 10;
            velZ += forward.z * getSpeed() * 10;
            a = true;
        } else if (mc.options.keyDown.isDown()) {
            velX -= forward.x * getSpeed() * 10;
            velZ -= forward.z * getSpeed() * 10;
            a = true;
        }

        if (mc.options.keyRight.isDown()) {
            velX += right.x * getSpeed() * 10;
            velZ += right.z * getSpeed() * 10;
            b = true;
        } else if (mc.options.keyLeft.isDown()) {
            velX -= right.x * getSpeed() * 10;
            velZ -= right.z * getSpeed() * 10;
            b = true;
        }

        if (a && b) {
            double diagonal = 1 / Math.sqrt(2);
            velX *= diagonal;
            velZ *= diagonal;
        }
    }

    public void handleVerticalSpeed(PlayerMoveEvent event) {
        if (mc.options.keyJump.isDown()) velY += 0.5 * elytraFly.verticalSpeed.get();
        else if (mc.options.keyShift.isDown()) velY -= 0.5 * elytraFly.verticalSpeed.get();
    }

    public void handleFallMultiplier() {
        if (velY < 0) velY *= elytraFly.fallMultiplier.get();
        else if (velY > 0) velY = 0;
    }

    public void handleAcceleration() {
        if (elytraFly.acceleration.get()) {
            if (!PlayerUtils.isMoving()) acceleration = 0;
            acceleration = Math.min(
                acceleration + elytraFly.accelerationMin.get() + elytraFly.accelerationStep.get() * .1,
                elytraFly.horizontalSpeed.get()
            );
        } else {
            acceleration = 0;
        }
    }

    public void zeroAcceleration() {
        acceleration = 0;
    }

    protected double getSpeed() {
        return elytraFly.acceleration.get() ? acceleration : elytraFly.horizontalSpeed.get();
    }

    public String getHudString() {
        return type.name();
    }
}

/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.movement.elytrafly;

import meteordevelopment.meteorclient.events.entity.player.PlayerMoveEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;

public class ElytraFlightMode {
    protected final MinecraftClient mc;
    protected final ElytraFly elytraFly;
    private final ElytraFlightModes type;

    protected boolean lastJumpPressed;
    protected boolean incrementJumpTimer;
    protected boolean lastForwardPressed;
    protected int jumpTimer;
    protected double velX, velY, velZ;
    protected double ticksLeft;
    protected Vec3d forward, right;

    public ElytraFlightMode(ElytraFlightModes type) {
        this.elytraFly = Modules.get().get(ElytraFly.class);
        this.mc = MinecraftClient.getInstance();
        this.type = type;
    }

    public void onTick() {
        if (elytraFly.autoReplenish.get()) {
            FindItemResult fireworks = InvUtils.find(Items.FIREWORK_ROCKET);
            FindItemResult hotbarFireworks = InvUtils.findInHotbar(Items.FIREWORK_ROCKET);

            if (!hotbarFireworks.found() && fireworks.found()) {
                InvUtils.move().from(fireworks.slot()).toHotbar(elytraFly.replenishSlot.get() - 1);
            }
        }

        if (elytraFly.replace.get()) {
            ItemStack chestStack = mc.player.getInventory().getArmorStack(2);

            if (chestStack.getItem() == Items.ELYTRA) {
                if (chestStack.getMaxDamage() - chestStack.getDamage() <= elytraFly.replaceDurability.get()) {
                    FindItemResult elytra = InvUtils.find(stack -> stack.getMaxDamage() - stack.getDamage() > elytraFly.replaceDurability.get() && stack.getItem() == Items.ELYTRA);

                    InvUtils.move().from(elytra.slot()).toArmor(2);
                }
            }
        }
    }

    public void onPacketSend(PacketEvent.Send event) {}

    public void onPlayerMove() {}

    public void onActivate() {
        lastJumpPressed = false;
        jumpTimer = 0;
        ticksLeft = 0;
    }

    public void onDeactivate() {}

    public void autoTakeoff() {
        if (incrementJumpTimer) jumpTimer++;

        boolean jumpPressed = mc.options.keyJump.isPressed();

        if (elytraFly.autoTakeOff.get() && jumpPressed) {
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

    public void handleAutopilot() {
        if (!mc.player.isFallFlying()) return;

        if (elytraFly.autoPilot.get() && mc.player.getY() > elytraFly.autoPilotMinimumHeight.get()) {
            mc.options.keyForward.setPressed(true);
            lastForwardPressed = true;
        }

        if (elytraFly.useFireworks.get()) {
            if (ticksLeft <= 0) {
                ticksLeft = elytraFly.autoPilotFireworkDelay.get() * 20;

                FindItemResult itemResult = InvUtils.findInHotbar(Items.FIREWORK_ROCKET);
                if (!itemResult.found()) return;

                if (itemResult.isOffhand()) {
                    mc.interactionManager.interactItem(mc.player, mc.world, Hand.OFF_HAND);
                    mc.player.swingHand(Hand.OFF_HAND);
                } else {
                    InvUtils.swap(itemResult.slot(), true);

                    mc.interactionManager.interactItem(mc.player, mc.world, Hand.MAIN_HAND);
                    mc.player.swingHand(Hand.MAIN_HAND);

                    InvUtils.swapBack();
                }
            }
            ticksLeft--;
        }
    }

    public void handleHorizontalSpeed(PlayerMoveEvent event) {
        boolean a = false;
        boolean b = false;

        if (mc.options.keyForward.isPressed()) {
            velX += forward.x * elytraFly.horizontalSpeed.get() * 10;
            velZ += forward.z * elytraFly.horizontalSpeed.get() * 10;
            a = true;
        } else if (mc.options.keyBack.isPressed()) {
            velX -= forward.x * elytraFly.horizontalSpeed.get() * 10;
            velZ -= forward.z * elytraFly.horizontalSpeed.get() * 10;
            a = true;
        }

        if (mc.options.keyRight.isPressed()) {
            velX += right.x * elytraFly.horizontalSpeed.get() * 10;
            velZ += right.z * elytraFly.horizontalSpeed.get() * 10;
            b = true;
        } else if (mc.options.keyLeft.isPressed()) {
            velX -= right.x * elytraFly.horizontalSpeed.get() * 10;
            velZ -= right.z * elytraFly.horizontalSpeed.get() * 10;
            b = true;
        }

        if (a && b) {
            double diagonal = 1 / Math.sqrt(2);
            velX *= diagonal;
            velZ *= diagonal;
        }
    }

    public void handleVerticalSpeed(PlayerMoveEvent event) {
        if (mc.options.keyJump.isPressed()) velY += 0.5 * elytraFly.verticalSpeed.get();
        else if (mc.options.keySneak.isPressed()) velY -= 0.5 * elytraFly.verticalSpeed.get();
    }

    public void handleFallMultiplier() {
        if (velY < 0) velY *= elytraFly.fallMultiplier.get();
        else if (velY > 0) velY = 0;
    }

    public String getHudString() {
        return type.name();
    }
}

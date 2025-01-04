/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.player;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class AntiAFK extends Module {
    private final SettingGroup sgActions = settings.createGroup("Actions");
    private final SettingGroup sgMessages = settings.createGroup("Messages");

    // Actions

    private final Setting<Boolean> jump = sgActions.add(new BoolSetting.Builder()
        .name("jump")
        .description("Jump randomly.")
        .defaultValue(true)
        .build()
    );
    private final Setting<Integer> jumpRate = sgActions.add(new IntSetting.Builder()
        .name("jump-rate")
        .description("How many seconds between jumps (set to 0 for random).")
        .defaultValue(0)
        .min(0)
        .sliderRange(0, 60)
        .visible(jump::get)
        .build()
    );

    private final Setting<Boolean> swing = sgActions.add(new BoolSetting.Builder()
        .name("swing")
        .description("Swings your hand.")
        .defaultValue(false)
        .build()
    );
    private final Setting<Integer> swingRate = sgActions.add(new IntSetting.Builder()
        .name("swing-rate")
        .description("How many seconds between hand swings (set to 0 for random).")
        .defaultValue(0)
        .min(0)
        .sliderRange(0, 60)
        .visible(swing::get)
        .build()
    );

    private final Setting<Boolean> sneak = sgActions.add(new BoolSetting.Builder()
        .name("sneak")
        .description("Sneaks and unsneaks quickly.")
        .defaultValue(false)
        .build()
    );
    private final Setting<Integer> sneakRate = sgActions.add(new IntSetting.Builder()
        .name("sneak-rate")
        .description("How many seconds between sneaks (set to 0 for random).")
        .defaultValue(0)
        .min(0)
        .sliderRange(0, 60)
        .visible(sneak::get)
        .build()
    );
    private final Setting<Integer> sneakTime = sgActions.add(new IntSetting.Builder()
        .name("sneak-time")
        .description("How many ticks to stay sneaked.")
        .defaultValue(5)
        .min(1)
        .sliderMin(1)
        .visible(sneak::get)
        .build()
    );

    private final Setting<Boolean> strafe = sgActions.add(new BoolSetting.Builder()
        .name("strafe")
        .description("Strafe right and left.")
        .defaultValue(false)
        .onChanged(aBoolean -> {
            strafeTimer = 0;
            direction = false;

            if (isActive()) {
                mc.options.leftKey.setPressed(false);
                mc.options.rightKey.setPressed(false);
            }
        })
        .build()
    );
    private final Setting<Integer> strafeRate = sgActions.add(new IntSetting.Builder()
        .name("strafe-rate")
        .description("How many seconds before changing strafe direction (set to 0 for random).")
        .defaultValue(1)
        .min(0)
        .sliderRange(0, 60)
        .visible(strafe::get)
        .build()
    );

    private final Setting<Boolean> spin = sgActions.add(new BoolSetting.Builder()
        .name("spin")
        .description("Spins the player in place.")
        .defaultValue(true)
        .build()
    );
    private final Setting<Boolean> silentSpin = sgActions.add(new BoolSetting.Builder()
        .name("silent-spin")
        .description("Spins using silent (server-side) rotations.")
        .defaultValue(true)
        .visible(spin::get)
        .build()
    );
    private final Setting<Integer> spinSpeed = sgActions.add(new IntSetting.Builder()
        .name("spin-speed")
        .description("The speed to spin you (set to 0 for random).")
        .defaultValue(7)
        .visible(spin::get)
        .build()
    );
    private final Setting<Integer> spinPitch = sgActions.add(new IntSetting.Builder()
        .name("spin-pitch")
        .description("The pitch to send to the server.")
        .defaultValue(0)
        .range(-90, 90)
        .sliderRange(-90, 90)
        .visible(() -> spin.get() && silentSpin.get())
        .build()
    );

    private final Setting<Boolean> look = sgActions.add(new BoolSetting.Builder()
        .name("look")
        .description("Periodically rotate to look in a random direction.")
        .defaultValue(false)
        .build()
    );
    private final Setting<Boolean> silentLook = sgActions.add(new BoolSetting.Builder()
        .name("silent-look")
        .description("Looks using silent (server-side) rotations.")
        .defaultValue(true)
        .visible(this::canSilentLook)
        .build()
    );
    private final Setting<LookMode> lookMode = sgActions.add(new EnumSetting.Builder<LookMode>()
        .name("look-mode")
        .description("Where to look.")
        .defaultValue(LookMode.Random)
        .visible(look::get)
        .build()
    );
    private final Setting<Boolean> wander = sgActions.add(new BoolSetting.Builder()
        .name("wander")
        .description("Wander around in random directions.")
        .defaultValue(false)
        .visible(() -> look.get() && lookMode.get().equals(LookMode.Random))
        .onChanged(value -> {
            if (value) silentLook.set(false);
            else if (this.ticksWalking > 0) {
                ticksWalking = 0;
                mc.options.forwardKey.setPressed(false);
                mc.options.getAutoJump().setValue(this.hadAutoJump);
            }
        })
        .build()
    );
    private final Setting<Integer> wanderPitch = sgActions.add(new IntSetting.Builder()
        .name("wander-pitch")
        .description("How to set the player's pitch while wandering (set outside of slider range for random).")
        .defaultValue(420)
        .sliderRange(-90, 90)
        .visible(() -> look.get() && wander.isVisible() && wander.get())
        .build()
    );
    private final Setting<Integer> wanderRate = sgActions.add(new IntSetting.Builder()
        .name("wander-rate")
        .description("How often to wander.")
        .defaultValue(20).min(1).sliderRange(1, 420)
        .visible(() -> wander.isVisible() && wander.get())
        .build()
    );
    private final Setting<Boolean> follow = sgActions.add(new BoolSetting.Builder()
        .name("follow")
        .description("Follow your target entity.")
        .defaultValue(false)
        .visible(() -> look.get() && !lookMode.get().equals(LookMode.Random))
        .onChanged(value -> {
            if (value) {
                silentLook.set(false);
                this.lookRate.set(0);
            } else if (this.currentTarget != null) {
                mc.options.forwardKey.setPressed(false);
                mc.options.getAutoJump().setValue(this.hadAutoJump);
            }
        })
        .build()
    );
    private final Setting<Set<EntityType<?>>> targetEntities = sgActions.add(new EntityTypeListSetting.Builder()
        .name("target-entities")
        .description("Which entities to look at.")
        .defaultValue(EntityType.PLAYER, EntityType.VILLAGER,
            EntityType.ALLAY, EntityType.CAT, EntityType.WOLF, EntityType.PARROT
        )
        .visible(() -> look.get() && lookMode.get().equals(LookMode.Entity))
        .build()
    );
    private final Setting<List<String>> targetPlayers = sgActions.add(new StringListSetting.Builder()
        .name("target-players")
        .description("Which players to look at.")
        .defaultValue(List.of())
        .visible(() -> look.get() && lookMode.get().equals(LookMode.Player))
        .build()
    );
    private final Setting<Integer> lookRate = sgActions.add(new IntSetting.Builder()
        .name("look-rate")
        .description("How many seconds before looking in a new direction (set to 0 for random/tracking).")
        .defaultValue(10).min(0)
        .sliderRange(0, 60)
        .visible(look::get)
        .onChanged(value -> {
            if (value > 0) follow.set(false);
        })
        .build()
    );
    private final Setting<Integer> followDistance = sgActions.add(new IntSetting.Builder()
        .name("follow-distance")
        .description("What distance to follow entities at.")
        .defaultValue(2)
        .min(0).max(getMaxDistance()).sliderMax(getMaxDistance())
        .visible(() -> follow.isVisible() && follow.get())
        .build()
    );
    private final Setting<Integer> maxDistance = sgActions.add(new IntSetting.Builder()
        .name("maximum-entity-distance")
        .description("How far away can target entities be.")
        .defaultValue(64).min(16)
        .sliderRange(16, 256)
        .visible(() -> look.get() && !lookMode.get().equals(LookMode.Random))
        .build()
    );

    // Messages

    private final Setting<Boolean> sendMessages = sgMessages.add(new BoolSetting.Builder()
        .name("send-messages")
        .description("Sends messages to prevent getting kicked for AFK.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> randomMessage = sgMessages.add(new BoolSetting.Builder()
        .name("random")
        .description("Selects a random message from your message list.")
        .defaultValue(false)
        .visible(sendMessages::get)
        .build()
    );

    private final Setting<Integer> delay = sgMessages.add(new IntSetting.Builder()
        .name("delay")
        .description("The delay between specified messages in seconds.")
        .defaultValue(15)
        .min(0)
        .sliderMax(30)
        .visible(sendMessages::get)
        .build()
    );

    private final Setting<List<String>> messages = sgMessages.add(new StringListSetting.Builder()
        .name("messages")
        .description("The messages to choose from.")
        .defaultValue(
            "Meteor on top!",
            "Meteor on crack!"
        )
        .visible(sendMessages::get)
        .build()
    );

    public AntiAFK() {
        super(Categories.Player, "anti-afk", "Performs different actions to prevent getting kicked while AFK.");
    }

    private final Random random = new Random();
    private int messageTimer = 0;
    private int messageI = 0;
    private int jumpTimer = 0;
    private int lookTimer = 0;
    private int swingTimer = 0;
    private int sneakTimer = 0;
    private int strafeTimer = 0;
    private int ticksSneaked = 0;
    private int ticksWalking = 0;
    private boolean direction = false;
    private boolean hadAutoJump = false;
    private float prevYaw;
    private @Nullable Entity currentTarget;
    private final List<Entity> validTargets = new ObjectArrayList<>();

    @Override
    public void onActivate() {
        if (sendMessages.get() && messages.get().isEmpty()) {
            warning("Message list is empty, disabling messages...");
            sendMessages.set(false);
        }

        prevYaw = mc.player.getYaw();
        messageTimer = delay.get() * 20;
    }

    @Override
    public void onDeactivate() {
        if (strafe.get()) {
            mc.options.leftKey.setPressed(false);
            mc.options.rightKey.setPressed(false);
        }

        if ((look.get() && follow.get() && currentTarget != null)
            || (look.get() && wander.get() && ticksWalking > 0)) {
            mc.options.forwardKey.setPressed(false);
            mc.options.getAutoJump().setValue(hadAutoJump);
        }

        hadAutoJump = false;
        validTargets.clear();
        currentTarget = null;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (!Utils.canUpdate()) return;

        // Jump
        if (jump.get()) {
            if (jumpRate.get() > 0) {
                ++jumpTimer;
                if (jumpTimer >= jumpRate.get() * 20) {
                    jumpTimer = 0;
                    if (mc.player.isOnGround()) mc.player.jump();
                }
            } else if (random.nextInt(99) == 0) {
                if (mc.player.isOnGround()) mc.player.jump();
            }
        }

        // Swing
        if (swing.get()) {
            if (swingRate.get() > 0) {
                ++swingTimer;
                if (swingTimer >= swingRate.get() * 20) {
                    swingTimer = 0;
                    mc.player.swingHand(mc.player.getActiveHand());
                }
            } else if (random.nextInt(99) == 0) {
                mc.player.swingHand(mc.player.getActiveHand());
            }
        }

        // Sneak
        if (sneak.get()) {
            if (ticksSneaked++ >= sneakTime.get()) {
                if (ticksSneaked <= sneakTime.get() + 5) {
                    mc.options.sneakKey.setPressed(false);
                }
                if (sneakRate.get() > 0) {
                  ++sneakTimer;
                  if (sneakTimer >= sneakRate.get() * 20) {
                      sneakTimer = 0;
                      ticksSneaked = 0;
                  }
                } else if (random.nextInt(99) == 0) ticksSneaked = 0; // Sneak after ~5 seconds
            } else mc.options.sneakKey.setPressed(true);
        }

        // Strafe
        if (strafe.get()) {
            if (strafeRate.get() > 0) {
                ++strafeTimer;
                if (strafeTimer >= strafeRate.get() * 20) {
                    strafeTimer = 0;
                    mc.options.leftKey.setPressed(!direction);
                    mc.options.rightKey.setPressed(direction);
                    direction = !direction;
                }
            } else if (random.nextInt(42) == 0) {
                mc.options.leftKey.setPressed(!direction);
                mc.options.rightKey.setPressed(direction);
                direction = !direction;
            }
        }

        // Spin
        if (spin.get()) {
            prevYaw += spinSpeed.get() <= 0
                ? random.nextInt(42) : spinSpeed.get();

            if (!silentSpin.get()) mc.player.setYaw(prevYaw);
            else Rotations.rotate(prevYaw, spinPitch.get(), -15);
        }

        // Messages
        if (sendMessages.get() && !messages.get().isEmpty() && messageTimer-- <= 0) {
            if (randomMessage.get()) messageI = random.nextInt(messages.get().size());
            else if (++messageI >= messages.get().size()) messageI = 0;

            ChatUtils.sendPlayerMsg(messages.get().get(messageI));
            messageTimer = delay.get() * 20;
        }

        // Look
        ++lookTimer; // Guard syntax to reduce nesting
        if (lookTimer < lookRate.get() * 20 || !look.get()) return;

        lookTimer = 0;
        switch (lookMode.get()) {
            case Random -> {
                if (wander.get()) {
                    if (ticksWalking <= 0) {
                        boolean shouldWander = random.nextInt(wanderRate.get()) == 0;

                        if (shouldWander) {
                            ticksWalking = random.nextInt(69,1337);
                            mc.options.forwardKey.setPressed(true);
                            hadAutoJump = mc.options.getAutoJump().getValue();
                            mc.options.getAutoJump().setValue(true);
                        } else {
                            --ticksWalking;
                            if (ticksWalking >= -5) {
                                mc.options.forwardKey.setPressed(false);
                                mc.options.getAutoJump().setValue(false);
                            }
                        }

                    } else --ticksWalking;

                    if (wanderPitchOutOfBounds()) {
                        if (lookRate.get() > 0) lookRandomly();
                        else if (random.nextInt(99) == 0) lookRandomly();
                    } else {
                        if (lookRate.get() > 0) yawRandomly(wanderPitch.get());
                        else if (random.nextInt(99) == 0) yawRandomly(wanderPitch.get());
                    }

                } else if (lookRate.get() > 0) lookRandomly();
                else if (random.nextInt(99) == 0) lookRandomly();
            }
            case Entity, RandomEntity, Player -> {
                boolean playerMode = lookMode.get().equals(LookMode.Player);

                if (lookRate.get() > 0) {
                    if (playerMode) {
                        findValidPlayers();
                    } else {
                        findValidTargets(lookMode.get().equals(LookMode.RandomEntity));
                    }
                    if (validTargets.isEmpty()) lookRandomly();
                    else {
                        int luckyIndex = random.nextInt(validTargets.size());
                        Entity entity = validTargets.get(luckyIndex);
                        if (alreadyTargeting(entity)) {
                            lookRandomly();
                        } else {
                            lookAtEntity(entity);
                        }
                    }
                } else if (isCurrentTargetInvalid()) {
                    if (playerMode) {
                        findValidPlayers();
                    } else {
                        findValidTargets(lookMode.get().equals(LookMode.RandomEntity));
                    }
                    if (validTargets.isEmpty()) {
                      if (random.nextInt(99) == 0) lookRandomly();
                    } else {
                        int luckyIndex = random.nextInt(validTargets.size());
                        currentTarget = validTargets.get(luckyIndex);
                        lookAtEntity(currentTarget);
                    }
                } else {
                    if (follow.get() && !mc.player.getBlockPos().isWithinDistance(currentTarget.getBlockPos(), followDistance.get())) {
                        mc.options.forwardKey.setPressed(true);
                        hadAutoJump = mc.options.getAutoJump().getValue();
                        mc.options.getAutoJump().setValue(true);
                    } else if (follow.get()) {
                        mc.options.forwardKey.setPressed(false);
                    }
                    lookAtEntity(currentTarget);
                }
            }
        }
    }

    private void yawRandomly(float pitch) {
        if (silentLook.get()) {
            Rotations.rotate(random.nextFloat(360), pitch, -13);
        } else {
            mc.player.setYaw(random.nextFloat(360));
            mc.player.setPitch(pitch);
        }
    }

    private void lookRandomly() {
        if (silentLook.get()) {
            Rotations.rotate(
                random.nextFloat(360),
                random.nextFloat(-90, 90), -13
            );
        } else {
            mc.player.setYaw(random.nextFloat(360));
            mc.player.setPitch(random.nextFloat(-90, 90));
        }
    }

    private void lookAtEntity(Entity entity) {
        Vec3d targetPos = entity.getEyePos();

        if (silentLook.get()) {
            Rotations.rotate(
                Rotations.getYaw(targetPos),
                Rotations.getPitch(targetPos), -13
            );
        } else {
            mc.player.setYaw((float) Rotations.getYaw(targetPos));
            mc.player.setPitch((float) Rotations.getPitch(targetPos));
        }
    }

    private void findValidTargets(boolean random) {
        validTargets.clear();
        for (Entity entity : mc.world.getEntities()) {
            if (entity.equals(mc.player)) continue;
            if (random || targetEntities.get().contains(entity.getType())) {
                if (entity.getBlockPos().isWithinDistance(mc.player.getBlockPos(), maxDistance.get())) {
                    validTargets.add(entity);
                }
            }
        }
    }

    private void findValidPlayers() {
        validTargets.clear();
        for (Entity entity : mc.world.getEntities()) {
            if (entity.equals(mc.player)) continue;
            if (entity instanceof PlayerEntity player && targetPlayers.get().contains(player.getName().getString().toLowerCase())) {
                validTargets.add(entity);
            }
        }
    }

    private boolean isCurrentTargetInvalid() {
        return currentTarget == null
            || currentTarget.isRemoved()
            || (lookMode.get().equals(LookMode.Player) && !(currentTarget instanceof PlayerEntity))
            || (lookMode.get().equals(LookMode.Entity) && !targetEntities.get().contains(currentTarget.getType()))
            || !currentTarget.getBlockPos().isWithinDistance(mc.player.getBlockPos(), maxDistance.get())
            || (lookMode.get().equals(LookMode.Player) && !targetPlayers.get().contains(currentTarget.getName().toString().toLowerCase()));
    }

    private boolean alreadyTargeting(Entity entity) {
        int raycastDistance = maxDistance.get();

        Vec3d eyes = mc.player.getEyePos();
        Vec3d lookDirection = mc.player.getRotationVec(0f);
        Vec3d lookingTowards = eyes.add(lookDirection.multiply(raycastDistance));
        Box box = mc.player.getBoundingBox().stretch(lookDirection.multiply(raycastDistance)).expand(1.0, 1.0, 1.0);

        EntityHitResult result = ProjectileUtil.raycast(
            mc.player, eyes, lookingTowards, box,
            EntityPredicates.VALID_ENTITY, raycastDistance * raycastDistance
        );

        return result != null && result.getEntity().equals(entity);
    }

    private boolean canSilentLook() {
        return look.get()
            && !(wander.isVisible() && wander.get())
            && !(follow.isVisible() && follow.get());
    }

    private boolean wanderPitchOutOfBounds() {
        return wanderPitch.get() > 90 || wanderPitch.get() < -90;
    }

    private int getMaxDistance() {
        if (maxDistance == null) {
            return 64;
        } else return maxDistance.get();
    }

    public enum LookMode {
        Random,
        Player,
        Entity,
        RandomEntity
    }
}

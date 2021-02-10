/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.modules.combat;

import baritone.api.BaritoneAPI;
import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import meteordevelopment.orbit.EventHandler;
import minegame159.meteorclient.events.world.TickEvent;
import minegame159.meteorclient.friends.Friends;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.settings.*;
import minegame159.meteorclient.utils.entity.EntityUtils;
import minegame159.meteorclient.utils.entity.SortPriority;
import minegame159.meteorclient.utils.entity.Target;
import minegame159.meteorclient.utils.player.PlayerUtils;
import minegame159.meteorclient.utils.player.Rotations;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.SwordItem;
import net.minecraft.util.Hand;

import java.util.ArrayList;
import java.util.List;

public class KillAura extends Module {
    public enum OnlyWith {
        Sword,
        Axe,
        SwordOrAxe,
        Any
    }

    public enum RotationMode {
        Always,
        OnHit,
        None
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRotations = settings.createGroup("Rotations");
    private final SettingGroup sgDelay = settings.createGroup("Delay");

    // General

    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
            .name("range")
            .description("The maximum range the entity can be to attack it.")
            .defaultValue(4)
            .min(0)
            .max(6)
            .sliderMax(6)
            .build()
    );

    private final Setting<Object2BooleanMap<EntityType<?>>> entities = sgGeneral.add(new EntityTypeListSetting.Builder()
            .name("entities")
            .description("Entities to attack.")
            .defaultValue(new Object2BooleanOpenHashMap<>(0))
            .onlyAttackable()
            .build()
    );

    private final Setting<SortPriority> priority = sgGeneral.add(new EnumSetting.Builder<SortPriority>()
            .name("priority")
            .description("What type of entities to target.")
            .defaultValue(SortPriority.LowestHealth)
            .build()
    );

    private final Setting<OnlyWith> onlyWith = sgGeneral.add(new EnumSetting.Builder<OnlyWith>()
            .name("only-with")
            .description("Only attacks an entity when a specified item is in your hand.")
            .defaultValue(OnlyWith.Any)
            .build()
    );

    private final Setting<Boolean> ignoreWalls = sgGeneral.add(new BoolSetting.Builder()
            .name("ignore-walls")
            .description("Whether or not to attack the entity through a wall.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> friends = sgGeneral.add(new BoolSetting.Builder()
            .name("friends")
            .description("Whether or not to attack friends. Useful if you select players selected.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> babies = sgGeneral.add(new BoolSetting.Builder()
            .name("babies")
            .description("Whether or not to attack baby variants of the entity.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> nametagged = sgGeneral.add(new BoolSetting.Builder()
            .name("nametagged")
            .description("Whether or not to attack mobs with a name tag.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Double> hitChance = sgGeneral.add(new DoubleSetting.Builder()
            .name("hit-chance")
            .description("The probability of your hits landing.")
            .defaultValue(100)
            .min(0)
            .max(100)
            .sliderMax(100)
            .build()
    );

    private final Setting<Boolean> pauseOnCombat = sgGeneral.add(new BoolSetting.Builder()
            .name("pause-on-combat")
            .description("Freezes Baritone temporarily until you are finished attacking the entity.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> targetMultiple = sgGeneral.add(new BoolSetting.Builder()
            .name("target-multiple")
            .description("Target multiple entities at once")
            .defaultValue(false)
            .build()
    );

    // Rotations

    private final Setting<RotationMode> rotationMode = sgRotations.add(new EnumSetting.Builder<RotationMode>()
            .name("rotation-mode")
            .description("Determines when you should rotate towards the target.")
            .defaultValue(RotationMode.OnHit)
            .build()
    );

    private final Setting<Target> rotationDirection = sgRotations.add(new EnumSetting.Builder<Target>()
            .name("rotation-direction")
            .description("The direction to use for rotating towards the enemy.")
            .defaultValue(Target.Head)
            .build()
    );

    // Delay

    private final Setting<Boolean> smartDelay = sgDelay.add(new BoolSetting.Builder()
            .name("smart-delay")
            .description("Smart delay.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Integer> hitDelay = sgDelay.add(new IntSetting.Builder()
            .name("hit-delay")
            .description("How fast you hit the entity in ticks.")
            .defaultValue(0)
            .min(0)
            .sliderMax(60)
            .build()
    );

    private final Setting<Boolean> randomDelayEnabled = sgDelay.add(new BoolSetting.Builder()
            .name("random-delay-enabled")
            .description("Adds a random delay between hits to attempt to bypass anti-cheats.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Integer> randomDelayMax = sgDelay.add(new IntSetting.Builder()
            .name("random-delay-max")
            .description("The maximum value for random delay.")
            .defaultValue(4)
            .min(0)
            .sliderMax(20)
            .build()
    );

    private int hitDelayTimer;
    private int randomDelayTimer;
    private boolean wasPathing;
    private boolean canAttack;

    private final List<Entity> entityList = new ArrayList<>();

    public KillAura() {
        super(Category.Combat, "kill-aura", "Attacks specified entities around you.");
    }

    @Override
    public void onDeactivate() {
        hitDelayTimer = 0;
        randomDelayTimer = 0;
        entityList.clear();
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {

        if (mc.player.isDead() || !mc.player.isAlive() || !itemInHand()) {
            entityList.clear();
            return;
        }
        entityList.clear();
        EntityUtils.getList(entity -> {
            if (entity == mc.player || entity == mc.cameraEntity) return false;
            if ((entity instanceof LivingEntity && ((LivingEntity) entity).isDead()) || !entity.isAlive()) return false;
            if (entity.distanceTo(mc.player) > range.get()) return false;
            if (!entities.get().getBoolean(entity.getType())) return false;
            if (!nametagged.get() && entity.hasCustomName()) return false;
            if (!ignoreWalls.get() && !PlayerUtils.canSeeEntity(entity)) return false;
            if (entity instanceof PlayerEntity) {
                if (((PlayerEntity) entity).isCreative()) return false;
                if (!friends.get() && !Friends.get().attack((PlayerEntity) entity)) return false;
            }
            if (entity instanceof AnimalEntity && !babies.get() && ((AnimalEntity) entity).isBaby()) return false;

            return true;
        }, priority.get(), entityList);

        if (!targetMultiple.get() && !entityList.isEmpty())
            entityList.subList(1, entityList.size()).clear();

        if (entityList.isEmpty()) {
            if (wasPathing){
                BaritoneAPI.getProvider().getPrimaryBaritone().getCommandManager().execute("resume");
                wasPathing = false;
            }
            return;
        }

        if (pauseOnCombat.get() && BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().isPathing() && !wasPathing) {
            BaritoneAPI.getProvider().getPrimaryBaritone().getCommandManager().execute("pause");
            wasPathing = true;
        }

        if (smartDelay.get() && mc.player.getAttackCooldownProgress(0.5f) < 1) {
            return;
        }

        if (hitDelayTimer >= 0) {
            hitDelayTimer--;
            return;
        }
        else {
            hitDelayTimer = hitDelay.get();
        }

        if (randomDelayEnabled.get()) {
            if (randomDelayTimer > 0) {
                randomDelayTimer--;
                return;
            } else {
                randomDelayTimer = (int) Math.round(Math.random() * randomDelayMax.get());
            }
        }

        for (Entity target : entityList) {
            if (attack(target) && rotationMode.get() == RotationMode.Always) {
                Rotations.rotate(Rotations.getYaw(target), Rotations.getPitch(target, rotationDirection.get()), () -> {
                    if (canAttack) hitEntity(target);
                });
            }
        }
    }

    private boolean attack(Entity target) {
        canAttack = false;

        if (Math.random() > hitChance.get() / 100) return false;

        if (rotationMode.get() == RotationMode.None) {
            hitEntity(target);
        }
        else {
            Rotations.rotate(Rotations.getYaw(target), Rotations.getPitch(target, rotationDirection.get()), () -> hitEntity(target));
        }

        canAttack = true;
        return true;
    }

    private void hitEntity(Entity target) {
        mc.interactionManager.attackEntity(mc.player, target);
        mc.player.swingHand(Hand.MAIN_HAND);
    }

    private boolean itemInHand() {
        switch(onlyWith.get()){
            case Axe:        return mc.player.getMainHandStack().getItem() instanceof AxeItem;
            case Sword:      return mc.player.getMainHandStack().getItem() instanceof SwordItem;
            case SwordOrAxe: return mc.player.getMainHandStack().getItem() instanceof AxeItem || mc.player.getMainHandStack().getItem() instanceof SwordItem;
            default:         return true;
        }
    }

    @Override
    public String getInfoString() {
        if (!entityList.isEmpty()) {
            Entity targetFirst = entityList.get(0);
            if (targetFirst instanceof PlayerEntity)
                return targetFirst.getEntityName();
            return targetFirst.getType().getName().getString();
        }
        return null;
    }
}

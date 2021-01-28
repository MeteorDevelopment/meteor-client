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
import minegame159.meteorclient.friends.FriendManager;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.settings.*;
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
    public enum Priority {
        LowestDistance,
        HighestDistance,
        LowestHealth,
        HighestHealth
    }

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
    private final SettingGroup sgDelay = settings.createGroup("Delay");

    // General

    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
            .name("range")
            .description("The maximum range the entity can be to attack it.")
            .defaultValue(4)
            .min(0)
            .max(6)
            .build()
    );

    private final Setting<Object2BooleanMap<EntityType<?>>> entities = sgGeneral.add(new EntityTypeListSetting.Builder()
            .name("entities")
            .description("Entities to attack.")
            .defaultValue(new Object2BooleanOpenHashMap<>(0))
            .onlyAttackable()
            .build()
    );

    private final Setting<Priority> priority = sgGeneral.add(new EnumSetting.Builder<Priority>()
            .name("priority")
            .description("What type of entities to target.")
            .defaultValue(Priority.LowestHealth)
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

    private final Setting<RotationMode> rotationMode = sgGeneral.add(new EnumSetting.Builder<RotationMode>()
            .name("rotation-mode")
            .description("Determines when you should rotate towards the target.")
            .defaultValue(RotationMode.OnHit)
            .build()
    );

    private final Setting<Target> rotationDirection = sgGeneral.add(new EnumSetting.Builder<Target>()
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
    private Entity target;
    private boolean wasPathing;

    private final List<Entity> entityList = new ArrayList<>();

    public KillAura() {
        super(Category.Combat, "kill-aura", "Attacks specified entities around you.");
    }

    @Override
    public void onDeactivate() {
        hitDelayTimer = 0;
        randomDelayTimer = 0;
        target = null;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        findEntity();
        if (target == null) return;

        if (!attack() && rotationMode.get() == RotationMode.Always) {
            Rotations.rotate(Rotations.getYaw(target), Rotations.getPitch(target, rotationDirection.get()));
        }
    }

    private boolean attack() {
        // Entities without health can be hit instantly
        if (target instanceof LivingEntity) {
            if (smartDelay.get()) {
                if (mc.player.getAttackCooldownProgress(0.5f) < 1) return false;
            }
            else {
                if (hitDelayTimer >= 0) {
                    hitDelayTimer--;
                    return false;
                } else hitDelayTimer = hitDelay.get();
            }
        }

        if (randomDelayEnabled.get()) {
            if (randomDelayTimer > 0) {
                randomDelayTimer--;
                return false;
            } else {
                randomDelayTimer = (int) Math.round(Math.random() * randomDelayMax.get());
            }
        }

        if (Math.random() > hitChance.get() / 100) return false;

        if (rotationMode.get() == RotationMode.None) {
            hitEntity();
        }
        else if (rotationMode.get() == RotationMode.OnHit) {
            Rotations.rotate(Rotations.getYaw(target), Rotations.getPitch(target, rotationDirection.get()), this::hitEntity);
        }

        return true;
    }

    private void hitEntity() {
        mc.interactionManager.attackEntity(mc.player, target);
        mc.player.swingHand(Hand.MAIN_HAND);
    }

    private void findEntity() {
        target = null;

        if (mc.player.isDead() || !mc.player.isAlive()) return;
        if (!itemInHand()) return;

        for (Entity entity : mc.world.getEntities()) {
            if (entity == mc.player || entity == mc.cameraEntity) continue;
            if ((entity instanceof LivingEntity && ((LivingEntity) entity).isDead()) || !entity.isAlive()) continue;
            if (entity.distanceTo(mc.player) > range.get()) continue;
            if (!entities.get().getBoolean(entity.getType())) continue;
            if (!nametagged.get() && entity.hasCustomName()) continue;
            if (!ignoreWalls.get() && !PlayerUtils.canSeeEntity(entity)) continue;

            if (entity instanceof PlayerEntity) {
                if (((PlayerEntity) entity).isCreative()) continue;
                if (!friends.get() && !FriendManager.INSTANCE.attack((PlayerEntity) entity)) continue;
            }

            if (entity instanceof AnimalEntity && !babies.get() && ((AnimalEntity) entity).isBaby()) continue;

            entityList.add(entity);
        }

        if (entityList.size() > 0) {
            entityList.sort(this::sort);
            target = entityList.get(0);
            entityList.clear();

            if (pauseOnCombat.get() && BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().isPathing() && !wasPathing) {
                BaritoneAPI.getProvider().getPrimaryBaritone().getCommandManager().execute("pause");
                wasPathing = true;
            }
        } else {
            if (wasPathing){
                BaritoneAPI.getProvider().getPrimaryBaritone().getCommandManager().execute("resume");
                wasPathing = false;
            }
        }
    }

    private boolean itemInHand() {
        switch(onlyWith.get()){
            case Axe:        return mc.player.getMainHandStack().getItem() instanceof AxeItem;
            case Sword:      return mc.player.getMainHandStack().getItem() instanceof SwordItem;
            case SwordOrAxe: return mc.player.getMainHandStack().getItem() instanceof AxeItem || mc.player.getMainHandStack().getItem() instanceof SwordItem;
            default:         return true;
        }
    }

    private int sort(Entity e1, Entity e2) {
        switch (priority.get()) {
            case LowestDistance:  return Double.compare(e1.distanceTo(mc.player), e2.distanceTo(mc.player));
            case HighestDistance: return invertSort(Double.compare(e1.distanceTo(mc.player), e2.distanceTo(mc.player)));
            case LowestHealth:    return sortHealth(e1, e2);
            case HighestHealth:   return invertSort(sortHealth(e1, e2));
            default:              return 0;
        }
    }

    private int sortHealth(Entity e1, Entity e2) {
        boolean e1l = e1 instanceof LivingEntity;
        boolean e2l = e2 instanceof LivingEntity;

        if (!e1l && !e2l) return 0;
        else if (e1l && !e2l) return 1;
        else if (!e1l && e2l) return -1;

        return Float.compare(((LivingEntity) e1).getHealth(), ((LivingEntity) e2).getHealth());
    }

    private int invertSort(int sort) {
        if (sort == 0) return 0;
        return sort > 0 ? -1 : 1;
    }

    @Override
    public String getInfoString() {
        if (target != null && target instanceof PlayerEntity) return target.getEntityName();
        if (target != null) return target.getType().getName().getString();
        return null;
    }
}

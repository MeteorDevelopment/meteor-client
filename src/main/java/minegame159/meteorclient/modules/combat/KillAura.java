/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.modules.combat;

import baritone.api.BaritoneAPI;
import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.world.PostTickEvent;
import minegame159.meteorclient.events.world.PreTickEvent;
import minegame159.meteorclient.friends.FriendManager;
import minegame159.meteorclient.mixininterface.IPlayerMoveC2SPacket;
import minegame159.meteorclient.mixininterface.IVec3d;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.settings.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.SwordItem;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

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

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgDelay = settings.createGroup("Delay");
    private final SettingGroup sgRandomDelay = settings.createGroup("Random Delay");

    // General

    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
            .name("range")
            .description("The maximum range the entity can be to attack it.")
            .defaultValue(4)
            .min(0)
            .build()
    );

    private final Setting<List<EntityType<?>>> entities = sgGeneral.add(new EntityTypeListSetting.Builder()
            .name("entities")
            .description("Entities to attack.")
            .defaultValue(new ArrayList<>(0))
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

    // Random hit delay

    private final Setting<Boolean> randomDelayEnabled = sgRandomDelay.add(new BoolSetting.Builder()
            .name("random-delay-enabled")
            .description("Adds a random delay between hits to attempt to bypass anti-cheats.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Integer> randomDelayMax = sgRandomDelay.add(new IntSetting.Builder()
            .name("random-delay-max")
            .description("The maximum value for random delay.")
            .defaultValue(4)
            .min(0)
            .sliderMax(20)
            .build()
    );

    private PlayerMoveC2SPacket movePacket;
    private int hitDelayTimer;
    private int randomDelayTimer;
    private LivingEntity entity;
    private boolean wasPathing;

    private final List<LivingEntity> entityList = new ArrayList<>();
    private final Vec3d vec1 = new Vec3d(0, 0, 0);
    private final Vec3d vec2 = new Vec3d(0, 0, 0);

    public KillAura() {
        super(Category.Combat, "Kill-Aura", "Attacks specified entities around you.");
    }

    @Override
    public void onDeactivate() {
        movePacket = null;
        hitDelayTimer = 0;
        randomDelayTimer = 0;
        entity = null;
    }

    @EventHandler
    private final Listener<PreTickEvent> onPreTick = new Listener<>(event -> {
        movePacket = null;
        entity = null;
    });

    /*@EventHandler
    private final Listener<SendPacketEvent> onSendPacket = new Listener<>(event -> {
        if (movePacket != null) return;

        if (event.packet instanceof PlayerMoveC2SPacket.PositionOnly) {
            event.cancel();

            PlayerMoveC2SPacket.PositionOnly p = (PlayerMoveC2SPacket.PositionOnly) event.packet;

            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.Both(
                    p.getX(mc.player.getX()),
                    p.getY(mc.player.getY()),
                    p.getZ(mc.player.getZ()),
                    mc.player.yaw,
                    mc.player.pitch,
                    p.isOnGround()
            ));
        } else if (event.packet instanceof PlayerMoveC2SPacket) {
            movePacket = (PlayerMoveC2SPacket) event.packet;

            rotatePacket();
        }
    });*/

    /*@EventHandler
    private final Listener<PacketSentEvent> onPacketSent = new Listener<>(event -> {
        if (event.packet == movePacket) attack();
    });

    @EventHandler
    private final Listener<PostTickEvent> onPostTick = new Listener<>(event -> {
        if (movePacket == null) {
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.LookOnly(
                    mc.player.yaw,
                    mc.player.pitch,
                    mc.player.isOnGround()
            ));
        }
    });*/

    @EventHandler
    private final Listener<PostTickEvent> onPostTick = new Listener<>(event -> {
        findEntity();
        if (entity == null) return;

        movePacket = new PlayerMoveC2SPacket.LookOnly(0, 0, mc.player.isOnGround());
        rotatePacket();
        attack();
    });

    private void rotatePacket() {
        ((IVec3d) vec1).set(mc.player.getX(), mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()), mc.player.getZ());
        ((IVec3d) vec2).set(entity.getX(), entity.getY() + entity.getHeight() / 2, entity.getZ());

        double d = vec2.x - vec1.x;
        double e = vec2.y - vec1.y;
        double f = vec2.z - vec1.z;
        double g = MathHelper.sqrt(d * d + f * f);
        double yaw = MathHelper.wrapDegrees((float) (MathHelper.atan2(f, d) * 57.2957763671875D) - 90.0F);
        double pitch = MathHelper.wrapDegrees((float) (-(MathHelper.atan2(e, g) * 57.2957763671875D)));

        ((IPlayerMoveC2SPacket) movePacket).setYaw((float) yaw);
        ((IPlayerMoveC2SPacket) movePacket).setPitch((float) pitch);
    }

    private void attack() {
        if (entity == null) return;

        mc.interactionManager.attackEntity(mc.player, entity);
        mc.player.swingHand(Hand.MAIN_HAND);
    }

    private void findEntity() {
        if (mc.player.isDead() || !mc.player.isAlive()) return;
        if (!itemInHand()) return;

        if (smartDelay.get()) {
            if (mc.player.getAttackCooldownProgress(0.5f) < 1) return;
        } else {
            if (hitDelayTimer >= 0) {
                hitDelayTimer--;
                return;
            } else hitDelayTimer = hitDelay.get();
        }

        if (randomDelayEnabled.get()) {
            if (randomDelayTimer > 0) {
                randomDelayTimer--;
                return;
            } else {
                randomDelayTimer = (int) Math.round(Math.random() * randomDelayMax.get());
            }
        }

        if (Math.random() > hitChance.get() / 100) return;

        for (Entity e : mc.world.getEntities()) {
            if (!(e instanceof LivingEntity)) continue;
            LivingEntity entity = (LivingEntity) e;

            if (entity == mc.player || entity == mc.cameraEntity) continue;
            if (entity.isDead() || !entity.isAlive()) continue;
            if (entity.distanceTo(mc.player) > range.get()) continue;
            if (!entities.get().contains(entity.getType())) continue;
            if (!nametagged.get() && entity.hasCustomName()) continue;
            if (!canSeeEntity(entity)) continue;

            if (entity instanceof PlayerEntity) {
                if (((PlayerEntity) entity).isCreative()) continue;
                if (!friends.get() && !FriendManager.INSTANCE.attack((PlayerEntity) entity)) continue;
            }

            if (entity instanceof AnimalEntity) {
                if (!babies.get() && entity.isBaby()) continue;
            }

            entityList.add(entity);
        }

        if (entityList.size() > 0) {
            entityList.sort(this::sort);
            entity = entityList.get(0);
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

    private boolean canSeeEntity(LivingEntity entity) {
        if (ignoreWalls.get()) return true;

        ((IVec3d) vec1).set(mc.player.getX(), mc.player.getY() + mc.player.getStandingEyeHeight(), mc.player.getZ());
        ((IVec3d) vec2).set(entity.getX(), entity.getY(), entity.getZ());
        boolean canSeeFeet =  mc.world.raycast(new RaycastContext(vec1, vec2, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player)).getType() == HitResult.Type.MISS;

        ((IVec3d) vec2).set(entity.getX(), entity.getY() + entity.getStandingEyeHeight(), entity.getZ());
        boolean canSeeEyes =  mc.world.raycast(new RaycastContext(vec1, vec2, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player)).getType() == HitResult.Type.MISS;

        return canSeeFeet || canSeeEyes;
    }

    private boolean itemInHand() {
        switch(onlyWith.get()){
            case Axe:        return mc.player.getMainHandStack().getItem() instanceof AxeItem;
            case Sword:      return mc.player.getMainHandStack().getItem() instanceof SwordItem;
            case SwordOrAxe: return mc.player.getMainHandStack().getItem() instanceof AxeItem || mc.player.getMainHandStack().getItem() instanceof SwordItem;
            default:         return true;
        }
    }

    private int sort(LivingEntity e1, LivingEntity e2) {
        switch (priority.get()) {
            case LowestDistance:  return Double.compare(e1.distanceTo(mc.player), e2.distanceTo(mc.player));
            case HighestDistance: return invertSort(Double.compare(e1.distanceTo(mc.player), e2.distanceTo(mc.player)));
            case LowestHealth:    return Float.compare(e1.getHealth(), e2.getHealth());
            case HighestHealth:   return invertSort(Float.compare(e1.getHealth(), e2.getHealth()));
            default:              return 0;
        }
    }

    private int invertSort(int sort) {
        if (sort == 0) return 0;
        return sort > 0 ? -1 : 1;
    }
}

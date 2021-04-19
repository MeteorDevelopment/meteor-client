/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.systems.modules.combat;

import baritone.api.BaritoneAPI;
import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import meteordevelopment.orbit.EventHandler;
import minegame159.meteorclient.events.entity.player.StartBreakingBlockEvent;
import minegame159.meteorclient.events.packets.PacketEvent;
import minegame159.meteorclient.events.render.RenderEvent;
import minegame159.meteorclient.events.world.TickEvent;
import minegame159.meteorclient.settings.*;
import minegame159.meteorclient.systems.friends.Friends;
import minegame159.meteorclient.systems.modules.Categories;
import minegame159.meteorclient.systems.modules.Module;
import minegame159.meteorclient.utils.Utils;
import minegame159.meteorclient.utils.entity.EntityUtils;
import minegame159.meteorclient.utils.entity.SortPriority;
import minegame159.meteorclient.utils.entity.Target;
import minegame159.meteorclient.utils.misc.Vec3;
import minegame159.meteorclient.utils.player.InvUtils;
import minegame159.meteorclient.utils.player.PlayerUtils;
import minegame159.meteorclient.utils.player.Rotations;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;

public class Aimbot extends Module {
    public enum MeleeItems {
        Axe,
        Sword,
        Both,
        None
    }

    public enum RangedItems {
        Bow,
        CrossBow,
        Both,
        None
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgEntities = settings.createGroup("Entities");
    private final SettingGroup sgAim = settings.createGroup("Aiming");

    // General

    private final Setting<MeleeItems> meleeItems = sgGeneral.add(new EnumSetting.Builder<MeleeItems>()
            .name("melee-items")
            .description("Allowed melee items to use.")
            .defaultValue(MeleeItems.Both)
            .build()
    );

    private final Setting<Double> meleeRange = sgGeneral.add(new DoubleSetting.Builder()
            .name("range")
            .description("The range at which an entity can be targeted using a melee weapon.")
            .defaultValue(4)
            .min(0)
            .max(8)
            .build()
    );

    private final Setting<RangedItems> rangedItems = sgGeneral.add(new EnumSetting.Builder<RangedItems>()
            .name("ranged-items")
            .description("Allowed ranged items to use.")
            .defaultValue(RangedItems.Both)
            .build()
    );

    private final Setting<Double> rangedRange = sgGeneral.add(new DoubleSetting.Builder()
            .name("ranged-range")
            .description("The range at which an entity can be targeted using a ranged weapon.")
            .defaultValue(20)
            .min(0)
            .max(100)
            .build()
    );


    private final Setting<Boolean> pauseOnCombat = sgGeneral.add(new BoolSetting.Builder()
            .name("pause-on-combat")
            .description("Freezes Baritone temporarily while in combat.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Double> unpauseDelay = sgGeneral.add(new DoubleSetting.Builder()
            .name("unpause-delay")
            .description("The delay in seconds before unpausing baritone after combat.")
            .defaultValue(3)
            .min(0)
            .max(10)
            .build()
    );

    // Entities

    private final Setting<Object2BooleanMap<EntityType<?>>> entities = sgEntities.add(new EntityTypeListSetting.Builder()
            .name("entities")
            .description("Entities to aim at.")
            .defaultValue(Utils.asObject2BooleanOpenHashMap(EntityType.PLAYER))
            .build()
    );

    private final Setting<Boolean> friends = sgEntities.add(new BoolSetting.Builder()
            .name("friends")
            .description("Whether or not to aim at friends.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> babies = sgEntities.add(new BoolSetting.Builder()
            .name("babies")
            .description("Whether or not to attack baby variants of the entity.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> nametagged = sgEntities.add(new BoolSetting.Builder()
            .name("nametagged")
            .description("Whether or not to attack mobs with a name tag.")
            .defaultValue(false)
            .build()
    );

    private final Setting<SortPriority> priority = sgEntities.add(new EnumSetting.Builder<SortPriority>()
            .name("priority")
            .description("How to select target from entities in range.")
            .defaultValue(SortPriority.LowestHealth)
            .build()
    );

    // Aiming

    private final Setting<Target> bodyTarget = sgAim.add(new EnumSetting.Builder<Target>()
            .name("target")
            .description("Which part of the entities body to aim at.")
            .defaultValue(Target.Body)
            .build()
    );

    private final Setting<Boolean> instant = sgAim.add(new BoolSetting.Builder()
            .name("instant-look")
            .description("Whether or not to instantly rotate towards the entity.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Double> speed = sgAim.add(new DoubleSetting.Builder()
            .name("speed")
            .description("How fast to rotate towards the entity.")
            .defaultValue(5)
            .min(0)
            .build()
    );

    private final Vec3 Pos = new Vec3();
    private int pauseTicks;
    private boolean wasPathing;
    private Entity target;

    public Aimbot() {
        super(Categories.Combat, "aimbot", "Automatically aims at entities.");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (isMeleeItem(mc.player.getMainHandStack().getItem())) {
            target = getEntity(meleeRange.get());
        } else if (isRangedItem(mc.player.getMainHandStack().getItem())) {
            target = getEntity(rangedRange.get());
        }

        if (wasPathing && pauseTicks <= 0) {
            BaritoneAPI.getProvider().getPrimaryBaritone().getCommandManager().execute("resume");
            wasPathing = false;
            pauseTicks = 0;
        }
        if (pauseTicks > 0) pauseTicks--;

        if (target != null && isMeleeItem(mc.player.getMainHandStack().getItem()) && mc.options.keyAttack.isPressed()) {
            pauseBaritone();
            mc.interactionManager.attackEntity(mc.player, target);
            mc.player.swingHand(Hand.MAIN_HAND);
        }
    }

    @EventHandler
    private void onRender(RenderEvent event) {
        if (target == null) return;

        if (isMeleeItem(mc.player.getMainHandStack().getItem())) {
            meleeAim(target, event.tickDelta, instant.get());
        }

        if (isRangedItem(mc.player.getMainHandStack().getItem()) && mc.options.keyUse.isPressed() && (InvUtils.findItemWithCount(Items.ARROW).slot != -1)) {
            rangedAim(target, event.tickDelta);
            pauseBaritone();
        }
    }

    @EventHandler
    private void onStartBreakingBlock(StartBreakingBlockEvent event) {
        if (target != null) event.cancel();
    }

    private Entity getEntity(double range) {
        return EntityUtils.get(entity -> {
            if (entity == mc.player || entity == mc.cameraEntity) return false;
            if (entity.distanceTo(mc.player) > range) return false;
            if ((entity instanceof LivingEntity && ((LivingEntity) entity).isDead()) || !entity.isAlive()) return false;
            if (!entities.get().getBoolean(entity.getType())) return false;
            if (!nametagged.get() && entity.hasCustomName()) return false;
            if (!PlayerUtils.canSeeEntity(entity)) return false;
            if (entity instanceof PlayerEntity) {
                if (((PlayerEntity) entity).isCreative()) return false;
                if (!friends.get() && !Friends.get().attack((PlayerEntity) entity)) return false;
            }
            return !(entity instanceof AnimalEntity) || babies.get() || !((AnimalEntity) entity).isBaby();
        }, priority.get());
    }

    private void meleeAim(Entity target, double delta, boolean instant) {
        setVec3dToTargetPoint(Pos, target, delta, false);

        double diffX = Pos.x - mc.player.getX();
        double diffZ = Pos.z - mc.player.getZ();
        double diffY = Pos.y - (mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()));

        // Yaw
        double yaw = Math.toDegrees(Math.atan2(diffZ, diffX)) - 90;
        double deltaYaw, deltaPitch;
        double yawToRotate, pitchToRotate;

        // Pitch
        double hDistance = Math.sqrt(diffX * diffX + diffZ * diffZ);
        double pitch = -Math.toDegrees(Math.atan2(diffY, hDistance));

        // Rotations
        if (instant) {
            Rotations.rotate(yaw, pitch);
        } else {
            deltaYaw = MathHelper.wrapDegrees(yaw - mc.player.yaw);
            deltaPitch = MathHelper.wrapDegrees(pitch - mc.player.pitch);

            yawToRotate = speed.get() * (deltaYaw >= 0 ? 1 : -1) * delta;
            pitchToRotate = speed.get() * (deltaPitch >= 0 ? 1 : -1) * delta;

            if ((yawToRotate >= 0 && yawToRotate > deltaYaw) || (yawToRotate < 0 && yawToRotate < deltaYaw)) yawToRotate = deltaYaw;
            if ((pitchToRotate >= 0 && pitchToRotate > deltaPitch) || (pitchToRotate < 0 && pitchToRotate < deltaPitch)) pitchToRotate = deltaPitch;

            Rotations.rotate(mc.player.yaw + yawToRotate, mc.player.pitch + pitchToRotate);
        }
    }

    private void rangedAim(Entity target, double delta) {
        // Velocity based on bow charge.
        float velocity = (mc.player.getItemUseTime() - mc.player.getItemUseTimeLeft()) / 20f;
        velocity = (velocity * velocity + velocity * 2) / 3;
        if (velocity > 1) velocity = 1;

        // Positions
        setVec3dToTargetPoint(Pos, target, delta, true);

        double diffX = Pos.x - mc.player.getX();
        double diffY = Pos.y - mc.player.getY();
        double diffZ = Pos.z - mc.player.getZ();

        // Adjusting for hitbox heights
        diffY -= 1.9f - target.getHeight();

        // Pitch Calculation
        double hDistance = Math.sqrt(diffX * diffX + diffZ * diffZ);
        double hDistanceSq = hDistance * hDistance;
        float g = 0.006f;
        float velocitySq = velocity * velocity;
        float pitch = (float) -Math.toDegrees(Math.atan((velocitySq - Math.sqrt(velocitySq * velocitySq - g * (g * hDistanceSq + 2 * diffY * velocitySq))) / (g * hDistance)));

        // Rotations
        if(Float.isNaN(pitch)) {
            Rotations.rotate(Rotations.getYaw(target), Rotations.getPitch(target));
        } else {
            Rotations.rotate(Rotations.getYaw(target), pitch);
        }
    }

    private void setVec3dToTargetPoint(Vec3 vec, Entity entity, double tickDelta, boolean isRanged) {
        vec.set(entity, tickDelta);

        if (isRanged) return; // Because adding the eye height messes up bow aimbot calculations
        switch (bodyTarget.get()) {
            case Head: vec.add(0, entity.getEyeHeight(entity.getPose()), 0); break;
            case Body: vec.add(0, entity.getEyeHeight(entity.getPose()) / 2, 0); break;
        }
    }

    @Override
    public String getInfoString() {
        if (target == null) return null;
        if (target instanceof PlayerEntity) return target.getEntityName();
        return target.getType().getName().getString();
    }

    private boolean isMeleeItem(Item item) {
        switch(meleeItems.get()) {
            case Axe:       return item instanceof AxeItem;
            case Sword:     return item instanceof SwordItem;
            case Both:      return item instanceof  AxeItem || item instanceof SwordItem;
            default:        return false;
        }
    }

    private boolean isRangedItem(Item item) {
        switch(rangedItems.get()) {
            case Bow:           return item instanceof BowItem;
            case CrossBow:      return item instanceof CrossbowItem;
            case Both:          return item instanceof  BowItem || item instanceof CrossbowItem;
            default:            return false;
        }
    }

    private void pauseBaritone() {
        if (pauseOnCombat.get()) {
            if (!wasPathing  && BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().isPathing()) {
                BaritoneAPI.getProvider().getPrimaryBaritone().getCommandManager().execute("pause");
                wasPathing = true;
            }
            pauseTicks = (int) (unpauseDelay.get() * 20);
        }
    }
}

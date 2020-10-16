package minegame159.meteorclient.modules.combat;

/**
 * Updated by squidoodly 14/07/2020
 * Updated by Sigha 16/10/2020 (ty seasnail & MineGame159 for help)
*/

import baritone.api.BaritoneAPI;
import com.google.common.collect.Streams;
import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.friends.FriendManager;
import minegame159.meteorclient.events.TickEvent;
import minegame159.meteorclient.mixininterface.IVec3d;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.settings.*;
import minegame159.meteorclient.utils.DamageCalcUtils;
import net.minecraft.command.argument.EntityAnchorArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.item.SwordItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class KillAura extends ToggleModule {
    public enum Priority {
        LowestDistance,
        HighestDistance,
        LowestHealth,
        HighestHealth
    }

    public enum OnlyWhen {
        AXE,
        SWORD,
        AXEORSWORD,
        ANY
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgDelay = settings.createGroup("Delay", "smart-delay", "Smart delay.", true);
    private final SettingGroup sgDelayDisabled = sgDelay.getDisabledGroup();
    private final SettingGroup sgRandomDelay = settings.createGroup("Random Delay", "random-delay-enabled", "Adds a random delay to hits to try and bypass anti-cheats.", false);

    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
            .name("range")
            .description("Attack range.")
            .defaultValue(5.5)
            .min(0.0)
            .build()
    );

    private final Setting<List<EntityType<?>>> entities = sgGeneral.add(new EntityTypeListSetting.Builder()
            .name("entities")
            .description("Entities to attack.")
            .defaultValue(new ArrayList<>(0))
            .onlyAttackable()
            .build()
    );

    private final Setting<Boolean> onlyOnGround = sgGeneral.add(new BoolSetting.Builder()
            .name("only-on-ground")
            .description("Only attacks players that are on the ground (useful to bypass anti-cheats)")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> nametagged = sgGeneral.add(new BoolSetting.Builder()
            .name("nametagged")
            .description("Hit nametagged mobs.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> babies = sgGeneral.add(new BoolSetting.Builder()
            .name("babies")
            .description("Hit baby animals.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Integer> hitChance = sgGeneral.add(new IntSetting.Builder()
            .name("hit-chance")
            .description("The probability of your hits counting")
            .defaultValue(100)
            .min(0)
            .max(100)
            .sliderMax(100)
            .build()
    );

    private final Setting<Boolean> friends = sgGeneral.add(new BoolSetting.Builder()
            .name("friends")
            .description("Attack friends, useful only if attack players is on.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> ignoreWalls = sgGeneral.add(new BoolSetting.Builder()
            .name("ignore-walls")
            .description("Attack through walls.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Priority> priority = sgGeneral.add(new EnumSetting.Builder<Priority>()
            .name("priority")
            .description("What entities to target.")
            .defaultValue(Priority.LowestHealth)
            .build()
    );

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
            .name("rotate")
            .description("Rotates you towards the target.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> instaKill = sgGeneral.add(new BoolSetting.Builder()
            .name("insta-kill")
            .description("If your sharpness is enough to kill then just swing")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> pauseOnCombat = sgGeneral.add(new BoolSetting.Builder()
            .name("pause-on-combat")
            .description("Pauses baritone when you get near a target")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> oneTickDelay = sgDelay.add(new BoolSetting.Builder()
            .name("one-tick-delay")
            .description("Adds one tick delay.")
            .defaultValue(true)
            .build()
    );



    private final Setting<OnlyWhen> itemOnly = sgGeneral.add(new EnumSetting.Builder<OnlyWhen>()
            .name("Item-only")
            .description("Only hits an entity when the specified item is in your hand. (or any item)")
            .defaultValue(OnlyWhen.ANY)
            .build()
    );

    private final Setting<Integer> hitDelay = sgDelayDisabled.add(new IntSetting.Builder()
            .name("hit-delay")
                .description("Hit delay in ticks. 20 ticks = 1 second.")
                .defaultValue(0)
                .min(0)
                .sliderMax(60)
                .build()
    );
                                                            
    private final Setting<Integer> randomDelayMax = sgRandomDelay.add(new IntSetting.Builder()
            .name("random-delay-max")
            .description("Maximum random value for random delay.")
            .defaultValue(4)
            .min(0)
            .sliderMax(20)
            .build()
    );

    private boolean canAutoDelayAttack;
    private int hitDelayTimer;
    private int randomHitDelayTimer;
    private Entity entity;
    private boolean didHit = false;
    private boolean wasPathing = false;
    private final Random random = new Random(System.currentTimeMillis());

    private final Vec3d vec3d1 = new Vec3d(0, 0, 0);
    private final Vec3d vec3d2 = new Vec3d(0, 0, 0);

    public KillAura() {
        super(Category.Combat, "kill-aura", "Automatically attacks entities.");
    }

    @Override
    public void onActivate() {
        hitDelayTimer = 0;
        randomHitDelayTimer = 0;
    }

    private boolean isInRange(Entity entity) {
        return entity.distanceTo(mc.player) <= range.get();
    }

    private boolean canAttackEntity(Entity entity) {
        if (entity == mc.player || entity == mc.cameraEntity || entity.getUuid().equals(mc.player.getUuid()) || !entities.get().contains(entity.getType())) return false;

        if (entity instanceof PlayerEntity) {
            if (friends.get()) return true;
            if (((PlayerEntity) entity).isCreative()) return false;
            return FriendManager.INSTANCE.attack((PlayerEntity) entity);
        }

        if (entity instanceof AnimalEntity) {
            if (babies.get()) return true;
            return !((AnimalEntity) entity).isBaby();
        }

        return true;
    }

    private boolean isPlayerOnGround(Entity entity){
        if (!onlyOnGround.get()) return true;
        else if (onlyOnGround.get() && entity instanceof PlayerEntity && entity.isOnGround()) return true;
        else if (onlyOnGround.get() && entity instanceof PlayerEntity && !entity.isOnGround()) return false;
        else return onlyOnGround.get() && !(entity instanceof PlayerEntity);
    }

    private boolean canSeeEntity(Entity entity) {
        if (ignoreWalls.get()) return true;

        ((IVec3d) vec3d1).set(mc.player.getX(), mc.player.getY() + mc.player.getStandingEyeHeight(), mc.player.getZ());
        ((IVec3d) vec3d2).set(entity.getX(), entity.getY(), entity.getZ());
        boolean canSeeFeet =  mc.world.raycast(new RaycastContext(vec3d1, vec3d2, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player)).getType() == HitResult.Type.MISS;

        ((IVec3d) vec3d2).set(entity.getX(), entity.getY() + entity.getStandingEyeHeight(), entity.getZ());
        boolean canSeeEyes =  mc.world.raycast(new RaycastContext(vec3d1, vec3d2, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player)).getType() == HitResult.Type.MISS;

        return canSeeFeet || canSeeEyes;
    }

    private int invertSort(int sort) {
        if (sort == 0) return 0;
        return sort > 0 ? -1 : 1;
    }

    private int sort(Entity e1, Entity e2) {
        switch (priority.get()) {
            case LowestDistance:  return Double.compare(e1.distanceTo(mc.player), e2.distanceTo(mc.player));
            case HighestDistance: return invertSort(Double.compare(e1.distanceTo(mc.player), e2.distanceTo(mc.player)));
            case LowestHealth: {
                float a = e1 instanceof LivingEntity ? ((LivingEntity) e1).getHealth() : 0;
                float b = e2 instanceof LivingEntity ? ((LivingEntity) e2).getHealth() : 0;
                return Float.compare(a, b);
            }
            case HighestHealth: {
                float a = e1 instanceof LivingEntity ? ((LivingEntity) e1).getHealth() : 0;
                float b = e2 instanceof LivingEntity ? ((LivingEntity) e2).getHealth() : 0;
                return invertSort(Float.compare(a, b));
            }
            default:              return 0;
        }
    }

    private boolean checkName(Entity entity){
        if (entity.hasCustomName() && !nametagged.get()) {
            return false;
        } else if (entity.hasCustomName() && nametagged.get()) {
            return true;
        }
        return true;
    }


    private boolean itemInHand(){
        switch(itemOnly.get()){
            case AXE:
                return mc.player.getMainHandStack().getItem() instanceof AxeItem;
            case SWORD:
                return mc.player.getMainHandStack().getItem() instanceof SwordItem;
            case AXEORSWORD:
                return mc.player.getMainHandStack().getItem() instanceof AxeItem || mc.player.getMainHandStack().getItem() instanceof SwordItem;
            default:
                return true;
        }

    }



    @EventHandler
    private final Listener<TickEvent> onTick = new Listener<>(event -> {
        if (mc.player.getHealth() <= 0) return;
        if (!itemInHand()) return;

        if(entity == null && wasPathing){
            BaritoneAPI.getProvider().getPrimaryBaritone().getCommandManager().execute("resume");
            wasPathing = false;
        }

        entity = null;
        didHit = false;

        Streams.stream(mc.world.getEntities())
                .filter(this::isInRange)
                .filter(this::canAttackEntity)
                .filter(this::canSeeEntity)
                .filter(Entity::isAlive)
                .filter(this::isPlayerOnGround)
                .filter(this::checkName)
                .min(this::sort)
                .ifPresent(tempEntity -> {
                    entity = tempEntity;
                    if (random.nextInt(100) > hitChance.get()) return;
                    if (entity instanceof PlayerEntity && instaKill.get()) {
                        if (DamageCalcUtils.getSwordDamage((PlayerEntity) entity, false) >= ((PlayerEntity) entity).getHealth() + ((PlayerEntity) entity).getAbsorptionAmount()) {
                            if (rotate.get()) {
                                ((IVec3d) vec3d1).set(entity.getX(), entity.getY() + entity.getHeight() / 2, entity.getZ());
                                mc.player.lookAt(EntityAnchorArgumentType.EntityAnchor.EYES, vec3d1);
                            }

                            mc.interactionManager.attackEntity(mc.player, entity);
                            mc.player.swingHand(Hand.MAIN_HAND);
                            didHit = true;
                        }
                    }
                    if (pauseOnCombat.get() && BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().isPathing() && !wasPathing) {
                        BaritoneAPI.getProvider().getPrimaryBaritone().getCommandManager().execute("pause");
                        wasPathing = true;
                    }
                });

        if(didHit) return;

        if (sgDelay.isEnabled()) {
            // Smart delay
            if (mc.player.getAttackCooldownProgress(0.5f) < 1) return;

            // One tick delay
            if (oneTickDelay.get()) {
                if (canAutoDelayAttack) {
                    canAutoDelayAttack = false;
                } else {
                    canAutoDelayAttack = true;
                    return;
                }
            }
        } else {
            // Manual delay
            if (hitDelayTimer >= 0) {
                hitDelayTimer--;
                return;
            }
            else hitDelayTimer = hitDelay.get();
        }

        // Random hit delay
        if (sgRandomDelay.isEnabled()) {
            if (randomHitDelayTimer > 0) {
                randomHitDelayTimer--;
                return;
            }
        }
        if(entity != null && random.nextInt(100) < hitChance.get()) {
            // Rotate
            if (rotate.get()) {
                ((IVec3d) vec3d1).set(entity.getX(), entity.getY() + entity.getHeight() / 2, entity.getZ());
                mc.player.lookAt(EntityAnchorArgumentType.EntityAnchor.EYES, vec3d1);
            }

            // Attack
            mc.interactionManager.attackEntity(mc.player, entity);
            mc.player.swingHand(Hand.MAIN_HAND);

            // Set next random delay length
            if (sgRandomDelay.isEnabled()) randomHitDelayTimer = (int) Math.round(Math.random() * randomDelayMax.get());
        }
    });
}

/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.entity;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.entity.fakeplayer.FakePlayerEntity;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiFunction;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class DamageUtils {
    private DamageUtils() {
    }

    // Explosion damage

    /**
     * It is recommended to use this {@link RaycastFactory} unless you implement custom behaviour, as soon:tm: it will be the
     * target of optimizations to make it more performant.
     *
     * @see BlockGetter#clip(ClipContext)
     */
    public static final RaycastFactory HIT_FACTORY = (context, blockPos) -> {
        BlockState blockState = mc.level.getBlockState(blockPos);
        if (blockState.getBlock().getExplosionResistance() < 600) return null;

        return blockState.getCollisionShape(mc.level, blockPos).clip(context.start(), context.end(), blockPos);
    };

    public static float crystalDamage(LivingEntity target, Vec3 targetPos, AABB targetBox, Vec3 explosionPos, RaycastFactory raycastFactory) {
        return explosionDamage(target, targetPos, targetBox, explosionPos, 12f, raycastFactory);
    }

    public static float bedDamage(LivingEntity target, Vec3 targetPos, AABB targetBox, Vec3 explosionPos, RaycastFactory raycastFactory) {
        return explosionDamage(target, targetPos, targetBox, explosionPos, 10f, raycastFactory);
    }

    public static float anchorDamage(LivingEntity target, Vec3 targetPos, AABB targetBox, Vec3 explosionPos, RaycastFactory raycastFactory) {
        return explosionDamage(target, targetPos, targetBox, explosionPos, 10f, raycastFactory);
    }

    /**
     * Low level control of parameters without having to reimplement everything, for addon authors who wish to use their
     * own predictions or other systems.
     *
     * @see net.minecraft.world.level.ExplosionDamageCalculator#getEntityDamageAmount(Explosion, Entity, float)
     */
    public static float explosionDamage(LivingEntity target, Vec3 targetPos, AABB targetBox, Vec3 explosionPos, float power, RaycastFactory raycastFactory) {
        double modDistance = PlayerUtils.distance(targetPos.x, targetPos.y, targetPos.z, explosionPos.x, explosionPos.y, explosionPos.z);
        if (modDistance > power) return 0f;

        double exposure = getExposure(explosionPos, targetBox, raycastFactory);
        double impact = (1 - (modDistance / power)) * exposure;
        float damage = (int) ((impact * impact + impact) / 2 * 7 * 12 + 1);

        return calculateReductions(damage, target, mc.level.damageSources().explosion(null));
    }

    /**
     * Meteor Client implementations
     */

    public static float crystalDamage(LivingEntity target, Vec3 crystal, boolean predictMovement, BlockPos obsidianPos) {
        return overridingExplosionDamage(target, crystal, 12f, predictMovement, obsidianPos, Blocks.OBSIDIAN.defaultBlockState());
    }

    public static float crystalDamage(LivingEntity target, Vec3 crystal) {
        return explosionDamage(target, crystal, 12f, false);
    }

    public static float bedDamage(LivingEntity target, Vec3 bed) {
        return explosionDamage(target, bed, 10f, false);
    }

    public static float anchorDamage(LivingEntity target, Vec3 anchor) {
        return overridingExplosionDamage(target, anchor, 10f, false, BlockPos.containing(anchor), Blocks.AIR.defaultBlockState());
    }

    private static float overridingExplosionDamage(LivingEntity target, Vec3 explosionPos, float power, boolean predictMovement, BlockPos overridePos, BlockState overrideState) {
        return explosionDamage(target, explosionPos, power, predictMovement, getOverridingHitFactory(overridePos, overrideState));
    }

    private static float explosionDamage(LivingEntity target, Vec3 explosionPos, float power, boolean predictMovement) {
        return explosionDamage(target, explosionPos, power, predictMovement, HIT_FACTORY);
    }

    private static float explosionDamage(LivingEntity target, Vec3 explosionPos, float power, boolean predictMovement, RaycastFactory raycastFactory) {
        if (target == null) return 0f;
        if (target instanceof Player player && EntityUtils.getGameMode(player) == GameType.CREATIVE && !(player instanceof FakePlayerEntity))
            return 0f;

        Vec3 position = predictMovement ? target.position().add(target.getDeltaMovement()) : target.position();

        AABB box = target.getBoundingBox();
        if (predictMovement) box = box.move(target.getDeltaMovement());

        return explosionDamage(target, position, box, explosionPos, power, raycastFactory);
    }

    public static RaycastFactory getOverridingHitFactory(BlockPos overridePos, BlockState overrideState) {
        return (context, blockPos) -> {
            BlockState blockState;
            if (blockPos.equals(overridePos)) blockState = overrideState;
            else {
                blockState = mc.level.getBlockState(blockPos);
                if (blockState.getBlock().getExplosionResistance() < 600) return null;
            }

            return blockState.getCollisionShape(mc.level, blockPos).clip(context.start(), context.end(), blockPos);
        };
    }

    // Sword damage

    /**
     * @see Player#attack(Entity)
     */
    public static float getAttackDamage(LivingEntity attacker, Entity target) {
        float itemDamage = (float) attacker.getAttributeValue(Attributes.ATTACK_DAMAGE);
        DamageSource damageSource = attacker instanceof Player player ? mc.level.damageSources().playerAttack(player) : mc.level.damageSources().mobAttack(attacker);

        float damage = modifyAttackDamage(attacker, target, attacker.getWeaponItem(), damageSource, itemDamage);
        return calculateReductions(damage, target, damageSource);
    }

    public static float getAttackDamage(LivingEntity attacker, Entity target, ItemStack weapon) {
        AttributeInstance original = attacker.getAttribute(Attributes.ATTACK_DAMAGE);
        AttributeInstance copy = new AttributeInstance(Attributes.ATTACK_DAMAGE, _ -> {
        });

        copy.setBaseValue(original.getBaseValue());
        for (AttributeModifier modifier : original.getModifiers()) {
            copy.addTransientModifier(modifier);
        }
        copy.removeModifier(Item.BASE_ATTACK_DAMAGE_ID);

        ItemAttributeModifiers attributeModifiers = weapon.get(DataComponents.ATTRIBUTE_MODIFIERS);
        if (attributeModifiers != null) {
            attributeModifiers.forEach(EquipmentSlot.MAINHAND, (entry, modifier) -> {
                if (entry == Attributes.ATTACK_DAMAGE) copy.addOrUpdateTransientModifier(modifier);
            });
        }

        float itemDamage = (float) copy.getValue();
        DamageSource damageSource = attacker instanceof Player player ? mc.level.damageSources().playerAttack(player) : mc.level.damageSources().mobAttack(attacker);

        float damage = modifyAttackDamage(attacker, target, weapon, damageSource, itemDamage);
        return calculateReductions(damage, target, damageSource);
    }

    private static float modifyAttackDamage(LivingEntity attacker, Entity target, ItemStack weapon, DamageSource damageSource, float damage) {
        // Get enchant damage
        Object2IntMap<Holder<Enchantment>> enchantments = new Object2IntOpenHashMap<>();
        Utils.getEnchantments(weapon, enchantments);
        float enchantDamage = 0f;

        int sharpness = Utils.getEnchantmentLevel(enchantments, Enchantments.SHARPNESS);
        if (sharpness > 0) {
            enchantDamage += 1 + 0.5f * (sharpness - 1);
        }

        int baneOfArthropods = Utils.getEnchantmentLevel(enchantments, Enchantments.BANE_OF_ARTHROPODS);
        if (baneOfArthropods > 0 && target.typeHolder().is(EntityTypeTags.SENSITIVE_TO_BANE_OF_ARTHROPODS)) {
            enchantDamage += 2.5f * baneOfArthropods;
        }

        int impaling = Utils.getEnchantmentLevel(enchantments, Enchantments.IMPALING);
        if (impaling > 0 && target.typeHolder().is(EntityTypeTags.SENSITIVE_TO_IMPALING)) {
            enchantDamage += 2.5f * impaling;
        }

        int smite = Utils.getEnchantmentLevel(enchantments, Enchantments.SMITE);
        if (smite > 0 && target.typeHolder().is(EntityTypeTags.SENSITIVE_TO_SMITE)) {
            enchantDamage += 2.5f * smite;
        }

        // Factor charge
        if (attacker instanceof Player playerEntity) {
            float charge = playerEntity.getAttackStrengthScale(0.5f);
            damage *= 0.2f + charge * charge * 0.8f;
            enchantDamage *= charge;

            if (weapon.getItem() instanceof MaceItem item) {
                float bonusDamage = item.getAttackDamageBonus(target, damage, damageSource);
                if (bonusDamage > 0f) {
                    int density = Utils.getEnchantmentLevel(weapon, Enchantments.DENSITY);
                    if (density > 0) bonusDamage += (float) (0.5f * attacker.fallDistance);
                    damage += bonusDamage;
                }
            }

            // Factor critical hit
            if (charge > 0.9f && attacker.fallDistance > 0f && !attacker.onGround() && !attacker.onClimbable() && !attacker.isInWater() && !attacker.hasEffect(MobEffects.BLINDNESS) && !attacker.isPassenger()) {
                damage *= 1.5f;
            }
        }

        return damage + enchantDamage;
    }

    // Fall Damage

    /**
     * @see LivingEntity#calculateFallDamage(double, float)
     */
    public static float fallDamage(LivingEntity entity) {
        if (entity instanceof Player player && player.getAbilities().flying) return 0f;
        if (entity.hasEffect(MobEffects.SLOW_FALLING) || entity.hasEffect(MobEffects.LEVITATION)) return 0f;

        // Fast path - Above the surface
        int surface = mc.level.getChunkAt(entity.blockPosition()).getOrCreateHeightmapUnprimed(Heightmap.Types.MOTION_BLOCKING).getFirstAvailable(entity.getBlockX() & 15, entity.getBlockZ() & 15);
        if (entity.getBlockY() >= surface) return fallDamageReductions(entity, surface);

        // Under the surface
        BlockHitResult raycastResult = mc.level.clip(new ClipContext(entity.position(), new Vec3(entity.getX(), mc.level.getMinY(), entity.getZ()), ClipContext.Block.COLLIDER, ClipContext.Fluid.WATER, entity));
        if (raycastResult.getType() == HitResult.Type.MISS) return 0;

        return fallDamageReductions(entity, raycastResult.getBlockPos().getY());
    }

    private static float fallDamageReductions(LivingEntity entity, int surface) {
        int fallHeight = (int) (entity.getY() - surface + entity.fallDistance - 3d);
        @Nullable MobEffectInstance jumpBoostInstance = entity.getEffect(MobEffects.JUMP_BOOST);
        if (jumpBoostInstance != null) fallHeight -= jumpBoostInstance.getAmplifier() + 1;

        return calculateReductions(fallHeight, entity, mc.level.damageSources().fall());
    }

    // Utils

    /**
     * @see LivingEntity#actuallyHurt(ServerLevel, DamageSource, float)
     */
    public static float calculateReductions(float damage, Entity entity, DamageSource damageSource) {
        if (damageSource.scalesWithDifficulty()) {
            switch (mc.level.getDifficulty()) {
                case EASY -> damage = Math.min(damage / 2 + 1, damage);
                case HARD -> damage *= 1.5f;
            }
        }

        if (entity instanceof LivingEntity livingEntity) { // Armor reduction
            damage = CombatRules.getDamageAfterAbsorb(livingEntity, damage, damageSource, getArmor(livingEntity), (float) livingEntity.getAttributeValue(Attributes.ARMOR_TOUGHNESS));

            // Resistance reduction
            damage = resistanceReduction(livingEntity, damage);

            // Protection reduction
            damage = protectionReduction(livingEntity, damage, damageSource);
        }

        return Math.max(damage, 0);
    }

    private static float getArmor(LivingEntity entity) {
        return (float) Math.floor(entity.getAttributeValue(Attributes.ARMOR));
    }

    /**
     * @see LivingEntity#getDamageAfterMagicAbsorb(DamageSource, float)
     */
    private static float protectionReduction(LivingEntity player, float damage, DamageSource source) {
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return damage;

        int damageProtection = 0;

        for (EquipmentSlot slot : EquipmentSlotGroup.ARMOR) {
            ItemStack stack = player.getItemBySlot(slot);

            Object2IntMap<Holder<Enchantment>> enchantments = new Object2IntOpenHashMap<>();
            Utils.getEnchantments(stack, enchantments);

            int protection = Utils.getEnchantmentLevel(enchantments, Enchantments.PROTECTION);
            if (protection > 0) {
                damageProtection += protection;
            }

            int fireProtection = Utils.getEnchantmentLevel(enchantments, Enchantments.FIRE_PROTECTION);
            if (fireProtection > 0 && source.is(DamageTypeTags.IS_FIRE)) {
                damageProtection += 2 * fireProtection;
            }

            int blastProtection = Utils.getEnchantmentLevel(enchantments, Enchantments.BLAST_PROTECTION);
            if (blastProtection > 0 && source.is(DamageTypeTags.IS_EXPLOSION)) {
                damageProtection += 2 * blastProtection;
            }

            int projectileProtection = Utils.getEnchantmentLevel(enchantments, Enchantments.PROJECTILE_PROTECTION);
            if (projectileProtection > 0 && source.is(DamageTypeTags.IS_PROJECTILE)) {
                damageProtection += 2 * projectileProtection;
            }

            int featherFalling = Utils.getEnchantmentLevel(enchantments, Enchantments.FEATHER_FALLING);
            if (featherFalling > 0 && source.is(DamageTypeTags.IS_FALL)) {
                damageProtection += 3 * featherFalling;
            }
        }

        return CombatRules.getDamageAfterMagicAbsorb(damage, damageProtection);
    }

    /**
     * @see LivingEntity#getDamageAfterMagicAbsorb(DamageSource, float)
     */
    private static float resistanceReduction(LivingEntity player, float damage) {
        MobEffectInstance resistance = player.getEffect(MobEffects.RESISTANCE);
        if (resistance != null) {
            int lvl = resistance.getAmplifier() + 1;
            damage *= (1 - (lvl * 0.2f));
        }

        return Math.max(damage, 0);
    }

    /**
     * @see net.minecraft.world.level.ServerExplosion#getSeenPercent(Vec3, Entity)
     */
    private static float getExposure(Vec3 source, AABB box, RaycastFactory raycastFactory) {
        double xDiff = box.maxX - box.minX;
        double yDiff = box.maxY - box.minY;
        double zDiff = box.maxZ - box.minZ;

        double xStep = 1 / (xDiff * 2 + 1);
        double yStep = 1 / (yDiff * 2 + 1);
        double zStep = 1 / (zDiff * 2 + 1);

        if (xStep > 0 && yStep > 0 && zStep > 0) {
            int misses = 0;
            int hits = 0;

            double xOffset = (1 - Math.floor(1 / xStep) * xStep) * 0.5;
            double zOffset = (1 - Math.floor(1 / zStep) * zStep) * 0.5;

            xStep = xStep * xDiff;
            yStep = yStep * yDiff;
            zStep = zStep * zDiff;

            double startX = box.minX + xOffset;
            double startY = box.minY;
            double startZ = box.minZ + zOffset;
            double endX = box.maxX + xOffset;
            double endY = box.maxY;
            double endZ = box.maxZ + zOffset;

            for (double x = startX; x <= endX; x += xStep) {
                for (double y = startY; y <= endY; y += yStep) {
                    for (double z = startZ; z <= endZ; z += zStep) {
                        Vec3 position = new Vec3(x, y, z);

                        if (raycast(new ExposureRaycastContext(position, source), raycastFactory) == null) misses++;

                        hits++;
                    }
                }
            }

            return (float) misses / hits;
        }

        return 0f;
    }

    /* Raycasts */

    private static BlockHitResult raycast(ExposureRaycastContext context, RaycastFactory raycastFactory) {
        return BlockGetter.traverseBlocks(context.start, context.end, context, raycastFactory, ctx -> null);
    }

    public record ExposureRaycastContext(Vec3 start, Vec3 end) {
    }

    @FunctionalInterface
    public interface RaycastFactory extends BiFunction<ExposureRaycastContext, BlockPos, BlockHitResult> {
    }
}

/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.entity;

import meteordevelopment.meteorclient.mixin.ShulkerEntityAccessor;
import meteordevelopment.meteorclient.mixin.StatusEffectAccessor;
import meteordevelopment.meteorclient.mixininterface.IAttributeContainer;
import meteordevelopment.meteorclient.mixininterface.IEntityAttributeInstance;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.*;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.mob.ShulkerEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

public abstract class EntityAttributeHelper {
    /**
     * @see LivingEntity#getAttributes()
     */
    public static AttributeContainer getAttributes(LivingEntity entity) {
        AttributeContainer attributes = new AttributeContainer(getDefaultForEntity(entity));

        // Equipment
        for (var equipmentSlot : EquipmentSlot.values()) {
            ItemStack stack = entity.getEquippedStack(equipmentSlot);
            //attributes.addTemporaryModifiers(stack.getAttributeModifiers(equipmentSlot));
            stack.applyAttributeModifiers(equipmentSlot, (attribute, modifier) -> {
                EntityAttributeInstance entityAttributeInstance = attributes.getCustomInstance(attribute);
                if (entityAttributeInstance != null) {
                    entityAttributeInstance.removeModifier(modifier.uuid());
                    entityAttributeInstance.addTemporaryModifier(modifier);
                }
            });
        }

        // Status effects
        for (var statusEffect : StatusEffectHelper.getStatusEffects(entity)) {
            statusEffect.getEffectType().value().onApplied(attributes, statusEffect.getAmplifier());
        }

        handleSpecialCases(entity, attributes::getCustomInstance);

        // Copy tracked attributes
        ((IAttributeContainer) attributes).meteor$copyFrom(entity.getAttributes());

        return attributes;
    }

    /**
     * @see LivingEntity#getAttributeInstance(RegistryEntry)
     */
    public static EntityAttributeInstance getAttributeInstance(LivingEntity entity, RegistryEntry<EntityAttribute> attribute) {
        double baseValue = getDefaultForEntity(entity).getBaseValue(attribute);
        EntityAttributeInstance attributeInstance = new EntityAttributeInstance(attribute, o1 -> {});
        attributeInstance.setBaseValue(baseValue);

        // Equipment
        for (EquipmentSlot equipmentSlot : EquipmentSlot.values()) {
            Item stack = entity.getEquippedStack(equipmentSlot).getItem();
            if (!(stack instanceof ArmorItem)) continue;
            List<AttributeModifiersComponent.Entry> entries = stack.getAttributeModifiers().modifiers();
            for (AttributeModifiersComponent.Entry entry : entries) {
                if (entry.attribute() == attribute) attributeInstance.addTemporaryModifier(entry.modifier());
            }
        }

        // Status effects
        for (var statusEffect : StatusEffectHelper.getStatusEffects(entity)) {
            Map<RegistryEntry<EntityAttribute>, StatusEffect.EffectAttributeModifierCreator> attributeModifiers = ((StatusEffectAccessor) statusEffect).getAttributeModifiers();
            attributeModifiers.forEach((entityAttribute, modifierCreator) -> {
                if (entityAttribute == attribute) attributeInstance.addPersistentModifier(modifierCreator.createAttributeModifier(statusEffect.getTranslationKey(), statusEffect.getAmplifier()));
            });
        }

        handleSpecialCases(entity, someAttribute -> someAttribute == attribute ? attributeInstance : null);

        // Copy tracked modifiers
        EntityAttributeInstance trackedInstance = entity.getAttributeInstance(attribute);
        if (trackedInstance != null) ((IEntityAttributeInstance) attributeInstance).meteor$copyFrom(trackedInstance);

        return attributeInstance;
    }

    /**
     * @see LivingEntity#getAttributeValue(RegistryEntry)
     */
    public static double getAttributeValue(LivingEntity entity, RegistryEntry<EntityAttribute> attribute) {
        return getAttributeInstance(entity, attribute).getValue();
    }

    private static void handleSpecialCases(LivingEntity entity, Function<RegistryEntry<EntityAttribute>, EntityAttributeInstance> consumer) {
        if (entity instanceof ShulkerEntity shulkerEntity) {
            if (shulkerEntity.getDataTracker().get(ShulkerEntityAccessor.meteor$getPeekAmount()) == 0) {
                @Nullable EntityAttributeInstance attributeInstance = consumer.apply(EntityAttributes.GENERIC_ARMOR);
                if (attributeInstance != null) attributeInstance.addPersistentModifier(ShulkerEntityAccessor.meteor$getCoveredArmorBonus());
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends LivingEntity> DefaultAttributeContainer getDefaultForEntity(T entity) {
        return DefaultAttributeRegistry.get((EntityType<? extends LivingEntity>) entity.getType());
    }
}

/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.entity;

import com.google.common.collect.Multimap;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.*;
import net.minecraft.item.ItemStack;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class EntityAttributeManager {
    /**
     * @see LivingEntity#getAttributes()
     */
    public static AttributeContainer getAttributes(LivingEntity entity) {
        if (entity == mc.player) return entity.getAttributes();

        @SuppressWarnings("unchecked")
        AttributeContainer attributes = new AttributeContainer(DefaultAttributeRegistry.get((EntityType<? extends LivingEntity>) entity.getType()));

        // Equipment
        for (var equipmentSlot : EquipmentSlot.values()) {
            ItemStack stack = entity.getEquippedStack(equipmentSlot);
            attributes.addTemporaryModifiers(stack.getAttributeModifiers(equipmentSlot));
        }

        // Status effects
        for (var statusEffect : entity.getStatusEffects()) {
            statusEffect.getEffectType().onApplied(entity, attributes, statusEffect.getAmplifier());
        }

        return attributes;
    }

    /**
     * @see LivingEntity#getAttributeInstance(EntityAttribute)
     */
    public static EntityAttributeInstance getAttributeInstance(LivingEntity entity, EntityAttribute attribute) {
        if (entity == mc.player) return entity.getAttributeInstance(attribute);

        double baseValue = DefaultAttributeRegistry.get((EntityType<? extends LivingEntity>) entity.getType()).getBaseValue(attribute);
        EntityAttributeInstance attributeInstance = new EntityAttributeInstance(attribute, o1 -> {});
        attributeInstance.setBaseValue(baseValue);

        // Equipment
        for (var equipmentSlot : EquipmentSlot.values()) {
            ItemStack stack = entity.getEquippedStack(equipmentSlot);
            Multimap<EntityAttribute, EntityAttributeModifier> modifiers = stack.getAttributeModifiers(equipmentSlot);
            for (var modifier : modifiers.get(attribute)) attributeInstance.addTemporaryModifier(modifier);
        }

        // Status effects
        for (var statusEffect : entity.getStatusEffects()) {
            EntityAttributeModifier modifier = statusEffect.getEffectType().getAttributeModifiers().get(EntityAttributes.GENERIC_ATTACK_DAMAGE);
            if (modifier == null) continue;
            attributeInstance.addTemporaryModifier(new EntityAttributeModifier(modifier.getId(), statusEffect.getTranslationKey() + " " + statusEffect.getAmplifier(), statusEffect.getEffectType().adjustModifierAmount(statusEffect.getAmplifier(), modifier), modifier.getOperation()));
        }

        return attributeInstance;
    }

    /**
     * @see LivingEntity#getAttributeValue(EntityAttribute)
     */
    public static double getAttributeValue(LivingEntity entity, EntityAttribute attribute) {
        if (entity == mc.player) return entity.getAttributeValue(attribute);

        return getAttributeInstance(entity, attribute).getValue();
    }
}

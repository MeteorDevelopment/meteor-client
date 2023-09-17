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
    public static double getAttributeValue(LivingEntity entity, EntityAttribute attribute) {
        if (entity == mc.player) return entity.getAttributeValue(attribute);

        EntityAttributeInstance attributeInstance = createDefault(entity, attribute);

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

        return attributeInstance.getValue();
    }

    @SuppressWarnings("unchecked")
    public static EntityAttributeInstance createDefault(LivingEntity entity, EntityAttribute attribute) {
        double baseValue = DefaultAttributeRegistry.get((EntityType<? extends LivingEntity>) entity.getType()).getBaseValue(attribute);
        EntityAttributeInstance attributeInstance = new EntityAttributeInstance(attribute, o -> {});
        attributeInstance.setBaseValue(baseValue);
        return attributeInstance;
    }
}

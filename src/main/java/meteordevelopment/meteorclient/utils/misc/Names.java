/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.misc;

import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.game.ResourcePacksReloadedEvent;
import meteordevelopment.meteorclient.utils.PreInit;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.sound.WeightedSoundSet;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtElement;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleType;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.StringHelper;
import org.apache.commons.lang3.text.WordUtils;

import java.util.HashMap;
import java.util.Map;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class Names {
    private static final Map<StatusEffect, String> statusEffectNames = new Reference2ObjectOpenHashMap<>(16);
    private static final Map<Item, String> itemNames = new Reference2ObjectOpenHashMap<>(128);
    private static final Map<Block, String> blockNames = new Reference2ObjectOpenHashMap<>(128);
    private static final Map<Enchantment, String> enchantmentNames = new Reference2ObjectOpenHashMap<>(16);
    private static final Map<EntityType<?>, String> entityTypeNames = new Reference2ObjectOpenHashMap<>(64);
    private static final Map<ParticleType<?>, String> particleTypesNames = new Reference2ObjectOpenHashMap<>(64);
    private static final Map<Identifier, String> soundNames = new HashMap<>(64);

    @PreInit
    public static void init() {
        MeteorClient.EVENT_BUS.subscribe(Names.class);
    }

    @EventHandler
    private static void onResourcePacksReloaded(ResourcePacksReloadedEvent event) {
        statusEffectNames.clear();
        itemNames.clear();
        blockNames.clear();
        enchantmentNames.clear();
        entityTypeNames.clear();
        particleTypesNames.clear();
        soundNames.clear();
    }

    public static String get(StatusEffect effect) {
        return statusEffectNames.computeIfAbsent(effect, effect1 -> StringHelper.stripTextFormat(I18n.translate(effect1.getTranslationKey())));
    }

    public static String get(Item item) {
        return itemNames.computeIfAbsent(item, item1 -> StringHelper.stripTextFormat(I18n.translate(item1.getTranslationKey())));
    }

    public static String get(Block block) {
        return blockNames.computeIfAbsent(block, block1 -> StringHelper.stripTextFormat(I18n.translate(block1.getTranslationKey())));
    }

    public static String get(Enchantment enchantment) {
        return enchantmentNames.computeIfAbsent(enchantment, enchantment1 -> StringHelper.stripTextFormat(I18n.translate(enchantment1.getTranslationKey())));
    }

    public static String get(EntityType<?> entityType) {
        return entityTypeNames.computeIfAbsent(entityType, entityType1 -> StringHelper.stripTextFormat(I18n.translate(entityType1.getTranslationKey())));
    }

    public static String get(ParticleType<?> type) {
        if (!(type instanceof ParticleEffect)) return "";
        return particleTypesNames.computeIfAbsent(type, effect1 -> WordUtils.capitalize(((ParticleEffect) effect1).asString().substring(10).replace("_", " ")));
    }

    public static String getSoundName(Identifier id) {
        return soundNames.computeIfAbsent(id, identifier -> {
            WeightedSoundSet soundSet = mc.getSoundManager().get(identifier);
            if (soundSet == null) return identifier.getPath();

            Text text = soundSet.getSubtitle();
            if (text == null) return identifier.getPath();

            return StringHelper.stripTextFormat(text.getString());
        });
    }

    public static String get(ItemStack stack) {
        return stack.hasNbt() && stack.getNbt().contains("display", NbtElement.COMPOUND_TYPE) ? stack.getName().getString() : I18n.translate(stack.getTranslationKey());
    }
}

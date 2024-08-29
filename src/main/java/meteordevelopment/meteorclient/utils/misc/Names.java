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
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.sound.WeightedSoundSet;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.StringHelper;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class Names {
    private static final Map<StatusEffect, String> statusEffectNames = new Reference2ObjectOpenHashMap<>(16);
    private static final Map<Item, String> itemNames = new Reference2ObjectOpenHashMap<>(128);
    private static final Map<Block, String> blockNames = new Reference2ObjectOpenHashMap<>(128);
    private static final Map<RegistryKey<Enchantment>, String> enchantmentKeyNames = new WeakHashMap<>(16);
    private static final Map<RegistryEntry<Enchantment>, String> enchantmentEntryNames = new Reference2ObjectOpenHashMap<>(16);
    private static final Map<EntityType<?>, String> entityTypeNames = new Reference2ObjectOpenHashMap<>(64);
    private static final Map<ParticleType<?>, String> particleTypesNames = new Reference2ObjectOpenHashMap<>(64);
    private static final Map<Identifier, String> soundNames = new HashMap<>(64);

    private Names() {
    }

    @PreInit
    public static void init() {
        MeteorClient.EVENT_BUS.subscribe(Names.class);
    }

    @EventHandler
    private static void onResourcePacksReloaded(ResourcePacksReloadedEvent event) {
        statusEffectNames.clear();
        itemNames.clear();
        blockNames.clear();
        enchantmentEntryNames.clear();
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

    /**
     * key -> entry, else key -> translation, else key -> identifier toString()
     * @author Crosby
     */
    @SuppressWarnings("StringEquality")
    public static String get(RegistryKey<Enchantment> enchantment) {
        return enchantmentKeyNames.computeIfAbsent(enchantment, enchantment1 -> Optional.ofNullable(MinecraftClient.getInstance().getNetworkHandler())
            .map(ClientPlayNetworkHandler::getRegistryManager)
            .flatMap(registryManager -> registryManager.getOptional(RegistryKeys.ENCHANTMENT))
            .flatMap(registry -> registry.getEntry(enchantment))
            .map(Names::get)
            .orElseGet(() -> {
                String key = "enchantment." + enchantment1.getValue().toTranslationKey();
                String translated = I18n.translate(key);
                return translated == key ? enchantment1.getValue().toString() : translated;
            }));
    }

    public static String get(RegistryEntry<Enchantment> enchantment) {
        return enchantmentEntryNames.computeIfAbsent(enchantment, enchantment1 -> StringHelper.stripTextFormat(enchantment.value().description().getString()));
    }

    public static String get(EntityType<?> entityType) {
        return entityTypeNames.computeIfAbsent(entityType, entityType1 -> StringHelper.stripTextFormat(I18n.translate(entityType1.getTranslationKey())));
    }

    public static String get(ParticleType<?> type) {
        if (!(type instanceof ParticleEffect)) return "";
        return particleTypesNames.computeIfAbsent(type, effect1 -> StringUtils.capitalize(Registries.PARTICLE_TYPE.getId(type).getPath().replace("_", " ")));
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
        return stack.getName().getString(); // pretty sure this is the same as it was
    }
}

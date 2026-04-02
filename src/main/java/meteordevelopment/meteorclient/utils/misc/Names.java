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
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.StringUtil;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.block.Block;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class Names {
    private static final Map<MobEffect, String> statusEffectNames = new Reference2ObjectOpenHashMap<>(16);
    private static final Map<Item, String> itemNames = new Reference2ObjectOpenHashMap<>(128);
    private static final Map<Block, String> blockNames = new Reference2ObjectOpenHashMap<>(128);
    private static final Map<ResourceKey<Enchantment>, String> enchantmentKeyNames = new WeakHashMap<>(16);
    private static final Map<Holder<Enchantment>, String> enchantmentEntryNames = new Reference2ObjectOpenHashMap<>(16);
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

    public static String get(MobEffect effect) {
        return statusEffectNames.computeIfAbsent(effect, effect1 -> StringUtil.stripColor(I18n.get(effect1.getDescriptionId())));
    }

    public static String get(Item item) {
        return itemNames.computeIfAbsent(item, item1 -> StringUtil.stripColor(I18n.get(item1.getDescriptionId())));
    }

    public static String get(Block block) {
        return blockNames.computeIfAbsent(block, block1 -> StringUtil.stripColor(I18n.get(block1.getDescriptionId())));
    }

    /**
     * key -> entry, else key -> translation, else key -> identifier toString()
     *
     * @author Crosby
     */
    @SuppressWarnings("StringEquality")
    public static String get(ResourceKey<Enchantment> enchantment) {
        return enchantmentKeyNames.computeIfAbsent(enchantment, enchantment1 -> Optional.ofNullable(Minecraft.getInstance().getConnection())
            .map(ClientPacketListener::registryAccess)
            .flatMap(registryManager -> registryManager.lookup(Registries.ENCHANTMENT))
            .flatMap(registry -> registry.get(enchantment.identifier()))
            .map(Names::get)
            .orElseGet(() -> {
                String key = "enchantment." + enchantment1.identifier().toLanguageKey();
                String translated = I18n.get(key);
                return translated == key ? enchantment1.identifier().toString() : translated;
            }));
    }

    public static String get(Holder<Enchantment> enchantment) {
        return enchantmentEntryNames.computeIfAbsent(enchantment, enchantment1 -> StringUtil.stripColor(enchantment.value().description().getString()));
    }

    public static String get(EntityType<?> entityType) {
        return entityTypeNames.computeIfAbsent(entityType, entityType1 -> StringUtil.stripColor(I18n.get(entityType1.getDescriptionId())));
    }

    public static String get(ParticleType<?> type) {
        if (!(type instanceof ParticleOptions)) return "";
        return particleTypesNames.computeIfAbsent(type, effect1 -> StringUtils.capitalize(BuiltInRegistries.PARTICLE_TYPE.getKey(type).getPath().replace("_", " ")));
    }

    public static String getSoundName(Identifier id) {
        return soundNames.computeIfAbsent(id, identifier -> {
            WeighedSoundEvents soundSet = mc.getSoundManager().getSoundEvent(identifier);
            if (soundSet == null) return identifier.getPath();

            Component text = soundSet.getSubtitle();
            if (text == null) return identifier.getPath();

            return StringUtil.stripColor(text.getString());
        });
    }

    public static String get(ItemStack stack) {
        return stack.getHoverName().getString(); // pretty sure this is the same as it was
    }
}

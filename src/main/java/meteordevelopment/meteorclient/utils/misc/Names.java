/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.misc;

import com.google.common.collect.ImmutableMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.game.ResourcePacksReloadedEvent;
import meteordevelopment.meteorclient.utils.PreInit;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
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

    private static final Map<Block, String> BLOCK_NAME_OVERRIDES = ImmutableMap.<Block, String>builder()
        .put(Blocks.WALL_TORCH, "Wall Torch")
        .put(Blocks.REDSTONE_WALL_TORCH, "Redstone Wall Torch")
        .put(Blocks.SOUL_WALL_TORCH, "Soul Wall Torch")
        .put(Blocks.COPPER_WALL_TORCH, "Copper Wall Torch")

        .put(Blocks.SKELETON_WALL_SKULL, "Skeleton Wall Skull")
        .put(Blocks.WITHER_SKELETON_WALL_SKULL, "Wither Skeleton Wall Skull")
        .put(Blocks.ZOMBIE_WALL_HEAD, "Zombie Wall Head")
        .put(Blocks.PLAYER_WALL_HEAD, "Player Wall Head")
        .put(Blocks.DRAGON_WALL_HEAD, "Dragon Wall Head")
        .put(Blocks.PIGLIN_WALL_HEAD, "Piglin Wall Head")
        .put(Blocks.CREEPER_WALL_HEAD, "Creeper Wall Head")

        .put(Blocks.OAK_WALL_SIGN, "Oak Wall Sign")
        .put(Blocks.BIRCH_WALL_SIGN, "Birch Wall Sign")
        .put(Blocks.SPRUCE_WALL_SIGN, "Spruce Wall Sign")
        .put(Blocks.ACACIA_WALL_SIGN, "Acacia Wall Sign")
        .put(Blocks.CHERRY_WALL_SIGN, "Cherry Wall Sign")
        .put(Blocks.JUNGLE_WALL_SIGN, "Jungle Wall Sign")
        .put(Blocks.BAMBOO_WALL_SIGN, "Bamboo Wall Sign")
        .put(Blocks.WARPED_WALL_SIGN, "Warped Wall Sign")
        .put(Blocks.CRIMSON_WALL_SIGN, "Crimson Wall Sign")
        .put(Blocks.DARK_OAK_WALL_SIGN, "Dark Oak Wall Sign")
        .put(Blocks.PALE_OAK_WALL_SIGN, "Pale Oak Wall Sign")
        .put(Blocks.MANGROVE_WALL_SIGN, "Mangrove Wall Sign")

        .put(Blocks.OAK_WALL_HANGING_SIGN, "Oak Wall Hanging Sign")
        .put(Blocks.BIRCH_WALL_HANGING_SIGN, "Birch Wall Hanging Sign")
        .put(Blocks.SPRUCE_WALL_HANGING_SIGN, "Spruce Wall Hanging Sign")
        .put(Blocks.ACACIA_WALL_HANGING_SIGN, "Acacia Wall Hanging Sign")
        .put(Blocks.CHERRY_WALL_HANGING_SIGN, "Cherry Wall Hanging Sign")
        .put(Blocks.JUNGLE_WALL_HANGING_SIGN, "Jungle Wall Hanging Sign")
        .put(Blocks.BAMBOO_WALL_HANGING_SIGN, "Bamboo Wall Hanging Sign")
        .put(Blocks.WARPED_WALL_HANGING_SIGN, "Warped Wall Hanging Sign")
        .put(Blocks.CRIMSON_WALL_HANGING_SIGN, "Crimson Wall Hanging Sign")
        .put(Blocks.DARK_OAK_WALL_HANGING_SIGN, "Dark Oak Wall Hanging Sign")
        .put(Blocks.PALE_OAK_WALL_HANGING_SIGN, "Pale Oak Wall Hanging Sign")
        .put(Blocks.MANGROVE_WALL_HANGING_SIGN, "Mangrove Wall Hanging Sign")

        .put(Blocks.YELLOW_WALL_BANNER, "Yellow Wall Banner")
        .put(Blocks.RED_WALL_BANNER, "Red Wall Banner")
        .put(Blocks.LIME_WALL_BANNER, "Lime Wall Banner")
        .put(Blocks.PINK_WALL_BANNER, "Pink Wall Banner")
        .put(Blocks.GRAY_WALL_BANNER, "Gray Wall Banner")
        .put(Blocks.CYAN_WALL_BANNER, "Cyan Wall Banner")
        .put(Blocks.BLUE_WALL_BANNER, "Blue Wall Banner")
        .put(Blocks.WHITE_WALL_BANNER, "White Wall Banner")
        .put(Blocks.LIGHT_BLUE_WALL_BANNER, "Light Blue Wall Banner")
        .put(Blocks.BROWN_WALL_BANNER, "Brown Wall Banner")
        .put(Blocks.GREEN_WALL_BANNER, "Green Wall Banner")
        .put(Blocks.BLACK_WALL_BANNER, "Black Wall Banner")
        .put(Blocks.ORANGE_WALL_BANNER, "Orange Wall Banner")
        .put(Blocks.PURPLE_WALL_BANNER, "Purple Wall Banner")
        .put(Blocks.MAGENTA_WALL_BANNER, "Magenta Wall Banner")
        .put(Blocks.LIGHT_GRAY_WALL_BANNER, "Light Gray Wall Banner")
        .buildOrThrow();

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
        return blockNames.computeIfAbsent(block, block1 -> {
            if (BLOCK_NAME_OVERRIDES.containsKey(block) && mc.options.language.startsWith("en_")) {
                return BLOCK_NAME_OVERRIDES.get(block);
            } else {
                return StringHelper.stripTextFormat(I18n.translate(block.getTranslationKey()));
            }
        });
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
            .flatMap(registry -> registry.getEntry(enchantment.getValue()))
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

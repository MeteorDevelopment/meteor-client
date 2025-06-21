/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.render;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import meteordevelopment.meteorclient.events.game.ItemStackTooltipEvent;
import meteordevelopment.meteorclient.events.render.TooltipDataEvent;
import meteordevelopment.meteorclient.mixin.EntityAccessor;
import meteordevelopment.meteorclient.mixin.EntityBucketItemAccessor;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.ByteCountDataOutput;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.player.EChestMemory;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.tooltip.*;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.*;
import net.minecraft.component.type.SuspiciousStewEffectsComponent.StewEffect;
import net.minecraft.entity.Bucketable;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffectUtil;
import net.minecraft.item.*;
import net.minecraft.item.consume.ApplyEffectsConsumeEffect;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.MutableText;
import net.minecraft.text.RawFilteredPair;
import net.minecraft.text.Text;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Formatting;

import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_ALT;

public class BetterTooltips extends Module {
    public static final Color ECHEST_COLOR = new Color(0, 50, 50);
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgPreviews = settings.createGroup("Previews");
    private final SettingGroup sgOther = settings.createGroup("Other");
    private final SettingGroup sgHideFlags = settings.createGroup("Hide Flags");

    // General

    private final Setting<DisplayWhen> displayWhen = sgGeneral.add(new EnumSetting.Builder<DisplayWhen>()
        .name("display-when")
        .description("When to display previews.")
        .defaultValue(DisplayWhen.Keybind)
        .onChanged(value -> updateTooltips = true)
        .build()
    );

    private final Setting<Keybind> keybind = sgGeneral.add(new KeybindSetting.Builder()
        .name("keybind")
        .description("The bind for keybind mode.")
        .defaultValue(Keybind.fromKey(GLFW_KEY_LEFT_ALT))
        .visible(() -> displayWhen.get() == DisplayWhen.Keybind)
        .onChanged(value -> updateTooltips = true)
        .build()
    );

    private final Setting<Boolean> middleClickOpen = sgGeneral.add(new BoolSetting.Builder()
        .name("middle-click-open")
        .description("Opens a GUI window with the inventory of the storage block or book when you middle click the item.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> pauseInCreative = sgGeneral.add(new BoolSetting.Builder()
        .name("pause-in-creative")
        .description("Pauses middle click open while the player is in creative mode.")
        .defaultValue(true)
        .visible(middleClickOpen::get)
        .build()
    );

    // Previews

    private final Setting<Boolean> shulkers = sgPreviews.add(new BoolSetting.Builder()
        .name("containers")
        .description("Shows a preview of a containers when hovering over it in an inventory.")
        .defaultValue(true)
        .onChanged(value -> updateTooltips = true)
        .build()
    );

    private final Setting<Boolean> shulkerCompactTooltip = sgPreviews.add(new BoolSetting.Builder()
        .name("compact-shulker-tooltip")
        .description("Compacts the lines of the shulker tooltip.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> echest = sgPreviews.add(new BoolSetting.Builder()
        .name("echests")
        .description("Shows a preview of your echest when hovering over it in an inventory.")
        .defaultValue(true)
        .onChanged(value -> updateTooltips = true)
        .build()
    );

    private final Setting<Boolean> maps = sgPreviews.add(new BoolSetting.Builder()
        .name("maps")
        .description("Shows a preview of a map when hovering over it in an inventory.")
        .defaultValue(true)
        .onChanged(value -> updateTooltips = true)
        .build()
    );

    public final Setting<Double> mapsScale = sgPreviews.add(new DoubleSetting.Builder()
        .name("map-scale")
        .description("The scale of the map preview.")
        .defaultValue(1)
        .min(0.001)
        .sliderMax(1)
        .visible(maps::get)
        .build()
    );

    private final Setting<Boolean> books = sgPreviews.add(new BoolSetting.Builder()
        .name("books")
        .description("Shows contents of a book when hovering over it in an inventory.")
        .defaultValue(true)
        .onChanged(value -> updateTooltips = true)
        .build()
    );

    private final Setting<Boolean> banners = sgPreviews.add(new BoolSetting.Builder()
        .name("banners")
        .description("Shows banners' patterns when hovering over it in an inventory. Also works with shields.")
        .defaultValue(true)
        .onChanged(value -> updateTooltips = true)
        .build()
    );

    private final Setting<Boolean> entitiesInBuckets = sgPreviews.add(new BoolSetting.Builder()
        .name("entities-in-buckets")
        .description("Shows entities in buckets when hovering over it in an inventory.")
        .defaultValue(true)
        .onChanged(value -> updateTooltips = true)
        .build()
    );

    // Extras

    public final Setting<Boolean> byteSize = sgOther.add(new BoolSetting.Builder()
        .name("byte-size")
        .description("Displays an item's size in bytes in the tooltip.")
        .defaultValue(true)
        .onChanged(value -> updateTooltips = true)
        .build()
    );

    private final Setting<Boolean> statusEffects = sgOther.add(new BoolSetting.Builder()
        .name("status-effects")
        .description("Adds list of status effects to tooltips of food items.")
        .defaultValue(true)
        .onChanged(value -> updateTooltips = true)
        .build()
    );

    // Hide flags

    public final Setting<Boolean> tooltip = sgHideFlags.add(new BoolSetting.Builder()
        .name("tooltip")
        .description("Show the tooltip when it's hidden.")
        .defaultValue(false)
        .build()
    );

    public final Setting<Boolean> additional = sgHideFlags.add(new BoolSetting.Builder()
        .name("tooltip-components")
        .description("Shows tooltip components when they're hidden - e.g. enchantments, attributes, lore, etc.")
        .defaultValue(false)
        .build()
    );

    private boolean updateTooltips = false;
    private static final ItemStack[] ITEMS = new ItemStack[27];

    public BetterTooltips() {
        super(Categories.Render, "better-tooltips", "Displays more useful tooltips for certain items.");
    }

    @EventHandler
    private void appendTooltip(ItemStackTooltipEvent event) {
        // Hide hidden (empty) tooltips unless the tooltip hide flag setting is true.
        if (!tooltip.get() && event.list().isEmpty()) {
            // Hold-to-preview tooltip text is always added when needed.
            appendPreviewTooltipText(event, false);
            return;
        }

        // Status effects
        if (statusEffects.get()) {
            if (event.itemStack().getItem() == Items.SUSPICIOUS_STEW) {
                SuspiciousStewEffectsComponent stewEffectsComponent = event.itemStack().get(DataComponentTypes.SUSPICIOUS_STEW_EFFECTS);
                if (stewEffectsComponent != null) {
                    for (StewEffect effectTag : stewEffectsComponent.effects()) {
                        StatusEffectInstance effect = new StatusEffectInstance(effectTag.effect(), effectTag.duration(), 0);
                        event.appendStart(getStatusText(effect));
                    }
                }
            } else {
                ConsumableComponent consumable = event.itemStack().get(DataComponentTypes.CONSUMABLE);
                if (consumable != null) {
                    consumable.onConsumeEffects().stream()
                        .filter(ApplyEffectsConsumeEffect.class::isInstance)
                        .map(ApplyEffectsConsumeEffect.class::cast)
                        .flatMap(apply -> apply.effects().stream())
                        .forEach(effect -> event.appendStart(getStatusText(effect)));
                }
            }
        }

        // Item size tooltip
        if (byteSize.get()) {
            try {
                event.itemStack().toNbt(mc.player.getRegistryManager()).write(ByteCountDataOutput.INSTANCE);

                int byteCount = ByteCountDataOutput.INSTANCE.getCount();
                String count;

                ByteCountDataOutput.INSTANCE.reset();

                if (byteCount >= 1024) count = String.format("%.2f kb", byteCount / (float) 1024);
                else count = String.format("%d bytes", byteCount);

                event.appendEnd(Text.literal(count).formatted(Formatting.GRAY));
            } catch (Exception e) {
                event.appendEnd(Text.literal("Error getting bytes.").formatted(Formatting.RED));
            }
        }

        // Hold to preview tooltip
        appendPreviewTooltipText(event, true);
    }

    @EventHandler
    private void getTooltipData(TooltipDataEvent event) {
        // Container preview
        if (previewShulkers() && Utils.hasItems(event.itemStack)) {
            Utils.getItemsInContainerItem(event.itemStack, ITEMS);
            event.tooltipData = new ContainerTooltipComponent(ITEMS, Utils.getShulkerColor(event.itemStack));
        }

        // EChest preview
        else if (event.itemStack.getItem() == Items.ENDER_CHEST && previewEChest()) {
            event.tooltipData = EChestMemory.isKnown()
                ? new ContainerTooltipComponent(EChestMemory.ITEMS.toArray(new ItemStack[27]), ECHEST_COLOR)
                : new TextTooltipComponent(Text.literal("Unknown inventory.").formatted(Formatting.DARK_RED));
        }

        // Map preview
        else if (event.itemStack.getItem() == Items.FILLED_MAP && previewMaps()) {
            MapIdComponent mapIdComponent = event.itemStack.get(DataComponentTypes.MAP_ID);
            if (mapIdComponent != null) event.tooltipData = new MapTooltipComponent(mapIdComponent.id());
        }

        // Book preview
        else if ((event.itemStack.getItem() == Items.WRITABLE_BOOK || event.itemStack.getItem() == Items.WRITTEN_BOOK) && previewBooks()) {
            Text page = getFirstPage(event.itemStack);
            if (page != null) event.tooltipData = new BookTooltipComponent(page);
        }

        // Banner preview
        else if (event.itemStack.getItem() instanceof BannerItem && previewBanners()) {
            event.tooltipData = new BannerTooltipComponent(event.itemStack);
        } else if (event.itemStack.contains(DataComponentTypes.PROVIDES_BANNER_PATTERNS) && previewBanners()) {
            event.tooltipData = createBannerFromBannerPatternItem(event.itemStack);
        } else if (event.itemStack.getItem() == Items.SHIELD && previewBanners()) {
            if (!event.itemStack.getOrDefault(DataComponentTypes.BANNER_PATTERNS, BannerPatternsComponent.DEFAULT).layers().isEmpty()) {
                event.tooltipData = createBannerFromShield(event.itemStack);
            }
        }

        // Fish peek
        else if (event.itemStack.getItem() instanceof EntityBucketItem bucketItem && previewEntities()) {
            EntityType<?> type = ((EntityBucketItemAccessor) bucketItem).getEntityType();
            Entity entity = type.create(mc.world, SpawnReason.NATURAL);
            if (entity != null) {
                NbtComponent nbtComponent = event.itemStack.getOrDefault(DataComponentTypes.BUCKET_ENTITY_DATA, NbtComponent.DEFAULT);
                if (nbtComponent.isEmpty()) {
                    return;
                }

                ((Bucketable) entity).copyDataFromNbt(nbtComponent.copyNbt());
                ((EntityAccessor) entity).setInWater(true);
                event.tooltipData = new EntityTooltipComponent(entity);
            }
        }
    }

    public void applyCompactShulkerTooltip(List<ItemStack> stacks, Consumer<Text> textConsumer) {
        Object2IntMap<Item> counts = new Object2IntOpenHashMap<>();

        for (ItemStack item : stacks) {
            if (item.isEmpty()) continue;

            int count = counts.getInt(item.getItem());
            counts.put(item.getItem(), count + item.getCount());
        }

        counts.keySet().stream().sorted(Comparator.comparingInt(value -> -counts.getInt(value))).limit(5).forEach(item -> {
            MutableText mutableText = item.getName().copyContentOnly();
            mutableText.append(Text.literal(" x").append(String.valueOf(counts.getInt(item))).formatted(Formatting.GRAY));
            textConsumer.accept(mutableText);
        });

        if (counts.size() > 5) {
            textConsumer.accept((Text.translatable("container.shulkerBox.more", counts.size() - 5)).formatted(Formatting.ITALIC));
        }
    }

    private void appendPreviewTooltipText(ItemStackTooltipEvent event, boolean spacer) {
        if (!isPressed() && (
            shulkers.get() && Utils.hasItems(event.itemStack())
                || (event.itemStack().getItem() == Items.ENDER_CHEST && echest.get())
                || (event.itemStack().getItem() == Items.FILLED_MAP && maps.get())
                || (event.itemStack().getItem() == Items.WRITABLE_BOOK && books.get())
                || (event.itemStack().getItem() == Items.WRITTEN_BOOK && books.get())
                || (event.itemStack().getItem() instanceof EntityBucketItem && entitiesInBuckets.get())
                || (event.itemStack().getItem() instanceof BannerItem && banners.get())
                || (event.itemStack().contains(DataComponentTypes.PROVIDES_BANNER_PATTERNS) && banners.get())
                || (event.itemStack().getItem() == Items.SHIELD && banners.get())
        )) {
            // we don't want to add the spacer if the tooltip is hidden
            if (spacer) event.appendEnd(Text.literal(""));
            event.appendEnd(Text.literal("Hold " + Formatting.YELLOW + keybind + Formatting.RESET + " to preview"));
        }
    }

    private MutableText getStatusText(StatusEffectInstance effect) {
        MutableText text = Text.translatable(effect.getTranslationKey());
        if (effect.getAmplifier() != 0) {
            text.append(String.format(" %d (%s)", effect.getAmplifier() + 1, StatusEffectUtil.getDurationText(effect, 1, mc.world.getTickManager().getTickRate()).getString()));
        } else {
            text.append(String.format(" (%s)", StatusEffectUtil.getDurationText(effect, 1, mc.world.getTickManager().getTickRate()).getString()));
        }

        if (effect.getEffectType().value().isBeneficial()) return text.formatted(Formatting.BLUE);
        return text.formatted(Formatting.RED);
    }

    @SuppressWarnings("DataFlowIssue")
    private Text getFirstPage(ItemStack bookItem) {
        if (bookItem.get(DataComponentTypes.WRITABLE_BOOK_CONTENT) != null) {
            List<RawFilteredPair<String>> pages = bookItem.get(DataComponentTypes.WRITABLE_BOOK_CONTENT).pages();

            if (pages.isEmpty()) return null;
            return Text.literal(pages.getFirst().get(false));
        } else if (bookItem.get(DataComponentTypes.WRITTEN_BOOK_CONTENT) != null) {
            List<RawFilteredPair<Text>> pages = bookItem.get(DataComponentTypes.WRITTEN_BOOK_CONTENT).pages();
            if (pages.isEmpty()) return null;

            return pages.getFirst().get(false);
        }

        return null;
    }

    private BannerTooltipComponent createBannerFromBannerPatternItem(ItemStack item) {
        // I can't imagine getting the banner pattern from a banner pattern item would fail without some serious messing around
        BannerPatternsComponent component = new BannerPatternsComponent.Builder().add(mc.player.getRegistryManager().getOrThrow(RegistryKeys.BANNER_PATTERN).getOrThrow(item.get(DataComponentTypes.PROVIDES_BANNER_PATTERNS)).get(0), DyeColor.WHITE).build();
        return new BannerTooltipComponent(DyeColor.GRAY, component);
    }

    private BannerTooltipComponent createBannerFromShield(ItemStack shieldItem) {
        DyeColor dyeColor2 = shieldItem.getOrDefault(DataComponentTypes.BASE_COLOR, DyeColor.WHITE);
        BannerPatternsComponent bannerPatternsComponent = shieldItem.getOrDefault(DataComponentTypes.BANNER_PATTERNS, BannerPatternsComponent.DEFAULT);
        return new BannerTooltipComponent(dyeColor2, bannerPatternsComponent);
    }

    public boolean middleClickOpen() {
        return (isActive() && middleClickOpen.get()) && (!pauseInCreative.get() || !mc.player.isInCreativeMode());
    }

    public boolean previewShulkers() {
        return isActive() && isPressed() && shulkers.get();
    }

    public boolean shulkerCompactTooltip() {
        return isActive() && shulkerCompactTooltip.get();
    }

    private boolean previewEChest() {
        return isPressed() && echest.get();
    }

    private boolean previewMaps() {
        return isPressed() && maps.get();
    }

    private boolean previewBooks() {
        return isPressed() && books.get();
    }

    private boolean previewBanners() {
        return isPressed() && banners.get();
    }

    private boolean previewEntities() {
        return isPressed() && entitiesInBuckets.get();
    }

    private boolean isPressed() {
        return (keybind.get().isPressed() && displayWhen.get() == DisplayWhen.Keybind) || displayWhen.get() == DisplayWhen.Always;
    }

    public boolean updateTooltips() {
        if (updateTooltips && isActive()) {
            updateTooltips = false;
            return true;
        }

        return false;
    }

    public enum DisplayWhen {
        Keybind,
        Always
    }
}

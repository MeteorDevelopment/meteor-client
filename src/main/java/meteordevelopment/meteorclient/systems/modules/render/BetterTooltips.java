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
import net.minecraft.component.ComponentMap;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.*;
import net.minecraft.component.type.BannerPatternsComponent.Layer;
import net.minecraft.component.type.SuspiciousStewEffectsComponent.StewEffect;
import net.minecraft.entity.Bucketable;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffectUtil;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.*;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.text.MutableText;
import net.minecraft.text.RawFilteredPair;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.collection.DefaultedList;

import java.util.Comparator;
import java.util.List;

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
        .build()
    );

    private final Setting<Keybind> keybind = sgGeneral.add(new KeybindSetting.Builder()
        .name("keybind")
        .description("The bind for keybind mode.")
        .defaultValue(Keybind.fromKey(GLFW_KEY_LEFT_ALT))
        .visible(() -> displayWhen.get() == DisplayWhen.Keybind)
        .build()
    );

    private final Setting<Boolean> middleClickOpen = sgGeneral.add(new BoolSetting.Builder()
        .name("middle-click-open")
        .description("Opens a GUI window with the inventory of the storage block or book when you middle click the item.")
        .defaultValue(true)
        .build()
    );

    // Previews

    private final Setting<Boolean> shulkers = sgPreviews.add(new BoolSetting.Builder()
        .name("containers")
        .description("Shows a preview of a containers when hovering over it in an inventory.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> shulkerCompactTooltip = sgPreviews.add(new BoolSetting.Builder()
        .name("compact-shulker-tooltip")
        .description("Compacts the lines of the shulker tooltip.")
        .defaultValue(true)
        .visible(shulkers::get)
        .build()
    );

    public final Setting<Boolean> echest = sgPreviews.add(new BoolSetting.Builder()
        .name("echests")
        .description("Shows a preview of your echest when hovering over it in an inventory.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> maps = sgPreviews.add(new BoolSetting.Builder()
        .name("maps")
        .description("Shows a preview of a map when hovering over it in an inventory.")
        .defaultValue(true)
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
        .build()
    );

    private final Setting<Boolean> banners = sgPreviews.add(new BoolSetting.Builder()
        .name("banners")
        .description("Shows banners' patterns when hovering over it in an inventory. Also works with shields.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> entitiesInBuckets = sgPreviews.add(new BoolSetting.Builder()
        .name("entities-in-buckets")
        .description("Shows entities in buckets when hovering over it in an inventory.")
        .defaultValue(true)
        .build()
    );

    // Extras

    public final Setting<Boolean> byteSize = sgOther.add(new BoolSetting.Builder()
        .name("byte-size")
        .description("Displays an item's size in bytes in the tooltip.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> statusEffects = sgOther.add(new BoolSetting.Builder()
        .name("status-effects")
        .description("Adds list of status effects to tooltips of food items.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> beehive = sgOther.add(new BoolSetting.Builder()
        .name("beehive")
        .description("Displays information about a beehive or bee nest.")
        .defaultValue(true)
        .build()
    );

    //Hide flags

    public final Setting<Boolean> tooltip = sgHideFlags.add(new BoolSetting.Builder()
        .name("tooltip")
        .description("Show the tooltip when it's hidden.")
        .defaultValue(false)
        .build()
    );

    public final Setting<Boolean> enchantments = sgHideFlags.add(new BoolSetting.Builder()
        .name("enchantments")
        .description("Show enchantments when it's hidden.")
        .defaultValue(false)
        .build()
    );

    public final Setting<Boolean> modifiers = sgHideFlags.add(new BoolSetting.Builder()
        .name("modifiers")
        .description("Show item modifiers when it's hidden.")
        .defaultValue(false)
        .build()
    );

    public final Setting<Boolean> unbreakable = sgHideFlags.add(new BoolSetting.Builder()
        .name("unbreakable")
        .description("Show \"Unbreakable\" tag when it's hidden.")
        .defaultValue(false)
        .build()
    );

    public final Setting<Boolean> canDestroy = sgHideFlags.add(new BoolSetting.Builder()
        .name("can-destroy")
        .description("Show \"CanDestroy\" tag when it's hidden.")
        .defaultValue(false)
        .build()
    );

    public final Setting<Boolean> canPlaceOn = sgHideFlags.add(new BoolSetting.Builder()
        .name("can-place-on")
        .description("Show \"CanPlaceOn\" tag when it's hidden.")
        .defaultValue(false)
        .build()
    );

    public final Setting<Boolean> additional = sgHideFlags.add(new BoolSetting.Builder()
        .name("additional")
        .description("Show potion effects, firework status, book author, etc when it's hidden.")
        .defaultValue(false)
        .build()
    );

    public final Setting<Boolean> dye = sgHideFlags.add(new BoolSetting.Builder()
        .name("dye")
        .description("Show dyed item tags when it's hidden.")
        .defaultValue(false)
        .build()
    );

    public final Setting<Boolean> upgrades = sgHideFlags.add(new BoolSetting.Builder()
        .name("armor-trim")
        .description("Show armor trims when it's hidden.")
        .defaultValue(false)
        .build()
    );

    public BetterTooltips() {
        super(Categories.Render, "better-tooltips", "Displays more useful tooltips for certain items.");
    }

    @EventHandler
    private void appendTooltip(ItemStackTooltipEvent event) {
        // Status effects
        if (statusEffects.get()) {
            if (event.itemStack.getItem() == Items.SUSPICIOUS_STEW) {
                SuspiciousStewEffectsComponent stewEffectsComponent = event.itemStack.get(DataComponentTypes.SUSPICIOUS_STEW_EFFECTS);
                if (stewEffectsComponent != null) {
                    for (StewEffect effectTag : stewEffectsComponent.effects()) {
                        StatusEffectInstance effect = new StatusEffectInstance(effectTag.effect(), effectTag.duration(), 0);
                        event.list.add(1, getStatusText(effect));
                    }
                }
            } else {
                FoodComponent food = event.itemStack.get(DataComponentTypes.FOOD);
                if (food != null) {
                    food.effects().forEach(e -> event.list.add(1, getStatusText(e.effect())));
                }
            }
        }

        //Beehive
        if (beehive.get()) {
            if (event.itemStack.getItem() == Items.BEEHIVE || event.itemStack.getItem() == Items.BEE_NEST) {
                ComponentMap components = event.itemStack.getComponents();
                BlockStateComponent blockStateComponent = components.get(DataComponentTypes.BLOCK_STATE);
                if (blockStateComponent != null) {
                    String level = blockStateComponent.properties().get("honey_level");
                    event.list.add(1, Text.literal(String.format("%sHoney level: %s%s%s.", Formatting.GRAY, Formatting.YELLOW, level, Formatting.GRAY)));
                }

                NbtComponent nbtComponent = components.get(DataComponentTypes.BLOCK_ENTITY_DATA);
                if (nbtComponent != null) {
                    NbtList beesTag = nbtComponent.copyNbt().getList("Bees", 10);
                    event.list.add(1, Text.literal(String.format("%sBees: %s%d%s.", Formatting.GRAY, Formatting.YELLOW, beesTag.size(), Formatting.GRAY)));
                }
            }
        }

        // Item size tooltip
        if (byteSize.get()) {
            try {
                event.itemStack.encode(mc.player.getRegistryManager()).write(ByteCountDataOutput.INSTANCE);

                int byteCount = ByteCountDataOutput.INSTANCE.getCount();
                String count;

                ByteCountDataOutput.INSTANCE.reset();

                if (byteCount >= 1024) count = String.format("%.2f kb", byteCount / (float) 1024);
                else count = String.format("%d bytes", byteCount);

                event.list.add(Text.literal(count).formatted(Formatting.GRAY));
            } catch (Exception e) {
                event.list.add(Text.literal("Error getting bytes.").formatted(Formatting.RED));
            }
        }

        // Hold to preview tooltip
        if ((shulkers.get() && !previewShulkers() && Utils.hasItems(event.itemStack))
            || (event.itemStack.getItem() == Items.ENDER_CHEST && echest.get() && !previewEChest())
            || (event.itemStack.getItem() == Items.FILLED_MAP && maps.get() && !previewMaps())
            || (event.itemStack.getItem() == Items.WRITABLE_BOOK && books.get() && !previewBooks())
            || (event.itemStack.getItem() == Items.WRITTEN_BOOK && books.get() && !previewBooks())
            || (event.itemStack.getItem() instanceof EntityBucketItem && entitiesInBuckets.get() && !previewEntities())
            || (event.itemStack.getItem() instanceof BannerItem && banners.get() && !previewBanners())
            || (event.itemStack.getItem() instanceof BannerPatternItem && banners.get() && !previewBanners())
            || (event.itemStack.getItem() == Items.SHIELD && banners.get() && !previewBanners())) {
            event.list.add(Text.literal(""));
            event.list.add(Text.literal("Hold " + Formatting.YELLOW + keybind + Formatting.RESET + " to preview"));
        }
    }

    @EventHandler
    private void getTooltipData(TooltipDataEvent event) {
        // Container preview
        if (previewShulkers() && Utils.hasItems(event.itemStack)) {
            ItemStack[] itemStacks = new ItemStack[27];
            Utils.getItemsInContainerItem(event.itemStack, itemStacks);
            event.tooltipData = new ContainerTooltipComponent(itemStacks, Utils.getShulkerColor(event.itemStack));
        }

        // EChest preview
        else if (event.itemStack.getItem() == Items.ENDER_CHEST && previewEChest()) {
            event.tooltipData = EChestMemory.isKnown()
                ? new ContainerTooltipComponent(EChestMemory.ITEMS.toArray(new ItemStack[27]), ECHEST_COLOR)
                : new TextTooltipComponent(Text.literal("Unknown ender chest inventory.").formatted(Formatting.DARK_RED));
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
        } else if (event.itemStack.getItem() instanceof BannerPatternItem && previewBanners()) {
            BannerPatternsComponent bannerPatternsComponent = event.itemStack.get(DataComponentTypes.BANNER_PATTERNS);
            if (bannerPatternsComponent != null) {
                event.tooltipData = new BannerTooltipComponent(createBannerFromLayers(bannerPatternsComponent.layers()));
            }
        } else if (event.itemStack.getItem() == Items.SHIELD && previewBanners()) {
            ItemStack banner = createBannerFromShield(event.itemStack);
            if (banner != null) event.tooltipData = new BannerTooltipComponent(banner);
        }

        // Fish peek
        else if (event.itemStack.getItem() instanceof EntityBucketItem bucketItem && previewEntities()) {
            EntityType<?> type = ((EntityBucketItemAccessor) bucketItem).getEntityType();
            Entity entity = type.create(mc.world);
            if (entity != null) {
                ((Bucketable) entity).copyDataFromNbt(event.itemStack.get(DataComponentTypes.BUCKET_ENTITY_DATA).copyNbt());
                ((EntityAccessor) entity).setInWater(true);
                event.tooltipData = new EntityTooltipComponent(entity);
            }
        }
    }

    public void applyCompactShulkerTooltip(ItemStack shulkerItem, List<Text> tooltip) {
        NbtComponent nbtComponent = shulkerItem.get(DataComponentTypes.BLOCK_ENTITY_DATA);

        if (nbtComponent != null) {
            if (nbtComponent.contains("LootTable")) {
                tooltip.add(Text.literal("???????"));
            }

            if (nbtComponent.contains("Items")) {
                DefaultedList<ItemStack> items = DefaultedList.ofSize(27, ItemStack.EMPTY);
                Inventories.readNbt(nbtComponent.copyNbt(), items, DynamicRegistryManager.EMPTY);

                Object2IntMap<Item> counts = new Object2IntOpenHashMap<>();

                for (ItemStack item : items) {
                    if (item.isEmpty()) continue;

                    int count = counts.getInt(item.getItem());
                    counts.put(item.getItem(), count + item.getCount());
                }

                counts.keySet().stream().sorted(Comparator.comparingInt(value -> -counts.getInt(value))).limit(5).forEach(item -> {
                    MutableText mutableText = item.getName().copyContentOnly();
                    mutableText.append(Text.literal(" x").append(String.valueOf(counts.getInt(item))).formatted(Formatting.GRAY));
                    tooltip.add(mutableText);
                });

                if (counts.size() > 5) {
                    tooltip.add((Text.translatable("container.shulkerBox.more", counts.size() - 5)).formatted(Formatting.ITALIC));
                }
            }
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

    private Text getFirstPage(ItemStack bookItem) {
        if (bookItem.get(DataComponentTypes.WRITABLE_BOOK_CONTENT) != null) {
            List<RawFilteredPair<String>> pages = bookItem.get(DataComponentTypes.WRITABLE_BOOK_CONTENT).pages();

            if (pages.isEmpty()) return null;
            return Text.literal(pages.getFirst().get(false));
        }
        else if (bookItem.get(DataComponentTypes.WRITTEN_BOOK_CONTENT) != null) {
            List<RawFilteredPair<Text>> pages = bookItem.get(DataComponentTypes.WRITTEN_BOOK_CONTENT).pages();
            if (pages.isEmpty()) return null;

            return pages.getFirst().get(false);
        }

        return null;
    }

    private ItemStack createBannerFromLayers(List<Layer> pattern) {
        ItemStack bannerItem = new ItemStack(Items.GRAY_BANNER);
        BannerPatternsComponent bannerPatterns = bannerItem.get(DataComponentTypes.BANNER_PATTERNS);
        bannerPatterns.layers().addAll(pattern);
        bannerItem.set(DataComponentTypes.BANNER_PATTERNS, bannerPatterns);
        return bannerItem;
    }

    private ItemStack createBannerFromShield(ItemStack shieldItem) {
        if (!shieldItem.getComponents().isEmpty()
            || shieldItem.get(DataComponentTypes.BLOCK_ENTITY_DATA) == null
            || shieldItem.get(DataComponentTypes.BASE_COLOR) == null)
            return null;
        ItemStack bannerItem = new ItemStack(Items.GRAY_BANNER);
        BannerPatternsComponent bannerPatternsComponent = bannerItem.get(DataComponentTypes.BANNER_PATTERNS);
        BannerPatternsComponent shieldPatternsComponent = shieldItem.get(DataComponentTypes.BANNER_PATTERNS);
        if (shieldPatternsComponent == null) return bannerItem;
        bannerPatternsComponent.layers().addAll(shieldPatternsComponent.layers());
        return bannerItem;
    }

    public boolean middleClickOpen() {
        return isActive() && middleClickOpen.get();
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

    public enum DisplayWhen {
        Keybind,
        Always
    }
}

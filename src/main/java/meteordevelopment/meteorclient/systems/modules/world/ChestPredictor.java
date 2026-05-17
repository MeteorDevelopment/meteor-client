/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.world;

import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import dev.xpple.cubiomes.Cubiomes;
import dev.xpple.cubiomes.EnchantInstance;
import dev.xpple.cubiomes.ItemStack;
import dev.xpple.cubiomes.LootTableContext;
import meteordevelopment.meteorclient.events.world.ChunkDataEvent;
import meteordevelopment.meteorclient.settings.SeedSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.text.RunnableClickEvent;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.render.PeekScreen;
import meteordevelopment.orbit.EventHandler;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.SectionPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Util;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/*
 * Predict loot of jigsaw structures. It should technically
 * also work for other structures, but for those the
 * coordinates of the chest do not always correspond with
 * the coordinates that are used to calculate the population
 * seed.
 */
public class ChestPredictor extends Module {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final Arena GLOBAL_ARENA = Arena.global();
    private static final ExecutorService sectionScannerService = Executors.newSingleThreadExecutor();

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Long> seed = sgGeneral.add(new SeedSetting.Builder()
        .name("seed")
        .description("Set the world seed to use.")
        .build()
    );

    private static final List<Rotation> ALL_ROTATIONS = List.of(Rotation.values());

    private static final class LootablePiecesHolder {
        // only pieces with one chest are supported due to optimizations
        private static final List<PieceData> LOOTABLE_PIECES = Util.make(() -> {
            Stream.Builder<@Nullable PieceData> builder = Stream.builder();

            // ancient city
            Set<Block> ANCIENT_CITY_FILTERED_BLOCKS = Set.of(
                // blocks affected by ANCIENT_CITY_START_DEGRADATION
                Blocks.DEEPSLATE_BRICKS,
                Blocks.DEEPSLATE_TILES,
                Blocks.SOUL_LANTERN,
                // blocks affected by ANCIENT_CITY_GENERIC_DEGRADATION
                Blocks.DEEPSLATE,
                Blocks.DEEPSLATE_BRICK_SLAB,
                Blocks.DEEPSLATE_TILE_SLAB,
                Blocks.DEEPSLATE_BRICK_STAIRS,
                Blocks.DEEPSLATE_TILE_WALL,
                Blocks.DEEPSLATE_BRICK_WALL,
                Blocks.COBBLED_DEEPSLATE,
                Blocks.CRACKED_DEEPSLATE_BRICKS,
                Blocks.CRACKED_DEEPSLATE_TILES,
                Blocks.GRAY_WOOL
            );

            List.of(
                "ancient_city/structures/chamber_1",
                "ancient_city/structures/chamber_2",
                "ancient_city/structures/chamber_3",
                "ancient_city/structures/tall_ruin_1",
                "ancient_city/structures/tall_ruin_3",
                "ancient_city/structures/tall_ruin_4"
            ).forEach(s -> builder.add(new PieceDataBuilder(Identifier.withDefaultNamespace(s), BuiltInLootTables.ANCIENT_CITY, Level.OVERWORLD)
                .withBiomeFunction(_ -> BuiltinStructures.ANCIENT_CITY)
                .withRotations(ALL_ROTATIONS)
                .withFilteredBlocks(ANCIENT_CITY_FILTERED_BLOCKS)
                .build()));

            builder.add(new PieceDataBuilder(Identifier.withDefaultNamespace("ancient_city/structures/ice_box_1"), BuiltInLootTables.ANCIENT_CITY_ICE_BOX, Level.OVERWORLD)
                .withBiomeFunction(_ -> BuiltinStructures.ANCIENT_CITY)
                .withRotations(ALL_ROTATIONS)
                .withFilteredBlocks(ANCIENT_CITY_FILTERED_BLOCKS)
                .build());

            // bastion
            Set<Block> BASTION_FILTERED_BLOCKS = Set.of(
                // blocks affected by BASTION_GENERIC_DEGRADATION
                Blocks.POLISHED_BLACKSTONE_BRICKS,
                Blocks.BLACKSTONE,
                Blocks.GOLD_BLOCK,
                Blocks.GILDED_BLACKSTONE
            );

            List.of(
                "bastion/bridge/ramparts/rampart_0",
                "bastion/hoglin_stable/ramparts/ramparts_3",
                "bastion/hoglin_stable/walls/side_wall_0",
                "bastion/hoglin_stable/walls/wall_base",
                "bastion/bridge/starting_pieces/entrance",
                "bastion/treasure/walls/bottom/wall_0",
                "bastion/treasure/walls/mid/wall_0",
                "bastion/units/center_pieces/center_0",
                "bastion/units/center_pieces/center_1",
                "bastion/units/center_pieces/center_2",
                "bastion/units/ramparts/ramparts_1",
                "bastion/units/stages/stage_0_2",
                "bastion/units/stages/stage_1_2"
            ).forEach(s -> builder.add(new PieceDataBuilder(Identifier.withDefaultNamespace(s), BuiltInLootTables.BASTION_OTHER, Level.NETHER)
                .withBiomeFunction(_ -> BuiltinStructures.BASTION_REMNANT)
                .withRotations(ALL_ROTATIONS)
                .withFilteredBlocks(BASTION_FILTERED_BLOCKS)
                .build()));

            List.of(
                "bastion/hoglin_stable/large_stables/inner_3",
                "bastion/hoglin_stable/small_stables/inner_2"
            ).forEach(s -> builder.add(new PieceDataBuilder(Identifier.withDefaultNamespace(s), BuiltInLootTables.BASTION_HOGLIN_STABLE, Level.NETHER)
                .withBiomeFunction(_ -> BuiltinStructures.BASTION_REMNANT)
                .withRotations(ALL_ROTATIONS)
                .withFilteredBlocks(BASTION_FILTERED_BLOCKS)
                .build()));

            List.of(
                "bastion/treasure/bases/centers/center_0",
                "bastion/treasure/bases/centers/center_2",
                "bastion/treasure/bases/centers/center_3"
            ).forEach(s -> builder.add(new PieceDataBuilder(Identifier.withDefaultNamespace(s), BuiltInLootTables.BASTION_TREASURE, Level.NETHER)
                .withBiomeFunction(_ -> BuiltinStructures.BASTION_REMNANT)
                .withRotations(ALL_ROTATIONS)
                .withFilteredBlocks(BASTION_FILTERED_BLOCKS)
                .build()));

            return builder.build().filter(Objects::nonNull).toList();
        });
    }

    private static final Map<ResourceKey<Structure>, SaltData> STRUCTURE_SALTS = Util.make(() -> {
        HolderLookup.Provider registry = VanillaRegistries.createLookup();
        HolderLookup.RegistryLookup<Structure> structures = registry.lookupOrThrow(Registries.STRUCTURE);
        return structures.listElements()
            .collect(Collectors.groupingBy(
                s -> s.value().step().ordinal(),
                Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(s -> s.key().identifier().toString()))))).entrySet().stream()
            .<Map.Entry<ResourceKey<Structure>, SaltData>>mapMulti((entry, consumer) -> {
                TreeSet<Holder.Reference<Structure>> forStep = entry.getValue();
                int num = forStep.size();
                for (int i = 0; i < num; i++) {
                    //noinspection DataFlowIssue
                    consumer.accept(Map.entry(forStep.pollFirst().key(), new SaltData(entry.getKey(), i)));
                }
            })
            .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
    });

    private static final Map<Integer, ResourceKey<Enchantment>> CUBIOMES_ENCHANTMENT_ID_TO_MC = ImmutableMap.<Integer, ResourceKey<Enchantment>>builder()
        .put(Cubiomes.PROTECTION(), Enchantments.PROTECTION)
        .put(Cubiomes.FIRE_PROTECTION(), Enchantments.FIRE_PROTECTION)
        .put(Cubiomes.BLAST_PROTECTION(), Enchantments.BLAST_PROTECTION)
        .put(Cubiomes.PROJECTILE_PROTECTION(), Enchantments.PROJECTILE_PROTECTION)
        .put(Cubiomes.RESPIRATION(), Enchantments.RESPIRATION)
        .put(Cubiomes.AQUA_AFFINITY(), Enchantments.AQUA_AFFINITY)
        .put(Cubiomes.THORNS(), Enchantments.THORNS)
        .put(Cubiomes.SWIFT_SNEAK(), Enchantments.SWIFT_SNEAK)
        .put(Cubiomes.FEATHER_FALLING(), Enchantments.FEATHER_FALLING)
        .put(Cubiomes.DEPTH_STRIDER(), Enchantments.DEPTH_STRIDER)
        .put(Cubiomes.FROST_WALKER(), Enchantments.FROST_WALKER)
        .put(Cubiomes.SOUL_SPEED(), Enchantments.SOUL_SPEED)
        .put(Cubiomes.SHARPNESS(), Enchantments.SHARPNESS)
        .put(Cubiomes.SMITE(), Enchantments.SMITE)
        .put(Cubiomes.BANE_OF_ARTHROPODS(), Enchantments.BANE_OF_ARTHROPODS)
        .put(Cubiomes.KNOCKBACK(), Enchantments.KNOCKBACK)
        .put(Cubiomes.FIRE_ASPECT(), Enchantments.FIRE_ASPECT)
        .put(Cubiomes.LOOTING(), Enchantments.LOOTING)
        .put(Cubiomes.SWEEPING_EDGE(), Enchantments.SWEEPING_EDGE)
        .put(Cubiomes.EFFICIENCY(), Enchantments.EFFICIENCY)
        .put(Cubiomes.SILK_TOUCH(), Enchantments.SILK_TOUCH)
        .put(Cubiomes.FORTUNE(), Enchantments.FORTUNE)
        .put(Cubiomes.LUCK_OF_THE_SEA(), Enchantments.LUCK_OF_THE_SEA)
        .put(Cubiomes.LUNGE(), Enchantments.LUNGE)
        .put(Cubiomes.LURE(), Enchantments.LURE)
        .put(Cubiomes.POWER(), Enchantments.POWER)
        .put(Cubiomes.PUNCH(), Enchantments.PUNCH)
        .put(Cubiomes.FLAME(), Enchantments.FLAME)
        .put(Cubiomes.INFINITY_ENCHANTMENT(), Enchantments.INFINITY)
        .put(Cubiomes.QUICK_CHARGE(), Enchantments.QUICK_CHARGE)
        .put(Cubiomes.MULTISHOT(), Enchantments.MULTISHOT)
        .put(Cubiomes.PIERCING(), Enchantments.PIERCING)
        .put(Cubiomes.IMPALING(), Enchantments.IMPALING)
        .put(Cubiomes.RIPTIDE(), Enchantments.RIPTIDE)
        .put(Cubiomes.LOYALTY(), Enchantments.LOYALTY)
        .put(Cubiomes.CHANNELING(), Enchantments.CHANNELING)
        .put(Cubiomes.DENSITY(), Enchantments.DENSITY)
        .put(Cubiomes.BREACH(), Enchantments.BREACH)
        .put(Cubiomes.WIND_BURST(), Enchantments.WIND_BURST)
        .put(Cubiomes.MENDING(), Enchantments.MENDING)
        .put(Cubiomes.UNBREAKING(), Enchantments.UNBREAKING)
        .put(Cubiomes.CURSE_OF_VANISHING(), Enchantments.VANISHING_CURSE)
        .put(Cubiomes.CURSE_OF_BINDING(), Enchantments.BINDING_CURSE)
        .build();

    public ChestPredictor() {
        super(Categories.World, "chest-predictor", "Predicts chest loot based on the seed");
    }

    @Override
    public void onActivate() {
        if (this.seed.get() == null) {
            ChatUtils.sendMsg(Component.literal("Seed not configured!"));
            this.toggle();
        } else {
            super.onActivate();
        }
    }

    @SuppressWarnings("unused")
    @EventHandler
    private void onChunkData(ChunkDataEvent event) {
        if (this.isActive()) {
            sectionScannerService.submit(() -> this.scanChunk(event.chunk().getPos()));
        }
    }

    private void scanChunk(ChunkPos chunkPos) {
        if (!this.isActive()) {
            return;
        }
        Level level = this.mc.level;
        if (level == null) {
            return;
        }
        ChunkAccess chunk = level.getChunk(chunkPos.x(), chunkPos.z(), ChunkStatus.FULL, false);
        if (chunk == null) {
            return;
        }
        LevelChunkSection[] sectionArray = chunk.getSections();
        for (int i = 0, sectionArrayLength = sectionArray.length; i < sectionArrayLength; i++) {
            LevelChunkSection chunkSection = sectionArray[i];
            if (chunkSection.hasOnlyAir()) {
                continue;
            }
            if (!chunkSection.maybeHas(blockState -> blockState.is(Blocks.CHEST))) {
                continue;
            }
            this.scanSection(SectionPos.of(chunkPos, chunk.getSectionYFromSectionIndex(i)));
        }
    }

    private void scanSection(SectionPos sectionPos) {
        BlockPos minPos = sectionPos.origin();
        BlockPos.MutableBlockPos blockPos = new BlockPos.MutableBlockPos();
        for (int x = minPos.getX(), maxX = x + LevelChunkSection.SECTION_WIDTH; x < maxX; x++) {
            blockPos.setX(x);
            for (int z = minPos.getZ(), maxZ = z + LevelChunkSection.SECTION_WIDTH; z < maxZ; z++) {
                blockPos.setZ(z);
                for (int y = minPos.getY(), maxY = y + LevelChunkSection.SECTION_HEIGHT; y < maxY; y++) {
                    blockPos.setY(y);

                    if (this.testBlock(blockPos)) {
                        return;
                    }
                }
            }
        }
    }

    private boolean testBlock(BlockPos pos) {
        Level level = this.mc.level;
        assert level != null;

        ResourceKey<Level> dimension = detectDimension(level.dimensionType());

        for (PieceData pieceData : LootablePiecesHolder.LOOTABLE_PIECES) {

            if (!pieceData.dimension.equals(dimension)) {
                continue;
            }

            for (Map<BlockPos, BlockState> blockMap : pieceData.variants) {
                BlockPos.MutableBlockPos chestPos = new BlockPos.MutableBlockPos();
                if (!matchesLevel(pos, blockMap, chestPos)) {
                    continue;
                }

                Holder<Biome> biome = level.getBiome(chestPos);

                SaltData saltData = pieceData.biomeFunction.apply(biome);

                WorldgenRandom worldgenRandom = new WorldgenRandom(new XoroshiroRandomSource(-1, -1));
                long decorationSeed = worldgenRandom.setDecorationSeed(this.seed.get(), chestPos.getX() & ~15, chestPos.getZ() & ~15);

                worldgenRandom.setFeatureSeed(decorationSeed, saltData.index, saltData.step);
                long lootSeed = worldgenRandom.nextLong();

                SimpleContainer container = this.generateLoot(pieceData.ltc, lootSeed);

                if (container == null) {
                    continue;
                }

                Runnable clickEvent = () -> {
                    var stack = Items.CHEST.getDefaultInstance();
                    stack.set(DataComponents.CUSTOM_NAME, Component.literal(Long.toString(lootSeed)));
                    mc.setScreen(new PeekScreen(stack, container));
                };

                Component showComponent = Component.translatable("module.chest-predictor.showLoot").withStyle(s -> s
                    .withUnderlined(true)
                    .withHoverEvent(new HoverEvent.ShowText(Component.translatable("module.chest-predictor.clickToShow")))
                    .withClickEvent(new RunnableClickEvent(clickEvent)));
                ChatUtils.sendMsg(Component.translatable("module.chest-predictor.predicted", ChatUtils.formatCoords(Vec3.atLowerCornerOf(chestPos)), pieceData.piece, showComponent));

                return true;
            }
        }
        return false;
    }

    private boolean matchesLevel(BlockPos start, Map<BlockPos, BlockState> blocks, BlockPos.MutableBlockPos chestPos) {
        Level level = this.mc.level;
        assert level != null;
        return blocks.entrySet().stream().allMatch(posEntry -> {
            BlockPos blockPos = posEntry.getKey();
            BlockPos worldPos = blockPos.offset(start);
            ChunkAccess chunk = level.getChunk(SectionPos.blockToSectionCoord(worldPos.getX()), SectionPos.blockToSectionCoord(worldPos.getZ()), ChunkStatus.FULL, false);
            if (chunk == null) {
                return false;
            }
            BlockState worldBlockState = chunk.getBlockState(worldPos);
            BlockState expectedBlockState = posEntry.getValue();

            // it is safe to compare block states by identity
            if (worldBlockState == expectedBlockState) {
                if (expectedBlockState.is(Blocks.CHEST)) {
                    chestPos.set(worldPos);
                }
                return true;
            }
            return false;
        });
    }

    private @Nullable SimpleContainer generateLoot(MemorySegment ltc, long lootSeed) {
        Cubiomes.set_loot_seed(ltc, lootSeed);
        Cubiomes.generate_loot(ltc);

        int lootCount = LootTableContext.generated_item_count(ltc);
        SimpleContainer container = new SimpleContainer(3 * 9);
        for (int lootIdx = 0; lootIdx < lootCount; lootIdx++) {
            MemorySegment itemStackInternal = ItemStack.asSlice(LootTableContext.generated_items(ltc), lootIdx);
            String itemName = Cubiomes.get_item_name(ltc, ItemStack.item(itemStackInternal)).getString(0);
            Registry<Item> itemRegistry = this.mc.player.registryAccess().lookupOrThrow(Registries.ITEM);
            Item item = itemRegistry.getValue(Identifier.parse(itemName));
            if (item == null) {
                LOGGER.error("Unknown item with name {}", itemName);
                return null;
            }
            var itemStack = new net.minecraft.world.item.ItemStack(item, ItemStack.count(itemStackInternal));
            MemorySegment enchantments = ItemStack.enchantments(itemStackInternal);
            int enchantmentCount = ItemStack.enchantment_count(itemStackInternal);
            for (int enchantmentIdx = 0; enchantmentIdx < enchantmentCount; enchantmentIdx++) {
                MemorySegment enchantInstance = EnchantInstance.asSlice(enchantments, enchantmentIdx);
                int itemEnchantment = EnchantInstance.enchantment(enchantInstance);
                ResourceKey<Enchantment> enchantmentKey = CUBIOMES_ENCHANTMENT_ID_TO_MC.get(itemEnchantment);
                Registry<Enchantment> enchantmentRegistry = this.mc.player.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
                itemStack.enchant(enchantmentRegistry.getOrThrow(enchantmentKey), EnchantInstance.level(enchantInstance));
            }
            container.addItem(itemStack);
        }

        return container;
    }

    private record PieceData(Identifier piece, ResourceKey<Level> dimension, List<Map<BlockPos, BlockState>> variants, Function<Holder<Biome>, SaltData> biomeFunction, MemorySegment ltc) {
    }

    private record SaltData(int step, int index) {
    }

    private static class PieceDataBuilder {

        private static final List<Rotation> NO_ROTATIONS = List.of(Rotation.NONE);
        private static final List<Mirror> NO_MIRRORS = List.of(Mirror.NONE);

        private static final Set<Block> FILTERED_BLOCKS = Set.of(
            // structure template blocks
            Blocks.JIGSAW,
            Blocks.STRUCTURE_BLOCK,
            Blocks.STRUCTURE_VOID,
            Blocks.AIR,
            // blocks affected by BlockAgeProcessor
            Blocks.STONE_BRICKS,
            Blocks.STONE,
            Blocks.CHISELED_STONE_BRICKS,
            Blocks.OBSIDIAN
        );

        private static final List<TagKey<Block>> FILTERED_TAGS = List.of(
            // blocks affected by BlockAgeProcessor
            BlockTags.STAIRS,
            BlockTags.SLABS,
            BlockTags.WALLS
        );

        private final Identifier piece;
        private final ResourceKey<LootTable> lootTable;
        private final ResourceKey<Level> dimension;

        private List<Rotation> rotations = NO_ROTATIONS;
        private List<Mirror> mirrors = NO_MIRRORS;
        private BlockPos pivot = BlockPos.ZERO;
        private boolean centerPivot = false;
        private Set<Block> filteredBlocks = Collections.emptySet();
        private @Nullable Function<Holder<Biome>, ResourceKey<Structure>> biomeFunction = null;

        private PieceDataBuilder(Identifier piece, ResourceKey<LootTable> lootTable, ResourceKey<Level> dimension) {
            this.piece = piece;
            this.lootTable = lootTable;
            this.dimension = dimension;
        }

        private PieceDataBuilder withRotations(List<Rotation> rotations) {
            this.rotations = rotations;
            return this;
        }

        private PieceDataBuilder withMirrors(List<Mirror> mirrors) {
            this.mirrors = mirrors;
            return this;
        }

        private PieceDataBuilder withPivot(BlockPos pivot) {
            this.pivot = pivot;
            return this;
        }

        private PieceDataBuilder withCenterPivot(boolean centerPivot) {
            this.centerPivot = centerPivot;
            return this;
        }

        private PieceDataBuilder withFilteredBlocks(Set<Block> filteredBlocks) {
            this.filteredBlocks = filteredBlocks;
            return this;
        }

        private PieceDataBuilder withBiomeFunction(Function<Holder<Biome>, ResourceKey<Structure>> biomeFunction) {
            this.biomeFunction = biomeFunction;
            return this;
        }

        private @Nullable PieceData build() {
            Pair<Vec3i, Map<BlockPos, BlockState>> pieceInfo = loadPiece(this.piece, this.filteredBlocks);
            if (pieceInfo == null) {
                return null;
            }

            MemorySegment ltc = loadLootTable(this.lootTable);
            if (ltc == null) {
                return null;
            }

            if (this.centerPivot) {
                Vec3i size = pieceInfo.getFirst();
                this.pivot = new BlockPos(size.getX() / 2, 0, size.getZ() / 2);
            }

            Map<BlockPos, BlockState> blockMap = pieceInfo.getSecond();
            List<BlockPos> chests = blockMap.entrySet().stream().filter(entry -> entry.getValue().is(Blocks.CHEST)).map(Map.Entry::getKey).toList();
            if (chests.size() != 1) {
                LOGGER.error("Structure piece does not have exactly one chest!");
                return null;
            }
            BlockPos chestPos = chests.getFirst();
            var variants = this.rotations.stream()
                .flatMap(rot -> this.mirrors.stream().map(mir -> new Object() {
                    private final Rotation rotation = rot;
                    private final Mirror mirror = mir;
                }))
                .map(variant -> {
                    BlockPos transformedChestPos = StructureTemplate.transform(chestPos, variant.mirror, variant.rotation, this.pivot);
                    return blockMap.entrySet().stream()
                        .collect(Collectors.toUnmodifiableMap(
                            entry -> StructureTemplate.transform(entry.getKey(), variant.mirror, variant.rotation, this.pivot).subtract(transformedChestPos),
                            entry -> entry.getValue().mirror(variant.mirror).rotate(variant.rotation))
                        );
                })
                .toList();

            if (this.biomeFunction == null) {
                LOGGER.error("Biome function must not be undefined for {}", this.piece);
                return null;
            }

            return new PieceData(this.piece, this.dimension, variants, this.biomeFunction.andThen(STRUCTURE_SALTS::get), ltc);
        }

        private static @Nullable Pair<Vec3i, Map<BlockPos, BlockState>> loadPiece(Identifier piece, Set<Block> filteredBlocks) {
            Identifier fileIdentifier = StructureTemplateManager.RESOURCE_STRUCTURE_LISTER.idToFile(piece);
            ModContainer modContainer = FabricLoader.getInstance().getModContainer(fileIdentifier.getNamespace()).orElse(null);
            if (modContainer == null) {
                LOGGER.error("Could not find mod container for {}", fileIdentifier.getNamespace());
                return null;
            }
            String path = "data/%s/%s".formatted(fileIdentifier.getNamespace(), fileIdentifier.getPath());
            try (InputStream is = Files.newInputStream(modContainer.findPath(path).orElseThrow())) {
                CompoundTag compoundTag = NbtIo.readCompressed(is, NbtAccounter.unlimitedHeap());
                int dataVersion = NbtUtils.getDataVersion(compoundTag, 500);
                CompoundTag updatedTag = DataFixTypes.STRUCTURE.updateToCurrentVersion(Minecraft.getInstance().getFixerUpper(), compoundTag, dataVersion);
                Vec3i size = updatedTag.read("size", Vec3i.CODEC).orElseThrow();
                ListTag blocks = updatedTag.getList("blocks").orElseThrow();
                ListTag palette = updatedTag.getList("palette").orElseThrow();
                return Pair.of(size, blocks.compoundStream()
                    .map(compound -> {
                        BlockPos pos = compound.read("pos", BlockPos.CODEC).orElseThrow();
                        int stateIdx = compound.getInt("state").orElseThrow();
                        BlockState state = BlockState.CODEC.parse(NbtOps.INSTANCE, palette.get(stateIdx)).getOrThrow();
                        return Map.entry(pos, state);
                    })
                    .filter(entry -> !FILTERED_BLOCKS.contains(entry.getValue().getBlock()))
                    .filter(entry -> FILTERED_TAGS.stream().noneMatch(tag -> entry.getValue().is(tag)))
                    .filter(entry -> !filteredBlocks.contains(entry.getValue().getBlock()))
                    .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue)));
            } catch (IOException | NoSuchElementException | IllegalStateException e) {
                LOGGER.error("Error while loading template for piece %s".formatted(piece), e);
                return null;
            }
        }

        private static @Nullable MemorySegment loadLootTable(ResourceKey<LootTable> lootTable) {
            Identifier fileIdentifier = FileToIdConverter.registry(Registries.LOOT_TABLE).idToFile(lootTable.identifier());
            ModContainer modContainer = FabricLoader.getInstance().getModContainer(fileIdentifier.getNamespace()).orElse(null);
            if (modContainer == null) {
                LOGGER.error("Could not find mod container for {}", fileIdentifier.getNamespace());
                return null;
            }
            String path = "data/%s/%s".formatted(fileIdentifier.getNamespace(), fileIdentifier.getPath());
            try (InputStream is = Files.newInputStream(modContainer.findPath(path).orElseThrow())) {
                String string = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                // use temporary arena so that the loot table string is deallocated
                try (Arena tempArena = Arena.ofConfined()) {
                    MemorySegment stringInternal = tempArena.allocateFrom(string);
                    MemorySegment ltc = LootTableContext.allocate(GLOBAL_ARENA);
                    if (Cubiomes.init_loot_table(stringInternal.reinterpret(GLOBAL_ARENA, null), ltc, Cubiomes.MC_NEWEST()) != 0) {
                        LOGGER.error("Could not initialize loot table {}", lootTable.identifier());
                        return null;
                    }
                    return ltc;
                }
            } catch (IOException | NoSuchElementException e) {
                LOGGER.error("Error while loading loot table %s".formatted(lootTable.identifier()), e);
                return null;
            }
        }
    }

    private static ResourceKey<Level> detectDimension(DimensionType dimension) {
        return switch (dimension.skybox()) {
            case OVERWORLD -> Level.OVERWORLD;
            case NONE -> Level.NETHER;
            case END -> Level.END;
        };
    }
}

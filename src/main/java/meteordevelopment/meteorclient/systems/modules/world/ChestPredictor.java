/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.world;

import com.google.common.collect.ImmutableMap;
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
import net.minecraft.core.HolderSet;
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
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Util;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.RandomizableContainer;
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
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.pools.ListPoolElement;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.structures.JigsawStructure;
import net.minecraft.world.level.levelgen.structure.templatesystem.AlwaysTrueTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlackstoneReplaceProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockAgeProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockMatchTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockRotProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockStateMatchTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.ProcessorRule;
import net.minecraft.world.level.levelgen.structure.templatesystem.RandomBlockMatchTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.RandomBlockStateMatchTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.levelgen.structure.templatesystem.TagMatchTest;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
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

    private static final List<PieceData> LOOTABLE_PIECES = Util.make(() -> {
        List<Rotation> allRotations = List.of(Rotation.values());

        return TemplateReplacements.getTemplateReplacements()
            .map(e -> new PieceDataBuilder(e.getKey())
                .withRotations(allRotations)
                .withReplacementCheck(e.getValue())
                .build())
            .filter(Objects::nonNull)
            .flatMap(List::stream)
            .filter(Objects::nonNull)
            .toList();
    });

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

        Holder<Biome> biome = level.getBiomeManager().getNoiseBiomeAtPosition(pos);

        for (PieceData pieceData : LOOTABLE_PIECES) {
            Identifier pieceLocation = pieceData.piece.withPath(p -> {
                int endIndex = p.indexOf('/');
                return endIndex != -1 ? p.substring(0, endIndex) : p;
            });
            ResourceKey<Structure> structureKey = findStructureKey(pieceLocation, biome);
            if (structureKey == null) {
                continue;
            }

            for (Map<BlockPos, BlockState> blockMap : pieceData.variants) {
                BlockPos.MutableBlockPos chestPos = new BlockPos.MutableBlockPos();
                if (!matchesLevel(pos, blockMap, pieceData.replacementCheck(), chestPos, pieceData)) {
                    continue;
                }

                SaltData saltData = STRUCTURE_SALTS.get(structureKey);

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

    private boolean matchesLevel(BlockPos start, Map<BlockPos, BlockState> blocks, ReplacementCheck replacementCheck, BlockPos.MutableBlockPos chestPos, PieceData pieceData) {
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
            if (replacementCheck.isValid(expectedBlockState, worldBlockState)) {
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

    private record PieceTemplateInfo(Vec3i size, Map<BlockPos, BlockState> blocks, Stream<Map.Entry<BlockPos, Identifier>> chests) {
    }

    private record PieceData(Identifier piece, List<Map<BlockPos, BlockState>> variants, ReplacementCheck replacementCheck, MemorySegment ltc) {
    }

    private record StructureInfo(Identifier piecesLocation, HolderSet<Biome> biomes) {
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
            Blocks.AIR
        );

        private final Identifier piece;

        private List<Rotation> rotations = NO_ROTATIONS;
        private List<Mirror> mirrors = NO_MIRRORS;
        private BlockPos pivot = BlockPos.ZERO;
        private boolean centerPivot = false;
        private ReplacementCheck replacements = (template, observed) -> template == observed;

        private PieceDataBuilder(Identifier piece) {
            this.piece = piece;
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

        private PieceDataBuilder withReplacementCheck(@Nullable ReplacementCheck replacementCheck) {
            if (replacementCheck != null) {
                this.replacements = replacementCheck;
            }
            return this;
        }

        private @Nullable List<@Nullable PieceData> build() {
            PieceTemplateInfo pieceTemplateInfo = loadPiece(this.piece);
            if (pieceTemplateInfo == null) {
                return null;
            }

            return pieceTemplateInfo.chests
                .map(e -> {
                    MemorySegment ltc = loadLootTable(e.getValue());
                    if (ltc == null) {
                        return null;
                    }

                    if (this.centerPivot) {
                        Vec3i size = pieceTemplateInfo.size;
                        this.pivot = new BlockPos(size.getX() / 2, 0, size.getZ() / 2);
                    }

                    Map<BlockPos, BlockState> blockMap = pieceTemplateInfo.blocks;
                    BlockPos chestPos = e.getKey();
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

                    return new PieceData(this.piece, variants, this.replacements, ltc);
                })
                .filter(Objects::nonNull)
                .toList();
        }

        private static @Nullable PieceTemplateInfo loadPiece(Identifier piece) {
            // https://mojira.dev/MC-249771
            if (piece.equals(Identifier.withDefaultNamespace("ancient_city/walls/intact_horizontal_wall_stairs_5"))) {
                return null;
            }
            Identifier fileIdentifier = StructureTemplateManager.RESOURCE_STRUCTURE_LISTER.idToFile(piece);
            ModContainer modContainer = FabricLoader.getInstance().getModContainer(fileIdentifier.getNamespace()).orElse(null);
            if (modContainer == null) {
                LOGGER.error("Could not find mod container for piece with namespace {}", fileIdentifier.getNamespace());
                return null;
            }
            String path = "data/%s/%s".formatted(fileIdentifier.getNamespace(), fileIdentifier.getPath());
            try (InputStream is = Files.newInputStream(modContainer.findPath(path).orElseThrow())) {
                CompoundTag compoundTag = NbtIo.readCompressed(is, NbtAccounter.unlimitedHeap());
                int dataVersion = NbtUtils.getDataVersion(compoundTag, 500);
                CompoundTag updatedTag = DataFixTypes.STRUCTURE.updateToCurrentVersion(Minecraft.getInstance().getFixerUpper(), compoundTag, dataVersion);
                Vec3i size = updatedTag.read(StructureTemplate.SIZE_TAG, Vec3i.CODEC).orElseThrow();
                ListTag blocksTag = updatedTag.getList(StructureTemplate.BLOCKS_TAG).orElseThrow();
                ListTag palette = updatedTag.getList(StructureTemplate.PALETTE_TAG).orElseThrow();
                ImmutableMap.Builder<BlockPos, BlockState> builder = ImmutableMap.builder();
                Stream.Builder<Map.Entry<BlockPos, Identifier>> chests = Stream.builder();
                for (Tag tag : blocksTag) {
                    if (!(tag instanceof CompoundTag compound)) {
                        continue;
                    }
                    BlockPos pos = compound.read(StructureTemplate.BLOCK_TAG_POS, BlockPos.CODEC).orElseThrow();
                    int stateIdx = compound.getInt(StructureTemplate.BLOCK_TAG_STATE).orElseThrow();
                    BlockState state = BlockState.CODEC.parse(NbtOps.INSTANCE, palette.get(stateIdx)).getOrThrow();
                    if (FILTERED_BLOCKS.contains(state.getBlock())) {
                        continue;
                    }
                    builder.put(pos, state);
                    if (state.is(Blocks.CHEST)) {
                        CompoundTag nbt = compound.getCompound(StructureTemplate.BLOCK_TAG_NBT).orElseThrow();
                        Optional<Identifier> optionalLootTable = nbt.read(RandomizableContainer.LOOT_TABLE_TAG, Identifier.CODEC);
                        optionalLootTable.ifPresent(identifier -> chests.add(Map.entry(pos, identifier)));
                    }
                }
                return new PieceTemplateInfo(size, builder.build(), chests.build());
            } catch (IOException | NoSuchElementException | IllegalStateException e) {
                LOGGER.error("Error while loading template for piece %s".formatted(piece), e);
                return null;
            }
        }

        private static @Nullable MemorySegment loadLootTable(Identifier lootTable) {
            String lootTableContent = getLootTableContent(lootTable);
            // use temporary arena so that the loot table string is deallocated
            try (Arena tempArena = Arena.ofConfined()) {
                MemorySegment lootTableContentInternal = tempArena.allocateFrom(lootTableContent);
                MemorySegment ltc = LootTableContext.allocate(GLOBAL_ARENA);
                if (Cubiomes.init_loot_table(lootTableContentInternal.reinterpret(GLOBAL_ARENA, null), ltc, Cubiomes.MC_NEWEST()) != 0) {
                    LOGGER.error("Could not initialize loot table {}", lootTable);
                    return null;
                }
                int unresolvedSubtableCount = LootTableContext.unresolved_subtable_count(ltc);
                for (int i = 0; i < unresolvedSubtableCount; i++) {
                    MemorySegment unresolvedSubtableName = LootTableContext.unresolved_subtable_names(ltc, i);
                    String subtableContent = getLootTableContent(Identifier.parse(unresolvedSubtableName.getString(0)));
                    MemorySegment subtableContentInternal = tempArena.allocateFrom(subtableContent);
                    if (Cubiomes.resolve_subtable(ltc, unresolvedSubtableName, subtableContentInternal.reinterpret(GLOBAL_ARENA, null)) != 0) {
                        LOGGER.error("Could not initialize loot subtable {}", lootTable);
                        return null;
                    }
                }
                return ltc;
            }
        }

        private static @Nullable String getLootTableContent(Identifier lootTable) {
            Identifier fileIdentifier = FileToIdConverter.registry(Registries.LOOT_TABLE).idToFile(lootTable);
            ModContainer modContainer = FabricLoader.getInstance().getModContainer(fileIdentifier.getNamespace()).orElse(null);
            if (modContainer == null) {
                LOGGER.error("Could not find mod container for loot table with namespace {}", fileIdentifier.getNamespace());
                return null;
            }
            String path = "data/%s/%s".formatted(fileIdentifier.getNamespace(), fileIdentifier.getPath());
            try (InputStream is = Files.newInputStream(modContainer.findPath(path).orElseThrow())) {
                return new String(is.readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException | NoSuchElementException e) {
                LOGGER.error("Error while loading loot table %s".formatted(lootTable), e);
                return null;
            }
        }
    }

    @FunctionalInterface
    private interface ReplacementCheck {
        boolean isValid(BlockState template, BlockState observed);

        default ReplacementCheck and(ReplacementCheck other) {
            Objects.requireNonNull(other);
            return (t, u) -> isValid(t, u) && other.isValid(t, u);
        }

        default ReplacementCheck negate() {
            return (t, u) -> !isValid(t, u);
        }

        default ReplacementCheck or(ReplacementCheck other) {
            Objects.requireNonNull(other);
            return (t, u) -> isValid(t, u) || other.isValid(t, u);
        }
    }

    // data stuff

    private static final class TemplateReplacements {

        // maybeReplaceFullStoneBlock
        private static final Set<Block> FULL_STONE_REPLACEMENTS = Set.of(Blocks.CRACKED_STONE_BRICKS, Blocks.STONE_BRICK_STAIRS, Blocks.MOSSY_STONE_BRICKS, Blocks.MOSSY_STONE_BRICK_STAIRS);

        // maybeReplaceStairs
        private static final Set<Block> STAIR_REPLACEMENTS = Stream.concat(Arrays.stream(BlockAgeProcessor.NON_MOSSY_REPLACEMENTS).map(BlockBehaviour.BlockStateBase::getBlock), Stream.of(Blocks.MOSSY_STONE_BRICK_STAIRS, Blocks.MOSSY_STONE_BRICK_SLAB)).collect(Collectors.toUnmodifiableSet());

        private static Stream<Map.Entry<Identifier, ReplacementCheck>> getTemplateReplacements() {
            HolderLookup.Provider registry = VanillaRegistries.createLookup();
            HolderLookup.RegistryLookup<StructureTemplatePool> templatePools = registry.lookupOrThrow(Registries.TEMPLATE_POOL);
            return templatePools.listElements()
                .<SinglePoolElement>mapMulti((pool, consumer) -> {
                    pool.value().getTemplates().forEach(elementPair -> {
                        StructurePoolElement element = elementPair.getFirst();
                        recursePoolElement(element, consumer);
                    });
                })
                .map(element -> {
                    ReplacementCheck replacementCheck = (template, observed) -> template.is(observed.getBlock());
                    for (StructureProcessor processor : element.processors.value().list()) {
                        switch (processor) {
                            case BlackstoneReplaceProcessor blackstoneReplaceProcessor -> {
                                replacementCheck = replacementCheck.or((template, observed) -> {
                                    return blackstoneReplaceProcessor.replacements.get(template.getBlock()) == observed.getBlock();
                                });
                            }
                            case BlockAgeProcessor _ -> {
                                replacementCheck = replacementCheck.or((template, observed) -> {
                                    if (template.is(Blocks.STONE_BRICKS) || template.is(Blocks.STONE) || template.is(Blocks.CHISELED_STONE_BRICKS)) {
                                        return FULL_STONE_REPLACEMENTS.contains(observed.getBlock());
                                    }
                                    if (template.is(BlockTags.STAIRS)) {
                                        return STAIR_REPLACEMENTS.contains(observed.getBlock());
                                    }
                                    if (template.is(BlockTags.SLABS)) {
                                        return observed.is(Blocks.MOSSY_STONE_BRICK_SLAB);
                                    }
                                    if (template.is(BlockTags.WALLS)) {
                                        return observed.is(Blocks.MOSSY_STONE_BRICK_WALL);
                                    }
                                    if (template.is(Blocks.OBSIDIAN)) {
                                        return observed.is(Blocks.CRYING_OBSIDIAN);
                                    }
                                    return false;
                                });
                            }
                            case BlockRotProcessor blockRotProcessor -> {
                                if (blockRotProcessor.rottableBlocks.isPresent()) {
                                    replacementCheck = replacementCheck.or((template, _) -> {
                                        // BlockRotProcessor does not replace block with air but keeps
                                        // the block that was there before the structure generated
                                        return template.is(blockRotProcessor.rottableBlocks.get());
                                    });
                                }
                            }
                            case RuleProcessor ruleProcessor -> {
                                for (ProcessorRule rule : ruleProcessor.rules) {
                                    ReplacementCheck ruleCheck = switch (rule.inputPredicate) {
                                        case BlockMatchTest blockMatchTest -> (template, observed) -> {
                                            if (template.is(blockMatchTest.block)) {
                                                return observed == rule.getOutputState();
                                            }
                                            return false;
                                        };
                                        case BlockStateMatchTest blockStateMatchTest -> (template, observed) -> {
                                            if (template == blockStateMatchTest.blockState) {
                                                return observed == rule.getOutputState();
                                            }
                                            return false;
                                        };
                                        case TagMatchTest tagMatchTest -> (template, observed) -> {
                                            if (template.is(tagMatchTest.tag)) {
                                                return observed == rule.getOutputState();
                                            }
                                            return false;
                                        };
                                        case RandomBlockMatchTest randomBlockMatchTest -> (template, observed) -> {
                                            if (template.is(randomBlockMatchTest.block)) {
                                                return observed == rule.getOutputState();
                                            }
                                            return false;
                                        };
                                        case RandomBlockStateMatchTest randomBlockStateMatchTest -> (template, observed) -> {
                                            if (template == randomBlockStateMatchTest.blockState) {
                                                return observed == rule.getOutputState();
                                            }
                                            return false;
                                        };
                                        case AlwaysTrueTest _ -> (_, observed) -> observed == rule.getOutputState();
                                        default -> throw new AssertionError("RuleTest " + rule.inputPredicate + " not implemented");
                                    };
                                    replacementCheck = replacementCheck.or(ruleCheck);
                                }
                            }
                            default -> {}
                        }
                    }
                    return Map.entry(element.getTemplateLocation(), replacementCheck);
                });
        }

        private static void recursePoolElement(StructurePoolElement element, Consumer<SinglePoolElement> consumer) {
            if (element instanceof SinglePoolElement singlePoolElement) {
                consumer.accept(singlePoolElement);
            } else if (element instanceof ListPoolElement listPoolElement) {
                listPoolElement.getElements().forEach(e -> recursePoolElement(e, consumer));
            }
        }
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

    private static final Map<ResourceKey<Structure>, StructureInfo> JIGSAW_STRUCTURE_INFO = Util.make(() -> {
        HolderLookup.Provider registry = VanillaRegistries.createLookup();
        HolderLookup.RegistryLookup<Structure> structures = registry.lookupOrThrow(Registries.STRUCTURE);
        return structures.listElements()
            .<Map.Entry<ResourceKey<Structure>, StructureInfo>>mapMulti((s, consumer) -> {
                if (!(s.value() instanceof JigsawStructure jigsawStructure)) {
                    return;
                }
                jigsawStructure.getStartPool().unwrapKey().ifPresent(key -> {
                    Identifier location = key.identifier().withPath(p -> {
                        int endIndex = p.indexOf('/');
                        return endIndex != -1 ? p.substring(0, endIndex) : p;
                    });
                    consumer.accept(Map.entry(s.key(), new StructureInfo(location, jigsawStructure.biomes())));
                });
            })
            .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
    });

    private static @Nullable ResourceKey<Structure> findStructureKey(Identifier piecesLocation, Holder<Biome> biome) {
        return JIGSAW_STRUCTURE_INFO.entrySet().stream()
            .filter(e -> e.getValue().piecesLocation.equals(piecesLocation))
            .filter(e -> e.getValue().biomes.contains(biome))
            .map(Map.Entry::getKey)
            .findAny().orElse(null);
    }

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
}

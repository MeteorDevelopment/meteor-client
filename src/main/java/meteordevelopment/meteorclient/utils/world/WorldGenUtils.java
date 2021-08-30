/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.utils.world;

import baritone.api.BaritoneAPI;
import kaptainwutax.biomeutils.source.BiomeSource;
import kaptainwutax.featureutils.misc.SlimeChunk;
import kaptainwutax.featureutils.structure.*;
import kaptainwutax.mcutils.rand.ChunkRand;
import kaptainwutax.mcutils.state.Dimension;
import kaptainwutax.mcutils.util.data.SpiralIterator;
import kaptainwutax.mcutils.util.pos.*;
import kaptainwutax.mcutils.version.MCVersion;
import kaptainwutax.terrainutils.TerrainGenerator;
import meteordevelopment.meteorclient.systems.seeds.Seed;
import meteordevelopment.meteorclient.systems.seeds.Seeds;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.*;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.vehicle.ChestMinecartEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.*;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;

import java.util.*;
import java.util.stream.StreamSupport;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static meteordevelopment.meteorclient.utils.Utils.mc;

public class WorldGenUtils {

    private static final Logger LOG = LogManager.getLogger();

    private static final HashMap<Feature, List<Block>> FEATURE_BLOCKS = new HashMap<>(){{
        put(Feature.nether_fortress, Arrays.asList(
            Blocks.NETHER_BRICKS,
            Blocks.NETHER_BRICK_FENCE,
            Blocks.NETHER_WART
        ));
        put(Feature.ocean_monument, Arrays.asList(
            Blocks.PRISMARINE_BRICKS,
            Blocks.SEA_LANTERN,
            Blocks.DARK_PRISMARINE
        ));
        put(Feature.stronghold, Arrays.asList(
            Blocks.END_PORTAL_FRAME,
            Blocks.END_PORTAL
        ));
        put(Feature.end_city, Arrays.asList(
           Blocks.PURPUR_BLOCK,
           Blocks.PURPUR_PILLAR,
           Blocks.PURPUR_STAIRS,
           Blocks.END_ROD
        ));
        put(Feature.village, Arrays.asList(
            Blocks.BELL,
            Blocks.BREWING_STAND,
            Blocks.SMOKER,
            Blocks.BLAST_FURNACE,
            Blocks.FLETCHING_TABLE,
            Blocks.STONECUTTER,
            Blocks.LOOM,
            Blocks.GRINDSTONE,
            Blocks.LECTERN
        ));
        put(Feature.mineshaft, Collections.singletonList(
            Blocks.RAIL
        ));
        put(Feature.desert_pyramid, Arrays.asList(
            Blocks.TNT,
            Blocks.CHISELED_SANDSTONE,
            Blocks.STONE_PRESSURE_PLATE
        ));
    }};

    private static final HashMap<Feature, List<Class<? extends Entity>>> FEATURE_ENTITIES = new HashMap<>(){{
       put(Feature.ocean_monument, Arrays.asList(
           ElderGuardianEntity.class,
           GuardianEntity.class
       ));
       put(Feature.nether_fortress, Arrays.asList(
          BlazeEntity.class,
          WitherSkeletonEntity.class
       ));
       put(Feature.mansion, Collections.singletonList(
           EvokerEntity.class
       ));
       put(Feature.slime_chunk, Collections.singletonList(
           SlimeEntity.class
       ));
       put(Feature.bastion_remnant, Collections.singletonList(
          PiglinBruteEntity.class
       ));
       put(Feature.end_city, Collections.singletonList(
           ShulkerEntity.class
       ));
       put(Feature.village, Arrays.asList(
           VillagerEntity.class,
           IronGolemEntity.class
       ));
       put(Feature.mineshaft, Collections.singletonList(
           ChestMinecartEntity.class
       ));
    }};

    public enum Feature {
        buried_treasure,
        mansion,
        stronghold,
        nether_fortress,
        ocean_monument,
        bastion_remnant,
        end_city,
        village,
        mineshaft,
        slime_chunk,
        desert_pyramid
    }

    public static BlockPos locateFeature(Feature feature, BlockPos center) {
        Seed seed = Seeds.get().getSeed();
        BlockPos pos = null;
        if (seed != null) {
            try {
                pos = locateFeature(seed, feature, center);
            } catch (Exception | Error ex) {
                LOG.error(ex);
            }
            if (pos != null) return pos;
        }
        if (mc.player != null) {
            ItemStack stack = mc.player.getStackInHand(Hand.MAIN_HAND);
            if (stack.getItem() != Items.FILLED_MAP)
                stack = mc.player.getStackInHand(Hand.OFF_HAND);
            if (stack.getItem() == Items.FILLED_MAP) {
                try {
                    pos = locateFeatureMap(feature, stack);
                } catch (Exception | Error ex) {
                    LOG.error(ex);
                }
                if (pos != null) return pos;
            }
        }
        try {
            pos = locateFeatureEntities(feature);
        } catch (Exception | Error ex) {
            LOG.error(ex);
        }
        if (pos != null) return pos;
        try {
            pos = locateFeatureBlocks(feature);
        } catch (Exception | Error ex) {
            LOG.error(ex);
        }
        return pos;
    }

    private static BlockPos locateFeatureMap(Feature feature, ItemStack stack) {
        if (!isValidMap(feature, stack)) return null;
        return getMapMarker(stack);
    }

    private static BlockPos locateFeatureBlocks(Feature feature) {
        List<Block> blocks = FEATURE_BLOCKS.get(feature);
        if (blocks == null) return null;
        List<BlockPos> posList = BaritoneAPI
            .getProvider()
            .getWorldScanner()
            .scanChunkRadius(
                BaritoneAPI
                    .getProvider()
                    .getPrimaryBaritone()
                    .getPlayerContext(),
            blocks,64,10,32);
        if (posList.isEmpty()) return null;
        if (posList.size() < 5) {
            ChatUtils.warning("Locate", "Only %d block(s) found. This search might be a false positive.", posList.size());
        }
        return posList.get(0);
    }

    private static BlockPos locateFeatureEntities(Feature feature) {
        List<Class<? extends Entity>> entities = FEATURE_ENTITIES.get(feature);
        if (entities == null) return null;
        if (mc.world == null) return null;
        for (Entity e: mc.world.getEntities()) {
            for (Class<? extends Entity> clazz: entities) {
                if (clazz.isInstance(e))
                    return e.getBlockPos();
            }
        }
        return null;
    }

    private static BlockPos locateFeature(Seed seed, Feature feature, BlockPos center) {
        if (feature == Feature.slime_chunk) return locateSlimeChunk(seed, center);
        return locateStructure(seed, feature, center);
    }

    private static BlockPos locateSlimeChunk(Seed seed, BlockPos center) {
        Dimension dimension = getDimension(Feature.slime_chunk);
        MCVersion mcVersion = seed.version;
        CPos centerChunk = new CPos(center.getX() >> 4, center.getZ() >> 4);
        CPos slimeChunkPos = locateSlimeChunk(new SlimeChunk(mcVersion), centerChunk, 6400, seed.seed, new ChunkRand(), dimension);
        if (slimeChunkPos == null) return null;
        return toBlockPos(slimeChunkPos.toBlockPos());
    }

    private static CPos locateSlimeChunk(SlimeChunk slimeChunk, CPos centerChunk, int radius, long seed, ChunkRand rand, Dimension dimension) {
        if (!slimeChunk.isValidDimension(dimension))
            return null;
        SpiralIterator<CPos> spiralIterator = new SpiralIterator<>(centerChunk, new CPos(radius, radius), (x, y, z) -> new CPos(x, z));
        for (CPos next : spiralIterator) {
            SlimeChunk.Data data = slimeChunk.at(next.getX(), next.getZ(), true);
            if (data.testStart(seed, rand)) {
                return next;
            }
        }
        return null;
    }

    private static BlockPos locateStructure(Seed seed, Feature feature, BlockPos center) {
        Dimension dimension = getDimension(feature);
        MCVersion mcVersion = seed.version;
        Structure<?, ?> structure = getStructure(feature, mcVersion);
        if (structure == null) return null;
        BiomeSource biomeSource = BiomeSource.of(dimension, mcVersion, seed.seed);
        if (!structure.isValidDimension(biomeSource.getDimension()))
            return null;
        BPos structurePos = locateStructure(structure, new BPos(center.getX(), center.getY(), center.getZ()), 6400, new ChunkRand(), biomeSource, TerrainGenerator.of(biomeSource));
        if (structurePos == null) return null;
        return toBlockPos(structurePos);
    }

    private static BPos locateStructure(Structure<?, ?> structure, BPos center, int radius, ChunkRand chunkRand, BiomeSource source, TerrainGenerator terrainGenerator) {
        if (structure instanceof RegionStructure<?, ?> regionStructure) {
            int chunkInRegion = regionStructure.getSpacing();
            int regionSize = chunkInRegion * 16;

            final int border = 30_000_000;
            SpiralIterator<RPos> spiralIterator = new SpiralIterator<>(center.toRegionPos(regionSize), new BPos(-border, 0, -border).toRegionPos(regionSize), new BPos(border, 0, border).toRegionPos(regionSize), 1, (x, y, z) -> new RPos(x, z, regionSize));
            return StreamSupport.stream(spiralIterator.spliterator(), false)
                .map(rPos -> regionStructure.getInRegion(source.getWorldSeed(), rPos.getX(), rPos.getZ(), chunkRand))
                .filter(Objects::nonNull)
                .filter(cPos -> (regionStructure.canSpawn(cPos, source)) && (terrainGenerator == null || regionStructure.canGenerate(cPos, terrainGenerator)))
                .findAny().map(cPos -> cPos.toBlockPos().add(9, 0, 9)).orElse(null);
        } else {
            if (structure instanceof Stronghold strongholdStructure) {
                CPos currentChunkPos = center.toChunkPos();
                int squaredDistance = Integer.MAX_VALUE;
                CPos closest = new CPos(0, 0);
                for (CPos stronghold : strongholdStructure.getAllStarts(source, chunkRand)) {
                    int newSquaredDistance = (currentChunkPos.getX() - stronghold.getX()) * (currentChunkPos.getX() - stronghold.getX()) + (currentChunkPos.getZ() - stronghold.getZ()) * (currentChunkPos.getZ() - stronghold.getZ());
                    if (newSquaredDistance < squaredDistance) {
                        squaredDistance = newSquaredDistance;
                        closest = stronghold;
                    }
                }
                BPos dimPos = closest.toBlockPos().add(9, 0, 9);
                return new BPos(dimPos.getX(), 0, dimPos.getZ());
            } else if (structure instanceof Mineshaft mineshaft) {
                SpiralIterator<CPos> spiralIterator = new SpiralIterator<>(new CPos(center.getX() >> 4, center.getZ() >> 4), new CPos(radius, radius), (x, y, z) -> new CPos(x, z));

                return StreamSupport.stream(spiralIterator.spliterator(), false)
                    .filter(cPos -> {
                        kaptainwutax.featureutils.Feature.Data<Mineshaft> data = mineshaft.at(cPos.getX(), cPos.getZ());
                        return data.testStart(source.getWorldSeed(), chunkRand) && data.testBiome(source) && data.testGenerate(terrainGenerator);
                    })
                    .findAny().map(cPos -> cPos.toBlockPos().add(9, 0, 9)).orElse(null);
            }
        }
        return null;
    }

    private static Dimension getDimension(Feature feature) {
        switch (feature) {
            case buried_treasure -> { return Dimension.OVERWORLD; }
            case mansion -> { return Dimension.OVERWORLD; }
            case stronghold -> { return Dimension.OVERWORLD; }
            case nether_fortress -> { return Dimension.NETHER; }
            case ocean_monument -> { return Dimension.OVERWORLD; }
            case bastion_remnant -> { return Dimension.NETHER; }
            case slime_chunk -> { return Dimension.OVERWORLD; }
            case village -> { return Dimension.OVERWORLD; }
            case mineshaft -> { return Dimension.OVERWORLD; }
            case end_city -> { return Dimension.END; }
            case desert_pyramid -> { return Dimension.OVERWORLD; }
            default -> { return Dimension.OVERWORLD; }
        }
    }

    private static Structure<?, ?> getStructure(Feature feature, MCVersion version) {
        switch (feature) {
            case buried_treasure -> { return new BuriedTreasure(version); }
            case mansion -> { return new Mansion(version); }
            case stronghold -> { return new Stronghold(version); }
            case nether_fortress -> { return new Fortress(version); }
            case ocean_monument -> { return new Monument(version); }
            case bastion_remnant -> { return new BastionRemnant(version); }
            case end_city -> { return new EndCity(version); }
            case village -> { return new Village(version); }
            case mineshaft -> { return new Mineshaft(version); }
            case desert_pyramid -> { return new DesertPyramid(version); }
            default -> { return null;}
        }
    }

    private static BlockPos toBlockPos(BPos pos) {
        return new BlockPos(pos.getX(), pos.getY(), pos.getZ());
    }

    private static boolean isValidMap(Feature feature, ItemStack stack) {
        if (!stack.hasNbt()) return false;
        if (!stack.getNbt().contains("display")) return false;
        NbtCompound displayTag = stack.getNbt().getCompound("display");
        if (!displayTag.contains("Name")) return false;
        String nameTag = displayTag.getString("Name");
        if (!nameTag.contains("translate")) return false;

        if (feature == Feature.buried_treasure) {
            return nameTag.contains("filled_map.buried_treasure");
        } else if (feature == Feature.ocean_monument) {
            return nameTag.contains("filled_map.monument");
        } else if (feature == Feature.mansion) {
            return nameTag.contains("filled_map.mansion");
        }
        return false;
    }

    private static BlockPos getMapMarker(ItemStack stack) {
        if (!stack.hasNbt()) return null;
        if (!stack.getNbt().contains("Decorations")) return null;
        NbtList decorationsTag = stack.getNbt().getList("Decorations", NbtElement.COMPOUND_TYPE);
        if (decorationsTag.size() < 1) return null;
        NbtCompound iconTag = decorationsTag.getCompound(0);
        return new BlockPos(
            (int)iconTag.getDouble("x"),
            (int)iconTag.getDouble("y"),
            (int)iconTag.getDouble("z")
        );
    }
}

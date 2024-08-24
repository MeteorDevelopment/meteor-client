/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.pathing;

import baritone.api.BaritoneAPI;
import baritone.api.utils.SettingsUtil;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.block.Block;
import net.minecraft.item.Item;

import java.awt.*;
import java.lang.reflect.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BaritoneSettings implements IPathManager.ISettings {
    private final Settings settings = new Settings();

    private Setting<Boolean> walkOnWater, walkOnLava;
    private Setting<Boolean> step, noFall;

    public BaritoneSettings() {
        createWrappers();
    }

    @Override
    public Settings get() {
        return settings;
    }

    @Override
    public Setting<Boolean> getWalkOnWater() {
        return walkOnWater;
    }

    @Override
    public Setting<Boolean> getWalkOnLava() {
        return walkOnLava;
    }

    @Override
    public Setting<Boolean> getStep() {
        return step;
    }

    @Override
    public Setting<Boolean> getNoFall() {
        return noFall;
    }

    @Override
    public void save() {
        SettingsUtil.save(BaritoneAPI.getSettings());
    }

    // Wrappers

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void createWrappers() {
        SettingGroup sgBool = settings.createGroup("Checkboxes");
        SettingGroup sgDouble = settings.createGroup("Numbers");
        SettingGroup sgInt = settings.createGroup("Whole Numbers");
        SettingGroup sgString = settings.createGroup("Strings");
        SettingGroup sgColor = settings.createGroup("Colors");

        SettingGroup sgBlockLists = settings.createGroup("Block Lists");
        SettingGroup sgItemLists = settings.createGroup("Item Lists");

        try {
            Class<? extends baritone.api.Settings> klass = BaritoneAPI.getSettings().getClass();

            for (Field field : klass.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) continue;

                Object obj = field.get(BaritoneAPI.getSettings());
                if (!(obj instanceof baritone.api.Settings.Setting setting)) continue;

                Object value = setting.value;

                if (value instanceof Boolean) {
                    Setting<Boolean> wrapper = sgBool.add(new BoolSetting.Builder()
                        .name(setting.getName())
                        .description(getDescription(setting.getName()))
                        .defaultValue((boolean) setting.defaultValue)
                        .onChanged(aBoolean -> setting.value = aBoolean)
                        .onModuleActivated(booleanSetting -> booleanSetting.set((Boolean) setting.value))
                        .build()
                    );

                    switch (wrapper.name) {
                        case "assumeWalkOnWater" -> walkOnWater = wrapper;
                        case "assumeWalkOnLava" -> walkOnLava = wrapper;
                        case "assumeStep" -> step = wrapper;
                    }
                }
                else if (value instanceof Double) {
                    sgDouble.add(new DoubleSetting.Builder()
                        .name(setting.getName())
                        .description(getDescription(setting.getName()))
                        .defaultValue((double) setting.defaultValue)
                        .onChanged(aDouble -> setting.value = aDouble)
                        .onModuleActivated(doubleSetting -> doubleSetting.set((Double) setting.value))
                        .build()
                    );
                }
                else if (value instanceof Float) {
                    sgDouble.add(new DoubleSetting.Builder()
                        .name(setting.getName())
                        .description(getDescription(setting.getName()))
                        .defaultValue(((Float) setting.defaultValue).doubleValue())
                        .onChanged(aDouble -> setting.value = aDouble.floatValue())
                        .onModuleActivated(doubleSetting -> doubleSetting.set(((Float) setting.value).doubleValue()))
                        .build()
                    );
                }
                else if (value instanceof Integer) {
                    Setting<Integer> wrapper = sgInt.add(new IntSetting.Builder()
                        .name(setting.getName())
                        .description(getDescription(setting.getName()))
                        .defaultValue((int) setting.defaultValue)
                        .onChanged(integer -> setting.value = integer)
                        .onModuleActivated(integerSetting -> integerSetting.set((Integer) setting.value))
                        .build()
                    );

                    if (wrapper.name.equals("maxFallHeightNoWater")) {
                        noFall = new BoolSetting.Builder()
                            .name(wrapper.name)
                            .description(wrapper.description)
                            .defaultValue(false)
                            .onChanged(aBoolean -> wrapper.set(aBoolean ? 159159 : wrapper.getDefaultValue()))
                            .onModuleActivated(booleanSetting -> booleanSetting.set(wrapper.get() >= 255))
                            .build();
                    }
                }
                else if (value instanceof Long) {
                    sgInt.add(new IntSetting.Builder()
                        .name(setting.getName())
                        .description(getDescription(setting.getName()))
                        .defaultValue(((Long) setting.defaultValue).intValue())
                        .onChanged(integer -> setting.value = integer.longValue())
                        .onModuleActivated(integerSetting -> integerSetting.set(((Long) setting.value).intValue()))
                        .build()
                    );
                }
                else if (value instanceof String) {
                    sgString.add(new StringSetting.Builder()
                        .name(setting.getName())
                        .description(getDescription(setting.getName()))
                        .defaultValue((String) setting.defaultValue)
                        .onChanged(string -> setting.value = string)
                        .onModuleActivated(stringSetting -> stringSetting.set((String) setting.value))
                        .build()
                    );
                }
                else if (value instanceof Color) {
                    Color c = (Color) setting.value;

                    sgColor.add(new ColorSetting.Builder()
                        .name(setting.getName())
                        .description(getDescription(setting.getName()))
                        .defaultValue(new SettingColor(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha()))
                        .onChanged(color -> setting.value = new Color(color.r, color.g, color.b, color.a))
                        .onModuleActivated(colorSetting -> colorSetting.set(new SettingColor(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha())))
                        .build()
                    );
                }
                else if (value instanceof List) {
                    Type listType = ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
                    Type type = ((ParameterizedType) listType).getActualTypeArguments()[0];

                    if (type == Block.class) {
                        sgBlockLists.add(new BlockListSetting.Builder()
                            .name(setting.getName())
                            .description(getDescription(setting.getName()))
                            .defaultValue((List<Block>) setting.defaultValue)
                            .onChanged(blockList -> setting.value = blockList)
                            .onModuleActivated(blockListSetting -> blockListSetting.set((List<Block>) setting.value))
                            .build()
                        );
                    }
                    else if (type == Item.class) {
                        sgItemLists.add(new ItemListSetting.Builder()
                            .name(setting.getName())
                            .description(getDescription(setting.getName()))
                            .defaultValue((List<Item>) setting.defaultValue)
                            .onChanged(itemList -> setting.value = itemList)
                            .onModuleActivated(itemListSetting -> itemListSetting.set((List<Item>) setting.value))
                            .build()
                        );
                    }
                }
            }
        }
        catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    // Descriptions

    private static Map<String, String> descriptions;

    private static void addDescription(String settingName, String description) {
        descriptions.put(settingName.toLowerCase(), description);
    }

    private static String getDescription(String settingName) {
        if (descriptions == null) loadDescriptions();

        return descriptions.get(settingName.toLowerCase());
    }

    private static void loadDescriptions() {
        descriptions = new HashMap<>();
        addDescription("acceptableThrowawayItems", "Blocks that Baritone is allowed to place (as throwaway, for sneak bridging, pillaring, etc.)");
        addDescription("allowBreak", "Allow Baritone to break blocks");
        addDescription("allowBreakAnyway", "Blocks that baritone will be allowed to break even with allowBreak set to false");
        addDescription("allowDiagonalAscend","Allow diagonal ascending");
        addDescription("allowDiagonalDescend", "Allow descending diagonally");
        addDescription("allowDownward", "Allow mining the block directly beneath its feet");
        addDescription("allowInventory", "Allow Baritone to move items in your inventory to your hotbar");
        addDescription("allowJumpAt256", "If true, parkour is allowed to make jumps when standing on blocks at the maximum height, so player feet is y=256");
        addDescription("allowOnlyExposedOres", "This will only allow baritone to mine exposed ores, can be used to stop ore obfuscators on servers that use them.");
        addDescription("allowOnlyExposedOresDistance", "When allowOnlyExposedOres is enabled this is the distance around to search.");
        addDescription("allowOvershootDiagonalDescend","Is it okay to sprint through a descend followed by a diagonal? The player overshoots the landing, but not enough to fall off.");
        addDescription("allowParkour", "You know what it is");
        addDescription("allowParkourAscend", "This should be monetized it's so good");
        addDescription("allowParkourPlace", "Actually pretty reliable.");
        addDescription("allowPlace", "Allow Baritone to place blocks");
        addDescription("allowSprint", "Allow Baritone to sprint");
        addDescription("allowVines", "Enables some more advanced vine features.");
        addDescription("allowWalkOnBottomSlab", "Slab behavior is complicated, disable this for higher path reliability.");
        addDescription("allowWaterBucketFall", "Allow Baritone to fall arbitrary distances and place a water bucket beneath it.");
        addDescription("antiCheatCompatibility", "Will cause some minor behavioral differences to ensure that Baritone works on anticheats.");
        addDescription("assumeExternalAutoTool", "Disable baritone's auto-tool at runtime, but still assume that another mod will provide auto tool functionality");
        addDescription("assumeSafeWalk", "Assume safe walk functionality; don't sneak on a backplace traverse.");
        addDescription("assumeStep", "Assume step functionality; don't jump on an Ascend.");
        addDescription("assumeWalkOnLava", "If you have Fire Resistance and Jesus then I guess you could turn this on lol");
        addDescription("assumeWalkOnWater", "Allow Baritone to assume it can walk on still water just like any other block.");
        addDescription("autoTool", "Automatically select the best available tool");
        addDescription("avoidance", "Toggle the following 4 settings");
        addDescription("avoidBreakingMultiplier", "this multiplies the break speed, if set above 1 it's \"encourage breaking\" instead");
        addDescription("avoidUpdatingFallingBlocks", "If this setting is true, Baritone will never break a block that is adjacent to an unsupported falling block.");
        addDescription("axisHeight", "The \"axis\" command (aka GoalAxis) will go to a axis, or diagonal axis, at this Y level.");
        addDescription("backfill", "Fill in blocks behind you (stealth +100)");
        addDescription("backtrackCostFavoringCoefficient", "Set to 1.0 to effectively disable this feature");
        addDescription("blacklistClosestOnFailure", "When GetToBlockProcess or MineProcess fails to calculate a path, instead of just giving up, mark the closest instance of that block as \"unreachable\" and go towards the next closest.");
        addDescription("blockBreakAdditionalPenalty", "This is just a tiebreaker to make it less likely to break blocks if it can avoid it.");
        addDescription("blockPlacementPenalty", "It doesn't actually take twenty ticks to place a block, this cost is so high because we want to generally conserve blocks which might be limited.");
        addDescription("blockReachDistance", "Block reach distance");
        addDescription("blocksToAvoid", "Blocks that Baritone will attempt to avoid (Used in avoidance)");
        addDescription("blocksToAvoidBreaking", "blocks that baritone shouldn't break, but can if it needs to.");
        addDescription("blocksToDisallowBreaking", "Blocks that Baritone is not allowed to break");
        addDescription("breakCorrectBlockPenaltyMultiplier", "Multiply the cost of breaking a block that's correct in the builder's schematic by this coefficient");
        addDescription("breakFromAbove", "Allow standing above a block while mining it, in BuilderProcess");
        addDescription("builderTickScanRadius", "Distance to scan every tick for updates.");
        addDescription("buildIgnoreBlocks", "A list of blocks to be treated as if they're air.");
        addDescription("buildIgnoreDirection", "If this is true, the builder will ignore directionality of certain blocks like glazed terracotta.");
        addDescription("buildIgnoreExisting", "If this is true, the builder will treat all non-air blocks as correct.");
        addDescription("buildInLayers", "Don't consider the next layer in builder until the current one is done");
        addDescription("buildOnlySelection", "Only build the selected part of schematics");
        addDescription("buildRepeat", "How far to move before repeating the build.");
        addDescription("buildRepeatCount", "How many times to buildrepeat.");
        addDescription("buildRepeatSneaky", "Don't notify schematics that they are moved.");
        addDescription("buildSkipBlocks", "A list of blocks to be treated as correct.");
        addDescription("buildSubstitutes", "A mapping of blocks to blocks to be built instead");
        addDescription("buildValidSubstitutes", "A mapping of blocks to blocks treated as correct in their position.");
        addDescription("cachedChunksExpirySeconds", "Cached chunks (regardless of if they're in RAM or saved to disk) expire and are deleted after this number of seconds -1 to disable");
        addDescription("cachedChunksOpacity", "0.0f = not visible, fully transparent (instead of setting this to 0, turn off renderCachedChunks) 1.0f = fully opaque");
        addDescription("cancelOnGoalInvalidation", "Cancel the current path if the goal has changed, and the path originally ended in the goal but doesn't anymore.");
        addDescription("censorCoordinates", "Censor coordinates in goals and block positions");
        addDescription("censorRanCommands", "Censor arguments to ran commands, to hide, for example, coordinates to #goal");
        addDescription("chatControl", "Allow chat based control of Baritone.");
        addDescription("chatControlAnyway", "Some clients like Impact try to force chatControl to off, so here's a second setting to do it anyway");
        addDescription("chatDebug", "Print all the debug messages to chat");
        addDescription("chunkCaching", "The big one.");
        addDescription("colorBestPathSoFar", "The color of the best path so far");
        addDescription("colorBlocksToBreak", "The color of the blocks to break");
        addDescription("colorBlocksToPlace", "The color of the blocks to place");
        addDescription("colorBlocksToWalkInto", "The color of the blocks to walk into");
        addDescription("colorCurrentPath", "The color of the current path");
        addDescription("colorGoalBox", "The color of the goal box");
        addDescription("colorInvertedGoalBox", "The color of the goal box when it's inverted");
        addDescription("colorMostRecentConsidered", "The color of the path to the most recent considered node");
        addDescription("colorNextPath", "The color of the next path");
        addDescription("colorSelection", "The color of all selections");
        addDescription("colorSelectionPos1", "The color of the selection pos 1");
        addDescription("colorSelectionPos2", "The color of the selection pos 2");
        addDescription("considerPotionEffects", "For example, if you have Mining Fatigue or Haste, adjust the costs of breaking blocks accordingly.");
        addDescription("costHeuristic", "This is the big A* setting.");
        addDescription("costVerificationLookahead", "Stop 5 movements before anything that made the path COST_INF.");
        addDescription("cutoffAtLoadBoundary", "After calculating a path (potentially through cached chunks), artificially cut it off to just the part that is entirely within currently loaded chunks.");
        addDescription("desktopNotifications", "Desktop notifications");
        addDescription("disableCompletionCheck", "Turn this on if your exploration filter is enormous, you don't want it to check if it's done, and you are just fine with it just hanging on completion");
        addDescription("disconnectOnArrival", "Disconnect from the server upon arriving at your goal");
        addDescription("distanceTrim", "Trim incorrect positions too far away, helps performance but hurts reliability in very large schematics");
        addDescription("doBedWaypoints", "Allows baritone to save bed waypoints when interacting with beds");
        addDescription("doDeathWaypoints", "Allows baritone to save death waypoints");
        addDescription("echoCommands", "Echo commands to chat when they are run");
        addDescription("enterPortal", "When running a goto towards a nether portal block, walk all the way into the portal instead of stopping one block before.");
        addDescription("exploreChunkSetMinimumSize", "Take the 10 closest chunks, even if they aren't strictly tied for distance metric from origin.");
        addDescription("exploreForBlocks", "When GetToBlock or non-legit Mine doesn't know any locations for the desired block, explore randomly instead of giving up.");
        addDescription("exploreMaintainY", "Attempt to maintain Y coordinate while exploring");
        addDescription("extendCacheOnThreshold", "When the cache scan gives less blocks than the maximum threshold (but still above zero), scan the main world too.");
        addDescription("fadePath", "Start fading out the path at 20 movements ahead, and stop rendering it entirely 30 movements ahead.");
        addDescription("failureTimeoutMS", "Pathing can never take longer than this, even if that means failing to find any path at all");
        addDescription("followOffsetDirection", "The actual GoalNear is set in this direction from the entity you're following.");
        addDescription("followOffsetDistance", "The actual GoalNear is set this distance away from the entity you're following");
        addDescription("followRadius", "The radius (for the GoalNear) of how close to your target position you actually have to be");
        addDescription("forceInternalMining", "When mining block of a certain type, try to mine two at once instead of one.");
        addDescription("freeLook", "Move without having to force the client-sided rotations");
        addDescription("goalBreakFromAbove", "As well as breaking from above, set a goal to up and to the side of all blocks to break.");
        addDescription("goalRenderLineWidthPixels", "Line width of the goal when rendered, in pixels");
        addDescription("incorrectSize", "The set of incorrect blocks can never grow beyond this size");
        addDescription("internalMiningAirException", "Modification to the previous setting, only has effect if forceInternalMining is true If true, only apply the previous setting if the block adjacent to the goal isn't air.");
        addDescription("itemSaver", "Stop using tools just before they are going to break.");
        addDescription("itemSaverThreshold", "Durability to leave on the tool when using itemSaver");
        addDescription("jumpPenalty", "Additional penalty for hitting the space bar (ascend, pillar, or parkour) because it uses hunger");
        addDescription("layerHeight", "How high should the individual layers be?");
        addDescription("layerOrder", "false = build from bottom to top");
        addDescription("legitMine", "Disallow MineBehavior from using X-Ray to see where the ores are.");
        addDescription("legitMineIncludeDiagonals", "Magically see ores that are separated diagonally from existing ores.");
        addDescription("legitMineYLevel", "What Y level to go to for legit strip mining");
        addDescription("logAsToast", "Shows popup message in the upper right corner, similarly to when you make an advancement");
        addDescription("mapArtMode", "Build in map art mode, which makes baritone only care about the top block in each column");
        addDescription("maxCachedWorldScanCount", "After finding this many instances of the target block in the cache, it will stop expanding outward the chunk search.");
        addDescription("maxCostIncrease", "If a movement's cost increases by more than this amount between calculation and execution (due to changes in the environment / world), cancel and recalculate");
        addDescription("maxFallHeightBucket", "How far are you allowed to fall onto solid ground (with a water bucket)? It's not that reliable, so I've set it below what would kill an unarmored player (23)");
        addDescription("maxFallHeightNoWater", "How far are you allowed to fall onto solid ground (without a water bucket)? 3 won't deal any damage.");
        addDescription("maxPathHistoryLength", "If we are more than 300 movements into the current path, discard the oldest segments, as they are no longer useful");
        addDescription("mineDropLoiterDurationMSThanksLouca", "While mining, wait this number of milliseconds after mining an ore to see if it will drop an item instead of immediately going onto the next one");
        addDescription("mineGoalUpdateInterval", "Rescan for the goal once every 5 ticks.");
        addDescription("mineScanDroppedItems", "While mining, should it also consider dropped items of the correct type as a pathing destination (as well as ore blocks)?");
        addDescription("minimumImprovementRepropagation", "Don't repropagate cost improvements below 0.01 ticks.");
        addDescription("minYLevelWhileMining", "Sets the minimum y level whilst mining - set to 0 to turn off. if world has negative y values, subtract the min world height to get the value to put here");
        addDescription("mobAvoidanceCoefficient", "Set to 1.0 to effectively disable this feature");
        addDescription("mobAvoidanceRadius", "Distance to avoid mobs.");
        addDescription("mobSpawnerAvoidanceCoefficient", "Set to 1.0 to effectively disable this feature");
        addDescription("mobSpawnerAvoidanceRadius", "Distance to avoid mob spawners.");
        addDescription("movementTimeoutTicks", "If a movement takes this many ticks more than its initial cost estimate, cancel it");
        addDescription("notificationOnBuildFinished", "Desktop notification on build finished");
        addDescription("notificationOnExploreFinished", "Desktop notification on explore finished");
        addDescription("notificationOnFarmFail", "Desktop notification on farm fail");
        addDescription("notificationOnMineFail", "Desktop notification on mine fail");
        addDescription("notificationOnPathComplete", "Desktop notification on path complete");
        addDescription("notifier", "The function that is called when Baritone will send a desktop notification.");
        addDescription("okIfAir", "A list of blocks to become air");
        addDescription("okIfWater", "Override builder's behavior to not attempt to correct blocks that are currently water");
        addDescription("overshootTraverse", "If we overshoot a traverse and end up one block beyond the destination, mark it as successful anyway.");
        addDescription("pathCutoffFactor", "Static cutoff factor.");
        addDescription("pathCutoffMinimumLength", "Only apply static cutoff for paths of at least this length (in terms of number of movements)");
        addDescription("pathHistoryCutoffAmount", "If the current path is too long, cut off this many movements from the beginning.");
        addDescription("pathingMapDefaultSize", "Default size of the Long2ObjectOpenHashMap used in pathing");
        addDescription("pathingMapLoadFactor", "Load factor coefficient for the Long2ObjectOpenHashMap used in pathing");
        addDescription("pathingMaxChunkBorderFetch", "The maximum number of times it will fetch outside loaded or cached chunks before assuming that pathing has reached the end of the known area, and should therefore stop.");
        addDescription("pathRenderLineWidthPixels", "Line width of the path when rendered, in pixels");
        addDescription("pathThroughCachedOnly", "Exclusively use cached chunks for pathing");
        addDescription("pauseMiningForFallingBlocks", "When breaking blocks for a movement, wait until all falling blocks have settled before continuing");
        addDescription("planAheadFailureTimeoutMS", "Planning ahead while executing a segment can never take longer than this, even if that means failing to find any path at all");
        addDescription("planAheadPrimaryTimeoutMS", "Planning ahead while executing a segment ends after this amount of time, but only if a path has been found");
        addDescription("planningTickLookahead", "Start planning the next path once the remaining movements tick estimates sum up to less than this value");
        addDescription("preferSilkTouch", "Always prefer silk touch tools over regular tools.");
        addDescription("prefix", "The command prefix for chat control");
        addDescription("prefixControl", "Whether or not to allow you to run Baritone commands with the prefix");
        addDescription("primaryTimeoutMS", "Pathing ends after this amount of time, but only if a path has been found");
        addDescription("pruneRegionsFromRAM", "On save, delete from RAM any cached regions that are more than 1024 blocks away from the player");
        addDescription("randomLooking", "How many degrees to randomize the pitch and yaw every tick.");
        addDescription("randomLooking113", "How many degrees to randomize the yaw every tick. Set to 0 to disable");
        addDescription("renderCachedChunks", "Render cached chunks as semitransparent.");
        addDescription("renderGoal", "Render the goal");
        addDescription("renderGoalAnimated", "Render the goal as a sick animated thingy instead of just a box (also controls animation of GoalXZ if renderGoalXZBeacon is enabled)");
        addDescription("renderGoalIgnoreDepth", "Ignore depth when rendering the goal");
        addDescription("renderGoalXZBeacon", "Renders X/Z type Goals with the vanilla beacon beam effect.");
        addDescription("renderPath", "Render the path");
        addDescription("renderPathAsLine", "Render the path as a line instead of a frickin thingy");
        addDescription("renderPathIgnoreDepth", "Ignore depth when rendering the path");
        addDescription("renderSelection", "Render selections");
        addDescription("renderSelectionBoxes", "Render selection boxes");
        addDescription("renderSelectionBoxesIgnoreDepth", "Ignore depth when rendering the selection boxes (to break, to place, to walk into)");
        addDescription("renderSelectionCorners", "Render selection corners");
        addDescription("renderSelectionIgnoreDepth", "Ignore depth when rendering selections");
        addDescription("repackOnAnyBlockChange", "Whenever a block changes, repack the whole chunk that it's in");
        addDescription("replantCrops", "Replant normal Crops while farming and leave cactus and sugarcane to regrow");
        addDescription("replantNetherWart", "Replant nether wart while farming.");
        addDescription("rightClickContainerOnArrival", "When running a goto towards a container block (chest, ender chest, furnace, etc), right click and open it once you arrive.");
        addDescription("rightClickSpeed", "How many ticks between right clicks are allowed.");
        addDescription("schematicFallbackExtension", "The fallback used by the build command when no extension is specified.");
        addDescription("schematicOrientationX", "When this setting is true, build a schematic with the highest X coordinate being the origin, instead of the lowest");
        addDescription("schematicOrientationY", "When this setting is true, build a schematic with the highest Y coordinate being the origin, instead of the lowest");
        addDescription("schematicOrientationZ", "When this setting is true, build a schematic with the highest Z coordinate being the origin, instead of the lowest");
        addDescription("selectionLineWidth", "Line width of the goal when rendered, in pixels");
        addDescription("selectionOpacity", "The opacity of the selection.");
        addDescription("shortBaritonePrefix", "Use a short Baritone prefix [B] instead of [Baritone] when logging to chat");
        addDescription("simplifyUnloadedYCoord", "If your goal is a GoalBlock in an unloaded chunk, assume it's far enough away that the Y coord doesn't matter yet, and replace it with a GoalXZ to the same place before calculating a path.");
        addDescription("skipFailedLayers", "If a layer is unable to be constructed, just skip it.");
        addDescription("slowPath", "For debugging, consider nodes much much slower");
        addDescription("slowPathTimeDelayMS", "Milliseconds between each node");
        addDescription("slowPathTimeoutMS", "The alternative timeout number when slowPath is on");
        addDescription("splicePath", "When a new segment is calculated that doesn't overlap with the current one, but simply begins where the current segment ends, splice it on and make a longer combined path.");
        addDescription("sprintAscends", "Sprint and jump a block early on ascends wherever possible");
        addDescription("sprintInWater", "Continue sprinting while in water");
        addDescription("startAtLayer", "Start building the schematic at a specific layer.");
        addDescription("toaster", "The function that is called when Baritone will show a toast.");
        addDescription("toastTimer", "The time of how long the message in the pop-up will display");
        addDescription("useSwordToMine", "Use sword to mine.");
        addDescription("verboseCommandExceptions", "Print out ALL command exceptions as a stack trace to stdout, even simple syntax errors");
        addDescription("walkOnWaterOnePenalty", "Walking on water uses up hunger really quick, so penalize it");
        addDescription("walkWhileBreaking", "Don't stop walking forward when you need to break blocks in your way");
        addDescription("worldExploringChunkOffset", "While exploring the world, offset the closest unloaded chunk by this much in both axes.");
        addDescription("yLevelBoxSize", "The size of the box that is rendered when the current goal is a GoalYLevel");
    }
}

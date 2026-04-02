package meteordevelopment.meteorclient.systems.modules.world;

import baritone.api.BaritoneAPI;
import baritone.api.pathing.goals.GoalBlock;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.combat.KillAura;
import meteordevelopment.meteorclient.systems.modules.player.AutoEat;
import meteordevelopment.meteorclient.systems.modules.player.AutoGap;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FallingBlock;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class PoweredRailBuilder extends Module {
    private enum RailSide {
        Left,
        Right
    }

    private enum RailMode {
        ALL_POWERED,
        MIXED
    }

    private enum PowerType {
        REDSTONE_BLOCK,
        REDSTONE_TORCH
    }

    private enum State {
        HandleLiquids,
        HandleFallingBlocks,
        ClearSpace,
        PlaceShell,
        PlaceFloors,
        PlacePower,
        PlaceLight,
        EncapsulatePower,
        PlaceRail,
        MoveToNext,
        Advance,
        Done
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgStructure = settings.createGroup("Structure");
    private final SettingGroup sgMaterials = settings.createGroup("Materials");
    private final SettingGroup sgPower = settings.createGroup("Power");
    private final SettingGroup sgLighting = settings.createGroup("Lighting");
    private final SettingGroup sgTools = settings.createGroup("Tools");

    private final Setting<Boolean> protectToolDurability = sgTools.add(new BoolSetting.Builder()
        .name("protect-tool-durability")
        .description("Avoid using tools below a durability threshold.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> minToolDurability = sgTools.add(new IntSetting.Builder()
        .name("min-tool-durability")
        .description("Minimum durability remaining before a tool is considered unusable.")
        .defaultValue(50)
        .range(1, 500)
        .sliderRange(1, 200)
        .visible(protectToolDurability::get)
        .build()
    );

    private final Setting<List<Block>> pickaxeBlocks = sgTools.add(new BlockListSetting.Builder()
        .name("pickaxe-blocks")
        .description("Blocks that should prefer a pickaxe.")
        .defaultValue(
            Blocks.STONE,
            Blocks.COBBLESTONE,
            Blocks.DEEPSLATE,
            Blocks.COBBLED_DEEPSLATE,
            Blocks.OBSIDIAN,
            Blocks.NETHERRACK,
            Blocks.BLACKSTONE,
            Blocks.BASALT,
            Blocks.ENDER_CHEST
        )
        .build()
    );

    private final Setting<List<Block>> shovelBlocks = sgTools.add(new BlockListSetting.Builder()
        .name("shovel-blocks")
        .description("Blocks that should prefer a shovel.")
        .defaultValue(
            Blocks.DIRT,
            Blocks.GRASS_BLOCK,
            Blocks.MYCELIUM,
            Blocks.PODZOL,
            Blocks.COARSE_DIRT,
            Blocks.ROOTED_DIRT,
            Blocks.SAND,
            Blocks.RED_SAND,
            Blocks.GRAVEL,
            Blocks.CLAY,
            Blocks.SOUL_SAND,
            Blocks.SOUL_SOIL,
            Blocks.SNOW_BLOCK
        )
        .build()
    );

    private final Setting<List<Block>> axeBlocks = sgTools.add(new BlockListSetting.Builder()
        .name("axe-blocks")
        .description("Blocks that should prefer an axe.")
        .defaultValue(
            Blocks.OAK_LOG,
            Blocks.SPRUCE_LOG,
            Blocks.BIRCH_LOG,
            Blocks.JUNGLE_LOG,
            Blocks.ACACIA_LOG,
            Blocks.DARK_OAK_LOG,
            Blocks.MANGROVE_LOG,
            Blocks.CHERRY_LOG,
            Blocks.CRIMSON_STEM,
            Blocks.WARPED_STEM,
            Blocks.OAK_PLANKS,
            Blocks.SPRUCE_PLANKS,
            Blocks.BIRCH_PLANKS,
            Blocks.JUNGLE_PLANKS,
            Blocks.ACACIA_PLANKS,
            Blocks.DARK_OAK_PLANKS,
            Blocks.MANGROVE_PLANKS,
            Blocks.CHERRY_PLANKS,
            Blocks.CRIMSON_PLANKS,
            Blocks.WARPED_PLANKS
        )
        .build()
    );

    private final Setting<Integer> length = sgGeneral.add(new IntSetting.Builder()
        .name("length")
        .description("How many slices to build.")
        .defaultValue(128)
        .min(1)
        .sliderRange(1, 2048)
        .build()
    );

    private final Setting<Boolean> useBaritone = sgGeneral.add(new BoolSetting.Builder()
        .name("use-baritone")
        .description("Uses Baritone to walk to the next built slice.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> stopOnDeath = sgGeneral.add(new BoolSetting.Builder()
        .name("stop-on-death")
        .description("Stops the module and cancels Baritone when you die.")
        .defaultValue(true)
        .visible(useBaritone::get)
        .build()
    );

    private final Setting<Integer> pathIdleTimeout = sgGeneral.add(new IntSetting.Builder()
        .name("path-idle-timeout")
        .description("Ticks with an active Baritone goal but no movement before recovery steps farther back.")
        .defaultValue(40)
        .range(5, 200)
        .sliderRange(10, 100)
        .visible(useBaritone::get)
        .build()
    );

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
        .name("rotate")
        .description("Rotate while placing blocks.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Double> placeRange = sgGeneral.add(new DoubleSetting.Builder()
        .name("place-range")
        .description("Max placement reach.")
        .defaultValue(4.5)
        .range(1, 6)
        .sliderRange(1, 5.5)
        .build()
    );


    private final Setting<Boolean> handleLiquids = sgGeneral.add(new BoolSetting.Builder()
        .name("handle-liquids")
        .description("Fill water and lava before placing the rest of the structure.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> width = sgStructure.add(new IntSetting.Builder()
        .name("width")
        .description("Interior tunnel width.")
        .defaultValue(2)
        .range(1, 7)
        .sliderRange(1, 5)
        .build()
    );

    private final Setting<Integer> height = sgStructure.add(new IntSetting.Builder()
        .name("height")
        .description("Interior tunnel height.")
        .defaultValue(3)
        .range(2, 6)
        .sliderRange(2, 5)
        .build()
    );

    private final Setting<RailSide> railSide = sgStructure.add(new EnumSetting.Builder<RailSide>()
        .name("rail-side")
        .description("Which side of the tunnel the rail should trend toward relative to your facing direction.")
        .defaultValue(RailSide.Right)
        .build()
    );

    private final Setting<Integer> railOffset = sgStructure.add(new IntSetting.Builder()
        .name("rail-offset")
        .description("Rail lateral offset inside the tunnel, where 0 is your walking lane and width-1 is furthest across the tunnel.")
        .defaultValue(1)
        .range(0, 7)
        .sliderRange(0, 4)
        .build()
    );

    private final Setting<Boolean> ensureShell = sgStructure.add(new BoolSetting.Builder()
        .name("ensure-shell")
        .description("Build side walls and a roof around the tunnel.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> secondFloorLayer = sgStructure.add(new BoolSetting.Builder()
        .name("second-floor-layer")
        .description("Places a second structural row one block below the normal floor. Hidden torch setups also force this on automatically.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> thirdFloorLayer = sgStructure.add(new BoolSetting.Builder()
        .name("third-floor-layer")
        .description("Places a third structural row below the normal floor for a flat bottom.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> encapsulatePower = sgStructure.add(new BoolSetting.Builder()
        .name("encapsulate-power")
        .description("Places floor blocks around the power source without overwriting rails.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> placeCorners = sgStructure.add(new BoolSetting.Builder()
        .name("place-corners")
        .description("Whether to place the shell corner blocks.")
        .defaultValue(true)
        .visible(ensureShell::get)
        .build()
    );

    private final Setting<Boolean> clearTunnelSpace = sgStructure.add(new BoolSetting.Builder()
        .name("clear-tunnel-space")
        .description("Break obstructing blocks in the tunnel interior.")
        .defaultValue(true)
        .build()
    );


    private final Setting<List<Block>> floorBlocks = sgMaterials.add(new BlockListSetting.Builder()
        .name("floor-blocks")
        .description("Allowed structural blocks for the floor and other non-shell support positions.")
        .defaultValue(Blocks.OBSIDIAN, Blocks.COBBLESTONE, Blocks.STONE, Blocks.DEEPSLATE)
        .filter(block -> Block.isShapeFullCube(block.getDefaultState().getCollisionShape(mc.world, BlockPos.ORIGIN)))
        .build()
    );

    private final Setting<List<Block>> shellBlocks = sgMaterials.add(new BlockListSetting.Builder()
        .name("shell-blocks")
        .description("Allowed blocks for shell positions.")
        .defaultValue(Blocks.OBSIDIAN, Blocks.COBBLESTONE, Blocks.STONE, Blocks.DEEPSLATE)
        .filter(block -> Block.isShapeFullCube(block.getDefaultState().getCollisionShape(mc.world, BlockPos.ORIGIN)))
        .build()
    );

    private final Setting<List<Block>> cornerBlocks = sgMaterials.add(new BlockListSetting.Builder()
        .name("corner-blocks")
        .description("Blocks used for shell corners.")
        .defaultValue(Blocks.OBSIDIAN)
        .visible(() -> ensureShell.get() && placeCorners.get())
        .filter(block -> Block.isShapeFullCube(block.getDefaultState().getCollisionShape(mc.world, BlockPos.ORIGIN)))
        .build()
    );

    private final Setting<Boolean> liquidUsesShellBlocks = sgMaterials.add(new BoolSetting.Builder()
        .name("liquid-uses-shell-blocks")
        .description("Use shell blocks for liquid sealing instead of a separate block list.")
        .defaultValue(true)
        .build()
    );

    private final Setting<List<Block>> liquidBlocks = sgMaterials.add(new BlockListSetting.Builder()
        .name("liquid-blocks")
        .description("Blocks used for liquid sealing when not using shell blocks.")
        .defaultValue(Blocks.NETHERRACK, Blocks.COBBLESTONE)
        .visible(() -> !liquidUsesShellBlocks.get())
        .filter(block -> Block.isShapeFullCube(block.getDefaultState().getCollisionShape(mc.world, BlockPos.ORIGIN)))
        .build()
    );

    private final Setting<RailMode> railMode = sgPower.add(new EnumSetting.Builder<RailMode>()
        .name("rail-mode")
        .description("Whether to use powered rails everywhere or alternate powered and regular rails.")
        .defaultValue(RailMode.ALL_POWERED)
        .build()
    );

    private final Setting<Integer> powerSpacing = sgPower.add(new IntSetting.Builder()
        .name("power-spacing")
        .description("Distance between power events.")
        .defaultValue(16)
        .range(1, 256)
        .sliderRange(1, 64)
        .build()
    );

    private final Setting<Integer> poweredLength = sgPower.add(new IntSetting.Builder()
        .name("powered-length")
        .description("How many rails at the start of each cycle are powered rails in mixed mode. Power is placed at the center of this section.")
        .defaultValue(4)
        .range(1, 64)
        .sliderRange(1, 32)
        .visible(() -> railMode.get() == RailMode.MIXED)
        .build()
    );

    private final Setting<PowerType> powerType = sgPower.add(new EnumSetting.Builder<PowerType>()
        .name("power-type")
        .description("What kind of power source to place.")
        .defaultValue(PowerType.REDSTONE_BLOCK)
        .build()
    );

    private final Setting<Integer> powerLateralOffset = sgPower.add(new IntSetting.Builder()
        .name("power-lateral-offset")
        .description("Power source lateral offset relative to the rail position. Negative moves back toward your walking lane, positive moves further across the tunnel.")
        .defaultValue(0)
        .range(-8, 8)
        .sliderRange(-4, 4)
        .build()
    );

    private final Setting<Integer> powerVerticalOffset = sgPower.add(new IntSetting.Builder()
        .name("power-vertical-offset")
        .description("Power source vertical offset relative to the rail position. Allowed values are -2 through 1.")
        .defaultValue(-1)
        .range(-2, 1)
        .sliderRange(-2, 1)
        .build()
    );
    
    private final Setting<Boolean> placeLight = sgLighting.add(new BoolSetting.Builder()
        .name("place-light")
        .description("Place light sources at intervals.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Integer> lightSpacing = sgLighting.add(new IntSetting.Builder()
        .name("light-spacing")
        .description("Distance between light placements.")
        .defaultValue(20)
        .range(1, 256)
        .sliderRange(1, 64)
        .visible(placeLight::get)
        .build()
    );

    private final Setting<Block> lightBlock = sgLighting.add(new BlockSetting.Builder()
        .name("light-block")
        .description("Block used for lighting. Default is a regular torch.")
        .defaultValue(Blocks.TORCH)
        .visible(placeLight::get)
        .build()
    );

    private final Setting<Integer> lightLateralOffset = sgLighting.add(new IntSetting.Builder()
        .name("light-lateral-offset")
        .description("Light position lateral offset relative to the rail position.")
        .defaultValue(0)
        .range(-8, 8)
        .sliderRange(-4, 4)
        .visible(placeLight::get)
        .build()
    );

    private final Setting<Integer> lightVerticalOffset = sgLighting.add(new IntSetting.Builder()
        .name("light-vertical-offset")
        .description("Light position vertical offset relative to the rail position.")
        .defaultValue(1)
        .range(-2, 3)
        .sliderRange(-2, 3)
        .visible(placeLight::get)
        .build()
    );

    private BlockPos origin;
    private Direction forward;
    private Direction crossDir;

    private BlockPos walkPos;
    private BlockPos railPos;
    private State state = State.Done;
    private int step;
    private BlockPos lastGoal;
    private int fallingStableTicks;
    private int pathIdleTicks;
    private Vec3d lastPathPlayerPos;
    private boolean recovering;
    private int recoveryResumeStep;
    private int recoveryAttempts;

    public PoweredRailBuilder() {
        super(Categories.World, "powered-rail-builder", "Builds a configurable tunnel with rails, shell, and configurable power placement.");
    }

    @Override
    public void onActivate() {
        if (!Utils.canUpdate()) return;

        origin = mc.player.getBlockPos();
        forward = mc.player.getHorizontalFacing();

        if (forward == Direction.UP || forward == Direction.DOWN) {
            error("Invalid facing direction.");
            toggle();
            return;
        }

        crossDir = railSide.get() == RailSide.Right ? forward.rotateYClockwise() : forward.rotateYCounterclockwise();
        step = 0;
        lastGoal = null;
        fallingStableTicks = 0;
        pathIdleTicks = 0;
        lastPathPlayerPos = null;
        recovering = false;
        recoveryResumeStep = 0;
        recoveryAttempts = 0;

        if (powerLateralOffset.get() == 0 && powerVerticalOffset.get() == 0) {
            error("Power source cannot occupy the same position as the rail.");
            toggle();
            return;
        }

        BaritoneAPI.getSettings().allowSprint.value = true;

        updateStepPositions();
        state = initialState();

        info(
    "Start locked. Facing %s. Size=%dx%d railOffset=%d shell=%s railMode=%s powerType=%s floorLayers=%d encapsulatePower=%s",
    forward,
    getWidthValue(),
    getHeightValue(),
    getRailLaneIndex(),
    shouldUseShell(),
    railMode.get(),
    powerType.get(),
    getFloorLayerCount(),
    encapsulatePower.get()
);
    }

    @Override
    public void onDeactivate() {
        stopBaritone();
    }

    private void stopBaritone() {
        lastGoal = null;
        pathIdleTicks = 0;
        lastPathPlayerPos = null;

        if (!useBaritone.get()) return;

        try {
            var baritone = BaritoneAPI.getProvider().getPrimaryBaritone();
            baritone.getPathingBehavior().cancelEverything();
            baritone.getCustomGoalProcess().setGoal(null);
        } catch (Throwable ignored) {
            // Ignore API differences across Baritone branches.
        }
    }

    private boolean shouldPauseForOtherModules() {
        return Modules.get().get(AutoEat.class).eating
            || Modules.get().get(AutoGap.class).isEating()
            || Modules.get().get(KillAura.class).attacking;
    }

    private void pausePathingForOtherModules() {
        stopBaritone();
    }

    private void resetPathIdle() {
        pathIdleTicks = 0;
        lastPathPlayerPos = mc.player != null ? mc.player.getEntityPos() : null;
    }

    private void setBaritoneGoal(BlockPos goal) {
        BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess()
            .setGoalAndPath(new GoalBlock(goal.getX(), goal.getY(), goal.getZ()));

        lastGoal = goal.toImmutable();
        resetPathIdle();
    }

    private void tickPathIdle() {
        if (lastGoal == null || mc.player == null) {
            pathIdleTicks = 0;
            lastPathPlayerPos = null;
            return;
        }

        Vec3d currentPos = mc.player.getEntityPos();

        if (lastPathPlayerPos != null && currentPos.squaredDistanceTo(lastPathPlayerPos) <= 0.01) {
            pathIdleTicks++;
        } else {
            pathIdleTicks = 0;
            lastPathPlayerPos = currentPos;
        }
    }

    private int getRecoveryBacktrackCap() {
        return ((getHeightValue() + 1) * 7) + 2;
    }

    private BlockPos getRecoveryTargetPos() {
        return origin.offset(forward, Math.max(0, recoveryResumeStep + 1));
    }

    private void startRecovery() {
        if (!useBaritone.get()) {
            error("Recovery requires Baritone.");
            toggle();
            return;
        }

        stopBaritone();
        recovering = true;
        recoveryResumeStep = step - 1;
        recoveryAttempts = 0;

        warning("Baritone stalled. Starting recovery.");
    }

    private void stepRecoveryFartherBack() {
        stopBaritone();
        recoveryResumeStep--;
        recoveryAttempts++;
    }

    private void tickRecovery() {
        int cap = getRecoveryBacktrackCap();

        if (recoveryAttempts > cap) {
            error("Recovery failed after %d backtrack attempts.", cap);
            toggle();
            return;
        }

        BlockPos target = getRecoveryTargetPos();

        if (mc.player.getBlockPos().equals(target)) {
            stopBaritone();

            recovering = false;
            step = Math.max(0, recoveryResumeStep);
            updateStepPositions();
            fallingStableTicks = 0;
            state = initialState();

            info("Recovery succeeded. Resuming from step %d.", step);
            return;
        }

        if (lastGoal == null || !lastGoal.equals(target)) {
            setBaritoneGoal(target);
            return;
        }

        if (pathIdleTicks >= pathIdleTimeout.get()) {
            stepRecoveryFartherBack();
        }
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (!Utils.canUpdate()) return;
        if (stopOnDeath.get() && mc.player != null) {
            if (mc.player.isDead() || mc.player.getHealth() <= 0) {
                warning("Player died. Stopping powered rail builder.");
                toggle();
                return;
            }
        }       
        if (state == State.Done) return;

        if (shouldPauseForOtherModules()) {
            pausePathingForOtherModules();
            return;
        }

        if (step >= length.get()) {
            stopBaritone();
            info("Finished.");
            state = State.Done;
            return;
        }

        tickPathIdle();

        if (recovering) {
            tickRecovery();
            return;
        }

        if (lastGoal != null && pathIdleTicks >= pathIdleTimeout.get()) {
            startRecovery();
            tickRecovery();
            return;
        }

        updateStepPositions();

        switch (state) {
            case HandleLiquids -> tickHandleLiquids();
            case HandleFallingBlocks -> tickHandleFallingBlocks();
            case ClearSpace -> tickClearSpace();
            case PlaceShell -> tickPlaceShell();
            case PlaceFloors -> tickPlaceFloors();
            case PlacePower -> tickPlacePower();
            case PlaceLight -> tickPlaceLight();
            case EncapsulatePower -> tickEncapsulatePower();
            case PlaceRail -> tickPlaceRail();
            case MoveToNext -> tickMoveToNext();
            case Advance -> tickAdvance();
            case Done -> {}
        }
    }

    private State initialState() {
        if (handleLiquids.get()) return State.HandleLiquids;
        return State.HandleFallingBlocks;
    }

    private void updateStepPositions() {
        walkPos = origin.offset(forward, step + 1);
        railPos = getLaneBase(getRailLaneIndex());
    }

    private void tickHandleLiquids() {
        // 1) Break gravity blocks in the lookahead slice (+2 from player, +1 from current build slice).
        for (BlockPos pos : getLookaheadFallingBlockPositions()) {
            BlockState stateAtPos = mc.world.getBlockState(pos);
            if (!isFallingBlock(stateAtPos)) continue;

            if (!ensureWorkAnchorInRange()) return;

            if (!ensureToolFor(pos)) return;
            BlockUtils.breakBlock(pos, true);
            return;
        }

        // Wait for falling entities in the lookahead slice to settle before sealing.
        if (hasFallingBlockEntitiesAhead()) return;

        // 2) If tunnel-region water exists above floor in the lookahead slice,
        // build the temporary tunnel face seal using liquid-fix blocks only.
        if (hasLookaheadTunnelLiquidAboveFloor()) {
            for (BlockPos pos : getLookaheadTunnelSealPositions()) {
                BlockState stateAtPos = mc.world.getBlockState(pos);

                if (isAcceptableStructuralBlock(stateAtPos, pos)) continue;

                if (!ensureWorkAnchorInRange()) return;

                FindItemResult block = findLiquidBlock();
                if (!block.found()) {
                    error("No liquid handling blocks found in inventory.");
                    toggle();
                    return;
                }

                if (isFluidOccupiedObstacle(stateAtPos)) {
                    if (!ensureToolFor(pos)) return;
                    BlockUtils.breakBlock(pos, true);
                    return;
                }

                if (canPlaceDirectlyInto(stateAtPos)) {
                    BlockUtils.place(pos, block, rotate.get(), 50);
                    return;
                }
                if (!ensureToolFor(pos)) return;
                BlockUtils.breakBlock(pos, true);
                return;
            }
        }

        // 3) If shell-region water exists above floor in the lookahead slice,
        // build the lookahead shell using shell blocks only.
        if (hasLookaheadShellLiquidAboveFloor()) {
            for (BlockPos pos : getRawShellPositions(1)) {
                BlockState stateAtPos = mc.world.getBlockState(pos);

                if (isAcceptableShellBlock(stateAtPos, pos)) continue;

                if (!ensureWorkAnchorInRange()) return;

                FindItemResult shell = findShellBlock();
                if (!shell.found()) {
                    error("No shell block found in inventory.");
                    toggle();
                    return;
                }

                if (isFluidOccupiedObstacle(stateAtPos)) {
                    if (!ensureToolFor(pos)) return;
                    BlockUtils.breakBlock(pos, true);
                    return;
                }

                if (canPlaceDirectlyInto(stateAtPos)) {
                    BlockUtils.place(pos, shell, rotate.get(), 50);
                    return;
                }

                if (!ensureToolFor(pos)) return;
                BlockUtils.breakBlock(pos, true);
                return;
            }
        }

        // Done with +2 handling. Now switch to the current slice (+1).
        fallingStableTicks = 0;
        state = State.HandleFallingBlocks;
    }

    private void tickHandleFallingBlocks() {
        for (BlockPos pos : getFallingBlockPositions()) {
            BlockState stateAtPos = mc.world.getBlockState(pos);
            if (!isFallingBlock(stateAtPos)) continue;

            fallingStableTicks = 0;

            if (!ensureWorkAnchorInRange()) return;
            if (!ensureToolFor(pos)) return;
            BlockUtils.breakBlock(pos, true);
            return;
        }

        // If gravel/sand is still physically falling through this slice, do not continue yet.
        if (hasFallingBlockEntitiesInSlice()) {
            fallingStableTicks = 0;
            return;
        }

        // Give gravity a short extra settle window after the last falling entity disappears.
        if (fallingStableTicks < 4) {
            fallingStableTicks++;
            return;
        }

        state = shouldUseShell() ? State.PlaceShell : (clearTunnelSpace.get() ? State.ClearSpace : State.PlaceFloors);
    }

    private boolean isFallingBlock(BlockState state) {
        return state.getBlock() instanceof FallingBlock;
    }

    private List<BlockPos> getFallingBlockPositions() {
        List<BlockPos> positions = new ArrayList<>();

        // Bottom-up on purpose.
        // Scan the full interior plus 2 blocks above it so hanging gravel/sand gets caught before build steps.
        for (int y = 0; y <= getHeightValue() + 3; y++) {
            for (int lane = 0; lane < getWidthValue(); lane++) {
                positions.add(getLaneBase(lane).up(y));
            }
        }

        return positions;
    }

    private List<BlockPos> getRawShellPositions(int sliceOffset) {
        List<BlockPos> positions = new ArrayList<>();

        for (int lateral = -1; lateral <= getWidthValue(); lateral++) {
            for (int vertical = -1; vertical <= getHeightValue(); vertical++) {
                boolean onSide = lateral == -1 || lateral == getWidthValue();
                boolean onRoof = vertical == getHeightValue();
                if (!onSide && !onRoof) continue;

                if (!placeCorners.get() && isShellCorner(lateral, vertical)) continue;

                positions.add(walkPos.offset(forward, sliceOffset).offset(crossDir, lateral).add(0, vertical, 0));
            }
        }

        return positions;
    }

    private boolean hasLookaheadTunnelLiquidAboveFloor() {
        for (BlockPos pos : getInteriorPositions(1, true)) {
            if (!mc.world.getBlockState(pos).getFluidState().isEmpty()) return true;
        }

        return false;
    }

    private boolean hasLookaheadShellLiquidAboveFloor() {
        int floorY = walkPos.getY();

        for (BlockPos pos : getRawShellPositions(1)) {
            if (pos.getY() < floorY) continue;
            if (!mc.world.getBlockState(pos).getFluidState().isEmpty()) return true;
        }

        return false;
    }

    private List<BlockPos> getLookaheadTunnelSealPositions() {
        // This is the temporary front wall directly in front of the tunnel region.
        return getInteriorPositions(1, true);
    }

    private boolean hasFallingBlockEntitiesInSlice() {
        Box box = getFallingBlockEntityBox();
        return !mc.world.getEntitiesByClass(FallingBlockEntity.class, box, entity -> true).isEmpty();
    }

    private Box getFallingBlockEntityBox() {
        List<BlockPos> positions = getFallingBlockPositions();

        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double minZ = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        double maxZ = Double.NEGATIVE_INFINITY;

        for (BlockPos pos : positions) {
            minX = Math.min(minX, pos.getX());
            minY = Math.min(minY, pos.getY());
            minZ = Math.min(minZ, pos.getZ());

            maxX = Math.max(maxX, pos.getX() + 1);
            maxY = Math.max(maxY, pos.getY() + 1);
            maxZ = Math.max(maxZ, pos.getZ() + 1);
        }

        return new Box(minX, minY, minZ, maxX, maxY, maxZ);
    }

    private void tickClearSpace() {
        for (BlockPos pos : getInteriorClearPositions()) {
            BlockState stateAtPos = mc.world.getBlockState(pos);

            if (pos.equals(railPos)) {
                if (isAnyRail(stateAtPos) || stateAtPos.isAir() || stateAtPos.isReplaceable()) continue;
            }
            else {
                if (stateAtPos.isAir() || stateAtPos.isReplaceable()) continue;
            }

            if (!ensureWorkAnchorInRange()) return;
            if (!ensureToolFor(pos)) return;
            BlockUtils.breakBlock(pos, true);
            return;
        }

        state = State.PlaceFloors;
    }

    private void tickPlaceShell() {
        for (BlockPos shellPos : getShellPositions()) {
            BlockState stateAtPos = mc.world.getBlockState(shellPos);
            if (isAcceptableShellBlock(stateAtPos, shellPos)) continue;

            if (!ensureWorkAnchorInRange()) return;

            boolean isCorner = isShellCornerPosition(shellPos);

            FindItemResult shell = isCorner ? findCornerBlock() : findShellBlock();
            if (!shell.found()) {
                error(isCorner ? "No corner block found in inventory." : "No shell block found in inventory.");
                toggle();
                return;
            }

            if (isFluidOccupiedObstacle(stateAtPos)) {
                if (!ensureToolFor(shellPos)) return;
                BlockUtils.breakBlock(shellPos, true);
                return;
            }

            if (canPlaceDirectlyInto(stateAtPos)) {
                BlockUtils.place(shellPos, shell, rotate.get(), 50);
                return;
            }
            if (!ensureToolFor(shellPos)) return;
            BlockUtils.breakBlock(shellPos, true);
            return;
        }

        state = clearTunnelSpace.get() ? State.ClearSpace : State.PlaceFloors;
    }

    private void tickPlaceFloors() {
        for (BlockPos floorPos : getFloorPositions()) {
            BlockState stateAtFloor = mc.world.getBlockState(floorPos);
            if (isAcceptableStructuralBlock(stateAtFloor, floorPos)) continue;

            if (!ensureWorkAnchorInRange()) return;

            FindItemResult floor = findFloorBlock();
            if (!floor.found()) {
                error("No floor block found in inventory.");
                toggle();
                return;
            }

            if (isFluidOccupiedObstacle(stateAtFloor)) {
                if (!ensureToolFor(floorPos)) return;
                BlockUtils.breakBlock(floorPos, true);
                return;
            }

            if (canPlaceDirectlyInto(stateAtFloor)) {
                BlockUtils.place(floorPos, floor, rotate.get(), 50);
                return;
            }
            if (!ensureToolFor(floorPos)) return;
            BlockUtils.breakBlock(floorPos, true);
            return;
        }

        state = State.PlacePower;
    }

    private void tickPlacePower() {
        if (!shouldPlacePowerAtStep()) {
            state = State.PlaceLight;
            return;
        }

        BlockPos powerPos = getPowerPos();

        if (powerType.get() == PowerType.REDSTONE_BLOCK) {
            BlockState current = mc.world.getBlockState(powerPos);
            if (current.getBlock() == Blocks.REDSTONE_BLOCK) {
                state = State.PlaceLight;
                return;
            }

            if (!ensureWorkAnchorInRange()) return;

            FindItemResult redstone = ensureHotbarItem(
                stack -> stack.getItem() == Items.REDSTONE_BLOCK,
                ItemType.POWER
            );
            if (!redstone.found()) {
                error("No redstone blocks found in inventory.");
                toggle();
                return;
            }

            if (isFluidOccupiedObstacle(current)) {
                if (!ensureToolFor(powerPos)) return;
                BlockUtils.breakBlock(powerPos, true);
                return;
            }

            if (canPlaceDirectlyInto(current)) {
                BlockUtils.place(powerPos, redstone, rotate.get(), 50);
                return;
            }

            if (current.getBlock() != Blocks.REDSTONE_BLOCK) {
                if (!ensureToolFor(powerPos)) return;
                BlockUtils.breakBlock(powerPos, true);
                return;
            }
        }
        else {
            BlockPos torchPos = getTorchPlacePos();
            BlockPos supportPos = getTorchSupportPos();

            BlockState supportState = mc.world.getBlockState(supportPos);

            // Special-case: "above the rail" torch should be wall-mounted.
            // If the wall block doesn't exist, just fail quietly and continue.
            if (isAboveRailTorchMode()) {
                if (!isAcceptableStructuralBlock(supportState, supportPos)) {
                    state = encapsulatePower.get() ? State.EncapsulatePower : State.PlaceRail;
                    return;
                }
            }
            else {
                if (!isAcceptableStructuralBlock(supportState, supportPos)) {
                    if (!ensureWorkAnchorInRange()) return;

                    FindItemResult support = chooseSupportBlockFor(supportPos);
                    if (!support.found()) {
                        error("No support block found for redstone torch placement.");
                        toggle();
                        return;
                    }

                    if (isFluidOccupiedObstacle(supportState)) {
                        if (!ensureToolFor(supportPos)) return;
                        BlockUtils.breakBlock(supportPos, true);
                        return;
                    }

                    if (canPlaceDirectlyInto(supportState)) {
                        BlockUtils.place(supportPos, support, rotate.get(), 50);
                        return;
                    }
                    if (!ensureToolFor(supportPos)) return;
                    BlockUtils.breakBlock(supportPos, true);
                    return;
                }
            }

            BlockState torchState = mc.world.getBlockState(torchPos);
            if (isAnyRedstoneTorch(torchState)) {
                state = State.PlaceLight;
                return;
            }

            if (!ensureWorkAnchorInRange()) return;

        FindItemResult torch = ensureHotbarItem(
            stack -> stack.getItem() == Items.REDSTONE_TORCH,
            ItemType.POWER
        );
            if (!torch.found()) {
                error("No redstone torches found in inventory.");
                toggle();
                return;
            }

            if (isFluidOccupiedObstacle(torchState)) {
                if (!ensureToolFor(torchPos)) return;
                BlockUtils.breakBlock(torchPos, true);
                return;
            }

            if (canPlaceDirectlyInto(torchState)) {
                placeTorch(torch, torchPos, supportPos);
                return;
            }
            if (!ensureToolFor(torchPos)) return;
            BlockUtils.breakBlock(torchPos, true);
            return;
        }

        state = State.PlaceLight;
    }

    private void tickEncapsulatePower() {
        for (BlockPos pos : getEncapsulationPositions()) {
            BlockState stateAtPos = mc.world.getBlockState(pos);

            if (isAcceptableStructuralBlock(stateAtPos, pos)) continue;
            if (isRailOrFutureRailPosition(pos)) continue;

            if (!ensureWorkAnchorInRange()) return;

            FindItemResult floor = findFloorBlock();
            if (!floor.found()) {
                error("No floor block found for power encapsulation.");
                toggle();
                return;
            }

            if (isFluidOccupiedObstacle(stateAtPos)) {
                if (!ensureToolFor(pos)) return;
                BlockUtils.breakBlock(pos, true);
                return;
            }

            if (canPlaceDirectlyInto(stateAtPos)) {
                BlockUtils.place(pos, floor, rotate.get(), 50);
                return;
            }
            if (!ensureToolFor(pos)) return;
            BlockUtils.breakBlock(pos, true);
            return;
        }

        state = State.PlaceRail;
    }

    private void tickPlaceRail() {
        BlockState railState = mc.world.getBlockState(railPos);
        Block desiredRail = getDesiredRailBlock();

        if (railState.getBlock() == desiredRail) {
            state = State.MoveToNext;
            return;
        }

        if (!ensureWorkAnchorInRange()) return;

        FindItemResult railItem = desiredRail == Blocks.POWERED_RAIL
            ? ensureHotbarItem(stack -> stack.getItem() == Items.POWERED_RAIL, ItemType.RAIL)
            : ensureHotbarItem(stack -> stack.getItem() == Items.RAIL, ItemType.RAIL);
        
        if (!railItem.found()) {
            error(desiredRail == Blocks.POWERED_RAIL ? "No powered rails found in inventory." : "No regular rails found in inventory.");
            toggle();
            return;
        }

        if (railState.isAir() || railState.isReplaceable()) {
            BlockUtils.place(railPos, railItem, rotate.get(), 50);
            return;
        }
        if (!ensureToolFor(railPos)) return;
        BlockUtils.breakBlock(railPos, true);
    }

    private void tickMoveToNext() {
        BlockPos playerPos = mc.player.getBlockPos();

        if (playerPos.equals(walkPos)) {
            stopBaritone();
            state = State.Advance;
            return;
        }

        if (!useBaritone.get()) return;

        if (lastGoal == null || !lastGoal.equals(walkPos)) {
            setBaritoneGoal(walkPos);
        }
    }

    private void tickAdvance() {
        step++;
        if (step >= length.get()) {
            stopBaritone();
            info("Finished.");
            state = State.Done;
            return;
        }

        updateStepPositions();
        fallingStableTicks = 0;
        
        state = initialState();
    }

    private boolean shouldPlaceLightAtStep() {
        return placeLight.get() && Math.floorMod(step, lightSpacing.get()) == 0;
    }

    private enum ToolType {
        PICKAXE,
        SHOVEL,
        AXE
    }

    private enum ItemType {
        PICKAXE,
        SHOVEL,
        AXE,
        RAIL,
        FLOOR_BLOCK,
        SHELL_BLOCK,
        LIQUID_BLOCK,
        POWER,
        LIGHT
    }

    private ToolType getPreferredToolType(BlockState state) {
        Block block = state.getBlock();

        if (shovelBlocks.get().contains(block)) return ToolType.SHOVEL;
        if (axeBlocks.get().contains(block)) return ToolType.AXE;
        if (pickaxeBlocks.get().contains(block)) return ToolType.PICKAXE;

        // Unknown block? Default to pickaxe.
        return ToolType.PICKAXE;
    }

    private boolean isToolValid(ItemStack stack) {
        if (!protectToolDurability.get()) return true;

        if (!stack.isDamageable()) return true;

        int remaining = stack.getMaxDamage() - stack.getDamage();
        return remaining > minToolDurability.get();
    }

    private boolean matchesToolType(ItemStack stack, ToolType type) {
        return switch (type) {
            case PICKAXE -> stack.isIn(net.minecraft.registry.tag.ItemTags.PICKAXES);
            case SHOVEL -> stack.isIn(net.minecraft.registry.tag.ItemTags.SHOVELS);
            case AXE -> stack.isIn(net.minecraft.registry.tag.ItemTags.AXES);
        };
    }

    private ItemType getToolItemType(ToolType type) {
        return switch (type) {
            case PICKAXE -> ItemType.PICKAXE;
            case SHOVEL -> ItemType.SHOVEL;
            case AXE -> ItemType.AXE;
        };
    }

    private int getPreferredHotbarSlot(ItemType type) {
        return switch (type) {
            case PICKAXE -> 2;
            case SHOVEL -> 1;
            case AXE -> 0;
            case RAIL -> 3;
            case FLOOR_BLOCK -> 4;
            case SHELL_BLOCK -> 5;
            case LIQUID_BLOCK -> 6;
            case POWER -> 7;
            case LIGHT -> 8;
        };
    }

    private FindItemResult getHotbarResult(int slot) {
        ItemStack stack = mc.player.getInventory().getStack(slot);
        return new FindItemResult(slot, stack.isEmpty() ? 0 : stack.getCount());
    }

    private FindItemResult findBestTool(ToolType type) {
        return InvUtils.find(stack -> {
            if (!matchesToolType(stack, type)) return false;
            return isToolValid(stack);
        });
    }

    private boolean ensureToolFor(BlockPos pos) {
        BlockState state = mc.world.getBlockState(pos);
        ToolType preferredType = getPreferredToolType(state);

        FindItemResult tool = findBestTool(preferredType);

        // If the preferred tool is missing, fall back to pickaxe.
        if (!tool.found() && preferredType != ToolType.PICKAXE) {
            tool = findBestTool(ToolType.PICKAXE);
        }

        if (!tool.found()) {
            error("No usable tool found for block: " + state.getBlock());
            toggle();
            return false;
        }

        ItemType itemType = getToolItemType(preferredType);

        // If we fell back to pickaxe, force it into the pickaxe slot.
        if (!matchesToolType(mc.player.getInventory().getStack(tool.slot()), preferredType)) {
            itemType = ItemType.PICKAXE;
        }

        int preferredSlot = getPreferredHotbarSlot(itemType);

        if (tool.slot() != preferredSlot) {
            InvUtils.move().from(tool.slot()).toHotbar(preferredSlot);
            InvUtils.dropHand();
        }

        InvUtils.swap(preferredSlot, false);
        return true;
    }


    private boolean isTorchLightBlock() {
        return lightBlock.get() == Blocks.TORCH;
    }

    private boolean isLanternLightBlock() {
        return lightBlock.get() == Blocks.LANTERN || lightBlock.get() == Blocks.SOUL_LANTERN;
    }

    private FindItemResult findLightBlock() {
        return ensureHotbarItem(
            stack -> stack.getItem() instanceof BlockItem bi && bi.getBlock() == lightBlock.get(),
            ItemType.LIGHT
        );
    }

    private boolean isDesiredLightBlock(BlockState state) {
        if (isTorchLightBlock()) {
            return state.getBlock() == Blocks.TORCH || state.getBlock() == Blocks.WALL_TORCH;
        }

        return state.getBlock() == lightBlock.get();
    }

    private int getLightLaneIndex() {
        return getRailLaneIndex() + lightLateralOffset.get();
    }

    private BlockPos getLightPos() {
        return getLaneBase(getLightLaneIndex()).up(lightVerticalOffset.get());
    }



    private boolean hasLanternSupport(BlockPos lightPos) {
        BlockState below = mc.world.getBlockState(lightPos.down());
        BlockState above = mc.world.getBlockState(lightPos.up());

        return isAcceptableStructuralBlock(below, lightPos.down()) || isAcceptableStructuralBlock(above, lightPos.up());
    }

    private void placeLantern(FindItemResult lantern, BlockPos lightPos) {
        BlockPos below = lightPos.down();
        BlockPos above = lightPos.up();

        InvUtils.swap(lantern.slot(), false);

        Runnable action;
        if (isAcceptableStructuralBlock(mc.world.getBlockState(above), above)) {
            action = () -> BlockUtils.interact(
                new BlockHitResult(Vec3d.ofCenter(above), Direction.DOWN, above, false),
                Hand.MAIN_HAND,
                true
            );
        } else {
            action = () -> BlockUtils.interact(
                new BlockHitResult(Vec3d.ofCenter(below), Direction.UP, below, false),
                Hand.MAIN_HAND,
                true
            );
        }

        if (rotate.get()) {
            Rotations.rotate(Rotations.getYaw(lightPos), Rotations.getPitch(lightPos), 50, action);
        } else {
            action.run();
        }
    }
    private boolean ensureWorkAnchorInRange() {
        BlockPos anchor = getWorkAnchor();

        if (mc.player.getEyePos().squaredDistanceTo(anchor.getX() + 0.5, anchor.getY() + 0.5, anchor.getZ() + 0.5) <= placeRange.get() * placeRange.get()) {
            return true;
        }

        if (!useBaritone.get()) {
            warning("Out of range for current work step.");
            return false;
        }

        if (lastGoal == null || !lastGoal.equals(anchor)) {
            setBaritoneGoal(anchor);
        }

        return false;
    }

    private boolean canPlaceDirectlyInto(BlockState state) {
        if (state.isAir() || state.isReplaceable()) return true;
        if (state.getFluidState().isEmpty()) return false;

        Block block = state.getBlock();

        // Only allow direct placement into actual fluid blocks.
        // Things like kelp, seagrass, bubble columns, and waterlogged blocks
        // should be broken first, not placed into directly.
        return block == Blocks.WATER || block == Blocks.LAVA;
    }

    private boolean isFluidOccupiedObstacle(BlockState state) {
        // Kelp, seagrass, bubble columns, waterlogged stairs/slabs/fences, etc.
        return !state.getFluidState().isEmpty() && !canPlaceDirectlyInto(state);
    }

    private BlockPos getWorkAnchor() {
        if (step == 0) return origin;
        return origin.offset(forward, step);
    }

    private int getWidthValue() {
        return Math.max(1, width.get());
    }

    private int getHeightValue() {
        return Math.max(2, height.get());
    }

    private int getRailLaneIndex() {
        return Math.max(0, Math.min(railOffset.get(), getWidthValue() - 1));
    }

    private boolean shouldUseShell() {
        return ensureShell.get();
    }

    private boolean shouldPlaceSecondFloorLayer() {
        if (secondFloorLayer.get()) return true;

        // Only auto-add a second floor row for the hidden torch setup at -2.
        return powerType.get() == PowerType.REDSTONE_TORCH && powerVerticalOffset.get() == -2;
    }

    private int getFloorLayerCount() {
        if (thirdFloorLayer.get()) return 3;
        if (shouldPlaceSecondFloorLayer()) return 2;
        return 1;
    }

    private FindItemResult findCornerBlock() {
        return ensureHotbarItem(
            stack -> stack.getItem() instanceof BlockItem bi && cornerBlocks.get().contains(bi.getBlock()),
            ItemType.SHELL_BLOCK
        );
    }
    private FindItemResult findLiquidBlock() {
        if (liquidUsesShellBlocks.get()) {
            return findShellBlock();
        }

        return ensureHotbarItem(
            stack -> stack.getItem() instanceof BlockItem bi && liquidBlocks.get().contains(bi.getBlock()),
            ItemType.LIQUID_BLOCK
        );
    }

    private int getEffectivePoweredLength() {
        return Math.max(1, Math.min(poweredLength.get(), powerSpacing.get()));
    }

    private int getCycleIndex() {
        return Math.floorMod(step, powerSpacing.get());
    }

    private boolean shouldUsePoweredRail() {
        if (railMode.get() == RailMode.ALL_POWERED) return true;
        return getCycleIndex() < getEffectivePoweredLength();
    }

    private boolean shouldPlacePowerAtStep() {
        if (railMode.get() == RailMode.ALL_POWERED) return getCycleIndex() == 0;
        int center = (getEffectivePoweredLength() - 1) / 2;
        return getCycleIndex() == center;
    }

    private Block getDesiredRailBlock() {
        return shouldUsePoweredRail() ? Blocks.POWERED_RAIL : Blocks.RAIL;
    }

    private BlockPos getLaneBase(int laneIndex) {
        return getLaneBase(0, laneIndex);
    }

    private BlockPos getLaneBase(int sliceOffset, int laneIndex) {
        return walkPos.offset(forward, sliceOffset).offset(crossDir, laneIndex);
    }

    private BlockPos getPowerPos() {
        return railPos.offset(crossDir, powerLateralOffset.get()).add(0, powerVerticalOffset.get(), 0);
    }

    private boolean isRailOrFutureRailPosition(BlockPos pos) {
        if (pos.equals(railPos)) return true;
        if (pos.equals(getLaneBase(-1, getRailLaneIndex()))) return true;
        if (pos.equals(getLaneBase(1, getRailLaneIndex()))) return true;

        return isAnyRail(mc.world.getBlockState(pos));
    }

    private List<BlockPos> getEncapsulationPositions() {
        List<BlockPos> positions = new ArrayList<>();
        BlockPos powerPos = powerType.get() == PowerType.REDSTONE_BLOCK ? getPowerPos() : getTorchPlacePos();

        for (Direction dir : Direction.values()) {
            BlockPos pos = powerPos.offset(dir);

            if (isRailOrFutureRailPosition(pos)) continue;
            positions.add(pos);
        }

        return positions;
    }

    private boolean isAboveRailTorchMode() {
    return powerType.get() == PowerType.REDSTONE_TORCH
        && powerVerticalOffset.get() == 1
        && powerLateralOffset.get() == 0;
    }

    private int getPreferredTorchWallLane() {
        // If the rail is on the left half, use the left wall.
        // If the rail is on the right half, use the right wall.
        return getRailLaneIndex() <= (getWidthValue() - 1) / 2 ? 0 : getWidthValue() - 1;
    }

    private BlockPos getTorchPlacePos() {
        if (!isAboveRailTorchMode()) return getPowerPos();

        // Place the torch in the top interior block adjacent to the chosen wall.
        return getLaneBase(getPreferredTorchWallLane()).up(1);
    }

    private BlockPos getTorchSupportPos() {
        if (!isAboveRailTorchMode()) return getTorchPlacePos().down();

        // Support block in the wall beside the torch, not underneath it.
        int wallOffset = getPreferredTorchWallLane() == 0 ? -1 : getWidthValue();
        return walkPos.offset(crossDir, wallOffset).up(1);
    }

    private Direction getTorchAttachSide() {
        if (!isAboveRailTorchMode()) return Direction.UP;

        // If we are using the left wall, the torch attaches to the side facing inward.
        // If we are using the right wall, same idea but opposite direction.
        return getPreferredTorchWallLane() == 0 ? crossDir : crossDir.getOpposite();
    }

    private void tickPlaceLight() {
        if (!shouldPlaceLightAtStep()) {
            state = encapsulatePower.get() ? State.EncapsulatePower : State.PlaceRail;
            return;
        }

        BlockPos lightPos = getLightPos();
        BlockState current = mc.world.getBlockState(lightPos);

        if (isDesiredLightBlock(current)) {
            state = encapsulatePower.get() ? State.EncapsulatePower : State.PlaceRail;
            return;
        }

        if (isLanternLightBlock()) {
            if (!hasLanternSupport(lightPos)) {
                error("Lantern at the chosen light position needs a floor or ceiling support.");
                toggle();
                return;
            }

            if (!ensureWorkAnchorInRange()) return;

            FindItemResult lantern = findLightBlock();
            if (!lantern.found()) {
                error("No light block found in inventory.");
                toggle();
                return;
            }

            if (isFluidOccupiedObstacle(current)) {
                if (!ensureToolFor(lightPos)) return;
                BlockUtils.breakBlock(lightPos, true);
                return;
            }

            if (canPlaceDirectlyInto(current)) {
                placeLantern(lantern, lightPos);
                return;
            }
            if (!ensureToolFor(lightPos)) return;
            BlockUtils.breakBlock(lightPos, true);
            return;
        }

        // All other light blocks, including torches, go exactly where configured.
        if (!ensureWorkAnchorInRange()) return;

        FindItemResult light = findLightBlock();
        if (!light.found()) {
            error("No light block found in inventory.");
            toggle();
            return;
        }

        if (isFluidOccupiedObstacle(current)) {
            if (!ensureToolFor(lightPos)) return;
            BlockUtils.breakBlock(lightPos, true);
            return;
        }

        if (canPlaceDirectlyInto(current)) {
            BlockUtils.place(lightPos, light, rotate.get(), 50);
            return;
        }
        if (!ensureToolFor(lightPos)) return;
        BlockUtils.breakBlock(lightPos, true);
    }
    private void placeTorch(FindItemResult torch, BlockPos torchPos, BlockPos supportPos) {
        // Special-case wall-mounted "above rail" torches so Minecraft can't choose the front wall.
        if (isAboveRailTorchMode()) {
            InvUtils.swap(torch.slot(), false);

            Runnable action = () -> BlockUtils.interact(
                new BlockHitResult(Vec3d.ofCenter(supportPos), getTorchAttachSide(), supportPos, false),
                Hand.MAIN_HAND,
                true
            );

            if (rotate.get()) {
                Rotations.rotate(Rotations.getYaw(supportPos), Rotations.getPitch(supportPos), 50, action);
            } else {
                action.run();
            }

            return;
        }

        // Normal torch placement path for non-wall special cases.
        BlockUtils.place(torchPos, torch, rotate.get(), 50);
    }

    private FindItemResult findFloorBlock() {
        return ensureHotbarItem(
            stack -> stack.getItem() instanceof BlockItem bi && floorBlocks.get().contains(bi.getBlock()),
            ItemType.FLOOR_BLOCK
        );
    }
    
    private FindItemResult findShellBlock() {
        return ensureHotbarItem(
            stack -> stack.getItem() instanceof BlockItem bi && shellBlocks.get().contains(bi.getBlock()),
            ItemType.SHELL_BLOCK
        );
    }

    private FindItemResult chooseSupportBlockFor(BlockPos pos) {
        if (isShellPosition(pos)) {
            FindItemResult shell = findShellBlock();
            if (shell.found()) return shell;
        }
        return findFloorBlock();
    }

    private FindItemResult ensureHotbarItem(Predicate<ItemStack> predicate, ItemType type) {
        int preferredSlot = getPreferredHotbarSlot(type);
        ItemStack current = mc.player.getInventory().getStack(preferredSlot);

        if (predicate.test(current)) return getHotbarResult(preferredSlot);

        FindItemResult inventory = InvUtils.find(predicate);
        if (!inventory.found()) return inventory;

        if (inventory.slot() != preferredSlot) {
            InvUtils.move().from(inventory.slot()).toHotbar(preferredSlot);
            InvUtils.dropHand();
        }

        return getHotbarResult(preferredSlot);
    }

    private boolean isAnyRail(BlockState state) {
        return state.getBlock() == Blocks.RAIL || state.getBlock() == Blocks.POWERED_RAIL;
    }

    private boolean isAnyRedstoneTorch(BlockState state) {
        return state.getBlock() == Blocks.REDSTONE_TORCH
            || state.getBlock() == Blocks.REDSTONE_WALL_TORCH;
    }

    private boolean isAcceptableStructuralBlock(BlockState state, BlockPos pos) {
        if (state.isAir()) return false;
        if (!state.getFluidState().isEmpty()) return false;
        return Block.isShapeFullCube(state.getCollisionShape(mc.world, pos));
    }

    private boolean isAcceptableShellBlock(BlockState state, BlockPos pos) {
        if (!isAcceptableStructuralBlock(state, pos)) return false;
        return !(state.getBlock() == Blocks.REDSTONE_BLOCK || state.getBlock() == Blocks.REDSTONE_TORCH);
    }

    private boolean isShellPosition(BlockPos pos) {
        for (int lateral = -1; lateral <= getWidthValue(); lateral++) {
            for (int vertical = -1; vertical <= getHeightValue(); vertical++) {
                boolean onSide = lateral == -1 || lateral == getWidthValue();
                boolean onRoof = vertical == getHeightValue();
                if (!onSide && !onRoof) continue;

                BlockPos shellPos = walkPos.offset(crossDir, lateral).add(0, vertical, 0);
                if (shellPos.equals(pos)) return true;
            }
        }

        return false;
    }

    private boolean hasLiquidAboveFloorInLookahead() {
        int baseY = railPos.getY();

        for (int y = 1; y <= getHeightValue() + 1; y++) {
            for (int lane = 0; lane < getWidthValue(); lane++) {
                BlockPos pos = getLaneBase(1, lane).up(y);
                if (!mc.world.getBlockState(pos).getFluidState().isEmpty()) return true;
            }
        }

        return false;
    }

    private List<BlockPos> getLookaheadFloorPositions() {
        List<BlockPos> positions = new ArrayList<>();

        for (int layer = 0; layer < getFloorLayerCount(); layer++) {
            for (int lane = 0; lane < getWidthValue(); lane++) {
                positions.add(getLaneBase(1, lane).down(layer));
            }
        }

        return positions;
    }

    private List<BlockPos> getLookaheadShellPositions() {
        List<BlockPos> positions = new ArrayList<>();

        // Side walls
        for (int y = 0; y < getHeightValue(); y++) {
            positions.add(getLaneBase(1, -1).up(y));
            positions.add(getLaneBase(1, getWidthValue()).up(y));
        }

        // Roof
        for (int lane = 0; lane < getWidthValue(); lane++) {
            positions.add(getLaneBase(1, lane).up(getHeightValue()));
        }

        // Roof corners
        if (placeCorners.get()) {
            positions.add(getLaneBase(1, -1).up(getHeightValue()));
            positions.add(getLaneBase(1, getWidthValue()).up(getHeightValue()));
        }

        return positions;
    }

    private List<BlockPos> getLookaheadSealPositions(boolean includeShell) {
        List<BlockPos> positions = new ArrayList<>();

        // Temporary tunnel fill in the second slice.
        for (int y = 0; y < getHeightValue(); y++) {
            for (int lane = 0; lane < getWidthValue(); lane++) {
                positions.add(getLaneBase(1, lane).up(y));
            }
        }

        // Permanent floor rows in the second slice.
        positions.addAll(getLookaheadFloorPositions());

        // If water is above floor level, also prebuild the shell in the second slice.
        if (includeShell) {
            positions.addAll(getLookaheadShellPositions());
        }

        return positions;
    }

    private FindItemResult chooseLookaheadSealBlock(BlockPos pos, List<BlockPos> lookaheadFloors, List<BlockPos> lookaheadShell) {
        if (lookaheadShell.contains(pos)) {
            if (isShellCornerPosition(pos)) {
                FindItemResult corner = findCornerBlock();
                if (corner.found()) return corner;
            }

            FindItemResult shell = findShellBlock();
            if (shell.found()) return shell;
        }

        if (lookaheadFloors.contains(pos)) {
            FindItemResult floor = findFloorBlock();
            if (floor.found()) return floor;
        }

        return findLiquidBlock();
    }

    private List<BlockPos> getInteriorClearPositions() {
        return getInteriorPositions(0, false);
    }

    private List<BlockPos> getInteriorPositions(int sliceOffset, boolean includeRailCellAtFloor) {
        List<BlockPos> positions = new ArrayList<>();

        for (int lane = 0; lane < getWidthValue(); lane++) {
            BlockPos laneBase = getLaneBase(sliceOffset, lane);
            boolean railLane = lane == getRailLaneIndex();

            for (int y = 0; y < getHeightValue(); y++) {
                if (!includeRailCellAtFloor && railLane && y == 0) continue;
                positions.add(laneBase.up(y));
            }
        }

        return positions;
    }

    private boolean isShellCorner(int lateral, int vertical) {
        boolean lateralEdge = lateral == -1 || lateral == getWidthValue();
        boolean verticalEdge = vertical == -1 || vertical == getHeightValue();
        return lateralEdge && verticalEdge;
    }

    private boolean isShellCornerPosition(BlockPos pos) {
        for (BlockPos corner : getCornerPositions(0)) {
            if (corner.equals(pos)) return true;
        }

        return false;
    }

    private List<BlockPos> getCornerPositions(int sliceOffset) {
        List<BlockPos> positions = new ArrayList<>();

        int[] laterals = new int[] { -1, getWidthValue() };
        int[] verticals = new int[] { -1, getHeightValue() };

        for (int lateral : laterals) {
            for (int vertical : verticals) {
                positions.add(walkPos.offset(forward, sliceOffset).offset(crossDir, lateral).add(0, vertical, 0));
            }
        }

        return positions;
    }

    private List<BlockPos> getShellPositions() {
        return getShellPositions(0);
    }

    private List<BlockPos> getShellPositions(int sliceOffset) {
        if (!shouldUseShell()) return new ArrayList<>();
        return getRawShellPositions(sliceOffset);
    }

    private List<BlockPos> getFloorPositions() {
        return getFloorPositions(0);
    }

    private List<BlockPos> getFloorPositions(int sliceOffset) {
        List<BlockPos> positions = new ArrayList<>();

        int layers = getFloorLayerCount();

        for (int y = 1; y <= layers; y++) {
            for (int lane = 0; lane < getWidthValue(); lane++) {
                positions.add(getLaneBase(sliceOffset, lane).down(y));
            }
        }

        return positions;
    }

    private boolean hasDangerousLiquidAhead() {
        // Slice +1 relative to current build slice.
        // Only liquids at or above floor level matter for pushing/flooding.
        for (BlockPos pos : getInteriorPositions(1, true)) {
            if (!mc.world.getBlockState(pos).getFluidState().isEmpty()) return true;
        }

        return false;
    }

    private List<BlockPos> getLookaheadLiquidSensitivePositions() {
        List<BlockPos> positions = new ArrayList<>();

        positions.addAll(getInteriorPositions(1, true));
        positions.addAll(getFloorPositions(1));

        if (shouldUseShell()) {
            positions.addAll(getShellPositions(1));
        }

        return positions;
    }

    private List<BlockPos> getLiquidSensitivePositions() {
        List<BlockPos> positions = new ArrayList<>();
        positions.addAll(getInteriorClearPositions());
        positions.addAll(getFloorPositions());

        if (powerType.get() == PowerType.REDSTONE_BLOCK) {
            positions.add(getPowerPos());

            if (getFloorLayerCount() > 1) {
                positions.add(getPowerPos().down());
            }
        } else {
            positions.add(getTorchPlacePos());
            positions.add(getTorchSupportPos());
        }

        if (shouldUseShell()) positions.addAll(getShellPositions());
        return positions;
    }



    private List<BlockPos> getLookaheadFallingBlockPositions() {
        List<BlockPos> positions = new ArrayList<>();

        for (int y = 0; y <= getHeightValue() + 3; y++) {
            for (int lane = 0; lane < getWidthValue(); lane++) {
                positions.add(getLaneBase(1, lane).up(y));
            }
        }

        return positions;
    }

    private boolean hasFallingBlockAhead() {
        for (BlockPos pos : getLookaheadFallingBlockPositions()) {
            if (isFallingBlock(mc.world.getBlockState(pos))) return true;
        }

        return false;
    }

    private boolean hasFallingBlockEntitiesAhead() {
        List<BlockPos> positions = getLookaheadFallingBlockPositions();

        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double minZ = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        double maxZ = Double.NEGATIVE_INFINITY;

        for (BlockPos pos : positions) {
            minX = Math.min(minX, pos.getX());
            minY = Math.min(minY, pos.getY());
            minZ = Math.min(minZ, pos.getZ());

            maxX = Math.max(maxX, pos.getX() + 1);
            maxY = Math.max(maxY, pos.getY() + 1);
            maxZ = Math.max(maxZ, pos.getZ() + 1);
        }

        Box box = new Box(minX, minY, minZ, maxX, maxY, maxZ);
        return !mc.world.getEntitiesByClass(FallingBlockEntity.class, box, entity -> true).isEmpty();
    }

    private List<BlockPos> getLookaheadSealPositions() {
        List<BlockPos> positions = new ArrayList<>();
        positions.addAll(getInteriorPositions(1, true));
        positions.addAll(getFloorPositions(1));
        if (shouldUseShell()) positions.addAll(getShellPositions(1));
        return positions;
    }

}

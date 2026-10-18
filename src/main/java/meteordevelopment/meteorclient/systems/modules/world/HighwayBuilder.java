/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.world;

import baritone.api.BaritoneAPI;
import baritone.api.pathing.goals.GoalBlock;
import baritone.api.process.IBaritoneProcess;
import baritone.api.process.PathingCommand;
import baritone.api.process.PathingCommandType;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixin.MinecraftMixin;
import meteordevelopment.meteorclient.mixin.ShulkerBoxMenuAccessor;
import meteordevelopment.meteorclient.mixininterface.IVec3;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.combat.KillAura;
import meteordevelopment.meteorclient.systems.modules.player.AutoEat;
import meteordevelopment.meteorclient.systems.modules.player.AutoGap;
import meteordevelopment.meteorclient.systems.modules.player.AutoTool;
import meteordevelopment.meteorclient.systems.modules.player.InstantRebreak;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.misc.HorizontalDirection;
import meteordevelopment.meteorclient.utils.misc.MBlockPos;
import meteordevelopment.meteorclient.utils.player.EChestMemory;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.meteorclient.utils.world.TickRate;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.ShulkerBoxScreen;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Range;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

@SuppressWarnings("ConstantConditions")
public class HighwayBuilder extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgDigging = settings.createGroup("Digging");
    private final SettingGroup sgPaving = settings.createGroup("Paving");
    private final SettingGroup sgInventory = settings.createGroup("Inventory");
    private final SettingGroup sgRenderDigging = settings.createGroup("Render Digging");
    private final SettingGroup sgRenderPaving = settings.createGroup("Render Paving");

    // General

    private final Setting<Integer> width = sgGeneral.add(new IntSetting.Builder()
        .name("width")
        .description("Width of the highway.")
        .defaultValue(4)
        .range(1, 9)
        .sliderRange(1, 9)
        .build()
    );

    private final Setting<Integer> height = sgGeneral.add(new IntSetting.Builder()
        .name("height")
        .description("Height of the highway.")
        .defaultValue(3)
        .range(2, 5)
        .sliderRange(2, 5)
        .build()
    );

    private final Setting<Boolean> onlyPlaceMissing = sgGeneral.add(new BoolSetting.Builder()
        .name("only-place-missing")
        .description("Not replace full blocks that are already present.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> railings = sgGeneral.add(new IntSetting.Builder()
        .name("railings")
        .description("Height of the railings next to the highway.")
        .defaultValue(1)
        .range(0, 4)
        .sliderRange(0, 4)
        .build()
    );

    private final Setting<Boolean> cornerBlock = sgGeneral.add(new BoolSetting.Builder()
        .name("corner-support-block")
        .description("Places a support block underneath the railings, to prevent air placing.")
        .defaultValue(true)
        .visible(() -> railings.get() > 0)
        .build()
    );

    private final Setting<Boolean> rotation = sgGeneral.add(new BoolSetting.Builder()
        .name("rotation")
        .description("Mode of rotation.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> disconnectOnToggle = sgGeneral.add(new BoolSetting.Builder()
        .name("disconnect-on-toggle")
        .description("Automatically disconnects when the module is turned off, for example for not having enough blocks.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> pauseOnLag = sgGeneral.add(new BoolSetting.Builder()
        .name("pause-on-lag")
        .description("Pauses the current process while the server stops responding.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> destroyCrystalTraps = sgGeneral.add(new BoolSetting.Builder()
        .name("destroy-crystal-traps")
        .description("Use a bow to defuse crystal traps safely from a distance. An infinity bow is recommended.")
        .defaultValue(true)
        .build()
    );

    // Digging

    private final Setting<Boolean> doubleMine = sgDigging.add(new BoolSetting.Builder()
        .name("double-mine")
        .description("Whether to double mine blocks when applicable (normal mine and packet mine simultaneously).")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> dontBreakTools = sgDigging.add(new BoolSetting.Builder()
        .name("dont-break-tools")
        .description("Don't break tools.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Integer> breakDurability = sgDigging.add(new IntSetting.Builder()
        .name("durability-percentage")
        .description("The durability percentage at which to stop using a tool.")
        .defaultValue(2)
        .range(1, 100)
        .sliderRange(1, 100)
        .visible(dontBreakTools::get)
        .build()
    );

    private final Setting<Integer> savePickaxes = sgDigging.add(new IntSetting.Builder()
        .name("save-pickaxes")
        .description("How many pickaxes to ensure are saved. Hitting this number in your inventory will trigger a restock or the module toggling off.")
        .defaultValue(1)
        .range(0, 36)
        .sliderRange(0, 36)
        .visible(() -> !dontBreakTools.get())
        .build()
    );

    private final Setting<Integer> breakDelay = sgDigging.add(new IntSetting.Builder()
        .name("break-delay")
        .description("The delay between breaking blocks.")
        .defaultValue(1)
        .range(1, 5)
        .sliderRange(1, 5)
        .build()
    );

    private final Setting<Integer> blocksPerTick = sgDigging.add(new IntSetting.Builder()
        .name("blocks-per-tick")
        .description("The maximum amount of blocks that can be mined in a tick. Only applies to blocks instantly breakable.")
        .defaultValue(1)
        .range(1, 100)
        .sliderRange(1, 25)
        .build()
    );

    // Paving

    public final Setting<List<Block>> blocksToPlace = sgPaving.add(new BlockListSetting.Builder()
        .name("blocks-to-place")
        .description("Blocks it is allowed to place.")
        .defaultValue(Blocks.OBSIDIAN)
        .filter(block -> Block.isShapeFullBlock(block.defaultBlockState().getCollisionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO)))
        .build()
    );

    private final Setting<Double> placeRange = sgPaving.add(new DoubleSetting.Builder()
        .name("place-range")
        .description("The maximum distance at which you can place blocks.")
        .defaultValue(4.5)
        .sliderMax(5.5)
        .build()
    );

    private final Setting<Integer> placeDelay = sgPaving.add(new IntSetting.Builder()
        .name("place-delay")
        .description("The delay between placing blocks.")
        .defaultValue(1)
        .range(1, 4)
        .sliderRange(1, 4)
        .build()
    );

    private final Setting<Integer> placementsPerTick = sgPaving.add(new IntSetting.Builder()
        .name("placements-per-tick")
        .description("The maximum amount of blocks that can be placed in a tick.")
        .defaultValue(1)
        .min(1)
        .build()
    );

    // Inventory

    private final Setting<List<Item>> trashItems = sgInventory.add(new ItemListSetting.Builder()
        .name("trash-items")
        .description("Items that are considered trash and can be thrown out.")
        .defaultValue(
            Items.NETHERRACK, Items.QUARTZ, Items.GOLD_NUGGET, Items.GOLDEN_SWORD, Items.GLOWSTONE_DUST,
            Items.GLOWSTONE, Items.BLACKSTONE, Items.BASALT, Items.GHAST_TEAR, Items.SOUL_SAND, Items.SOUL_SOIL,
            Items.ROTTEN_FLESH, Items.MAGMA_BLOCK
        )
        .build()
    );

    private final Setting<Integer> inventoryDelay = sgInventory.add(new IntSetting.Builder()
        .name("inventory-delay")
        .description("Delay in ticks on inventory interactions.")
        .defaultValue(3)
        .min(0)
        .build()
    );

    private final Setting<Boolean> ejectUselessShulkers = sgInventory.add(new BoolSetting.Builder()
        .name("eject-useless-shulkers")
        .description("Whether you should eject useless shulkers.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> searchEnderChest = sgInventory.add(new BoolSetting.Builder()
        .name("search-ender-chest")
        .description("Searches your ender chest to find items to use. Be careful with this one, especially if you let it search through shulkers.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> searchShulkers = sgInventory.add(new BoolSetting.Builder()
        .name("search-shulkers")
        .description("Searches through shulkers to find items to use.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> minEmpty = sgInventory.add(new IntSetting.Builder()
        .name("minimum-empty-slots")
        .description("The minimum amount of empty slots you want left after mining obsidian.")
        .defaultValue(3)
        .sliderRange(0, 9)
        .min(0)
        .build()
    );

    private final Setting<Boolean> mineEnderChests = sgInventory.add(new BoolSetting.Builder()
        .name("mine-ender-chests")
        .description("Mines ender chests for obsidian.")
        .defaultValue(true)
        .build()
    );

    public final Setting<Integer> saveEchests = sgInventory.add(new IntSetting.Builder()
        .name("save-ender-chests")
        .description("How many ender chests to ensure are saved. Hitting this number in your inventory will trigger a restock or the module toggling off.")
        .defaultValue(2)
        .range(0, 64)
        .sliderRange(0, 64)
        .visible(mineEnderChests::get)
        .build()
    );

    private final Setting<Boolean> rebreakEchests = sgInventory.add(new BoolSetting.Builder()
        .name("instantly-rebreak-echests")
        .description("Whether or not to use the instant rebreak exploit to break echests.")
        .defaultValue(false)
        .visible(mineEnderChests::get)
        .build()
    );

    private final Setting<Integer> rebreakTimer = sgInventory.add(new IntSetting.Builder()
        .name("rebreak-delay")
        .description("Delay between rebreak attempts.")
        .defaultValue(0)
        .sliderMax(20)
        .visible(() -> mineEnderChests.get() && rebreakEchests.get())
        .build()
    );

    // Render Digging

    private final Setting<Boolean> renderMine = sgRenderDigging.add(new BoolSetting.Builder()
        .name("render-blocks-to-mine")
        .description("Render blocks to be mined.")
        .defaultValue(true)
        .build()
    );

    private final Setting<ShapeMode> renderMineShape = sgRenderDigging.add(new EnumSetting.Builder<ShapeMode>()
        .name("blocks-to-mine-shape-mode")
        .description("How the blocks to be mined are rendered.")
        .defaultValue(ShapeMode.Both)
        .build()
    );

    private final Setting<SettingColor> renderMineSideColor = sgRenderDigging.add(new ColorSetting.Builder()
        .name("blocks-to-mine-side-color")
        .description("Color of blocks to be mined.")
        .defaultValue(new SettingColor(225, 25, 25, 25))
        .build()
    );

    private final Setting<SettingColor> renderMineLineColor = sgRenderDigging.add(new ColorSetting.Builder()
        .name("blocks-to-mine-line-color")
        .description("Color of blocks to be mined.")
        .defaultValue(new SettingColor(225, 25, 25))
        .build()
    );

    // Render Paving

    private final Setting<Boolean> renderPlace = sgRenderPaving.add(new BoolSetting.Builder()
        .name("render-blocks-to-place")
        .description("Render blocks to be placed.")
        .defaultValue(true)
        .build()
    );

    private final Setting<ShapeMode> renderPlaceShape = sgRenderPaving.add(new EnumSetting.Builder<ShapeMode>()
        .name("blocks-to-place-shape-mode")
        .description("How the blocks to be placed are rendered.")
        .defaultValue(ShapeMode.Both)
        .build()
    );

    private final Setting<SettingColor> renderPlaceSideColor = sgRenderPaving.add(new ColorSetting.Builder()
        .name("blocks-to-place-side-color")
        .description("Color of blocks to be placed.")
        .defaultValue(new SettingColor(25, 25, 225, 25))
        .build()
    );

    private final Setting<SettingColor> renderPlaceLineColor = sgRenderPaving.add(new ColorSetting.Builder()
        .name("blocks-to-place-line-color")
        .description("Color of blocks to be placed.")
        .defaultValue(new SettingColor(25, 25, 225))
        .build()
    );

    private HorizontalDirection dir;
    private HorizontalDirection leftDir;
    private HorizontalDirection rightDir;

    private final MBlockPos start = new MBlockPos();
    private final MBlockPos currentPos = new MBlockPos();
    private final MBlockPos movePos = new MBlockPos();
    private final MBlockPos lastBreakingPos = new MBlockPos();
    private final MBlockPos normalMining = new MBlockPos();
    private final MBlockPos packetMining = new MBlockPos();

    public boolean drawingBow;

    private int blocksBroken;
    private int blocksPlaced;
    private int placeTimer;
    private int breakTimer;
    private int count;

    private final RestockTask restockTask = new RestockTask(this);

    private boolean suspended = true, inventory = true;
    private int containerId;
    private final Set<EndCrystal> ignoreCrystals = new ReferenceOpenHashSet<>();

    private boolean btSettingAllowBreak;
    private boolean btSettingAllowInventory;
    private boolean btSettingAllowPlace;
    private boolean btSettingRenderGoal;

    private State state;

    private final ArrayDeque<MBlockPos> breakQueue = new ArrayDeque<>();
    private final ArrayDeque<MBlockPos> placeQueue = new ArrayDeque<>();
    private final ArrayDeque<MBlockPos> breakQueueCurrent = new ArrayDeque<>();
    private final ArrayDeque<MBlockPos> placeQueueCurrent = new ArrayDeque<>();

    public HighwayBuilder() {
        super(Categories.World, "highway-builder", "Automatically builds highways.");
        runInMainMenu = true;
    }

    /* todo
        - separate digging and paving more effectively
        - separate walking forwards from the current state to speed up actions
        - scan one block behind you to ensure the highway is still valid
        - do something about god damn lava flowing in
     */

    @Override
    public void onActivate() {
        if (!Utils.canUpdate()) return;

        updateVariables();

        dir = HorizontalDirection.get(mc.player.getYRot());
        leftDir = dir.diagonal ? dir.rotateLeft() : dir.rotateLeftSkipOne();
        rightDir = leftDir.opposite();

        start.set(playerPos());
        currentPos.set(start);
        movePos.set(start);
        lastBreakingPos.set(0, 0, 0);
        normalMining.set(0, 0, 0);
        packetMining.set(0, 0, 0);

        blocksBroken = 0;
        blocksPlaced = 0;
        placeTimer = 0;
        breakTimer = 0;
        count = 0;

        btSettingAllowBreak = BaritoneAPI.getSettings().allowBreak.value;
        btSettingAllowInventory = BaritoneAPI.getSettings().allowInventory.value;
        btSettingAllowPlace = BaritoneAPI.getSettings().allowPlace.value;
        btSettingRenderGoal = BaritoneAPI.getSettings().renderGoal.value;

        BaritoneAPI.getSettings().renderGoal.value = false;

        BaritoneAPI.getProvider().getPrimaryBaritone().getPathingControlManager().registerProcess(new btProcess());

        restockTask.complete();

        if (Modules.get().get(InstantRebreak.class).isActive()) warning("It's recommended to disable the Instant Rebreak module and instead use the 'instantly-rebreak-echests' setting to avoid errors.");

        state = State.Wait;

        breakQueue.clear();
        placeQueue.clear();
        breakQueueCurrent.clear();
        placeQueueCurrent.clear();
    }

    @Override
    public void onDeactivate() {
        if (!Utils.canUpdate()) return;

        BaritoneAPI.getSettings().allowBreak.value = btSettingAllowBreak;
        BaritoneAPI.getSettings().allowInventory.value = btSettingAllowInventory;
        BaritoneAPI.getSettings().allowPlace.value = btSettingAllowPlace;
        BaritoneAPI.getSettings().renderGoal.value = btSettingRenderGoal;

        info("Distance: (highlight)%.0f", distance(start, currentPos));
        info("Blocks broken: (highlight)%d", blocksBroken);
        info("Blocks placed: (highlight)%d", blocksPlaced);
    }

    @Override
    public void error(String message, Object... args) {
        super.error(message, args);
        if (Modules.get().get(HighwayBuilder.class).isActive())
            toggle();

        if (disconnectOnToggle.get())
            disconnect(message, args);
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (!Utils.canUpdate()) return;

        if (dir == null) onActivate();

        if (Modules.get().get(AutoEat.class).eating || Modules.get().get(AutoGap.class).isEating() || Modules.get().get(KillAura.class).attacking)
            return;

        if (suspended) {
            if (inventory && Utils.canUpdate()) {
                updateVariables();
                suspended = false;
            } else return;
        }

        if (state == State.Collect || state == State.ReLevel) {
            BaritoneAPI.getSettings().allowBreak.value = true;
            BaritoneAPI.getSettings().allowInventory.value = true;
            BaritoneAPI.getSettings().allowPlace.value = true;
        } else {
            BaritoneAPI.getSettings().allowBreak.value = false;
            BaritoneAPI.getSettings().allowInventory.value = false;
            BaritoneAPI.getSettings().allowPlace.value = false;
        }

        if (state != State.Collect)
            movePos.set(currentPos);

        // don't let the current state keep ticking, switch to re-levelling straight away
        if (state != State.Collect && state != State.ReLevel) {
            if (mc.player.getY() < start.y - 0.5)
                setState(State.ReLevel);
        }

        if (pauseOnLag.get() && TickRate.INSTANCE.getTimeSinceLastTick() >= 1.5f)
            return;

        count = 0;

        if (mc.player.getY() < start.y - 0.5)
            setState(State.ReLevel); // don't let the current state keep ticking, switch to re-levelling straight away

        refreshTasks();
        if (destroyCrystalTraps.get() && isCrystalTrap())
            setState(State.DefuseCrystalTraps);
        state.tick(this);

        if (breakTimer > 0) breakTimer--;
        if (placeTimer > 0) placeTimer--;
        count = 0;

        if (state != State.Wait)
            return;

        if (breakQueue.iterator().hasNext()) {
            if (breakTimer > 0)
                return;
            breakTimer = breakDelay.get();
            boolean packetMined = false;
            while (count < blocksPerTick.get() && breakQueue.iterator().hasNext()) {
                MBlockPos pos = findClosest(breakQueue);
                breakQueueCurrent.add(pos);
                if (packetMining.equals(pos)) {
                    packetMined = true;
                    continue;
                }
                count++;
                BlockPos mcPos = pos.getBlockPos();
                int slot = state.findAndMoveBestToolToHotbar(this, mc.level.getBlockState(mcPos), false);
                if (slot == -1) return;
                InvUtils.swap(slot, false);
                if (!BlockUtils.canInstaBreak(mcPos))
                    count = blocksPerTick.get();
                if (!BlockUtils.canInstaBreak(mcPos) && doubleMine.get() && !normalMining.equals(pos) && !packetMined) {
                    packetMining.set(pos);
                    mc.gameMode.startPrediction(mc.level, sequence -> new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, mcPos, BlockUtils.getDirection(mcPos), sequence));
                    mc.gameMode.startPrediction(mc.level, sequence -> new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, mcPos, BlockUtils.getDirection(mcPos), sequence));
                    break;
                } else {
                    normalMining.set(pos);
                    if (rotation.get() && count == blocksPerTick.get())
                        Rotations.rotate(Rotations.getYaw(mcPos), Rotations.getPitch(mcPos), () -> BlockUtils.breakBlock(mcPos, true));
                    else
                        BlockUtils.breakBlock(mcPos, true);
                }
            }
        } else if (placeQueue.iterator().hasNext()) {
            if (placeTimer > 0)
                return;
            placeTimer = placeDelay.get();
            while (count < placementsPerTick.get() && placeQueue.iterator().hasNext()) {
                count++;
                MBlockPos pos = findClosest(placeQueue);
                placeQueueCurrent.add(pos);
                int slot = state.findBlocksToPlace(this);
                if (slot == -1) return;
                BlockUtils.place(pos.getBlockPos(), InteractionHand.MAIN_HAND, slot, rotation.get() && count == placementsPerTick.get(), 0, true, true, false);
            }
        }
    }

    @EventHandler
    private void onPacket(PacketEvent.Receive event) {
        if (event.packet instanceof ClientboundContainerSetContentPacket p) {
            if (p.containerId() == 0 && suspended)
                inventory = true;
            else
                this.containerId = p.containerId();
        }
    }

    @EventHandler
    private void onGameLeave(GameLeftEvent event) {
        updateVariables();
    }

    @EventHandler
    private void onRender3D(Render3DEvent event) {
        if (!Utils.canUpdate()) return;

        if (renderMine.get()) {
            for (MBlockPos pos : breakQueue)
                event.renderer.box(pos.getBlockPos(), renderMineSideColor.get(), renderMineLineColor.get(), renderMineShape.get(), 0);

            for (MBlockPos pos : breakQueueCurrent)
                event.renderer.box(pos.getBlockPos(), new Color(255, 255, 25, 25), new Color(255, 255, 25), renderMineShape.get(), 0);
        }

        if (renderPlace.get()) {
            for (MBlockPos pos : placeQueue)
                event.renderer.box(pos.getBlockPos(), renderPlaceSideColor.get(), renderPlaceLineColor.get(), renderPlaceShape.get(), 0);

            for (MBlockPos pos : placeQueueCurrent)
                event.renderer.box(pos.getBlockPos(), new Color(25, 255, 25, 25), new Color(25, 255, 25), renderPlaceShape.get(), 0);
        }
    }

    private void updateVariables() {
        placeTimer = breakTimer = count = containerId = 0;
        ignoreCrystals.clear();
    }

    private void setState(State state) {
        this.state = state;
        state.start(this);
    }

    private void disconnect(String message, Object... args) {
        MutableComponent text = Component.literal(String.format("%s[%s%s%s] %s", ChatFormatting.GRAY, ChatFormatting.BLUE, title, ChatFormatting.GRAY, ChatFormatting.RED) + String.format(message, args)).append("\n");
        text.append(getStatsText());

        mc.getConnection().getConnection().disconnect(text);
    }

    public MutableComponent getStatsText() {
        MutableComponent text = Component.literal(String.format("%sDistance: %s%.0f\n", ChatFormatting.GRAY, ChatFormatting.WHITE, distance(start, currentPos)));
        text.append(String.format("%sBlocks broken: %s%d\n", ChatFormatting.GRAY, ChatFormatting.WHITE, blocksBroken));
        text.append(String.format("%sBlocks placed: %s%d", ChatFormatting.GRAY, ChatFormatting.WHITE, blocksPlaced));

        return text;
    }

    private MBlockPos playerPos() {
        int x = mc.player.getBlockX();
        int y = mc.player.getBlockY();
        int z = mc.player.getBlockZ();
        return new MBlockPos().set(x, y, z);
    }

    private double distance(MBlockPos a, MBlockPos b) {
        int xDiff = a.x - b.x;
        int yDiff = a.y - b.y;
        int zDiff = a.z - b.z;
        return Math.sqrt(xDiff * xDiff + yDiff * yDiff + zDiff * zDiff);
    }

    private void updatePosition() {
        MBlockPos next = new MBlockPos().set(currentPos).offset(dir);

        if (distance(next, playerPos()) < 3) {
            currentPos.set(next);
            setState(State.Wait);
        }
    }

    private class btProcess implements IBaritoneProcess {
        @Override
        public boolean isActive() {
            return true;
        }

        @Override
        public boolean isTemporary() {
            return true;
        }

        @Override
        public PathingCommand onTick(boolean b1, boolean b2) {
            if (Modules.get().get(HighwayBuilder.class).isActive()) {
                BlockPos goal = new BlockPos(movePos.x, movePos.y, movePos.z);
                return new PathingCommand(new GoalBlock(goal), PathingCommandType.SET_GOAL_AND_PATH);
            } else {
                return new PathingCommand(null, PathingCommandType.DEFER);
            }
        }

        @Override
        public void onLostControl() {}

        @Override
        public String displayName0() {
            return "HighwayTools";
        }

        @Override
        public double priority() {
            return 2.0;
        }
    }

    private int checkBlock(MBlockPos block, boolean place) {
        if (Math.sqrt(block.getBlockPos().distToCenterSqr(mc.player.getEyePosition())) > placeRange.get())
            return 0;

        if (place) {
            if (BlockUtils.canBreak(block.getBlockPos())) {
                if (onlyPlaceMissing.get()) {
                    if (!block.getState().isCollisionShapeFullBlock(mc.level, block.getBlockPos()))
                        breakQueue.add(new MBlockPos().set(block));
                } else if (!blocksToPlace.get().contains(block.getState().getBlock())) {
                    breakQueue.add(new MBlockPos().set(block));
                }
            } else if (block.getState().isAir()) {
                placeQueue.add(new MBlockPos().set(block));
            }
        } else {
            if (BlockUtils.canBreak(block.getBlockPos()))
                breakQueue.add(new MBlockPos().set(block));
        }

        return 1;
    }

    private void refreshTasks() {
        breakQueue.clear();
        placeQueue.clear();
        for (MBlockPos pos : breakQueueCurrent)
            if (pos.getState().isAir())
                blocksBroken++;
        breakQueueCurrent.clear();
        for (MBlockPos pos : placeQueueCurrent)
            if (!pos.getState().isAir())
                blocksPlaced++;
        placeQueueCurrent.clear();

        if (packetMining.getState().isAir())
            packetMining.set(0, 0, 0);

        if (normalMining.getState().isAir())
            normalMining.set(0, 0, 0);

        MBlockPos origin = new MBlockPos().set(currentPos).add(0, -1, 0).offset(dir);
        int addedBlocks = 1;
        int direction = 0;
        while (addedBlocks > 0) {
            addedBlocks = 0;
            int heightCounter = 0;
            while (heightCounter <= height.get()) {
                int widthCounter = width.get();
                if (railings.get() > 0 && (heightCounter > 0 || cornerBlock.get()))
                    widthCounter += 2;

                MBlockPos pos = new MBlockPos().set(origin).add(0, heightCounter, 0).offset(leftDir, widthCounter / 2);

                if (heightCounter == 0) {
                    while (widthCounter > 0) {
                        addedBlocks += checkBlock(pos, true);
                        pos.offset(rightDir);
                        widthCounter--;
                    }

                    heightCounter++;
                    continue;
                }

                if (railings.get() >= heightCounter) {
                    addedBlocks += checkBlock(pos, true);
                    widthCounter--;
                    while (widthCounter > 1) {
                        pos.offset(rightDir);
                        addedBlocks += checkBlock(pos, false);
                        widthCounter--;
                    }
                    pos.offset(rightDir);
                    addedBlocks += checkBlock(pos, true);
                    heightCounter++;
                    continue;
                }

                addedBlocks += checkBlock(pos, false);
                widthCounter--;

                while (widthCounter > 0) {
                    pos.offset(rightDir);
                    addedBlocks += checkBlock(pos, false);
                    widthCounter--;
                }

                heightCounter++;
            }

            if (addedBlocks == 0 && direction == 0) {
                direction = 1;
                addedBlocks = 1;
                origin.set(currentPos).add(0, -1, 0).offset(dir, 2);
                if (!breakQueue.iterator().hasNext() && !placeQueue.iterator().hasNext())
                    updatePosition();
                continue;
            }

            if (direction == 0)
                origin.offset(dir, -1);
            else
                origin.offset(dir);
        }
    }

    private MBlockPos findClosest(ArrayDeque<MBlockPos> set) {
        MBlockPos pos = set.getFirst();
        double distance = pos.getBlockPos().distToCenterSqr(mc.player.getEyePosition());
        for (MBlockPos current : set) {
            double currentDistance = current.getBlockPos().distToCenterSqr(mc.player.getEyePosition());
            if (currentDistance < distance) {
                pos = current;
                distance = currentDistance;
            }
        }

        set.remove(pos);
        return pos;
    }

    private boolean isCrystalTrap() {
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof EndCrystal endCrystal)) continue;
            if (PlayerUtils.isWithin(endCrystal, 12) || !PlayerUtils.isWithin(endCrystal, 24)) continue;
            if (ignoreCrystals.contains(endCrystal)) continue;

            Vec3 vec1 = new Vec3(0, 0, 0);
            Vec3 vec2 = new Vec3(0, 0, 0);

            // todo add a better raytrace check
            ((IVec3) vec1).meteor$set(mc.player.getX(), mc.player.getY() + mc.player.getEyeHeight(), mc.player.getZ());
            ((IVec3) vec2).meteor$set(entity.getX(), entity.getY() + 0.5, entity.getZ());
            return mc.level.clip(new ClipContext(vec1, vec2, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player)).getType() == HitResult.Type.MISS;
        }

        return false;
    }

    private enum State {
        ReLevel {
            @Override
            protected void start(HighwayBuilder b) {
                b.currentPos.add(-b.dir.offsetX * 4, 0, -b.dir.offsetZ * 4);
            }

            @Override
            protected void tick(HighwayBuilder b) {
                if (b.distance(b.movePos, b.playerPos()) <= 1) {
                    b.setState(Wait);
                    return;
                }

                BlockPos bp1 = b.mc.player.blockPosition().offset(0, -1, 0);
                BlockPos bp2 = b.mc.player.blockPosition().offset(0, -2, 0);

                BlockState blockState1 = b.mc.level.getBlockState(bp1);
                BlockState blockState2 = b.mc.level.getBlockState(bp2);

                if (!blockState1.isAir() || !blockState2.isAir())
                    return;

                int slot = findBlocksToPlacePrioritizeTrash(b);
                if (slot == -1)
                    return;

                BlockUtils.place(bp2, InteractionHand.MAIN_HAND, slot, b.rotation.get(), 100, true, true, false);
            }
        },

        Collect {
            private final MBlockPos pos = new MBlockPos();
            private int timer;

            @Override
            protected void start(HighwayBuilder b) {
                pos.set(b.mc.player);
                timer = 100;
            }

            @Override
            protected void tick(HighwayBuilder b) {
                MBlockPos itemPos = new MBlockPos();
                boolean itemFound = false;

                Rotations.rotate(b.dir.opposite().yaw, 0);

                for (Entity entity : b.mc.level.getEntities(b.mc.player, new AABB(pos.x - 5, pos.y - 2, pos.z - 5, pos.x + 5, pos.y + 2, pos.z + 5))) {
                    if (!(entity instanceof ItemEntity itemEntity))
                        return;

                    if (itemEntity.getItem().getItem() == Items.OBSIDIAN || Utils.isShulker(itemEntity.getItem().getItem())) {
                        int x = itemEntity.getBlockX();
                        int y = itemEntity.getBlockY();
                        int z = itemEntity.getBlockZ();
                        itemPos.set(x, y, z);
                        itemFound = true;
                        break;
                    }
                }

                if (b.movePos.x == itemPos.x && b.movePos.z == itemPos.z)
                    timer--;
                else
                    timer = 100;

                if (itemFound) {
                    b.movePos.set(itemPos);
                } else {
                    b.movePos.set(b.currentPos);
                    b.setState(Wait);
                    return;
                }

                if (!b.mc.player.containerMenu.getCarried().isEmpty()) {
                    InvUtils.dropHand();
                    return;
                }

                for (int i = 0; i < b.mc.player.getInventory().getNonEquipmentItems().size(); i++) {
                    ItemStack itemStack = b.mc.player.getInventory().getItem(i);

                    if (b.trashItems.get().contains(itemStack.getItem())) {
                        InvUtils.drop().slot(i);
                        return;
                    }

                    if (b.ejectUselessShulkers.get() && Utils.isShulker(itemStack.getItem())) {
                        boolean eject = true;
                        ItemStack[] items = new ItemStack[27];
                        Utils.getItemsInContainerItem(itemStack, items);
                        for (ItemStack stack : items) {
                            if (!b.trashItems.get().contains(stack.getItem()) && !(stack.getItem() == Items.AIR)) {
                                eject = false;
                                break;
                            }
                        }
                        if (eject) {
                            InvUtils.drop().slot(i);
                            return;
                        }
                    }
                }

                if (timer == 0) {
                    b.movePos.set(b.currentPos);
                    b.setState(Wait);
                }
            }
        },

        MineEnderChests {
            private int counter;
            private int rebreakTimer;
            private int timeout;
            private boolean first;
            private boolean primed;

            @Override
            protected void start(HighwayBuilder b) {
                int availableSlots = 0;
                for (int i = 0; i < b.mc.player.getInventory().getNonEquipmentItems().size(); i++) {
                    ItemStack itemStack = b.mc.player.getInventory().getItem(i);
                    if (itemStack.isEmpty() ||
                        b.trashItems.get().contains(itemStack.getItem()))
                        availableSlots++;
                }

                if (availableSlots == 0) {
                    b.error("No empty slots.");
                    return;
                }

                counter = Math.max(availableSlots - b.minEmpty.get(), 1) * 8;

                first = true;
                primed = false;
            }

            @Override
            protected void tick(HighwayBuilder b) {
                if (b.distance(b.currentPos, b.playerPos()) >= 1)
                    return;

                BlockPos bp = b.currentPos.getBlockPos().offset(-b.dir.offsetX * 2, 0, -b.dir.offsetZ * 2);
                BlockState blockState = b.mc.level.getBlockState(bp);

                if (blockState.getBlock() == Blocks.ENDER_CHEST) {
                    if (b.mc.gui.screen() instanceof ContainerScreen screen) {
                        // wait for the screen to be properly loaded
                        if (screen.getMenu().containerId != b.containerId) return;

                        b.mc.gui.screen().onClose();
                    }

                    // if we don't know what's in your echest, open it quickly while we have one available to check
                    if (!EChestMemory.isKnown()) {
                        if (b.rotation.get()) Rotations.rotate(Rotations.getYaw(bp), Rotations.getPitch(bp), () ->
                            b.mc.gameMode.useItemOn(b.mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(bp), Direction.UP, bp, false)));
                        else
                            b.mc.gameMode.useItemOn(b.mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(bp), Direction.UP, bp, false));

                        return;
                    }

                    // Mine ender chest
                    int slot = findAndMoveBestToolToHotbar(b, blockState, true);
                    if (slot == -1) {
                        b.error("Cannot find pickaxe without silk touch to mine ender chests.");
                        return;
                    }

                    InvUtils.swap(slot, false);

                    if (b.rebreakEchests.get() && primed) {
                        timeout++;
                        if (timeout > 60) {
                            first = true;
                            primed = false;
                            timeout = 0;
                            return;
                        }

                        if (rebreakTimer > 0) {
                            rebreakTimer--;
                            return;
                        }

                        rebreakTimer = b.rebreakTimer.get();

                        if (b.rotation.get()) {
                            Rotations.rotate(Rotations.getYaw(bp), Rotations.getPitch(bp), () ->
                                b.mc.gameMode.startPrediction(b.mc.level, sequence ->
                                    new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, bp, BlockUtils.getDirection(bp), sequence)
                                )
                            );
                        } else b.mc.gameMode.startPrediction(b.mc.level, sequence ->
                            new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, bp, BlockUtils.getDirection(bp), sequence)
                        );
                    } else {
                        if (b.rotation.get())
                            Rotations.rotate(Rotations.getYaw(bp), Rotations.getPitch(bp), () -> BlockUtils.breakBlock(bp, true));
                        else BlockUtils.breakBlock(bp, true);
                    }
                } else {
                    // Place ender chest
                    int slot = findAndMoveToHotbar(b, itemStack -> itemStack.getItem() == Items.ENDER_CHEST);
                    if (slot == -1 || countItem(b, stack -> stack.getItem().equals(Items.ENDER_CHEST)) <= b.saveEchests.get() || counter == 0) {
                        b.setState(Collect);
                        return;
                    }
                    counter--;

                    if (countItem(b, stack -> stack.is(ItemTags.PICKAXES)) <= b.savePickaxes.get()) {
                        if (b.searchEnderChest.get() || b.searchShulkers.get()) {
                            b.restockTask.setPickaxes();
                        }
                    }

                    if (!first)
                        primed = true;
                    else
                        first = false;

                    BlockUtils.place(bp, InteractionHand.MAIN_HAND, slot, b.rotation.get(), 0, true, true, false);
                    timeout = 0;
                }
            }
        },

        // this one was rough to do
        Restock {
            private static final MBlockPos pos = new MBlockPos();
            private static final ItemStack[] ITEMS = new ItemStack[27];
            private int minimumSlots,stopTimer,delayTimer;
            private boolean breakContainer,indicateStopping;
            private Predicate<ItemStack> shulkerPredicate;

            // if this is ever not -1 when we expect it to be, things break a lot
            private int slot = -1;

            @Override
            protected void start(HighwayBuilder b) {
                slot = -1; // :ptsd:

                // set the predicate to test for shulker boxes
                if (shulkerPredicate == null) setShulkerPredicate(b);

                if (b.restockTask.tasksInactive()) {
                    b.setState(Wait);
                    return;
                }

                // firstly search your inventory for shulkers that have the items you need
                if (b.searchShulkers.get()) {
                    slot = findAndMoveToHotbar(b, shulkerPredicate);
                }

                // next search your ender chest for raw items and shulkers containing items
                if (slot == -1 && b.searchEnderChest.get() && countItem(b, stack -> stack.getItem().equals(Items.ENDER_CHEST)) > 0) {
                    // todo handle pulling ecs from shulker boxes so we can search through them

                    boolean stop = EChestMemory.isKnown();
                    if (EChestMemory.isKnown()) {
                        for (ItemStack stack : EChestMemory.ITEMS) {
                            if (b.restockTask.materials && stack.getItem() instanceof BlockItem bi) {
                                if (b.blocksToPlace.get().contains(bi.getBlock()) || (b.blocksToPlace.get().contains(Blocks.OBSIDIAN) && bi == Items.ENDER_CHEST)) {
                                    stop = false;
                                    break;
                                }
                            }
                            if (b.restockTask.pickaxes && stack.is(ItemTags.PICKAXES)) {
                                stop = false;
                                break;
                            }
                            if (b.restockTask.food && Utils.isFood(stack) && !Modules.get().get(AutoEat.class).blacklist.get().contains(stack.getItem())) {
                                stop = false;
                                break;
                            }

                            if (b.searchShulkers.get() && shulkerPredicate.test(stack)) {
                                stop = false;
                                break;
                            }
                        }
                    }

                    if (!stop) slot = findAndMoveToHotbar(b, itemStack -> itemStack.getItem() == Items.ENDER_CHEST);
                }

                // by this point we have searched shulkers and your ender chest, and no more items could be found to pull from
                if (slot == -1) {
                    boolean restockOccurred = (
                        (b.restockTask.materials && (hasItem(b, stack -> stack.getItem() instanceof BlockItem bi && b.blocksToPlace.get().contains(bi.getBlock())) || b.blocksToPlace.get().contains(Blocks.OBSIDIAN) && countItem(b, itemStack -> itemStack.getItem() == Items.ENDER_CHEST) > b.saveEchests.get())) ||
                            (b.restockTask.pickaxes && countItem(b, itemStack -> itemStack.is(ItemTags.PICKAXES)) > b.savePickaxes.get()) ||
                            (b.restockTask.food && hasItem(b, itemStack -> Utils.isFood(itemStack) && !Modules.get().get(AutoEat.class).blacklist.get().contains(itemStack.getItem())))
                    );

                    if (restockOccurred) {
                        b.setState(Collect);
                    } else b.error("Unable to perform restock for '" + b.restockTask.item() + "'.");

                    return;
                }

                int restockSlots = -b.minEmpty.get();
                for (int i = 0; i < b.mc.player.getInventory().getNonEquipmentItems().size(); i++) {
                    if (b.mc.player.getInventory().getItem(i).isEmpty()) restockSlots++;
                }

                if (restockSlots <= 0) {
                    b.error("No empty slots for restocking items.");
                    return;
                }

                // todo when we add a digging only mode, make pickaxes fill all empty slots
                minimumSlots = b.restockTask.materials ? restockSlots : 1;

                HorizontalDirection dir = b.dir.diagonal ? b.dir.rotateLeft().rotateLeftSkipOne() : b.dir.opposite();
                pos.set(b.mc.player).offset(dir);

                // Quick fix for a specific issue - if your pickaxe breaks while mining echests, it will start a new
                // task to restock pickaxes. However, there will be an echest placed down in the same position specified
                // above, and if you have the search echest setting enabled it will assume it needs to pull items from
                // your echest, even if you have a shulker full of pickaxes in your inventory.
                breakContainer = b.mc.level.getBlockState(pos.getBlockPos()).getBlock() == Blocks.ENDER_CHEST;

                indicateStopping = false;
                delayTimer = b.inventoryDelay.get();
            }

            @Override
            protected void tick(HighwayBuilder b) {
                // this should only tick if there's a valid slot we can restock from
                if (slot == -1) {
                    b.error("Invalid restocking action.");
                    return;
                }

                if (indicateStopping && !breakContainer) {
                    if (stopTimer > 0)
                        stopTimer--;
                    else
                        b.setState(Collect);

                    return;
                }

                // prevent tasks executing when they shouldn't
                if (b.restockTask.tasksInactive()) {
                    b.setState(Wait);
                    return;
                }

                if (delayTimer > 0) {
                    delayTimer--;
                    return;
                }

                // calculate the amount of materials we have already pulled
                int slotsPulled = 0;
                if (b.restockTask.materials) {
                    slotsPulled += countSlots(b, itemStack -> itemStack.getItem() instanceof BlockItem bi && b.blocksToPlace.get().contains(bi.getBlock()));
                    if (b.blocksToPlace.get().contains(Blocks.OBSIDIAN))
                        slotsPulled += ((countItem(b, itemStack -> itemStack.getItem() == Items.ENDER_CHEST) - b.saveEchests.get()) * 8) / 64;
                }
                if (b.restockTask.pickaxes)
                    slotsPulled += countSlots(b, itemStack -> itemStack.is(ItemTags.PICKAXES)) - b.savePickaxes.get();
                if (b.restockTask.food)
                    slotsPulled += countSlots(b, itemStack -> Utils.isFood(itemStack) && !Modules.get().get(AutoEat.class).blacklist.get().contains(itemStack.getItem()));


                // whether we have pulled the minimum amount of items we want
                if (slotsPulled >= minimumSlots && !indicateStopping) {
                    indicateStopping = true;
                    breakContainer = true;
                    stopTimer = 12;
                    if (b.mc.gui.screen() != null) b.mc.gui.screen().onClose();
                    return;
                }

                // Check block state
                BlockPos blockPos = pos.getBlockPos();
                BlockState blockState = b.mc.level.getBlockState(blockPos);

                switch (blockState.getBlock()) {
                    // if we have placed a shulker box there should be items inside we want
                    case ShulkerBoxBlock _ -> {
                        if (b.mc.gui.screen() instanceof ShulkerBoxScreen screen) {
                            // wait for the screen to be properly loaded
                            if (screen.getMenu().containerId != b.containerId) return;

                            Container inv = ((ShulkerBoxMenuAccessor) screen.getMenu()).meteor$getContainer();

                            if (restockItems(b, inv)) {
                                delayTimer = b.inventoryDelay.get();
                                return;
                            }

                            // we have taken everything we can from the shulker box, and since slotsPulled >= minimumSlots is false, we should keep going
                            // close the screen, break the shulker box, look for more containers to loot from
                            b.mc.gui.screen().onClose();
                            breakContainer = true;
                        } else {
                            if (!b.searchShulkers.get()) breakContainer = true;
                            handleContainerBlock(b, blockPos);
                        }
                    }

                    // we are either pulling items themselves, or shulkers containing items from your ec
                    case EnderChestBlock _ -> {
                        if (b.mc.gui.screen() instanceof ContainerScreen screen) {
                            // wait for the screen to be properly loaded
                            if (screen.getMenu().containerId != b.containerId) return;

                            Container inv = screen.getMenu().getContainer();

                            if (restockItems(b, inv)) {
                                delayTimer = b.inventoryDelay.get();
                                return;
                            }

                            // we may have taken items themselves from the ec, but still need more. Now we try to find a shulker containing the items
                            if (b.searchShulkers.get()) {
                                int moveTo = InvUtils.findEmpty().slot();

                                if (moveTo != -1) {
                                    for (int i = 0; i < inv.getContainerSize(); i++) {
                                        if (shulkerPredicate.test(inv.getItem(i))) {
                                            InvUtils.move().fromId(i).to(moveTo);
                                            delayTimer = b.inventoryDelay.get();
                                            break;
                                        }
                                    }
                                }
                            }

                            // if it reaches here, we have taken everything we can from your ender chest, and may have also grabbed a shulker
                            // we should be finished in your ender chest, so we can break it and either continue on our way or start checking shulkers
                            b.mc.gui.screen().onClose();
                            breakContainer = true;
                        } else {
                            if (!b.searchEnderChest.get()) breakContainer = true;
                            handleContainerBlock(b, blockPos);
                        }
                    }

                    // handling when there is no container there
                    case AirBlock _ -> {
                        // indicates we have just broken a container
                        if (breakContainer) {
                            breakContainer = false;

                            // if we don't signal intent to stop, we loop back to the start and continue restocking
                            if (indicateStopping) b.restockTask.complete();
                            else start(b);

                            return;
                        }

                        BlockUtils.place(blockPos, InteractionHand.MAIN_HAND, slot, b.rotation.get(), 0, true, true, false);
                    }

                    // the only valid blocks should be air, a shulker box, or an ender chest
                    // if there is another type of block, assume something has gone wrong and error out (e.g. lava flowed in)
                    default -> b.error("Invalid block at container restocking position?");
                }
            }

            private boolean restockItems(HighwayBuilder b, Container inv) {
                if (b.restockTask.materials) {
                    // take raw material
                    if (grabFromInventory(inv, itemStack -> itemStack.getItem() instanceof BlockItem bi && b.blocksToPlace.get().contains(bi.getBlock())))
                        return true;

                    // prefer taking raw material before echests
                    if (b.blocksToPlace.get().contains(Blocks.OBSIDIAN)) {
                        if (grabFromInventory(inv, itemStack -> itemStack.getItem() == Items.ENDER_CHEST)) return true;
                    }
                }
                if (b.restockTask.pickaxes) {
                    if (grabFromInventory(inv, itemStack -> itemStack.is(ItemTags.PICKAXES))) return true;
                }
                if (b.restockTask.food) {
                    return grabFromInventory(inv, itemStack -> Utils.isFood(itemStack) && !Modules.get().get(AutoEat.class).blacklist.get().contains(itemStack.getItem()));
                }

                return false;
            }

            // scans the inventory, takes out the first item that matches the predicate and returns
            private boolean grabFromInventory(Container inv, Predicate<ItemStack> filterItem) {
                for (int i = 0; i < inv.getContainerSize(); i++) {
                    if (filterItem.test(inv.getItem(i))) {
                        InvUtils.shiftClick().slotId(i);
                        return true;
                    }
                }

                return false;
            }

            private void setShulkerPredicate(HighwayBuilder b) {
                shulkerPredicate = itemStack -> {
                    if (!Utils.isShulker(itemStack.getItem())) return false;
                    Utils.getItemsInContainerItem(itemStack, ITEMS);

                    for (ItemStack stack : ITEMS) {
                        if (b.restockTask.materials && stack.getItem() instanceof BlockItem bi) {
                            if (b.blocksToPlace.get().contains(bi.getBlock()) || (b.blocksToPlace.get().contains(Blocks.OBSIDIAN) && bi == Items.ENDER_CHEST))
                                return true;
                        }
                        if (b.restockTask.pickaxes && stack.is(ItemTags.PICKAXES)) return true;
                        if (b.restockTask.food && Utils.isFood(stack) && !Modules.get().get(AutoEat.class).blacklist.get().contains(stack.getItem()))
                            return true;
                    }

                    return false;
                };
            }

            private void handleContainerBlock(HighwayBuilder b, BlockPos bp) {
                if (breakContainer) {
                    BlockState state = b.mc.level.getBlockState(bp);

                    int toolSlot = findAndMoveBestToolToHotbar(b, state, false);
                    InvUtils.swap(toolSlot, false);

                    if (b.rotation.get())
                        Rotations.rotate(Rotations.getYaw(bp), Rotations.getPitch(bp), () -> BlockUtils.breakBlock(bp, true));
                    else BlockUtils.breakBlock(bp, true);
                } else {
                    if (b.rotation.get()) {
                        Rotations.rotate(Rotations.getYaw(bp), Rotations.getPitch(bp), () ->
                            b.mc.gameMode.useItemOn(b.mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(bp), Direction.UP, bp, false))
                        );
                    } else
                        b.mc.gameMode.useItemOn(b.mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(bp), Direction.UP, bp, false));

                    delayTimer = b.inventoryDelay.get();
                }
            }

            private int countSlots(HighwayBuilder b, Predicate<ItemStack> predicate) {
                int count = 0;
                for (int i = 0; i < b.mc.player.getInventory().getNonEquipmentItems().size(); i++) {
                    ItemStack stack = b.mc.player.getInventory().getItem(i);
                    if (predicate.test(stack)) count++;
                }

                return count;
            }
        },

        DefuseCrystalTraps {
            private int cooldown,shots;
            private EndCrystal target;

            @Override
            protected void start(HighwayBuilder b) {
                if (!InvUtils.find(Items.BOW).found() || (!InvUtils.find(itemStack -> itemStack.getItem() instanceof ArrowItem).found() && !b.mc.player.getAbilities().instabuild)) {
                    b.destroyCrystalTraps.set(false);
                    b.warning("No bow found to destroy crystal traps with. Toggling the setting off.");
                    b.setState(Wait);
                }

                shots = cooldown = 0;
                target = null;
            }

            /**
             * Need to perform the linked injection to ensure that vanilla code does not interfere with us drawing our
             * bow. The {@link net.minecraft.client.Minecraft#handleKeybinds} method is only called when you are not in a screen,
             * meaning we cannot draw our bow using {@link net.minecraft.client.Options#keyUse} since it would not work if you are in a
             * screen. Similarly, drawing our bow by {@link net.minecraft.client.multiplayer.MultiPlayerGameMode#useItem} would get
             * cancelled by default within the handleKeybinds method if you do not have the use key held down,
             * essentially meaning without the following injection it would not work if you don't have a screen open.
             *
             * @see MinecraftMixin#wrapStopUsing(MultiPlayerGameMode, Player)
             */
            @Override
            protected void tick(HighwayBuilder b) {
                if (cooldown > 0) {
                    cooldown--;
                    return;
                }

                if (!InvUtils.testInMainHand(Items.BOW)) {
                    int slot = findAndMoveToHotbar(b, itemStack -> itemStack.getItem() instanceof BowItem);
                    if (slot == -1) {
                        b.destroyCrystalTraps.set(false);
                        b.warning("No bow found to destroy crystal traps with. Toggling the setting off.");
                        b.setState(Wait);
                        b.mc.gameMode.releaseUsingItem(b.mc.player);
                        b.drawingBow = false;
                        return;
                    }

                    InvUtils.swap(slot, false);
                }

                EndCrystal potentialTarget = (EndCrystal) TargetUtils.get(entity -> {
                    if (!(entity instanceof EndCrystal endCrystal)) return false;
                    if (PlayerUtils.isWithin(endCrystal, 12) || !PlayerUtils.isWithin(endCrystal, 24)) return false;
                    if (b.ignoreCrystals.contains(endCrystal)) return false;

                    Vec3 vec1 = new Vec3(0, 0, 0);
                    Vec3 vec2 = new Vec3(0, 0, 0);

                    ((IVec3) vec1).meteor$set(b.mc.player.getX(), b.mc.player.getY() + b.mc.player.getEyeHeight(), b.mc.player.getZ());
                    ((IVec3) vec2).meteor$set(entity.getX(), entity.getY() + 0.5, entity.getZ());
                    return b.mc.level.clip(new ClipContext(vec1, vec2, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, b.mc.player)).getType() == HitResult.Type.MISS;
                }, SortPriority.LowestDistance);

                if (target == null || target.isRemoved()) {
                    if (potentialTarget == null) {
                        b.setState(Wait);
                        b.mc.gameMode.releaseUsingItem(b.mc.player);
                        b.drawingBow = false;
                        return;
                    } else {
                        target = potentialTarget;
                        shots = 0;
                    }
                }

                if (shots >= 3) {
                    b.ignoreCrystals.add(target);
                    b.warning("Detected potential hangup on a crystal. Adding it to ignore list and continuing forward.");
                    b.setState(Wait);
                    b.mc.gameMode.releaseUsingItem(b.mc.player);
                    b.drawingBow = false;
                    return;
                }

                b.mc.player.setYRot((float) Rotations.getYaw(target));

                float pitch = aim(b, target);
                if (Float.isNaN(pitch)) b.mc.player.setXRot((float) Rotations.getPitch(target));
                else b.mc.player.setXRot(pitch);

                if (BowItem.getPowerForTime(b.mc.player.getTicksUsingItem() - 3) >= 1.0f) {
                    b.mc.gameMode.releaseUsingItem(b.mc.player);
                    b.drawingBow = false;
                    cooldown = 20;
                    shots++;
                } else {
                    b.drawingBow = true;
                    b.mc.gameMode.useItem(b.mc.player, InteractionHand.MAIN_HAND);
                }
            }

            private float aim(HighwayBuilder b, Entity target) {
                // Velocity based on bow charge.
                float velocity = BowItem.getPowerForTime(b.mc.player.getTicksUsingItem());

                // Positions
                Vec3 pos = target.position();

                double relativeX = pos.x - b.mc.player.getX();
                double relativeY = pos.y + 0.5 - b.mc.player.getEyeY(); // aiming a little bit above the bottom of the crystal, hopefully prevents shooting the floor or failing the raytrace check
                double relativeZ = pos.z - b.mc.player.getZ();

                // Calculate the pitch
                double hDistance = Math.sqrt(relativeX * relativeX + relativeZ * relativeZ);
                double hDistanceSq = hDistance * hDistance;
                float g = 0.006f;
                float velocitySq = velocity * velocity;

                return (float) -Math.toDegrees(Math.atan((velocitySq - Math.sqrt(velocitySq * velocitySq - g * (g * hDistanceSq + 2 * relativeY * velocitySq))) / (g * hDistance)));
            }
        },

        Wait {
            @Override
            protected void tick(HighwayBuilder b) {}
        };

        protected void start(HighwayBuilder b) {
        }

        protected abstract void tick(HighwayBuilder b);

        private int findSlot(HighwayBuilder b, Predicate<ItemStack> predicate, boolean hotbar) {
            for (int i = hotbar ? 0 : 9; i < (hotbar ? 9 : b.mc.player.getInventory().getNonEquipmentItems().size()); i++) {
                if (predicate.test(b.mc.player.getInventory().getItem(i))) return i;
            }

            return -1;
        }

        protected int findHotbarSlot(HighwayBuilder b, boolean replaceTools) {
            int thrashSlot = -1;
            int slotsWithBlocks = 0;
            int slotWithLeastBlocks = -1;
            int slotWithLeastBlocksCount = Integer.MAX_VALUE;

            // Loop hotbar
            for (int i = 0; i < 9; i++) {
                ItemStack itemStack = b.mc.player.getInventory().getItem(i);

                // Return if the slot is empty
                if (itemStack.isEmpty()) return i;

                // Return if the slot contains a tool and replacing tools is enabled
                if (replaceTools && AutoTool.isTool(itemStack)) return i;

                // Store the slot if it contains thrash
                if (b.trashItems.get().contains(itemStack.getItem())) thrashSlot = i;

                // Update tracked stats about slots that contain building blocks
                if (itemStack.getItem() instanceof BlockItem blockItem && (b.blocksToPlace.get().contains(blockItem.getBlock()) || b.blocksToPlace.get().contains(Blocks.OBSIDIAN) && blockItem == Items.ENDER_CHEST)) {
                    slotsWithBlocks++;

                    if (itemStack.getCount() < slotWithLeastBlocksCount) {
                        slotWithLeastBlocksCount = itemStack.getCount();
                        slotWithLeastBlocks = i;
                    }
                }
            }

            // Return thrash slot if found
            if (thrashSlot != -1) return thrashSlot;

            // If there are more than 1 slots with building blocks return the slot with the lowest amount of blocks
            if (slotsWithBlocks > 0) return slotWithLeastBlocks;

            // No space found in hotbar
            b.error("No empty space in hotbar.");
            return -1;
        }

        protected boolean hasItem(HighwayBuilder b, Predicate<ItemStack> predicate) {
            for (int i = 0; i < b.mc.player.getInventory().getNonEquipmentItems().size(); i++) {
                if (predicate.test(b.mc.player.getInventory().getItem(i))) return true;
            }

            return false;
        }

        protected int countItem(HighwayBuilder b, Predicate<ItemStack> predicate) {
            int count = 0;
            for (int i = 0; i < b.mc.player.getInventory().getNonEquipmentItems().size(); i++) {
                ItemStack stack = b.mc.player.getInventory().getItem(i);
                if (predicate.test(stack)) count += stack.getCount();
            }

            return count;
        }

        protected int findAndMoveToHotbar(HighwayBuilder b, Predicate<ItemStack> predicate) {
            // Check hotbar
            int slot = findSlot(b, predicate, true);
            if (slot != -1) return slot;

            // Find hotbar slot to move to
            int hotbarSlot = findHotbarSlot(b, false);
            if (hotbarSlot == -1) return -1;

            // Check inventory
            slot = findSlot(b, predicate, false);

            // Return if no items were found
            if (slot == -1) return -1;

            // Move items from inventory to hotbar
            InvUtils.move().from(slot).toHotbar(hotbarSlot);
            InvUtils.dropHand();

            return hotbarSlot;
        }

        protected int findAndMoveBestToolToHotbar(HighwayBuilder b, BlockState blockState, boolean noSilkTouch) {
            // Find best tool
            double bestScore = -1;
            int bestSlot = -1;

            for (int i = 0; i < b.mc.player.getInventory().getNonEquipmentItems().size(); i++) {
                double score = AutoTool.getScore(b.mc.player.getInventory().getItem(i), blockState, false, false, AutoTool.EnchantPreference.None, itemStack -> {
                    if (noSilkTouch && Utils.hasEnchantment(itemStack, Enchantments.SILK_TOUCH)) return false;
                    return !b.dontBreakTools.get() || itemStack.getMaxDamage() - itemStack.getDamageValue() > (itemStack.getMaxDamage() * (b.breakDurability.get() / 100));
                });

                if (score > bestScore) {
                    bestScore = score;
                    bestSlot = i;
                }
            }

            if (bestSlot == -1) return b.mc.player.getInventory().getSelectedSlot();

            ItemStack bestStack = b.mc.player.getInventory().getItem(bestSlot);
            if (bestStack.is(ItemTags.PICKAXES)) {
                int count = countItem(b, stack -> stack.is(ItemTags.PICKAXES));

                // If we are in the process of restocking pickaxes and happen to need one, we should allow using it
                // as long as it has enough durability, since we will obtain more shortly thereafter
                if (count <= b.savePickaxes.get() && !(b.restockTask.pickaxes && bestStack.getMaxDamage() - bestStack.getDamageValue() > (bestStack.getMaxDamage() * (b.breakDurability.get() / 100)))) {
                    if (!b.restockTask.pickaxes && (b.searchEnderChest.get() || b.searchShulkers.get())) {
                        b.restockTask.setPickaxes();
                    } else {
                        b.error("Found less than the minimum amount of pickaxes required: " + count + "/" + (b.savePickaxes.get() + 1));
                    }

                    return -1;
                }
            }

            // Check if the tool is already in hotbar
            if (bestSlot < 9) return bestSlot;

            // Find hotbar slot to move to
            int hotbarSlot = findHotbarSlot(b, true);
            if (hotbarSlot == -1) return -1;

            // Move tool from inventory to hotbar
            InvUtils.move().from(bestSlot).toHotbar(hotbarSlot);
            InvUtils.dropHand();

            return hotbarSlot;
        }

        protected int findBlocksToPlace(HighwayBuilder b) {
            // find a block and move it to your hotbar
            int slot = findAndMoveToHotbar(b, itemStack -> itemStack.getItem() instanceof BlockItem blockItem && b.blocksToPlace.get().contains(blockItem.getBlock()));

            if (slot == -1) {
                if (b.mineEnderChests.get() && b.blocksToPlace.get().contains(Blocks.OBSIDIAN) && countItem(b, stack -> stack.getItem().equals(Items.ENDER_CHEST)) > b.saveEchests.get()) {
                    // can grind echests for obsidian
                    b.setState(MineEnderChests);
                } else if (b.searchEnderChest.get() || b.searchShulkers.get()) {
                    // start restocking if we're allowed
                    b.restockTask.setMaterials();
                } else {
                    b.error("Out of blocks to place.");
                }

                return -1;
            }

            return slot;
        }

        protected int findBlocksToPlacePrioritizeTrash(HighwayBuilder b) {
            int slot = findAndMoveToHotbar(b, itemStack -> {
                if (!(itemStack.getItem() instanceof BlockItem)) return false;
                return b.trashItems.get().contains(itemStack.getItem());
            });

            return slot != -1 ? slot : findBlocksToPlace(b);
        }
    }

    private class RestockTask {
        public boolean materials;
        public boolean pickaxes;
        public boolean food;
        private final HighwayBuilder b;

        public RestockTask(HighwayBuilder b) {
            this.b = b;
        }

        public void setMaterials() {
            setTask(0);
        }

        public void setPickaxes() {
            setTask(1);
        }

        public void setFood() {
            setTask(2);
        }

        private void setTask(@Range(from = 0, to = 2) int value) {
            complete();

            switch (value) {
                case 0 -> materials = true;
                case 1 -> pickaxes = true;
                case 2 -> food = true;
            }

            setState(State.Restock);
            b.info("Starting new restock task for " + item());
        }

        public void complete() {
            materials = false;
            pickaxes = false;
            food = false;
        }

        public boolean tasksInactive() {
            return !materials && !pickaxes && !food;
        }

        public String item() {
            if (materials) return "building materials";
            if (pickaxes) return "pickaxes";
            if (food) return "food";
            return "unknown";
        }
    }
}

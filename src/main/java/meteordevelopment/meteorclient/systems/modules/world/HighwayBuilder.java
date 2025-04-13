/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.world;

import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixin.ShulkerBoxScreenHandlerAccessor;
import meteordevelopment.meteorclient.mixininterface.IVec3d;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.combat.KillAura;
import meteordevelopment.meteorclient.systems.modules.player.*;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.misc.HorizontalDirection;
import meteordevelopment.meteorclient.utils.misc.MBlockPos;
import meteordevelopment.meteorclient.utils.player.*;
import meteordevelopment.meteorclient.utils.render.NametagUtils;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.meteorclient.utils.world.Dir;
import meteordevelopment.meteorclient.utils.world.TickRate;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.ingame.ShulkerBoxScreen;
import net.minecraft.client.input.Input;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.option.GameOptions;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.*;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.s2c.play.InventoryS2CPacket;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.EmptyBlockView;
import net.minecraft.world.RaycastContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;
import org.joml.Vector3d;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;

@SuppressWarnings("ConstantConditions")
public class HighwayBuilder extends Module {
    public enum Floor {
        Replace,
        PlaceMissing
    }

    public enum Rotation {
        None(false, false),
        Mine(true, false),
        Place(false, true),
        Both(true, true);

        public final boolean mine, place;

        Rotation(boolean mine, boolean place) {
            this.mine = mine;
            this.place = place;
        }
    }

    public enum BlockadeType {
        Full(6),
        Partial(4),
        Shulker(3);

        public final int columns;

        BlockadeType(int columns) {
            this.columns = columns;
        }
    }

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
        .range(1, 5)
        .sliderRange(1, 5)
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

    private final Setting<Floor> floor = sgGeneral.add(new EnumSetting.Builder<Floor>()
        .name("floor")
        .description("What floor placement mode to use.")
        .defaultValue(Floor.Replace)
        .build()
    );

    private final Setting<Boolean> railings = sgGeneral.add(new BoolSetting.Builder()
        .name("railings")
        .description("Builds railings next to the highway.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> cornerBlock = sgGeneral.add(new BoolSetting.Builder()
        .name("corner-support-block")
        .description("Places a support block underneath the railings, to prevent air placing.")
        .defaultValue(true)
        .visible(railings::get)
        .build()
    );

    private final Setting<Boolean> mineAboveRailings = sgGeneral.add(new BoolSetting.Builder()
        .name("mine-above-railings")
        .description("Mines blocks above railings.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Rotation> rotation = sgGeneral.add(new EnumSetting.Builder<Rotation>()
        .name("rotation")
        .description("Mode of rotation.")
        .defaultValue(Rotation.Both)
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

    private final Setting<Boolean> fastBreak = sgDigging.add(new BoolSetting.Builder()
        .name("fast-break")
        .description("Whether to finish breaking blocks faster than normal while double mining.")
        .defaultValue(true)
        .visible(doubleMine::get)
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
        .defaultValue(0)
        .min(0)
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
        .filter(block -> Block.isShapeFullCube(block.getDefaultState().getCollisionShape(EmptyBlockView.INSTANCE, BlockPos.ORIGIN)))
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
        .defaultValue(0)
        .min(0)
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
        .description("Whether you should eject useless shulkers. Warning - will throw out any shulkers that don't contain blocks to place, pickaxes, or food. Be careful with your kits.")
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

    private final Setting<BlockadeType> blockadeType = sgInventory.add(new EnumSetting.Builder<BlockadeType>()
        .name("echest-blockade-type")
        .description("What blockade type to use (the structure placed when mining echests).")
        .defaultValue(BlockadeType.Full)
        .visible(mineEnderChests::get)
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

    private HorizontalDirection dir, leftDir, rightDir;

    private Input prevInput;
    private CustomPlayerInput input;

    private State state, lastState;
    private IBlockPosProvider blockPosProvider;

    public Vec3d start;
    public int blocksBroken, blocksPlaced;
    private final MBlockPos lastBreakingPos = new MBlockPos();
    private boolean displayInfo;
    private boolean suspended = true, inventory = true;
    private int placeTimer, breakTimer, count, syncId;
    private final RestockTask restockTask = new RestockTask(this);
    private final ArrayList<EndCrystalEntity> ignoreCrystals = new ArrayList<>();
    public boolean drawingBow;
    public DoubleMineBlock normalMining, packetMining;

    private final MBlockPos posRender2 = new MBlockPos();
    private final MBlockPos posRender3 = new MBlockPos();

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

        dir = HorizontalDirection.get(mc.player.getYaw());
        leftDir = dir.rotateLeftSkipOne();
        rightDir = leftDir.opposite();

        blockPosProvider = dir.diagonal ? new DiagonalBlockPosProvider() : new StraightBlockPosProvider();
        state = State.Forward;
        setState(State.Center);
        lastBreakingPos.set(0, 0, 0);

        start = mc.player.getPos();
        blocksBroken = blocksPlaced = 0;
        displayInfo = true;
        suspended = false;

        restockTask.complete();

        if (blocksPerTick.get() > 1 && rotation.get().mine) warning("With rotations enabled, you can break at most 1 block per tick.");
        if (placementsPerTick.get() > 1 && rotation.get().place) warning("With rotations enabled, you can place at most 1 block per tick.");

        if (Modules.get().get(InstantRebreak.class).isActive()) warning("It's recommended to disable the Instant Rebreak module and instead use the 'instantly-rebreak-echests' setting to avoid errors.");
    }

    @Override
    public void onDeactivate() {
        if (!Utils.canUpdate()) return;

        mc.player.input = prevInput;
        mc.player.setYaw(dir.yaw);
        mc.options.useKey.setPressed(false);

        if (displayInfo) {
            info("Distance: (highlight)%.0f", PlayerUtils.distanceTo(start));
            info("Blocks broken: (highlight)%d", blocksBroken);
            info("Blocks placed: (highlight)%d", blocksPlaced);
        }
    }

    @Override
    public void error(String message, Object... args) {
        super.error(message, args);
        toggle();

        if (disconnectOnToggle.get()) {
            disconnect(message, args);
        }
    }

    private void errorEarly(String message, Object... args) {
        super.error(message, args);

        displayInfo = false;
        toggle();
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (dir == null) {
            onActivate();
            return;
        }

        if (suspended) {
            if (inventory && Utils.canUpdate()) {
                updateVariables();
                suspended = false;
            }
            else return;
        }

        if (width.get() < 3 && dir.diagonal) {
            errorEarly("Diagonal highways with width less than 3 are not supported.");
            return;
        }

        if (Modules.get().get(AutoEat.class).eating || Modules.get().get(AutoGap.class).isEating() || Modules.get().get(KillAura.class).attacking) {
            input.stop();
            return;
        }

        if (pauseOnLag.get() && TickRate.INSTANCE.getTimeSinceLastTick() >= 1.5f) {
            input.stop();
            return;
        }

        count = 0;

        if (mc.player.getY() < start.y - 0.5) setState(State.ReLevel); // don't let the current state keep ticking, switch to re-levelling straight away
        tickDoubleMine();
        state.tick(this);

        if (breakTimer > 0) breakTimer--;
        if (placeTimer > 0) placeTimer--;
    }

    @EventHandler
    private void onPacket(PacketEvent.Receive event) {
        if (event.packet instanceof InventoryS2CPacket p) {
            if (p.syncId() == 0 && suspended)
                inventory = true;
            else
                this.syncId = p.syncId();
        }
    }

    @EventHandler
    private void onGameLeave(GameLeftEvent event) {
        suspended = true;
        inventory = false;
    }

    @EventHandler
    private void onRender2d(Render2DEvent event) {
        if (suspended || !renderMine.get()) return;

        if (normalMining != null) normalMining.renderLetter();
        if (packetMining != null) packetMining.renderLetter();
    }

    @EventHandler
    private void onRender3D(Render3DEvent event) {
        if (suspended || blockPosProvider == null) return; // prevents a fascinating crash

        if (renderMine.get()) {
            render(event, blockPosProvider.getFront(), mBlockPos -> canMine(mBlockPos, true), true);
            if (floor.get() == Floor.Replace) render(event, blockPosProvider.getFloor(), mBlockPos -> canMine(mBlockPos, false), true);
            if (railings.get()) render(event, blockPosProvider.getRailings(0), mBlockPos -> canMine(mBlockPos, false), true);
            if (mineAboveRailings.get()) render(event, blockPosProvider.getRailings(1), mBlockPos -> canMine(mBlockPos, true), true);
            if (state == State.MineEChestBlockade) render(event, blockPosProvider.getBlockade(true, blockadeType.get()), mBlockPos -> canMine(mBlockPos, true), true);
        }

        if (renderPlace.get()) {
            render(event, blockPosProvider.getLiquids(), mBlockPos -> canPlace(mBlockPos, true), false);

            if (railings.get()) {
                render(event, blockPosProvider.getRailings(0), mBlockPos -> canPlace(mBlockPos, false), false);

                if (cornerBlock.get()) {
                    // make sure we only render corner support blocks if we are actually planning to place a block there
                    render(event, blockPosProvider.getRailings(-1), mBlockPos -> {
                        boolean valid = false;
                        for (MBlockPos pos : blockPosProvider.getRailings(0)) {
                            if (!blocksToPlace.get().contains(pos.getState().getBlock()) && pos.add(0, -1, 0).equals(mBlockPos)) {
                                valid = true;
                                break;
                            }
                        }

                        return valid && canPlace(mBlockPos, false);
                    }, false);
                }
            }

            render(event, blockPosProvider.getFloor(), mBlockPos -> canPlace(mBlockPos, false), false);
            if (state == State.PlaceEChestBlockade) render(event, blockPosProvider.getBlockade(false, blockadeType.get()), mBlockPos -> canPlace(mBlockPos, false), false);
        }
    }

    private void render(Render3DEvent event, MBPIterator it, Predicate<MBlockPos> predicate, boolean mine) {
        Color sideColor = mine ? renderMineSideColor.get() : renderPlaceSideColor.get();
        Color lineColor = mine ? renderMineLineColor.get() : renderPlaceLineColor.get();
        ShapeMode shapeMode = mine ? renderMineShape.get() : renderPlaceShape.get();

        for (MBlockPos pos : it) {
            posRender2.set(pos);

            if (predicate.test(posRender2)) {
                int excludeDir = 0;

                for (Direction side : Direction.values()) {
                    posRender3.set(posRender2).add(side.getOffsetX(), side.getOffsetY(), side.getOffsetZ());

                    it.save();
                    for (MBlockPos p : it) {
                        if (p.equals(posRender3) && predicate.test(p)) excludeDir |= Dir.get(side);
                    }
                    it.restore();
                }

                event.renderer.box(posRender2.getBlockPos(), sideColor, lineColor, shapeMode, excludeDir);
            }
        }
    }

    private void updateVariables() {
        prevInput = mc.player.input;
        mc.player.input = input = new CustomPlayerInput();

        placeTimer = breakTimer = count = syncId = 0;
        ignoreCrystals.clear();

        normalMining = null;
        packetMining = null;
    }

    private void setState(State state) {
        setState(state, this.state);
    }

    private void setState(State state, State lastState) {
        this.lastState = lastState;
        this.state = state;

        input.stop();
        state.start(this);
    }

    private int getWidthLeft() {
        return switch (width.get()) {
            case 5, 4 -> 2;
            case 3, 2 -> 1;
            default -> 0;
        };
    }

    private int getWidthRight() {
        return switch (width.get()) {
            case 5 -> 2;
            case 4, 3 -> 1;
            default -> 0;
        };
    }

    private boolean canMine(MBlockPos pos, boolean mineBlocksToPlace) {
        BlockState state = pos.getState();
        return BlockUtils.canBreak(pos.getBlockPos(), state) && (mineBlocksToPlace || !blocksToPlace.get().contains(state.getBlock()));
    }

    private boolean canPlace(MBlockPos pos, boolean liquids) {
        if (pos.getBlockPos().getSquaredDistance(mc.player.getEyePos()) > placeRange.get() * placeRange.get()) return false;
        return liquids ? !pos.getState().getFluidState().isEmpty() : BlockUtils.canPlace(pos.getBlockPos());
    }

    private void disconnect(String message, Object... args) {
        MutableText text = Text.literal(String.format("%s[%s%s%s] %s", Formatting.GRAY, Formatting.BLUE, title, Formatting.GRAY, Formatting.RED) + String.format(message, args)).append("\n");
        text.append(getStatsText());

        mc.getNetworkHandler().getConnection().disconnect(text);
    }

    public MutableText getStatsText() {
        MutableText text = Text.literal(String.format("%sDistance: %s%.0f\n", Formatting.GRAY, Formatting.WHITE, mc.player == null ? 0.0f : PlayerUtils.distanceTo(start)));
        text.append(String.format("%sBlocks broken: %s%d\n", Formatting.GRAY, Formatting.WHITE, blocksBroken));
        text.append(String.format("%sBlocks placed: %s%d", Formatting.GRAY, Formatting.WHITE, blocksPlaced));

        return text;
    }

    private void tickDoubleMine() {
        // could add clientside block breaking to speed the system up, but it would probably make it too vulnerable to desyncs
        if (normalMining != null) {
            if (normalMining.shouldRemove()) {
                mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK, normalMining.blockPos, normalMining.direction));
                normalMining = null;
                DoubleMineBlock.rateLimited = true;
            }
            else if (mc.world.getBlockState(normalMining.blockPos).getBlock() != normalMining.block) {
                normalMining = null;
                blocksBroken++;
                count++;
                DoubleMineBlock.rateLimited = false;
            }
            else if (normalMining.isReady()) {
                normalMining.stopDestroying();
            }

            mc.player.swingHand(Hand.MAIN_HAND);
        }

        if (packetMining != null) {
            if (packetMining.shouldRemove()) {
                // should we add rate limiting for packet mined blocks? More testing required to see if appropriate
                packetMining = null;
            }
            else if (mc.world.getBlockState(packetMining.blockPos).getBlock() != packetMining.block) {
                packetMining = null;
                blocksBroken++;
                count++;
            }
        }
    }

    private enum State {
        Center {
            @Override
            protected void start(HighwayBuilder b) {
                if (b.mc.player.getPos().isInRange(Vec3d.ofBottomCenter(b.mc.player.getBlockPos()), 0.1)) {
                    stop(b);
                }
            }

            @Override
            protected void tick(HighwayBuilder b) {
                // There is probably a much better way to do this
                double x = Math.abs(b.mc.player.getX() - (int) b.mc.player.getX()) - 0.5;
                double z = Math.abs(b.mc.player.getZ() - (int) b.mc.player.getZ()) - 0.5;

                boolean isX = Math.abs(x) <= 0.1;
                boolean isZ = Math.abs(z) <= 0.1;

                if (isX && isZ) {
                    stop(b);
                }
                else {
                    b.mc.player.setYaw(0);

                    if (!isZ) {
                        b.input.forward(z < 0);
                        b.input.backward(z > 0);

                        if (b.mc.player.getZ() < 0) {
                            boolean forward = b.input.playerInput.forward();
                            b.input.forward(b.input.playerInput.backward());
                            b.input.backward(forward);
                        }
                    }

                    if (!isX) {
                        b.input.right(x > 0);
                        b.input.left(x < 0);

                        if (b.mc.player.getX() < 0) {
                            boolean right = b.input.playerInput.right();
                            b.input.right(b.input.playerInput.left());
                            b.input.left(right);
                        }
                    }

                    b.input.sneak(true);
                }
            }

            private void stop(HighwayBuilder b) {
                b.input.stop();
                b.mc.player.setVelocity(0, 0, 0);
                b.mc.player.setPosition((int) b.mc.player.getX() + (b.mc.player.getX() < 0 ? -0.5 : 0.5), b.mc.player.getY(), (int) b.mc.player.getZ() + (b.mc.player.getZ() < 0 ? -0.5 : 0.5));
                b.setState(b.lastState);
            }
        },

        Forward {
            @Override
            protected void start(HighwayBuilder b) {
                checkTasks(b);

                if (b.state == Forward) b.mc.player.setYaw(b.dir.yaw);
            }

            @Override
            protected void tick(HighwayBuilder b) {
                checkTasks(b);

                if (b.state == Forward) b.input.forward(true); // Move
            }

            private void checkTasks(HighwayBuilder b) {
                if (b.destroyCrystalTraps.get() && isCrystalTrap(b)) b.setState(DefuseCrystalTraps); // Destroy crystal traps
                else if (needsToPlace(b, b.blockPosProvider.getLiquids(), true)) b.setState(FillLiquids); // Fill Liquids
                else if (needsToMine(b, b.blockPosProvider.getFront(), true)) b.setState(MineFront); // Mine Front
                else if (b.floor.get() == Floor.Replace && needsToMine(b, b.blockPosProvider.getFloor(), false)) b.setState(MineFloor); // Mine Floor
                else if (b.railings.get() && needsToMine(b, b.blockPosProvider.getRailings(0), false)) b.setState(MineRailings); // Mine Railings
                else if (b.mineAboveRailings.get() && needsToMine(b, b.blockPosProvider.getRailings(1), true)) b.setState(MineAboveRailings); // Mine above railings
                else if (b.railings.get() && needsToPlace(b, b.blockPosProvider.getRailings(0), false)) {
                    if (b.cornerBlock.get() && needsToPlace(b, b.blockPosProvider.getRailings(-1), false)) b.setState(PlaceCornerBlock); // Place corner support block
                    else b.setState(PlaceRailings); // Place Railings
                }
                else if (needsToPlace(b, b.blockPosProvider.getFloor(), false)) b.setState(PlaceFloor); // Place Floor
            }

            private boolean needsToMine(HighwayBuilder b, MBPIterator it, boolean mineBlocksToPlace) {
                for (MBlockPos pos : it) {
                    if (b.canMine(pos, mineBlocksToPlace)) return true;
                }

                return false;
            }

            private boolean needsToPlace(HighwayBuilder b, MBPIterator it, boolean liquids) {
                for (MBlockPos pos : it) {
                    if (b.canPlace(pos, liquids)) return true;
                }

                return false;
            }

            private boolean isCrystalTrap(HighwayBuilder b) {
                for (Entity entity : b.mc.world.getEntities()) {
                    if (!(entity instanceof EndCrystalEntity endCrystal)) continue;
                    if (PlayerUtils.isWithin(endCrystal, 12) || !PlayerUtils.isWithin(endCrystal, 24)) continue;
                    if (b.ignoreCrystals.contains(endCrystal)) continue;

                    Vec3d vec1 = new Vec3d(0, 0, 0);
                    Vec3d vec2 = new Vec3d(0, 0, 0);

                    // todo add a better raytrace check
                    ((IVec3d) vec1).meteor$set(b.mc.player.getX(), b.mc.player.getY() + b.mc.player.getStandingEyeHeight(), b.mc.player.getZ());
                    ((IVec3d) vec2).meteor$set(entity.getX(), entity.getY() + 0.5, entity.getZ());
                    return b.mc.world.raycast(new RaycastContext(vec1, vec2, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, b.mc.player)).getType() == HitResult.Type.MISS;
                }

                return false;
            }
        },

        ReLevel {
            private final BlockPos.Mutable pos = new BlockPos.Mutable();
            private BlockPos startPos;
            private int timer = 30;

            @Override
            protected void start(HighwayBuilder b) {
                startPos = BlockPos.ofFloored(b.start);
            }

            @Override
            protected void tick(HighwayBuilder b) {
                Vec3d vec = b.mc.player.getPos().add(b.mc.player.getVelocity()).add(0, -0.75, 0);
                pos.set(b.mc.player.getBlockX(), vec.y, b.mc.player.getBlockZ());

                if (pos.getY() >= b.mc.player.getBlockPos().getY()) {
                    pos.setY(b.mc.player.getBlockPos().getY() - 1);
                }

                if (pos.getY() >= startPos.getY()) pos.setY(startPos.getY() - 1);

                if (b.mc.player.getY() > b.start.y - 0.5 && !b.mc.world.getBlockState(pos).isReplaceable()) {
                    b.input.jump(false);

                    if (timer > 0) timer--;
                    else {
                        b.setState(Forward);
                        timer = 30;
                    }

                    return;
                }

                if (b.placeTimer > 0) return;

                if (timer < 30) timer = 30;
                b.input.jump(true);

                int slot = -1;
                if (pos.getY() == startPos.down().getY()) {
                    // we would prefer the block flush with the highway to be an appropriate placement block, not trash
                    slot = findAndMoveToHotbar(b, itemStack -> itemStack.getItem() instanceof BlockItem blockItem && b.blocksToPlace.get().contains(blockItem.getBlock()));
                }

                if (slot == -1) {
                    slot = findAcceptablePlacementBlock(b);
                    if (slot == -1) return;
                }

                if (BlockUtils.place(pos.toImmutable(), Hand.MAIN_HAND, slot, b.rotation.get().place, 100, true, true, false)) {
                    if (b.renderPlace.get()) RenderUtils.renderTickingBlock(pos.toImmutable(), b.renderPlaceSideColor.get(), b.renderPlaceLineColor.get(), b.renderPlaceShape.get(), 0, 5, true, false);
                    b.placeTimer = b.placeDelay.get();
                }
            }

            private int findAcceptablePlacementBlock(HighwayBuilder b) {
                // still should prioritise trash
                int slot = findAndMoveToHotbar(b, itemStack -> {
                    if (!(itemStack.getItem() instanceof BlockItem)) return false;
                    return b.trashItems.get().contains(itemStack.getItem());
                });

                // next we prioritise placement blocks
                if (slot == -1) slot = findAndMoveToHotbar(b, itemStack -> {
                    if (!(itemStack.getItem() instanceof BlockItem bi)) return false;
                    return b.blocksToPlace.get().contains(bi.getBlock());
                });

                // falling is an emergency; in this case only, we allow access to any whole block in your inventory
                return slot != -1 ? slot : findAndMoveToHotbar(b, itemStack -> {
                    if (!(itemStack.getItem() instanceof BlockItem bi)) return false;
                    if (Utils.isShulker(bi)) return false;
                    Block block = bi.getBlock();

                    if (!Block.isShapeFullCube(block.getDefaultState().getCollisionShape(b.mc.world, pos))) return false;
                    return !(block instanceof FallingBlock) || !FallingBlock.canFallThrough(b.mc.world.getBlockState(pos));
                });
            }
        },

        FillLiquids {
            @Override
            protected void tick(HighwayBuilder b) {
                int slot = findBlocksToPlacePrioritizeTrash(b);
                if (slot == -1) return;

                place(b, new MBPIteratorFilter(b.blockPosProvider.getLiquids(), pos -> !pos.getState().getFluidState().isEmpty()), slot, Forward);
            }
        },

        MineFront {
            @Override
            protected void start(HighwayBuilder b) {
                mine(b, b.blockPosProvider.getFront(), true, Forward, this);
            }

            @Override
            protected void tick(HighwayBuilder b) {
                mine(b, b.blockPosProvider.getFront(), true, Forward, this);
            }
        },

        MineFloor {
            @Override
            protected void start(HighwayBuilder b) {
                mine(b, b.blockPosProvider.getFloor(), false, Forward, this);
            }

            @Override
            protected void tick(HighwayBuilder b) {
                mine(b, b.blockPosProvider.getFloor(), false, Forward, this);
            }
        },

        MineRailings {
            @Override
            protected void start(HighwayBuilder b) {
                mine(b, b.blockPosProvider.getRailings(0), false, Forward, this);
            }

            @Override
            protected void tick(HighwayBuilder b) {
                mine(b, b.blockPosProvider.getRailings(0), false, Forward, this);
            }
        },

        MineAboveRailings {
            @Override
            protected void start(HighwayBuilder b) {
                mine(b, b.blockPosProvider.getRailings(1), true, Forward, this);
            }

            @Override
            protected void tick(HighwayBuilder b) {
                mine(b, b.blockPosProvider.getRailings(1), true, Forward, this);
            }
        },

        PlaceCornerBlock {
            @Override
            protected void start(HighwayBuilder b) {
                int slot = findBlocksToPlacePrioritizeTrash(b);
                if (slot == -1) return;

                place(b, b.blockPosProvider.getRailings(-1), slot, Forward);
            }

            @Override
            protected void tick(HighwayBuilder b) {
                int slot = findBlocksToPlacePrioritizeTrash(b);
                if (slot == -1) return;

                place(b, b.blockPosProvider.getRailings(-1), slot, Forward);
            }
        },

        PlaceRailings {
            @Override
            protected void start(HighwayBuilder b) {
                int slot = findBlocksToPlace(b);
                if (slot == -1) return;

                place(b, b.blockPosProvider.getRailings(0), slot, Forward);
            }

            @Override
            protected void tick(HighwayBuilder b) {
                int slot = findBlocksToPlace(b);
                if (slot == -1) return;

                place(b, b.blockPosProvider.getRailings(0), slot, Forward);
            }
        },

        PlaceFloor {
            @Override
            protected void start(HighwayBuilder b) {
                int slot = findBlocksToPlace(b);
                if (slot == -1) return;

                place(b, b.blockPosProvider.getFloor(), slot, Forward);
            }

            @Override
            protected void tick(HighwayBuilder b) {
                int slot = findBlocksToPlace(b);
                if (slot == -1) return;

                place(b, b.blockPosProvider.getFloor(), slot, Forward);
            }
        },

        ThrowOutTrash {
            private int skipSlot;
            private boolean timerEnabled, firstTick, threwItems;
            private int timer;
            private static final ItemStack[] ITEMS = new ItemStack[27];

            @Override
            protected void start(HighwayBuilder b) {
                int biggestCount = 0;

                for (int i = 0; i < b.mc.player.getInventory().getMainStacks().size(); i++) {
                    ItemStack itemStack = b.mc.player.getInventory().getStack(i);

                    if (itemStack.getItem() instanceof BlockItem && b.trashItems.get().contains(itemStack.getItem()) && itemStack.getCount() > biggestCount) {
                        biggestCount = itemStack.getCount();
                        skipSlot = i;

                        if (biggestCount >= 64) break;
                    }
                }

                if (biggestCount == 0) skipSlot = -1;
                timerEnabled = false;
                firstTick = true;
                threwItems = false;
            }

            @Override
            protected void tick(HighwayBuilder b) {
                if (timerEnabled) {
                    if (timer > 0) timer--;
                    else b.setState(b.lastState);

                    return;
                }

                b.mc.player.setYaw(b.dir.opposite().yaw);
                b.mc.player.setPitch(-25);

                if (firstTick) {
                    firstTick = false;
                    return;
                }

                if (!b.mc.player.currentScreenHandler.getCursorStack().isEmpty()) {
                    InvUtils.dropHand();
                    return;
                }

                for (int i = 0; i < b.mc.player.getInventory().getMainStacks().size(); i++) {
                    if (i == skipSlot) continue;

                    ItemStack itemStack = b.mc.player.getInventory().getStack(i);

                    if (b.trashItems.get().contains(itemStack.getItem())) {
                        InvUtils.drop().slot(i);
                        threwItems = true;
                        return;
                    }

                    if (b.ejectUselessShulkers.get() && Utils.isShulker(itemStack.getItem())) {
                        Utils.getItemsInContainerItem(itemStack, ITEMS);
                        boolean eject = true;
                        for (ItemStack stack : ITEMS) {
                            if (stack.getItem() instanceof BlockItem bi && (b.blocksToPlace.get().contains(bi.getBlock()) || (b.blocksToPlace.get().contains(Blocks.OBSIDIAN) && bi == Items.ENDER_CHEST))) {
                                eject = false;
                                break;
                            }
                            if (stack.isIn(ItemTags.PICKAXES)) {
                                eject = false;
                                break;
                            }
                            if (stack.contains(DataComponentTypes.FOOD) && !Modules.get().get(AutoEat.class).blacklist.get().contains(stack.getItem())) {
                                eject = false;
                                break;
                            }
                        }

                        if (eject) {
                            InvUtils.drop().slot(i);
                            threwItems = true;
                            return;
                        }
                    }
                }

                timerEnabled = true;
                timer = threwItems ? 10 : 1;
            }
        },

        PlaceEChestBlockade {
            @Override
            protected void tick(HighwayBuilder b) {
                int slot = findBlocksToPlacePrioritizeTrash(b);
                if (slot == -1) return;

                place(b, b.blockPosProvider.getBlockade(false, b.blockadeType.get()), slot, MineEnderChests);
            }
        },

        MineEChestBlockade {
            @Override
            protected void tick(HighwayBuilder b) {
                mine(b, b.blockPosProvider.getBlockade(true, b.blockadeType.get()), true, Center, Forward);
            }
        },

        MineEnderChests {
            private static final MBlockPos pos = new MBlockPos();
            private int minimumObsidian;
            private boolean first, primed;
            private boolean stopTimerEnabled;
            private int stopTimer, moveTimer, rebreakTimer, timeout;

            @Override
            protected void start(HighwayBuilder b) {
                if (b.lastState != Center && b.lastState != ThrowOutTrash && b.lastState != PlaceEChestBlockade) {
                    b.setState(Center);
                    return;
                }
                else if (b.lastState == Center) {
                    b.setState(ThrowOutTrash);
                    return;
                }
                else if (b.lastState == ThrowOutTrash) {
                    b.setState(PlaceEChestBlockade);
                    return;
                }

                int emptySlots = 0;
                for (int i = 0; i < b.mc.player.getInventory().getMainStacks().size(); i++) {
                    if (b.mc.player.getInventory().getStack(i).isEmpty()) emptySlots++;
                }

                if (emptySlots == 0) {
                    b.error("No empty slots.");
                    return;
                }

                int minimumSlots = Math.max(emptySlots - b.minEmpty.get(), 1);
                minimumObsidian = minimumSlots * 64;
                first = true;
                moveTimer = timeout = 0;

                stopTimerEnabled = false;
                primed = false;
            }

            @Override
            protected void tick(HighwayBuilder b) {
                if (stopTimerEnabled) {
                    if (stopTimer > 0) stopTimer--;
                    else b.setState(MineEChestBlockade);

                    return;
                }

                HorizontalDirection dir = b.dir.diagonal ? b.dir.rotateLeft().rotateLeftSkipOne() : b.dir.opposite();
                pos.set(b.mc.player).offset(dir);

                // Move
                if (moveTimer > 0) {
                    b.mc.player.setYaw(dir.yaw);
                    b.input.forward(moveTimer > 2);

                    moveTimer--;
                    return;
                }

                // Check for obsidian count
                int obsidianCount = 0;

                for (Entity entity : b.mc.world.getOtherEntities(b.mc.player, new Box(pos.x, pos.y, pos.z, pos.x + 1, pos.y + 2, pos.z + 1))) {
                    if (entity instanceof ItemEntity itemEntity && itemEntity.getStack().getItem() == Items.OBSIDIAN) {
                        obsidianCount += itemEntity.getStack().getCount();
                    }
                }

                for (int i = 0; i < b.mc.player.getInventory().getMainStacks().size(); i++) {
                    ItemStack itemStack = b.mc.player.getInventory().getStack(i);
                    if (itemStack.getItem() == Items.OBSIDIAN) obsidianCount += itemStack.getCount();
                }

                if (obsidianCount >= minimumObsidian) {
                    stopTimerEnabled = true;
                    stopTimer = 12;
                    return;
                }

                BlockPos bp = pos.getBlockPos();

                // Check block state
                BlockState blockState = b.mc.world.getBlockState(bp);

                if (blockState.getBlock() == Blocks.ENDER_CHEST) {
                    if (b.mc.currentScreen instanceof GenericContainerScreen screen) {
                        // wait for the screen to be properly loaded
                        if (screen.getScreenHandler().syncId != b.syncId) return;

                        b.mc.currentScreen.close();
                    }

                    // if we don't know what's in your echest, open it quickly while we have one available to check
                    if (!EChestMemory.isKnown()) {
                        if (b.rotation.get().place) Rotations.rotate(Rotations.getYaw(bp), Rotations.getPitch(bp), () ->
                            b.mc.interactionManager.interactBlock(b.mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.ofCenter(bp), Direction.UP, bp, false)));
                        else b.mc.interactionManager.interactBlock(b.mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.ofCenter(bp), Direction.UP, bp, false));

                        return;
                    }

                    if (first) {
                        moveTimer = 8;
                        first = false;
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
                            primed = false;
                            timeout = 0;
                            return;
                        }

                        if (rebreakTimer > 0) {
                            rebreakTimer--;
                            return;
                        }

                        PlayerActionC2SPacket p = new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, bp, BlockUtils.getDirection(bp));
                        rebreakTimer = b.rebreakTimer.get();

                        if (b.rotation.get().mine) Rotations.rotate(Rotations.getYaw(bp), Rotations.getPitch(bp), () -> b.mc.getNetworkHandler().sendPacket(p));
                        else b.mc.getNetworkHandler().sendPacket(p);
                    }
                    else {
                        if (b.rotation.get().mine) Rotations.rotate(Rotations.getYaw(bp), Rotations.getPitch(bp), () -> BlockUtils.breakBlock(bp, true));
                        else BlockUtils.breakBlock(bp, true);
                    }
                }
                else {
                    // Place ender chest
                    int slot = findAndMoveToHotbar(b, itemStack -> itemStack.getItem() == Items.ENDER_CHEST);
                    if (slot == -1 || countItem(b, stack -> stack.getItem().equals(Items.ENDER_CHEST)) <= b.saveEchests.get()) {
                        stopTimerEnabled = true;
                        stopTimer = 12;
                        return;
                    }

                    if (countItem(b, stack -> stack.isIn(ItemTags.PICKAXES)) <= b.savePickaxes.get()) {
                        if (b.searchEnderChest.get() || b.searchShulkers.get()) {
                            b.restockTask.setPickaxes();
                        }
                    }

                    if (!first) primed = true;

                    BlockUtils.place(bp, Hand.MAIN_HAND, slot, b.rotation.get().place, 0, true, true, false);
                    timeout = 0;
                }
            }
        },

        // this one was rough to do
        Restock {
            private static final MBlockPos pos = new MBlockPos();
            private static final ItemStack[] ITEMS = new ItemStack[27];
            private int minimumSlots, stopTimer, delayTimer;
            private boolean breakContainer, indicateStopping;
            private Predicate<ItemStack> shulkerPredicate;

            // if this is ever not -1 when we expect it to be, things break a lot
            private int slot = -1;

            @Override
            protected void start(HighwayBuilder b) {
                slot = -1; // :ptsd:

                // set the predicate to test for shulker boxes
                if (shulkerPredicate == null) setShulkerPredicate(b);

                if (b.restockTask.tasksInactive()) {
                    b.setState(Forward);
                    return;
                }

                if (b.lastState != Center && b.lastState != ThrowOutTrash && b.lastState != PlaceShulkerBlockade && b.lastState != this) {
                    b.setState(Center);
                    return;
                }
                else if (b.lastState == Center) {
                    b.setState(ThrowOutTrash);
                    return;
                }

                // firstly search your inventory for shulkers that have the items you need
                if (slot == -1 && b.searchShulkers.get()) {
                    slot = findAndMoveToHotbar(b, shulkerPredicate);

                    if (slot != -1 && b.lastState != PlaceShulkerBlockade) {
                        b.setState(PlaceShulkerBlockade);
                    }
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
                            if (b.restockTask.pickaxes && stack.isIn(ItemTags.PICKAXES)) {
                                stop = false;
                                break;
                            }
                            if (b.restockTask.food && stack.contains(DataComponentTypes.FOOD) && !Modules.get().get(AutoEat.class).blacklist.get().contains(stack.getItem())) {
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
                        (b.restockTask.pickaxes && countItem(b, itemStack -> itemStack.isIn(ItemTags.PICKAXES)) > b.savePickaxes.get()) ||
                        (b.restockTask.food && hasItem(b, itemStack -> itemStack.contains(DataComponentTypes.FOOD) && !Modules.get().get(AutoEat.class).blacklist.get().contains(itemStack.getItem())))
                    );

                    if (restockOccurred) {
                        b.setState(ThrowOutTrash, Forward);
                    } else b.error("Unable to perform restock for '" + b.restockTask.item() + "'.");

                    return;
                }

                int restockSlots = -b.minEmpty.get();
                for (int i = 0; i < b.mc.player.getInventory().getMainStacks().size(); i++) {
                    if (b.mc.player.getInventory().getStack(i).isEmpty()) restockSlots++;
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
                breakContainer = b.mc.world.getBlockState(pos.getBlockPos()).getBlock() == Blocks.ENDER_CHEST;

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
                    if (stopTimer > 0) stopTimer--;
                    else {
                        if (b.lastState == PlaceShulkerBlockade) {// && !(b.blocksToPlace.get().contains(Blocks.OBSIDIAN) && countItem(b, stack -> stack.getItem() == Items.ENDER_CHEST) > b.saveEchests.get() && !hasItem(b, stack -> stack.getItem() == Items.OBSIDIAN))) {
                            b.setState(MineShulkerBlockade);
                        } else {
                            b.setState(ThrowOutTrash, Forward);
                        }
                    }

                    return;
                }

                // prevent tasks executing when they shouldn't
                if (b.restockTask.tasksInactive()) {
                    b.setState(Forward);
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
                    if (b.blocksToPlace.get().contains(Blocks.OBSIDIAN)) slotsPulled += ((countItem(b, itemStack -> itemStack.getItem() == Items.ENDER_CHEST) - b.saveEchests.get()) * 8) / 64;
                }
                if (b.restockTask.pickaxes) slotsPulled += countSlots(b, itemStack -> itemStack.isIn(ItemTags.PICKAXES)) - b.savePickaxes.get();
                if (b.restockTask.food) slotsPulled += countSlots(b, itemStack -> itemStack.contains(DataComponentTypes.FOOD) && !Modules.get().get(AutoEat.class).blacklist.get().contains(itemStack.getItem()));


                // whether we have pulled the minimum amount of items we want
                if (slotsPulled >= minimumSlots && !indicateStopping) {
                    indicateStopping = true;
                    breakContainer = true;
                    stopTimer = 12;
                    if (b.mc.currentScreen != null) b.mc.currentScreen.close();
                    return;
                }

                // Check block state
                BlockPos blockPos = pos.getBlockPos();
                BlockState blockState = b.mc.world.getBlockState(blockPos);

                switch (blockState.getBlock()) {
                    // if we have placed a shulker box there should be items inside we want
                    case ShulkerBoxBlock ignored -> {
                        if (b.mc.currentScreen instanceof ShulkerBoxScreen screen) {
                            // wait for the screen to be properly loaded
                            if (screen.getScreenHandler().syncId != b.syncId) return;

                            Inventory inv = ((ShulkerBoxScreenHandlerAccessor) screen.getScreenHandler()).meteor$getInventory();

                            if (restockItems(b, inv)) {
                                delayTimer = b.inventoryDelay.get();
                                return;
                            }

                            // we have taken everything we can from the shulker box, and since slotsPulled >= minimumSlots is false, we should keep going
                            // close the screen, break the shulker box, look for more containers to loot from
                            b.mc.currentScreen.close();
                            breakContainer = true;
                        }
                        else {
                            if (!b.searchShulkers.get()) breakContainer = true;
                            handleContainerBlock(b, blockPos);
                        }
                    }

                    // we are either pulling items themselves, or shulkers containing items from your ec
                    case EnderChestBlock ignored -> {
                        if (b.mc.currentScreen instanceof GenericContainerScreen screen) {
                            // wait for the screen to be properly loaded
                            if (screen.getScreenHandler().syncId != b.syncId) return;

                            Inventory inv = screen.getScreenHandler().getInventory();

                            if (restockItems(b, inv)) {
                                delayTimer = b.inventoryDelay.get();
                                return;
                            }

                            // we may have taken items themselves from the ec, but still need more. Now we try to find a shulker containing the items
                            if (b.searchShulkers.get()) {
                                int moveTo = InvUtils.findEmpty().slot();

                                if (moveTo != -1) {
                                    for (int i = 0; i < inv.size(); i++) {
                                        if (shulkerPredicate.test(inv.getStack(i))) {
                                            InvUtils.move().fromId(i).to(moveTo);
                                            delayTimer = b.inventoryDelay.get();
                                            break;
                                        }
                                    }
                                }
                            }

                            // if it reaches here, we have taken everything we can from your ender chest, and may have also grabbed a shulker
                            // we should be finished in your ender chest, so we can break it and either continue on our way or start checking shulkers
                            b.mc.currentScreen.close();
                            breakContainer = true;
                        }
                        else {
                            if (!b.searchEnderChest.get()) breakContainer = true;
                            handleContainerBlock(b, blockPos);
                        }
                    }

                    // handling when there is no container there
                    case AirBlock ignored -> {
                        // indicates we have just broken a container
                        if (breakContainer) {
                            breakContainer = false;

                            // if we don't signal intent to stop, we loop back to the start and continue restocking
                            if (indicateStopping) b.restockTask.complete();
                            else start(b);

                            return;
                        }

                        BlockUtils.place(blockPos, Hand.MAIN_HAND, slot, b.rotation.get().place, 0, true, true, false);
                    }

                    // the only valid blocks should be air, a shulker box, or an ender chest
                    // if there is another type of block, assume something has gone wrong and error out (e.g. lava flowed in)
                    default -> b.error("Invalid block at container restocking position?");
                }
            }

            private boolean restockItems(HighwayBuilder b, Inventory inv) {
                if (b.restockTask.materials) {
                    // take raw material
                    if (grabFromInventory(inv, itemStack -> itemStack.getItem() instanceof BlockItem bi && b.blocksToPlace.get().contains(bi.getBlock()))) return true;

                    // prefer taking raw material before echests
                    if (b.blocksToPlace.get().contains(Blocks.OBSIDIAN)) {
                        if (grabFromInventory(inv, itemStack -> itemStack.getItem() == Items.ENDER_CHEST)) return true;
                    }
                }
                if (b.restockTask.pickaxes) {
                    if (grabFromInventory(inv, itemStack -> itemStack.isIn(ItemTags.PICKAXES))) return true;
                }
                if (b.restockTask.food) {
                    return grabFromInventory(inv, itemStack -> itemStack.contains(DataComponentTypes.FOOD) && !Modules.get().get(AutoEat.class).blacklist.get().contains(itemStack.getItem()));
                }

                return false;
            }

            // scans the inventory, takes out the first item that matches the predicate and returns
            private boolean grabFromInventory(Inventory inv, Predicate<ItemStack> filterItem) {
                for (int i = 0; i < inv.size(); i++) {
                    if (filterItem.test(inv.getStack(i))) {
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
                            if (b.blocksToPlace.get().contains(bi.getBlock()) || (b.blocksToPlace.get().contains(Blocks.OBSIDIAN) && bi == Items.ENDER_CHEST)) return true;
                        }
                        if (b.restockTask.pickaxes && stack.isIn(ItemTags.PICKAXES)) return true;
                        if (b.restockTask.food && stack.contains(DataComponentTypes.FOOD) && !Modules.get().get(AutoEat.class).blacklist.get().contains(stack.getItem())) return true;
                    }

                    return false;
                };
            }

            private void handleContainerBlock(HighwayBuilder b, BlockPos bp) {
                if (breakContainer) {
                    BlockState state = b.mc.world.getBlockState(bp);

                    int toolSlot = findAndMoveBestToolToHotbar(b, state, false);
                    InvUtils.swap(toolSlot, false);

                    if (b.rotation.get().mine) Rotations.rotate(Rotations.getYaw(bp), Rotations.getPitch(bp), () -> BlockUtils.breakBlock(bp, true));
                    else BlockUtils.breakBlock(bp, true);
                } else {
                    if (b.rotation.get().place) {
                        Rotations.rotate(Rotations.getYaw(bp), Rotations.getPitch(bp), () ->
                            b.mc.interactionManager.interactBlock(b.mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.ofCenter(bp), Direction.UP, bp, false))
                        );
                    }
                    else b.mc.interactionManager.interactBlock(b.mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.ofCenter(bp), Direction.UP, bp, false));

                    delayTimer = b.inventoryDelay.get();
                }
            }

            private int countSlots(HighwayBuilder b, Predicate<ItemStack> predicate) {
                int count = 0;
                for (int i = 0; i < b.mc.player.getInventory().getMainStacks().size(); i++) {
                    ItemStack stack = b.mc.player.getInventory().getStack(i);
                    if (predicate.test(stack)) count++;
                }

                return count;
            }
        },

        PlaceShulkerBlockade {
            @Override
            protected void tick(HighwayBuilder b) {
                int slot = findBlocksToPlacePrioritizeTrash(b);
                if (slot == -1) return;

                place(b, b.blockPosProvider.getBlockade(false, BlockadeType.Shulker), slot, Restock);
            }
        },

        MineShulkerBlockade {
            private boolean stopTimerEnabled;
            private int stopTimer;

            @Override
            protected void start(HighwayBuilder b) {
                stopTimerEnabled = false;
                if (b.lastState == this) {
                    stopTimerEnabled = true;
                    stopTimer = 12;
                }
            }

            @Override
            protected void tick(HighwayBuilder b) {
                if (!stopTimerEnabled) {
                    // mining b.blockadeType instead of BlockadeType.Shulker is the fastest fix to the module leaving
                    // some blocks behind if you start a pickaxe restock task while mining echests
                    mine(b, b.blockPosProvider.getBlockade(true, b.blockadeType.get()), true, this, this);
                }
                else {
                    stopTimer--;
                    if (stopTimer <= 0) {
                        b.setState(ThrowOutTrash, Forward);
                    }
                }
            }
        },

        DefuseCrystalTraps {
            private int cooldown, shots;
            private EndCrystalEntity target;

            @Override
            protected void start(HighwayBuilder b) {
                if (!InvUtils.find(Items.BOW).found() || (!InvUtils.find(itemStack -> itemStack.getItem() instanceof ArrowItem).found() && !b.mc.player.getAbilities().creativeMode)) {
                    b.destroyCrystalTraps.set(false);
                    b.warning("No bow found to destroy crystal traps with. Toggling the setting off.");
                    b.setState(Forward);
                }

                shots = cooldown = 0;
                target = null;
            }

            /**
             * Need to perform the linked injection to ensure that vanilla code does not interfere with us drawing our
             * bow. The {@link MinecraftClient#handleInputEvents} method is only called when you are not in a screen,
             * meaning we cannot draw our bow using {@link GameOptions#useKey} since it would not work if you are in a
             * screen. Similarly, drawing our bow by {@link ClientPlayerInteractionManager#interactItem} would get
             * cancelled by default within the handleInputEvents method if you do not have the use key held down,
             * essentially meaning without the following injection it would not work if you don't have a screen open.
             *
             * @see meteordevelopment.meteorclient.mixin.MinecraftClientMixin#wrapStopUsing(ClientPlayerInteractionManager, PlayerEntity)
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
                        b.setState(Forward);
                        b.mc.interactionManager.stopUsingItem(b.mc.player);
                        b.drawingBow = false;
                        return;
                    }

                    InvUtils.swap(slot, false);
                }

                EndCrystalEntity potentialTarget = (EndCrystalEntity) TargetUtils.get(entity -> {
                    if (!(entity instanceof EndCrystalEntity endCrystal)) return false;
                    if (PlayerUtils.isWithin(endCrystal, 12) || !PlayerUtils.isWithin(endCrystal, 24)) return false;
                    if (b.ignoreCrystals.contains(endCrystal)) return false;

                    Vec3d vec1 = new Vec3d(0, 0, 0);
                    Vec3d vec2 = new Vec3d(0, 0, 0);

                    ((IVec3d) vec1).meteor$set(b.mc.player.getX(), b.mc.player.getY() + b.mc.player.getStandingEyeHeight(), b.mc.player.getZ());
                    ((IVec3d) vec2).meteor$set(entity.getX(), entity.getY() + 0.5, entity.getZ());
                    return b.mc.world.raycast(new RaycastContext(vec1, vec2, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, b.mc.player)).getType() == HitResult.Type.MISS;
                }, SortPriority.LowestDistance);

                if (target == null || target.isRemoved()) {
                    if (potentialTarget == null) {
                        b.setState(Forward);
                        b.mc.interactionManager.stopUsingItem(b.mc.player);
                        b.drawingBow = false;
                        return;
                    }
                    else {
                        target = potentialTarget;
                        shots = 0;
                    }
                }

                if (shots >= 3) {
                    b.ignoreCrystals.add(target);
                    b.warning("Detected potential hangup on a crystal. Adding it to ignore list and continuing forward.");
                    b.setState(Forward);
                    b.mc.interactionManager.stopUsingItem(b.mc.player);
                    b.drawingBow = false;
                    return;
                }

                b.mc.player.setYaw((float) Rotations.getYaw(target));

                float pitch = aim(b, target);
                if (Float.isNaN(pitch)) b.mc.player.setPitch((float) Rotations.getPitch(target));
                else b.mc.player.setPitch(pitch);

                if (BowItem.getPullProgress(b.mc.player.getItemUseTime() - 3) >= 1.0f) {
                    b.mc.interactionManager.stopUsingItem(b.mc.player);
                    b.drawingBow = false;
                    cooldown = 20;
                    shots++;
                }
                else {
                    b.drawingBow = true;
                    b.mc.interactionManager.interactItem(b.mc.player, Hand.MAIN_HAND);
                }
            }

            private float aim(HighwayBuilder b, Entity target) {
                // Velocity based on bow charge.
                float velocity = BowItem.getPullProgress(b.mc.player.getItemUseTime());

                // Positions
                Vec3d pos = target.getPos();

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
        };

        protected void start(HighwayBuilder b) {}

        protected abstract void tick(HighwayBuilder b);

        protected void mine(HighwayBuilder b, MBPIterator it, boolean mineBlocksToPlace, State nextState, State lastState) {
            boolean breaking = false;
            boolean finishedBreaking = false; // if you can multi break this lets you mine blocks between tasks in a single tick

            // extract all candidates for double mining and enqueue them to be mined. After those we can break the remaining
            // blocks normally
            if (b.doubleMine.get()) {
                // todo hold the best mining tool before performing the double mining checks so we dont double mine blocks unnecessarily
                ArrayDeque<BlockPos> toDoubleMine = new ArrayDeque<>();

                it.save();
                it.forEach(pos -> {
                    // only want to double mine blocks that we can mine, that are not instamined, and we are not already mining
                    if (
                        BlockUtils.canBreak(pos.getBlockPos(), pos.getState())
                        && (mineBlocksToPlace || !b.blocksToPlace.get().contains(pos.getState().getBlock()))
                        && !BlockUtils.canInstaBreak(pos.getBlockPos()) && (!Modules.get().get(SpeedMine.class).instamine() || pos.getState().calcBlockBreakingDelta(b.mc.player, b.mc.world, pos.getBlockPos()) <= 0.5)
                        && (b.normalMining == null || !pos.getBlockPos().equals(b.normalMining.blockPos))
                        && (b.packetMining == null || !pos.getBlockPos().equals(b.packetMining.blockPos))
                    ) {
                        toDoubleMine.add(pos.getBlockPos().mutableCopy());
                    }
                });

                // have to save and restore the iterator from the beginning to make sure the subsequent loop can use it properly
                it.restore();

                // repeating the code for swapping to a tool, since we don't want to start mining a block if we don't
                // have a tool to mine it with, but also we want to lock the slot to the tool while we are mining even
                // the ArrayDequeue is empty
                if (!toDoubleMine.isEmpty()) {
                    int slot = findAndMoveBestToolToHotbar(b, b.mc.world.getBlockState(toDoubleMine.peek()), false);
                    if (slot == -1) return;

                    InvUtils.swap(slot, false);
                    doubleMine(b, toDoubleMine);
                }

                if (b.normalMining != null || b.packetMining != null) {
                    int slot = findAndMoveBestToolToHotbar(b, b.normalMining != null ? b.normalMining.blockState : b.packetMining.blockState, false);
                    if (slot == -1) return;

                    InvUtils.swap(slot, false);
                    return;
                }
            }

            for (MBlockPos pos : it) {
                if (b.count >= b.blocksPerTick.get()) return;
                if (b.breakTimer > 0) return;

                BlockState state = pos.getState();
                if (state.isAir() || (!mineBlocksToPlace && b.blocksToPlace.get().contains(state.getBlock()))) continue;

                int slot = findAndMoveBestToolToHotbar(b, state, false);
                if (slot == -1) return;

                InvUtils.swap(slot, false);

                BlockPos mcPos = pos.getBlockPos();
                boolean multiBreak = b.blocksPerTick.get() > 1 && BlockUtils.canInstaBreak(mcPos) && !b.rotation.get().mine;
                if (BlockUtils.canBreak(mcPos)) {
                    if (b.rotation.get().mine) Rotations.rotate(Rotations.getYaw(mcPos), Rotations.getPitch(mcPos), () -> BlockUtils.breakBlock(mcPos, true));
                    else BlockUtils.breakBlock(mcPos, true);
                    breaking = true;

                    b.breakTimer = b.breakDelay.get();

                    if (!b.lastBreakingPos.equals(pos)) {
                        b.lastBreakingPos.set(pos);
                        b.blocksBroken++;
                    }

                    b.count++;

                    // can only multi break if we aren't rotating and the block can be insta-mined
                    if (!multiBreak) break;
                }

                if (!it.hasNext() && BlockUtils.canInstaBreak(mcPos)) finishedBreaking = true;
            }

            // we quickly jump to the next state, to remove micro delays in the process and allow us to break blocks
            // between tasks if we can multi break
            if (finishedBreaking || !breaking) {
                b.setState(nextState, lastState);
            }
        }

        private void doubleMine(HighwayBuilder b, ArrayDeque<BlockPos> blocks) {
            if (b.breakTimer > 0) return;

            if (b.normalMining == null) {
                DoubleMineBlock block = new DoubleMineBlock(b, blocks.pop());
                b.normalMining = block.startDestroying();

                b.breakTimer = b.breakDelay.get();
                if (b.breakTimer > 0) return;
            }

            if (DoubleMineBlock.rateLimited) return;

            if (b.packetMining == null && !blocks.isEmpty()) {
                DoubleMineBlock block = new DoubleMineBlock(b, blocks.pop());

                if (block != null) {
                    b.packetMining = b.normalMining.packetMine();
                    b.normalMining = block.startDestroying();

                    b.breakTimer = b.breakDelay.get();
                }
            }
        }

        protected void place(HighwayBuilder b, MBPIterator it, int slot, State nextState) {
            boolean placed = false;
            boolean finishedPlacing = false;

            for (MBlockPos pos : it) {
                if (b.count >= it.placementsPerTick(b)) return;
                if (b.placeTimer > 0) return;

                if (pos.getBlockPos().getSquaredDistance(b.mc.player.getEyePos()) > b.placeRange.get() * b.placeRange.get()) continue;

                if (BlockUtils.place(pos.getBlockPos(), Hand.MAIN_HAND, slot, b.rotation.get().place, 0, true, true, true)) {
                    placed = true;
                    b.blocksPlaced++;
                    b.placeTimer = b.placeDelay.get();

                    b.count++;
                    if (b.placementsPerTick.get() == 1) break;
                }

                if (!it.hasNext()) finishedPlacing = true;
            }

            if (finishedPlacing || !placed) b.setState(nextState);
        }

        private int findSlot(HighwayBuilder b, Predicate<ItemStack> predicate, boolean hotbar) {
            for (int i = hotbar ? 0 : 9; i < (hotbar ? 9 : b.mc.player.getInventory().getMainStacks().size()); i++) {
                if (predicate.test(b.mc.player.getInventory().getStack(i))) return i;
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
                ItemStack itemStack = b.mc.player.getInventory().getStack(i);

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
            for (int i = 0; i < b.mc.player.getInventory().getMainStacks().size(); i++) {
                if (predicate.test(b.mc.player.getInventory().getStack(i))) return true;
            }

            return false;
        }

        protected int countItem(HighwayBuilder b, Predicate<ItemStack> predicate) {
            int count = 0;
            for (int i = 0; i < b.mc.player.getInventory().getMainStacks().size(); i++) {
                ItemStack stack = b.mc.player.getInventory().getStack(i);
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
            // Check for creative
            if (b.mc.player.isCreative()) return b.mc.player.getInventory().getSelectedSlot();

            // Find best tool
            double bestScore = -1;
            int bestSlot = -1;

            for (int i = 0; i < b.mc.player.getInventory().getMainStacks().size(); i++) {
                double score = AutoTool.getScore(b.mc.player.getInventory().getStack(i), blockState, false, false, AutoTool.EnchantPreference.None, itemStack -> {
                    if (noSilkTouch && Utils.hasEnchantment(itemStack, Enchantments.SILK_TOUCH)) return false;
                    return !b.dontBreakTools.get() || itemStack.getMaxDamage() - itemStack.getDamage() > (itemStack.getMaxDamage() * (b.breakDurability.get() / 100));
                });

                if (score > bestScore) {
                    bestScore = score;
                    bestSlot = i;
                }
            }

            if (bestSlot == -1) return b.mc.player.getInventory().getSelectedSlot();

            ItemStack bestStack = b.mc.player.getInventory().getStack(bestSlot);
            if (bestStack.isIn(ItemTags.PICKAXES)) {
                int count = countItem(b, stack -> stack.isIn(ItemTags.PICKAXES));

                // If we are in the process of restocking pickaxes and happen to need one, we should allow using it
                // as long as it has enough durability, since we will obtain more shortly thereafter
                if (count <= b.savePickaxes.get() && !(b.restockTask.pickaxes && bestStack.getMaxDamage() - bestStack.getDamage() > (bestStack.getMaxDamage() * (b.breakDurability.get() / 100)))) {
                    if (!b.restockTask.pickaxes && (b.searchEnderChest.get() || b.searchShulkers.get())) {
                        b.restockTask.setPickaxes();
                    }
                    else {
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
                }
                else if (b.searchEnderChest.get() || b.searchShulkers.get()) {
                    // start restocking if we're allowed
                    b.restockTask.setMaterials();
                }
                else {
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

    private interface MBPIterator extends Iterator<MBlockPos>, Iterable<MBlockPos> {
        void save();
        void restore();

        @NotNull
        @Override
        default Iterator<MBlockPos> iterator() {
            return this;
        }

        default int placementsPerTick(HighwayBuilder b) {
            return b.placementsPerTick.get();
        }
    }

    private static class MBPIteratorFilter implements MBPIterator {
        private final MBPIterator it;
        private final Predicate<MBlockPos> predicate;

        private MBlockPos pos;
        private boolean isOld = true;

        private boolean pisOld = true;

        public MBPIteratorFilter(MBPIterator it, Predicate<MBlockPos> predicate) {
            this.it = it;
            this.predicate = predicate;
        }

        @Override
        public void save() {
            it.save();
            pisOld = isOld;
            isOld = true;
        }

        @Override
        public void restore() {
            it.restore();
            isOld = pisOld;
        }

        @Override
        public boolean hasNext() {
            if (isOld) {
                isOld = false;
                pos = null;

                while (it.hasNext()) {
                    pos = it.next();

                    if (predicate.test(pos)) return true;
                    else pos = null;
                }
            }

            return pos != null && predicate.test(pos);
        }

        @Override
        public MBlockPos next() {
            isOld = true;
            return pos;
        }
    }

    private interface IBlockPosProvider {
        MBPIterator getFront();
        MBPIterator getFloor();

        /**
         * state:
         *  1 for above the railings,
         *  0 for the railings themselves,
         *  -1 for the block under the railings
         */
        MBPIterator getRailings(int state);

        MBPIterator getLiquids();
        MBPIterator getBlockade(boolean mine, BlockadeType type);
    }

    private class StraightBlockPosProvider implements IBlockPosProvider {
        private final MBlockPos pos = new MBlockPos();
        private final MBlockPos pos2 = new MBlockPos();

        @Override
        public MBPIterator getFront() {
            pos.coerceBlockLevel(mc.player).offset(dir).offset(leftDir, getWidthLeft());

            return new MBPIterator() {
                private int w, y;
                private int pw, py;

                @Override
                public boolean hasNext() {
                    return w < width.get() && y < height.get();
                }

                @Override
                public MBlockPos next() {
                    pos2.set(pos).offset(rightDir, w).add(0, y, 0);

                    w++;
                    if (w >= width.get()) {
                        w = 0;
                        y++;
                    }

                    return pos2;
                }

                @Override
                public void save() {
                    pw = w;
                    py = y;
                    w = y = 0;
                }

                @Override
                public void restore() {
                    w = pw;
                    y = py;
                }
            };
        }

        @Override
        public MBPIterator getFloor() {
            pos.coerceBlockLevel(mc.player).offset(dir).offset(leftDir, getWidthLeft()).add(0, -1, 0);

            return new MBPIterator() {
                private int w;
                private int pw;

                @Override
                public boolean hasNext() {
                    return w < width.get();
                }

                @Override
                public MBlockPos next() {
                    return pos2.set(pos).offset(rightDir, w++);
                }

                @Override
                public void save() {
                    pw = w;
                    w = 0;
                }

                @Override
                public void restore() {
                    w = pw;
                }
            };
        }

        @Override
        public MBPIterator getRailings(int state) {
            pos.coerceBlockLevel(mc.player).offset(dir);

            return new MBPIterator() {
                private int i, y = state;
                private int pi, py;

                @Override
                public boolean hasNext() {
                    // state == 1 : height
                    // state == 0 : 1
                    // state == -1 : 0
                    return i < 2 && y < (state == 1 ? height.get() : state + 1);
                }

                @Override
                public MBlockPos next() {
                    if (i == 0) pos2.set(pos).offset(leftDir, getWidthLeft() + 1).add(0, y, 0);
                    else pos2.set(pos).offset(rightDir, getWidthRight() + 1).add(0, y, 0);

                    y++;
                    if (y >= (state == 1 ? height.get() : state + 1)) {
                        y = state;
                        i++;
                    }

                    return pos2;
                }

                @Override
                public void save() {
                    pi = i;
                    py = y;
                    i = 0;
                    y = state;
                }

                @Override
                public void restore() {
                    i = pi;
                    y = py;
                }
            };
        }

        @Override
        public MBPIterator getLiquids() {
            pos.coerceBlockLevel(mc.player).offset(dir, 2).offset(leftDir, getWidthLeft() + (mineAboveRailings.get() ? 2 : 1));

            return new MBPIterator() {
                private int w, y;
                private int pw, py;

                private int getWidth() {
                    return width.get() + (mineAboveRailings.get() ? 2 : 0);
                }

                @Override
                public boolean hasNext() {
                    return w < getWidth() + 2 && y < height.get() + 1;
                }

                @Override
                public MBlockPos next() {
                    pos2.set(pos).offset(rightDir, w).add(0, y, 0);

                    w++;
                    if (w >= getWidth() + 2) {
                        w = 0;
                        y++;
                    }

                    return pos2;
                }

                @Override
                public void save() {
                    pw = w;
                    py = y;
                    w = y = 0;
                }

                @Override
                public void restore() {
                    w = pw;
                    y = py;
                }
            };
        }

        @Override
        public MBPIterator getBlockade(boolean mine, BlockadeType blockadeType) {
            return new MBPIterator() {
                private int i = mine ? -1 : 0, y;
                private int pi, py;

                private MBlockPos get(int i) {
                    pos.coerceBlockLevel(mc.player).offset(dir.opposite());

                    return switch (i) {
                        case -1 -> pos;
                        case 0 -> pos.offset(dir.opposite());
                        case 1 -> pos.offset(leftDir);
                        case 2 -> pos.offset(rightDir);
                        case 3 -> pos.offset(dir, 2);
                        case 4 -> pos.offset(dir).offset(leftDir);
                        case 5 -> pos.offset(dir).offset(rightDir);
                        default -> throw new IllegalStateException("Unexpected value: " + i);
                    };
                }

                @Override
                public boolean hasNext() {
                    return i < blockadeType.columns && y < 2;
                }

                @Override
                public MBlockPos next() {
                    if (width.get() == 1 && railings.get() && i > 0 && y == 0) y++;

                    MBlockPos pos = get(i).add(0, y, 0);

                    y++;
                    if (y > 1) {
                        y = 0;
                        i++;
                    }

                    return pos;
                }

                @Override
                public void save() {
                    pi = i;
                    py = y;
                    i = y = 0;
                }

                @Override
                public void restore() {
                    i = pi;
                    y = py;
                }

                @Override
                public int placementsPerTick(HighwayBuilder b) {
                    return 1;
                }
            };
        }
    }

    private class DiagonalBlockPosProvider implements IBlockPosProvider {
        private final MBlockPos pos = new MBlockPos();
        private final MBlockPos pos2 = new MBlockPos();

        @Override
        public MBPIterator getFront() {
            pos.coerceBlockLevel(mc.player).offset(dir.rotateLeft()).offset(leftDir, getWidthLeft() - 1);

            return new MBPIterator() {
                private int i, w, y;
                private int pi, pw, py;

                @Override
                public boolean hasNext() {
                    return i < 2 && w < width.get() && y < height.get();
                }

                @Override
                public MBlockPos next() {
                    pos2.set(pos).offset(rightDir, w).add(0, y++, 0);

                    if (y >= height.get()) {
                        y = 0;
                        w++;

                        if (w >= (i == 0 ? width.get() - 1 : width.get())) {
                            w = 0;
                            i++;

                            pos.coerceBlockLevel(mc.player).offset(dir).offset(leftDir, getWidthLeft());
                        }
                    }

                    return pos2;
                }

                private void initPos() {
                    if (i == 0) pos.coerceBlockLevel(mc.player).offset(dir.rotateLeft()).offset(leftDir, getWidthLeft() - 1);
                    else pos.coerceBlockLevel(mc.player).offset(dir).offset(leftDir, getWidthLeft());
                }

                @Override
                public void save() {
                    pi = i;
                    pw = w;
                    py = y;
                    i = w = y = 0;

                    initPos();
                }

                @Override
                public void restore() {
                    i = pi;
                    w = pw;
                    y = py;

                    initPos();
                }
            };
        }

        @Override
        public MBPIterator getFloor() {
            pos.coerceBlockLevel(mc.player).add(0, -1, 0).offset(dir.rotateLeft()).offset(leftDir, getWidthLeft() - 1);

            return new MBPIterator() {
                private int i, w;
                private int pi, pw;

                @Override
                public boolean hasNext() {
                    return i < 2 && w < width.get();
                }

                @Override
                public MBlockPos next() {
                    pos2.set(pos).offset(rightDir, w++);

                    if (w >= (i == 0 ? width.get() - 1 : width.get())) {
                        w = 0;
                        i++;

                        pos.coerceBlockLevel(mc.player).add(0, -1, 0).offset(dir).offset(leftDir, getWidthLeft());
                    }

                    return pos2;
                }

                private void initPos() {
                    if (i == 0) pos.coerceBlockLevel(mc.player).add(0, -1, 0).offset(dir.rotateLeft()).offset(leftDir, getWidthLeft() - 1);
                    else pos.coerceBlockLevel(mc.player).add(0, -1, 0).offset(dir).offset(leftDir, getWidthLeft());
                }

                @Override
                public void save() {
                    pi = i;
                    pw = w;
                    i = w = 0;

                    initPos();
                }

                @Override
                public void restore() {
                    i = pi;
                    w = pw;

                    initPos();
                }
            };
        }

        @Override
        public MBPIterator getRailings(int state) {
            pos.coerceBlockLevel(mc.player).offset(dir.rotateLeft()).offset(leftDir, getWidthLeft());

            return new MBPIterator() {
                private int i, y = state;
                private int pi, py;

                @Override
                public boolean hasNext() {
                    return i < 2 && y < (state == 1 ? height.get() : state + 1);
                }

                @Override
                public MBlockPos next() {
                    pos2.set(pos).add(0, y++, 0);

                    if (y >= (state == 1 ? height.get() : state + 1)) {
                        y = state;
                        i++;

                        pos.coerceBlockLevel(mc.player).offset(dir.rotateRight()).offset(rightDir, getWidthRight());
                    }

                    return pos2;
                }

                private void initPos() {
                    if (i == 0) pos.coerceBlockLevel(mc.player).offset(dir.rotateLeft()).offset(leftDir, getWidthLeft());
                    else pos.coerceBlockLevel(mc.player).offset(dir.rotateRight()).offset(rightDir, getWidthRight());
                }

                @Override
                public void save() {
                    pi = i;
                    py = y;
                    i = 0;
                    y = state;

                    initPos();
                }

                @Override
                public void restore() {
                    i = pi;
                    y = py;

                    initPos();
                }
            };
        }

        @Override
        public MBPIterator getLiquids() {
            boolean m = mineAboveRailings.get();
            pos.coerceBlockLevel(mc.player).offset(dir).offset(dir.rotateLeft()).offset(leftDir, getWidthLeft());

            return new MBPIterator() {
                private int i, w, y;
                private int pi, pw, py;

                private int getWidth() {
                    return width.get() + (i == 0 ? 1 : 0) + (m && i == 1 ? 2 : 0);
                }

                @Override
                public boolean hasNext() {
                    if (m && i == 1 && y == height.get() &&  w == getWidth() - 1) return false;
                    return i < 2 && w < getWidth() && y < height.get() + 1;
                }

                private void updateW() {
                    w++;

                    if (w >= getWidth()) {
                        w = 0;
                        i++;

                        pos.coerceBlockLevel(mc.player).offset(dir, 2).offset(leftDir, getWidthLeft() + (m ? 1 : 0));
                    }
                }

                @Override
                public MBlockPos next() {
                    if (i == (m ? 1 : 0) && y == height.get() && (w == 0 || w == getWidth() - 1)) {
                        y = 0;
                        updateW();
                    }

                    pos2.set(pos).offset(rightDir, w).add(0, y++, 0);

                    if (y >= height.get() + 1) {
                        y = 0;
                        updateW();
                    }

                    return pos2;
                }

                private void initPos() {
                    if (i == 0) pos.coerceBlockLevel(mc.player).offset(dir).offset(dir.rotateLeft()).offset(leftDir, getWidthLeft());
                    else pos.coerceBlockLevel(mc.player).offset(dir, 2).offset(leftDir, getWidthLeft() + (m ? 1 : 0));
                }

                @Override
                public void save() {
                    pi = i;
                    pw = w;
                    py = y;
                    i = w = y = 0;

                    initPos();
                }

                @Override
                public void restore() {
                    i = pi;
                    w = pw;
                    y = py;

                    initPos();
                }
            };
        }

        @Override
        public MBPIterator getBlockade(boolean mine, BlockadeType blockadeType) {
            return new MBPIterator() {
                private int i = mine ? -1 : 0, y;
                private int pi, py;

                private MBlockPos get(int i) {
                    HorizontalDirection dir2 = dir.rotateLeft().rotateLeftSkipOne();

                    pos.coerceBlockLevel(mc.player).offset(dir2);

                    return switch (i) {
                        case -1 -> pos;
                        case 0 -> pos.offset(dir2);
                        case 1 -> pos.offset(dir2.rotateLeftSkipOne());
                        case 2 -> pos.offset(dir2.rotateLeftSkipOne().opposite());
                        case 3 -> pos.offset(dir2.opposite(), 2);
                        case 4 -> pos.offset(dir2.opposite()).offset(dir2.rotateLeftSkipOne());
                        case 5 -> pos.offset(dir2.opposite()).offset(dir2.rotateLeftSkipOne().opposite());
                        default -> throw new IllegalStateException("Unexpected value: " + i);
                    };
                }

                @Override
                public boolean hasNext() {
                    return i < blockadeType.columns && y < 2;
                }

                @Override
                public MBlockPos next() {
                    MBlockPos pos = get(i).add(0, y, 0);

                    y++;
                    if (y > 1) {
                        y = 0;
                        i++;
                    }

                    return pos;
                }

                @Override
                public void save() {
                    pi = i;
                    py = y;
                    i = y = 0;
                }

                @Override
                public void restore() {
                    i = pi;
                    y = py;
                }

                @Override
                public int placementsPerTick(HighwayBuilder b) {
                    return 1;
                }
            };
        }
    }

    public static class DoubleMineBlock {
        public static boolean rateLimited = false;
        public final BlockPos blockPos;
        public final BlockState blockState;

        private final Block block;
        private final Direction direction;
        private final HighwayBuilder b;
        private final Vector3d vec3 = new Vector3d(0);

        private int normalStartTime, packetStartTime;
        private boolean packet;

        public DoubleMineBlock(HighwayBuilder b, BlockPos pos) {
            this.b = b;
            this.blockPos = pos;
            this.blockState = b.mc.world.getBlockState(this.blockPos);
            this.block = this.blockState.getBlock();
            this.direction = BlockUtils.getDirection(pos);
            this.packet = false;
        }

        public DoubleMineBlock startDestroying() {
            b.mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, this.blockPos, this.direction));
            normalStartTime = b.mc.player.age;
            return this;
        }

        public DoubleMineBlock stopDestroying() {
            b.mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, this.blockPos, this.direction));
            return this;
        }

        public DoubleMineBlock packetMine() {
            packetStartTime = b.mc.player.age;
            packet = true;
            return stopDestroying();
        }

        public boolean isReady() {
            return progress() >= (b.fastBreak.get() ? 0.7 : 1.0);
        }

        public boolean shouldRemove() {
            boolean distance = !packet && Utils.distance(b.mc.player.getEyePos().x, b.mc.player.getEyePos().y, b.mc.player.getEyePos().z, blockPos.getX() + direction.getOffsetX(), blockPos.getY() + direction.getOffsetY(), blockPos.getZ() + direction.getOffsetZ()) > b.mc.player.getBlockInteractionRange();

            // a minimum amount of time needs to have elapsed for the timeout check to occur, otherwise it may trigger
            // when it isn't supposed to due to latency
            boolean timeout = progress() > 2 && (b.mc.player.age - (packet ? packetStartTime : normalStartTime) > 60);

            return  distance || timeout;
        }

        public double progress() {
            int slot = b.mc.player.getInventory().getSelectedSlot();
            return BlockUtils.getBreakDelta(slot , blockState) * ((b.mc.player.age - (packet ? packetStartTime : normalStartTime)) + 1);
        }

        public void renderLetter() {
            vec3.set(blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5);
            if (!NametagUtils.to2D(vec3, 2)) return;

            NametagUtils.begin(vec3);
            TextRenderer.get().begin(1.0, false, true);

            String letter = packet ? "P" : "N";
            double w = TextRenderer.get().getWidth(letter) / 2.0;
            TextRenderer.get().render(letter, -w, 0.0, Color.WHITE, true);

            TextRenderer.get().end();
            NametagUtils.end();
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

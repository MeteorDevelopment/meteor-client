/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.world;

import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.player.AutoEat;
import meteordevelopment.meteorclient.systems.modules.player.AutoTool;
import meteordevelopment.meteorclient.utils.misc.HorizontalDirection;
import meteordevelopment.meteorclient.utils.misc.MBlockPos;
import meteordevelopment.meteorclient.utils.player.CustomPlayerInput;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.meteorclient.utils.world.Dir;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.input.Input;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;

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

        public boolean mine, place;

        Rotation(boolean mine, boolean place) {
            this.mine = mine;
            this.place = place;
        }
    }

    private static final BlockPos ZERO = new BlockPos(0, 0, 0);

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRenderMine = settings.createGroup("Render Mine");
    private final SettingGroup sgRenderPlace = settings.createGroup("Render Place");

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

    private final Setting<Boolean> mineAboveRailings = sgGeneral.add(new BoolSetting.Builder()
        .name("mine-above-railings")
        .description("Mines blocks above railings.")
        .visible(railings::get)
        .defaultValue(true)
        .build()
    );

    private final Setting<Rotation> rotation = sgGeneral.add(new EnumSetting.Builder<Rotation>()
        .name("rotation")
        .description("Mode of rotation.")
        .defaultValue(Rotation.Both)
        .build()
    );

    private final Setting<List<Block>> blocksToPlace = sgGeneral.add(new BlockListSetting.Builder()
        .name("blocks-to-place")
        .description("Blocks it is allowed to place.")
        .defaultValue(Blocks.OBSIDIAN)
        .filter(block -> Block.isShapeFullCube(block.getDefaultState().getCollisionShape(mc.world, ZERO)))
        .build()
    );

    private final Setting<List<Item>> trashItems = sgGeneral.add(new ItemListSetting.Builder()
        .name("trash-items")
        .description("Items that are considered trash and can be thrown out.")
        .defaultValue(Items.NETHERRACK, Items.QUARTZ, Items.GOLD_NUGGET, Items.GLOWSTONE_DUST, Items.BLACKSTONE, Items.BASALT)
        .build()
    );

    private final Setting<Boolean> dontBreakTools = sgGeneral.add(new BoolSetting.Builder()
        .name("dont-break-tools")
        .description("Don't break tools.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> mineEnderChests = sgGeneral.add(new BoolSetting.Builder()
        .name("mine-ender-chests")
        .description("Mines ender chests for obsidian.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> disconnectOnToggle = sgGeneral.add(new BoolSetting.Builder()
        .name("disconnect-on-toggle")
        .description("Automatically disconnects when the module is turned off, for example for not having enough blocks.")
        .defaultValue(false)
        .build()
    );

    // Render Mine

    private final Setting<Boolean> renderMine = sgRenderMine.add(new BoolSetting.Builder()
        .name("render-blocks-to-mine")
        .description("Render blocks to be mined.")
        .defaultValue(true)
        .build()
    );

    private final Setting<ShapeMode> renderMineShape = sgRenderMine.add(new EnumSetting.Builder<ShapeMode>()
        .name("blocks-to-mine-shape-mode")
        .description("How the blocks to be mined are rendered.")
        .defaultValue(ShapeMode.Both)
        .build()
    );

    private final Setting<SettingColor> renderMineSideColor = sgRenderMine.add(new ColorSetting.Builder()
        .name("blocks-to-mine-side-color")
        .description("Color of blocks to be mined.")
        .defaultValue(new SettingColor(225, 25, 25, 25))
        .build()
    );

    private final Setting<SettingColor> renderMineLineColor = sgRenderMine.add(new ColorSetting.Builder()
        .name("blocks-to-mine-line-color")
        .description("Color of blocks to be mined.")
        .defaultValue(new SettingColor(225, 25, 25))
        .build()
    );

    // Render Place

    private final Setting<Boolean> renderPlace = sgRenderPlace.add(new BoolSetting.Builder()
        .name("render-blocks-to-place")
        .description("Render blocks to be placed.")
        .defaultValue(true)
        .build()
    );

    private final Setting<ShapeMode> renderPlaceShape = sgRenderPlace.add(new EnumSetting.Builder<ShapeMode>()
        .name("blocks-to-place-shape-mode")
        .description("How the blocks to be placed are rendered.")
        .defaultValue(ShapeMode.Both)
        .build()
    );

    private final Setting<SettingColor> renderPlaceSideColor = sgRenderPlace.add(new ColorSetting.Builder()
        .name("blocks-to-place-side-color")
        .description("Color of blocks to be placed.")
        .defaultValue(new SettingColor(25, 25, 225, 25))
        .build()
    );

    private final Setting<SettingColor> renderPlaceLineColor = sgRenderPlace.add(new ColorSetting.Builder()
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

    private final MBlockPos posRender2 = new MBlockPos();
    private final MBlockPos posRender3 = new MBlockPos();

    public HighwayBuilder() {
        super(Categories.World, "highway-builder", "Automatically builds highways.");
    }

    @Override
    public void onActivate() {
        dir = HorizontalDirection.get(mc.player.getYaw());
        leftDir = dir.rotateLeftSkipOne();
        rightDir = leftDir.opposite();

        prevInput = mc.player.input;
        mc.player.input = input = new CustomPlayerInput();

        state = State.Forward;
        setState(State.Center);
        blockPosProvider = dir.diagonal ? new DiagonalBlockPosProvider() : new StraightBlockPosProvider();

        start = mc.player.getPos();
        blocksBroken = blocksPlaced = 0;
        lastBreakingPos.set(0, 0, 0);
        displayInfo = true;
    }

    @Override
    public void onDeactivate() {
        mc.player.input = prevInput;

        mc.player.setYaw(dir.yaw);

        if (displayInfo) {
            info("Distance: (highlight)%.0f", mc.player.getPos().distanceTo(start));
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
        if (width.get() < 3 && dir.diagonal) {
            errorEarly("Diagonal highways with width less than 3 are not supported.");
            return;
        }

        if (Modules.get().get(AutoEat.class).eating) return;

        state.tick(this);
    }

    @EventHandler
    private void onRender3D(Render3DEvent event) {
        if (renderMine.get()) {
            render(event, blockPosProvider.getFront(), mBlockPos -> canMine(mBlockPos, true), true);
            if (floor.get() == Floor.Replace) render(event, blockPosProvider.getFloor(), mBlockPos -> canMine(mBlockPos, false), true);
            if (railings.get()) render(event, blockPosProvider.getRailings(true), mBlockPos -> canMine(mBlockPos, false), true);
            if (state == State.MineEChestBlockade) render(event, blockPosProvider.getEChestBlockade(true), mBlockPos -> canMine(mBlockPos, true), true);
        }

        if (renderPlace.get()) {
            render(event, blockPosProvider.getLiquids(), mBlockPos -> canPlace(mBlockPos, true), false);
            if (railings.get()) render(event, blockPosProvider.getRailings(false), mBlockPos -> canPlace(mBlockPos, false), false);
            render(event, blockPosProvider.getFloor(), mBlockPos -> canPlace(mBlockPos, false), false);
            if (state == State.PlaceEChestBlockade) render(event, blockPosProvider.getEChestBlockade(false), mBlockPos -> canPlace(mBlockPos, false), false);
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

                event.renderer.box(posRender2.getMcPos(), sideColor, lineColor, shapeMode, excludeDir);
            }
        }
    }

    private void setState(State state) {
        lastState = this.state;
        this.state = state;

        input.stop();
        state.start(this);
    }

    private int getWidthLeft() {
        return switch (width.get()) {
            default -> 0;
            case 2, 3 -> 1;
            case 4, 5 -> 2;
        };
    }

    private int getWidthRight() {
        return switch (width.get()) {
            default -> 0;
            case 3, 4 -> 1;
            case 5 -> 2;
        };
    }

    private boolean canMine(MBlockPos pos, boolean ignoreBlocksToPlace) {
        BlockState state = pos.getState();
        return BlockUtils.canBreak(pos.getMcPos(), state) && (ignoreBlocksToPlace || !blocksToPlace.get().contains(state.getBlock()));
    }

    private boolean canPlace(MBlockPos pos, boolean liquids) {
        return liquids ? !pos.getState().getFluidState().isEmpty() : pos.getState().isAir();
    }

    private void disconnect(String message, Object... args) {
        MutableText text = new LiteralText(String.format("%s[%s%s%s] %s", Formatting.GRAY, Formatting.BLUE, title, Formatting.GRAY, Formatting.RED) + String.format(message, args)).append("\n");
        text.append(getStatsText());

        mc.getNetworkHandler().getConnection().disconnect(text);
    }

    public MutableText getStatsText() {
        MutableText text = new LiteralText(String.format("%sDistance: %s%.0f\n", Formatting.GRAY, Formatting.WHITE, mc.player.getPos().distanceTo(start)));
        text.append(String.format("%sBlocks broken: %s%d\n", Formatting.GRAY, Formatting.WHITE, blocksBroken));
        text.append(String.format("%sBlocks placed: %s%d", Formatting.GRAY, Formatting.WHITE, blocksPlaced));

        return text;
    }

    private enum State {
        Center {
            @Override
            protected void tick(HighwayBuilder b) {
                // There is probably a much better way to do this
                double x = Math.abs(b.mc.player.getX() - (int) b.mc.player.getX()) - 0.5;
                double z = Math.abs(b.mc.player.getZ() - (int) b.mc.player.getZ()) - 0.5;

                boolean isX = Math.abs(x) <= 0.1;
                boolean isZ = Math.abs(z) <= 0.1;

                if (isX && isZ) {
                    b.mc.player.setPosition((int) b.mc.player.getX() + (b.mc.player.getX() < 0 ? -0.5 : 0.5), b.mc.player.getY(), (int) b.mc.player.getZ() + (b.mc.player.getZ() < 0 ? -0.5 : 0.5));
                    b.setState(b.lastState);
                }
                else {
                    b.mc.player.setYaw(0);

                    if (!isZ) {
                        b.input.pressingForward = z < 0;
                        b.input.pressingBack = z > 0;

                        if (b.mc.player.getZ() < 0) {
                            boolean forward = b.input.pressingForward;
                            b.input.pressingForward = b.input.pressingBack;
                            b.input.pressingBack = forward;
                        }
                    }

                    if (!isX) {
                        b.input.pressingRight = x > 0;
                        b.input.pressingLeft = x < 0;

                        if (b.mc.player.getX() < 0) {
                            boolean right = b.input.pressingRight;
                            b.input.pressingRight = b.input.pressingLeft;
                            b.input.pressingLeft = right;
                        }
                    }

                    b.input.sneaking = true;
                }
            }
        },

        Forward {
            @Override
            protected void tick(HighwayBuilder b) {
                b.mc.player.setYaw(b.dir.yaw);

                if (needsToPlace(b, b.blockPosProvider.getLiquids(), true)) b.setState(FillLiquids); // Fill Liquids
                else if (needsToMine(b, b.blockPosProvider.getFront(), true)) b.setState(MineFront); // Mine Front
                else if (b.floor.get() == Floor.Replace && needsToMine(b, b.blockPosProvider.getFloor(), false)) b.setState(MineFloor); // Mine Floor
                else if (b.railings.get() && needsToMine(b, b.blockPosProvider.getRailings(true), false)) b.setState(MineRailings); // Mine Railings
                else if (b.railings.get() && needsToPlace(b, b.blockPosProvider.getRailings(false), false)) b.setState(PlaceRailings); // Place Railings
                else if (needsToPlace(b, b.blockPosProvider.getFloor(), false)) b.setState(PlaceFloor); // Place Floor
                else b.input.pressingForward = true; // Move
            }

            private boolean needsToMine(HighwayBuilder b, MBPIterator it, boolean ignoreBlocksToPlace) {
                for (MBlockPos pos : it) {
                    if (b.canMine(pos, ignoreBlocksToPlace)) return true;
                }

                return false;
            }

            private boolean needsToPlace(HighwayBuilder b, MBPIterator it, boolean liquids) {
                for (MBlockPos pos : it) {
                    if (b.canPlace(pos, liquids)) return true;
                }

                return false;
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
            protected void tick(HighwayBuilder b) {
                mine(b, b.blockPosProvider.getFront(), true);
            }
        },

        MineFloor {
            @Override
            protected void tick(HighwayBuilder b) {
                mine(b, b.blockPosProvider.getFloor(), false);
            }
        },

        PlaceFloor {
            @Override
            protected void tick(HighwayBuilder b) {
                int slot = findBlocksToPlace(b);
                if (slot == -1) return;

                place(b, b.blockPosProvider.getFloor(), slot, Forward);
            }
        },

        MineRailings {
            @Override
            protected void tick(HighwayBuilder b) {
                mine(b, b.blockPosProvider.getRailings(true), false);
            }
        },

        PlaceRailings {
            @Override
            protected void tick(HighwayBuilder b) {
                int slot = findBlocksToPlace(b);
                if (slot == -1) return;

                place(b, b.blockPosProvider.getRailings(false), slot, Forward);
            }
        },

        ThrowOutTrash {
            private int skipSlot;
            private boolean timerEnabled, firstTick;
            private int timer;

            @Override
            protected void start(HighwayBuilder b) {
                int biggestCount = 0;

                for (int i = 0; i < b.mc.player.getInventory().main.size(); i++) {
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

                for (int i = 0; i < b.mc.player.getInventory().main.size(); i++) {
                    if (i == skipSlot) continue;

                    ItemStack itemStack = b.mc.player.getInventory().getStack(i);

                    if (b.trashItems.get().contains(itemStack.getItem())) {
                        InvUtils.drop().slot(i);
                        return;
                    }
                }

                timerEnabled = true;
                timer = 10;
            }
        },

        PlaceEChestBlockade {
            @Override
            protected void tick(HighwayBuilder b) {
                int slot = findBlocksToPlacePrioritizeTrash(b);
                if (slot == -1) return;

                place(b, b.blockPosProvider.getEChestBlockade(false), slot, MineEnderChests);
            }
        },

        MineEChestBlockade {
            @Override
            protected void tick(HighwayBuilder b) {
                mine(b, b.blockPosProvider.getEChestBlockade(true), true, Center, Forward);
            }
        },

        MineEnderChests {
            private static final MBlockPos pos = new MBlockPos();

            private int minimumObsidian;
            private boolean first;
            private int moveTimer;

            private boolean stopTimerEnabled;
            private int stopTimer;

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
                for (int i = 0; i < b.mc.player.getInventory().main.size(); i++) {
                    if (b.mc.player.getInventory().getStack(i).isEmpty()) emptySlots++;
                }

                if (emptySlots == 0) {
                    b.error("No empty slots.");
                    return;
                }

                int minimumSlots = Math.max(emptySlots - 4, 1);
                minimumObsidian = minimumSlots * 64;
                first = true;
                moveTimer = 0;

                stopTimerEnabled = false;
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
                    b.input.pressingForward = moveTimer > 2;

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

                for (int i = 0; i < b.mc.player.getInventory().main.size(); i++) {
                    ItemStack itemStack = b.mc.player.getInventory().getStack(i);
                    if (itemStack.getItem() == Items.OBSIDIAN) obsidianCount += itemStack.getCount();
                }

                if (obsidianCount >= minimumObsidian) {
                    stopTimerEnabled = true;
                    stopTimer = 8;
                    return;
                }

                // Check block state
                BlockState blockState = b.mc.world.getBlockState(pos.getMcPos());

                if (blockState.getBlock() == Blocks.ENDER_CHEST) {
                    // Mine ender chest
                    int slot = findAndMoveBestToolToHotbar(b, blockState, true, false);
                    if (slot == -1) {
                        b.error("Cannot find pickaxe with silk touch to mine ender chests.");
                        return;
                    }

                    InvUtils.swap(slot, false);
                    BlockUtils.breakBlock(pos.getMcPos(), true);
                }
                else {
                    // Place ender chest
                    int slot = findAndMoveToHotbar(b, itemStack -> itemStack.getItem() == Items.ENDER_CHEST, false);
                    if (slot == -1) {
                        stopTimerEnabled = true;
                        stopTimer = 4;
                        return;
                    }

                    if (first) {
                        moveTimer = 8;
                        first = false;
                    }

                    BlockUtils.place(pos.getMcPos(), Hand.MAIN_HAND, slot, true, 0, true, true, false);
                }
            }
        };

        protected void start(HighwayBuilder b) {}

        protected abstract void tick(HighwayBuilder b);

        protected void mine(HighwayBuilder b, MBPIterator it, boolean ignoreBlocksToPlace, State nextState, State lastState) {
            boolean breaking = false;

            for (MBlockPos pos : it) {
                BlockState state = pos.getState();
                if (state.isAir() || (!ignoreBlocksToPlace && b.blocksToPlace.get().contains(state.getBlock()))) continue;

                int slot = findAndMoveBestToolToHotbar(b, state, false, true);
                if (slot == -1) return;

                InvUtils.swap(slot, false);

                BlockPos mcPos = pos.getMcPos();
                if (BlockUtils.canBreak(mcPos)) {
                    if (b.rotation.get().mine) Rotations.rotate(Rotations.getYaw(mcPos), Rotations.getPitch(mcPos), () -> BlockUtils.breakBlock(pos.getMcPos(), true));
                    else BlockUtils.breakBlock(mcPos, true);

                    breaking = true;

                    if (!b.lastBreakingPos.equals(pos)) {
                        b.lastBreakingPos.set(pos);
                        b.blocksBroken++;
                    }

                    break;
                }
            }

            if (!breaking) {
                b.setState(nextState);
                b.lastState = lastState;
            }
        }
        protected void mine(HighwayBuilder b, MBPIterator it, boolean ignoreBlocksToPlace) {
            mine(b, it, ignoreBlocksToPlace, Forward, b.state);
        }

        protected void place(HighwayBuilder b, MBPIterator it, int slot, State nextState) {
            boolean placed = false;

            for (MBlockPos pos : it) {
                if (BlockUtils.place(pos.getMcPos(), Hand.MAIN_HAND, slot, b.rotation.get().place, 0, true, true, true)) {
                    placed = true;
                    b.blocksPlaced++;
                    break;
                }
            }

            if (!placed) b.setState(nextState);
        }

        private int findSlot(HighwayBuilder b, Predicate<ItemStack> predicate, boolean hotbar) {
            for (int i = hotbar ? 0 : 9; i < (hotbar ? 9 : b.mc.player.getInventory().main.size()); i++) {
                if (predicate.test(b.mc.player.getInventory().getStack(i))) return i;
            }

            return -1;
        }

        private int findHotbarSlot(HighwayBuilder b, boolean replaceTools) {
            int thrashSlot = -1;
            int slotsWithBlocks = 0;
            int slotWithLeastBlocks = 65;
            int slowWithLeastBlocksCount = 0;

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
                if (itemStack.getItem() instanceof BlockItem blockItem && b.blocksToPlace.get().contains(blockItem.getBlock())) {
                    slotsWithBlocks++;

                    if (itemStack.getCount() < slowWithLeastBlocksCount) {
                        slowWithLeastBlocksCount = itemStack.getCount();
                        slotWithLeastBlocks = i;
                    }
                }
            }

            // Return thrash slot if found
            if (thrashSlot != -1) return thrashSlot;

            // If there are more than 1 slots with building blocks return the slot with the lowest amount of blocks
            if (slotsWithBlocks > 1) return slotWithLeastBlocks;

            // No space found in hotbar
            b.error("No empty space in hotbar.");
            return -1;
        }

        private boolean hasItem(HighwayBuilder b, Item item) {
            for (int i = 0; i < b.mc.player.getInventory().main.size(); i++) {
                if (b.mc.player.getInventory().getStack(i).getItem() == item) return true;
            }

            return false;
        }

        protected int findAndMoveToHotbar(HighwayBuilder b, Predicate<ItemStack> predicate, boolean required) {
            // Check hotbar
            int slot = findSlot(b, predicate, true);
            if (slot != -1) return slot;

            // Find hotbar slot to move to
            int hotbarSlot = findHotbarSlot(b, false);
            if (hotbarSlot == -1) return -1;

            // Check inventory
            slot = findSlot(b, predicate, false);

            // Stop if no items were found and are required
            if (slot == -1) {
                if (required) {
                    b.error("Out of items.");
                }

                return -1;
            }

            // Move items from inventory to hotbar
            InvUtils.move().from(slot).toHotbar(hotbarSlot);
            InvUtils.dropHand();

            return hotbarSlot;
        }

        protected int findAndMoveBestToolToHotbar(HighwayBuilder b, BlockState blockState, boolean noSilkTouch, boolean error) {
            // Check for creative
            if (b.mc.player.isCreative()) return b.mc.player.getInventory().selectedSlot;

            // Find best tool
            double bestScore = -1;
            int bestSlot = -1;

            for (int i = 0; i < b.mc.player.getInventory().main.size(); i++) {
                double score = AutoTool.getScore(b.mc.player.getInventory().getStack(i), blockState, false, AutoTool.EnchantPreference.None, itemStack -> {
                    if (noSilkTouch && EnchantmentHelper.getLevel(Enchantments.SILK_TOUCH, itemStack) != 0) return false;
                    return !b.dontBreakTools.get() || itemStack.getMaxDamage() - itemStack.getDamage() > 1;
                });

                if (score > bestScore) {
                    bestScore = score;
                    bestSlot = i;
                }
            }

            // Stop if not found
            if (bestSlot == -1) {
                if (error) b.error("Failed to find suitable tool for mining.");
                return -1;
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
            int slot = findAndMoveToHotbar(b, itemStack -> itemStack.getItem() instanceof BlockItem blockItem && b.blocksToPlace.get().contains(blockItem.getBlock()), false);

            if (slot == -1) {
                if (!b.mineEnderChests.get()) {
                    b.error("Out of blocks to place.");
                }
                else {
                    if (hasItem(b, Items.ENDER_CHEST)) b.setState(MineEnderChests);
                    else b.error("Out of blocks to place.");
                }

                return -1;
            }

            return slot;
        }

        protected int findBlocksToPlacePrioritizeTrash(HighwayBuilder b) {
            int slot = findAndMoveToHotbar(b, itemStack -> {
                if (!(itemStack.getItem() instanceof BlockItem)) return false;
                return b.trashItems.get().contains(itemStack.getItem());
            }, false);

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
        MBPIterator getRailings(boolean mine);
        MBPIterator getLiquids();
        MBPIterator getEChestBlockade(boolean mine);
    }

    private class StraightBlockPosProvider implements IBlockPosProvider {
        private final MBlockPos pos = new MBlockPos();
        private final MBlockPos pos2 = new MBlockPos();

        @Override
        public MBPIterator getFront() {
            pos.set(mc.player).offset(dir).offset(leftDir, getWidthLeft());

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
            pos.set(mc.player).offset(dir).offset(leftDir, getWidthLeft()).add(0, -1, 0);

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
        public MBPIterator getRailings(boolean mine) {
            boolean mineAll = mine && mineAboveRailings.get();
            pos.set(mc.player).offset(dir);

            return new MBPIterator() {
                private int i, y;
                private int pi, py;

                @Override
                public boolean hasNext() {
                    return i < 2 && y < (mineAll ? height.get() : 1);
                }

                @Override
                public MBlockPos next() {
                    if (i == 0) pos2.set(pos).offset(leftDir, getWidthLeft() + 1).add(0, y, 0);
                    else pos2.set(pos).offset(rightDir, getWidthRight() + 1).add(0, y, 0);

                    y++;
                    if (y >= (mineAll ? height.get() : 1)) {
                        y = 0;
                        i++;
                    }

                    return pos2;
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
            };
        }

        @Override
        public MBPIterator getLiquids() {
            pos.set(mc.player).offset(dir, 2).offset(leftDir, getWidthLeft() + (railings.get() && mineAboveRailings.get() ? 2 : 1));

            return new MBPIterator() {
                private int w, y;
                private int pw, py;

                private int getWidth() {
                    return width.get() + (railings.get() && mineAboveRailings.get() ? 2 : 0);
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
        public MBPIterator getEChestBlockade(boolean mine) {
            return new MBPIterator() {
                private int i = mine ? -1 : 0, y;
                private int pi, py;

                private MBlockPos get(int i) {
                    pos.set(mc.player).offset(dir.opposite());

                    return switch (i) {
                        case -1 -> pos;
                        default -> pos.offset(dir.opposite());
                        case 1 -> pos.offset(leftDir);
                        case 2 -> pos.offset(rightDir);
                    };
                }

                @Override
                public boolean hasNext() {
                    return i < 3 && y < 2;
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
            };
        }
    }

    private class DiagonalBlockPosProvider implements IBlockPosProvider {
        private final MBlockPos pos = new MBlockPos();
        private final MBlockPos pos2 = new MBlockPos();

        @Override
        public MBPIterator getFront() {
            pos.set(mc.player).offset(dir.rotateLeft()).offset(leftDir, getWidthLeft() - 1);

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

                            pos.set(mc.player).offset(dir).offset(leftDir, getWidthLeft());
                        }
                    }

                    return pos2;
                }

                private void initPos() {
                    if (i == 0) pos.set(mc.player).offset(dir.rotateLeft()).offset(leftDir, getWidthLeft() - 1);
                    else pos.set(mc.player).offset(dir).offset(leftDir, getWidthLeft());
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
            pos.set(mc.player).add(0, -1, 0).offset(dir.rotateLeft()).offset(leftDir, getWidthLeft() - 1);

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

                        pos.set(mc.player).add(0, -1, 0).offset(dir).offset(leftDir, getWidthLeft());
                    }

                    return pos2;
                }

                private void initPos() {
                    if (i == 0) pos.set(mc.player).add(0, -1, 0).offset(dir.rotateLeft()).offset(leftDir, getWidthLeft() - 1);
                    else pos.set(mc.player).add(0, -1, 0).offset(dir).offset(leftDir, getWidthLeft());
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
        public MBPIterator getRailings(boolean mine) {
            boolean mineAll = mine && mineAboveRailings.get();
            pos.set(mc.player).offset(dir.rotateLeft()).offset(leftDir, getWidthLeft());

            return new MBPIterator() {
                private int i, y;
                private int pi, py;

                @Override
                public boolean hasNext() {
                    return i < 2 && y < (mineAll ? height.get() : 1);
                }

                @Override
                public MBlockPos next() {
                    pos2.set(pos).add(0, y++, 0);

                    if (y >= (mineAll ? height.get() : 1)) {
                        y = 0;
                        i++;

                        pos.set(mc.player).offset(dir.rotateRight()).offset(rightDir, getWidthRight());
                    }

                    return pos2;
                }

                private void initPos() {
                    if (i == 0) pos.set(mc.player).offset(dir.rotateLeft()).offset(leftDir, getWidthLeft());
                    else pos.set(mc.player).offset(dir.rotateRight()).offset(rightDir, getWidthRight());
                }

                @Override
                public void save() {
                    pi = i;
                    py = y;
                    i = y = 0;

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
            boolean m = railings.get() && mineAboveRailings.get();
            pos.set(mc.player).offset(dir).offset(dir.rotateLeft()).offset(leftDir, getWidthLeft());

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

                        pos.set(mc.player).offset(dir, 2).offset(leftDir, getWidthLeft() + (m ? 1 : 0));
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
                    if (i == 0) pos.set(mc.player).offset(dir).offset(dir.rotateLeft()).offset(leftDir, getWidthLeft());
                    else pos.set(mc.player).offset(dir, 2).offset(leftDir, getWidthLeft() + (m ? 1 : 0));
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
        public MBPIterator getEChestBlockade(boolean mine) {
            return new MBPIterator() {
                private int i = mine ? -1 : 0, y;
                private int pi, py;

                private MBlockPos get(int i) {
                    HorizontalDirection dir2 = dir.rotateLeft().rotateLeftSkipOne();

                    pos.set(mc.player).offset(dir2);

                    return switch (i) {
                        case -1 -> pos;
                        default -> pos.offset(dir2);
                        case 1 -> pos.offset(dir2.rotateLeftSkipOne());
                        case 2 -> pos.offset(dir2.rotateLeftSkipOne().opposite());
                    };
                }

                @Override
                public boolean hasNext() {
                    return i < 3 && y < 2;
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
            };
        }
    }
}

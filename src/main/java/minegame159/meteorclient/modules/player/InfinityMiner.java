/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.modules.player;

import baritone.api.BaritoneAPI;
import baritone.api.pathing.goals.GoalBlock;
import com.google.common.collect.Lists;
import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.ActiveModulesChangedEvent;
import minegame159.meteorclient.events.GameJoinedEvent;
import minegame159.meteorclient.events.GameLeftEvent;
import minegame159.meteorclient.events.PostTickEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.modules.combat.AutoLog;
import minegame159.meteorclient.modules.movement.InvMove;
import minegame159.meteorclient.modules.movement.Jesus;
import minegame159.meteorclient.modules.movement.NoFall;
import minegame159.meteorclient.settings.*;
import minegame159.meteorclient.utils.MeteorExecutor;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.ToolItem;
import net.minecraft.network.packet.s2c.play.DisconnectS2CPacket;
import net.minecraft.text.LiteralText;

import java.util.HashMap;
import java.util.List;

/**
 * @author Inclement
 * @version 1.1
 * InfinityMiner is a module which alternates between mining a target block, and a repair block.
 * This allows the user to mine indefinitely, provided they have the mending enchantment.
 */
public class InfinityMiner extends ToggleModule {
    public enum Mode {
        TARGET,
        REPAIR,
        STILL,
        HOME
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgAutoToggles = settings.createGroup("Auto Toggles");
    private final SettingGroup sgExtras = settings.createGroup("Extras");

    private final Setting<Block> targetBlock = sgGeneral.add(new BlockSetting.Builder()
            .name("target-block")
            .description("The target block to mine.")
            .defaultValue(Blocks.ANCIENT_DEBRIS)
            .build()
    );

    private final Setting<Block> repairBlock = sgGeneral.add(new BlockSetting.Builder()
            .name("repair-block")
            .description("The block mined to repair your pickaxe.")
            .defaultValue(Blocks.NETHER_QUARTZ_ORE)
            .build()
    );

    private final Setting<Double> durabilityThreshold = sgGeneral.add(new DoubleSetting.Builder()
            .name("durability-threshold")
            .description("The durability at which to start repairing as a percent of maximum durability.")
            .defaultValue(.15)
            .max(.95)
            .min(.05)
            .sliderMin(.05)
            .sliderMax(.95)
            .build());

    private final Setting<Boolean> smartModuleToggle = sgAutoToggles.add(new BoolSetting.Builder()
            .name("smart-module-toggle")
            .description("Automatically enable helpful modules.")
            .defaultValue(true)
            .build());

    private final Setting<Boolean> autoWalkHome = sgExtras.add(new BoolSetting.Builder()
            .name("walk-home")
            .description("When your inventory is full, walk home.")
            .defaultValue(false)
            .build());

    private final Setting<Boolean> autoLogOut = sgExtras.add(new BoolSetting.Builder()
            .name("log-out")
            .description("Log out when inventory is full. Will walk home first if enabled.")
            .defaultValue(false)
            .build());


    public InfinityMiner() {
        super(Category.Player, "infinity-miner", "Mine forever");
    }

    private Mode currentMode = Mode.STILL;
    private Mode secondaryMode;
    private boolean baritoneRunning = false;
    private int playerX;
    private int playerY;
    private int playerZ;
    private final HashMap<String, Boolean> originalSettings = new HashMap<>();
    private volatile Boolean BLOCKER = false;

    @Override
    public void onActivate() {
        if (smartModuleToggle.get()) {
            BLOCKER = true;
            MeteorExecutor.execute(() -> { //fixes pause issue caused by too many modules being toggled
                BaritoneAPI.getSettings().mineScanDroppedItems.value = false;
                for (ToggleModule module : getToggleModules()) {
                    originalSettings.put(module.name, module.isActive());
                    if (!module.isActive()) module.toggle();
                }
                BLOCKER = false;
            });
        }
        BaritoneAPI.getSettings().mineScanDroppedItems.value = false;
        if (mc.player != null) {
            playerX = (int) mc.player.getX();
            playerY = (int) mc.player.getY();
            playerZ = (int) mc.player.getZ();
        }
    }

    @Override
    public void onDeactivate() {
        if (smartModuleToggle.get()) {
            BLOCKER = true;
            MeteorExecutor.execute(() -> {
                for (ToggleModule module : getToggleModules()) {
                    if (originalSettings.get(module.name) != module.isActive()) module.toggle();
                }
                originalSettings.clear();
                BLOCKER = false;
            });
        }
        baritoneRequestStop();
        baritoneRunning = false;
        currentMode = Mode.STILL;
        secondaryMode = null;
    }

    @SuppressWarnings("unused")
    @EventHandler
    private final Listener<PostTickEvent> onTick = new Listener<>(event -> {
        try {
            if (mc.player == null) return;
            if (!baritoneRunning && currentMode == Mode.STILL) {
                if (autoWalkHome.get() && isInventoryFull() && secondaryMode != Mode.HOME) {
                    baritoneRequestPathHome();
                    return;
                }
                currentMode = (isTool() && getCurrentDamage() <= durabilityThreshold.get()) ? Mode.REPAIR : Mode.TARGET;
                if (currentMode == Mode.REPAIR) baritoneRequestMineRepairBlock();
                else baritoneRequestMineTargetBlock();
            } else if (autoWalkHome.get() && isInventoryFull() && secondaryMode != Mode.HOME)
                baritoneRequestPathHome();
            else if (!autoWalkHome.get() && isInventoryFull() && autoLogOut.get()) {
                this.toggle();
                requestLogout(currentMode);
            } else if (currentMode == Mode.REPAIR) {
                int REPAIR_BUFFER = 15;
                if (isTool() && getCurrentDamage() >= mc.player.getMainHandStack().getMaxDamage() - REPAIR_BUFFER) {
                    if (secondaryMode != Mode.HOME) {
                        currentMode = Mode.TARGET;
                        baritoneRequestMineTargetBlock();
                    } else {
                        currentMode = Mode.HOME;
                        baritoneRequestPathHome();
                    }
                }
            } else if (currentMode == Mode.TARGET) {
                if (isTool() && getCurrentDamage() <= durabilityThreshold.get() * mc.player.getMainHandStack().getMaxDamage()) {
                    currentMode = Mode.REPAIR;
                    baritoneRequestMineRepairBlock();
                } else if (autoWalkHome.get() && isInventoryFull()) baritoneRequestPathHome();
                else if (!autoWalkHome.get() && isInventoryFull() && autoWalkHome.get()) requestLogout(currentMode);
            } else if (currentMode == Mode.HOME) {
                if (Math.abs(mc.player.getY() - playerY) <= .5 && Math.abs(mc.player.getX() - playerX) <= .5 && Math.abs(mc.player.getZ() - playerZ) <= .5) {
                    if (autoLogOut.get()) requestLogout(currentMode);
                    this.toggle();
                } else if (isTool() && getCurrentDamage() <= durabilityThreshold.get()) currentMode = Mode.REPAIR;
            }
        } catch (Exception ignored) {
        }
    });

    @SuppressWarnings("unused")
    @EventHandler
    private final Listener<ActiveModulesChangedEvent> moduleChange = new Listener<>(event -> {
        if (!BLOCKER) {
            for (ToggleModule module : getToggleModules()) {
                if (!module.isActive()) originalSettings.remove(module.name);
            }
        }
    });

    private void baritoneRequestMineTargetBlock() {
        try {
            BaritoneAPI.getProvider().getPrimaryBaritone().getMineProcess().mine(targetBlock.get());
            baritoneRunning = true;
        } catch (Exception ignored) {
        }
    }

    private void baritoneRequestMineRepairBlock() {
        try {
            BaritoneAPI.getProvider().getPrimaryBaritone().getMineProcess().mine(repairBlock.get());
            baritoneRunning = true;
        } catch (Exception ignored) {
        }
    }

    private synchronized void baritoneRequestStop() {
        BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().cancelEverything();
        baritoneRunning = false;
        currentMode = Mode.STILL;
    }

    private void baritoneRequestPathHome() {
        if (autoWalkHome.get()) {
            baritoneRequestStop();
            secondaryMode = Mode.HOME;
            currentMode = Mode.HOME;
            BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().setGoalAndPath(new GoalBlock(playerX, playerY, playerZ));
        }
    }

    private Boolean isInventoryFull() {
        if (mc.player == null) return false;
        if (mc.player.inventory.getEmptySlot() != -1) return false;
        for (int i = 0; i < mc.player.inventory.size(); i++)
            if (mc.player.inventory.getStack(i).getItem() == targetBlock.get().asItem() &&
                    mc.player.inventory.getStack(i).getCount() < targetBlock.get().asItem().getMaxCount()) return false;
        return true;
    }

    private List<ToggleModule> getToggleModules() {
        return Lists.newArrayList(
                ModuleManager.INSTANCE.get(Jesus.class),
                ModuleManager.INSTANCE.get(NoBreakDelay.class),
                ModuleManager.INSTANCE.get(AntiHunger.class),
                ModuleManager.INSTANCE.get(AutoEat.class),
                ModuleManager.INSTANCE.get(NoFall.class),
                ModuleManager.INSTANCE.get(AutoLog.class),
                ModuleManager.INSTANCE.get(AutoTool.class),
                ModuleManager.INSTANCE.get(AutoDrop.class),
                ModuleManager.INSTANCE.get(InvMove.class)
        );
    }

    private void requestLogout(Mode mode) {
        if (mc.player != null) {
            if (mode == Mode.HOME)
                mc.player.networkHandler.onDisconnect(new DisconnectS2CPacket(new LiteralText("Infinity Miner: Inventory is Full and You Are Home")));
            else
                mc.player.networkHandler.onDisconnect(new DisconnectS2CPacket(new LiteralText("Infinity Miner: Inventory is Full")));
        }
    }

    @SuppressWarnings("unused")
    @EventHandler
    private final Listener<GameLeftEvent> onGameDisconnect = new Listener<>(event -> {
        baritoneRequestStop();
        if (this.isActive()) this.toggle();
    });

    @SuppressWarnings("unused")
    @EventHandler
    private final Listener<GameJoinedEvent> onGameJoin = new Listener<>(event -> {
        baritoneRequestStop();
        if (this.isActive()) this.toggle();
    });


    public Mode getMode() {
        return currentMode;
    }

    public Block getCurrentTarget() {
        return (currentMode == Mode.REPAIR) ? repairBlock.get() : targetBlock.get();
    }

    public int[] getHomeCoords() {
        return new int[]{playerX, playerY, playerX};
    }

    private boolean isTool() {
        return mc.player != null && mc.player.getMainHandStack() != null && mc.player.getMainHandStack().getItem() instanceof ToolItem;
    }

    private int getCurrentDamage() {
        return (mc.player != null) ? mc.player.getMainHandStack().getItem().getMaxDamage() - mc.player.getMainHandStack().getDamage() : -1;
    }
}

/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.modules.player;

import baritone.api.BaritoneAPI;
import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.*;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.modules.combat.AutoLog;
import minegame159.meteorclient.modules.movement.InvMove;
import minegame159.meteorclient.modules.movement.Jesus;
import minegame159.meteorclient.modules.movement.NoFall;
import minegame159.meteorclient.settings.*;
import minegame159.meteorclient.utils.MeteorExecutor;
import net.minecraft.item.ToolItem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

/**
 * @author Inclement
 * InfinityMiner is a module which alternates between mining a target block, and a repair block.
 * This allows the user to mine indefinitely, provided they have the mending enchantment.
 */
public class InfinityMiner extends ToggleModule {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgAutoToggles = settings.createGroup("Auto Toggles");
    private final SettingGroup sgExtras = settings.createGroup("Extras");

    private final Setting<String> targetBlock = sgGeneral.add(new StringSetting.Builder()
            .name("Target Block")
            .description("The Target Block to Mine")
            .defaultValue("ancient_debris")
            .build()
    );

    private final Setting<String> repairBlock = sgGeneral.add(new StringSetting.Builder()
            .name("Repair Block")
            .description("The Block Mined to Repair Your Pickaxe")
            .defaultValue("nether_quartz_ore")
            .build()
    );
    private final Setting<Integer> durabilityThreshold = sgGeneral.add(new IntSetting.Builder()
            .name("Durability Threshold")
            .description("The durability at which to start repairing.")
            .defaultValue(150)
            .max(500)
            .min(50)
            .sliderMin(50)
            .sliderMax(500)
            .build());

    private final Setting<Boolean> smartModuleToggle = sgAutoToggles.add(new BoolSetting.Builder()
            .name("Smart Module Toggle")
            .description("Automatically enable helpful modules")
            .defaultValue(true)
            .build());

    private final Setting<Boolean> autoWalkHome = sgExtras.add(new BoolSetting.Builder()
            .name("Walk Home")
            .description("When your inventory is full, walk home.")
            .defaultValue(false)
            .build());


    public InfinityMiner() {
        super(Category.Player, "infinity-miner", "Mine forever");
    }

    private final String BARITONE_MINE = "mine ";
    private Mode currentMode = Mode.STILL;
    private Mode secondaryMode;
    private boolean baritoneRunning = false;
    private int playerX;
    private int playerY;
    private int playerZ;
    private final HashMap<String, Boolean> originalSettings = new HashMap<>();
    private volatile Boolean BLOCKER = false;
    private volatile Boolean MINING_BLOCKER = false;

    public enum Mode {
        TARGET,
        REPAIR,
        STILL,
        HOME
    }

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
        if (mc.player != null && autoWalkHome.get()) {
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
            else if (currentMode == Mode.REPAIR) {
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
                if (isTool() && getCurrentDamage() <= durabilityThreshold.get()) {
                    currentMode = Mode.REPAIR;
                    baritoneRequestMineRepairBlock();
                } else if (autoWalkHome.get() && isInventoryFull()) baritoneRequestPathHome();
            } else if (currentMode == Mode.HOME)
                if (isTool() && getCurrentDamage() <= durabilityThreshold.get()) currentMode = Mode.REPAIR;
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
            BaritoneAPI.getProvider().getPrimaryBaritone().getCommandManager().execute(BARITONE_MINE + targetBlock.get());
            baritoneRunning = true;
        } catch (Exception ignored) {
        }
    }

    private void baritoneRequestMineRepairBlock() {
        try {
            BaritoneAPI.getProvider().getPrimaryBaritone().getCommandManager().execute(BARITONE_MINE + repairBlock.get());
            baritoneRunning = true;
        } catch (Exception ignored) {
        }

    }

    private synchronized void baritoneRequestStop() {
        BaritoneAPI.getProvider().getPrimaryBaritone().getCommandManager().execute("stop");
        baritoneRunning = false;
        currentMode = Mode.STILL;
    }

    private void baritoneRequestPathHome() {
        if (autoWalkHome.get()) {
            baritoneRequestStop();
            secondaryMode = Mode.HOME;
            currentMode = Mode.HOME;
            String BARITONE_GOTO = "goto ";
            BaritoneAPI.getProvider().getPrimaryBaritone().getCommandManager().execute(BARITONE_GOTO + playerX + " " + playerY + " " + playerZ);
        }
    }


    private Boolean isInventoryFull() {
        return mc.player != null && mc.player.inventory.getEmptySlot() == -1;
    }

    private ArrayList<ToggleModule> getToggleModules() {
        return new ArrayList<>(Arrays.asList(
                ModuleManager.INSTANCE.get(Jesus.class),
                ModuleManager.INSTANCE.get(NoBreakDelay.class),
                ModuleManager.INSTANCE.get(AntiHunger.class),
                ModuleManager.INSTANCE.get(AutoEat.class),
                ModuleManager.INSTANCE.get(NoFall.class),
                ModuleManager.INSTANCE.get(AutoLog.class),
                ModuleManager.INSTANCE.get(AutoTool.class),
                ModuleManager.INSTANCE.get(AutoDrop.class),
                ModuleManager.INSTANCE.get(InvMove.class)));
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


    public Enum<Mode> getMode() {
        return currentMode;
    }

    public String getCurrentTarget() {
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

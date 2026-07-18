/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.baritoneflow;

import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.ISerializable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;

/**
 * A single step in a {@link Flow}. Holds its own {@link Settings} (so parameters serialize and
 * render for free), a position on the editor canvas, and the ids of the nodes it points to.
 */
public class FlowNode implements ISerializable<FlowNode> {
    public final Settings settings = new Settings();
    private final SettingGroup sg = settings.getDefaultGroup();

    public final Setting<FlowNodeType> type = sg.add(new EnumSetting.Builder<FlowNodeType>()
        .name("type")
        .description("What this node makes Baritone do.")
        .defaultValue(FlowNodeType.Goto)
        .build()
    );

    public final Setting<String> label = sg.add(new StringSetting.Builder()
        .name("label")
        .description("Optional custom label shown on the node.")
        .defaultValue("")
        .build()
    );

    // Goto

    public final Setting<BlockPos> target = sg.add(new BlockPosSetting.Builder()
        .name("target")
        .description("Destination to path to.")
        .visible(() -> type.get() == FlowNodeType.Goto)
        .build()
    );

    public final Setting<Boolean> ignoreY = sg.add(new BoolSetting.Builder()
        .name("ignore-y")
        .description("Reach the X and Z of the target, ignoring its Y.")
        .defaultValue(false)
        .visible(() -> type.get() == FlowNodeType.Goto)
        .build()
    );

    // Mine

    public final Setting<List<Block>> blocks = sg.add(new BlockListSetting.Builder()
        .name("blocks")
        .description("Blocks to mine.")
        .visible(() -> type.get() == FlowNodeType.Mine)
        .build()
    );

    // Follow

    public final Setting<FollowTarget> followTarget = sg.add(new EnumSetting.Builder<FollowTarget>()
        .name("follow-target")
        .description("Who to follow.")
        .defaultValue(FollowTarget.NearestPlayer)
        .visible(() -> type.get() == FlowNodeType.Follow)
        .build()
    );

    public final Setting<String> followName = sg.add(new StringSetting.Builder()
        .name("player-name")
        .description("Name of the player to follow.")
        .defaultValue("")
        .visible(() -> type.get() == FlowNodeType.Follow && followTarget.get() == FollowTarget.PlayerByName)
        .build()
    );

    // Wait

    public final Setting<Double> seconds = sg.add(new DoubleSetting.Builder()
        .name("seconds")
        .description("For Wait: how long to pause (e.g. 1800 for 30 minutes). For Follow: how long to follow before continuing (0 keeps following in the background).")
        .defaultValue(3)
        .min(0)
        .sliderRange(0, 1800)
        .visible(() -> type.get() == FlowNodeType.Wait || type.get() == FlowNodeType.Follow)
        .build()
    );

    // Command

    public final Setting<String> command = sg.add(new StringSetting.Builder()
        .name("command")
        .description("Raw Baritone command without the prefix, e.g. \"farm 30\" or \"build house\".")
        .defaultValue("")
        .visible(() -> type.get() == FlowNodeType.Command)
        .build()
    );

    // Module (any Meteor Client module, not just Baritone)

    public final Setting<ModuleAction> moduleAction = sg.add(new EnumSetting.Builder<ModuleAction>()
        .name("module-action")
        .description("What to do with the target module.")
        .defaultValue(ModuleAction.Toggle)
        .visible(() -> type.get() == FlowNodeType.Module)
        .build()
    );

    public final Setting<List<Module>> targetModule = sg.add(new ModuleListSetting.Builder()
        .name("module")
        .description("The module to control. Only the first selected module is used.")
        .visible(() -> type.get() == FlowNodeType.Module)
        .build()
    );

    public final Setting<String> moduleSettingName = sg.add(new StringSetting.Builder()
        .name("setting-name")
        .description("Name of the setting to change, e.g. \"target-block\" or \"delay\".")
        .defaultValue("")
        .visible(() -> type.get() == FlowNodeType.Module && moduleAction.get() == ModuleAction.SetSetting)
        .build()
    );

    public final Setting<String> moduleSettingValue = sg.add(new StringSetting.Builder()
        .name("setting-value")
        .description("New value for the setting, in the same text format you'd use in chat/commands.")
        .defaultValue("")
        .visible(() -> type.get() == FlowNodeType.Module && moduleAction.get() == ModuleAction.SetSetting)
        .build()
    );

    // Trigger: On Player Near Appear

    public final Setting<Integer> triggerRange = sg.add(new IntSetting.Builder()
        .name("range")
        .description("How close a player has to appear to fire this trigger.")
        .defaultValue(32)
        .min(1)
        .sliderMax(64)
        .visible(() -> type.get() == FlowNodeType.OnPlayerNearAppear)
        .build()
    );

    public final Setting<Boolean> ignoreFriends = sg.add(new BoolSetting.Builder()
        .name("ignore-friends")
        .description("Don't fire when the player who appeared is on your friends list.")
        .defaultValue(true)
        .visible(() -> type.get() == FlowNodeType.OnPlayerNearAppear)
        .build()
    );

    public int id;
    public int x, y; // Position on the editor canvas
    public final List<Integer> children = new ArrayList<>();

    public FlowNode(int id, int x, int y) {
        this.id = id;
        this.x = x;
        this.y = y;
    }

    public FlowNode(Tag tag) {
        fromTag((CompoundTag) tag);
    }

    /** Short text shown in the node header on the canvas. */
    public String title() {
        String custom = label.get();
        if (custom != null && !custom.isBlank()) return custom;
        return type.get().label();
    }

    /** One-line summary of the node's key parameter, shown in the node body. */
    public String summary() {
        return switch (type.get()) {
            case Goto -> {
                BlockPos p = target.get();
                yield p.getX() + ", " + (ignoreY.get() ? "~" : p.getY()) + ", " + p.getZ();
            }
            case Mine -> blocks.get().isEmpty() ? "(no blocks)" : blocks.get().size() + " block(s)";
            case Follow -> followTarget.get() == FollowTarget.PlayerByName ? followName.get() : followTarget.get().toString();
            case Wait -> seconds.get() + "s";
            case Command -> command.get().isBlank() ? "(empty)" : command.get();
            case Module -> moduleSummary();
            case OnPlayerNearAppear -> "range " + triggerRange.get() + (ignoreFriends.get() ? ", friends ignored" : "");
            case Leave -> "Disconnects from the server";
            case Reconnect -> "Reconnects to the last server";
            default -> "";
        };
    }

    private String moduleSummary() {
        String modName = targetModule.get().isEmpty() ? "(no module)" : targetModule.get().getFirst().title;

        return switch (moduleAction.get()) {
            case Enable -> "Enable " + modName;
            case Disable -> "Disable " + modName;
            case Toggle -> "Toggle " + modName;
            case SetSetting -> modName + "." + moduleSettingName.get() + " = " + moduleSettingValue.get();
        };
    }

    @Override
    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();

        tag.putInt("id", id);
        tag.putInt("x", x);
        tag.putInt("y", y);

        int[] c = new int[children.size()];
        for (int i = 0; i < c.length; i++) c[i] = children.get(i);
        tag.putIntArray("children", c);

        tag.put("settings", settings.toTag());

        return tag;
    }

    @Override
    public FlowNode fromTag(CompoundTag tag) {
        id = tag.getIntOr("id", 0);
        x = tag.getIntOr("x", 0);
        y = tag.getIntOr("y", 0);

        children.clear();
        for (int v : tag.getIntArray("children").orElse(new int[0])) children.add(v);

        if (tag.contains("settings")) settings.fromTag(tag.getCompoundOrEmpty("settings"));

        return this;
    }

    public enum FollowTarget {
        NearestPlayer,
        NearestEntity,
        PlayerByName
    }

    public enum ModuleAction {
        Enable,
        Disable,
        Toggle,
        SetSetting
    }
}

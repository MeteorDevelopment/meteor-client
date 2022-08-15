/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.commands.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.systems.commands.Command;
import meteordevelopment.meteorclient.systems.commands.arguments.WaypointArgumentType;
import meteordevelopment.meteorclient.systems.waypoints.Waypoint;
import meteordevelopment.meteorclient.systems.waypoints.Waypoints;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import net.minecraft.command.CommandSource;
import net.minecraft.util.Formatting;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class WaypointCommand extends Command {
    public WaypointCommand() {
        super("waypoint", "Manages waypoints.", "wp");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(literal("list").executes(context -> {
            if (Waypoints.get().waypoints.isEmpty()) error("No created waypoints.");
            else {
                info(Formatting.WHITE + "Created Waypoints:");
                for (Waypoint waypoint : Waypoints.get()) {
                    info("Name: (highlight)'%s'(default), Dimension: (highlight)%s(default), Pos: (highlight)%s(default)", waypoint.nameSetting.get(), waypoint.dimensionSetting.get(), waypointPos(waypoint));
                }
            }
            return SINGLE_SUCCESS;
        }));

        builder.then(literal("get").then(argument("waypoint", WaypointArgumentType.waypoint()).executes(context -> {
            Waypoint waypoint = WaypointArgumentType.getWaypoint(context, "waypoint");
            info("Name: " + Formatting.WHITE + waypoint.nameSetting.get());
            info("Actual Dimension: " + Formatting.WHITE + waypoint.dimensionSetting.get());
            info("Position: " + Formatting.WHITE + waypointFullPos(waypoint));
            info("Visible: " + (waypoint.visibleSetting.get() ? Formatting.GREEN + "True" : Formatting.RED + "False"));
            return SINGLE_SUCCESS;
        })));

        builder.then(literal("add").then(argument("waypoint", StringArgumentType.greedyString()).executes(context -> {
            if (mc.player == null) return -1;

            Waypoint waypoint = new Waypoint.Builder()
                .name(StringArgumentType.getString(context, "waypoint"))
                .pos(mc.player.getBlockPos().up(2))
                .dimension(PlayerUtils.getDimension())
                .build();

            Waypoints.get().add(waypoint);

            info("Created waypoint with name: (highlight)%s(default)", waypoint.nameSetting.get());
            return SINGLE_SUCCESS;
        })));

        builder.then(literal("delete").then(argument("waypoint", WaypointArgumentType.waypoint()).executes(context -> {
            Waypoint waypoint = WaypointArgumentType.getWaypoint(context, "waypoint");

            info("The waypoint (highlight)'%s'(default) has been deleted.", waypoint.nameSetting.get());

            Waypoints.get().remove(waypoint);

            return SINGLE_SUCCESS;
        })));

        builder.then(literal("toggle").then(argument("waypoint", WaypointArgumentType.waypoint()).executes(context -> {
            Waypoint waypoint = WaypointArgumentType.getWaypoint(context, "waypoint");
            waypoint.visibleSetting.set(!waypoint.visibleSetting.get());

            Waypoints.get().save();
            return SINGLE_SUCCESS;
        })));
    }

    private String waypointPos(Waypoint waypoint) {
        return "X: " + waypoint.posSetting.get().getX() + " Z: " + waypoint.posSetting.get().getZ();
    }

    private String waypointFullPos(Waypoint waypoint) {
        return "X: " + waypoint.posSetting.get().getX() +  ", Y: " + waypoint.posSetting.get().getY() + ", Z: " + waypoint.posSetting.get().getZ();
    }
}

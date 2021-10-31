/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
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
                    info("Name: (highlight)'%s'(default), Dimension: (highlight)%s(default), Pos: (highlight)%s(default)", waypoint.name, waypoint.actualDimension, waypointPos(waypoint));
                }
            }
            return SINGLE_SUCCESS;
        }));

        builder.then(literal("get").then(argument("waypoint", WaypointArgumentType.waypoint()).executes(context -> {
            Waypoint waypoint = WaypointArgumentType.getWaypoint(context, "waypoint");
            info("Name: " + Formatting.WHITE + waypoint.name);
            info("Actual Dimension: " + Formatting.WHITE + waypoint.actualDimension);
            info("Position: " + Formatting.WHITE + waypointFullPos(waypoint));
            info("Visible: " + (waypoint.visible ? Formatting.GREEN + "True" : Formatting.RED + "False"));
            return SINGLE_SUCCESS;
        })));

        builder.then(literal("add").then(argument("waypoint", StringArgumentType.greedyString()).executes(context -> {
            if (mc.player == null) return -1;
            Waypoint waypoint = new Waypoint() {{
                name = StringArgumentType.getString(context, "waypoint");
                actualDimension = PlayerUtils.getDimension();

                x = (int) mc.player.getX();
                y = (int) mc.player.getY() + 1;
                z = (int) mc.player.getZ();

                switch (actualDimension) {
                    case Overworld -> overworld = true;
                    case Nether -> nether = true;
                    case End -> end = true;
                }
            }};

            Waypoints.get().add(waypoint);
            Waypoints.get().save();

            info("Created waypoint with name: (highlight)%s(default)", waypoint.name);
            return SINGLE_SUCCESS;
        })));

        builder.then(literal("delete").then(argument("waypoint", WaypointArgumentType.waypoint()).executes(context -> {
            Waypoint waypoint = WaypointArgumentType.getWaypoint(context, "waypoint");

            info("The waypoint (highlight)'%s'(default) has been deleted.", waypoint.name);

            Waypoints.get().remove(waypoint);
            Waypoints.get().save();

            return SINGLE_SUCCESS;
        })));

        builder.then(literal("toggle").then(argument("waypoint", WaypointArgumentType.waypoint()).executes(context -> {
            Waypoint waypoint = WaypointArgumentType.getWaypoint(context, "waypoint");
            waypoint.visible = !waypoint.visible;

            Waypoints.get().save();
            return SINGLE_SUCCESS;
        })));
    }

    private String waypointPos(Waypoint waypoint) {
        return "X: " + waypoint.x + " Z: " + waypoint.z;
    }

    private String waypointFullPos(Waypoint waypoint) {
        return "X: " + waypoint.x +  ", Y: " + waypoint.y + ", Z: " + waypoint.z;
    }
}

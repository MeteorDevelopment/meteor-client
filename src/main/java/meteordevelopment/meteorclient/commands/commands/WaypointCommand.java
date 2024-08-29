/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.commands.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.commands.arguments.WaypointArgumentType;
import meteordevelopment.meteorclient.systems.waypoints.Waypoint;
import meteordevelopment.meteorclient.systems.waypoints.Waypoints;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.PosArgument;
import net.minecraft.command.argument.Vec3ArgumentType;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

public class WaypointCommand extends Command {
    public WaypointCommand() {
        super("waypoint", "Manages waypoints.", "wp");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(literal("list").executes(context -> {
            if (Waypoints.get().isEmpty()) error("No created waypoints.");
            else {
                info(Formatting.WHITE + "Created Waypoints:");
                for (Waypoint waypoint : Waypoints.get()) {
                    info("Name: (highlight)'%s'(default), Dimension: (highlight)%s(default), Pos: (highlight)%s(default)", waypoint.name.get(), waypoint.dimension.get(), waypointPos(waypoint));
                }
            }
            return SINGLE_SUCCESS;
        }));

        builder.then(literal("get").then(argument("waypoint", WaypointArgumentType.create()).executes(context -> {
            Waypoint waypoint = WaypointArgumentType.get(context);
            info("Name: " + Formatting.WHITE + waypoint.name.get());
            info("Actual Dimension: " + Formatting.WHITE + waypoint.dimension.get());
            info("Position: " + Formatting.WHITE + waypointFullPos(waypoint));
            info("Visible: " + (waypoint.visible.get() ? Formatting.GREEN + "True" : Formatting.RED + "False"));
            return SINGLE_SUCCESS;
        })));

        builder.then(literal("add")
            .then(argument("pos", Vec3ArgumentType.vec3())
                .then(argument("waypoint", StringArgumentType.greedyString()).executes(context -> addWaypoint(context, true)))
            )

            .then(argument("waypoint", StringArgumentType.greedyString()).executes(context -> addWaypoint(context, false)))
        );

        builder.then(literal("delete").then(argument("waypoint", WaypointArgumentType.create()).executes(context -> {
            Waypoint waypoint = WaypointArgumentType.get(context);

            info("The waypoint (highlight)'%s'(default) has been deleted.", waypoint.name.get());

            Waypoints.get().remove(waypoint);

            return SINGLE_SUCCESS;
        })));

        builder.then(literal("toggle").then(argument("waypoint", WaypointArgumentType.create()).executes(context -> {
            Waypoint waypoint = WaypointArgumentType.get(context);
            waypoint.visible.set(!waypoint.visible.get());

            Waypoints.get().save();
            return SINGLE_SUCCESS;
        })));
    }

    private String waypointPos(Waypoint waypoint) {
        return "X: " + waypoint.pos.get().getX() + " Z: " + waypoint.pos.get().getZ();
    }

    private String waypointFullPos(Waypoint waypoint) {
        return "X: " + waypoint.pos.get().getX() + ", Y: " + waypoint.pos.get().getY() + ", Z: " + waypoint.pos.get().getZ();
    }

    private int addWaypoint(CommandContext<CommandSource> context, boolean withCoords) {
        if (mc.player == null) return -1;

        BlockPos pos = withCoords ? context.getArgument("pos", PosArgument.class).toAbsoluteBlockPos(mc.player.getCommandSource()) : mc.player.getBlockPos().up(2);
        Waypoint waypoint = new Waypoint.Builder()
            .name(StringArgumentType.getString(context, "waypoint"))
            .pos(pos)
            .dimension(PlayerUtils.getDimension())
            .build();

        Waypoints.get().add(waypoint);

        info("Created waypoint with name: (highlight)%s(default)", waypoint.name.get());
        return SINGLE_SUCCESS;
    }
}

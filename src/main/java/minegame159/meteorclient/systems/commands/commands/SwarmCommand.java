/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.systems.commands.commands;

import baritone.api.BaritoneAPI;
import baritone.api.pathing.goals.GoalXZ;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import minegame159.meteorclient.systems.commands.Command;
import minegame159.meteorclient.systems.commands.arguments.ModuleArgumentType;
import minegame159.meteorclient.systems.commands.arguments.PlayerArgumentType;
import minegame159.meteorclient.systems.modules.Module;
import minegame159.meteorclient.systems.modules.Modules;
import minegame159.meteorclient.systems.modules.misc.Swarm;
import minegame159.meteorclient.systems.modules.world.InfinityMiner;
import minegame159.meteorclient.utils.player.ChatUtils;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.BlockStateArgument;
import net.minecraft.command.argument.BlockStateArgumentType;
import net.minecraft.entity.player.PlayerEntity;

import java.util.Random;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class SwarmCommand extends Command {
    public SwarmCommand() {
        super("swarm", "Sends commands to connected swarm accouns.");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(literal("close").executes(context -> {
                    try {
                        Swarm swarm = Modules.get().get(Swarm.class);
                        if(swarm.isActive()) {
                            swarm.closeAllServerConnections();
                            swarm.currentMode = Swarm.Mode.Idle;
                            if (BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().isPathing())
                                BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().cancelEverything();
                            if (Modules.get().isActive(Swarm.class))
                                Modules.get().get(Swarm.class).toggle();
                        }
                    } catch (Exception ignored) {
                    }
                    return SINGLE_SUCCESS;
                })
        );

        builder.then(literal("escape").executes(context -> {
                    Swarm swarm = Modules.get().get(Swarm.class);
                    if (swarm.isActive()) {
                        if (swarm.currentMode != Swarm.Mode.Queen) {
                            swarm.closeAllServerConnections();
                            if (BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().isPathing())
                                BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().cancelEverything();
                            swarm.currentMode = Swarm.Mode.Idle;
                            Modules.get().get(Swarm.class).toggle();
                        } else {
                            ChatUtils.moduleInfo(Modules.get().get(Swarm.class), "You are the queen.");
                        }
                    }
                    return SINGLE_SUCCESS;
                })
        );

        builder.then(literal("follow").executes(context -> {
                    Swarm swarm = Modules.get().get(Swarm.class);
                    if (swarm.currentMode == Swarm.Mode.Queen && swarm.server != null && mc.player != null) {
                        swarm.server.sendMessage(context.getInput() + " " + mc.player.getDisplayName().getString());
                    }
                    return SINGLE_SUCCESS;
                }).then(argument("player", PlayerArgumentType.player()).executes(context -> {
                    PlayerEntity playerEntity = PlayerArgumentType.getPlayer(context);
                    Swarm swarm = Modules.get().get(Swarm.class);
                    if (swarm.currentMode == Swarm.Mode.Queen && swarm.server != null) {
                        swarm.server.sendMessage(context.getInput());
                    } else {
                        if (playerEntity != null) {
                            BaritoneAPI.getProvider().getPrimaryBaritone().getFollowProcess().follow(entity -> entity.getDisplayName().asString().equalsIgnoreCase(playerEntity.getDisplayName().asString()));
                        }
                    }
                    return SINGLE_SUCCESS;
                })
                )
        );

        builder.then(literal("goto")
                .then(argument("x", IntegerArgumentType.integer())
                        .then(argument("z", IntegerArgumentType.integer()).executes(context -> {
                                    int x = context.getArgument("x", Integer.class);
                                    int z = context.getArgument("z", Integer.class);
                                    Swarm swarm = Modules.get().get(Swarm.class);
                                    if (swarm.isActive()) {
                                        if (swarm.currentMode == Swarm.Mode.Queen && swarm.server != null) {
                                            swarm.server.sendMessage(context.getInput());
                                        } else if (swarm.currentMode != Swarm.Mode.Queen) {
                                            BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().setGoalAndPath(new GoalXZ(x, z));
                                        }
                                    }
                                    return SINGLE_SUCCESS;
                                })
                        )
                )
        );

        builder.then(literal("im").executes(context -> {
                    Swarm swarm = Modules.get().get(Swarm.class);
                    if (swarm.isActive()) {
                        if (swarm.currentMode == Swarm.Mode.Queen) {
                            swarm.server.sendMessage(context.getInput());
                        } else {
                            runInfinityMiner();
                        }
                    }
                    return SINGLE_SUCCESS;
                }).then(argument("target", BlockStateArgumentType.blockState()).executes(context -> {
                    Swarm swarm = Modules.get().get(Swarm.class);
                    if (swarm.isActive()) {
                        if (swarm.currentMode == Swarm.Mode.Queen) {
                            swarm.server.sendMessage(context.getInput());
                        } else {
                            Modules.get().get(InfinityMiner.class).targetBlock.set(context.getArgument("target", BlockStateArgument.class).getBlockState().getBlock());
                            runInfinityMiner();
                        }
                    }
                    return SINGLE_SUCCESS;
                }).then(argument("repair", BlockStateArgumentType.blockState()).executes(context -> {
                    Swarm swarm = Modules.get().get(Swarm.class);
                    if (swarm.isActive()) {
                        if (swarm.currentMode == Swarm.Mode.Queen) {
                            swarm.server.sendMessage(context.getInput());
                        } else {
                            Modules.get().get(InfinityMiner.class).targetBlock.set(context.getArgument("target", BlockStateArgument.class).getBlockState().getBlock());
                            Modules.get().get(InfinityMiner.class).repairBlock.set(context.getArgument("repair", BlockStateArgument.class).getBlockState().getBlock());
                            runInfinityMiner();
                        }
                    }
                    return SINGLE_SUCCESS;
                })))
                        .then(literal("logout").then(argument("autologout", BoolArgumentType.bool()).executes(context -> {
                            Swarm swarm = Modules.get().get(Swarm.class);
                            if (swarm.isActive()) {
                                if (swarm.currentMode == Swarm.Mode.Queen) {
                                    swarm.server.sendMessage(context.getInput());
                                } else {
                                    boolean bool = context.getArgument("autologout", Boolean.class);
                                    InfinityMiner infinityMiner = Modules.get().get(InfinityMiner.class);
                                    infinityMiner.autoLogOut.set(bool);
                                }
                            }
                            return SINGLE_SUCCESS;
                        })))
                        .then(literal("walkhome").then(argument("walkhome", BoolArgumentType.bool()).executes(context -> {
                            Swarm swarm = Modules.get().get(Swarm.class);
                            if (swarm.isActive()) {
                                if (swarm.currentMode == Swarm.Mode.Queen) {
                                    swarm.server.sendMessage(context.getInput());
                                } else {
                                    boolean bool = context.getArgument("walkhome", Boolean.class);
                                    InfinityMiner infinityMiner = Modules.get().get(InfinityMiner.class);
                                    infinityMiner.autoWalkHome.set(bool);
                                }
                            }
                            return SINGLE_SUCCESS;
                        })))
        );

        builder.then(literal("mine")
                .then(argument("block", BlockStateArgumentType.blockState())
                        .executes(context -> {
                            try {
                                Swarm swarm = Modules.get().get(Swarm.class);
                                if (swarm.isActive()) {
                                    if (swarm.currentMode == Swarm.Mode.Queen && swarm.server != null)
                                        swarm.server.sendMessage(context.getInput());
                                    if (swarm.currentMode != Swarm.Mode.Queen) {
                                        swarm.targetBlock = context.getArgument("block",BlockStateArgument.class).getBlockState();
                                    } else ChatUtils.moduleError(Modules.get().get(Swarm.class),"Null block");
                                }
                            } catch (Exception e) {
                                ChatUtils.moduleError(Modules.get().get(Swarm.class),"Error in baritone command. " + e.getClass().getSimpleName());
                            }
                            return SINGLE_SUCCESS;
                        })
                )
        );

        builder.then(literal("module").then(argument("m", ModuleArgumentType.module()).then(argument("bool", BoolArgumentType.bool()).executes(context -> {
            Swarm swarm = Modules.get().get(Swarm.class);
            if (swarm.currentMode == Swarm.Mode.Queen && swarm.server != null) {
                swarm.server.sendMessage(context.getInput());
            } else {
                Module module = context.getArgument("m", Module.class);
                if (module.isActive() != context.getArgument("bool", Boolean.class)) {
                    module.toggle();
                }
            }
            return SINGLE_SUCCESS;
        }))));

        builder.then(literal("queen").executes(context -> {
                    Swarm swarm = Modules.get().get(Swarm.class);
                    if (swarm.isActive()) {
                        if (swarm.server == null)
                            swarm.runServer();
                    }
                    return SINGLE_SUCCESS;
                })

        );

        builder.then(literal("release").executes(context -> {
                    Swarm swarm = Modules.get().get(Swarm.class);
                    if (swarm.isActive()) {
                        if (swarm.currentMode == Swarm.Mode.Queen && swarm.server != null) {
                            swarm.server.sendMessage("s stop");
                            swarm.server.closeAllClients();
                        }
                    }
                    return SINGLE_SUCCESS;
                })
        );

        builder.then(literal("scatter").executes(context -> {
            Swarm swarm = Modules.get().get(Swarm.class);
            if(swarm.isActive()){
                if(swarm.currentMode == Swarm.Mode.Queen && swarm.server != null){
                    swarm.server.sendMessage(context.getInput());
                }
                else{
                    scatter(100);
                }
            }
            return SINGLE_SUCCESS;
        }).then(argument("radius", IntegerArgumentType.integer()).executes(context -> {
            Swarm swarm = Modules.get().get(Swarm.class);
            if(swarm.isActive()){
                if(swarm.currentMode == Swarm.Mode.Queen && swarm.server != null){
                    swarm.server.sendMessage(context.getInput());
                }
                else{
                    scatter(context.getArgument("radius",Integer.class));
                }
            }
            return SINGLE_SUCCESS;
        })));

        builder.then(literal("slave").executes(context -> {
                    Swarm swarm = Modules.get().get(Swarm.class);
                    if (swarm.isActive()) {
                        if (swarm.client == null)
                            swarm.runClient();
                    }
                    return SINGLE_SUCCESS;
                })
        );

        builder.then(literal("stop").executes(context -> {
            Swarm swarm = Modules.get().get(Swarm.class);
            if(swarm.isActive()) {
                if (swarm.currentMode == Swarm.Mode.Queen && swarm.server != null) {
                    swarm.server.sendMessage(context.getInput());
                } else {
                    swarm.idle();
                }
            }
            return SINGLE_SUCCESS;
        }));
    }

    private void runInfinityMiner() {
        InfinityMiner infinityMiner = Modules.get().get(InfinityMiner.class);
        if (infinityMiner.isActive()) infinityMiner.toggle();
        infinityMiner.smartModuleToggle.set(true);
        if (!infinityMiner.isActive()) infinityMiner.toggle();
    }

    private void scatter(int radius) {
        if(mc.player != null) {
            Random random = new Random();
            double a = random.nextDouble() * 2 * Math.PI;
            double r = radius * Math.sqrt(random.nextDouble());
            double x = mc.player.getX() + r * Math.cos(a);
            double z = mc.player.getZ() + r * Math.sin(a);
            if(BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().isPathing())
                BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().cancelEverything();
            BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().setGoalAndPath(new GoalXZ((int)x,(int)z));
        }
    }
}

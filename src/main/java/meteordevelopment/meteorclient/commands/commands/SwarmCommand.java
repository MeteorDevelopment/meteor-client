/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.commands.commands;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import it.unimi.dsi.fastutil.objects.ObjectIntImmutablePair;
import it.unimi.dsi.fastutil.objects.ObjectIntPair;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.commands.arguments.ModuleArgumentType;
import meteordevelopment.meteorclient.commands.arguments.PlayerArgumentType;
import meteordevelopment.meteorclient.pathing.PathManagers;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.misc.swarm.Swarm;
import meteordevelopment.meteorclient.systems.modules.misc.swarm.SwarmConnection;
import meteordevelopment.meteorclient.systems.modules.misc.swarm.SwarmWorker;
import meteordevelopment.meteorclient.systems.modules.world.InfinityMiner;
import meteordevelopment.meteorclient.utils.misc.text.MeteorClickEvent;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.blocks.BlockInput;
import net.minecraft.commands.arguments.blocks.BlockStateArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Random;

public class SwarmCommand extends Command {

    private final static SimpleCommandExceptionType SWARM_NOT_ACTIVE = new SimpleCommandExceptionType(Component.literal("The swarm module must be active to use this command."));
    private @Nullable ObjectIntPair<String> pendingConnection;

    public SwarmCommand() {
        super("swarm", "Sends commands to connected swarm workers.");
    }

    @Override
    public void build(LiteralArgumentBuilder<SharedSuggestionProvider> builder) {
        builder.then(literal("disconnect").executes(context -> {
            Swarm swarm = Modules.get().get(Swarm.class);
            if (swarm.isActive()) {
                swarm.close();
            } else {
                throw SWARM_NOT_ACTIVE.create();
            }

            return SINGLE_SUCCESS;
        }));

        builder.then(literal("join")
            .then(argument("ip", StringArgumentType.string())
                .then(argument("port", IntegerArgumentType.integer(0, 65535))
                    .executes(context -> {
                        String ip = StringArgumentType.getString(context, "ip");
                        int port = IntegerArgumentType.getInteger(context, "port");

                        pendingConnection = new ObjectIntImmutablePair<>(ip, port);

                        info("Are you sure you want to connect to '%s:%s'?", ip, port);
                        info(Component.literal("Click here to confirm").setStyle(Style.EMPTY
                            .applyFormats(ChatFormatting.UNDERLINE, ChatFormatting.GREEN)
                            .withClickEvent(new MeteorClickEvent(".swarm join confirm"))
                        ));

                        return SINGLE_SUCCESS;
                    })
                )
            )
            .then(literal("confirm").executes(ctx -> {
                if (pendingConnection == null) {
                    error("No pending swarm connections.");
                    return SINGLE_SUCCESS;
                }

                Swarm swarm = Modules.get().get(Swarm.class);
                swarm.enable();

                swarm.close();
                swarm.mode.set(Swarm.Mode.Worker);
                swarm.worker = new SwarmWorker(pendingConnection.left(), pendingConnection.rightInt());

                pendingConnection = null;

                try {
                    info("Connected to (highlight)%s.", swarm.worker.getConnection());
                } catch (NullPointerException e) {
                    error("Error connecting to swarm host.");
                    swarm.close();
                    swarm.toggle();
                }

                return SINGLE_SUCCESS;
            }))
        );

        builder.then(literal("connections").executes(context -> {
            Swarm swarm = Modules.get().get(Swarm.class);
            if (swarm.isActive()) {
                if (swarm.isHost()) {
                    if (swarm.host.getConnectionCount() > 0) {
                        ChatUtils.info("--- Swarm Connections (highlight)(%s/%s)(default) ---", swarm.host.getConnectionCount(), swarm.host.getConnections().length);

                        for (int i = 0; i < swarm.host.getConnections().length; i++) {
                            SwarmConnection connection = swarm.host.getConnections()[i];
                            if (connection != null)
                                ChatUtils.info("(highlight)Worker %s(default): %s.", i, connection.getConnection());
                        }
                    } else {
                        warning("No active connections");
                    }
                } else if (swarm.isWorker()) {
                    info("Connected to (highlight)%s", swarm.worker.getConnection());
                }
            } else {
                throw SWARM_NOT_ACTIVE.create();
            }

            return SINGLE_SUCCESS;
        }));

        builder.then(literal("follow").executes(context -> {
                Swarm swarm = Modules.get().get(Swarm.class);
                if (swarm.isActive()) {
                    if (swarm.isHost()) {
                        swarm.host.sendMessage(context.getInput() + " " + mc.player.getName().getString());
                    } else if (swarm.isWorker()) {
                        error("The follow host command must be used by the host.");
                    }
                } else {
                    throw SWARM_NOT_ACTIVE.create();
                }

                return SINGLE_SUCCESS;
            }).then(argument("player", PlayerArgumentType.create()).executes(context -> {
                Player playerEntity = PlayerArgumentType.get(context);

                Swarm swarm = Modules.get().get(Swarm.class);
                if (swarm.isActive()) {
                    if (swarm.isHost()) {
                        swarm.host.sendMessage(context.getInput());
                    } else if (swarm.isWorker() && playerEntity != null) {
                        PathManagers.get().follow(entity -> entity.getName().getString().equalsIgnoreCase(playerEntity.getName().getString()));
                    }
                } else {
                    throw SWARM_NOT_ACTIVE.create();
                }
                return SINGLE_SUCCESS;
            }))
        );

        builder.then(literal("goto")
            .then(argument("x", IntegerArgumentType.integer())
                .then(argument("z", IntegerArgumentType.integer()).executes(context -> {
                    Swarm swarm = Modules.get().get(Swarm.class);
                    if (swarm.isActive()) {
                        if (swarm.isHost()) {
                            swarm.host.sendMessage(context.getInput());
                        } else if (swarm.isWorker()) {
                            int x = IntegerArgumentType.getInteger(context, "x");
                            int z = IntegerArgumentType.getInteger(context, "z");

                            PathManagers.get().moveTo(new BlockPos(x, 0, z), true);
                        }
                    } else {
                        throw SWARM_NOT_ACTIVE.create();
                    }
                    return SINGLE_SUCCESS;
                }))
            )
        );

        builder.then(literal("infinity-miner").executes(context -> {
                Swarm swarm = Modules.get().get(Swarm.class);
                if (swarm.isActive()) {
                    if (swarm.isHost()) {
                        swarm.host.sendMessage(context.getInput());
                    } else if (swarm.isWorker()) {
                        runInfinityMiner();
                    }
                } else {
                    throw SWARM_NOT_ACTIVE.create();
                }
                return SINGLE_SUCCESS;
            })
            .then(argument("target", BlockStateArgument.block(REGISTRY_ACCESS)).executes(context -> {
                    Swarm swarm = Modules.get().get(Swarm.class);
                    if (swarm.isActive()) {
                        if (swarm.isHost()) {
                            swarm.host.sendMessage(context.getInput());
                        } else if (swarm.isWorker()) {
                            Modules.get().get(InfinityMiner.class).targetBlocks.set(List.of(context.getArgument("target", BlockInput.class).getState().getBlock()));
                            runInfinityMiner();
                        }
                    } else {
                        throw SWARM_NOT_ACTIVE.create();
                    }
                    return SINGLE_SUCCESS;
                })
                .then(argument("repair", BlockStateArgument.block(REGISTRY_ACCESS)).executes(context -> {
                    Swarm swarm = Modules.get().get(Swarm.class);
                    if (swarm.isActive()) {
                        if (swarm.isHost()) {
                            swarm.host.sendMessage(context.getInput());
                        } else if (swarm.isWorker()) {
                            Modules.get().get(InfinityMiner.class).targetBlocks.set(List.of(context.getArgument("target", BlockInput.class).getState().getBlock()));
                            Modules.get().get(InfinityMiner.class).repairBlocks.set(List.of(context.getArgument("repair", BlockInput.class).getState().getBlock()));
                            runInfinityMiner();
                        }
                    } else {
                        throw SWARM_NOT_ACTIVE.create();
                    }
                    return SINGLE_SUCCESS;
                })))
            .then(literal("logout").then(argument("logout", BoolArgumentType.bool()).executes(context -> {
                Swarm swarm = Modules.get().get(Swarm.class);
                if (swarm.isActive()) {
                    if (swarm.isHost()) {
                        swarm.host.sendMessage(context.getInput());
                    } else if (swarm.isWorker()) {
                        Modules.get().get(InfinityMiner.class).logOut.set(BoolArgumentType.getBool(context, "logout"));
                    }
                } else {
                    throw SWARM_NOT_ACTIVE.create();
                }
                return SINGLE_SUCCESS;
            })))
            .then(literal("walkhome").then(argument("walkhome", BoolArgumentType.bool()).executes(context -> {
                Swarm swarm = Modules.get().get(Swarm.class);
                if (swarm.isActive()) {
                    if (swarm.isHost()) {
                        swarm.host.sendMessage(context.getInput());
                    } else if (swarm.isWorker()) {
                        Modules.get().get(InfinityMiner.class).walkHome.set(BoolArgumentType.getBool(context, "walkhome"));
                    }
                } else {
                    throw SWARM_NOT_ACTIVE.create();
                }
                return SINGLE_SUCCESS;
            }))));

        builder.then(literal("mine")
            .then(argument("block", BlockStateArgument.block(REGISTRY_ACCESS)).executes(context -> {
                Swarm swarm = Modules.get().get(Swarm.class);
                if (swarm.isActive()) {
                    if (swarm.isHost()) {
                        swarm.host.sendMessage(context.getInput());
                    } else if (swarm.isWorker()) {
                        swarm.worker.target = context.getArgument("block", BlockInput.class).getState().getBlock();
                    }
                } else {
                    throw SWARM_NOT_ACTIVE.create();
                }
                return SINGLE_SUCCESS;
            }))
        );

        builder.then(literal("toggle")
            .then(argument("module", ModuleArgumentType.create())
                .executes(context -> {
                    Swarm swarm = Modules.get().get(Swarm.class);
                    if (swarm.isActive()) {
                        if (swarm.isHost()) {
                            swarm.host.sendMessage(context.getInput());
                        } else if (swarm.isWorker()) {
                            Module module = ModuleArgumentType.get(context);
                            module.toggle();
                        }
                    } else {
                        throw SWARM_NOT_ACTIVE.create();
                    }
                    return SINGLE_SUCCESS;
                }).then(literal("on")
                    .executes(context -> {
                        Swarm swarm = Modules.get().get(Swarm.class);
                        if (swarm.isActive()) {
                            if (swarm.isHost()) {
                                swarm.host.sendMessage(context.getInput());
                            } else if (swarm.isWorker()) {
                                Module m = ModuleArgumentType.get(context);
                                m.enable();
                            }
                        } else {
                            throw SWARM_NOT_ACTIVE.create();
                        }
                        return SINGLE_SUCCESS;
                    })).then(literal("off")
                    .executes(context -> {
                        Swarm swarm = Modules.get().get(Swarm.class);
                        if (swarm.isActive()) {
                            if (swarm.isHost()) {
                                swarm.host.sendMessage(context.getInput());
                            } else if (swarm.isWorker()) {
                                Module m = ModuleArgumentType.get(context);
                                m.disable();
                            }
                        } else {
                            throw SWARM_NOT_ACTIVE.create();
                        }
                        return SINGLE_SUCCESS;
                    })
                )
            )
        );

        builder.then(literal("scatter").executes(context -> {
            Swarm swarm = Modules.get().get(Swarm.class);
            if (swarm.isActive()) {
                if (swarm.isHost()) {
                    swarm.host.sendMessage(context.getInput());
                } else if (swarm.isWorker()) {
                    scatter(100);
                }
            } else {
                throw SWARM_NOT_ACTIVE.create();
            }
            return SINGLE_SUCCESS;
        }).then(argument("radius", IntegerArgumentType.integer()).executes(context -> {
            Swarm swarm = Modules.get().get(Swarm.class);
            if (swarm.isActive()) {
                if (swarm.isHost()) {
                    swarm.host.sendMessage(context.getInput());
                } else if (swarm.isWorker()) {
                    scatter(IntegerArgumentType.getInteger(context, "radius"));
                }
            } else {
                throw SWARM_NOT_ACTIVE.create();
            }
            return SINGLE_SUCCESS;
        })));

        builder.then(literal("stop").executes(context -> {
            Swarm swarm = Modules.get().get(Swarm.class);
            if (swarm.isActive()) {
                if (swarm.isHost()) {
                    swarm.host.sendMessage(context.getInput());
                } else if (swarm.isWorker()) {
                    PathManagers.get().stop();
                }
            } else {
                throw SWARM_NOT_ACTIVE.create();
            }
            return SINGLE_SUCCESS;
        }));

        builder.then(literal("exec").then(argument("command", StringArgumentType.greedyString()).executes(context -> {
            Swarm swarm = Modules.get().get(Swarm.class);
            if (swarm.isActive()) {
                if (swarm.isHost()) {
                    swarm.host.sendMessage(context.getInput());
                } else if (swarm.isWorker()) {
                    ChatUtils.sendPlayerMsg(StringArgumentType.getString(context, "command"));
                }
            } else {
                throw SWARM_NOT_ACTIVE.create();
            }
            return SINGLE_SUCCESS;
        })));
    }

    private void runInfinityMiner() {
        InfinityMiner infinityMiner = Modules.get().get(InfinityMiner.class);
        infinityMiner.disable();
//        infinityMiner.smartModuleToggle.set(true);
        infinityMiner.enable();
    }

    private void scatter(int radius) {
        Random random = new Random();

        double a = random.nextDouble() * 2 * Math.PI;
        double r = radius * Math.sqrt(random.nextDouble());
        double x = mc.player.getX() + r * Math.cos(a);
        double z = mc.player.getZ() + r * Math.sin(a);

        PathManagers.get().stop();
        PathManagers.get().moveTo(new BlockPos((int) x, 0, (int) z), true);
    }
}

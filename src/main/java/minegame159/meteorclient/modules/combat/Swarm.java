package minegame159.meteorclient.modules.combat;

import baritone.api.BaritoneAPI;
import baritone.api.IBaritone;
import baritone.api.pathing.goals.GoalXZ;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.MeteorClient;
import minegame159.meteorclient.commands.CommandManager;
import minegame159.meteorclient.events.entity.player.PlayerMoveEvent;
import minegame159.meteorclient.events.world.PostTickEvent;
import minegame159.meteorclient.mixininterface.IKeyBinding;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.modules.misc.Timer;
import minegame159.meteorclient.modules.movement.Step;
import minegame159.meteorclient.modules.player.InfinityMiner;
import minegame159.meteorclient.settings.*;
import minegame159.meteorclient.utils.Chat;
import minegame159.meteorclient.utils.MeteorExecutor;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.BlockStateArgument;
import net.minecraft.command.argument.BlockStateArgumentType;
import net.minecraft.command.argument.EntityAnchorArgumentType;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;


/**
 * @author Inclemental
 * Special thanks to Eli for lending me the test account. Love you bud.
 */
public class Swarm extends ToggleModule {
    public Swarm() {
        super(Category.Combat, "Swarm-Beta", "I Am... The Swarm.");
    }

    public enum Mode {
        QUEEN,
        SLAVE,
        IDLE
    }

    public enum CurrentTask {
        COMBAT,
        BARITONE,
        IDLE
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Mode> currentMode = sgGeneral.add(new EnumSetting.Builder<Mode>()
            .name("current-mode")
            .description("The current mode to operate in.")
            .defaultValue(Mode.IDLE)
            .build());
    private final Setting<CurrentTask> currentTaskSetting = sgGeneral.add(new EnumSetting.Builder<CurrentTask>()
            .name("current-task")
            .description("The current task.")
            .defaultValue(CurrentTask.IDLE)
            .build());

    private final Setting<String> targetString = sgGeneral.add(new StringSetting.Builder()
            .name("target")
            .description("Player Name to Target")
            .defaultValue("Sheep")
            .onChanged(string -> resetTarget())
            .build());
    private final Setting<String> ipAddress = sgGeneral.add(new StringSetting.Builder()
            .name("ip-address")
            .description("Server ip address")
            .defaultValue("localhost")
            .build());
    private final Setting<Integer> serverPort = sgGeneral.add(new IntSetting.Builder()
            .name("Port")
            .description("The port for which to run the server on.")
            .defaultValue(7777)
            .build());

    private final SwarmCommandManager commandManager = new SwarmCommandManager();
    private SwarmServer server;
    private SwarmClient client;
    private PlayerEntity QUEEN = null;
    private static boolean COMMANDS_REGISTERED = false;
    protected Entity targetEntity;
    private final PathFinder pathFinder = new PathFinder();

    @Override
    public void onDeactivate() {
        closeAllServerConnections();
        resetTarget();
    }

    public void closeAllServerConnections() {
        Chat.info("Closing Server Connections");
        if (server != null) {
            server.interrupt();
            server.close();
            server = null;
        }
        if (client != null) {
            client.interrupt();
            client.disconnect();
            client = null;
        }
    }

    @SuppressWarnings("unused")
    @EventHandler
    private final Listener<PostTickEvent> onTick = new Listener<>(event -> {
        try {
            if (mc.world == null || mc.player == null) return;
            try {
                commandManager.execute();
            } catch (Exception ignored) {
            }
            if (currentMode.get() == Mode.QUEEN) {
                try {
                    if (server == null)
                        server = new SwarmServer(serverPort.get());
                    if (QUEEN != mc.world.getPlayerByUuid(mc.player.getUuid())) {
                        QUEEN = mc.world.getPlayerByUuid(mc.player.getUuid());
                    }
                } catch (IOException e) {
                    currentMode.set(Mode.IDLE);
                    closeAllServerConnections();
                }
            }
        } catch (Exception ignored) {
        }
    });

    private void resetTarget() {
        targetEntity = null;
    }

    public void idle() {
        currentMode.set(Mode.IDLE);
        currentTaskSetting.set(CurrentTask.IDLE);
        if (ModuleManager.INSTANCE.get(InfinityMiner.class).isActive())
            ModuleManager.INSTANCE.get(InfinityMiner.class).toggle();
        if (getPrimaryBaritone().getPathingBehavior().isPathing())
            getPrimaryBaritone().getPathingBehavior().cancelEverything();
        resetTarget();
    }


    private double getEntitySpeed(Entity entity) {
        if (entity == null) {
            return 0.0;
        }
        double tX = Math.abs(entity.getX() - entity.prevX);
        double tZ = Math.abs(entity.getZ() - entity.prevZ);
        //double tY = Math.abs(entity.getY() - entity.prevY);
        double length = Math.sqrt(tX * tX + tZ * tZ);
        if (ModuleManager.INSTANCE.get(Timer.class).isActive()) {
            length *= ModuleManager.INSTANCE.get(Timer.class).getMultiplier();
        }
        return length * 20;
    }

    public class SwarmClient extends Thread {

        public Socket socket;
        public String ipAddress;

        SwarmClient() {
            ipAddress = strip(Swarm.this.ipAddress.get());
            start();
        }

        @Override
        public void run() {
            InputStream inputStream;
            DataInputStream dataInputStream;
            try {
                socket = new Socket(ipAddress, serverPort.get());
                inputStream = socket.getInputStream();
                dataInputStream = new DataInputStream(inputStream);
                Chat.info("New Socket");
                while (!isInterrupted()) {
                    if (socket != null) {
                        String read;
                        read = dataInputStream.readUTF();
                        if (!read.equals("")) {
                            add(read);
                            Chat.info("New Command: " + read);
                        }
                    }
                }
                dataInputStream.close();
                inputStream.close();
            } catch (Exception e) {
                Chat.error("Error in connection to server.");
                disconnect();
                client = null;
            } finally {
                if (socket != null) {
                    try {
                        socket.close();
                    } catch (Exception e) {
                        Chat.error("Error in connection to server");
                    }
                }
            }
        }

        public void disconnect() {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    Chat.error("Error disconnecting client.");
                }
            }
        }
    }

    public class SwarmServer extends Thread {
        final private ServerSocket serverSocket;
        public int MAX_CLIENTS = 25;
        final private SubServer[] clientConnections = new SubServer[MAX_CLIENTS];

        public SwarmServer(int port) throws IOException {
            this.serverSocket = new ServerSocket(port);
            Chat.info("Swarm Server: New Server Opened On Port " + serverPort.get());
            start();
        }

        @Override
        public void run() {
            try {
                while (!this.isInterrupted()) {
                    Chat.info("Swarm Server: Listening for incoming connections");
                    Socket connection = this.serverSocket.accept();
                    assignConnectionToSubServer(connection);
                }
            } catch (Exception ignored) {

            }

        }

        public void assignConnectionToSubServer(Socket connection) {
            for (int i = 0; i < clientConnections.length; i++) {
                if (this.clientConnections[i] == null) {
                    this.clientConnections[i] = new SubServer(connection);
                    Chat.info("Swarm Server: New Slave Connected");
                    break;
                }
            }
        }

        public void close() {
            try {
                interrupt();
                for (SubServer clientConnection : clientConnections) {
                    if (clientConnection != null) {
                        clientConnection.close();
                    }
                }
                serverSocket.close();
            } catch (Exception e) {
                Chat.info("Server Closed");
            }
        }

        public void closeAllClients() {
            try {
                for (SubServer s : clientConnections) {
                    if (s.connection != null)
                        s.close();
                }
            } catch (Exception e) {
                closeAllServerConnections();
            }
        }

        public void sendMessage(String s) {
            MeteorExecutor.execute(() -> {
                try {
                    for (SubServer clientConnection : clientConnections) {
                        if (clientConnection != null) {
                            clientConnection.messageToSend = s;
                            Chat.info("writing command: " + s);
                        }
                    }
                } catch (Exception ignored) {
                }
            });

        }

    }

    public static class SubServer extends Thread {
        final private Socket connection;
        private volatile String messageToSend;

        public SubServer(Socket connection) {
            this.connection = connection;
            start();
        }

        @Override
        public void run() {
            OutputStream outputStream;
            DataOutputStream dataOutputStream;
            try {
                outputStream = connection.getOutputStream();
                dataOutputStream = new DataOutputStream(outputStream);
                while (!this.isInterrupted()) {
                    if (messageToSend != null) {
                        dataOutputStream.writeUTF(messageToSend);
                        dataOutputStream.flush();
                        messageToSend = null;
                    }
                }
                outputStream.close();
                dataOutputStream.close();
            } catch (Exception e) {
                Chat.info("Error in Subsystem");
            }
        }

        public void close() {
            try {
                Chat.info("Closing client connection");
                interrupt();
                this.connection.close();
            } catch (Exception ignored) {
            }
        }
    }

    public class SwarmCommandManager {

        private final Queue<String> commandQueue = new LinkedList<>();
        private final List<SwarmCommand> commands = new ArrayList<>();

        public SwarmCommandManager() {
            registerCommands();
        }

        public void registerCommands() {
            try {
                addCommand(new SwarmMineCommand());
                addCommand(new SwarmSlaveCommand());
                addCommand(new SwarmEscapeCommand());
                addCommand(new SwarmReleaseCommand());
                addCommand(new SwarmPrintInfoCommand());
                addCommand(new SwarmCloseConnectionsCommand());
                addCommand(new SwarmGoToCommand());
                //addCommand(new SwarmFollowCommand());
                addCommand(new SwarmInfinityMinerCommand());
                addCommand(new SwarmStopCommand());
                //addCommand(new SwarmTargetCommand());
                COMMANDS_REGISTERED = true;
            } catch (
                    Exception ignored) {
            }
        }


        public void addCommand(SwarmCommand command) {
            if (command != null) {
                registerCommand(command);
                commands.add(command);
            }
        }

        public void registerCommand(SwarmCommand swarmCommand) {
            if (!COMMANDS_REGISTERED) {
                LiteralArgumentBuilder<CommandSource> builder = LiteralArgumentBuilder.literal("s");
                swarmCommand.build(builder);
                CommandManager.getDispatcher().register(builder);
            }
        }

        public void execute() {
            if (mc.player == null || mc.world == null || commandQueue.isEmpty()) return;
            try {
                CommandManager.dispatch(commandQueue.poll());
            } catch (CommandSyntaxException ignored) {
            }

        }
    }

    public void add(String s) {
        commandManager.commandQueue.add(s);
    }

    public IBaritone getPrimaryBaritone() {
        return BaritoneAPI.getProvider().getPrimaryBaritone();
    }


    public abstract static class SwarmCommand {

        protected String description, example;

        protected static LiteralArgumentBuilder<CommandSource> literal(final String name) {
            return LiteralArgumentBuilder.literal(name);
        }

        protected static <T> RequiredArgumentBuilder<CommandSource, T> argument(final String name, final ArgumentType<T> type) {
            return RequiredArgumentBuilder.argument(name, type);
        }

        public abstract void build(LiteralArgumentBuilder<CommandSource> builder);

        public void help() {
            Chat.info("Swarm Command Help");
            Chat.info(description);
            Chat.info("Example(s): " + example);
        }

    }

    public class SwarmMineCommand extends SwarmCommand {

        SwarmMineCommand() {
            description = "(highlight)mine <playername>(default) - Baritone Mine A Block";
        }

        @Override
        public void build(LiteralArgumentBuilder<CommandSource> builder) {
            builder.then(literal("mine")
                    .then(argument("block", BlockStateArgumentType.blockState())
                            .executes(context -> {
                                String raw = context.getInput();
                                Block block = context.getArgument("block", BlockStateArgument.class).getBlockState().getBlock();
                                if (currentMode.get() == Mode.QUEEN && server != null) server.sendMessage(strip(raw));
                                if (currentMode.get() != Mode.QUEEN && block != null) {
                                    currentTaskSetting.set(CurrentTask.BARITONE);
                                    getPrimaryBaritone().getMineProcess().mine(block);
                                }
                                return SINGLE_SUCCESS;
                            })
                    )
            );
        }
    }

    public class SwarmSlaveCommand extends SwarmCommand {

        public SwarmSlaveCommand() {
            description = "(highlight)slave (default)- Slave this account to the Queen.";
        }

        @Override
        public void build(LiteralArgumentBuilder<CommandSource> builder) {
            builder.then(literal("slave").executes(context -> {
                        if (currentMode.get() != Mode.QUEEN && client == null)
                            client = new SwarmClient();
                        return SINGLE_SUCCESS;
                    })
            );
        }
    }

    public class SwarmEscapeCommand extends SwarmCommand {

        public SwarmEscapeCommand() {
            description = "(highlight)escape(default)- Removes this player from the active swarm.";
        }

        @Override
        public void build(LiteralArgumentBuilder<CommandSource> builder) {
            builder.then(literal("escape").executes(context -> {
                        if (currentMode.get() != Mode.QUEEN) {
                            closeAllServerConnections();
                            if (getPrimaryBaritone().getPathingBehavior().isPathing())
                                getPrimaryBaritone().getPathingBehavior().cancelEverything();
                            currentMode.set(Mode.IDLE);
                            currentTaskSetting.set(CurrentTask.IDLE);
                            ModuleManager.INSTANCE.get(Swarm.class).toggle();
                        } else {
                            Chat.info("Swarm: You are the queen.");
                        }
                        return SINGLE_SUCCESS;
                    })
            );
        }
    }

    public class SwarmReleaseCommand extends SwarmCommand {

        public SwarmReleaseCommand() {
            description = "(highlight)release(default) - Release your bots.";
        }

        @Override
        public void build(LiteralArgumentBuilder<CommandSource> builder) {
            builder.then(literal("release").executes(context -> {
                        if (currentMode.get() == Mode.QUEEN && server != null) {
                            server.sendMessage("s stop");
                            server.closeAllClients();
                        }
                        return SINGLE_SUCCESS;
                    })
            );
        }
    }

    public class SwarmPrintInfoCommand extends SwarmCommand {

        public SwarmPrintInfoCommand() {
            description = "(highlight)help(default) - Prints a list of all commands, and formatting information.";
        }

        public void printSwarmInfo() {
            Chat.info("(highlight)Welcome to Swarm.");
            Chat.info("Below are all listed commands, check the docs for more detailed information");
            Chat.info("<> Denotes a field to fill. <?> Denotes an optional field.");
            for (SwarmCommand s : commandManager.commands) {
                Chat.info(s.description);
            }
        }

        @Override
        public void build(LiteralArgumentBuilder<CommandSource> builder) {
            builder.then(literal("help").executes(context -> {
                        printSwarmInfo();
                        return SINGLE_SUCCESS;
                    })
            );
        }
    }

    public class SwarmCloseConnectionsCommand extends SwarmCommand {

        public SwarmCloseConnectionsCommand() {
            description = "(highlight)close(default) - Close all network connections and cancel all tasks.";
        }

        @Override
        public void build(LiteralArgumentBuilder<CommandSource> builder) {
            builder.then(literal("close").executes(context -> {
                        try {
                            closeAllServerConnections();
                            currentMode.set(Mode.IDLE);
                            currentTaskSetting.set(CurrentTask.IDLE);
                            if (getPrimaryBaritone().getPathingBehavior().isPathing())
                                getPrimaryBaritone().getPathingBehavior().cancelEverything();
                            resetTarget();
                            if (ModuleManager.INSTANCE.get(Swarm.class).isActive())
                                ModuleManager.INSTANCE.get(Swarm.class).toggle();
                        } catch (Exception ignored) {
                        }
                        return SINGLE_SUCCESS;
                    })
            );
        }
    }

    public class SwarmGoToCommand extends SwarmCommand {

        public SwarmGoToCommand() {
            description = "(highlight)goto <x> <z>(default) - Path to a destination.";
            example = ".s goto 0 ~";
        }

        @Override
        public void build(LiteralArgumentBuilder<CommandSource> builder) {
            builder.then(literal("goto")
                    .then(argument("x", IntegerArgumentType.integer())
                            .then(argument("z", IntegerArgumentType.integer()).executes(context -> {
                                        int x = context.getArgument("x", Integer.class);
                                        int z = context.getArgument("z", Integer.class);
                                        Chat.info("X: " + x + " z: " + z);
                                        if (currentMode.get() == Mode.QUEEN && server != null) {
                                            server.sendMessage(context.getInput());
                                        } else if (currentMode.get() != Mode.QUEEN) {
                                            getPrimaryBaritone().getCustomGoalProcess().setGoalAndPath(new GoalXZ(x, z));
                                        }
                                        return SINGLE_SUCCESS;
                                    })
                            )
                    ).then(literal("help").executes(context -> {
                        help();
                        return SINGLE_SUCCESS;
                    }))
            );
        }

        @Override
        public String toString() {
            return super.toString();
        }
    }

    public class SwarmFollowCommand extends SwarmCommand {
        public SwarmFollowCommand() {
            description = "(highlight)follow <?player>(default) - Follow a player. Defaults to the Queen.";
        }

        @Override
        public void build(LiteralArgumentBuilder<CommandSource> builder) {
            builder.then(literal("follow")
                    .then(argument("name", EntityArgumentType.players()).executes(context -> {

                                return SINGLE_SUCCESS;
                            })
                    )
            );
        }
    }

    public class SwarmInfinityMinerCommand extends SwarmCommand {

        public SwarmInfinityMinerCommand() {
            description = "(highlight)im <?TargetBlock> <?RepairBlock>(default) - Start Infinity Miner.";
            example = "(highlight).s im minecraft:stone minecraft:dirt, .s im minecraft:stone, .s logout false";
        }

        public void runInfinityMiner() {
            InfinityMiner infinityMiner = ModuleManager.INSTANCE.get(InfinityMiner.class);
            if (infinityMiner.isActive()) infinityMiner.toggle();
            infinityMiner.smartModuleToggle.set(true);
            if (!infinityMiner.isActive()) infinityMiner.toggle();
        }


        @Override
        public void build(LiteralArgumentBuilder<CommandSource> builder) {
            builder.then(literal("im").executes(context -> {
                        if (currentMode.get() == Mode.QUEEN) {
                            server.sendMessage(context.getInput());
                        } else {
                            runInfinityMiner();
                        }
                        return SINGLE_SUCCESS;
                    }).then(argument("target", BlockStateArgumentType.blockState()).executes(context -> {
                        if (currentMode.get() == Mode.QUEEN) {
                            server.sendMessage(context.getInput());
                        } else {
                            ModuleManager.INSTANCE.get(InfinityMiner.class).targetBlock.set(context.getArgument("target", BlockStateArgument.class).getBlockState().getBlock());
                            runInfinityMiner();
                        }
                        return SINGLE_SUCCESS;
                    }).then(argument("repair", BlockStateArgumentType.blockState()).executes(context -> {
                        if (currentMode.get() == Mode.QUEEN) {
                            server.sendMessage(context.getInput());
                        } else {
                            ModuleManager.INSTANCE.get(InfinityMiner.class).targetBlock.set(context.getArgument("target", BlockStateArgument.class).getBlockState().getBlock());
                            ModuleManager.INSTANCE.get(InfinityMiner.class).repairBlock.set(context.getArgument("repair", BlockStateArgument.class).getBlockState().getBlock());
                            runInfinityMiner();
                        }
                        return SINGLE_SUCCESS;
                    })))
                            .then(literal("logout").then(argument("autologout", BoolArgumentType.bool()).executes(context -> {
                                if (currentMode.get() == Mode.QUEEN) {
                                    server.sendMessage(context.getInput());
                                } else {
                                    boolean bool = context.getArgument("autologout", Boolean.class);
                                    InfinityMiner infinityMiner = ModuleManager.INSTANCE.get(InfinityMiner.class);
                                    infinityMiner.autoLogOut.set(bool);
                                }
                                return SINGLE_SUCCESS;
                            })))
                            .then(literal("walkhome").then(argument("walkhome", BoolArgumentType.bool()).executes(context -> {
                                if (currentMode.get() == Mode.QUEEN) {
                                    server.sendMessage(context.getInput());
                                } else {
                                    boolean bool = context.getArgument("walkhome", Boolean.class);
                                    InfinityMiner infinityMiner = ModuleManager.INSTANCE.get(InfinityMiner.class);
                                    infinityMiner.autoWalkHome.set(bool);
                                }
                                return SINGLE_SUCCESS;
                            })))
                            .then(literal("help").executes(context -> {
                                help();
                                return SINGLE_SUCCESS;
                            }))
            );
        }
    }

    public class SwarmStopCommand extends SwarmCommand {

        public SwarmStopCommand() {
            description = "(highlight)stop(default) Stop all current tasks.";
        }

        @Override
        public void build(LiteralArgumentBuilder<CommandSource> builder) {
            builder.then(literal("stop").executes(context -> {
                if (currentMode.get() == Mode.QUEEN && server != null) {
                    server.sendMessage(context.getInput());
                } else {
                    idle();
                }
                return SINGLE_SUCCESS;
            }));
        }
    }

    public class SwarmTargetCommand extends SwarmCommand {

        public SwarmTargetCommand() {
            description = "(highlight)target <player>";
        }

        @Override
        public void build(LiteralArgumentBuilder<CommandSource> builder) {
            builder.then(literal("target").then(argument("target", EntityArgumentType.players()).executes(context -> {
                if (currentMode.get() == Mode.QUEEN && server != null) {
                    server.sendMessage(context.getInput());
                } else {
                    pathFinder.initiate(context.getArgument("target",Entity.class));
                }
                return SINGLE_SUCCESS;
            })));
        }
    }

    public class PathFinder {
        private Entity target;
        private final static int PATH_AHEAD = 3;
        private final ArrayList<PathBlock> PATH = new ArrayList<>(PATH_AHEAD);
        private final static int QUAD_1 = 1, QUAD_2 = 2, SOUTH = 0, NORTH = 180;
        private PathBlock currentPathBlock;

        public class PathBlock {
            public final Block block;
            public final BlockPos blockPos;
            public final BlockState blockState;
            public double yaw;

            public PathBlock(Block b, BlockPos pos, BlockState state) {
                block = b;
                blockPos = pos;
                blockState = state;
            }

            public PathBlock(Block b, BlockPos pos) {
                block = b;
                blockPos = pos;
                blockState = getBlockStateAtPos(blockPos);
            }

            public PathBlock(BlockPos pos) {
                blockPos = pos;
                block = getBlockAtPos(pos);
                blockState = getBlockStateAtPos(blockPos);
            }

        }

        public PathBlock getNextPathBlock() {
            PathBlock nextBlock = new PathBlock(new BlockPos(getNextStraightPos()));
            double stepHeight = (ModuleManager.INSTANCE.get(Step.class).isActive()) ? ModuleManager.INSTANCE.get(Step.class).height.get() : 0;
            if (isSolidFloor(nextBlock.blockPos) && isAirAbove(nextBlock.blockPos)) {
                return nextBlock;
            } else if (!isSolidFloor(nextBlock.blockPos) && isAirAbove(nextBlock.blockPos)) {
                int drop = getDrop(nextBlock.blockPos);
                if (getDrop(nextBlock.blockPos) < 3) {
                    nextBlock = new PathBlock(new BlockPos(nextBlock.blockPos.getX(), nextBlock.blockPos.getY() - drop, nextBlock.blockPos.getZ()));
                }
            }

            return nextBlock;
        }

        public int getDrop(BlockPos pos) {
            int drop = 0;
            while (!isSolidFloor(pos) && drop < 3) {
                drop++;
                pos = new BlockPos(pos.getX(), pos.getY() - 1, pos.getZ());
            }
            return drop;
        }

        public boolean isAirAbove(BlockPos blockPos) {
            if (!getBlockStateAtPos(blockPos.getX(), blockPos.getY(), blockPos.getZ()).isAir())
                return false;
            if (!getBlockStateAtPos(blockPos.getX(), blockPos.getY() + 1, blockPos.getZ()).isAir())
                return false;
            return true;
        }

        public Vec3d getNextStraightPos() {
            Vec3d nextPos = new Vec3d(mc.player.getX(), mc.player.getY(), mc.player.getZ());
            double multiplier = 1.0;
            while (nextPos == mc.player.getPos()) {
                nextPos = new Vec3d((int) (mc.player.getX() + multiplier * Math.cos(Math.toRadians(mc.player.yaw))), (int) (mc.player.getY()), (int) (mc.player.getZ() + multiplier * Math.sin(Math.toRadians(mc.player.yaw))));
                multiplier += .1;
            }
            return nextPos;
        }

        public int getYawToTarget() {
            if (target == null || mc.player == null) return Integer.MAX_VALUE;
            Vec3d tPos = target.getPos();
            Vec3d pPos = mc.player.getPos();
            int yaw = 0;
            int direction = getDirection();
            double tan = (tPos.z - pPos.z) / (tPos.x - pPos.x);
            if (direction == QUAD_1)
                yaw = (int) (Math.PI / 2 - Math.atan(tan));
            else if (direction == QUAD_2)
                yaw = (int) (-1 * Math.PI / 2 - Math.atan(tan));
            else return direction;
            return yaw;
        }

        public int getDirection() {
            if (target == null || mc.player == null) return 0;
            Vec3d targetPos = target.getPos();
            Vec3d playerPos = mc.player.getPos();
            if (targetPos.x == playerPos.x && targetPos.z > playerPos.z)
                return SOUTH;
            if (targetPos.x == playerPos.x && targetPos.z < playerPos.z)
                return NORTH;
            if (targetPos.x < playerPos.x)
                return QUAD_1;
            if (targetPos.x > playerPos.x)
                return QUAD_2;
            return 0;
        }

        public BlockState getBlockStateAtPos(BlockPos pos) {
            if (mc.world != null)
                return mc.world.getBlockState(pos);
            return null;
        }

        public BlockState getBlockStateAtPos(int x, int y, int z) {
            if (mc.world != null)
                return mc.world.getBlockState(new BlockPos(x, y, z));
            return null;
        }

        public Block getBlockAtPos(BlockPos pos) {
            if (mc.world != null)
                return mc.world.getBlockState(pos).getBlock();
            return null;
        }

        public boolean isSolidFloor(BlockPos blockPos) {
            return isAir(getBlockAtPos(blockPos));
        }

        public boolean isAir(Block block) {
            return block.is(Blocks.AIR);
        }

        public boolean isWater(Block block) {
            return block.is(Blocks.WATER);
        }

        public void lookAtDestination(PathBlock pathBlock) {
            if(mc.player != null) {
                mc.player.lookAt(EntityAnchorArgumentType.EntityAnchor.EYES, new Vec3d(pathBlock.blockPos.getX(), pathBlock.blockPos.getY() + mc.player.getStandingEyeHeight(), pathBlock.blockPos.getZ()));
            }
        }

        Listener<PlayerMoveEvent> moveEventListener = new Listener<>(event -> {
            if (target != null && mc.player != null) {
                if (mc.player.distanceTo(targetEntity) > 3) {
                    if (currentPathBlock == null) currentPathBlock = getNextPathBlock();
                    if (mc.player.getPos().distanceTo(new Vec3d(currentPathBlock.blockPos.getX(), currentPathBlock.blockPos.getY(), currentPathBlock.blockPos.getZ())) < .1)
                        currentPathBlock = getNextPathBlock();
                    lookAtDestination(currentPathBlock);
                    if (!mc.options.keyForward.isPressed())
                        ((IKeyBinding) mc.options.keyForward).setPressed(true);
                } else {
                    if (mc.options.keyForward.isPressed())
                        ((IKeyBinding) mc.options.keyForward).setPressed(false);
                    PATH.clear();
                    currentPathBlock = null;
                }
            }
        });

        public void initiate(Entity entity) {
            target = entity;
            if (target != null)
                currentPathBlock = getNextPathBlock();
            MeteorClient.EVENT_BUS.subscribe(moveEventListener);
        }

        public void disable() {
            target = null;
            PATH.clear();
            if (mc.options.keyForward.isPressed())
                ((IKeyBinding) mc.options.keyForward).setPressed(false);
            MeteorClient.EVENT_BUS.unsubscribe(moveEventListener);
        }

    }

    public static String strip(String s) {
        StringBuilder sb = new StringBuilder(s);
        for (int i = 0; i < sb.length(); i++) {
            if (sb.charAt(i) != ' ')
                break;
            else
                sb.deleteCharAt(i);
        }
        for (int i = sb.length() - 1; i >= 0; i--) {
            if (sb.charAt(i) != ' ')
                break;
            else
                sb.deleteCharAt(i);
        }
        return sb.toString();
    }

}
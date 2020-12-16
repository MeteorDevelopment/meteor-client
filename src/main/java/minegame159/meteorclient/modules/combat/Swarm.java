package minegame159.meteorclient.modules.combat;

import baritone.api.BaritoneAPI;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.commands.CommandManager;
import minegame159.meteorclient.events.world.PostTickEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.modules.misc.Timer;
import minegame159.meteorclient.modules.player.InfinityMiner;
import minegame159.meteorclient.settings.*;
import minegame159.meteorclient.utils.Chat;
import minegame159.meteorclient.utils.MeteorExecutor;
import minegame159.meteorclient.utils.PathFinder;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;


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

    public final Setting<Mode> currentMode = sgGeneral.add(new EnumSetting.Builder<Mode>()
            .name("current-mode")
            .description("The current mode to operate in.")
            .defaultValue(Mode.IDLE)
            .onChanged(mode -> {
                try {
                    if (mode == Mode.QUEEN) {
                        if (!isClientNull())
                            closeClient();
                        if (isServerNull()) {
                            startServer();
                        }
                    } else if (mode == Mode.SLAVE) {
                        if (!isServerNull())
                            closeServer();
                        if (isClientNull()) {
                            startClient();
                        }
                    } else if (mode == Mode.IDLE) {
                        resetTarget();
                        BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().cancelEverything();
                    }
                } catch (Exception ignored) {
                }
            })
            .build());

    public final Setting<CurrentTask> currentTaskSetting = sgGeneral.add(new EnumSetting.Builder<CurrentTask>()
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

    public SwarmServer server;
    public SwarmClient client;
    public Entity targetEntity;
    public PathFinder pathFinder = new PathFinder();
    //public final PathFinder pathFinder = new PathFinder();
    public BlockState targetBlock;


    public boolean isClientNull() {
        return client == null;
    }

    public void closeClient() {
        if (!isClientNull()) {
            client.interrupt();
            client.disconnect();
            client = null;
        }
    }

    public boolean isServerNull() {
        return client == null;
    }

    public void closeServer() {
        if (!isClientNull()) {
            server.interrupt();
            server.close();
            server = null;
        }
    }


    @Override
    public void onActivate() {
        if (currentMode.get() == Mode.QUEEN && server == null) {
            server = new SwarmServer();
        } else if (currentMode.get() == Mode.SLAVE && client == null) {
            client = new SwarmClient();
        }
    }

    @Override
    public void onDeactivate() {
        closeAllServerConnections();
        resetTarget();
    }

    public void closeAllServerConnections() {
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
        mine();
    });

    public void resetTarget() {
        targetEntity = null;
    }

    public void idle() {
        currentMode.set(Mode.IDLE);
        currentTaskSetting.set(CurrentTask.IDLE);
        if (ModuleManager.INSTANCE.get(InfinityMiner.class).isActive())
            ModuleManager.INSTANCE.get(InfinityMiner.class).toggle();
        if (BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().isPathing())
            BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().cancelEverything();

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

    public void startClient() {
        if (client == null) {
            client = new SwarmClient();
        }
    }

    public void startServer() {
        if (server == null) {
            server = new SwarmServer();
        }
    }

    public void mine() {
        if (targetBlock != null) {
            if (BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().isPathing())
                BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().cancelEverything();
            BaritoneAPI.getProvider().getPrimaryBaritone().getMineProcess().mine(targetBlock.getBlock());
            targetBlock = null;
        }
    }

    public class SwarmClient extends Thread {

        public Socket socket;
        public String ipAddress;

        SwarmClient() {
            ipAddress = Swarm.this.ipAddress.get();
            start();
        }

        @Override
        public void run() {
            InputStream inputStream;
            DataInputStream dataInputStream;
            try {
                while (socket == null && !isInterrupted()) {
                    try {
                        if (!server.isAlive())
                            socket = new Socket(ipAddress, serverPort.get());
                    } catch (Exception ignored) {
                        Chat.info("Server Not Found. Retrying in 5 seconds.");
                    }
                    if (socket == null) {
                        Thread.sleep(5000);
                    }
                }
                if (socket != null) {
                    inputStream = socket.getInputStream();
                    dataInputStream = new DataInputStream(inputStream);
                    Chat.info("New Socket");
                    while (!isInterrupted()) {
                        if (socket != null) {
                            String read;
                            read = dataInputStream.readUTF();
                            if (!read.equals("")) {
                                Chat.info("New Command: " + read);
                                execute(read);
                            }
                        }
                    }
                    dataInputStream.close();
                    inputStream.close();
                }
            } catch (Exception e) {
                Chat.info("Error in connection to server");
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
                }
            }
        }
    }

    public class SwarmServer extends Thread {
        private ServerSocket serverSocket;
        public int MAX_CLIENTS = 25;
        final private SubServer[] clientConnections = new SubServer[MAX_CLIENTS];
        private int port = serverPort.get();

        public SwarmServer() {
            try {
                while (serverSocket == null) {
                    if (client == null)
                        this.serverSocket = new ServerSocket(port);
                }
                Chat.info("Swarm Server: New Server Opened On Port " + serverPort.get());
                start();
            } catch (Exception ignored) {

            }
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
                interrupt();
                this.connection.close();
            } catch (Exception ignored) {
            }
        }
    }

    public void execute(String s) {
        if (mc.player == null || mc.world == null) return;
        try {
            CommandManager.dispatch(s);
        } catch (CommandSyntaxException ignored) {
        }
    }

}
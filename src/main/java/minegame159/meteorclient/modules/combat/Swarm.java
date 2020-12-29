package minegame159.meteorclient.modules.combat;

import baritone.api.BaritoneAPI;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.commands.CommandManager;
import minegame159.meteorclient.events.game.GameJoinedEvent;
import minegame159.meteorclient.events.game.GameLeftEvent;
import minegame159.meteorclient.events.world.PostTickEvent;
import minegame159.meteorclient.gui.widgets.WButton;
import minegame159.meteorclient.gui.widgets.WTable;
import minegame159.meteorclient.gui.widgets.WWidget;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.modules.player.InfinityMiner;
import minegame159.meteorclient.settings.*;
import minegame159.meteorclient.utils.Chat;
import minegame159.meteorclient.utils.MeteorExecutor;
import net.minecraft.block.BlockState;

import javax.annotation.Nonnull;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;


/**
 * @author Inclemental
 * Special thanks to Eli for lending me the test account. Love you bud.
 */

public class Swarm extends ToggleModule {
    public Swarm() {
        super(Category.Combat, "Swarm", "I Am... The Swarm.");
    }

    public enum Mode {
        QUEEN,
        SLAVE,
        IDLE
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<String> ipAddress = sgGeneral.add(new StringSetting.Builder()
            .name("ip-address")
            .description("Ip address of the Queen.")
            .defaultValue("localhost")
            .build());

    private final Setting<Integer> serverPort = sgGeneral.add(new IntSetting.Builder()
            .name("Port")
            .description("The port used for connections.")
            .defaultValue(7777)
            .sliderMin(1)
            .sliderMax(65535)
            .build());

    private final SettingGroup sgModes = settings.createGroup("Modes");

    public final Setting<Mode> currentMode = sgModes.add(new EnumSetting.Builder<Mode>()
            .name("current-mode")
            .description("The current mode to operate in. No need to change.")
            .defaultValue(Mode.IDLE)
            .build());

    public SwarmServer server;
    public SwarmClient client;
    public BlockState targetBlock;

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
    }

    @Override
    public WWidget getWidget() {
        WTable table = new WTable();
        WButton runServer = new WButton("Run Server(Q)");
        runServer.action = this::runServer;
        table.add(runServer);
        WButton connect = new WButton("Connect(S)");
        connect.action = this::runClient;
        table.add(connect);
        WButton reset = new WButton("Reset");
        reset.action = () -> {
            Chat.info("Swarm: Closing all connections.");
            closeAllServerConnections();
            currentMode.set(Mode.IDLE);
        };
        table.add(reset);
        return table;
    }

    public void runServer() {
        if (server == null) {
            currentMode.set(Mode.QUEEN);
            closeAllServerConnections();
            server = new SwarmServer();
        }
    }

    public void runClient() {
        if (client == null) {
            currentMode.set(Mode.SLAVE);
            closeAllServerConnections();
            client = new SwarmClient();
        }
    }

    public void closeAllServerConnections() {
        try {
            if (server != null) {
                server.interrupt();
                server.close();
                server.serverSocket.close();
                server = null;
            }
            if (client != null) {
                client.interrupt();
                client.disconnect();
                client.socket.close();
                client = null;
            }
        } catch (Exception ignored) {
        }
    }

    @SuppressWarnings("unused")
    @EventHandler
    private final Listener<PostTickEvent> onTick = new Listener<>(event -> {
        if (targetBlock != null)
            mine();
    });

    public void idle() {
        currentMode.set(Mode.IDLE);
        if (ModuleManager.INSTANCE.get(InfinityMiner.class).isActive())
            ModuleManager.INSTANCE.get(InfinityMiner.class).toggle();
        if (BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().isPathing())
            BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().cancelEverything();
    }

    public void mine() {
        Chat.info("Swarm: Starting Mining Job.");
        if (BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().isPathing())
            BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().cancelEverything();
        BaritoneAPI.getProvider().getPrimaryBaritone().getMineProcess().mine(targetBlock.getBlock());
        targetBlock = null;

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
                        socket = new Socket(ipAddress, serverPort.get());
                    } catch (Exception ignored) {
                        Chat.info("Server not found. Retrying in 5 seconds.");
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
                Chat.error("There is in error in your connection to the server.");
                disconnect();
                client = null;
            } finally {
                if (socket != null) {
                    try {
                        socket.close();
                    } catch (Exception e) {
                        Chat.error("There is in error in your connection to the server.");
                    }
                }
            }
        }

        public void disconnect() {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    public class SwarmServer extends Thread {
        private ServerSocket serverSocket;
        public final static int MAX_CLIENTS = 50;
        final private SubServer[] clientConnections = new SubServer[MAX_CLIENTS];

        public SwarmServer() {
            try {
                int port = serverPort.get();
                this.serverSocket = new ServerSocket(port);
                Chat.info("Swarm Server: New Server Opened On Port " + serverPort.get());
                start();
            } catch (Exception ignored) {

            }
        }

        @Override
        public void run() {
            try {
                Chat.info("Swarm Server: Listening for incoming connections.");
                while (!this.isInterrupted()) {
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
                    Chat.info("Swarm Server: New Slave Connected.");
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
                Chat.info("Server closed.");
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

        public synchronized void sendMessage(@Nonnull String s) {
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

        public SubServer(@Nonnull Socket connection) {
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
                Chat.info("Error in subsystem.");
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

    public void execute(@Nonnull String s) {
        try {
            CommandManager.dispatch(s);
        } catch (CommandSyntaxException ignored) {
        }
    }

    @SuppressWarnings("unused")
    @EventHandler
    private final Listener<GameLeftEvent> gameLeftEventListener = new Listener<>(event -> {
        closeAllServerConnections();
        this.toggle();
    });

    @SuppressWarnings("unused")
    @EventHandler
    private final Listener<GameJoinedEvent> gameJoinedEventListener = new Listener<>(event -> {
        closeAllServerConnections();
        this.toggle();
    });

}